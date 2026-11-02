package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Thread-safe mathematical ledger for counting today's steps live from hardware
 * Sensor.TYPE_STEP_COUNTER without opening Google Fit, with seamless catch-up
 * when Google Fit flushes to Health Connect.
 */
data class StepLedger(
  val dayKey: String = "",
  val bootCount: Int = -1,
  val baselineRaw: Float = -1f,
  val carry: Int = 0,
  val lastRaw: Float = -1f,
  val lastRawAtMs: Long = 0L,
  val offset: Int = 0,
)

object StepLedgerMath {
  fun sensorPart(l: StepLedger): Int {
    if (l.lastRaw < 0f || l.baselineRaw < 0f) return l.carry
    val delta = max(0f, l.lastRaw - l.baselineRaw).roundToInt()
    return delta + l.carry
  }

  fun total(l: StepLedger): Int = sensorPart(l) + l.offset

  fun onRaw(
    l: StepLedger,
    raw: Float,
    nowMs: Long,
    today: String,
    startOfTodayMs: Long,
    boot: Int,
    bootTimeMs: Long,
  ): StepLedger {
    if (raw <= 0f) return l
    val freshBaseline = if (bootTimeMs >= startOfTodayMs) 0f else raw
    return when {
      l.dayKey.isEmpty() -> {
        StepLedger(
          dayKey = today,
          bootCount = boot,
          baselineRaw = raw,
          carry = 0,
          lastRaw = raw,
          lastRawAtMs = nowMs,
          offset = 0,
        )
      }
      l.dayKey != today -> {
        val sameBoot = (l.bootCount == boot && boot != -1)
        val nearMidnight = (startOfTodayMs - l.lastRawAtMs) in 0..3_600_000L
        val base = if (sameBoot && nearMidnight && l.lastRaw > 0f) {
          l.lastRaw
        } else if (sameBoot && l.lastRaw > 0f) {
          raw
        } else {
          freshBaseline
        }
        StepLedger(
          dayKey = today,
          bootCount = boot,
          baselineRaw = base,
          carry = 0,
          lastRaw = raw,
          lastRawAtMs = nowMs,
          offset = 0,
        )
      }
      l.bootCount != boot && boot != -1 && l.bootCount != -1 -> {
        l.copy(
          carry = sensorPart(l),
          bootCount = boot,
          baselineRaw = freshBaseline,
          lastRaw = raw,
          lastRawAtMs = nowMs,
        )
      }
      raw < l.lastRaw -> {
        if (l.lastRaw - raw > 50f) {
          l.copy(
            carry = sensorPart(l),
            baselineRaw = 0f,
            lastRaw = raw,
            lastRawAtMs = nowMs,
          )
        } else {
          l
        }
      }
      else -> {
        l.copy(
          lastRaw = raw,
          lastRawAtMs = nowMs,
        )
      }
    }
  }

  fun onFitTotal(l: StepLedger, fitSteps: Int): StepLedger {
    if (fitSteps <= 0) return l
    val s = sensorPart(l)
    return if (fitSteps > s + l.offset) {
      l.copy(offset = fitSteps - s)
    } else {
      l
    }
  }
}

/**
 * Device-local storage snapshot containing all user profile, anthropometric,
 * blueprint, climate, occupation, and custom target override parameters.
 * Stored strictly on the user's device via private SharedPreferences.
 */
