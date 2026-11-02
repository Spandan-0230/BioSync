package com.example

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time background health service that keeps Google Fit & Health Connect
 * telemetry (Total Steps, Distance, Hydration, Macros, and 6-Pillar Readiness)
 * lively synchronized in the unified 6-Pillar Native XML Telemetry HUD Notification
 * and Home Screen Widgets.
 */
class BioSyncStepBackgroundService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var syncLoopJob: Job? = null
  private var notificationUpdateJob: Job? = null
  private var lastNotifiedSteps: Int = -1
  private var lastNotifiedDistanceHundredthsKm: Int = -1
  private var lastStoreRevision: Long = -1L
  private var isScreenWakeReceiverRegistered = false

  private val screenWakeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
        val appCtx = applicationContext
        BioSyncHealthStepManager.startLiveStepSensorTracking(appCtx)
        BioSyncHealthStepManager.flushHardwareStepSensorNow(appCtx)
        serviceScope.launch {
          runCatching {
            BioSyncHealthStepManager.syncWithHealthConnect(appCtx, force = false)
          }
        }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    cancelLegacyDuplicateNotification()
    promoteToForegroundSafely()
    registerScreenWakeReceiverSafely()
    BioSyncHealthStepManager.startLiveStepSensorTracking(applicationContext)
    startBackgroundSyncLoop()
    startLiveNotificationObserver()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    cancelLegacyDuplicateNotification()
    promoteToForegroundSafely()
    registerScreenWakeReceiverSafely()
    BioSyncHealthStepManager.startLiveStepSensorTracking(applicationContext)
    BioSyncHealthStepManager.flushHardwareStepSensorNow(applicationContext)
    if (syncLoopJob?.isActive != true) {
      startBackgroundSyncLoop()
    } else {
      serviceScope.launch {
        runCatching {
          BioSyncHealthStepManager.syncWithHealthConnect(applicationContext, force = false)
        }
      }
    }
    if (notificationUpdateJob?.isActive != true) {
      startLiveNotificationObserver()
    }
    return START_STICKY
  }

  private fun registerScreenWakeReceiverSafely() {
    if (isScreenWakeReceiverRegistered) return
    runCatching {
      val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
      }
      ContextCompat.registerReceiver(
        this,
        screenWakeReceiver,
        filter,
        ContextCompat.RECEIVER_NOT_EXPORTED,
      )
      isScreenWakeReceiverRegistered = true
    }
  }

  private fun unregisterScreenWakeReceiverSafely() {
    if (!isScreenWakeReceiverRegistered) return
    runCatching {
      unregisterReceiver(screenWakeReceiver)
    }
    isScreenWakeReceiverRegistered = false
  }

  private fun cancelLegacyDuplicateNotification() {
    runCatching {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.cancel(LEGACY_NOTIFICATION_ID)
    }
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
          BioSyncHealthStepManager.flushHardwareStepSensorNow(applicationContext)
          BioSyncHealthStepManager.syncWithHealthConnect(applicationContext)
        }
        delay(10_000L)
      }
    }
  }

  private fun startLiveNotificationObserver() {
    notificationUpdateJob?.cancel()
    notificationUpdateJob = serviceScope.launch {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      combine(
        BioSyncHealthStepManager.syncState,
        DashboardLocalStore.storeRevisionFlow,
      ) { stepState, revision ->
        stepState to revision
      }.collectLatest { (state, revision) ->
        val distHundredthsKm = (state.liveDistanceKm * 100f).roundToInt()
        if (
          state.liveTodaySteps != lastNotifiedSteps ||
          distHundredthsKm != lastNotifiedDistanceHundredthsKm ||
          revision != lastStoreRevision
        ) {
          lastNotifiedSteps = state.liveTodaySteps
          lastNotifiedDistanceHundredthsKm = distHundredthsKm
          lastStoreRevision = revision
          runCatching {
            if (BioSyncWidgetAndNotificationEngine.hasNotificationPermission(this@BioSyncStepBackgroundService)) {
              nm?.notify(
                NOTIFICATION_ID,
                buildUnified6PillarTelemetryNotification(),
              )
            }
            BioSyncWidgetAndNotificationEngine.refreshAllWidgets(this@BioSyncStepBackgroundService)
          }
          delay(250L)
        }
      }
    }
  }

  private fun promoteToForegroundSafely() {
    runCatching {
      val notification = buildUnified6PillarTelemetryNotification()
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

  private fun buildUnified6PillarTelemetryNotification(): Notification {
    return BioSyncWidgetAndNotificationEngine.buildLiveTelemetryHudNotification(
      context = this,
      ongoing = true,
      channelId = BioSyncWidgetAndNotificationEngine.LIVE_TELEMETRY_CHANNEL_ID,
    )
  }

  override fun onDestroy() {
    unregisterScreenWakeReceiverSafely()
    syncLoopJob?.cancel()
    notificationUpdateJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    private const val LEGACY_NOTIFICATION_ID = 4108
    private const val NOTIFICATION_ID = BioSyncWidgetAndNotificationEngine.LIVE_TELEMETRY_NOTIFICATION_ID

    fun startIfAuthorized(context: Context) {
      val appContext = context.applicationContext
      val hasActivityPerm = BioSyncHealthStepManager.hasActivityRecognitionPermission(appContext)
      val hasHcPerm = BioSyncLocalStore.load(appContext).healthConnectPermissionsGranted
      if (!hasActivityPerm && !hasHcPerm) {
        // Still refresh the 6-Pillar notification even if background health service cannot start
        BioSyncWidgetAndNotificationEngine.refreshLiveTelemetryNotificationIfActive(appContext)
        return
      }
      runCatching {
        val serviceIntent = Intent(appContext, BioSyncStepBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          appContext.startForegroundService(serviceIntent)
        } else {
          appContext.startService(serviceIntent)
        }
      }.onFailure {
        BioSyncWidgetAndNotificationEngine.refreshLiveTelemetryNotificationIfActive(appContext)
      }
    }

    fun stop(context: Context) {
      val appContext = context.applicationContext
      runCatching {
        val serviceIntent = Intent(appContext, BioSyncStepBackgroundService::class.java)
        appContext.stopService(serviceIntent)
      }
    }
  }
}

/**
 * Restores background step sensor, Health Connect synchronization, durable user database mirror,
 * Auto-Backup AlarmManager schedule, Home Screen widgets, and Circadian notifications automatically
 * after a device reboot or app version update.
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
      // Load both stores so self-healing from durable mirror runs immediately on app update
      val stored = BioSyncLocalStore.load(appContext)
      val dash = DashboardLocalStore.load(appContext)
      if (stored.protocolLockedIn || dash.dailyHistoryMap.isNotEmpty() || dash.todayTimelineEntries.isNotEmpty()) {
        BioSyncBackupManager.autoMirrorCurrentDatabase(appContext)
        BioSyncAutoBackupScheduler.scheduleOrCancelAutoBackupAlarm(appContext)
      }
      BioSyncWidgetAndNotificationEngine.refreshAllWidgets(appContext)
      BioSyncWidgetAndNotificationEngine.refreshLiveTelemetryNotificationIfActive(appContext)
      if (stored.protocolLockedIn && stored.dayOneInitialCountdownCompleted) {
        BioSyncCircadianHydrationEngine.scheduleCircadianNotifications(
          context = appContext,
          subjectName = stored.subjectName,
          wakeUpHour = stored.wakeUpHour,
          wakeUpMinute = stored.wakeUpMinute,
          sleepHour = stored.sleepHour,
          sleepMinute = stored.sleepMinute,
          totalDailyHydrationMl = stored.customHydrationMl,
          ageYears = stored.ageYears,
          selectedSex = stored.selectedSex,
          weightKg = stored.weightKg,
        )
      }
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
