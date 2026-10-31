package com.example

import android.content.Context
import android.content.SharedPreferences

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

  // Dashboard Anchor & Live Schedule Persistence (for upcoming Dashboard countdown & live active/rest state)
  val protocolLockedAtEpochMillis: Long = 0L,
  val dayOneAnchorStartEpochMillis: Long = 0L,

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


  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Computes the epoch timestamp (local midnight 00:00:00.000) of the next occurrence of [dayOne].
   * Used so the Dashboard can display a countdown timer until Day 1 Anchor arrives,
   * and then go live according to the user's selected Active/Rest days.
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
    var daysUntilAnchor = (targetCalendarDay - currentCalendarDay + 7) % 7
    if (daysUntilAnchor == 0) {
      // If locked in after midnight on the same weekday, count down to the next Day 1 anchor cycle
      // unless it's within the very start of the day; advancing to next midnight keeps countdown deterministic.
      daysUntilAnchor = 7
    }
    cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntilAnchor)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  /**
   * Returns true if the Day 1 Anchor countdown has completed and the Dashboard should be live.
   */
  fun isDashboardLive(
    state: BioSyncStoredState,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    return state.protocolLockedIn &&
      state.dayOneAnchorStartEpochMillis > 0L &&
      nowEpochMillis >= state.dayOneAnchorStartEpochMillis
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

  fun load(context: Context): BioSyncStoredState {
    val p = prefs(context)
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
    val lockedIn = p.getBoolean(KEY_PROTOCOL_LOCKED, defaults.protocolLockedIn)
    val lockedAtMs = p.getLong(KEY_PROTOCOL_LOCKED_AT_EPOCH_MS, defaults.protocolLockedAtEpochMillis)
    val storedAnchorMs = p.getLong(KEY_DAY_ONE_ANCHOR_START_EPOCH_MS, defaults.dayOneAnchorStartEpochMillis)
    val effectiveAnchorMs = if (lockedIn && storedAnchorMs <= 0L) {
      computeNextDayOneAnchorEpochMillis(loadedDayOne)
    } else {
      storedAnchorMs
    }
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
      protocolLockedAtEpochMillis = lockedAtMs,
      dayOneAnchorStartEpochMillis = effectiveAnchorMs,
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
    )
  }

  fun save(context: Context, state: BioSyncStoredState) {
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
      putInt(KEY_DECIDED_DAILY_STEPS_GOAL, state.decidedDailyStepsGoal)
      apply()
    }
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
      apply()
    }
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