data class BioSyncStoredState(
  // Step 01: Profile
  val subjectName: String = "Elena Rostova",
  val selectedSex: String = "FEMALE",
  val birthDay: Int = 24,
  val birthMonthZeroBased: Int = 9,
  val birthYear: Int = 1994,
  val ageYears: Int = 31,

  // Step 02: Personal Details
  val heightCm: Float = 172.0f,
  val heightUnit: HeightUnit = HeightUnit.CM,
  val weightKg: Float = 62.0f,
  val weightUnit: WeightUnit = WeightUnit.KG,

  // Step 03: Blueprint & Lifestyle
  val activityLevel: ActivityLevelOption = ActivityLevelOption.SEDENTARY,
  val occupationLoad: OccupationLoadOption = OccupationLoadOption.DESK,
  val objective: BlueprintObjectiveOption = BlueprintObjectiveOption.MAINTAIN,
  val trainingModality: TrainingModalityOption = TrainingModalityOption.MIXED,
  val workoutDuration: WorkoutDurationOption = WorkoutDurationOption.HR_1_0,
  val experienceLevel: ExperienceLevelOption = ExperienceLevelOption.INTERMEDIATE,
  val targetPace: TargetPaceOption = TargetPaceOption.NORMAL,
  val dayOne: DayOfWeekOption = DayOfWeekOption.MONDAY,
  val selectedWorkoutDays: Set<DayOfWeekOption> = DailyTargetsEngine.defaultWorkoutDaysFor(ActivityLevelOption.SEDENTARY),
  val mealFrequency: MealFrequencyOption = MealFrequencyOption.MEALS_3,
  val caffeineAlcoholYes: Boolean = false,
  val sodiumFrequency: SodiumFrequencyOption = SodiumFrequencyOption.RARELY,
  val streakThreshold: StreakThresholdOption = StreakThresholdOption.PERCENT_90,

  // Step 04: Climate, Optional Body-Fat %, and Custom Target Overrides
  val climateBand: ClimateBandOption = ClimateBandOption.defaultForCurrentDate(),
  val bodyFatOverrideEnabled: Boolean = false,
  val bodyFatPercent: Float = 18.0f,
  val isCustomTargetsMode: Boolean = false,
  val customCaloriesKcal: Int = 2000,
  val customProteinGrams: Int = 130,
  val customCarbsGrams: Int = 220,
  val customFatsGrams: Int = 60,
  val customHydrationMl: Int = 3000,
  val customStepsCount: Int = 8500,
  val protocolLockedIn: Boolean = false,

  // Dashboard Anchor & Live Schedule Persistence (one-time initial Day 1 countdown & live active/rest state)
  // Circadian Wake-Up & Sleep Schedule (Step 03)
  val wakeUpHour: Int = 7,
  val wakeUpMinute: Int = 0,
  val sleepHour: Int = 23,
  val sleepMinute: Int = 0,

  val protocolLockedAtEpochMillis: Long = 0L,
  val dayOneAnchorStartEpochMillis: Long = 0L,
  val dayOneInitialCountdownCompleted: Boolean = false,
  val dayOneStartNotificationSent: Boolean = false,

  // Internal Health Connect & Google Fit Live Sync Persistence (Steps & Distance only)
  val decidedDailyStepsGoal: Int = 8500,
  val todaySyncedSteps: Int = 0,
  val todayDistanceMeters: Float = 0f,
  val lastStepSyncEpochMillis: Long = 0L,
  val healthConnectPermissionsGranted: Boolean = false,
  val activityRecognitionGranted: Boolean = false,
  val stepSensorDayKey: String = "",
  val stepSensorBaselineCounter: Float = -1f,
  val stepSensorLastRawCounter: Float = -1f,
  val lastHealthConnectWrittenSteps: Int = 0,
  val lastHcRecordEndEpochMillis: Long = 0L,
)

object BioSyncLocalStore {
  private const val PREFS_NAME = "biosync_on_device_protocol_v2"

  private const val KEY_SUBJECT_NAME = "subject_name"
  private const val KEY_SELECTED_SEX = "selected_sex"
  private const val KEY_BIRTH_DAY = "birth_day"
  private const val KEY_BIRTH_MONTH = "birth_month"
  private const val KEY_BIRTH_YEAR = "birth_year"
  private const val KEY_AGE_YEARS = "age_years"

  private const val KEY_HEIGHT_CM = "height_cm"
  private const val KEY_HEIGHT_UNIT = "height_unit"
  private const val KEY_WEIGHT_KG = "weight_kg"
  private const val KEY_WEIGHT_UNIT = "weight_unit"

  private const val KEY_ACTIVITY_LEVEL = "activity_level"
  private const val KEY_OCCUPATION_LOAD = "occupation_load"
  private const val KEY_OBJECTIVE = "objective"
  private const val KEY_TRAINING_MODALITY = "training_modality"
  private const val KEY_WORKOUT_DURATION = "workout_duration"
  private const val KEY_EXPERIENCE_LEVEL = "experience_level"
  private const val KEY_TARGET_PACE = "target_pace"
  private const val KEY_DAY_ONE = "day_one"
  private const val KEY_SELECTED_WORKOUT_DAYS = "selected_workout_days"
  private const val KEY_MEAL_FREQUENCY = "meal_frequency"
  private const val KEY_CAFFEINE_ALCOHOL = "caffeine_alcohol"
  private const val KEY_SODIUM_FREQUENCY = "sodium_frequency"
  private const val KEY_STREAK_THRESHOLD = "streak_threshold"
  private const val KEY_WAKE_UP_HOUR = "wake_up_hour"
  private const val KEY_WAKE_UP_MINUTE = "wake_up_minute"
  private const val KEY_SLEEP_HOUR = "sleep_hour"
  private const val KEY_SLEEP_MINUTE = "sleep_minute"

