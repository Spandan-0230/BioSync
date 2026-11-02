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
    val snap = BioSyncWidgetAndNotificationEngine.collectSnapshot(appContext)
    val bannerBmp = BioSyncWidgetAndNotificationEngine.renderWidgetTelemetryBitmap(
      snapshot = snap,
      widthDp = 360f,
      heightDp = 195f,
      density = 2.4f,
    )
    val water250Pending = PendingIntent.getBroadcast(
      appContext,
      9111,
      Intent(appContext, BioSyncWidgetActionReceiver::class.java).apply {
        action = BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_WATER_250
      },
      pendingFlags,
    )
    val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_widget_flame)
      .setColor(0xFFFFB703.toInt())
      .setContentTitle("🔥 Protocol Day 01 Is Live • ${dayOne.fullName}")
      .setContentText("$cleanName, your Day 01 countdown reached 00:00:00. All 6 pillars are unlocked!")
      .setSubText("BIOSYNC LAUNCH GATE")
      .setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(bannerBmp)
          .setBigContentTitle("🔥 Protocol Day 01 Is Live • ${dayOne.fullName}")
          .setSummaryText("$cleanName, your BioSync 6-pillar telemetry HUD is now unlocked."),
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setWhen(System.currentTimeMillis())
      .setShowWhen(true)
      .setContentIntent(contentPendingIntent)
      .addAction(R.drawable.ic_widget_plus, "+250ml H₂O", water250Pending)
      .addAction(R.drawable.ic_widget_pulse, "Open HUD", contentPendingIntent)
      .build()

    runCatching {
      NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }
    BioSyncWidgetAndNotificationEngine.refreshAllWidgets(appContext)
    return true
  }

  /**
   * Schedules a one-time exact AlarmManager broadcast at [triggerAtEpochMillis] so that if the user
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
      val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
      } else {
        true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canExact) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtEpochMillis,
          pendingIntent,
        )
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtEpochMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setExact(
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

/**
 * Scientific Circadian Hydration & Sleep/Wake Schedule Plan.
 * Computes waking window, Good Night (-15 min before sleep), and hourly water intake
 * capped physiologically by age, sex, and body weight so a single hourly serving never
 * exceeds safe renal/gastric absorption capacity.
 */
data class BioSyncCircadianHydrationPlan(
  val wakeUpHour: Int,
  val wakeUpMinute: Int,
  val sleepHour: Int,
  val sleepMinute: Int,
  val goodNightHour: Int,
  val goodNightMinute: Int,
  val wakingHoursCount: Int,
  val totalDailyHydrationMl: Int,
  val rawHourlyMl: Int,
  val physiologicalMaxHourlyMl: Int,
  val recommendedHourlyMl: Int,
  val recommendedSipEvery30MinMl: Int,
  val isCappedByPhysiology: Boolean,
  val safetyCapReason: String,
)

object BioSyncCircadianHydrationEngine {
  private const val CHANNEL_ID = "biosync_circadian_hydration_channel"
  private const val CHANNEL_NAME = "BioSync Circadian & Hydration Protocol"
  private const val REQ_GOOD_MORNING = 9301
  private const val REQ_GOOD_NIGHT = 9302
  private const val REQ_HOURLY_WATER_BASE = 9400

  const val EXTRA_EVENT_TYPE = "biosync_circadian_event_type"
  const val EVENT_GOOD_MORNING = "GOOD_MORNING"
  const val EVENT_GOOD_NIGHT = "GOOD_NIGHT"
  const val EVENT_HOURLY_WATER = "HOURLY_WATER"
  const val EXTRA_HOUR_INDEX = "biosync_hour_index"
  const val EXTRA_TOTAL_HOURS = "biosync_total_hours"
  const val EXTRA_RECOMMENDED_ML = "biosync_recommended_ml"
  const val EXTRA_MAX_SAFE_ML = "biosync_max_safe_ml"

