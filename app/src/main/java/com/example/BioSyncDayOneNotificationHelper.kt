package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Strictly one-time Day 01 Anchor launch notification & alarm scheduler.
 * - Fires ONLY when the user's very first Day 1 Anchor countdown reaches 00:00:00.
 * - Atomically marks `dayOneInitialCountdownCompleted` and `dayOneStartNotificationSent` in
 *   `BioSyncLocalStore` so it can never trigger again at any future time.
 */
object BioSyncDayOneNotificationHelper {
  private const val CHANNEL_ID = "biosync_day_one_launch_channel"
  private const val CHANNEL_NAME = "BioSync Day 01 Protocol Launch"
  private const val NOTIFICATION_ID = 9101
  private const val ALARM_REQUEST_CODE = 9102

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "One-time alert when your initial Day 01 Protocol Anchor countdown reaches zero."
          enableVibration(true)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  /**
   * Fires the one-time Day 01 launch notification if and only if it has not been sent yet.
   * Immediately records `dayOneInitialCountdownCompleted = true` and `dayOneStartNotificationSent = true`.
   */
  @Synchronized
  fun fireDayOneStartedNotificationOnce(
    context: Context,
    subjectName: String,
    dayOne: DayOfWeekOption,
  ): Boolean {
    val appContext = context.applicationContext
    if (BioSyncLocalStore.hasSentDayOneStartNotification(appContext)) {
      BioSyncLocalStore.markDayOneCountdownCompletedAndNotified(appContext)
      return false
    }

    // Mark completed and notified immediately so neither foreground nor background alarm can ever repeat it
    BioSyncLocalStore.markDayOneCountdownCompletedAndNotified(appContext)
    cancelOneTimeDayOneAlarm(appContext)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasPermission = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!hasPermission) {
        return false
      }
    }

    ensureChannel(appContext)

    val launchIntent = Intent(appContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val contentPendingIntent = PendingIntent.getActivity(
      appContext,
      NOTIFICATION_ID,
      launchIntent,
      pendingFlags,
    )

    val cleanName = subjectName.ifBlank { "Subject" }
    val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Protocol Day 01 Is Live • ${dayOne.fullName}")
      .setContentText("$cleanName, your Day 01 countdown reached 00:00:00. Your BioSync Dashboard is now unlocked.")
      .setStyle(
        NotificationCompat.BigTextStyle().bigText(
          "$cleanName, your ${dayOne.fullName} Day 01 Anchor has arrived. Your daily steps, calories, macros, and hydration dashboard is now live.",
        ),
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .setContentIntent(contentPendingIntent)
      .build()

    runCatching {
      NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }
    return true
  }

  /**
   * Schedules a one-time AlarmManager broadcast at [triggerAtEpochMillis] so that if the user
   * leaves the app while waiting for their Day 1 Anchor date, the notification still triggers
   * at 00:00:00 when the timer reaches 0.
   */
  fun scheduleOneTimeDayOneAlarm(
    context: Context,
    triggerAtEpochMillis: Long,
  ) {
    val appContext = context.applicationContext
    if (BioSyncLocalStore.hasSentDayOneStartNotification(appContext)) return
    if (triggerAtEpochMillis <= System.currentTimeMillis()) return

    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(appContext, BioSyncDayOneAlarmReceiver::class.java)
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val pendingIntent = PendingIntent.getBroadcast(
      appContext,
      ALARM_REQUEST_CODE,
      intent,
      pendingFlags,
    )

    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtEpochMillis,
          pendingIntent,
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          triggerAtEpochMillis,
          pendingIntent,
        )
      }
    }
  }

  fun cancelOneTimeDayOneAlarm(context: Context) {
    val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(appContext, BioSyncDayOneAlarmReceiver::class.java)
    val pendingFlags = PendingIntent.FLAG_NO_CREATE or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val pendingIntent = PendingIntent.getBroadcast(
      appContext,
      ALARM_REQUEST_CODE,
      intent,
      pendingFlags,
    )
    if (pendingIntent != null) {
      runCatching {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
      }
    }
  }
}

/**
 * BroadcastReceiver that fires strictly once when the initial Day 01 Anchor countdown reaches 0.
 */
class BioSyncDayOneAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val stored = BioSyncLocalStore.load(context)
    if (!stored.protocolLockedIn || stored.dayOneStartNotificationSent) return
    BioSyncDayOneNotificationHelper.fireDayOneStartedNotificationOnce(
      context = context,
      subjectName = stored.subjectName,
      dayOne = stored.dayOne,
    )
  }
}