  private const val KEY_CLIMATE_BAND = "climate_band"
  private const val KEY_BF_OVERRIDE_ENABLED = "bf_override_enabled"
  private const val KEY_BF_PERCENT = "bf_percent"
  private const val KEY_IS_CUSTOM_MODE = "is_custom_mode"
  private const val KEY_CUSTOM_CALORIES = "custom_calories"
  private const val KEY_CUSTOM_PROTEIN = "custom_protein"
  private const val KEY_CUSTOM_CARBS = "custom_carbs"
  private const val KEY_CUSTOM_FATS = "custom_fats"
  private const val KEY_CUSTOM_HYDRATION = "custom_hydration"
  private const val KEY_CUSTOM_STEPS = "custom_steps"
  private const val KEY_PROTOCOL_LOCKED = "protocol_locked"
  private const val KEY_PROTOCOL_LOCKED_AT_EPOCH_MS = "protocol_locked_at_epoch_ms"
  private const val KEY_DAY_ONE_ANCHOR_START_EPOCH_MS = "day_one_anchor_start_epoch_ms"
  private const val KEY_DAY_ONE_INITIAL_COUNTDOWN_COMPLETED = "day_one_initial_countdown_completed"
  private const val KEY_DAY_ONE_START_NOTIFICATION_SENT = "day_one_start_notification_sent"

