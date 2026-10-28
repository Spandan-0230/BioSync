package com.example

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Occupation Load Add-On to PAL & Hydration.
 */
enum class OccupationLoadOption(
  val title: String,
  val subtitle: String,
  val palAddOn: Float,
  val waterAddMl: Int,
  val badgeCode: String,
  val accentColor: Color,
) {
  DESK(
    title = "Desk / Seated",
    subtitle = "Office, study, remote",
    palAddOn = 0.00f,
    waterAddMl = 0,
    badgeCode = "+0.00 PAL",
    accentColor = Color(0xFF7BE3FF),
  ),
  LIGHT_STANDING(
    title = "Light Standing",
    subtitle = "Teaching, retail, lab",
    palAddOn = 0.05f,
    waterAddMl = 150,
    badgeCode = "+0.05 PAL",
    accentColor = Color(0xFF34D399),
  ),
  ACTIVE_ON_FEET(
    title = "Active on Feet",
    subtitle = "Hospitality, nursing, floor",
    palAddOn = 0.10f,
    waterAddMl = 300,
    badgeCode = "+0.10 PAL",
    accentColor = Color(0xFFFBBF24),
  ),
  HEAVY_MANUAL(
    title = "Heavy Manual",
    subtitle = "Construction, outdoor, field",
    palAddOn = 0.15f,
    waterAddMl = 500,
    badgeCode = "+0.15 PAL",
    accentColor = Color(0xFFFF7A29),
  ),
}

/**
 * Ambient Climate / Temperature Band.
 * Automatically derived from the device calendar (will sync with device weather).
 */
enum class ClimateBandOption(
  val tempRangeLabel: String,
  val seasonName: String,
  val baselineWaterAddMl: Int,
  val sweatRateMultiplier: Float,
  val representativeTempC: Int,
  val accentColor: Color,
) {
  COOL_MILD(
    tempRangeLabel = "< 25°C",
    seasonName = "Cool / Winter",
    baselineWaterAddMl = 0,
    sweatRateMultiplier = 1.00f,
    representativeTempC = 22,
    accentColor = Color(0xFF7BE3FF),
  ),
  WARM(
    tempRangeLabel = "25–29.9°C",
    seasonName = "Warm / Monsoon",
    baselineWaterAddMl = 250,
    sweatRateMultiplier = 1.00f,
    representativeTempC = 28,
    accentColor = Color(0xFF34D399),
  ),
  HOT(
    tempRangeLabel = "30–34.9°C",
    seasonName = "Hot / Post-Monsoon",
    baselineWaterAddMl = 400,
    sweatRateMultiplier = 1.25f,
    representativeTempC = 32,
    accentColor = Color(0xFFFBBF24),
  ),
  VERY_HOT(
    tempRangeLabel = "35–39.9°C",
    seasonName = "Peak Summer",
    baselineWaterAddMl = 650,
    sweatRateMultiplier = 1.25f,
    representativeTempC = 37,
    accentColor = Color(0xFFFF7A29),
  ),
  EXTREME_HEAT(
    tempRangeLabel = "≥ 40°C",
    seasonName = "Heatwave",
    baselineWaterAddMl = 900,
    sweatRateMultiplier = 1.25f,
    representativeTempC = 41,
    accentColor = Color(0xFFF43F5E),
  ),
  ;

  companion object {
    fun defaultForCurrentDate(): ClimateBandOption {
      val monthZeroBased = Calendar.getInstance().get(Calendar.MONTH)
      return when (monthZeroBased) {
        Calendar.DECEMBER, Calendar.JANUARY -> COOL_MILD
        Calendar.FEBRUARY, Calendar.JULY, Calendar.AUGUST, Calendar.NOVEMBER -> WARM
        Calendar.MARCH, Calendar.SEPTEMBER, Calendar.OCTOBER -> HOT
        Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> VERY_HOT
        else -> HOT
      }
    }
  }
}

/**
 * Which day profile is currently displayed when Workout / Rest Split is active.
 */
enum class SplitDayFocus {
  WORKOUT_DAY,
  REST_DAY,
}

/**
 * Safety Gate Status.
 */
enum class SafetyGateType {
  NONE,
  UNDER_13_PAEDIATRIC_NOTICE,
  TEEN_13_17_GROWTH_GUARD,
  UNDERWEIGHT_BMI_GUARD,
  CALORIE_FLOOR_APPLIED,
  DEFICIT_CEILING_APPLIED,
}

@Immutable
data class SafetyGateNotice(
  val type: SafetyGateType,
  val badgeLabel: String,
  val title: String,
  val explanation: String,
  val accentColor: Color,
)

/**
 * Out-of-Range Health Impact Severity & Alert Model.
 */
enum class HealthAlertSeverity(
  val badgeText: String,
  val accentColor: Color,
) {
  CRITICAL(
    badgeText = "HIGH IMPACT",
    accentColor = Color(0xFFF43F5E),
  ),
  WARNING(
    badgeText = "OUT OF RANGE",
    accentColor = Color(0xFFFF8E4D),
  ),
  CAUTION(
    badgeText = "NEEDS BALANCE",
    accentColor = Color(0xFFFBBF24),
  ),
}

@Immutable
data class HealthImpactAlert(
  val id: String,
  val metricName: String,
  val severity: HealthAlertSeverity,
  val userValueFormatted: String,
  val recommendedRangeFormatted: String,
  val headline: String,
  val physiologicalImpact: String,
  val realLifeExamples: String,
)

/**
 * User-editable target overrides on Step 04.
 */
@Immutable
data class CustomTargetsOverride(
  val isCustomMode: Boolean = false,
  val caloriesKcal: Int = 2000,
  val proteinGrams: Int = 130,
  val carbsGrams: Int = 220,
  val fatsGrams: Int = 60,
  val hydrationMl: Int = 3000,
  val stepsCount: Int = 8500,
)

/**
 * Complete output of the Mathematical Calculation Pipeline.
 */
