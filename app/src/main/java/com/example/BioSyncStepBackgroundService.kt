package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time background health service that keeps Google Fit & Health Connect
 * telemetry (Total Steps and Distance) lively synchronized even when BioSync is in the background.
 */
class BioSyncStepBackgroundService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var syncLoopJob: Job? = null
  private var notificationUpdateJob: Job? = null
  private var lastNotifiedSteps: Int = -1
  private var lastNotifiedDistanceHundredthsKm: Int = -1

  override fun onCreate() {
    super.onCreate()
    promoteToForegroundSafely()
    BioSyncHealthStepManager.startLiveStepSensorTracking(applicationContext)
    startBackgroundSyncLoop()
    startLiveNotificationObserver()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    promoteToForegroundSafely()
    BioSyncHealthStepManager.startLiveStepSensorTracking(applicationContext)
    BioSyncHealthStepManager.flushHardwareStepSensorNow(applicationContext)
    if (syncLoopJob?.isActive != true) {
      startBackgroundSyncLoop()
    }
    if (notificationUpdateJob?.isActive != true) {
      startLiveNotificationObserver()
    }
    return START_STICKY
  }

  private fun startBackgroundSyncLoop() {
    syncLoopJob?.cancel()
    syncLoopJob = serviceScope.launch {
      val stored = BioSyncLocalStore.load(applicationContext)
      BioSyncHealthStepManager.initializeFromLocalStore(
        context = applicationContext,
        currentDecidedGoal = stored.decidedDailyStepsGoal,
      )
      while (isActive) {
        runCatching {
          BioSyncHealthStepManager.syncWithHealthConnect(applicationContext)
        }
        delay(30_000L)
      }
    }
  }

  private fun startLiveNotificationObserver() {
    notificationUpdateJob?.cancel()
    notificationUpdateJob = serviceScope.launch {
      val channelId = ensureNotificationChannel()
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      BioSyncHealthStepManager.syncState.collectLatest { state ->
        val distHundredthsKm = (state.liveDistanceKm * 100f).roundToInt()
        if (
          state.liveTodaySteps != lastNotifiedSteps ||
          distHundredthsKm != lastNotifiedDistanceHundredthsKm
        ) {
          lastNotifiedSteps = state.liveTodaySteps
          lastNotifiedDistanceHundredthsKm = distHundredthsKm
          runCatching {
            nm?.notify(NOTIFICATION_ID, buildSilentTelemetryNotification(channelId))
          }
          delay(300L)
        }
      }
    }
  }

  private fun promoteToForegroundSafely() {
    runCatching {
      val channelId = ensureNotificationChannel()
      val notification = buildSilentTelemetryNotification(channelId)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    }
  }

  private fun ensureNotificationChannel(): String {
    val channelId = "biosync_live_step_telemetry_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val existing = nm?.getNotificationChannel(channelId)
      if (existing == null && nm != null) {
        val channel = NotificationChannel(
          channelId,
          "BioSync Live Step & Health Sync",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Real-time Google Fit and Health Connect background telemetry sync"
          setShowBadge(false)
          enableVibration(false)
          setSound(null, null)
        }
        nm.createNotificationChannel(channel)
      }
    }
    return channelId
  }

  private fun buildSilentTelemetryNotification(channelId: String): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingFlags)
    val state = BioSyncHealthStepManager.syncState.value
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    val stepsFormatted = fmt.format(state.liveTodaySteps)
    val distKmFormatted = String.format(Locale.US, "%.2f km", state.liveDistanceKm.coerceAtLeast(0f))
    val summaryLine = "$stepsFormatted steps • $distKmFormatted"

    return NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("BioSync • Synced with Google Fit")
      .setContentText(summaryLine)
      .setStyle(NotificationCompat.BigTextStyle().bigText(summaryLine))
      .setOngoing(true)
      .setSilent(true)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setContentIntent(pendingIntent)
      .build()
  }

  override fun onDestroy() {
    syncLoopJob?.cancel()
    notificationUpdateJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    private const val NOTIFICATION_ID = 4108

    fun startIfAuthorized(context: Context) {
      val appContext = context.applicationContext
      if (!BioSyncHealthStepManager.hasActivityRecognitionPermission(appContext)) return
      runCatching {
        val serviceIntent = Intent(appContext, BioSyncStepBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          appContext.startForegroundService(serviceIntent)
        } else {
          appContext.startService(serviceIntent)
        }
      }
    }
  }
}

/**
 * Restores background step sensor & Health Connect synchronization automatically after a device
 * reboot or app update once the user has configured their BioSync protocol.
 */
class BioSyncBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    val appContext = context?.applicationContext ?: return
    val action = intent?.action.orEmpty()
    if (
      action == Intent.ACTION_BOOT_COMPLETED ||
      action == "android.intent.action.QUICKBOOT_POWERON" ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      val stored = BioSyncLocalStore.load(appContext)
      BioSyncHealthStepManager.initializeFromLocalStore(
        context = appContext,
        currentDecidedGoal = stored.decidedDailyStepsGoal,
      )
      if (BioSyncHealthStepManager.hasActivityRecognitionPermission(appContext)) {
        BioSyncHealthStepManager.startLiveStepSensorTracking(appContext)
        BioSyncStepBackgroundService.startIfAuthorized(appContext)
      }
    }
  }
}
