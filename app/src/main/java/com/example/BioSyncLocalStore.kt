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
  val activityLevel: ActivityLevelOption = ActivityLevelOption.MODERATE,
  val occupationLoad: OccupationLoadOption = OccupationLoadOption.DESK,
  val objective: BlueprintObjectiveOption = BlueprintObjectiveOption.MAINTAIN,
  val trainingModality: TrainingModalityOption = TrainingModalityOption.MIXED,
  val workoutDuration: WorkoutDurationOption = WorkoutDurationOption.HR_1_0,
  val experienceLevel: ExperienceLevelOption = ExperienceLevelOption.INTERMEDIATE,
  val targetPace: TargetPaceOption = TargetPaceOption.NORMAL,
  val dayOne: DayOfWeekOption = DayOfWeekOption.MONDAY,
  val scheduleCadence: ScheduleCadenceOption = ScheduleCadenceOption.UNIFORM_DAILY,
  val selectedWorkoutDays: Set<DayOfWeekOption> = DailyTargetsEngine.defaultWorkoutDaysFor(ActivityLevelOption.MODERATE),
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
  private const val KEY_SCHEDULE_CADENCE = "schedule_cadence"
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

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun load(context: Context): BioSyncStoredState {
    val p = prefs(context)
    val defaults = BioSyncStoredState()
    val loadedActivity = enumOrDefault(p.getString(KEY_ACTIVITY_LEVEL, null), defaults.activityLevel)
    val rawDaysCsv = p.getString(KEY_SELECTED_WORKOUT_DAYS, null)
    val parsedWorkoutDays = if (rawDaysCsv == null) {
      DailyTargetsEngine.defaultWorkoutDaysFor(loadedActivity)
    } else if (rawDaysCsv.isBlank()) {
      emptySet()
    } else {
      rawDaysCsv.split(",")
        .mapNotNull { token -> runCatching { DayOfWeekOption.valueOf(token.trim()) }.getOrNull() }
        .toSet()
    }
    val rawScheduleCadence = enumOrDefault(p.getString(KEY_SCHEDULE_CADENCE, null), defaults.scheduleCadence)
    val effectiveCadence = if (loadedActivity == ActivityLevelOption.SEDENTARY) {
      ScheduleCadenceOption.UNIFORM_DAILY
    } else {
      rawScheduleCadence
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
      dayOne = enumOrDefault(p.getString(KEY_DAY_ONE, null), defaults.dayOne),
      scheduleCadence = effectiveCadence,
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
      protocolLockedIn = p.getBoolean(KEY_PROTOCOL_LOCKED, defaults.protocolLockedIn),
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
      putString(KEY_SCHEDULE_CADENCE, state.scheduleCadence.name)
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