@Immutable
data class DailyTargetsCalculationReport(
  // Safety Gates
  val isBlockedUnder13: Boolean,
  val effectiveObjective: BlueprintObjectiveOption,
  val objectiveWasOverridden: Boolean,
  val safetyNotices: List<SafetyGateNotice>,
  val calorieSafetyFloorKcal: Int,
  val calorieSafeCeilingKcal: Int,
  val maxSafeDeficitKcal: Int,

  // BMI & Effective Weight
  val bmi: Float,
  val bmiCategoryStandard: String,
  val bmiCategoryAsianWho: String,
  val actualWeightKg: Float,
  val referenceWeightKg: Float,
  val effectiveWeightKg: Float,
  val isEffectiveWeightAdjusted: Boolean,

  // BMR
  val bmrKcalExact: Float,
  val bmrKcalRounded: Int,
  val bmrDescriptor: String,
  val leanBodyMassKg: Float?,

  // PAL & TDEE
  val basePal: Float,
  val occupationPalAddOn: Float,
  val teenPalAddOn: Float,
  val totalActivityFactor: Float,
  val tdeeKcalExact: Float,
  val tdeeKcalRounded: Int,

  // Baseline Uniform Targets (7-day average)
  val uniformCaloriesKcal: Int,
  val uniformCarbsGrams: Int,
  val uniformSteps: Int,

  // Active Recommended Targets (Synced with Uniform vs Workout/Rest Split & active SplitDayFocus)
  val recommendedCaloriesKcal: Int,
  val calorieDeltaFromTdeeKcal: Int,
  val weeklyWeightRatePercent: Float,
  val estimatedWeeklyKgChange: Float,

  // Protein (Recommended)
  val recommendedProteinGrams: Int,
  val proteinFactorGPerKgEff: Float,
  val proteinFloorGrams: Int,
  val proteinCeilingGrams: Int,
  val proteinCalorieSharePct: Int,

  // Fat (Recommended)
  val recommendedFatsGrams: Int,
  val fatTargetSharePct: Int,
  val fatFloorGrams: Int,
  val fatCeilingGrams: Int,
  val fatCalorieSharePct: Int,

  // Carbohydrates & Fibre (Synced with active schedule split)
  val recommendedCarbsGrams: Int,
  val carbsFloorGrams: Int,
  val carbsCeilingGrams: Int,
  val carbsCalorieSharePct: Int,
  val isCarbsBelowBrainFloor: Boolean,
  val recommendedFibreGrams: Int,
  val maxFreeSugarGrams: Int,
  val maxSodiumMg: Int,

  // Daily Steps (Synced with active schedule split)
  val recommendedSteps: Int,
  val workoutDaySteps: Int,
  val restDaySteps: Int,
  val stepsFloor: Int,
  val stepsCeiling: Int,
  val strideLengthMeters: Float,
  val estimatedWalkingKm: Float,
  val informationalWalkingKcal: Int,

  // Hydration (ml/day, Synced with active schedule split)
  val baseWaterMl: Int,
  val climateWaterAddMl: Int,
  val occupationWaterAddMl: Int,
  val caffeineWaterAddMl: Int,
  val sodiumWaterAddMl: Int,
  val exerciseWaterAddOnWorkoutDayMl: Int,
  val restDayWaterMl: Int,
  val workoutDayWaterMl: Int,
  val uniformDayWaterMl: Int,
  val primaryRecommendedWaterMl: Int,
  val waterFloorMl: Int,
  val waterCeilingMl: Int,
  val waterGlasses250Ml: Float,
  val wakingHourlyReminderMl: Int,

  // Uniform vs Workout/Rest Day Split
  val trainingDaysPerWeek: Int,
  val restDaysPerWeek: Int,
  val selectedWorkoutDays: Set<DayOfWeekOption>,
  val selectedRestDays: Set<DayOfWeekOption>,
  val workoutDayCaloriesKcal: Int,
  val restDayCaloriesKcal: Int,
  val workoutDayCarbsGrams: Int,
  val restDayCarbsGrams: Int,
  val swingFractionPhi: Float,
  val activeSplitDayFocus: SplitDayFocus,

  // Meal Frequency Split (Synced with active calories & protein)
  val mealCount: Int,
  val mainMealsCount: Int,
  val snackMealsCount: Int,
  val caloriesPerMainMealKcal: Int,
  val caloriesPerSnackKcal: Int,
  val proteinPerMealGrams: Int,
  val carbsPerMealGrams: Int,
  val fatsPerMealGrams: Int,
  val proteinPerMealGPerKgEff: Float,
  val isProteinPerMealOptimalMps: Boolean,
)

/**
 * Pure scientific mathematical calculation engine implementing all 13 steps of
 * the Daily Targets Engine report.
 */
object DailyTargetsEngine {