  fun formatTime12h(hour24: Int, minute: Int): String {
    val h = ((hour24 % 24) + 24) % 24
    val m = minute.coerceIn(0, 59)
    val amPm = if (h < 12) "AM" else "PM"
    val h12 = when {
      h == 0 -> 12
      h > 12 -> h - 12
      else -> h
    }
    return String.format(java.util.Locale.US, "%02d:%02d %s", h12, m, amPm)
  }

  fun formatTime24h(hour24: Int, minute: Int): String {
    val h = ((hour24 % 24) + 24) % 24
    val m = minute.coerceIn(0, 59)
    return String.format(java.util.Locale.US, "%02d:%02d", h, m)
  }

  /**
   * Computes the scientific hourly hydration schedule from wake-up until sleep,
   * applying physiological absorption caps according to age, biological sex, and weight.
   *
   * Scientific basis (ACSM / NASEM / Renal & Gastric Emptying Kinetics):
   * - Maximum comfortable hourly fluid absorption at rest/daily living is ~8.5 to 10.0 mL/kg/hr
   *   (with an absolute ceiling of 650–750 mL/hr to prevent hyponatremia / bladder urgency).
   * - Age modulation: youth (<18) and older adults (55+) have tighter single-bolus renal comfort caps.
   * - Sex modulation: accounts for lean-mass intracellular water distribution differences.
   */
  fun computePlan(
    wakeUpHour: Int,
    wakeUpMinute: Int,
    sleepHour: Int,
    sleepMinute: Int,
    totalDailyHydrationMl: Int,
    ageYears: Int,
    selectedSex: String,
    weightKg: Float,
  ): BioSyncCircadianHydrationPlan {
    val safeWakeH = wakeUpHour.coerceIn(0, 23)
    val safeWakeM = wakeUpMinute.coerceIn(0, 59)
    val safeSleepH = sleepHour.coerceIn(0, 23)
    val safeSleepM = sleepMinute.coerceIn(0, 59)

    val wakeTotalMins = safeWakeH * 60 + safeWakeM
    val sleepTotalMins = safeSleepH * 60 + safeSleepM
    val rawAwakeMinutes = if (sleepTotalMins > wakeTotalMins) {
      sleepTotalMins - wakeTotalMins
    } else {
      (24 * 60 - wakeTotalMins) + sleepTotalMins
    }
    // Clamp waking window to realistic 8..20 hours (default 16h if identical)
    val effectiveAwakeMinutes = if (rawAwakeMinutes in 240..1260) rawAwakeMinutes else 16 * 60
    // Stop heavy water intake 15 mins before sleep so sleep quality isn't disrupted
    val hydrationWindowMinutes = (effectiveAwakeMinutes - 15).coerceAtLeast(240)
    val wakingHoursCount = kotlin.math.max(6, kotlin.math.round(hydrationWindowMinutes / 60f).toInt())

    // Good night time = 15 minutes before sleep time
    val goodNightTotalMins = ((sleepTotalMins - 15) + 24 * 60) % (24 * 60)
    val goodNightHour = goodNightTotalMins / 60
    val goodNightMinute = goodNightTotalMins % 60

    val safeWeight = weightKg.coerceIn(35f, 180f)
    val safeAge = ageYears.coerceIn(13, 95)
    val isMale = selectedSex.uppercase().contains("MALE") && !selectedSex.uppercase().contains("FEMALE")

    // Physiological hourly comfortable absorption rate (mL per kg per hour)
    val sexRatePerKg = if (isMale) 9.2f else 8.4f
    val ageFactor = when {
      safeAge < 18 -> 0.88f
      safeAge <= 35 -> 1.00f
      safeAge <= 55 -> 0.94f
      else -> 0.85f
    }
    val absoluteCeilingMl = when {
      safeAge < 18 -> 500
      isMale -> 650
      else -> 560
    }
    val absoluteFloorMl = 180

    val computedMaxSafe = ((safeWeight * sexRatePerKg * ageFactor) / 10f).toInt() * 10
    val physiologicalMaxHourlyMl = computedMaxSafe.coerceIn(absoluteFloorMl, absoluteCeilingMl)

    val safeTotalMl = totalDailyHydrationMl.coerceIn(1500, 7500)
    val rawHourlyMl = ((safeTotalMl.toFloat() / wakingHoursCount.toFloat()) / 10f).toInt() * 10
    val isCapped = rawHourlyMl > physiologicalMaxHourlyMl
    val recommendedHourlyMl = rawHourlyMl.coerceIn(140, physiologicalMaxHourlyMl)
    val recommendedSipEvery30MinMl = ((recommendedHourlyMl / 2f) / 5f).toInt() * 5

    val sexLabel = if (isMale) "Male" else "Female"
    val safetyCapReason = "Capped at ${physiologicalMaxHourlyMl} ml/hr for ${safeAge}y $sexLabel (${safeWeight.toInt()} kg) to match renal & gastric comfort."

    return BioSyncCircadianHydrationPlan(
      wakeUpHour = safeWakeH,
      wakeUpMinute = safeWakeM,
      sleepHour = safeSleepH,
      sleepMinute = safeSleepM,
      goodNightHour = goodNightHour,
      goodNightMinute = goodNightMinute,
      wakingHoursCount = wakingHoursCount,
      totalDailyHydrationMl = safeTotalMl,
      rawHourlyMl = rawHourlyMl.coerceAtLeast(140),
      physiologicalMaxHourlyMl = physiologicalMaxHourlyMl,
      recommendedHourlyMl = recommendedHourlyMl,
      recommendedSipEvery30MinMl = recommendedSipEvery30MinMl.coerceAtLeast(70),
      isCappedByPhysiology = isCapped,
      safetyCapReason = safetyCapReason,
    )
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Good Morning wake-up greeting, Good Night wind-down (-15m), and hourly scientific water intake prompts."
          enableVibration(true)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  /**
   * Schedules the user's Good Morning notification (at wakeUpTime),
   * Good Night notification (15 minutes before sleepTime),
   * and hourly hydration notifications across the waking hours.
   */
  fun scheduleCircadianNotifications(
    context: Context,
    subjectName: String,
    wakeUpHour: Int,
    wakeUpMinute: Int,
    sleepHour: Int,
    sleepMinute: Int,
    totalDailyHydrationMl: Int,
    ageYears: Int,
    selectedSex: String,
    weightKg: Float,
  ) {
    val appContext = context.applicationContext
    ensureChannel(appContext)
    cancelAllCircadianAlarms(appContext)

    val plan = computePlan(
      wakeUpHour = wakeUpHour,
      wakeUpMinute = wakeUpMinute,
      sleepHour = sleepHour,
      sleepMinute = sleepMinute,
      totalDailyHydrationMl = totalDailyHydrationMl,
      ageYears = ageYears,
      selectedSex = selectedSex,
      weightKg = weightKg,
    )

    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val now = System.currentTimeMillis()

    // 1. Schedule Good Morning at wakeUpHour:wakeUpMinute
    val morningTrigger = nextOccurrenceEpochMillis(plan.wakeUpHour, plan.wakeUpMinute, now)
    scheduleSingleAlarm(
      context = appContext,
      alarmManager = alarmManager,
      requestCode = REQ_GOOD_MORNING,
      triggerAtMillis = morningTrigger,
      eventType = EVENT_GOOD_MORNING,
      hourIndex = 0,
      totalHours = plan.wakingHoursCount,
      recommendedMl = plan.recommendedHourlyMl,
      maxSafeMl = plan.physiologicalMaxHourlyMl,
    )

    // 2. Schedule Good Night 15 mins before sleepTime
    val nightTrigger = nextOccurrenceEpochMillis(plan.goodNightHour, plan.goodNightMinute, now)
    scheduleSingleAlarm(
      context = appContext,
      alarmManager = alarmManager,
      requestCode = REQ_GOOD_NIGHT,
      triggerAtMillis = nightTrigger,
      eventType = EVENT_GOOD_NIGHT,
      hourIndex = plan.wakingHoursCount,
      totalHours = plan.wakingHoursCount,
      recommendedMl = plan.recommendedHourlyMl,
      maxSafeMl = plan.physiologicalMaxHourlyMl,
    )

    // 3. Schedule Hourly Water reminders for each waking hour (1 .. wakingHoursCount)
    for (hourIdx in 1..plan.wakingHoursCount.coerceAtMost(18)) {
      val totalMinsAfterMidnight = (plan.wakeUpHour * 60 + plan.wakeUpMinute + hourIdx * 60) % (24 * 60)
      val h = totalMinsAfterMidnight / 60
      val m = totalMinsAfterMidnight % 60
      val triggerAt = nextOccurrenceEpochMillis(h, m, now)
      scheduleSingleAlarm(
        context = appContext,
        alarmManager = alarmManager,
        requestCode = REQ_HOURLY_WATER_BASE + hourIdx,
        triggerAtMillis = triggerAt,
        eventType = EVENT_HOURLY_WATER,
        hourIndex = hourIdx,
        totalHours = plan.wakingHoursCount,
        recommendedMl = plan.recommendedHourlyMl,
        maxSafeMl = plan.physiologicalMaxHourlyMl,
      )
    }
  }

  private fun nextOccurrenceEpochMillis(hour24: Int, minute: Int, nowMillis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(java.util.Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
      set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= nowMillis + 5_000L) {
      cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
  }

  private fun scheduleSingleAlarm(
    context: Context,
    alarmManager: AlarmManager,
    requestCode: Int,
    triggerAtMillis: Long,
    eventType: String,
    hourIndex: Int,
    totalHours: Int,
    recommendedMl: Int,
    maxSafeMl: Int,
  ) {
    val intent = Intent(context, BioSyncCircadianAlarmReceiver::class.java).apply {
      putExtra(EXTRA_EVENT_TYPE, eventType)
      putExtra(EXTRA_HOUR_INDEX, hourIndex)
      putExtra(EXTRA_TOTAL_HOURS, totalHours)
      putExtra(EXTRA_RECOMMENDED_ML, recommendedMl)
      putExtra(EXTRA_MAX_SAFE_ML, maxSafeMl)
    }
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags)
    runCatching {
      val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
      } else {
        true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canExact) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    }
  }

