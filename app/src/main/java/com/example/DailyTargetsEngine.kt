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
 * Ambient Area Temperature / Climate Band.
 * User-selectable on Step 04 & Settings (with smart seasonal default) to calibrate daily thermal hydration and sweat rate.
 */
enum class ClimateBandOption(
  val tempRangeLabel: String,
  val tempFahrenheitLabel: String,
  val seasonName: String,
  val baselineWaterAddMl: Int,
  val sweatRateMultiplier: Float,
  val representativeTempC: Int,
  val accentColor: Color,
) {
  COOL_MILD(
    tempRangeLabel = "< 20°C",
    tempFahrenheitLabel = "< 68°F",
    seasonName = "Cool / Chilly",
    baselineWaterAddMl = 0,
    sweatRateMultiplier = 0.92f,
    representativeTempC = 17,
    accentColor = Color(0xFF7BE3FF),
  ),
  WARM(
    tempRangeLabel = "20–25°C",
    tempFahrenheitLabel = "68–77°F",
    seasonName = "Temperate / Mild",
    baselineWaterAddMl = 250,
    sweatRateMultiplier = 1.00f,
    representativeTempC = 23,
    accentColor = Color(0xFF34D399),
  ),
  HOT(
    tempRangeLabel = "26–30°C",
    tempFahrenheitLabel = "79–86°F",
    seasonName = "Warm / Humid",
    baselineWaterAddMl = 500,
    sweatRateMultiplier = 1.15f,
    representativeTempC = 28,
    accentColor = Color(0xFFFBBF24),
  ),
  VERY_HOT(
    tempRangeLabel = "31–35°C",
    tempFahrenheitLabel = "88–95°F",
    seasonName = "Hot / Summer",
    baselineWaterAddMl = 800,
    sweatRateMultiplier = 1.28f,
    representativeTempC = 33,
    accentColor = Color(0xFFFF7A29),
  ),
  EXTREME_HEAT(
    tempRangeLabel = "> 35°C",
    tempFahrenheitLabel = "> 95°F",
    seasonName = "Tropical / Heatwave",
    baselineWaterAddMl = 1150,
    sweatRateMultiplier = 1.42f,
    representativeTempC = 38,
    accentColor = Color(0xFFF43F5E),
  ),
  ;

  companion object {
    fun defaultForCurrentDate(): ClimateBandOption {
      val monthZeroBased = Calendar.getInstance().get(Calendar.MONTH)
      return when (monthZeroBased) {
        Calendar.DECEMBER, Calendar.JANUARY -> COOL_MILD
        Calendar.FEBRUARY, Calendar.NOVEMBER -> WARM
        Calendar.MARCH, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER -> HOT
        Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> VERY_HOT
        else -> HOT
      }
    }
  }
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
  val sessionEatPalAddOn: Float = 0f,
  val totalActivityFactor: Float,
  val tdeeKcalExact: Float,
  val tdeeKcalRounded: Int,

  // Active Recommended Targets
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

  // Carbohydrates & Fibre
  val recommendedCarbsGrams: Int,
  val carbsFloorGrams: Int,
  val carbsCeilingGrams: Int,
  val carbsCalorieSharePct: Int,
  val isCarbsBelowBrainFloor: Boolean,
  val recommendedFibreGrams: Int,
  val maxFreeSugarGrams: Int,
  val maxSodiumMg: Int,

  // Daily Steps
  val recommendedSteps: Int,
  val stepsFloor: Int,
  val stepsCeiling: Int,
  val strideLengthMeters: Float,
  val estimatedWalkingKm: Float,
  val informationalWalkingKcal: Int,

  // Hydration (ml/day)
  val baseWaterMl: Int,
  val anabolicAndProteinWaterAddMl: Int = 0,
  val climateWaterAddMl: Int,
  val occupationWaterAddMl: Int,
  val caffeineWaterAddMl: Int,
  val sodiumWaterAddMl: Int,
  val exerciseWaterAddOnWorkoutDayMl: Int,
  val workoutDayWaterMl: Int = 0,
  val restDayWaterMl: Int = 0,
  val primaryRecommendedWaterMl: Int,
  val waterFloorMl: Int,
  val waterCeilingMl: Int,
  val waterGlasses250Ml: Float,
  val wakingHourlyReminderMl: Int,

  // Weekly Workout & Rest Day Schedule
  val trainingDaysPerWeek: Int,
  val restDaysPerWeek: Int,
  val selectedWorkoutDays: Set<DayOfWeekOption>,
  val selectedRestDays: Set<DayOfWeekOption>,

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
    mealFrequency: MealFrequencyOption,
    caffeineAlcoholYes: Boolean,
    sodiumFrequency: SodiumFrequencyOption,
    climateBand: ClimateBandOption = ClimateBandOption.defaultForCurrentDate(),
    bodyFatPercentOverride: Float? = null,
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
    val isEffectiveWeightAdjusted: Boolean
    val effectiveWeightKg: Float = if (hasValidBodyFat && bodyFatPercentOverride != null) {
      val lbmFromBf = safeWeightKg * (1f - bodyFatPercentOverride / 100f)
      val healthyBfRef = if (isMale) 0.15f else 0.23f
      isEffectiveWeightAdjusted = bmi > 27.0f || bodyFatPercentOverride > (if (isMale) 24f else 32f)
      (lbmFromBf / (1f - healthyBfRef)).coerceIn(35f, safeWeightKg * 1.05f)
    } else if (!isTeen && !isUnder13 && bmi > 27.0f) {
      isEffectiveWeightAdjusted = true
      referenceWeightKg + 0.25f * (safeWeightKg - referenceWeightKg)
    } else {
      isEffectiveWeightAdjusted = false
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

    // ── TDEE (Total Daily Energy Expenditure: BMR + NEAT + Occupation + Workout EAT) ──
    val isYoungAdult18To25 = safeAge in 18..25
    val basePal = when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> 1.200f
      ActivityLevelOption.LIGHT -> when (trainingDaysPerWeek) {
        1 -> 1.325f
        2 -> 1.355f
        else -> 1.385f // Max 3 days/week
      }
      ActivityLevelOption.MODERATE -> when (trainingDaysPerWeek) {
        1, 2 -> 1.440f
        3 -> 1.490f
        4 -> 1.525f
        else -> 1.560f // Max 5 days/week
      }
      ActivityLevelOption.VERY_ACTIVE -> when (trainingDaysPerWeek) {
        7 -> 1.760f
        else -> 1.725f // 6 days/week
      }
    }
    val occupationPalAddOn = occupationLoad.palAddOn
    val teenPalAddOn = if (isTeen || isUnder13) 0.10f else 0.00f

    // Exercise Activity Thermogenesis (EAT) Session Duration & Modality Calibration:
    // Using Compendium of Physical Activities (Ainsworth et al.) METs & EPOC:
    val sessionEatPalAddOn: Float = if (activityLevel == ActivityLevelOption.SEDENTARY || trainingDaysPerWeek == 0) {
      0.00f
    } else {
      val modalityIntensityFactor = when (trainingModality) {
        TrainingModalityOption.STRENGTH -> 1.00f
        TrainingModalityOption.MIXED -> 1.15f
        TrainingModalityOption.CARDIO -> 1.28f
      }
      val durationDeltaFrom45Min = when (workoutDuration) {
        WorkoutDurationOption.HR_0_5 -> -0.025f
        WorkoutDurationOption.HR_1_0 -> 0.020f * modalityIntensityFactor
        WorkoutDurationOption.HR_1_5 -> 0.065f * modalityIntensityFactor
        WorkoutDurationOption.HR_2_0 -> 0.110f * modalityIntensityFactor
        WorkoutDurationOption.HR_2_5_PLUS -> 0.145f * modalityIntensityFactor
      }
      val weeklyFreqWeight = (trainingDaysPerWeek / 5f).coerceIn(0.40f, 1.30f)
      (durationDeltaFrom45Min * weeklyFreqWeight).coerceIn(-0.04f, 0.16f)
    }

    val totalActivityFactor = (basePal + occupationPalAddOn + teenPalAddOn + sessionEatPalAddOn).coerceIn(1.20f, 2.15f)
    val tdeeKcalExact = bmrKcalExact * totalActivityFactor
    val tdeeKcalRounded = roundToNearest5(tdeeKcalExact)

    // ── Objective Calories (Jeff Nippard / Eric Helms / ISSN Evidence-Based Model) ──
    val minSexFloor = if (isMale) 1500f else 1200f
    val calorieSafetyFloorFloat = max(minSexFloor, 0.92f * bmrKcalExact)
    val calorieSafetyFloorKcal = roundToNearest5(calorieSafetyFloorFloat)
    val calorieSafeCeilingKcal = max(
      calorieSafetyFloorKcal + 450,
      roundToNearest5(tdeeKcalExact * 1.20f),
    )
    val maxSafeDeficitFloat = 0.25f * tdeeKcalExact
    val maxSafeDeficitKcal = roundToNearest5(maxSafeDeficitFloat)

    var weeklyWeightRatePercent = 0f
    var estimatedWeeklyKgChange = 0f

    val recommendedCaloriesKcal: Int = when (effectiveObjective) {
      BlueprintObjectiveOption.MAINTAIN -> {
        weeklyWeightRatePercent = 0f
        estimatedWeeklyKgChange = 0f
        tdeeKcalRounded
      }

      BlueprintObjectiveOption.LOSE_FAT -> {
        // Evidence-based fat loss rates (% of body weight per week: 0.35% Slow, 0.60% Normal, 0.85% Fast)
        val requestedRate = when (targetPace) {
          TargetPaceOption.SLOW -> 0.0035f
          TargetPaceOption.NORMAL -> 0.0060f
          TargetPaceOption.FAST -> 0.0085f
        }
        val expCap = when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 0.0100f
          ExperienceLevelOption.INTERMEDIATE -> 0.0080f
          ExperienceLevelOption.ADVANCED -> 0.0060f
        }
        val bmiCap = when {
          bmi < 21.5f -> 0.0050f
          bmi <= 26.0f -> 0.0080f
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
        // Jeff Nippard & Eric Helms Lean Hypertrophy Surplus Model:
        // Beginners: +10% to +16% TDEE (~250–450 kcal/day)
        // Intermediates: +8% to +13% TDEE (~200–350 kcal/day)
        // Advanced lifters: +5% to +9% TDEE (~125–250 kcal/day lean bulk)
        val requestedSurplus = when (targetPace) {
          TargetPaceOption.SLOW -> 0.07f
          TargetPaceOption.NORMAL -> 0.11f
          TargetPaceOption.FAST -> 0.15f
        }
        val expSurplusCap = when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 0.16f
          ExperienceLevelOption.INTERMEDIATE -> 0.13f
          ExperienceLevelOption.ADVANCED -> 0.09f
        }
        val bmiSurplusCap = if (bmi >= 27.5f) 0.06f else 0.16f
        val teenSurplusCap = if (isTeen || isUnder13) 0.12f else 0.16f
        val sFinal = min(min(requestedSurplus, expSurplusCap), min(bmiSurplusCap, teenSurplusCap))

        val minEffectiveSurplusKcal = when {
          bmi >= 27.5f -> 120f
          experienceLevel == ExperienceLevelOption.ADVANCED -> 140f
          isTeen || isYoungAdult18To25 -> 240f
          else -> 200f
        }
        val rawSurplusKcal = max(tdeeKcalExact * sFinal, minEffectiveSurplusKcal)
        val finalC = roundToNearest5(tdeeKcalExact + rawSurplusKcal)
        val actualSurplus = (finalC - tdeeKcalExact).coerceAtLeast(0f)
        estimatedWeeklyKgChange = (actualSurplus * 7f) / 7700f
        weeklyWeightRatePercent = (actualSurplus / tdeeKcalExact.coerceAtLeast(1f)) * 100f
        finalC
      }
    }

    // ── Protein (P, grams — Jeff Nippard & ISSN Position Stand: Jäger et al.) ──
    val proteinFactorGPerKgEff: Float = if (isUnder13) {
      when (activityLevel) {
        ActivityLevelOption.SEDENTARY, ActivityLevelOption.LIGHT -> 1.10f
        ActivityLevelOption.MODERATE, ActivityLevelOption.VERY_ACTIVE -> 1.35f
      }
    } else if (isTeen) {
      when (effectiveObjective) {
        BlueprintObjectiveOption.BUILD_MUSCLE -> when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 1.75f
          ExperienceLevelOption.INTERMEDIATE -> 1.85f
          ExperienceLevelOption.ADVANCED -> 1.95f
        }
        BlueprintObjectiveOption.MAINTAIN -> when (activityLevel) {
          ActivityLevelOption.SEDENTARY -> 1.15f
          ActivityLevelOption.LIGHT -> 1.45f
          ActivityLevelOption.MODERATE -> 1.65f
          ActivityLevelOption.VERY_ACTIVE -> 1.80f
        }
        BlueprintObjectiveOption.LOSE_FAT -> 1.80f
      }
    } else {
      val baseFactor = when (effectiveObjective) {
        BlueprintObjectiveOption.MAINTAIN -> when (activityLevel) {
          ActivityLevelOption.SEDENTARY -> 1.20f
          ActivityLevelOption.LIGHT -> 1.55f
          ActivityLevelOption.MODERATE -> 1.75f // Body Recomposition sweet spot (Jeff Nippard)
          ActivityLevelOption.VERY_ACTIVE -> 1.90f
        }
        BlueprintObjectiveOption.LOSE_FAT -> when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 1.90f
          ExperienceLevelOption.INTERMEDIATE -> 2.10f
          ExperienceLevelOption.ADVANCED -> 2.30f // Helms / Nippard lean-mass retention in deficit
        }
        BlueprintObjectiveOption.BUILD_MUSCLE -> when (experienceLevel) {
          ExperienceLevelOption.BEGINNER -> 1.85f // 1.85 g/kg (~0.84 g/lb) optimal MPS
          ExperienceLevelOption.INTERMEDIATE -> 2.00f // 2.00 g/kg (~0.91 g/lb) bodybuilding standard
          ExperienceLevelOption.ADVANCED -> 2.15f // 2.15 g/kg (~0.98 g/lb)
        }
      }
      val cardioTweaked = if (
        trainingModality == TrainingModalityOption.CARDIO &&
        effectiveObjective != BlueprintObjectiveOption.LOSE_FAT
      ) {
        val floorAfterCardio = if (effectiveObjective == BlueprintObjectiveOption.MAINTAIN) 1.35f else 1.65f
        max(floorAfterCardio, baseFactor - 0.15f)
      } else {
        baseFactor
      }
      val seniorModifier = if (isSenior60Plus) 0.20f else 0.0f
      (cardioTweaked + seniorModifier).coerceAtMost(2.40f)
    }

    val proteinFloorFloat = if (activityLevel == ActivityLevelOption.SEDENTARY) {
      1.0f * effectiveWeightKg
    } else {
      1.4f * effectiveWeightKg
    }
    val proteinCeilingFloat = min(2.6f * effectiveWeightKg, (0.38f * recommendedCaloriesKcal) / 4f)
    val proteinFloorGrams = proteinFloorFloat.roundToInt().coerceAtLeast(45)
    val proteinCeilingGrams = max(proteinFloorGrams + 20, proteinCeilingFloat.roundToInt())

    val rawProteinGrams = (proteinFactorGPerKgEff * effectiveWeightKg).roundToInt()
    val recommendedProteinGrams = rawProteinGrams.coerceIn(proteinFloorGrams, proteinCeilingGrams)

    // ── Fat (F, grams — Hormonal & Essential Fatty Acid Floor) ─────────────
    val fatShareFraction = when {
      isTeen || isUnder13 || isSenior60Plus -> 0.28f
      effectiveObjective == BlueprintObjectiveOption.LOSE_FAT ->
        if (experienceLevel == ExperienceLevelOption.ADVANCED) 0.22f else 0.25f
      effectiveObjective == BlueprintObjectiveOption.MAINTAIN -> 0.27f
      effectiveObjective == BlueprintObjectiveOption.BUILD_MUSCLE -> 0.25f
      else -> 0.25f
    }
    val fatTargetSharePct = (fatShareFraction * 100f).roundToInt()

    val minFatPerKg = if (isMale) 0.65f else 0.75f
    val fatFloorFloat = max(minFatPerKg * effectiveWeightKg, (0.18f * recommendedCaloriesKcal) / 9f)
    val maxFatShare = if (isTeen || isUnder13) 0.35f else 0.32f
    val fatCeilingFloat = (maxFatShare * recommendedCaloriesKcal) / 9f

    val fatFloorGrams = fatFloorFloat.roundToInt().coerceAtLeast(35)
    val fatCeilingGrams = max(fatFloorGrams + 15, fatCeilingFloat.roundToInt())

    var recommendedFatsGrams = ((fatShareFraction * recommendedCaloriesKcal) / 9f)
      .roundToInt()
      .coerceIn(fatFloorGrams, fatCeilingGrams)

    // ── Carbohydrates (K, grams) ───────────────────────────────────────────
    var recommendedCarbsGrams = ((recommendedCaloriesKcal - 4 * recommendedProteinGrams - 9 * recommendedFatsGrams) / 4f)
      .roundToInt()
      .coerceAtLeast(0)

    if (recommendedCarbsGrams < 130 && recommendedFatsGrams > fatFloorGrams) {
      val neededCarbGrams = 130 - recommendedCarbsGrams
      val neededKcal = neededCarbGrams * 4f
      val reducibleFatGrams = (neededKcal / 9f).roundToInt()
      recommendedFatsGrams = max(fatFloorGrams, recommendedFatsGrams - reducibleFatGrams)
      recommendedCarbsGrams = ((recommendedCaloriesKcal - 4 * recommendedProteinGrams - 9 * recommendedFatsGrams) / 4f)
        .roundToInt()
        .coerceAtLeast(0)
    }

    val calorieDeltaFromTdeeKcal = recommendedCaloriesKcal - tdeeKcalRounded
    val isCarbsBelowBrainFloor = recommendedCarbsGrams < 130
    val carbsFloorGrams = 130
    val carbsCeilingGrams = max(
      200,
      ((recommendedCaloriesKcal * 0.65f) / 4f).roundToInt(),
    )

    val adultFibreMin = if (isTeen || isUnder13) 22 else 28
    val recommendedFibreGrams = max(
      adultFibreMin,
      ((14f * recommendedCaloriesKcal) / 1000f).roundToInt(),
    ).coerceAtMost(50)
    val maxFreeSugarGrams = ((0.10f * recommendedCaloriesKcal) / 4f).roundToInt()
    val maxSodiumMg = 2300

    val totalMacroCalories = (4 * recommendedProteinGrams + 4 * recommendedCarbsGrams + 9 * recommendedFatsGrams)
      .coerceAtLeast(1)
    val proteinCalorieSharePct = ((4f * recommendedProteinGrams / totalMacroCalories) * 100f).roundToInt()
    val fatCalorieSharePct = ((9f * recommendedFatsGrams / totalMacroCalories) * 100f).roundToInt()
    val carbsCalorieSharePct = (100 - proteinCalorieSharePct - fatCalorieSharePct).coerceIn(0, 100)

    // ── Daily Steps (Activity + Occupation + Objective + Modality) ─────────
    val tierBaseSteps = when (activityLevel) {
      ActivityLevelOption.SEDENTARY -> 5500
      ActivityLevelOption.LIGHT -> 7500
      ActivityLevelOption.MODERATE -> 8500
      ActivityLevelOption.VERY_ACTIVE -> 10000
    }
    val occupationStepAddOn = when (occupationLoad) {
      OccupationLoadOption.DESK -> 0
      OccupationLoadOption.LIGHT_STANDING -> 500
      OccupationLoadOption.ACTIVE_ON_FEET -> 1500
      OccupationLoadOption.HEAVY_MANUAL -> 2000
    }
    val objectiveStepTweak = when (effectiveObjective) {
      BlueprintObjectiveOption.LOSE_FAT -> 1500
      BlueprintObjectiveOption.MAINTAIN -> 0
      BlueprintObjectiveOption.BUILD_MUSCLE -> 0
    }
    val modalityStepTweak = if (trainingModality == TrainingModalityOption.CARDIO) 500 else 0
    val stepsFloor = if (isTeen || isUnder13) 7500 else 5000
    val stepsCeiling = when {
      isSenior60Plus -> 10000
      else -> 14000
    }
    val rawSteps = (tierBaseSteps + occupationStepAddOn + objectiveStepTweak + modalityStepTweak)
      .coerceIn(stepsFloor, stepsCeiling)
    val recommendedSteps = roundToNearest500(rawSteps.toFloat()).coerceIn(stepsFloor, stepsCeiling)

    val strideFactor = if (isMale) 0.415f else 0.413f
    val strideLengthMeters = strideFactor * heightMeters
    val estimatedWalkingKm = (recommendedSteps * strideLengthMeters) / 1000f
    val informationalWalkingKcal = (recommendedSteps * safeWeightKg * 0.0004f).roundToInt()

    // ── Hydration (ml/day — NASEM DRI + ACSM Sweat Rate + ISSN Muscle & Urea Load) ──
    val waterPerKgFactor = when {
      isTeen || isUnder13 -> if (isMale) 39.0f else 36.0f
      isYoungAdult18To25 -> if (isMale) 38.5f else 35.0f
      isSenior65Plus -> if (isMale) 33.0f else 31.0f
      else -> if (isMale) 36.5f else 33.5f
    }
    val minSexBaselineWaterFloat = when {
      isUnder13 -> 1600f
      isTeen -> if (isMale) 2250f else 1900f
      else -> if (isMale) 2400f else 2000f
    }
    val baseWaterFloat = max(waterPerKgFactor * effectiveWeightKg, minSexBaselineWaterFloat)
    val baseWaterMl = roundToNearest10(baseWaterFloat)

    val objectiveMetabolicWaterMl = when (effectiveObjective) {
      BlueprintObjectiveOption.BUILD_MUSCLE -> if (activityLevel == ActivityLevelOption.SEDENTARY) 150 else 350
      BlueprintObjectiveOption.LOSE_FAT -> 220
      BlueprintObjectiveOption.MAINTAIN -> if (activityLevel == ActivityLevelOption.SEDENTARY) 0 else 120
    }
    val proteinUreaClearanceWaterMl = ((recommendedProteinGrams - 95).coerceAtLeast(0) * 2.2f)
      .roundToInt()
      .coerceIn(0, 280)
    val anabolicAndProteinWaterAddMl = roundToNearest10((objectiveMetabolicWaterMl + proteinUreaClearanceWaterMl).toFloat())

    val climateWaterAddMl = climateBand.baselineWaterAddMl + (if (highAltitude2500m) 500 else 0)
    val occupationWaterAddMl = occupationLoad.waterAddMl
    val caffeineWaterAddMl = if (caffeineAlcoholYes) 300 else 0
    val sodiumWaterAddMl = when (sodiumFrequency) {
      SodiumFrequencyOption.RARELY -> 0
      SodiumFrequencyOption.SOMETIMES -> 150
      SodiumFrequencyOption.OFTEN -> 350
    }

    val restDayWaterRaw = baseWaterMl +
      anabolicAndProteinWaterAddMl +
      climateWaterAddMl +
      occupationWaterAddMl +
      caffeineWaterAddMl +
      sodiumWaterAddMl

    val workoutMinutes = if (activityLevel == ActivityLevelOption.SEDENTARY || trainingDaysPerWeek == 0) {
      0f
    } else {
      workoutDuration.hours * 60f
    }
    val modalitySweatRateMlPerMin = when (trainingModality) {
      TrainingModalityOption.STRENGTH -> 10.0f
      TrainingModalityOption.MIXED -> 12.8f
      TrainingModalityOption.CARDIO -> 15.2f
    }
    val exerciseWaterAddFloat = workoutMinutes * modalitySweatRateMlPerMin * climateBand.sweatRateMultiplier

    val waterFloorMl = when {
      isUnder13 -> 1500
      isTeen -> if (isMale) 2200 else 1850
      else -> if (isMale) 2500 else 2100
    }
    val waterCeilingMl = 5500

    val restDayWaterMl = roundToNearest10(restDayWaterRaw.toFloat()).coerceIn(waterFloorMl, waterCeilingMl)
    val workoutDayWaterMl = if (trainingDaysPerWeek > 0 && activityLevel != ActivityLevelOption.SEDENTARY) {
      roundToNearest10(restDayWaterMl + exerciseWaterAddFloat).coerceIn(restDayWaterMl, waterCeilingMl)
    } else {
      restDayWaterMl
    }
    val exerciseWaterAddOnWorkoutDayMl = (workoutDayWaterMl - restDayWaterMl).coerceAtLeast(0)

    // Primary recommended hydration matches Training Day Target when user has active training days,
    // or Rest Day Baseline when sedentary (0 training days), eliminating any discrepancy between cards.
    val primaryRecommendedWaterMl = if (trainingDaysPerWeek > 0 && activityLevel != ActivityLevelOption.SEDENTARY) {
      workoutDayWaterMl
    } else {
      restDayWaterMl
    }

    val waterGlasses250Ml = primaryRecommendedWaterMl / 250f
    val wakingHourlyReminderMl = roundToNearest10(primaryRecommendedWaterMl / 16f)

    // ── Meal Frequency Split ───────────────────────────────────────────────
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
      sessionEatPalAddOn = sessionEatPalAddOn,
      totalActivityFactor = totalActivityFactor,
      tdeeKcalExact = tdeeKcalExact,
      tdeeKcalRounded = tdeeKcalRounded,
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
      stepsFloor = stepsFloor,
      stepsCeiling = stepsCeiling,
      strideLengthMeters = strideLengthMeters,
      estimatedWalkingKm = estimatedWalkingKm,
      informationalWalkingKcal = informationalWalkingKcal,
      baseWaterMl = baseWaterMl,
      anabolicAndProteinWaterAddMl = anabolicAndProteinWaterAddMl,
      climateWaterAddMl = climateWaterAddMl,
      occupationWaterAddMl = occupationWaterAddMl,
      caffeineWaterAddMl = caffeineWaterAddMl,
      sodiumWaterAddMl = sodiumWaterAddMl,
      exerciseWaterAddOnWorkoutDayMl = exerciseWaterAddOnWorkoutDayMl,
      workoutDayWaterMl = workoutDayWaterMl,
      restDayWaterMl = restDayWaterMl,
      primaryRecommendedWaterMl = primaryRecommendedWaterMl,
      waterFloorMl = waterFloorMl,
      waterCeilingMl = waterCeilingMl,
      waterGlasses250Ml = waterGlasses250Ml,
      wakingHourlyReminderMl = wakingHourlyReminderMl,
      trainingDaysPerWeek = trainingDaysPerWeek,
      restDaysPerWeek = restDaysPerWeek,
      selectedWorkoutDays = effectiveWorkoutDays,
      selectedRestDays = effectiveRestDays,
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