  fun defaultWorkoutDaysFor(activityLevel: ActivityLevelOption): Set<DayOfWeekOption> =
    when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> emptySet()
      ActivityLevelOption.LIGHT -> setOf(
        DayOfWeekOption.MONDAY,
        DayOfWeekOption.WEDNESDAY,
        DayOfWeekOption.FRIDAY,
      )
      ActivityLevelOption.MODERATE -> setOf(
        DayOfWeekOption.MONDAY,
        DayOfWeekOption.TUESDAY,
        DayOfWeekOption.THURSDAY,
        DayOfWeekOption.FRIDAY,
        DayOfWeekOption.SATURDAY,
      )
      ActivityLevelOption.VERY_ACTIVE -> setOf(
        DayOfWeekOption.MONDAY,
        DayOfWeekOption.TUESDAY,
        DayOfWeekOption.WEDNESDAY,
        DayOfWeekOption.THURSDAY,
        DayOfWeekOption.FRIDAY,
        DayOfWeekOption.SATURDAY,
      )
    }

  private fun roundToNearest5(value: Float): Int =
    ((value / 5f).roundToInt() * 5).coerceAtLeast(0)

  private fun roundToNearest10(value: Float): Int =
    ((value / 10f).roundToInt() * 10).coerceAtLeast(0)

  private fun roundToNearest500(value: Float): Int =
    ((value / 500f).roundToInt() * 500).coerceAtLeast(0)

  fun calculate(
    ageYears: Int,
    biologicalSex: String,
    heightCm: Float,
    weightKg: Float,
    activityLevel: ActivityLevelOption,
    occupationLoad: OccupationLoadOption = OccupationLoadOption.DESK,
    objective: BlueprintObjectiveOption,
    trainingModality: TrainingModalityOption,
    workoutDuration: WorkoutDurationOption,
    experienceLevel: ExperienceLevelOption,
    targetPace: TargetPaceOption,
    scheduleCadence: ScheduleCadenceOption,
    mealFrequency: MealFrequencyOption,
    caffeineAlcoholYes: Boolean,
    sodiumFrequency: SodiumFrequencyOption,
    climateBand: ClimateBandOption = ClimateBandOption.defaultForCurrentDate(),
    bodyFatPercentOverride: Float? = null,
    splitDayFocus: SplitDayFocus = SplitDayFocus.WORKOUT_DAY,
    selectedWorkoutDays: Set<DayOfWeekOption> = emptySet(),
    highAltitude2500m: Boolean = false,
  ): DailyTargetsCalculationReport {
    val isMale = biologicalSex.equals("MALE", ignoreCase = true)
    val safeAge = ageYears.coerceIn(5, 110)
    val isUnder13 = safeAge < 13
    val isTeen = safeAge in 13..17
    val isSenior60Plus = safeAge >= 60
    val isSenior65Plus = safeAge >= 65

    val safeHeightCm = heightCm.coerceIn(100f, 250f)
    val heightMeters = safeHeightCm / 100f
    val safeWeightKg = weightKg.coerceIn(25f, 260f)

    // ── BMI & Effective Weight (W_eff) ──────────────────────────────────────
    val bmi = safeWeightKg / (heightMeters * heightMeters)
    val bmiCategoryStandard = when {
      bmi < 18.5f -> "Underweight"
      bmi < 25.0f -> "Normal Range"
      bmi < 30.0f -> "Overweight"
      else -> "High Mass"
    }
    val bmiCategoryAsianWho = when {
      bmi < 18.5f -> "Underweight"
      bmi < 23.0f -> "Optimal Asian"
      bmi < 27.5f -> "Elevated"
      else -> "High Risk"
    }

    val referenceWeightKg = 25f * heightMeters * heightMeters
    val hasValidBodyFat = bodyFatPercentOverride != null && bodyFatPercentOverride in 4f..60f
    val isEffectiveWeightAdjusted = !isTeen && !isUnder13 && !hasValidBodyFat && bmi > 27.0f
    val effectiveWeightKg = if (isEffectiveWeightAdjusted) {
      referenceWeightKg + 0.25f * (safeWeightKg - referenceWeightKg)
    } else {
      safeWeightKg
    }

    // ── Safety Gates ───────────────────────────────────────────────────────
    val safetyNotices = mutableListOf<SafetyGateNotice>()
    var effectiveObjective = objective
    var objectiveWasOverridden = false

    if (isUnder13) {
      effectiveObjective = BlueprintObjectiveOption.MAINTAIN
      objectiveWasOverridden = objective != BlueprintObjectiveOption.MAINTAIN
      safetyNotices.add(
        SafetyGateNotice(
          type = SafetyGateType.UNDER_13_PAEDIATRIC_NOTICE,
          badgeLabel = "YOUTH SAFETY (<13 YRS)",
          title = "Growth Protection Enabled",
          explanation = "Children under 13 need steady energy for healthy growth. Calorie cutting is turned off automatically.",
          accentColor = Color(0xFFF43F5E),
        )
      )
    } else if (isTeen) {
      if (objective == BlueprintObjectiveOption.LOSE_FAT) {
        effectiveObjective = BlueprintObjectiveOption.MAINTAIN
        objectiveWasOverridden = true
      }
      safetyNotices.add(
        SafetyGateNotice(
          type = SafetyGateType.TEEN_13_17_GROWTH_GUARD,
          badgeLabel = "TEEN GROWTH GUARD (13–17)",
          title = if (objective == BlueprintObjectiveOption.LOSE_FAT) {
            "Switched to Maintain for Healthy Teen Growth"
          } else {
            "Teen Growth Support Active"
          },
          explanation = "Extra growth energy, healthy fats, and daily movement are prioritized for ages 13–17 so bone and height development stay protected.",
          accentColor = Color(0xFF38BDF8),
        )
      )
    }

    if (bmi < 18.5f && effectiveObjective == BlueprintObjectiveOption.LOSE_FAT) {
      effectiveObjective = BlueprintObjectiveOption.MAINTAIN
      objectiveWasOverridden = true
      safetyNotices.add(
        SafetyGateNotice(
          type = SafetyGateType.UNDERWEIGHT_BMI_GUARD,
          badgeLabel = "UNDERWEIGHT SAFETY GUARD",
          title = "Fat-Loss Deficit Paused for Your Safety",
          explanation = "Your BMI (${String.format(Locale.US, "%.1f", bmi)}) is currently in the underweight range. Eating in a deficit right now would burn muscle and drain your energy, so your goal is set to Maintain.",
          accentColor = Color(0xFF38BDF8),
        )
      )
    }

    // ── BMR (kcal/day) ─────────────────────────────────────────────────────
    val leanBodyMassKg: Float?
    val bmrKcalExact: Float
    val bmrDescriptor: String

    if (hasValidBodyFat && bodyFatPercentOverride != null) {
      leanBodyMassKg = safeWeightKg * (1f - bodyFatPercentOverride / 100f)
      bmrKcalExact = 370f + 21.6f * leanBodyMassKg
      bmrDescriptor = "Lean Mass (${String.format(Locale.US, "%.1f", leanBodyMassKg)} kg LBM)"
    } else if (isTeen || isUnder13) {
      leanBodyMassKg = null
      bmrKcalExact = if (isMale) {
        17.5f * safeWeightKg + 651f
      } else {
        12.2f * safeWeightKg + 746f
      }
      bmrDescriptor = "Youth Resting Burn"
    } else {
      leanBodyMassKg = null
      bmrKcalExact = if (isMale) {
        10f * safeWeightKg + 6.25f * safeHeightCm - 5f * safeAge + 5f
      } else {
        10f * safeWeightKg + 6.25f * safeHeightCm - 5f * safeAge - 161f
      }
      bmrDescriptor = "Resting Organ & Cell Burn"
    }
    val bmrKcalRounded = bmrKcalExact.roundToInt()

    // ── Resolve Workout Days & Rest Days for Activity Tier ─────────────────
    val effectiveWorkoutDays: Set<DayOfWeekOption> = when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> emptySet()
      ActivityLevelOption.LIGHT -> {
        if (selectedWorkoutDays.isNotEmpty()) {
          selectedWorkoutDays
        } else {
          defaultWorkoutDaysFor(ActivityLevelOption.LIGHT) // Max 3 days (3 workout, 4 rest)
        }
      }
      ActivityLevelOption.MODERATE -> {
        if (selectedWorkoutDays.isNotEmpty()) {
          selectedWorkoutDays
        } else {
          defaultWorkoutDaysFor(ActivityLevelOption.MODERATE) // Max 5 days (5 workout, 2 rest)
        }
      }
      ActivityLevelOption.VERY_ACTIVE -> {
        if (selectedWorkoutDays.isNotEmpty()) {
          selectedWorkoutDays
        } else {
          defaultWorkoutDaysFor(ActivityLevelOption.VERY_ACTIVE)
        }
      }
    }
    val effectiveRestDays: Set<DayOfWeekOption> = DayOfWeekOption.entries.toSet() - effectiveWorkoutDays
    val trainingDaysPerWeek = effectiveWorkoutDays.size.coerceIn(0, 7)
    val restDaysPerWeek = (7 - trainingDaysPerWeek).coerceIn(0, 7)

    // ── TDEE (Total Daily Energy Expenditure) ──────────────────────────────
    val basePal = when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> 1.200f
      ActivityLevelOption.LIGHT -> when (trainingDaysPerWeek) {
        1 -> 1.325f
        2 -> 1.350f
        else -> 1.375f // Max 3 days/week
      }
      ActivityLevelOption.MODERATE -> when (trainingDaysPerWeek) {
        3 -> 1.490f
        4 -> 1.520f
        else -> 1.550f // Max 5 days/week
      }
      ActivityLevelOption.VERY_ACTIVE -> 1.725f
    }
    val occupationPalAddOn = occupationLoad.palAddOn
    val teenPalAddOn = if (isTeen || isUnder13) 0.10f else 0.00f
    val totalActivityFactor = (basePal + occupationPalAddOn + teenPalAddOn).coerceAtMost(2.00f)
    val tdeeKcalExact = bmrKcalExact * totalActivityFactor
    val tdeeKcalRounded = roundToNearest5(tdeeKcalExact)

    // ── Baseline Uniform Objective Calories ────────────────────────────────
    val minSexFloor = if (isMale) 1500f else 1200f
    val calorieSafetyFloorFloat = max(minSexFloor, 0.90f * bmrKcalExact)
    val calorieSafetyFloorKcal = roundToNearest5(calorieSafetyFloorFloat)
    val calorieSafeCeilingKcal = max(
      calorieSafetyFloorKcal + 400,
      roundToNearest5(tdeeKcalExact * 1.18f),
    )
    val maxSafeDeficitFloat = 0.25f * tdeeKcalExact
    val maxSafeDeficitKcal = roundToNearest5(maxSafeDeficitFloat)

    var weeklyWeightRatePercent = 0f
    var estimatedWeeklyKgChange = 0f

    val uniformCaloriesKcal: Int = when (effectiveObjective) {
      BlueprintObjectiveOption.MAINTAIN -> {
        weeklyWeightRatePercent = 0f
        estimatedWeeklyKgChange = 0f
        tdeeKcalRounded
      }

      BlueprintObjectiveOption.LOSE_FAT -> {
        val requestedRate = when (targetPace) {
          TargetPaceOption.SLOW -> 0.0025f
          TargetPaceOption.NORMAL -> 0.0050f
          TargetPaceOption.FAST -> 0.0075f
        }
        val expCap = when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 0.0100f
          ExperienceLevelOption.INTERMEDIATE -> 0.0075f
          ExperienceLevelOption.ADVANCED -> 0.0050f
        }
        val bmiCap = when {
          bmi < 22.0f -> 0.0050f
          bmi <= 25.0f -> 0.0075f
          else -> 0.0100f
        }
        val ageCap = if (isSenior65Plus) 0.0050f else 0.0100f
        val rCapped = min(min(requestedRate, expCap), min(bmiCap, ageCap))

        val rawDeficit = 1100f * safeWeightKg * rCapped
        val cappedDeficit = min(rawDeficit, maxSafeDeficitFloat)
        if (rawDeficit > maxSafeDeficitFloat + 5f) {
          safetyNotices.add(
            SafetyGateNotice(
              type = SafetyGateType.DEFICIT_CEILING_APPLIED,
              badgeLabel = "SAFE PACE LIMIT",
              title = "Daily Calorie Cut Capped for Muscle Protection",
              explanation = "Cutting more than 25% of your daily burn causes muscle loss and severe fatigue, so your deficit was capped at $maxSafeDeficitKcal kcal/day.",
              accentColor = Color(0xFFFBBF24),
            )
          )
        }

        val targetRaw = tdeeKcalExact - cappedDeficit
        if (targetRaw < calorieSafetyFloorFloat) {
          safetyNotices.add(
            SafetyGateNotice(
              type = SafetyGateType.CALORIE_FLOOR_APPLIED,
              badgeLabel = "MINIMUM ENERGY FLOOR",
              title = "Minimum Safe Calories Applied ($calorieSafetyFloorKcal kcal)",
              explanation = "Daily calories are kept at or above $calorieSafetyFloorKcal kcal so your brain, hormones, and organs get essential daily nutrition.",
              accentColor = Color(0xFFFBBF24),
            )
          )
        }
        val finalC = roundToNearest5(max(targetRaw, calorieSafetyFloorFloat))
        val actualDeficit = (tdeeKcalExact - finalC).coerceAtLeast(0f)
        estimatedWeeklyKgChange = -(actualDeficit * 7f) / 7700f
        weeklyWeightRatePercent = (actualDeficit / (1100f * safeWeightKg)) * 100f
        finalC
      }

      BlueprintObjectiveOption.BUILD_MUSCLE -> {
        val requestedSurplus = when (targetPace) {
          TargetPaceOption.SLOW -> 0.05f
          TargetPaceOption.NORMAL -> 0.10f
          TargetPaceOption.FAST -> 0.15f
        }
        val expSurplusCap = when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 0.15f
          ExperienceLevelOption.INTERMEDIATE -> 0.12f
          ExperienceLevelOption.ADVANCED -> 0.08f
        }
        val bmiSurplusCap = if (bmi >= 27.0f) 0.05f else 0.15f
        val teenSurplusCap = if (isTeen || isUnder13) 0.05f else 0.15f
        val sFinal = min(min(requestedSurplus, expSurplusCap), min(bmiSurplusCap, teenSurplusCap))

        val finalC = roundToNearest5(tdeeKcalExact * (1f + sFinal))
        val actualSurplus = (finalC - tdeeKcalExact).coerceAtLeast(0f)
        estimatedWeeklyKgChange = (actualSurplus * 7f) / 7700f
        weeklyWeightRatePercent = sFinal * 100f
        finalC
      }
    }

    // ── Protein (P, grams — constant across Workout & Rest days) ───────────
    val proteinFactorGPerKgEff: Float = if (isTeen || isUnder13) {
      when (activityLevel) {
        ActivityLevelOption.SEDENTARY, ActivityLevelOption.LIGHT -> 0.95f
        ActivityLevelOption.MODERATE, ActivityLevelOption.VERY_ACTIVE -> 1.20f
      }
    } else {
      val baseFactor = when (effectiveObjective) {
        BlueprintObjectiveOption.MAINTAIN -> when (activityLevel) {
          ActivityLevelOption.SEDENTARY -> 0.9f
          ActivityLevelOption.LIGHT -> 1.1f
          ActivityLevelOption.MODERATE -> 1.3f
          ActivityLevelOption.VERY_ACTIVE -> 1.5f
        }
        BlueprintObjectiveOption.LOSE_FAT -> when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 1.6f
          ExperienceLevelOption.INTERMEDIATE -> 2.0f
          ExperienceLevelOption.ADVANCED -> 2.4f
        }
        BlueprintObjectiveOption.BUILD_MUSCLE -> when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 1.6f
          ExperienceLevelOption.INTERMEDIATE -> 1.8f
          ExperienceLevelOption.ADVANCED -> 2.0f
        }
      }
      val cardioTweaked = if (
        trainingModality == TrainingModalityOption.CARDIO &&
        effectiveObjective != BlueprintObjectiveOption.LOSE_FAT
      ) {
        val floorAfterCardio = if (effectiveObjective == BlueprintObjectiveOption.MAINTAIN) 0.8f else 1.4f
        max(floorAfterCardio, baseFactor - 0.1f)
      } else {
        baseFactor
      }
      val seniorModifier = if (isSenior60Plus) 0.3f else 0.0f
      cardioTweaked + seniorModifier
    }

    val proteinFloorFloat = 0.8f * effectiveWeightKg
    val proteinCeilingFloat = min(2.5f * effectiveWeightKg, (0.35f * uniformCaloriesKcal) / 4f)
    val proteinFloorGrams = proteinFloorFloat.roundToInt().coerceAtLeast(30)
    val proteinCeilingGrams = max(proteinFloorGrams, proteinCeilingFloat.roundToInt())

    val rawProteinGrams = (proteinFactorGPerKgEff * effectiveWeightKg).roundToInt()
    val recommendedProteinGrams = rawProteinGrams.coerceIn(proteinFloorGrams, proteinCeilingGrams)

    // ── Fat (F, grams — constant across Workout & Rest days) ───────────────
    val fatShareFraction = when {
      isTeen || isUnder13 || isSenior60Plus -> 0.30f
      effectiveObjective == BlueprintObjectiveOption.LOSE_FAT ->
        if (experienceLevel == ExperienceLevelOption.ADVANCED) 0.22f else 0.25f
      effectiveObjective == BlueprintObjectiveOption.MAINTAIN -> 0.28f
      effectiveObjective == BlueprintObjectiveOption.BUILD_MUSCLE -> 0.25f
      else -> 0.25f
    }
    val fatTargetSharePct = (fatShareFraction * 100f).roundToInt()

    val minFatPerKg = if (isMale) 0.5f else 0.6f
    val fatFloorFloat = max(minFatPerKg * effectiveWeightKg, (0.20f * uniformCaloriesKcal) / 9f)
    val maxFatShare = if (isTeen || isUnder13) 0.35f else 0.30f
    val fatCeilingFloat = (maxFatShare * uniformCaloriesKcal) / 9f

    val fatFloorGrams = fatFloorFloat.roundToInt().coerceAtLeast(25)
    val fatCeilingGrams = max(fatFloorGrams, fatCeilingFloat.roundToInt())

    var recommendedFatsGrams = ((fatShareFraction * uniformCaloriesKcal) / 9f)
      .roundToInt()
      .coerceIn(fatFloorGrams, fatCeilingGrams)

    // ── Baseline Uniform Carbohydrates (K, grams) ──────────────────────────
    var uniformCarbsGrams = ((uniformCaloriesKcal - 4 * recommendedProteinGrams - 9 * recommendedFatsGrams) / 4f)
      .roundToInt()
      .coerceAtLeast(0)

    if (uniformCarbsGrams < 130 && recommendedFatsGrams > fatFloorGrams) {
      val neededCarbGrams = 130 - uniformCarbsGrams
      val neededKcal = neededCarbGrams * 4f
      val reducibleFatGrams = (neededKcal / 9f).roundToInt()
      recommendedFatsGrams = max(fatFloorGrams, recommendedFatsGrams - reducibleFatGrams)
      uniformCarbsGrams = ((uniformCaloriesKcal - 4 * recommendedProteinGrams - 9 * recommendedFatsGrams) / 4f)
        .roundToInt()
        .coerceAtLeast(0)
    }

    // ── Uniform Days vs Workout / Rest Days Split (Synced across C, Carbs, H2O, Steps) ──
    val expSwingPhi = when (experienceLevel) {
      ExperienceLevelOption.BEGINNER -> 0.05f
      ExperienceLevelOption.INTERMEDIATE -> 0.08f
      ExperienceLevelOption.ADVANCED -> 0.10f
    }
    var swingPhi = if (trainingDaysPerWeek > 0 && restDaysPerWeek > 0) {
      min(expSwingPhi, 0.15f * restDaysPerWeek.toFloat() / trainingDaysPerWeek.toFloat())
    } else {
      0f
    }

    var workoutDayCaloriesKcal = uniformCaloriesKcal
    var restDayCaloriesKcal = uniformCaloriesKcal
    var workoutDayCarbsGrams = uniformCarbsGrams
    var restDayCarbsGrams = uniformCarbsGrams

    if (trainingDaysPerWeek > 0 && restDaysPerWeek > 0) {
      workoutDayCaloriesKcal = roundToNearest5(uniformCaloriesKcal * (1f + swingPhi))
      restDayCaloriesKcal = roundToNearest5(
        uniformCaloriesKcal * (1f - swingPhi * (trainingDaysPerWeek.toFloat() / restDaysPerWeek.toFloat())),
      )
      workoutDayCarbsGrams = uniformCarbsGrams + ((workoutDayCaloriesKcal - uniformCaloriesKcal) / 4f).roundToInt()
      restDayCarbsGrams = (uniformCarbsGrams - ((uniformCaloriesKcal - restDayCaloriesKcal) / 4f).roundToInt())
        .coerceAtLeast(0)

      if (uniformCarbsGrams >= 130 && restDayCarbsGrams < 130) {
        val maxCarbDrop = (uniformCarbsGrams - 130).coerceAtLeast(0)
        val maxCalDrop = maxCarbDrop * 4f
        val safePhi = (maxCalDrop * restDaysPerWeek.toFloat()) /
          (uniformCaloriesKcal.toFloat() * trainingDaysPerWeek.toFloat())
        swingPhi = min(swingPhi, safePhi.coerceAtLeast(0f))
        workoutDayCaloriesKcal = roundToNearest5(uniformCaloriesKcal * (1f + swingPhi))
        restDayCaloriesKcal = roundToNearest5(
          uniformCaloriesKcal * (1f - swingPhi * (trainingDaysPerWeek.toFloat() / restDaysPerWeek.toFloat())),
        )
        workoutDayCarbsGrams = uniformCarbsGrams + ((workoutDayCaloriesKcal - uniformCaloriesKcal) / 4f).roundToInt()
        restDayCarbsGrams = (uniformCarbsGrams - ((uniformCaloriesKcal - restDayCaloriesKcal) / 4f).roundToInt())
          .coerceAtLeast(130)
      }
    }

    // Active recommended Calories & Carbs synced with Uniform vs Workout/Rest Split!
    val isSplitMode = scheduleCadence == ScheduleCadenceOption.REST_DAYS && trainingDaysPerWeek > 0
    val recommendedCaloriesKcal = if (isSplitMode) {
      if (splitDayFocus == SplitDayFocus.WORKOUT_DAY) workoutDayCaloriesKcal else restDayCaloriesKcal
    } else {
      uniformCaloriesKcal
    }
    val recommendedCarbsGrams = if (isSplitMode) {
      if (splitDayFocus == SplitDayFocus.WORKOUT_DAY) workoutDayCarbsGrams else restDayCarbsGrams
    } else {
      uniformCarbsGrams
    }

    val calorieDeltaFromTdeeKcal = recommendedCaloriesKcal - tdeeKcalRounded
    val isCarbsBelowBrainFloor = recommendedCarbsGrams < 130
    val carbsFloorGrams = 130
    val carbsCeilingGrams = max(
      180,
      ((recommendedCaloriesKcal * 0.65f) / 4f).roundToInt(),
    )

    val adultFibreMin = if (isTeen || isUnder13) 19 else 25
    val recommendedFibreGrams = max(
      adultFibreMin,
      ((14f * recommendedCaloriesKcal) / 1000f).roundToInt(),
    )
    val maxFreeSugarGrams = ((0.10f * recommendedCaloriesKcal) / 4f).roundToInt()
    val maxSodiumMg = 2000

    val totalMacroCalories = (4 * recommendedProteinGrams + 4 * recommendedCarbsGrams + 9 * recommendedFatsGrams)
      .coerceAtLeast(1)
    val proteinCalorieSharePct = ((4f * recommendedProteinGrams / totalMacroCalories) * 100f).roundToInt()
    val fatCalorieSharePct = ((9f * recommendedFatsGrams / totalMacroCalories) * 100f).roundToInt()
    val carbsCalorieSharePct = (100 - proteinCalorieSharePct - fatCalorieSharePct).coerceIn(0, 100)

    // ── Daily Steps (Synced with Uniform vs Workout/Rest Split) ────────────
    val tierBaseSteps = when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> 5000
      ActivityLevelOption.LIGHT -> 7000
      ActivityLevelOption.MODERATE -> 8500
      ActivityLevelOption.VERY_ACTIVE -> 10000
    }
    val objectiveStepTweak = when (effectiveObjective) {
      BlueprintObjectiveOption.LOSE_FAT -> 1500
      BlueprintObjectiveOption.MAINTAIN -> 0
      BlueprintObjectiveOption.BUILD_MUSCLE -> -500
    }
    val stepsFloor = if (isTeen || isUnder13) 10000 else 4000
    val stepsCeiling = when {
      isTeen || isUnder13 -> 12000
      isSenior60Plus -> 8000
      else -> 12000
    }
    val rawSteps = (tierBaseSteps + objectiveStepTweak).coerceIn(stepsFloor, stepsCeiling)
    val uniformSteps = roundToNearest500(rawSteps.toFloat()).coerceIn(stepsFloor, stepsCeiling)
    val workoutDaySteps = if (trainingDaysPerWeek > 0) {
      (uniformSteps + 1000).coerceIn(stepsFloor, stepsCeiling)
    } else {
      uniformSteps
    }
    val restDaySteps = if (trainingDaysPerWeek > 0) {
      (uniformSteps - 1000).coerceIn(stepsFloor, stepsCeiling)
    } else {
      uniformSteps
    }

    val recommendedSteps = if (isSplitMode) {
      if (splitDayFocus == SplitDayFocus.WORKOUT_DAY) workoutDaySteps else restDaySteps
    } else {
      uniformSteps
    }

    val strideFactor = if (isMale) 0.415f else 0.413f
    val strideLengthMeters = strideFactor * heightMeters
    val estimatedWalkingKm = (recommendedSteps * strideLengthMeters) / 1000f
    val informationalWalkingKcal = (recommendedSteps * safeWeightKg * 0.0004f).roundToInt()

    // ── Hydration (ml/day, Synced with Uniform vs Workout/Rest Split) ──────
    val waterPerKgFactor = when {
      isTeen || isUnder13 -> 38f
      isSenior65Plus -> 30f
      else -> 32f
    }
    val baseWaterFloat = waterPerKgFactor * effectiveWeightKg
    val baseWaterMl = baseWaterFloat.roundToInt()
    val climateWaterAddMl = climateBand.baselineWaterAddMl + (if (highAltitude2500m) 500 else 0)
    val occupationWaterAddMl = occupationLoad.waterAddMl
    val caffeineWaterAddMl = if (caffeineAlcoholYes) 300 else 0
    val sodiumWaterAddMl = when (sodiumFrequency) {
      SodiumFrequencyOption.RARELY -> 0
      SodiumFrequencyOption.SOMETIMES -> 150
      SodiumFrequencyOption.OFTEN -> 350
    }

    val restDayWaterRaw = baseWaterFloat +
      climateWaterAddMl +
      occupationWaterAddMl +
      caffeineWaterAddMl +
      sodiumWaterAddMl

    val workoutMinutes = workoutDuration.hours * 60f
    val modalitySweatRateMlPerMin = when (trainingModality) {
      TrainingModalityOption.STRENGTH -> 8f
      TrainingModalityOption.MIXED -> 10f
      TrainingModalityOption.CARDIO -> 13f
    }
    val exerciseWaterAddFloat = workoutMinutes * modalitySweatRateMlPerMin * climateBand.sweatRateMultiplier
    val exerciseWaterAddOnWorkoutDayMl = roundToNearest10(exerciseWaterAddFloat)
    val workoutDayWaterRaw = restDayWaterRaw + exerciseWaterAddFloat

    val uniformWaterRaw = (trainingDaysPerWeek * workoutDayWaterRaw + restDaysPerWeek * restDayWaterRaw) / 7f

    val waterFloorMl = if (isTeen || isUnder13) 1200 else 1500
    val waterCeilingMl = 5000

    val restDayWaterMl = roundToNearest10(restDayWaterRaw).coerceIn(waterFloorMl, waterCeilingMl)
    val workoutDayWaterMl = roundToNearest10(workoutDayWaterRaw).coerceIn(waterFloorMl, waterCeilingMl)
    val uniformDayWaterMl = roundToNearest10(uniformWaterRaw).coerceIn(waterFloorMl, waterCeilingMl)

    val primaryRecommendedWaterMl = if (isSplitMode) {
      if (splitDayFocus == SplitDayFocus.WORKOUT_DAY) workoutDayWaterMl else restDayWaterMl
    } else {
      uniformDayWaterMl
    }

    val waterGlasses250Ml = primaryRecommendedWaterMl / 250f
    val wakingHourlyReminderMl = roundToNearest10(primaryRecommendedWaterMl / 16f)

    // ── Meal Frequency Split (Synced with active Calories & Carbs) ─────────
    val mealCount = mealFrequency.count.coerceIn(2, 6)
    val mainMealsCount = min(3, mealCount)
    val snackMealsCount = (mealCount - 3).coerceAtLeast(0)
    val totalMealWeight = mainMealsCount * 1.0f + snackMealsCount * 0.5f

    val caloriesPerMainMealKcal = roundToNearest5(recommendedCaloriesKcal * (1.0f / totalMealWeight))
    val caloriesPerSnackKcal = if (snackMealsCount > 0) {
      roundToNearest5(recommendedCaloriesKcal * (0.5f / totalMealWeight))
    } else {
      0
    }
    val proteinPerMealGrams = (recommendedProteinGrams.toFloat() / mealCount.toFloat()).roundToInt()
    val carbsPerMealGrams = (recommendedCarbsGrams.toFloat() / mealCount.toFloat()).roundToInt()
    val fatsPerMealGrams = (recommendedFatsGrams.toFloat() / mealCount.toFloat()).roundToInt()
    val proteinPerMealGPerKgEff = if (effectiveWeightKg > 0f) {
      proteinPerMealGrams / effectiveWeightKg
    } else {
      0.3f
    }
    val isProteinPerMealOptimalMps = proteinPerMealGPerKgEff in 0.25f..0.55f

    return DailyTargetsCalculationReport(
      isBlockedUnder13 = isUnder13,
      effectiveObjective = effectiveObjective,
      objectiveWasOverridden = objectiveWasOverridden,
      safetyNotices = safetyNotices,
      calorieSafetyFloorKcal = calorieSafetyFloorKcal,
      calorieSafeCeilingKcal = calorieSafeCeilingKcal,
      maxSafeDeficitKcal = maxSafeDeficitKcal,
      bmi = bmi,
      bmiCategoryStandard = bmiCategoryStandard,
      bmiCategoryAsianWho = bmiCategoryAsianWho,
      actualWeightKg = safeWeightKg,
      referenceWeightKg = referenceWeightKg,
      effectiveWeightKg = effectiveWeightKg,
      isEffectiveWeightAdjusted = isEffectiveWeightAdjusted,
      bmrKcalExact = bmrKcalExact,
      bmrKcalRounded = bmrKcalRounded,
      bmrDescriptor = bmrDescriptor,
      leanBodyMassKg = leanBodyMassKg,
      basePal = basePal,
      occupationPalAddOn = occupationPalAddOn,
      teenPalAddOn = teenPalAddOn,
      totalActivityFactor = totalActivityFactor,
      tdeeKcalExact = tdeeKcalExact,
      tdeeKcalRounded = tdeeKcalRounded,
      uniformCaloriesKcal = uniformCaloriesKcal,
      uniformCarbsGrams = uniformCarbsGrams,
      uniformSteps = uniformSteps,
      recommendedCaloriesKcal = recommendedCaloriesKcal,
      calorieDeltaFromTdeeKcal = calorieDeltaFromTdeeKcal,
      weeklyWeightRatePercent = weeklyWeightRatePercent,
      estimatedWeeklyKgChange = estimatedWeeklyKgChange,
      recommendedProteinGrams = recommendedProteinGrams,
      proteinFactorGPerKgEff = proteinFactorGPerKgEff,
      proteinFloorGrams = proteinFloorGrams,
      proteinCeilingGrams = proteinCeilingGrams,
      proteinCalorieSharePct = proteinCalorieSharePct,
      recommendedFatsGrams = recommendedFatsGrams,
      fatTargetSharePct = fatTargetSharePct,
      fatFloorGrams = fatFloorGrams,
      fatCeilingGrams = fatCeilingGrams,
      fatCalorieSharePct = fatCalorieSharePct,
      recommendedCarbsGrams = recommendedCarbsGrams,
      carbsFloorGrams = carbsFloorGrams,
      carbsCeilingGrams = carbsCeilingGrams,
      carbsCalorieSharePct = carbsCalorieSharePct,
      isCarbsBelowBrainFloor = isCarbsBelowBrainFloor,
      recommendedFibreGrams = recommendedFibreGrams,
      maxFreeSugarGrams = maxFreeSugarGrams,
      maxSodiumMg = maxSodiumMg,
      recommendedSteps = recommendedSteps,
      workoutDaySteps = workoutDaySteps,
      restDaySteps = restDaySteps,
      stepsFloor = stepsFloor,
      stepsCeiling = stepsCeiling,
      strideLengthMeters = strideLengthMeters,
      estimatedWalkingKm = estimatedWalkingKm,
      informationalWalkingKcal = informationalWalkingKcal,
      baseWaterMl = baseWaterMl,
      climateWaterAddMl = climateWaterAddMl,
      occupationWaterAddMl = occupationWaterAddMl,
      caffeineWaterAddMl = caffeineWaterAddMl,
      sodiumWaterAddMl = sodiumWaterAddMl,
      exerciseWaterAddOnWorkoutDayMl = exerciseWaterAddOnWorkoutDayMl,
      restDayWaterMl = restDayWaterMl,
      workoutDayWaterMl = workoutDayWaterMl,
      uniformDayWaterMl = uniformDayWaterMl,
      primaryRecommendedWaterMl = primaryRecommendedWaterMl,
      waterFloorMl = waterFloorMl,
      waterCeilingMl = waterCeilingMl,
      waterGlasses250Ml = waterGlasses250Ml,
      wakingHourlyReminderMl = wakingHourlyReminderMl,
      trainingDaysPerWeek = trainingDaysPerWeek,
      restDaysPerWeek = restDaysPerWeek,
      selectedWorkoutDays = effectiveWorkoutDays,
      selectedRestDays = effectiveRestDays,
      workoutDayCaloriesKcal = workoutDayCaloriesKcal,
      restDayCaloriesKcal = restDayCaloriesKcal,
      workoutDayCarbsGrams = workoutDayCarbsGrams,
      restDayCarbsGrams = restDayCarbsGrams,
      swingFractionPhi = swingPhi,
      activeSplitDayFocus = splitDayFocus,
      mealCount = mealCount,
      mainMealsCount = mainMealsCount,
      snackMealsCount = snackMealsCount,
      caloriesPerMainMealKcal = caloriesPerMainMealKcal,
      caloriesPerSnackKcal = caloriesPerSnackKcal,
      proteinPerMealGrams = proteinPerMealGrams,
      carbsPerMealGrams = carbsPerMealGrams,
      fatsPerMealGrams = fatsPerMealGrams,
      proteinPerMealGPerKgEff = proteinPerMealGPerKgEff,
      isProteinPerMealOptimalMps = isProteinPerMealOptimalMps,
    )
  }

  /**
   * Evaluates user-customized targets and returns simple, everyday, easy-to-understand
   * health impact summaries with real-life examples of what the user will actually feel.
   */
  fun evaluateHealthImpacts(
    report: DailyTargetsCalculationReport,
    custom: CustomTargetsOverride,
  ): List<HealthImpactAlert> {
    if (!custom.isCustomMode) return emptyList()

    val alerts = mutableListOf<HealthImpactAlert>()
    val minSafeCal = report.calorieSafetyFloorKcal
    val maxSafeCal = (report.tdeeKcalRounded * 1.25f).roundToInt()

    // 1. Calories Check
    if (custom.caloriesKcal < minSafeCal) {
      alerts.add(
        HealthImpactAlert(
          id = "calories_below_floor",
          metricName = "Calories Too Low",
          severity = HealthAlertSeverity.CRITICAL,
          userValueFormatted = "${custom.caloriesKcal} kcal",
          recommendedRangeFormatted = "${report.recommendedCaloriesKcal} kcal recommended",
          headline = "Too little energy for your body's daily needs",
          physiologicalImpact = "Eating this little forces your body to burn your own muscle for energy and slows down your metabolism.",
          realLifeExamples = "Real-life impact: Feeling exhausted all day, dizzy when standing up, feeling cold, hair thinning, irritable mood, and sudden binge cravings at night.",
        )
      )
    } else if (report.tdeeKcalRounded - custom.caloriesKcal > report.maxSafeDeficitKcal + 25) {
      alerts.add(
        HealthImpactAlert(
          id = "calories_excess_deficit",
          metricName = "Calorie Cut Too Aggressive",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.caloriesKcal} kcal",
          recommendedRangeFormatted = "${report.recommendedCaloriesKcal} kcal recommended",
          headline = "Cutting calories too fast can burn muscle instead of fat",
          physiologicalImpact = "A steep calorie drop makes your body hold onto fat while breaking down muscle tissue and spiking stress hormones.",
          realLifeExamples = "Real-life impact: Mid-day energy crashes, weak workouts, poor sleep, constant hunger pangs, and rapid weight rebound later.",
        )
      )
    } else if (custom.caloriesKcal > maxSafeCal) {
      alerts.add(
        HealthImpactAlert(
          id = "calories_excess_surplus",
          metricName = "Calories Too High",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.caloriesKcal} kcal",
          recommendedRangeFormatted = "${report.recommendedCaloriesKcal} kcal recommended",
          headline = "Extra calories beyond what your muscles can use",
          physiologicalImpact = "Your muscles can only build so fast each day. Eating far above your daily burn stores the extra energy directly as body fat.",
          realLifeExamples = "Real-life impact: Feeling overly full and sluggish, puffiness, unwanted belly fat gain, and feeling heavy during movement.",
        )
      )
    }

    // 2. Protein Check
    if (custom.proteinGrams < report.proteinFloorGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "protein_below_floor",
          metricName = "Protein Too Low",
          severity = HealthAlertSeverity.CRITICAL,
          userValueFormatted = "${custom.proteinGrams}g",
          recommendedRangeFormatted = "${report.recommendedProteinGrams}g recommended",
          headline = "Not enough protein to protect your muscles and recovery",
          physiologicalImpact = "Without enough daily protein, your body cannot repair muscles after daily activity and your immune system weakens.",
          realLifeExamples = "Real-life impact: Losing muscle tone, staying sore for days after workouts, brittle nails or hair, and feeling hungry soon after meals.",
        )
      )
    } else if (custom.proteinGrams > report.proteinCeilingGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "protein_above_ceiling",
          metricName = "Protein Too High",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.proteinGrams}g",
          recommendedRangeFormatted = "${report.recommendedProteinGrams}g recommended",
          headline = "More protein than your body can use for muscle building",
          physiologicalImpact = "Extra protein beyond this level doesn't build more muscle—it makes your digestion and kidneys work harder and crowds out energy foods.",
          realLifeExamples = "Real-life impact: Stomach bloating, heavy/slow digestion, increased thirst, dehydration, and feeling low on quick workout energy.",
        )
      )
    }

    // 3. Carbohydrates Check
    if (custom.carbsGrams < report.carbsFloorGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "carbs_below_brain_floor",
          metricName = "Carbs Too Low",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.carbsGrams}g",
          recommendedRangeFormatted = "${report.recommendedCarbsGrams}g recommended",
          headline = "Low fuel for your brain and daily workouts",
          physiologicalImpact = "Carbs are the main quick fuel for your brain and muscles. Going too low drains your muscle energy stores.",
          realLifeExamples = "Real-life impact: Brain fog, trouble focusing at work/study, feeling flat or weak during workouts, headaches, and low mood.",
        )
      )
    } else if (custom.carbsGrams > report.carbsCeilingGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "carbs_above_ceiling",
          metricName = "Carbs Too High",
          severity = HealthAlertSeverity.CAUTION,
          userValueFormatted = "${custom.carbsGrams}g",
          recommendedRangeFormatted = "${report.recommendedCarbsGrams}g recommended",
          headline = "High carb load can cause blood sugar spikes and crashes",
          physiologicalImpact = "Eating very high carbs causes big blood-sugar swings and pushes extra unused energy into fat storage.",
          realLifeExamples = "Real-life impact: Feeling very sleepy and drowsy after meals (food coma), afternoon energy crashes, water retention, and frequent sweet cravings.",
        )
      )
    }

    // 4. Fats Check
    if (custom.fatsGrams < report.fatFloorGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "fats_below_floor",
          metricName = "Fats Too Low",
          severity = HealthAlertSeverity.CRITICAL,
          userValueFormatted = "${custom.fatsGrams}g",
          recommendedRangeFormatted = "${report.recommendedFatsGrams}g recommended",
          headline = "Too little healthy fat for hormones and vitamin absorption",
          physiologicalImpact = "Your body needs healthy fats to make key hormones and absorb essential vitamins (A, D, E, and K).",
          realLifeExamples = "Real-life impact: Dry skin and hair, low mood, hormonal imbalance, low drive/energy, and joints feeling stiff.",
        )
      )
    } else if (custom.fatsGrams > report.fatCeilingGrams) {
      alerts.add(
        HealthImpactAlert(
          id = "fats_above_ceiling",
          metricName = "Fats Too High",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.fatsGrams}g",
          recommendedRangeFormatted = "${report.recommendedFatsGrams}g recommended",
          headline = "High fat intake slows digestion and takes up calorie room",
          physiologicalImpact = "Fats pack more than double the calories per gram compared to protein or carbs, leaving less room for workout fuel.",
          realLifeExamples = "Real-life impact: Feeling sluggish or heavy after meals, slower stomach emptying, less stamina in training, and easier fat gain.",
        )
      )
    }

    // 5. Hydration Check
    if (custom.hydrationMl < report.waterFloorMl) {
      alerts.add(
        HealthImpactAlert(
          id = "hydration_below_floor",
          metricName = "Water Too Low",
          severity = HealthAlertSeverity.CRITICAL,
          userValueFormatted = "${custom.hydrationMl} ml",
          recommendedRangeFormatted = "${report.primaryRecommendedWaterMl} ml recommended",
          headline = "Dehydration risk — your body needs more daily fluid",
          physiologicalImpact = "Even mild dehydration makes your heart work harder, thickens blood flow, and slows muscle recovery.",
          realLifeExamples = "Real-life impact: Headaches, dry mouth, dark urine, muscle cramps, afternoon tiredness, and mistaking thirst for hunger.",
        )
      )
    } else if (custom.hydrationMl > report.waterCeilingMl) {
      alerts.add(
        HealthImpactAlert(
          id = "hydration_above_ceiling",
          metricName = "Water Too High",
          severity = HealthAlertSeverity.CRITICAL,
          userValueFormatted = "${custom.hydrationMl} ml",
          recommendedRangeFormatted = "${report.primaryRecommendedWaterMl} ml recommended",
          headline = "Over-hydration can flush out your body's natural salts",
          physiologicalImpact = "Drinking extreme amounts of plain water dilutes essential electrolytes (like sodium and potassium) in your blood.",
          realLifeExamples = "Real-life impact: Constant bathroom trips, Interrupted sleep, lightheadedness, nausea, and watery/weak muscles.",
        )
      )
    }

    // 6. Daily Steps Check
    if (custom.stepsCount < report.stepsFloor) {
      alerts.add(
        HealthImpactAlert(
          id = "steps_below_floor",
          metricName = "Steps Too Low",
          severity = HealthAlertSeverity.WARNING,
          userValueFormatted = "${custom.stepsCount} steps",
          recommendedRangeFormatted = "${report.recommendedSteps} steps recommended",
          headline = "Low daily movement slows down your daily calorie burn",
          physiologicalImpact = "Sitting most of the day lowers circulation, reduces blood sugar control, and slows down fat loss.",
          realLifeExamples = "Real-life impact: Stiff lower back and hips, low daily energy, poorer digestion after meals, and slower progress toward your goal.",
        )
      )
    } else if (custom.stepsCount > report.stepsCeiling) {
      alerts.add(
        HealthImpactAlert(
          id = "steps_above_ceiling",
          metricName = "Steps Very High",
          severity = HealthAlertSeverity.CAUTION,
          userValueFormatted = "${custom.stepsCount} steps",
          recommendedRangeFormatted = "${report.recommendedSteps} steps recommended",
          headline = "Very high step count can tire out your legs for workouts",
          physiologicalImpact = "Doing extreme daily steps on top of workouts can wear down leg recovery and add extra joint stress.",
          realLifeExamples = "Real-life impact: Heavy/tired legs, sore knees or feet, and less strength during leg workouts.",
        )
      )
    }

    // 7. Macro-to-Calorie Sync Check (Simple, friendly explanation!)
    val macroSumKcal = 4 * custom.proteinGrams + 4 * custom.carbsGrams + 9 * custom.fatsGrams
    val kcalDiscrepancy = abs(macroSumKcal - custom.caloriesKcal)
    if (kcalDiscrepancy > 120) {
      val isMacrosHigher = macroSumKcal > custom.caloriesKcal
      alerts.add(
        HealthImpactAlert(
          id = "macro_calorie_mismatch",
          metricName = "Macros & Calories Out of Sync",
          severity = HealthAlertSeverity.CAUTION,
          userValueFormatted = "$macroSumKcal kcal from food vs ${custom.caloriesKcal} kcal goal",
          recommendedRangeFormatted = "Tap 'Match Calories' to align both",
          headline = if (isMacrosHigher) {
            "Your Protein, Carbs & Fats add up to MORE than your Calorie goal"
          } else {
            "Your Protein, Carbs & Fats add up to LESS than your Calorie goal"
          },
          physiologicalImpact = if (isMacrosHigher) {
            "Your macro grams equal $macroSumKcal kcal, which is ${macroSumKcal - custom.caloriesKcal} kcal higher than your ${custom.caloriesKcal} kcal target."
          } else {
            "Your macro grams equal $macroSumKcal kcal, which is ${custom.caloriesKcal - macroSumKcal} kcal lower than your ${custom.caloriesKcal} kcal target."
          },
          realLifeExamples = if (isMacrosHigher) {
            "Real-life impact: If you eat all your macro grams, you'll unknowingly eat extra calories every day and slow down fat loss."
          } else {
            "Real-life impact: If you only eat your macro grams, you'll undereat compared to your calorie goal and feel low on energy."
          },
        )
      )
    }

    return alerts
  }
}