  fun cancelAllCircadianAlarms(context: Context) {
    val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val codes = mutableListOf(REQ_GOOD_MORNING, REQ_GOOD_NIGHT)
    for (i in 1..20) codes.add(REQ_HOURLY_WATER_BASE + i)
    val pendingFlags = PendingIntent.FLAG_NO_CREATE or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    codes.forEach { code ->
      val intent = Intent(appContext, BioSyncCircadianAlarmReceiver::class.java)
      val pi = PendingIntent.getBroadcast(appContext, code, intent, pendingFlags)
      if (pi != null) {
        runCatching {
          alarmManager.cancel(pi)
          pi.cancel()
        }
      }
    }
  }

  fun fireCircadianNotification(
    context: Context,
    eventType: String,
    hourIndex: Int,
    totalHours: Int,
    recommendedMl: Int,
    maxSafeMl: Int,
  ) {
    val appContext = context.applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    ensureChannel(appContext)
    val stored = BioSyncLocalStore.load(appContext)
    val cleanName = stored.subjectName.ifBlank { "Athlete" }
    val snap = BioSyncWidgetAndNotificationEngine.collectSnapshot(appContext)
    val bannerBmp = BioSyncWidgetAndNotificationEngine.renderWidgetTelemetryBitmap(
      snapshot = snap,
      widthDp = 360f,
      heightDp = 195f,
      density = 2.4f,
    )

    val launchIntent = Intent(appContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    val contentIntent = PendingIntent.getActivity(
      appContext,
      eventType.hashCode() + hourIndex,
      launchIntent,
      pendingFlags,
    )
    val water250Pending = PendingIntent.getBroadcast(
      appContext,
      9600 + hourIndex,
      Intent(appContext, BioSyncWidgetActionReceiver::class.java).apply {
        action = BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_WATER_250
      },
      pendingFlags,
    )
    val water500Pending = PendingIntent.getBroadcast(
      appContext,
      9700 + hourIndex,
      Intent(appContext, BioSyncWidgetActionReceiver::class.java).apply {
        action = BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_WATER_500
      },
      pendingFlags,
    )

    val (title, shortBody, bigBody, notifId) = when (eventType) {
      EVENT_GOOD_MORNING -> {
        val wakeBolus = (recommendedMl + 60).coerceAtMost(maxSafeMl)
        Quadruple(
          "☀️ Good Morning, $cleanName • ${snap.streakDays}d Streak Active",
          "Prime metabolism with $wakeBolus ml H₂O • Daily Readiness ${snap.overallReadinessPercent}%.",
          "Good Morning, $cleanName! Your ${totalHours}h waking telemetry cycle has begun. Drink $wakeBolus ml of water now (capped at $maxSafeMl ml/hr for your physiology).",
          REQ_GOOD_MORNING,
        )
      }
      EVENT_GOOD_NIGHT -> {
        Quadruple(
          "🌙 Recovery Lock in 15m, $cleanName • ${snap.overallReadinessPercent}% Ready",
          "Sleep window starts in 15 mins. Take a light 80–120 ml sip only if thirsty.",
          "Good Night, $cleanName! Your sleep recovery window begins in 15 minutes (${formatTime12h(stored.sleepHour, stored.sleepMinute)}). Today's readiness: ${snap.overallReadinessPercent}%.",
          REQ_GOOD_NIGHT,
        )
      }
      else -> {
        val sipHalf = (recommendedMl / 2).coerceAtLeast(75)
        Quadruple(
          "💧 Hour $hourIndex/$totalHours Hydration • +$recommendedMl ml H₂O",
          "Logged ${snap.loggedWaterMl}/${snap.targetWaterMl} ml • Drink $recommendedMl ml (~$sipHalf ml/30m).",
          "$cleanName, stay on pace with your hydration target (${snap.loggedWaterMl}/${snap.targetWaterMl} ml logged): drink $recommendedMl ml this hour (capped at $maxSafeMl ml/hr).",
          REQ_HOURLY_WATER_BASE + hourIndex,
        )
      }
    }

    val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_widget_pulse)
      .setColor(0xFF06B6D4.toInt())
      .setContentTitle(title)
      .setContentText(shortBody)
      .setSubText("BIOSYNC CIRCADIAN HUD")
      .setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(bannerBmp)
          .setBigContentTitle(title)
          .setSummaryText(bigBody),
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setWhen(System.currentTimeMillis())
      .setShowWhen(true)
      .setContentIntent(contentIntent)
      .addAction(R.drawable.ic_widget_plus, "+250ml H₂O", water250Pending)
      .addAction(R.drawable.ic_widget_plus, "+500ml H₂O", water500Pending)
      .addAction(R.drawable.ic_widget_flame, "Open HUD", contentIntent)
      .build()

    runCatching {
      NotificationManagerCompat.from(appContext).notify(notifId, notification)
    }

    // Re-arm for the next 24h cycle
    scheduleCircadianNotifications(
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

  private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

class BioSyncCircadianAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val eventType = intent?.getStringExtra(BioSyncCircadianHydrationEngine.EXTRA_EVENT_TYPE)
      ?: BioSyncCircadianHydrationEngine.EVENT_HOURLY_WATER
    val hourIndex = intent?.getIntExtra(BioSyncCircadianHydrationEngine.EXTRA_HOUR_INDEX, 1) ?: 1
    val totalHours = intent?.getIntExtra(BioSyncCircadianHydrationEngine.EXTRA_TOTAL_HOURS, 16) ?: 16
    val recommendedMl = intent?.getIntExtra(BioSyncCircadianHydrationEngine.EXTRA_RECOMMENDED_ML, 250) ?: 250
    val maxSafeMl = intent?.getIntExtra(BioSyncCircadianHydrationEngine.EXTRA_MAX_SAFE_ML, 550) ?: 550
    BioSyncCircadianHydrationEngine.fireCircadianNotification(
      context = context,
      eventType = eventType,
      hourIndex = hourIndex,
      totalHours = totalHours,
      recommendedMl = recommendedMl,
      maxSafeMl = maxSafeMl,
    )
  }
}