  private const val KEY_DECIDED_DAILY_STEPS_GOAL = "decided_daily_steps_goal"
  private const val KEY_TODAY_SYNCED_STEPS = "today_synced_steps_v5_strict_fit"
  private const val KEY_TODAY_DISTANCE_METERS = "today_distance_meters_v5_strict_fit"
  private const val KEY_LAST_STEP_SYNC_EPOCH_MS = "last_step_sync_epoch_ms_v5_strict_fit"
  private const val KEY_HEALTH_CONNECT_GRANTED = "health_connect_permissions_granted"
  private const val KEY_ACTIVITY_RECOGNITION_GRANTED = "activity_recognition_granted"
  private const val KEY_STEP_SENSOR_DAY_KEY = "step_sensor_day_key_v5_strict_fit"
  private const val KEY_STEP_SENSOR_BASELINE = "step_sensor_baseline_counter_v5"
  private const val KEY_STEP_SENSOR_LAST_RAW = "step_sensor_last_raw_counter_v5"
  private const val KEY_LAST_HC_WRITTEN_STEPS = "last_hc_written_steps_v5"
  private const val KEY_LAST_HC_RECORD_END_EPOCH_MS = "last_hc_record_end_epoch_ms_v5"


  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Returns true if [dayOne] matches the current local calendar day of the week at [nowEpochMillis].
   * For example, if the user selected Thursday and today is Thursday, this returns true so the
   * Dashboard starts automatically without any countdown timer.
   */
  fun isSameWeekdayAsToday(
    dayOne: DayOfWeekOption,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = nowEpochMillis
    }
    val targetCalendarDay = when (dayOne) {
      DayOfWeekOption.MONDAY -> java.util.Calendar.MONDAY
      DayOfWeekOption.TUESDAY -> java.util.Calendar.TUESDAY
      DayOfWeekOption.WEDNESDAY -> java.util.Calendar.WEDNESDAY
      DayOfWeekOption.THURSDAY -> java.util.Calendar.THURSDAY
      DayOfWeekOption.FRIDAY -> java.util.Calendar.FRIDAY
      DayOfWeekOption.SATURDAY -> java.util.Calendar.SATURDAY
      DayOfWeekOption.SUNDAY -> java.util.Calendar.SUNDAY
    }
    return cal.get(java.util.Calendar.DAY_OF_WEEK) == targetCalendarDay
  }

  /**
   * Computes the epoch timestamp for the one-time initial Day 1 Anchor launch:
   * - If [dayOne] is the SAME weekday as today (e.g., user selected Thursday and today is Thursday),
   *   returns [fromEpochMillis] immediately so the Dashboard starts automatically with zero timer.
   * - Otherwise, returns local midnight (00:00:00.000) of the next upcoming [dayOne].
   */
  fun computeNextDayOneAnchorEpochMillis(
    dayOne: DayOfWeekOption,
    fromEpochMillis: Long = System.currentTimeMillis(),
  ): Long {
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = fromEpochMillis
    }
    val targetCalendarDay = when (dayOne) {
      DayOfWeekOption.MONDAY -> java.util.Calendar.MONDAY
      DayOfWeekOption.TUESDAY -> java.util.Calendar.TUESDAY
      DayOfWeekOption.WEDNESDAY -> java.util.Calendar.WEDNESDAY
      DayOfWeekOption.THURSDAY -> java.util.Calendar.THURSDAY
      DayOfWeekOption.FRIDAY -> java.util.Calendar.FRIDAY
      DayOfWeekOption.SATURDAY -> java.util.Calendar.SATURDAY
      DayOfWeekOption.SUNDAY -> java.util.Calendar.SUNDAY
    }
    val currentCalendarDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val daysUntilAnchor = (targetCalendarDay - currentCalendarDay + 7) % 7
    if (daysUntilAnchor == 0) {
      // Selected Day 1 Anchor is today — start immediately without any timer!
      return fromEpochMillis
    }
    cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntilAnchor)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  /**
   * Returns true if the one-time Day 1 Anchor countdown has completed (or started immediately)
   * and the full Dashboard should be live.
   */
  fun isDashboardLive(
    state: BioSyncStoredState,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    if (!state.protocolLockedIn) return false
    if (state.dayOneInitialCountdownCompleted) return true
    return state.dayOneAnchorStartEpochMillis > 0L &&
      nowEpochMillis >= state.dayOneAnchorStartEpochMillis
  }

  fun hasSentDayOneStartNotification(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_DAY_ONE_START_NOTIFICATION_SENT, false)
  }

  fun markDayOneCountdownCompletedAndNotified(context: Context) {
    prefs(context).edit().apply {
      putBoolean(KEY_DAY_ONE_INITIAL_COUNTDOWN_COMPLETED, true)
      putBoolean(KEY_DAY_ONE_START_NOTIFICATION_SENT, true)
      apply()
    }
  }

  /**
   * Resolves whether the given timestamp falls on an Active workout day or a Rest day
   * according to the persisted activity level and selected workout days.
   */
  fun isActiveWorkoutDay(
    state: BioSyncStoredState,
    atEpochMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    if (state.activityLevel == ActivityLevelOption.SEDENTARY) return false
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = atEpochMillis
    }
    val todayOption = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.MONDAY -> DayOfWeekOption.MONDAY
      java.util.Calendar.TUESDAY -> DayOfWeekOption.TUESDAY
      java.util.Calendar.WEDNESDAY -> DayOfWeekOption.WEDNESDAY
      java.util.Calendar.THURSDAY -> DayOfWeekOption.THURSDAY
      java.util.Calendar.FRIDAY -> DayOfWeekOption.FRIDAY
      java.util.Calendar.SATURDAY -> DayOfWeekOption.SATURDAY
      else -> DayOfWeekOption.SUNDAY
    }
    return todayOption in state.selectedWorkoutDays
  }

  @Volatile
  private var isRestoringMirrorGuard = false

  private fun migrateLegacyPrefsIfNeeded(context: Context, targetPrefs: SharedPreferences) {
    if (targetPrefs.contains(KEY_PROTOCOL_LOCKED) || targetPrefs.contains(KEY_SUBJECT_NAME)) return
    val legacyNames = listOf(
      "biosync_on_device_protocol_v1",
      "biosync_on_device_protocol",
      "biosync_user_profile_v1",
    )
    for (legacyName in legacyNames) {
      val legacy = context.applicationContext.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
      val allEntries = legacy.all
      if (allEntries.isNotEmpty()) {
        val editor = targetPrefs.edit()
        for ((k, v) in allEntries) {
          when (v) {
            is String -> editor.putString(k, v)
            is Int -> editor.putInt(k, v)
            is Long -> editor.putLong(k, v)
            is Float -> editor.putFloat(k, v)
            is Boolean -> editor.putBoolean(k, v)
          }
        }
        editor.commit()
        break
      }
    }
  }

  fun load(context: Context): BioSyncStoredState {
    val p = prefs(context)
    migrateLegacyPrefsIfNeeded(context, p)

    // Self-heal from durable mirror file / Recovery Vault ONLY if primary prefs were wiped during an APK update
    if (!isRestoringMirrorGuard && !p.contains(KEY_PROTOCOL_LOCKED) && !p.contains(KEY_SUBJECT_NAME)) {
      isRestoringMirrorGuard = true
      try {
        BioSyncBackupManager.restoreFromAutoMirrorIfPrimaryMissing(context)
      } finally {
        isRestoringMirrorGuard = false
      }
    }

    val defaults = BioSyncStoredState()
    val loadedActivity = enumOrDefault(p.getString(KEY_ACTIVITY_LEVEL, null), defaults.activityLevel)
    val rawDaysCsv = p.getString(KEY_SELECTED_WORKOUT_DAYS, null)
    val parsedWorkoutDays = if (loadedActivity == ActivityLevelOption.SEDENTARY) {
      emptySet()
    } else if (rawDaysCsv == null) {
      DailyTargetsEngine.defaultWorkoutDaysFor(loadedActivity)
    } else if (rawDaysCsv.isBlank()) {
      emptySet()
    } else {
      rawDaysCsv.split(",")
        .mapNotNull { token -> runCatching { DayOfWeekOption.valueOf(token.trim()) }.getOrNull() }
        .toSet()
    }
    val loadedDayOne = enumOrDefault(p.getString(KEY_DAY_ONE, null), defaults.dayOne)
    val hasExistingDashboardHistory = runCatching {
      val dashPrefs = context.applicationContext.getSharedPreferences("biosync_dashboard_telemetry_v1", Context.MODE_PRIVATE)
      val hist = dashPrefs.getString("dashboard_daily_history_map_json", "{}") ?: "{}"
      val quick = dashPrefs.getString("dashboard_saved_quick_items_json", "[]") ?: "[]"
      val tl = dashPrefs.getString("dashboard_timeline_entries_json", "[]") ?: "[]"
      hist.length > 4 || quick.length > 4 || tl.length > 4
    }.getOrDefault(false)

    val lockedIn = if (p.contains(KEY_PROTOCOL_LOCKED)) {
      p.getBoolean(KEY_PROTOCOL_LOCKED, defaults.protocolLockedIn)
    } else {
      hasExistingDashboardHistory
    }
    val lockedAtMs = p.getLong(KEY_PROTOCOL_LOCKED_AT_EPOCH_MS, defaults.protocolLockedAtEpochMillis)
    val storedAnchorMs = p.getLong(KEY_DAY_ONE_ANCHOR_START_EPOCH_MS, defaults.dayOneAnchorStartEpochMillis)
    val effectiveAnchorMs = if (lockedIn && storedAnchorMs <= 0L) {
      computeNextDayOneAnchorEpochMillis(loadedDayOne)
    } else {
      storedAnchorMs
    }
    val storedCompleted = p.getBoolean(KEY_DAY_ONE_INITIAL_COUNTDOWN_COMPLETED, false) || hasExistingDashboardHistory
    val effectiveCompleted = storedCompleted || (lockedIn && effectiveAnchorMs > 0L && System.currentTimeMillis() >= effectiveAnchorMs)
    val storedNotified = p.getBoolean(KEY_DAY_ONE_START_NOTIFICATION_SENT, false) || hasExistingDashboardHistory
    return BioSyncStoredState(
      subjectName = p.getString(KEY_SUBJECT_NAME, defaults.subjectName) ?: defaults.subjectName,
      selectedSex = p.getString(KEY_SELECTED_SEX, defaults.selectedSex) ?: defaults.selectedSex,
      birthDay = p.getInt(KEY_BIRTH_DAY, defaults.birthDay),
      birthMonthZeroBased = p.getInt(KEY_BIRTH_MONTH, defaults.birthMonthZeroBased),
      birthYear = p.getInt(KEY_BIRTH_YEAR, defaults.birthYear),
      ageYears = p.getInt(KEY_AGE_YEARS, defaults.ageYears),
      heightCm = p.getFloat(KEY_HEIGHT_CM, defaults.heightCm),
      heightUnit = enumOrDefault(p.getString(KEY_HEIGHT_UNIT, null), defaults.heightUnit),
      weightKg = p.getFloat(KEY_WEIGHT_KG, defaults.weightKg),
      weightUnit = enumOrDefault(p.getString(KEY_WEIGHT_UNIT, null), defaults.weightUnit),
      activityLevel = loadedActivity,
      occupationLoad = enumOrDefault(p.getString(KEY_OCCUPATION_LOAD, null), defaults.occupationLoad),
      objective = enumOrDefault(p.getString(KEY_OBJECTIVE, null), defaults.objective),
      trainingModality = enumOrDefault(p.getString(KEY_TRAINING_MODALITY, null), defaults.trainingModality),
      workoutDuration = enumOrDefault(p.getString(KEY_WORKOUT_DURATION, null), defaults.workoutDuration),
      experienceLevel = enumOrDefault(p.getString(KEY_EXPERIENCE_LEVEL, null), defaults.experienceLevel),
      targetPace = enumOrDefault(p.getString(KEY_TARGET_PACE, null), defaults.targetPace),
      dayOne = loadedDayOne,
      selectedWorkoutDays = parsedWorkoutDays,
      mealFrequency = enumOrDefault(p.getString(KEY_MEAL_FREQUENCY, null), defaults.mealFrequency),
      caffeineAlcoholYes = p.getBoolean(KEY_CAFFEINE_ALCOHOL, false),
      sodiumFrequency = enumOrDefault(p.getString(KEY_SODIUM_FREQUENCY, null), defaults.sodiumFrequency),
      streakThreshold = enumOrDefault(p.getString(KEY_STREAK_THRESHOLD, null), defaults.streakThreshold),
      climateBand = enumOrDefault(p.getString(KEY_CLIMATE_BAND, null), defaults.climateBand),
      bodyFatOverrideEnabled = p.getBoolean(KEY_BF_OVERRIDE_ENABLED, false),
      bodyFatPercent = p.getFloat(KEY_BF_PERCENT, defaults.bodyFatPercent),
      isCustomTargetsMode = p.getBoolean(KEY_IS_CUSTOM_MODE, defaults.isCustomTargetsMode),
      customCaloriesKcal = p.getInt(KEY_CUSTOM_CALORIES, defaults.customCaloriesKcal),
      customProteinGrams = p.getInt(KEY_CUSTOM_PROTEIN, defaults.customProteinGrams),
      customCarbsGrams = p.getInt(KEY_CUSTOM_CARBS, defaults.customCarbsGrams),
      customFatsGrams = p.getInt(KEY_CUSTOM_FATS, defaults.customFatsGrams),
      customHydrationMl = p.getInt(KEY_CUSTOM_HYDRATION, defaults.customHydrationMl),
      customStepsCount = p.getInt(KEY_CUSTOM_STEPS, defaults.customStepsCount),
      protocolLockedIn = lockedIn,
      wakeUpHour = p.getInt(KEY_WAKE_UP_HOUR, defaults.wakeUpHour).coerceIn(0, 23),
      wakeUpMinute = p.getInt(KEY_WAKE_UP_MINUTE, defaults.wakeUpMinute).coerceIn(0, 59),
      sleepHour = p.getInt(KEY_SLEEP_HOUR, defaults.sleepHour).coerceIn(0, 23),
      sleepMinute = p.getInt(KEY_SLEEP_MINUTE, defaults.sleepMinute).coerceIn(0, 59),
      protocolLockedAtEpochMillis = lockedAtMs,
      dayOneAnchorStartEpochMillis = effectiveAnchorMs,
      dayOneInitialCountdownCompleted = effectiveCompleted,
      dayOneStartNotificationSent = storedNotified,
      decidedDailyStepsGoal = p.getInt(KEY_DECIDED_DAILY_STEPS_GOAL, defaults.decidedDailyStepsGoal),
      todaySyncedSteps = p.getInt(KEY_TODAY_SYNCED_STEPS, defaults.todaySyncedSteps),
      todayDistanceMeters = p.getFloat(KEY_TODAY_DISTANCE_METERS, defaults.todayDistanceMeters),
      lastStepSyncEpochMillis = p.getLong(KEY_LAST_STEP_SYNC_EPOCH_MS, defaults.lastStepSyncEpochMillis),
      healthConnectPermissionsGranted = p.getBoolean(KEY_HEALTH_CONNECT_GRANTED, defaults.healthConnectPermissionsGranted),
      activityRecognitionGranted = p.getBoolean(KEY_ACTIVITY_RECOGNITION_GRANTED, defaults.activityRecognitionGranted),
      stepSensorDayKey = p.getString(KEY_STEP_SENSOR_DAY_KEY, defaults.stepSensorDayKey) ?: defaults.stepSensorDayKey,
      stepSensorBaselineCounter = p.getFloat(KEY_STEP_SENSOR_BASELINE, defaults.stepSensorBaselineCounter),
      stepSensorLastRawCounter = p.getFloat(KEY_STEP_SENSOR_LAST_RAW, defaults.stepSensorLastRawCounter),
      lastHealthConnectWrittenSteps = p.getInt(KEY_LAST_HC_WRITTEN_STEPS, defaults.lastHealthConnectWrittenSteps),
      lastHcRecordEndEpochMillis = p.getLong(KEY_LAST_HC_RECORD_END_EPOCH_MS, defaults.lastHcRecordEndEpochMillis),
    )
  }

  fun save(context: Context, state: BioSyncStoredState, mirrorToVault: Boolean = true) {
    prefs(context).edit().apply {
      putString(KEY_SUBJECT_NAME, state.subjectName)
      putString(KEY_SELECTED_SEX, state.selectedSex)
      putInt(KEY_BIRTH_DAY, state.birthDay)
      putInt(KEY_BIRTH_MONTH, state.birthMonthZeroBased)
      putInt(KEY_BIRTH_YEAR, state.birthYear)
      putInt(KEY_AGE_YEARS, state.ageYears)

      putFloat(KEY_HEIGHT_CM, state.heightCm)
      putString(KEY_HEIGHT_UNIT, state.heightUnit.name)
      putFloat(KEY_WEIGHT_KG, state.weightKg)
      putString(KEY_WEIGHT_UNIT, state.weightUnit.name)

      putString(KEY_ACTIVITY_LEVEL, state.activityLevel.name)
      putString(KEY_OCCUPATION_LOAD, state.occupationLoad.name)
      putString(KEY_OBJECTIVE, state.objective.name)
      putString(KEY_TRAINING_MODALITY, state.trainingModality.name)
      putString(KEY_WORKOUT_DURATION, state.workoutDuration.name)
      putString(KEY_EXPERIENCE_LEVEL, state.experienceLevel.name)
      putString(KEY_TARGET_PACE, state.targetPace.name)
      putString(KEY_DAY_ONE, state.dayOne.name)
      putString(
        KEY_SELECTED_WORKOUT_DAYS,
        state.selectedWorkoutDays
          .sortedBy { it.dayNumber }
          .joinToString(",") { it.name },
      )
      putString(KEY_MEAL_FREQUENCY, state.mealFrequency.name)
      putBoolean(KEY_CAFFEINE_ALCOHOL, state.caffeineAlcoholYes)
      putString(KEY_SODIUM_FREQUENCY, state.sodiumFrequency.name)
      putString(KEY_STREAK_THRESHOLD, state.streakThreshold.name)
      putInt(KEY_WAKE_UP_HOUR, state.wakeUpHour.coerceIn(0, 23))
      putInt(KEY_WAKE_UP_MINUTE, state.wakeUpMinute.coerceIn(0, 59))
      putInt(KEY_SLEEP_HOUR, state.sleepHour.coerceIn(0, 23))
      putInt(KEY_SLEEP_MINUTE, state.sleepMinute.coerceIn(0, 59))

      putString(KEY_CLIMATE_BAND, state.climateBand.name)
      putBoolean(KEY_BF_OVERRIDE_ENABLED, state.bodyFatOverrideEnabled)
      putFloat(KEY_BF_PERCENT, state.bodyFatPercent)
      putBoolean(KEY_IS_CUSTOM_MODE, state.isCustomTargetsMode)
      putInt(KEY_CUSTOM_CALORIES, state.customCaloriesKcal)
      putInt(KEY_CUSTOM_PROTEIN, state.customProteinGrams)
      putInt(KEY_CUSTOM_CARBS, state.customCarbsGrams)
      putInt(KEY_CUSTOM_FATS, state.customFatsGrams)
      putInt(KEY_CUSTOM_HYDRATION, state.customHydrationMl)
      putInt(KEY_CUSTOM_STEPS, state.customStepsCount)
      putBoolean(KEY_PROTOCOL_LOCKED, state.protocolLockedIn)
      putLong(KEY_PROTOCOL_LOCKED_AT_EPOCH_MS, state.protocolLockedAtEpochMillis)
      putLong(KEY_DAY_ONE_ANCHOR_START_EPOCH_MS, state.dayOneAnchorStartEpochMillis)
      putBoolean(KEY_DAY_ONE_INITIAL_COUNTDOWN_COMPLETED, state.dayOneInitialCountdownCompleted)
      putBoolean(KEY_DAY_ONE_START_NOTIFICATION_SENT, state.dayOneStartNotificationSent)
      putInt(KEY_DECIDED_DAILY_STEPS_GOAL, state.decidedDailyStepsGoal)
      apply()
    }
    if (mirrorToVault && state.protocolLockedIn && !isRestoringMirrorGuard) {
      BioSyncBackupManager.autoMirrorCurrentDatabase(context)
    }
    BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
  }

  /**
   * Updates only the live Google Fit & Health Connect telemetry fields
   * (Total Steps and Distance) in SharedPreferences without overwriting user profile settings.
   */
  fun saveStepTelemetrySnapshot(
    context: Context,
    decidedDailyStepsGoal: Int? = null,
    todaySyncedSteps: Int? = null,
    todayDistanceMeters: Float? = null,
    lastStepSyncEpochMillis: Long? = null,
    healthConnectPermissionsGranted: Boolean? = null,
    activityRecognitionGranted: Boolean? = null,
    stepSensorDayKey: String? = null,
    stepSensorBaselineCounter: Float? = null,
    stepSensorLastRawCounter: Float? = null,
    lastHealthConnectWrittenSteps: Int? = null,
    lastHcRecordEndEpochMillis: Long? = null,
  ) {
    prefs(context).edit().apply {
      decidedDailyStepsGoal?.let { putInt(KEY_DECIDED_DAILY_STEPS_GOAL, it) }
      todaySyncedSteps?.let { putInt(KEY_TODAY_SYNCED_STEPS, it) }
      todayDistanceMeters?.let { putFloat(KEY_TODAY_DISTANCE_METERS, it) }
      lastStepSyncEpochMillis?.let { putLong(KEY_LAST_STEP_SYNC_EPOCH_MS, it) }
      healthConnectPermissionsGranted?.let { putBoolean(KEY_HEALTH_CONNECT_GRANTED, it) }
      activityRecognitionGranted?.let { putBoolean(KEY_ACTIVITY_RECOGNITION_GRANTED, it) }
      stepSensorDayKey?.let { putString(KEY_STEP_SENSOR_DAY_KEY, it) }
      stepSensorBaselineCounter?.let { putFloat(KEY_STEP_SENSOR_BASELINE, it) }
      stepSensorLastRawCounter?.let { putFloat(KEY_STEP_SENSOR_LAST_RAW, it) }
      lastHealthConnectWrittenSteps?.let { putInt(KEY_LAST_HC_WRITTEN_STEPS, it) }
      lastHcRecordEndEpochMillis?.let { putLong(KEY_LAST_HC_RECORD_END_EPOCH_MS, it) }
      apply()
    }
  }

  fun saveTodaySteps(context: Context, steps: Int) {
    saveStepTelemetrySnapshot(context = context, todaySyncedSteps = steps)
  }

  fun getTodaySteps(context: Context): Int {
    return prefs(context).getInt(KEY_TODAY_SYNCED_STEPS, 0)
  }

  private const val KEY_LEDGER_DAY_KEY = "ledger_day_key"
  private const val KEY_LEDGER_BOOT_COUNT = "ledger_boot_count"
  private const val KEY_LEDGER_BASELINE_RAW = "ledger_baseline_raw"
  private const val KEY_LEDGER_CARRY = "ledger_carry"
  private const val KEY_LEDGER_LAST_RAW = "ledger_last_raw"
  private const val KEY_LEDGER_LAST_RAW_AT_MS = "ledger_last_raw_at_ms"
  private const val KEY_LEDGER_OFFSET = "ledger_offset"

  fun saveLedger(context: Context, ledger: StepLedger) {
    prefs(context).edit().apply {
      putString(KEY_LEDGER_DAY_KEY, ledger.dayKey)
      putInt(KEY_LEDGER_BOOT_COUNT, ledger.bootCount)
      putFloat(KEY_LEDGER_BASELINE_RAW, ledger.baselineRaw)
      putInt(KEY_LEDGER_CARRY, ledger.carry)
      putFloat(KEY_LEDGER_LAST_RAW, ledger.lastRaw)
      putLong(KEY_LEDGER_LAST_RAW_AT_MS, ledger.lastRawAtMs)
      putInt(KEY_LEDGER_OFFSET, ledger.offset)
      putInt(KEY_TODAY_SYNCED_STEPS, StepLedgerMath.total(ledger))
      putString(KEY_STEP_SENSOR_DAY_KEY, ledger.dayKey)
      apply()
    }
  }

  fun loadLedger(context: Context): StepLedger {
    val p = prefs(context)
    return StepLedger(
      dayKey = p.getString(KEY_LEDGER_DAY_KEY, "") ?: "",
      bootCount = p.getInt(KEY_LEDGER_BOOT_COUNT, -1),
      baselineRaw = p.getFloat(KEY_LEDGER_BASELINE_RAW, -1f),
      carry = p.getInt(KEY_LEDGER_CARRY, 0),
      lastRaw = p.getFloat(KEY_LEDGER_LAST_RAW, -1f),
      lastRawAtMs = p.getLong(KEY_LEDGER_LAST_RAW_AT_MS, 0L),
      offset = p.getInt(KEY_LEDGER_OFFSET, 0),
    )
  }

  /**
   * Generates the automatic 2-character Subject ID Badge from the Full Name:
   * Uses the first letter of the 1st word and the first letter of the 2nd word.
   */
  fun computeSubjectIdBadge(subjectName: String): String {
    val parts = subjectName
      .filter { it.isLetter() || it == ' ' }
      .trim()
      .split("\\s+".toRegex())
      .filter { it.isNotEmpty() }
    return when {
      parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
      parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
      parts.size == 1 -> "${parts[0].first().uppercaseChar()}•"
      else -> "ID"
    }
  }

  /**
   * Returns true if a backup was imported (so profile/dashboard data exists in SharedPreferences)
   * while `protocol_locked` is explicitly false awaiting Step 04 mandatory permission lock-in.
   */
  fun hasRestoredDataPendingLockIn(context: Context): Boolean {
    val p = prefs(context)
    if (!p.contains(KEY_PROTOCOL_LOCKED)) return false
    if (p.getBoolean(KEY_PROTOCOL_LOCKED, false)) return false
    val hasHistory = runCatching {
      val dashPrefs = context.applicationContext.getSharedPreferences("biosync_dashboard_telemetry_v1", Context.MODE_PRIVATE)
      val hist = dashPrefs.getString("dashboard_daily_history_map_json", "{}") ?: "{}"
      val quick = dashPrefs.getString("dashboard_saved_quick_items_json", "[]") ?: "[]"
      val tl = dashPrefs.getString("dashboard_timeline_entries_json", "[]") ?: "[]"
      hist.length > 4 || quick.length > 4 || tl.length > 4
    }.getOrDefault(false)
    return hasHistory || p.getLong(KEY_PROTOCOL_LOCKED_AT_EPOCH_MS, 0L) > 0L
  }

  /**
   * Factory Reset: Wipes all persisted protocol, profile, anthropometric, blueprint,
   * and step ledger data from on-device storage.
   */
  fun clearAll(context: Context) {
    prefs(context).edit().clear().commit()
  }

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return try {
      enumValueOf<T>(raw)
    } catch (_: IllegalArgumentException) {
      default
    }
  }
}
