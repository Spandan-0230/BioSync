package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CaloriesEnd
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsEnd
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.FatsEnd
import com.example.ui.theme.FatsMid
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationEnd
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianTrack
import com.example.ui.theme.ObsidianTrackBorder
import com.example.ui.theme.ProteinEnd
import com.example.ui.theme.ProteinMid
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsEnd
import com.example.ui.theme.StepsMid
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SumZinc100 = Color(0xFFF4F4F5)
private val SumZinc200 = Color(0xFFE4E4E7)
private val SumZinc300 = Color(0xFFD4D4D8)
private val SumZinc400 = Color(0xFFA1A1AA)
private val SumZinc500 = Color(0xFF71717A)
private val SumZinc600 = Color(0xFF52525B)
private val SumZinc700 = Color(0xFF3F3F46)
private val SumZinc800 = Color(0xFF27272A)
private val SumZinc850 = Color(0xFF18181C)
private val SumZinc900 = Color(0xFF121216)
private val SumZinc950 = Color(0xFF0A0A0D)
private val SumCardTop = Color(0xFF15171F)
private val SumCardMid = Color(0xFF0E1015)
private val SumCardBottom = Color(0xFF08090C)

internal fun formatSummaryInt(value: Int): String =
  NumberFormat.getIntegerInstance(Locale.US).format(value)

// ═════════════════════════════════════════════════════════════════════════════
// CALCULATION BREAKDOWN MODEL (REVEALED ON 2–3 SECOND HOLD)
// ═════════════════════════════════════════════════════════════════════════════

internal enum class CalculationTopic {
  CALORIES,
  PROTEIN,
  CARBS,
  FATS,
  HYDRATION,
  STEPS,
  MICRONUTRIENTS,
  BMI,
  EFFECTIVE_MASS,
  BMR,
  TDEE,
}

@Immutable
internal data class CalculationStepRow(
  val stepLabel: String,
  val formulaOrInput: String,
  val resultValue: String,
)

@Immutable
internal data class CalculationBreakdownSpec(
  val topic: CalculationTopic,
  val categoryTag: String,
  val title: String,
  val finalValueFormatted: String,
  val summarySubtitle: String,
  val gradientColors: List<Color>,
  val equationBanner: String,
  val steps: List<CalculationStepRow>,
  val scientificFootnote: String,
)

internal fun buildCalculationBreakdown(
  topic: CalculationTopic,
  report: DailyTargetsCalculationReport,
  ageYears: Int,
  selectedSex: String,
  heightCm: Float,
  weightKg: Float,
  activityLevel: ActivityLevelOption,
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  scheduleCadence: ScheduleCadenceOption,
  mealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
): CalculationBreakdownSpec {
  val isMale = selectedSex.equals("MALE", ignoreCase = true)
  val isSplit = scheduleCadence == ScheduleCadenceOption.REST_DAYS && report.trainingDaysPerWeek > 0

  return when (topic) {
    CalculationTopic.CALORIES -> {
      val deltaSign = if (report.calorieDeltaFromTdeeKcal >= 0) "+" else ""
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "ENERGY ENGINE • CALORIES",
        title = "How Daily Calories Are Calculated",
        finalValueFormatted = "${formatSummaryInt(report.recommendedCaloriesKcal)} kcal / day",
        summarySubtitle = "Derived from your Resting Burn (BMR), Daily Activity Multiplier (TDEE), and ${report.effectiveObjective.name.replace('_', ' ')} pace.",
        gradientColors = listOf(CaloriesStart, CaloriesMid, CaloriesEnd),
        equationBanner = "Target Calories = TDEE (${formatSummaryInt(report.tdeeKcalRounded)}) $deltaSign ${report.calorieDeltaFromTdeeKcal} kcal",
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Basal Metabolic Rate (BMR)",
            formulaOrInput = report.bmrDescriptor,
            resultValue = "${formatSummaryInt(report.bmrKcalRounded)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "2. Total Daily Burn (TDEE)",
            formulaOrInput = "BMR × ${String.format(Locale.US, "%.3f", report.totalActivityFactor)} (${activityLevel.name.lowercase().replaceFirstChar { it.uppercase() }} + ${occupationLoad.title})",
            resultValue = "${formatSummaryInt(report.tdeeKcalRounded)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "3. Goal & Pace Adjustment",
            formulaOrInput = "${report.effectiveObjective.name.replace('_', ' ')} • ${targetPace.title} (${targetPace.weeklyRateKg})",
            resultValue = "${deltaSign}${report.calorieDeltaFromTdeeKcal} kcal",
          ),
          CalculationStepRow(
            stepLabel = "4. Schedule Mode",
            formulaOrInput = if (isSplit) {
              if (report.activeSplitDayFocus == SplitDayFocus.WORKOUT_DAY) {
                "Workout Day Allocation (${report.trainingDaysPerWeek}d/wk)"
              } else {
                "Rest Day Recovery Allocation (${report.restDaysPerWeek}d/wk)"
              }
            } else {
              "Uniform 7-Day Daily Average"
            },
            resultValue = "${formatSummaryInt(report.recommendedCaloriesKcal)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "5. Safety Floor Guard",
            formulaOrInput = "Minimum protected cellular intake",
            resultValue = "≥ ${formatSummaryInt(report.calorieSafetyFloorKcal)} kcal",
          ),
        ),
        scientificFootnote = "Rounded to the nearest 5 kcal. Protects lean muscle tissue while matching your weekly pace.",
      )
    }

    CalculationTopic.PROTEIN -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "STRUCTURAL MACRO • PROTEIN",
        title = "How Daily Protein Is Calculated",
        finalValueFormatted = "${report.recommendedProteinGrams}g / day (${report.recommendedProteinGrams * 4} kcal)",
        summarySubtitle = "Anchored to your Effective Body Mass, goal, training style, and experience level to protect and build lean muscle.",
        gradientColors = listOf(ProteinStart, ProteinMid, ProteinEnd),
        equationBanner = String.format(
          Locale.US,
          "%.1f kg Effective Mass × %.2f g/kg = %dg Protein",
          report.effectiveWeightKg,
          report.proteinFactorGPerKgEff,
          report.recommendedProteinGrams,
        ),
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Effective Mass (W_eff)",
            formulaOrInput = if (report.isEffectiveWeightAdjusted) {
              "Lean-adjusted mass from ${String.format(Locale.US, "%.1f", weightKg)} kg"
            } else {
              "Actual body weight (${String.format(Locale.US, "%.1f", weightKg)} kg)"
            },
            resultValue = String.format(Locale.US, "%.1f kg", report.effectiveWeightKg),
          ),
          CalculationStepRow(
            stepLabel = "2. Protein Factor (g/kg)",
            formulaOrInput = "${report.effectiveObjective.name.replace('_', ' ')} • ${experienceLevel.name} • ${trainingModality.shortLabel}",
            resultValue = String.format(Locale.US, "%.2f g/kg", report.proteinFactorGPerKgEff),
          ),
          CalculationStepRow(
            stepLabel = "3. Safe Physiological Range",
            formulaOrInput = "Floor to Ceiling for your mass",
            resultValue = "${report.proteinFloorGrams}g – ${report.proteinCeilingGrams}g",
          ),
          CalculationStepRow(
            stepLabel = "4. Per-Meal Distribution",
            formulaOrInput = "Divided across ${mealFrequency.displayCount} meals/day (4 kcal/g)",
            resultValue = "~${report.proteinPerMealGrams}g / meal",
          ),
        ),
        scientificFootnote = "Protein provides 4 kcal per gram (${report.proteinCalorieSharePct}% of your daily calories) and stays constant on both workout and rest days.",
      )
    }

    CalculationTopic.CARBS -> {
      val proteinKcal = report.recommendedProteinGrams * 4
      val fatKcal = report.recommendedFatsGrams * 9
      val carbKcal = (report.recommendedCaloriesKcal - proteinKcal - fatKcal).coerceAtLeast(0)
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "GLYCOGEN FUEL • CARBOHYDRATES",
        title = "How Daily Carbs Are Calculated",
        finalValueFormatted = "${report.recommendedCarbsGrams}g / day (${report.recommendedCarbsGrams * 4} kcal)",
        summarySubtitle = "Allocated from your remaining energy budget after locking in Protein and Essential Fats, fueling workouts and brain focus.",
        gradientColors = listOf(CarbsStart, CarbsMid, CarbsEnd),
        equationBanner = "(${formatSummaryInt(report.recommendedCaloriesKcal)} - $proteinKcal P - $fatKcal F) ÷ 4 = ${report.recommendedCarbsGrams}g",
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Daily Calorie Budget",
            formulaOrInput = "Total target energy for the day",
            resultValue = "${formatSummaryInt(report.recommendedCaloriesKcal)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "2. Subtract Protein & Fats",
            formulaOrInput = "Protein ($proteinKcal kcal) + Fats ($fatKcal kcal)",
            resultValue = "-${formatSummaryInt(proteinKcal + fatKcal)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "3. Remaining Glycogen Energy",
            formulaOrInput = "$carbKcal kcal ÷ 4 kcal per gram",
            resultValue = "${report.recommendedCarbsGrams}g Carbs",
          ),
          CalculationStepRow(
            stepLabel = "4. Brain & CNS Glucose Floor",
            formulaOrInput = "Minimum daily carb floor for focus & thyroid",
            resultValue = "${report.carbsFloorGrams}g – ${report.carbsCeilingGrams}g",
          ),
        ),
        scientificFootnote = "Carbohydrates provide 4 kcal per gram (${report.carbsCalorieSharePct}% of daily energy) and scale up on workout days when using Workout/Rest Split.",
      )
    }

    CalculationTopic.FATS -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "HORMONAL & CELLULAR • DIETARY FATS",
        title = "How Daily Fats Are Calculated",
        finalValueFormatted = "${report.recommendedFatsGrams}g / day (${report.recommendedFatsGrams * 9} kcal)",
        summarySubtitle = "Calibrated as a percentage of total energy to support hormone production, brain health, and fat-soluble vitamins (A, D, E, K).",
        gradientColors = listOf(FatsStart, FatsMid, FatsEnd),
        equationBanner = "(${formatSummaryInt(report.uniformCaloriesKcal)} kcal × ${report.fatTargetSharePct}%) ÷ 9 = ${report.recommendedFatsGrams}g",
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Target Energy Share",
            formulaOrInput = "Matched to ${trainingModality.shortLabel} & ${report.effectiveObjective.name.replace('_', ' ')}",
            resultValue = "${report.fatTargetSharePct}% of kcal",
          ),
          CalculationStepRow(
            stepLabel = "2. Energy Density Conversion",
            formulaOrInput = "Dietary fat contains 9 kcal per gram",
            resultValue = "${report.recommendedFatsGrams * 9} kcal ÷ 9",
          ),
          CalculationStepRow(
            stepLabel = "3. Hormonal Safety Floor",
            formulaOrInput = "Minimum 0.65g/kg to protect endocrine health",
            resultValue = "≥ ${report.fatFloorGrams}g / day",
          ),
          CalculationStepRow(
            stepLabel = "4. Healthy Upper Ceiling",
            formulaOrInput = "35% maximum energy share cap",
            resultValue = "≤ ${report.fatCeilingGrams}g / day",
          ),
        ),
        scientificFootnote = "Fats provide 9 kcal per gram (${report.fatCalorieSharePct}% of your daily calories) and ~${report.fatsPerMealGrams}g per meal.",
      )
    }

    CalculationTopic.HYDRATION -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "FLUID BALANCE • DAILY HYDRATION",
        title = "How Daily Water Is Calculated",
        finalValueFormatted = "${formatSummaryInt(report.primaryRecommendedWaterMl)} ml (${String.format(Locale.US, "%.2f L", report.primaryRecommendedWaterMl / 1000f)})",
        summarySubtitle = "Combines your body-mass baseline with exercise sweat replacement, work load, and diuretic/sodium flush add-ons.",
        gradientColors = listOf(HydrationStart, HydrationMid, HydrationEnd),
        equationBanner = "Base ${report.baseWaterMl} + Sweat ${report.exerciseWaterAddOnWorkoutDayMl} + Lifestyle ${report.climateWaterAddMl + report.occupationWaterAddMl + report.caffeineWaterAddMl + report.sodiumWaterAddMl} ml",
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Body Mass Baseline",
            formulaOrInput = String.format(Locale.US, "%.1f kg Effective Mass × age/sex factor", report.effectiveWeightKg),
            resultValue = "${formatSummaryInt(report.baseWaterMl)} ml",
          ),
          CalculationStepRow(
            stepLabel = "2. Workout Sweat Loss",
            formulaOrInput = "${trainingModality.shortLabel} (${workoutDuration.displayHours})",
            resultValue = "+${formatSummaryInt(report.exerciseWaterAddOnWorkoutDayMl)} ml",
          ),
          CalculationStepRow(
            stepLabel = "3. Ambient & Work Load",
            formulaOrInput = "${occupationLoad.title} (+${report.occupationWaterAddMl} ml) + Ambient (+${report.climateWaterAddMl} ml)",
            resultValue = "+${report.occupationWaterAddMl + report.climateWaterAddMl} ml",
          ),
          CalculationStepRow(
            stepLabel = "4. Caffeine & Sodium Flush",
            formulaOrInput = "Coffee/Alcohol (+${report.caffeineWaterAddMl} ml) • Salt (${sodiumFrequency.title} +${report.sodiumWaterAddMl} ml)",
            resultValue = "+${report.caffeineWaterAddMl + report.sodiumWaterAddMl} ml",
          ),
          CalculationStepRow(
            stepLabel = "5. Daily Drinking Pace",
            formulaOrInput = "${String.format(Locale.US, "%.1f", report.waterGlasses250Ml)} glasses (250 ml) over 16 waking hrs",
            resultValue = "~${report.wakingHourlyReminderMl} ml / hr",
          ),
        ),
        scientificFootnote = "Safe daily hydration range for your profile is ${formatSummaryInt(report.waterFloorMl)} – ${formatSummaryInt(report.waterCeilingMl)} ml.",
      )
    }

    CalculationTopic.STEPS -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "LOCOMOTION & NEAT • DAILY STEPS",
        title = "How Daily Steps Are Calculated",
        finalValueFormatted = "${formatSummaryInt(report.recommendedSteps)} steps / day",
        summarySubtitle = "Calibrated from your activity tier, occupation load, fat-loss/muscle goal, and height-derived stride length.",
        gradientColors = listOf(StepsStart, StepsMid, StepsEnd),
        equationBanner = String.format(
          Locale.US,
          "%s steps × %.2f m stride = ~%.1f km (~%d kcal)",
          formatSummaryInt(report.recommendedSteps),
          report.strideLengthMeters,
          report.estimatedWalkingKm,
          report.informationalWalkingKcal,
        ),
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Activity & Goal Baseline",
            formulaOrInput = "${activityLevel.name.replace('_', ' ')} • ${report.effectiveObjective.name.replace('_', ' ')}",
            resultValue = "${formatSummaryInt(report.uniformSteps)} steps",
          ),
          CalculationStepRow(
            stepLabel = "2. Stride Length (from Height)",
            formulaOrInput = String.format(Locale.US, "%.1f cm × %s factor", heightCm, if (isMale) "0.415" else "0.413"),
            resultValue = String.format(Locale.US, "%.2f m / step", report.strideLengthMeters),
          ),
          CalculationStepRow(
            stepLabel = "3. Estimated Walking Distance",
            formulaOrInput = "Steps × Stride Length ÷ 1,000",
            resultValue = String.format(Locale.US, "~%.1f km / day", report.estimatedWalkingKm),
          ),
          CalculationStepRow(
            stepLabel = "4. Walking Energy Burn",
            formulaOrInput = "Included inside your TDEE activity factor",
            resultValue = "~${report.informationalWalkingKcal} kcal",
          ),
        ),
        scientificFootnote = "Workout days target ${formatSummaryInt(report.workoutDaySteps)} steps; rest days target ${formatSummaryInt(report.restDaySteps)} steps.",
      )
    }

    CalculationTopic.MICRONUTRIENTS -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "METABOLIC HEALTH • FIBRE, SUGAR & SODIUM",
        title = "How Fibre, Sugar & Sodium Caps Work",
        finalValueFormatted = "${report.recommendedFibreGrams}g Fibre • <${report.maxFreeSugarGrams}g Sugar • <${report.maxSodiumMg}mg Na",
        summarySubtitle = "Evidence-based daily thresholds for gut health, blood sugar stability, and blood pressure protection.",
        gradientColors = listOf(StepsStart, StepsMid, StepsEnd),
        equationBanner = "14g Fibre per 1,000 kcal = ${report.recommendedFibreGrams}g/day",
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Dietary Fibre Target",
            formulaOrInput = "(${formatSummaryInt(report.recommendedCaloriesKcal)} kcal ÷ 1,000) × 14g",
            resultValue = "${report.recommendedFibreGrams}g / day",
          ),
          CalculationStepRow(
            stepLabel = "2. Free Sugar Ceiling",
            formulaOrInput = "Max 5% of daily calories ÷ 4 kcal/g",
            resultValue = "< ${report.maxFreeSugarGrams}g / day",
          ),
          CalculationStepRow(
            stepLabel = "3. Daily Sodium Cap",
            formulaOrInput = "WHO cardiovascular protection limit",
            resultValue = "< ${formatSummaryInt(report.maxSodiumMg)} mg",
          ),
        ),
        scientificFootnote = "Hitting your fibre goal slows digestion, improves fullness on a cut, and stabilizes insulin response.",
      )
    }

    CalculationTopic.BMI -> {
      val hMeters = heightCm / 100f
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "ANTHROPOMETRY • BODY MASS INDEX",
        title = "How Your BMI Is Calculated",
        finalValueFormatted = String.format(Locale.US, "%.1f kg/m² (%s)", report.bmi, report.bmiCategoryStandard),
        summarySubtitle = "Ratio of your total body weight to your squared height in meters, evaluated on both WHO Standard and Asian scales.",
        gradientColors = listOf(StepsStart, StepsMid, StepsEnd),
        equationBanner = String.format(Locale.US, "%.1f kg ÷ (%.2f m)² = %.1f kg/m²", weightKg, hMeters, report.bmi),
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Body Weight",
            formulaOrInput = "Current recorded mass",
            resultValue = String.format(Locale.US, "%.1f kg", weightKg),
          ),
          CalculationStepRow(
            stepLabel = "2. Squared Height (m²)",
            formulaOrInput = String.format(Locale.US, "(%.1f cm ÷ 100)² = %.2f²", heightCm, hMeters),
            resultValue = String.format(Locale.US, "%.3f m²", hMeters * hMeters),
          ),
          CalculationStepRow(
            stepLabel = "3. Standard WHO Classification",
            formulaOrInput = "Normal corridor: 18.5 – 24.9 kg/m²",
            resultValue = report.bmiCategoryStandard,
          ),
          CalculationStepRow(
            stepLabel = "4. Asian Metabolic Scale",
            formulaOrInput = "Optimal corridor: 18.5 – 22.9 kg/m²",
            resultValue = report.bmiCategoryAsianWho,
          ),
        ),
        scientificFootnote = "Used alongside Effective Mass to prevent overestimating protein or water needs at higher body weights.",
      )
    }

    CalculationTopic.EFFECTIVE_MASS -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "MASS CALIBRATION • EFFECTIVE WEIGHT",
        title = "How Effective Mass Is Calculated",
        finalValueFormatted = String.format(Locale.US, "%.1f kg", report.effectiveWeightKg),
        summarySubtitle = "Metabolically active reference weight used to calculate accurate Protein and Hydration targets without distortion.",
        gradientColors = listOf(ProteinStart, ProteinMid, ProteinEnd),
        equationBanner = if (report.isEffectiveWeightAdjusted) {
          String.format(
            Locale.US,
            "%.1f + 0.25 × (%.1f - %.1f) = %.1f kg",
            report.referenceWeightKg,
            weightKg,
            report.referenceWeightKg,
            report.effectiveWeightKg,
          )
        } else {
          String.format(Locale.US, "Actual Mass = %.1f kg (No adjustment needed)", report.effectiveWeightKg)
        },
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Actual Scale Weight",
            formulaOrInput = "Recorded on Step 02",
            resultValue = String.format(Locale.US, "%.1f kg", weightKg),
          ),
          CalculationStepRow(
            stepLabel = "2. BMI 25 Reference Weight",
            formulaOrInput = String.format(Locale.US, "25 × (%.2f m)²", heightCm / 100f),
            resultValue = String.format(Locale.US, "%.1f kg", report.referenceWeightKg),
          ),
          CalculationStepRow(
            stepLabel = "3. Effective Mass Rule",
            formulaOrInput = if (report.isEffectiveWeightAdjusted) {
              "Adjusted because BMI > 27 without Body-Fat % override"
            } else {
              "Uses 100% of actual scale weight"
            },
            resultValue = String.format(Locale.US, "%.1f kg", report.effectiveWeightKg),
          ),
        ),
        scientificFootnote = "Adipose tissue requires less protein and water than skeletal muscle; Effective Mass keeps your targets realistic.",
      )
    }

    CalculationTopic.BMR -> {
      val sexConst = if (isMale) "+ 5" else "- 161"
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "RESTING ENGINE • BASAL METABOLIC RATE",
        title = "How Your BMR Is Calculated",
        finalValueFormatted = "${formatSummaryInt(report.bmrKcalRounded)} kcal / day",
        summarySubtitle = "The exact energy your organs, brain, heart, and cells burn in 24 hours of complete rest.",
        gradientColors = listOf(CaloriesStart, CaloriesMid, CaloriesEnd),
        equationBanner = if (report.leanBodyMassKg != null) {
          String.format(Locale.US, "370 + (21.6 × %.1f kg LBM) = %d kcal", report.leanBodyMassKg, report.bmrKcalRounded)
        } else {
          String.format(
            Locale.US,
            "(10 × %.1fkg) + (6.25 × %.1fcm) - (5 × %dy) %s = %d kcal",
            weightKg,
            heightCm,
            ageYears,
            sexConst,
            report.bmrKcalRounded,
          )
        },
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Mass Energy Term",
            formulaOrInput = if (report.leanBodyMassKg != null) {
              String.format(Locale.US, "Lean Body Mass: %.1f kg × 21.6", report.leanBodyMassKg)
            } else {
              String.format(Locale.US, "10 × %.1f kg weight", weightKg)
            },
            resultValue = if (report.leanBodyMassKg != null) {
              String.format(Locale.US, "%.0f kcal", 21.6f * report.leanBodyMassKg)
            } else {
              String.format(Locale.US, "%.0f kcal", 10f * weightKg)
            },
          ),
          CalculationStepRow(
            stepLabel = "2. Stature & Age Term",
            formulaOrInput = if (report.leanBodyMassKg != null) {
              "Base organ constant (+370 kcal)"
            } else {
              String.format(Locale.US, "(6.25 × %.1f cm) - (5 × %d yrs) %s", heightCm, ageYears, sexConst)
            },
            resultValue = if (report.leanBodyMassKg != null) {
              "+370 kcal"
            } else {
              String.format(Locale.US, "%+.0f kcal", 6.25f * heightCm - 5f * ageYears + if (isMale) 5f else -161f)
            },
          ),
          CalculationStepRow(
            stepLabel = "3. Protected Calorie Floor",
            formulaOrInput = "Higher of ${if (isMale) "1,500" else "1,200"} kcal or 90% BMR",
            resultValue = "${formatSummaryInt(report.calorieSafetyFloorKcal)} kcal",
          ),
        ),
        scientificFootnote = "Enable Body-Fat % Precision Override anytime if you want BMR computed directly from your lean muscle mass.",
      )
    }

    CalculationTopic.TDEE -> {
      CalculationBreakdownSpec(
        topic = topic,
        categoryTag = "TOTAL EXPENDITURE • DAILY ENERGY BURN",
        title = "How Your TDEE Is Calculated",
        finalValueFormatted = "${formatSummaryInt(report.tdeeKcalRounded)} kcal / day",
        summarySubtitle = "Your total 24-hour calorie burn combining Resting Metabolism (BMR), Exercise Schedule, and Daily Occupation Load.",
        gradientColors = listOf(HydrationStart, HydrationMid, HydrationEnd),
        equationBanner = String.format(
          Locale.US,
          "%d BMR × %.3f Activity Factor = %s kcal",
          report.bmrKcalRounded,
          report.totalActivityFactor,
          formatSummaryInt(report.tdeeKcalRounded),
        ),
        steps = listOf(
          CalculationStepRow(
            stepLabel = "1. Resting Burn (BMR)",
            formulaOrInput = report.bmrDescriptor,
            resultValue = "${formatSummaryInt(report.bmrKcalRounded)} kcal",
          ),
          CalculationStepRow(
            stepLabel = "2. Exercise Activity Tier",
            formulaOrInput = "${activityLevel.name.replace('_', ' ')} (${activityLevel.cadenceBadge})",
            resultValue = String.format(Locale.US, "%.3f PAL", report.basePal),
          ),
          CalculationStepRow(
            stepLabel = "3. Daily Occupation Add-On",
            formulaOrInput = "${occupationLoad.title} (${occupationLoad.subtitle})",
            resultValue = String.format(Locale.US, "+%.2f PAL", report.occupationPalAddOn),
          ),
          CalculationStepRow(
            stepLabel = "4. Total Daily Burn (TDEE)",
            formulaOrInput = String.format(Locale.US, "%d × %.3f (rounded to 5 kcal)", report.bmrKcalRounded, report.totalActivityFactor),
            resultValue = "${formatSummaryInt(report.tdeeKcalRounded)} kcal",
          ),
        ),
        scientificFootnote = "Eating at your TDEE maintains your current weight; eating below burns fat; eating above builds mass.",
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2-SECOND HOLD GESTURE MODIFIER WITH LIVE PROGRESS FEEDBACK
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Detects a normal tap (`onTap`) OR a quick press-and-hold (~0.55s)
 * while reporting live hold progress (`0f..1f`) so the card can render a sleek
 * progress bar while the user holds down.
 */
@Composable
internal fun Modifier.tapOrHoldForCalculation(
  onTap: () -> Unit,
  onHoldComplete: () -> Unit,
  onHoldProgress: (Float) -> Unit,
): Modifier {
  val currentOnTap by rememberUpdatedState(onTap)
  val currentOnHoldComplete by rememberUpdatedState(onHoldComplete)
  val currentOnHoldProgress by rememberUpdatedState(onHoldProgress)
  val coroutineScope = rememberCoroutineScope()

  return this.pointerInput(Unit) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false)
      var holdTriggered = false
      val holdJob = coroutineScope.launch {
        // Short 110ms threshold so normal taps/scrolls don't flash the hold bar,
        // followed by a fast 440ms fill (~0.55s total hold time for best UX)
        delay(110L)
        val totalSteps = 22
        val stepDelayMs = 20L // 22 * 20ms = 440ms + 110ms = 550ms
        for (step in 1..totalSteps) {
          currentOnHoldProgress(step.toFloat() / totalSteps.toFloat())
          delay(stepDelayMs)
        }
        holdTriggered = true
        currentOnHoldProgress(0f)
        currentOnHoldComplete()
      }
      val upEvent = waitForUpOrCancellation()
      holdJob.cancel()
      currentOnHoldProgress(0f)
      if (upEvent != null && !holdTriggered) {
        currentOnTap()
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// WELCOME-SCREEN-EXACT PROGRESS BARS & GEOMETRIC GAUGES (CIRCLE / HEXAGON / LINE)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Exact Welcome Screen (`MetabolicTelemetryCard`) Multicolor Lit Progress Bar
 * using `ObsidianTrack` (`#151820`), `ObsidianTrackBorder` (`#232834`),
 * 3-stop category gradient, and dimensional top-rim sheen.
 */
@Composable
internal fun WelcomeCategoryLitProgressBar(
  fraction: Float,
  gradientColors: List<Color>,
  barHeight: Dp,
  modifier: Modifier = Modifier,
) {
  val animatedFraction by animateFloatAsState(
    targetValue = fraction.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 380),
    label = "welcome_cat_bar_fraction",
  )

  Spacer(
    modifier = modifier
      .height(barHeight)
      .graphicsLayer()
      .drawWithCache {
        val trackHeight = size.height.coerceAtLeast(1f)
        val radius = trackHeight / 2f
        val cornerRadius = CornerRadius(radius, radius)
        val trackBorderBrush = Brush.horizontalGradient(
          colors = listOf(
            gradientColors.first().copy(alpha = 0.25f),
            ObsidianTrackBorder,
            gradientColors.last().copy(alpha = 0.22f),
          ),
        )
        val verticalLightingBrush = Brush.verticalGradient(
          0.0f to PureWhite.copy(alpha = 0.26f),
          0.42f to Color.Transparent,
          1.0f to ObsidianBlack.copy(alpha = 0.20f),
          startY = 0f,
          endY = trackHeight,
        )
        val topRimBrush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.32f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = (trackHeight * 0.65f).coerceAtLeast(1f),
        )
        val fillPath = Path()

        onDrawBehind {
          if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
          val progress = animatedFraction.coerceIn(0f, 1f)

          // 1. Recessed Obsidian Capsule Track
          drawRoundRect(
            color = ObsidianTrack,
            topLeft = Offset.Zero,
            size = Size(size.width, trackHeight),
            cornerRadius = cornerRadius,
          )
          drawRoundRect(
            brush = trackBorderBrush,
            topLeft = Offset.Zero,
            size = Size(size.width, trackHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
          )

          if (progress > 0.005f) {
            val minFill = (trackHeight * 0.55f).coerceAtMost(size.width)
            val fillWidth = (size.width * progress).coerceIn(minFill, size.width)
            fillPath.rewind()
            fillPath.addRoundRect(
              RoundRect(
                rect = Rect(0f, 0f, fillWidth, trackHeight),
                cornerRadius = cornerRadius,
              ),
            )

            // 2. Smooth 3-Stop Welcome Screen Category Gradient Fill
            drawPath(
              path = fillPath,
              brush = Brush.horizontalGradient(
                colors = gradientColors,
                startX = 0f,
                endX = fillWidth.coerceAtLeast(1f),
              ),
            )

            // 3. Dimensional Surface Lighting & Sheen
            clipPath(fillPath) {
              drawRect(
                brush = verticalLightingBrush,
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )
              val sheenCenter = fillWidth * 0.36f
              val sheenSpan = (fillWidth * 0.26f).coerceAtLeast(trackHeight * 2f)
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color.Transparent,
                    PureWhite.copy(alpha = 0.16f),
                    Color.Transparent,
                  ),
                  start = Offset(sheenCenter - sheenSpan, 0f),
                  end = Offset(sheenCenter + sheenSpan, trackHeight),
                ),
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )
            }

            // 4. Crisp Top Specular Capsule Rim
            drawRoundRect(
              brush = topRimBrush,
              topLeft = Offset(0.5f, 0.5f),
              size = Size((fillWidth - 1f).coerceAtLeast(1f), (trackHeight - 1f).coerceAtLeast(1f)),
              cornerRadius = cornerRadius,
              style = Stroke(width = 0.75.dp.toPx()),
            )
          }
        }
      },
  )
}

/**
 * Unified Horizontal Capsule Progress Bar with an Embedded Safe Range Corridor (`[safeMin..safeMax]`).
 * - Used identically across ALL 6 Daily Target categories (Calories, Protein, Carbs, Fats, Hydration, Steps).
 * - Scales the full track (`trackMax = max(safeMax * 1.28f, recommendedValue * 1.56f)`) so 100% of the
 *   recommended goal sits comfortably inside the Safe Range (~62%–66% of the bar width) instead of maxing
 *   out to the right edge.
 * - Draws the Safe Range corridor band and crisp floor/ceiling boundary ticks directly inside the bar.
 */
@Composable
internal fun SafeRangeCategoryProgressBar(
  currentValue: Int,
  recommendedValue: Int,
  safeMin: Int,
  safeMax: Int,
  safeRangeText: String,
  gradientColors: List<Color>,
  barHeight: Dp,
  holdProgress: Float = 0f,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val effectiveSafeMin = minOf(safeMin, safeMax).coerceAtLeast(0)
  val effectiveSafeMax = maxOf(safeMin, safeMax).coerceAtLeast(effectiveSafeMin + 1)
  val trackMax = maxOf(
    effectiveSafeMax * 1.28f,
    recommendedValue * 1.56f,
    currentValue * 1.08f,
    1f,
  )

  val targetFraction = if (holdProgress > 0.02f) {
    holdProgress.coerceIn(0f, 1f)
  } else {
    (currentValue.toFloat() / trackMax).coerceIn(0.08f, 0.96f)
  }
  val safeMinFraction = (effectiveSafeMin.toFloat() / trackMax).coerceIn(0.06f, 0.86f)
  val safeMaxFraction = (effectiveSafeMax.toFloat() / trackMax).coerceIn(safeMinFraction + 0.08f, 0.92f)
  val recFraction = (recommendedValue.toFloat() / trackMax).coerceIn(safeMinFraction, safeMaxFraction)

  val animatedFill by animateFloatAsState(
    targetValue = targetFraction,
    animationSpec = tween(durationMillis = if (holdProgress > 0.02f) 60 else 360),
    label = "safe_range_bar_fill",
  )

  val isBelowSafe = currentValue < effectiveSafeMin
  val isAboveSafe = currentValue > effectiveSafeMax
  val statusText = when {
    isBelowSafe -> "BELOW SAFE FLOOR"
    isAboveSafe -> "ABOVE SAFE CEILING"
    else -> "IN SAFE ZONE"
  }
  val statusColor = when {
    isBelowSafe || isAboveSafe -> Color(0xFFFBBF24)
    else -> SumZinc300
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((4.5f * metrics.scale).coerceIn(3.5f, 6f).dp),
  ) {
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(barHeight)
        .graphicsLayer()
        .drawWithCache {
          val trackHeight = size.height.coerceAtLeast(1f)
          val radius = trackHeight / 2f
          val cornerRadius = CornerRadius(radius, radius)
          val trackBorderBrush = Brush.horizontalGradient(
            colors = listOf(
              gradientColors.first().copy(alpha = 0.28f),
              ObsidianTrackBorder,
              gradientColors.last().copy(alpha = 0.25f),
            ),
          )
          val verticalLightingBrush = Brush.verticalGradient(
            0.0f to PureWhite.copy(alpha = 0.26f),
            0.42f to Color.Transparent,
            1.0f to ObsidianBlack.copy(alpha = 0.20f),
            startY = 0f,
            endY = trackHeight,
          )
          val topRimBrush = Brush.verticalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.34f),
              Color.Transparent,
            ),
            startY = 0f,
            endY = (trackHeight * 0.65f).coerceAtLeast(1f),
          )
          val trackClipPath = Path().apply {
            addRoundRect(
              RoundRect(
                rect = Rect(0f, 0f, size.width, trackHeight),
                cornerRadius = cornerRadius,
              ),
            )
          }
          val fillPath = Path()

          onDrawBehind {
            if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
            val progress = animatedFill.coerceIn(0f, 1f)
            val safeStartX = (size.width * safeMinFraction).coerceIn(0f, size.width)
            val safeEndX = (size.width * safeMaxFraction).coerceIn(safeStartX + 4f, size.width)
            val recX = (size.width * recFraction).coerceIn(0f, size.width)

            // 1. Recessed Obsidian Capsule Track
            drawRoundRect(
              color = ObsidianTrack,
              topLeft = Offset.Zero,
              size = Size(size.width, trackHeight),
              cornerRadius = cornerRadius,
            )

            // 2. Interior Safe Range Corridor Hatching Band (Visible even in the unfilled portion)
            clipPath(trackClipPath) {
              drawRect(
                color = PureWhite.copy(alpha = 0.08f),
                topLeft = Offset(safeStartX, 0f),
                size = Size((safeEndX - safeStartX).coerceAtLeast(2f), trackHeight),
              )
            }

            drawRoundRect(
              brush = trackBorderBrush,
              topLeft = Offset.Zero,
              size = Size(size.width, trackHeight),
              cornerRadius = cornerRadius,
              style = Stroke(width = 1.dp.toPx()),
            )

            // 3. Smooth 3-Stop Welcome Screen Category Gradient Fill (Sits around ~64% at 100% Recommended)
            if (progress > 0.005f) {
              val minFill = (trackHeight * 0.55f).coerceAtMost(size.width)
              val fillWidth = (size.width * progress).coerceIn(minFill, size.width)
              fillPath.rewind()
              fillPath.addRoundRect(
                RoundRect(
                  rect = Rect(0f, 0f, fillWidth, trackHeight),
                  cornerRadius = cornerRadius,
                ),
              )

              drawPath(
                path = fillPath,
                brush = Brush.horizontalGradient(
                  colors = gradientColors,
                  startX = 0f,
                  endX = fillWidth.coerceAtLeast(1f),
                ),
              )

              clipPath(fillPath) {
                drawRect(
                  brush = verticalLightingBrush,
                  topLeft = Offset.Zero,
                  size = Size(fillWidth, trackHeight),
                )
              }

              drawRoundRect(
                brush = topRimBrush,
                topLeft = Offset(0.5f, 0.5f),
                size = Size((fillWidth - 1f).coerceAtLeast(1f), (trackHeight - 1f).coerceAtLeast(1f)),
                cornerRadius = cornerRadius,
                style = Stroke(width = 0.75.dp.toPx()),
              )
            }

            // 4. Safe Range Floor & Ceiling Boundary Markers + Recommended Anchor Tick inside the bar
            clipPath(trackClipPath) {
              val tickStroke = 1.5.dp.toPx()
              // Safe Floor Tick
              drawLine(
                color = PureWhite.copy(alpha = 0.78f),
                start = Offset(safeStartX, 0f),
                end = Offset(safeStartX, trackHeight),
                strokeWidth = tickStroke,
              )
              // Safe Ceiling Tick
              drawLine(
                color = PureWhite.copy(alpha = 0.78f),
                start = Offset(safeEndX, 0f),
                end = Offset(safeEndX, trackHeight),
                strokeWidth = tickStroke,
              )
              // Recommended Goal Center Marker
              drawCircle(
                color = PureWhite,
                radius = (trackHeight * 0.22f).coerceIn(1.8f, 3.2f),
                center = Offset(recX, trackHeight / 2f),
              )
            }
          }
        },
    )

    // Safe Range Legend Row directly coupled to the progress bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "SAFE RANGE: $safeRangeText",
        color = SumZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(8.8f, 7.8f, 9.8f),
        letterSpacing = 0.3.sp,
      )
      Text(
        text = statusText,
        color = statusColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
        letterSpacing = 0.4.sp,
      )
    }
  }
}

/**
 * Welcome-Screen-Style Category Pip (Rounded Square with 3-stop linear gradient).
 */
@Composable
internal fun WelcomeCategoryPip(
  gradientColors: List<Color>,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val r = this.size.minDimension * 0.34f
    drawRoundRect(
      brush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset.Zero,
        end = Offset(this.size.width.coerceAtLeast(1f), this.size.height.coerceAtLeast(1f)),
      ),
      cornerRadius = CornerRadius(r, r),
    )
  }
}

/**
 * Geometric Progress Gauge (Hexagon or Circle Ring) using the exact Welcome Screen
 * category gradient and `ObsidianTrack` recessed track.
 */
@Composable
internal fun CategoryGeometricGauge(
  fraction: Float,
  gradientColors: List<Color>,
  isHexagon: Boolean,
  centerText: String,
  size: Dp,
  strokeWidth: Dp = 4.2.dp,
  modifier: Modifier = Modifier,
) {
  val animatedFraction by animateFloatAsState(
    targetValue = fraction.coerceIn(0.04f, 1f),
    animationSpec = tween(durationMillis = 420),
    label = "geo_gauge_fraction",
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val sw = strokeWidth.toPx()
      val pad = sw / 2f + 1.5.dp.toPx()
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val r = (this.size.minDimension / 2f - pad).coerceAtLeast(4f)
      val gradBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(0f, 0f),
        end = Offset(this.size.width, this.size.height),
      )

      if (isHexagon) {
        val hexPath = Path().apply {
          for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60.0) - 90.0)
            val px = cx + r * cos(angleRad).toFloat()
            val py = cy + r * sin(angleRad).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
          }
          close()
        }
        drawPath(
          path = hexPath,
          color = ObsidianTrack,
          style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
          path = hexPath,
          color = ObsidianTrackBorder,
          style = Stroke(width = sw + 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val perimeter = 6f * r
        val activeLen = (perimeter * animatedFraction).coerceAtLeast(2f)
        drawPath(
          path = hexPath,
          brush = gradBrush,
          style = Stroke(
            width = sw,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
              floatArrayOf(activeLen, perimeter),
              0f,
            ),
          ),
        )
      } else {
        val arcTopLeft = Offset(cx - r, cy - r)
        val arcSize = Size(r * 2f, r * 2f)
        drawArc(
          color = ObsidianTrack,
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = sw, cap = StrokeCap.Round),
        )
        drawArc(
          brush = gradBrush,
          startAngle = -90f,
          sweepAngle = 360f * animatedFraction,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = sw, cap = StrokeCap.Round),
        )
      }
    }

    Text(
      text = centerText,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp,
      letterSpacing = (-0.3).sp,
      maxLines = 1,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// HOLD-TO-REPEAT STEPPER BUTTON (Tap once or Hold to rapidly increase/decrease)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun HoldRepeatableStepperButton(
  label: String,
  onStep: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
  accentBorderColor: Color = SumZinc700,
  testTag: String? = null,
) {
  val currentOnStep by rememberUpdatedState(onStep)
  val coroutineScope = rememberCoroutineScope()
  var isPressed by remember { mutableStateOf(false) }

  val shape = RoundedCornerShape((8.5f * metrics.scale).dp)
  val bg by animateColorAsState(
    targetValue = if (isPressed) PureWhite else SumZinc850,
    animationSpec = tween(100),
    label = "stepper_bg_$label",
  )
  val txtColor by animateColorAsState(
    targetValue = if (isPressed) ObsidianBlack else PureWhite,
    animationSpec = tween(100),
    label = "stepper_txt_$label",
  )

  Box(
    modifier = modifier
      .clip(shape)
      .background(bg)
      .border(1.dp, if (isPressed) PureWhite else accentBorderColor, shape)
      .semantics {
        role = Role.Button
        contentDescription = label
      }
      .pointerInput(Unit) {
        awaitEachGesture {
          awaitFirstDown(requireUnconsumed = false)
          isPressed = true
          currentOnStep()
          val repeatJob = coroutineScope.launch {
            delay(320L)
            var ticks = 0
            while (true) {
              currentOnStep()
              ticks++
              val nextDelay = when {
                ticks > 18 -> 42L
                ticks > 7 -> 68L
                else -> 110L
              }
              delay(nextDelay)
            }
          }
          waitForUpOrCancellation()
          repeatJob.cancel()
          isPressed = false
        }
      }
      .padding(
        horizontal = (11f * metrics.scale).dp,
        vertical = (9f * metrics.scale).dp,
      )
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = txtColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(11.4f, 10.2f, 12.8f),
      maxLines = 1,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. WORKOUT VS REST DAY SPLIT & MEAL FREQUENCY ARCHITECTURE CARD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun ScheduleAndMealArchitectureCard(
  report: DailyTargetsCalculationReport,
  activityLevel: ActivityLevelOption = ActivityLevelOption.MODERATE,
  selectedWorkoutDays: Set<DayOfWeekOption> = emptySet(),
  inspectedDayOfWeek: DayOfWeekOption? = null,
  onSelectDayOfWeek: (DayOfWeekOption) -> Unit = {},
  scheduleCadence: ScheduleCadenceOption,
  splitDayFocus: SplitDayFocus,
  onSelectScheduleCadence: (ScheduleCadenceOption) -> Unit,
  onSelectSplitDayFocus: (SplitDayFocus) -> Unit,
  dayOne: DayOfWeekOption,
  mealFrequency: MealFrequencyOption,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val isSedentary = activityLevel == ActivityLevelOption.SEDENTARY
  val effectiveCadence = if (isSedentary) ScheduleCadenceOption.UNIFORM_DAILY else scheduleCadence

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("summary_schedule_meal_card"),
  ) {
    SummarySectionHeader(
      title = if (isSedentary) "Daily Schedule & Meal Architecture" else "Schedule Split & Meal Architecture",
      badgeText = "${mealFrequency.displayCount} MEALS/D • DAY 01 ${dayOne.shortCode}",
      badgeColor = SumZinc300,
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height((8f * metrics.scale).coerceIn(6f, 11f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .summaryCardSurface(metrics.cardCornerRadius, borderFlashProvider)
        .padding(metrics.cardPadding),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(9f, 15f).dp),
      ) {
        // Schedule Mode Selector: When Sedentary ("Little / No Exercise"), NEVER mention Workout / Rest Split!
        val visibleOptions = if (isSedentary) {
          listOf(ScheduleCadenceOption.UNIFORM_DAILY)
        } else {
          ScheduleCadenceOption.entries
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
        ) {
          visibleOptions.forEach { option ->
            val isSelected = option == effectiveCadence
            val pillShape = RoundedCornerShape((9f * metrics.scale).dp)
            val bg by animateColorAsState(
              targetValue = if (isSelected) PureWhite else SumZinc950,
              animationSpec = tween(200),
              label = "cadence_toggle_bg_${option.name}",
            )
            val txt by animateColorAsState(
              targetValue = if (isSelected) ObsidianBlack else PureWhite,
              animationSpec = tween(200),
              label = "cadence_toggle_txt_${option.name}",
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .background(bg)
                .border(
                  width = if (isSelected) 1.4.dp else 1.dp,
                  color = if (isSelected) PureWhite else SumZinc800,
                  shape = pillShape,
                )
                .clickable(role = Role.RadioButton) {
                  if (!isSedentary) {
                    onSelectScheduleCadence(option)
                  }
                }
                .padding(vertical = (10f * metrics.scale).dp, horizontal = 8.dp)
                .testTag("summary_cadence_${option.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (option == ScheduleCadenceOption.UNIFORM_DAILY) {
                  "Uniform Daily (7d)"
                } else {
                  "Workout / Rest Split"
                },
                color = txt,
                fontFamily = InterFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = metrics.spScaled(12.2f, 10.8f, 13.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // Interactive Workout Day vs Rest Day Comparison Matrix + Day-by-Day Schedule Strip
        if (!isSedentary && effectiveCadence == ScheduleCadenceOption.REST_DAYS && report.trainingDaysPerWeek > 0) {
          val activeWorkoutSet = if (selectedWorkoutDays.isNotEmpty()) {
            selectedWorkoutDays
          } else {
            report.selectedWorkoutDays
          }

          // 7-Day Weekly Schedule Strip (Tap any day of the week to calculate all categories for that day)
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "WEEKLY WORKOUT & REST DAY MAP",
                color = SumZinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(9.0f, 8.0f, 10.0f),
                letterSpacing = 0.6.sp,
              )
              Text(
                text = if (inspectedDayOfWeek != null) {
                  val isWork = inspectedDayOfWeek in activeWorkoutSet
                  "${inspectedDayOfWeek.fullName.uppercase()} • ${if (isWork) "WORKOUT" else "REST"}"
                } else {
                  "${report.trainingDaysPerWeek} WORKOUT • ${report.restDaysPerWeek} REST"
                },
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(9.0f, 8.0f, 10.0f),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy((5f * metrics.scale).dp),
            ) {
              DayOfWeekOption.entries.forEach { day ->
                val isWorkoutDay = day in activeWorkoutSet
                val isDayFocused = if (inspectedDayOfWeek != null) {
                  day == inspectedDayOfWeek
                } else {
                  (isWorkoutDay && splitDayFocus == SplitDayFocus.WORKOUT_DAY) ||
                    (!isWorkoutDay && splitDayFocus == SplitDayFocus.REST_DAY)
                }
                val dayShape = RoundedCornerShape((8f * metrics.scale).dp)
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .clip(dayShape)
                    .background(if (isDayFocused) PureWhite else SumZinc950)
                    .border(
                      width = if (isDayFocused) 1.3.dp else 0.9.dp,
                      color = if (isDayFocused) PureWhite else SumZinc800,
                      shape = dayShape,
                    )
                    .clickable(role = Role.RadioButton) {
                      onSelectDayOfWeek(day)
                    }
                    .padding(vertical = (7f * metrics.scale).dp)
                    .testTag("summary_day_chip_${day.shortCode.lowercase()}"),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = day.shortCode,
                    color = if (isDayFocused) ObsidianBlack else PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(9.4f, 8.2f, 10.4f),
                  )
                  Text(
                    text = if (isWorkoutDay) "WORK" else "REST",
                    color = if (isDayFocused) ObsidianBlack.copy(alpha = 0.75f) else SumZinc400,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = metrics.spScaled(7.2f, 6.4f, 8.2f),
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
          ) {
            DayTypeSplitTile(
              badge = "WORKOUT DAYS (${report.trainingDaysPerWeek}D)",
              isSelected = splitDayFocus == SplitDayFocus.WORKOUT_DAY,
              calories = "${formatSummaryInt(report.workoutDayCaloriesKcal)} kcal",
              macrosLine = "P ${report.recommendedProteinGrams}g • C ${report.workoutDayCarbsGrams}g • F ${report.recommendedFatsGrams}g",
              waterAndStepsLine = "${formatSummaryInt(report.workoutDayWaterMl)} ml • ${formatSummaryInt(report.workoutDaySteps)} steps",
              onClick = { onSelectSplitDayFocus(SplitDayFocus.WORKOUT_DAY) },
              metrics = metrics,
              modifier = Modifier
                .weight(1f)
                .testTag("split_tile_workout_day"),
            )
            DayTypeSplitTile(
              badge = "REST DAYS (${report.restDaysPerWeek}D)",
              isSelected = splitDayFocus == SplitDayFocus.REST_DAY,
              calories = "${formatSummaryInt(report.restDayCaloriesKcal)} kcal",
              macrosLine = "P ${report.recommendedProteinGrams}g • C ${report.restDayCarbsGrams}g • F ${report.recommendedFatsGrams}g",
              waterAndStepsLine = "${formatSummaryInt(report.restDayWaterMl)} ml • ${formatSummaryInt(report.restDaySteps)} steps",
              onClick = { onSelectSplitDayFocus(SplitDayFocus.REST_DAY) },
              metrics = metrics,
              modifier = Modifier
                .weight(1f)
                .testTag("split_tile_rest_day"),
            )
          }
        } else {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape((10f * metrics.scale).dp))
              .background(SumZinc950)
              .border(1.dp, SumZinc800, RoundedCornerShape((10f * metrics.scale).dp))
              .padding((12f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                  drawCircle(color = PureWhite)
                }
                Text(
                  text = "UNIFORM 7-DAY SCHEDULE ACTIVE",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
                  letterSpacing = 0.6.sp,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Consistent ${formatSummaryInt(report.uniformCaloriesKcal)} kcal, ${report.uniformCarbsGrams}g carbs, ${formatSummaryInt(report.uniformDayWaterMl)} ml water, and ${formatSummaryInt(report.uniformSteps)} steps every day of the week.",
                color = SumZinc300,
                fontFamily = InterFontFamily,
                fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
                lineHeight = metrics.spScaled(16.2f, 14.5f, 18.0f),
              )
            }
          }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF20242E))

        // Per-Meal Split Breakdown (Main Meals vs Snacks)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "PER-MEAL FEEDING BREAKDOWN",
              color = SumZinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
              letterSpacing = 0.7.sp,
            )
            Text(
              text = "~${report.proteinPerMealGrams}g P • ~${report.carbsPerMealGrams}g C • ~${report.fatsPerMealGrams}g F",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
          ) {
            MealSplitStatBox(
              label = "${report.mainMealsCount}× MAIN MEALS",
              primaryValue = "~${report.caloriesPerMainMealKcal} kcal",
              secondaryValue = "per main meal",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
            if (report.snackMealsCount > 0) {
              MealSplitStatBox(
                label = "${report.snackMealsCount}× SNACKS",
                primaryValue = "~${report.caloriesPerSnackKcal} kcal",
                secondaryValue = "per snack",
                metrics = metrics,
                modifier = Modifier.weight(1f),
              )
            }
            MealSplitStatBox(
              label = "PROTEIN / MEAL",
              primaryValue = "~${report.proteinPerMealGrams}g",
              secondaryValue = "${report.mealCount} meals / day",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DayTypeSplitTile(
  badge: String,
  isSelected: Boolean,
  calories: String,
  macrosLine: String,
  waterAndStepsLine: String,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape((10.5f * metrics.scale).dp)
  val borderColor = if (isSelected) PureWhite else SumZinc800
  val bgColor = if (isSelected) Color(0xFF171922) else SumZinc950

  Column(
    modifier = modifier
      .clip(shape)
      .background(bgColor)
      .border(if (isSelected) 1.5.dp else 1.dp, borderColor, shape)
      .clickable(role = Role.RadioButton, onClick = onClick)
      .padding((11f * metrics.scale).dp),
    verticalArrangement = Arrangement.spacedBy(4.5.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = badge,
        color = if (isSelected) PureWhite else SumZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(9.4f, 8.4f, 10.6f),
        maxLines = 1,
      )
      if (isSelected) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PureWhite)
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
        ) {
          Text(
            text = "ACTIVE",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(7.8f, 7.0f, 8.8f),
          )
        }
      }
    }
    Text(
      text = calories,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(16.5f, 14.5f, 18.5f),
    )
    Text(
      text = macrosLine,
      color = SumZinc200,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = waterAndStepsLine,
      color = SumZinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = metrics.spScaled(9.6f, 8.6f, 10.8f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MealSplitStatBox(
  label: String,
  primaryValue: String,
  secondaryValue: String,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape((9.5f * metrics.scale).dp)
  Column(
    modifier = modifier
      .clip(shape)
      .background(SumZinc950)
      .border(1.dp, SumZinc800, shape)
      .padding(horizontal = (10f * metrics.scale).dp, vertical = (9f * metrics.scale).dp),
    verticalArrangement = Arrangement.spacedBy(2.5.dp),
  ) {
    Text(
      text = label,
      color = SumZinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(9.0f, 8.0f, 10.0f),
      maxLines = 1,
    )
    Text(
      text = primaryValue,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(15.0f, 13.2f, 16.8f),
      maxLines = 1,
    )
    Text(
      text = secondaryValue,
      color = SumZinc400,
      fontFamily = InterFontFamily,
      fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. SCIENTIFIC METABOLIC ENGINE AUDIT CARD (Supports 2s Hold Calculation View)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun ScientificMetabolicAuditCard(
  report: DailyTargetsCalculationReport,
  bodyFatOverrideEnabled: Boolean,
  bodyFatPercent: Float,
  onToggleBodyFatOverride: (Boolean) -> Unit,
  onBodyFatPercentChange: (Float) -> Unit,
  onInspectCalculation: (CalculationTopic) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("summary_metabolic_audit_card"),
  ) {
    SummarySectionHeader(
      title = "Metabolic Baseline & Body Composition",
      badgeText = "HOLD TILE FOR MATH",
      badgeColor = SumZinc400,
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height((8f * metrics.scale).coerceIn(6f, 11f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .summaryCardSurface(metrics.cardCornerRadius, borderFlashProvider)
        .padding(metrics.cardPadding),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((10f * metrics.scale).coerceIn(8f, 13f).dp),
      ) {
        // 2x2 Core Biometric Grid: BMI, Effective Mass, BMR, TDEE
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
        ) {
          AuditFormulaTile(
            metricTag = "BODY MASS INDEX",
            value = String.format(Locale.US, "%.1f kg/m²", report.bmi),
            subline = report.bmiCategoryStandard,
            extraNote = "Asian Scale: ${report.bmiCategoryAsianWho}",
            gradientColors = listOf(StepsStart, StepsMid, StepsEnd),
            onInspectCalculation = { onInspectCalculation(CalculationTopic.BMI) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
          AuditFormulaTile(
            metricTag = "EFFECTIVE MASS",
            value = String.format(Locale.US, "%.1f kg", report.effectiveWeightKg),
            subline = if (report.isEffectiveWeightAdjusted) {
              "Lean-Adjusted Mass"
            } else {
              "Actual Body Mass"
            },
            extraNote = "Ref Mass: ${String.format(Locale.US, "%.1f", report.referenceWeightKg)} kg",
            gradientColors = listOf(ProteinStart, ProteinMid, ProteinEnd),
            onInspectCalculation = { onInspectCalculation(CalculationTopic.EFFECTIVE_MASS) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
        ) {
          AuditFormulaTile(
            metricTag = "BASAL METABOLIC RATE",
            value = "${formatSummaryInt(report.bmrKcalRounded)} kcal",
            subline = report.bmrDescriptor,
            extraNote = "Safe Floor: ${formatSummaryInt(report.calorieSafetyFloorKcal)} kcal",
            gradientColors = listOf(CaloriesStart, CaloriesMid, CaloriesEnd),
            onInspectCalculation = { onInspectCalculation(CalculationTopic.BMR) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
          AuditFormulaTile(
            metricTag = "DAILY ENERGY BURN",
            value = "${formatSummaryInt(report.tdeeKcalRounded)} kcal",
            subline = String.format(Locale.US, "BMR × %.2f Activity", report.totalActivityFactor),
            extraNote = String.format(
              Locale.US,
              "Base %.2f + Work %.2f",
              report.basePal,
              report.occupationPalAddOn,
            ),
            gradientColors = listOf(HydrationStart, HydrationMid, HydrationEnd),
            onInspectCalculation = { onInspectCalculation(CalculationTopic.TDEE) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF20242E))

        // Optional Body-Fat % Precision Tuning (with Hold-to-Repeat buttons)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "BODY-FAT % PRECISION OVERRIDE",
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(12.2f, 10.8f, 13.6f),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Optional: Enable if you know your body-fat % to compute BMR directly from lean muscle mass.",
                color = SumZinc400,
                fontFamily = InterFontFamily,
                fontSize = metrics.spScaled(10.8f, 9.6f, 12.0f),
                lineHeight = metrics.spScaled(15.0f, 13.4f, 16.6f),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (bodyFatOverrideEnabled) PureWhite else SumZinc950)
                .border(1.dp, if (bodyFatOverrideEnabled) PureWhite else SumZinc700, RoundedCornerShape(8.dp))
                .clickable(role = Role.Switch) { onToggleBodyFatOverride(!bodyFatOverrideEnabled) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("toggle_katch_mcardle"),
            ) {
              Text(
                text = if (bodyFatOverrideEnabled) "ACTIVE" else "OFF",
                color = if (bodyFatOverrideEnabled) ObsidianBlack else SumZinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(10.4f, 9.2f, 11.6f),
              )
            }
          }

          if (bodyFatOverrideEnabled) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(SumZinc950)
                .border(1.dp, SumZinc800, RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = "BODY FAT: ${String.format(Locale.US, "%.1f%%", bodyFatPercent)}",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = metrics.spScaled(13.0f, 11.6f, 14.4f),
                )
                Text(
                  text = "Hold buttons to fast-adjust",
                  color = SumZinc400,
                  fontFamily = InterFontFamily,
                  fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HoldRepeatableStepperButton(
                  label = "-1%",
                  onStep = { onBodyFatPercentChange((bodyFatPercent - 1f).coerceIn(5f, 55f)) },
                  metrics = metrics,
                )
                HoldRepeatableStepperButton(
                  label = "-0.5%",
                  onStep = { onBodyFatPercentChange((bodyFatPercent - 0.5f).coerceIn(5f, 55f)) },
                  metrics = metrics,
                )
                HoldRepeatableStepperButton(
                  label = "+0.5%",
                  onStep = { onBodyFatPercentChange((bodyFatPercent + 0.5f).coerceIn(5f, 55f)) },
                  metrics = metrics,
                )
                HoldRepeatableStepperButton(
                  label = "+1%",
                  onStep = { onBodyFatPercentChange((bodyFatPercent + 1f).coerceIn(5f, 55f)) },
                  metrics = metrics,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuditFormulaTile(
  metricTag: String,
  value: String,
  subline: String,
  extraNote: String,
  gradientColors: List<Color>,
  onInspectCalculation: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  var holdProgress by remember { mutableFloatStateOf(0f) }
  val shape = RoundedCornerShape((10.5f * metrics.scale).dp)
  val borderColor = if (holdProgress > 0.02f) PureWhite else SumZinc800

  Column(
    modifier = modifier
      .clip(shape)
      .background(SumZinc950)
      .border(if (holdProgress > 0.02f) 1.4.dp else 1.dp, borderColor, shape)
      .tapOrHoldForCalculation(
        onTap = {},
        onHoldComplete = onInspectCalculation,
        onHoldProgress = { holdProgress = it },
      )
      .padding((11f * metrics.scale).dp),
    verticalArrangement = Arrangement.spacedBy(3.5.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        WelcomeCategoryPip(
          gradientColors = gradientColors,
          size = (6.5f * metrics.scale).dp,
        )
        Text(
          text = metricTag,
          color = SumZinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
          maxLines = 1,
        )
      }
      if (holdProgress > 0.02f) {
        Text(
          text = "${(holdProgress * 100).roundToInt()}%",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
        )
      }
    }
    Text(
      text = value,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(16.0f, 14.0f, 18.0f),
    )
    Text(
      text = subline,
      color = SumZinc200,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(11.0f, 9.8f, 12.2f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = extraNote,
      color = SumZinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = metrics.spScaled(9.4f, 8.4f, 10.6f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (holdProgress > 0.02f) {
      Spacer(modifier = Modifier.height(2.dp))
      WelcomeCategoryLitProgressBar(
        fraction = holdProgress,
        gradientColors = gradientColors,
        barHeight = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. CROSS-DEVICE SELECTION SUMMARY CARDS (HIGH-VISIBILITY B&W BENTO GRID)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun CrossDeviceSelectionsSummarySection(
  subjectName: String,
  selectedSex: String,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  ageYears: Int,
  heightCm: Float,
  weightKg: Float,
  report: DailyTargetsCalculationReport,
  activityLevel: ActivityLevelOption,
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  dayOne: DayOfWeekOption,
  scheduleCadence: ScheduleCadenceOption,
  mealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
  streakThreshold: StreakThresholdOption,
  onTeleportToStep01: () -> Unit,
  onTeleportToStep02: () -> Unit,
  onTeleportToStep03: (focusSection: String?) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val monthNames = remember {
    listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
  }
  val dobFormatted = remember(birthDay, birthMonthZeroBased, birthYear) {
    val m = monthNames.getOrElse(birthMonthZeroBased.coerceIn(0, 11)) { "OCT" }
    String.format(Locale.US, "%02d %s %04d", birthDay, m, birthYear)
  }
  val totalInches = heightCm / 2.54f
  val feet = (totalInches / 12f).toInt()
  val remInches = (totalInches % 12f).roundToInt()
  val weightLbs = weightKg * 2.20462f

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("cross_device_selections_summary"),
    verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(10f, 15f).dp),
  ) {
    SummarySectionHeader(
      title = "Your Profile & Blueprint (Tap Any to Edit)",
      badgeText = "1-TAP TELEPORT EDIT",
      badgeColor = SumZinc300,
      metrics = metrics,
    )

    // ── STEP 01 SUMMARY CARD (2x2 Bento Quick-Edit Grid) ──
    TeleportStepSummaryCard(
      stepBadge = "STEP 01",
      stepTitle = "Identity & Age",
      editButtonTag = "teleport_edit_step_01",
      onEditClick = onTeleportToStep01,
      borderFlashProvider = borderFlashProvider,
      metrics = metrics,
      items = listOf(
        SummaryKeyValueItem("Subject Name", subjectName.ifBlank { "Unassigned" }, onTeleportToStep01),
        SummaryKeyValueItem("Biological Sex", if (selectedSex.equals("MALE", true)) "Male (XY)" else "Female (XX)", onTeleportToStep01),
        SummaryKeyValueItem("Date of Birth", dobFormatted, onTeleportToStep01),
        SummaryKeyValueItem("Current Age", "$ageYears years", onTeleportToStep01),
      ),
    )

    // ── STEP 02 SUMMARY CARD (2x2 Bento Quick-Edit Grid) ──
    TeleportStepSummaryCard(
      stepBadge = "STEP 02",
      stepTitle = "Body Measurements",
      editButtonTag = "teleport_edit_step_02",
      onEditClick = onTeleportToStep02,
      borderFlashProvider = borderFlashProvider,
      metrics = metrics,
      items = listOf(
        SummaryKeyValueItem(
          "Height",
          String.format(Locale.US, "%.1f cm (%d'%d\")", heightCm, feet, remInches),
          onTeleportToStep02,
        ),
        SummaryKeyValueItem(
          "Weight",
          String.format(Locale.US, "%.1f kg (%.1f lbs)", weightKg, weightLbs),
          onTeleportToStep02,
        ),
        SummaryKeyValueItem(
          "BMI Status",
          String.format(Locale.US, "%.1f • %s", report.bmi, report.bmiCategoryStandard),
          onTeleportToStep02,
        ),
        SummaryKeyValueItem(
          "Effective Mass",
          String.format(Locale.US, "%.1f kg", report.effectiveWeightKg),
          onTeleportToStep02,
        ),
      ),
    )

    // ── STEP 03 SUMMARY CARD (2x6 Bento Quick-Edit Grid) ──
    TeleportStepSummaryCard(
      stepBadge = "STEP 03",
      stepTitle = "Training, Nutrition & Lifestyle",
      editButtonTag = "teleport_edit_step_03",
      onEditClick = { onTeleportToStep03(null) },
      borderFlashProvider = borderFlashProvider,
      metrics = metrics,
      items = listOf(
        SummaryKeyValueItem(
          "Primary Goal",
          if (report.objectiveWasOverridden) {
            "${report.effectiveObjective.name.replace('_', ' ')} (Protected)"
          } else {
            objective.name.replace('_', ' ')
          },
          onRowClick = { onTeleportToStep03("objective") },
        ),
        SummaryKeyValueItem(
          "Target Pace",
          "${targetPace.title} (${targetPace.weeklyRateKg})",
          onRowClick = { onTeleportToStep03("pace") },
        ),
        SummaryKeyValueItem(
          "Activity Level",
          if (activityLevel == ActivityLevelOption.SEDENTARY) {
            "SEDENTARY (0W / 7R)"
          } else {
            val wDays = DayOfWeekOption.entries.filter { it in report.selectedWorkoutDays }
              .joinToString(",") { it.shortCode }
            "${activityLevel.name.replace('_', ' ')} (${report.trainingDaysPerWeek}W/${report.restDaysPerWeek}R: $wDays)"
          },
          onRowClick = { onTeleportToStep03("activity") },
        ),
        SummaryKeyValueItem(
          "Daily Work Load",
          occupationLoad.title,
          onRowClick = { onTeleportToStep03("activity") },
        ),
        SummaryKeyValueItem(
          "Workout Style",
          trainingModality.shortLabel,
          onRowClick = { onTeleportToStep03("training") },
        ),
        SummaryKeyValueItem(
          "Session Duration",
          "${workoutDuration.displayHours} / day",
          onRowClick = { onTeleportToStep03("training") },
        ),
        SummaryKeyValueItem(
          "Experience",
          experienceLevel.name,
          onRowClick = { onTeleportToStep03("training") },
        ),
        SummaryKeyValueItem(
          "Day 01 & Schedule",
          if (activityLevel == ActivityLevelOption.SEDENTARY || scheduleCadence == ScheduleCadenceOption.UNIFORM_DAILY) {
            "${dayOne.shortCode} • Uniform"
          } else {
            "${dayOne.shortCode} • Workout/Rest"
          },
          onRowClick = { onTeleportToStep03("pace") },
        ),
        SummaryKeyValueItem(
          "Meals / Day",
          "${mealFrequency.displayCount} meals (${mealFrequency.cadenceTag})",
          onRowClick = { onTeleportToStep03("nutrition") },
        ),
        SummaryKeyValueItem(
          "Coffee / Alcohol",
          if (caffeineAlcoholYes) "Yes (+300 ml H2O)" else "No (+0 ml)",
          onRowClick = { onTeleportToStep03("nutrition") },
        ),
        SummaryKeyValueItem(
          "Fast Food / Salt",
          sodiumFrequency.title,
          onRowClick = { onTeleportToStep03("nutrition") },
        ),
        SummaryKeyValueItem(
          "Streak Goal",
          "${streakThreshold.percentage}% (${streakThreshold.tierTitle})",
          onRowClick = { onTeleportToStep03("streak") },
        ),
      ),
    )
  }
}

internal data class SummaryKeyValueItem(
  val label: String,
  val value: String,
  val onRowClick: (() -> Unit)? = null,
)

@Composable
private fun TeleportStepSummaryCard(
  stepBadge: String,
  stepTitle: String,
  editButtonTag: String,
  onEditClick: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
  items: List<SummaryKeyValueItem>,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .summaryCardSurface(metrics.cardCornerRadius, borderFlashProvider)
      .padding(metrics.cardPadding),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy((10f * metrics.scale).dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(SumZinc850)
              .border(1.dp, SumZinc700, RoundedCornerShape(5.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp),
          ) {
            Text(
              text = stepBadge,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
            )
          }
          Text(
            text = stepTitle,
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(14.2f, 12.6f, 16.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Teleport Edit Button -> Jumps to that page & returns directly to Step 04
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onEditClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .testTag(editButtonTag),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "EDIT ↗",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
            letterSpacing = 0.3.sp,
          )
        }
      }

      // 2-Column Bento Chip Grid of Editable Values with Generous Text Sizes
      val rows = items.chunked(2)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).dp),
      ) {
        rows.forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).dp),
          ) {
            pair.forEach { item ->
              val chipShape = RoundedCornerShape((9f * metrics.scale).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(chipShape)
                  .background(SumZinc950)
                  .border(1.dp, SumZinc800, chipShape)
                  .clickable(role = Role.Button) {
                    (item.onRowClick ?: onEditClick).invoke()
                  }
                  .padding(
                    horizontal = (10f * metrics.scale).dp,
                    vertical = (8.5f * metrics.scale).dp,
                  ),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = item.label.uppercase(Locale.US),
                    color = SumZinc400,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = metrics.spScaled(9.0f, 8.0f, 10.2f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "✎",
                    color = SumZinc400,
                    fontSize = metrics.spScaled(9.5f, 8.5f, 10.8f),
                  )
                }
                Text(
                  text = item.value,
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(11.8f, 10.5f, 13.2f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. CALCULATION BREAKDOWN MODAL (SHOWN ON 2–3 SECOND HOLD)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun CalculationBreakdownDialog(
  spec: CalculationBreakdownSpec,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()

    Column(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF161922), Color(0xFF0E1016), Color(0xFF090A0E)),
          )
        )
        .border(1.3.dp, PureWhite.copy(alpha = 0.34f), shape)
        .padding(20.dp)
        .testTag("calculation_breakdown_dialog"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Eyebrow & Close Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          WelcomeCategoryPip(
            gradientColors = spec.gradientColors,
            size = 9.dp,
          )
          Text(
            text = spec.categoryTag,
            color = SumZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.9.sp,
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SumZinc850)
            .border(1.dp, SumZinc700, RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = "CLOSE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
          )
        }
      }

      // Main Title & Final Calculated Value
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = spec.title,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 18.5.sp,
          letterSpacing = (-0.3).sp,
        )
        Text(
          text = spec.finalValueFormatted,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 22.sp,
        )
      }

      // Welcome-Screen-Style Category Lit Progress Bar
      WelcomeCategoryLitProgressBar(
        fraction = 1.0f,
        gradientColors = spec.gradientColors,
        barHeight = 8.dp,
        modifier = Modifier.fillMaxWidth(),
      )

      Text(
        text = spec.summarySubtitle,
        color = SumZinc300,
        fontFamily = InterFontFamily,
        fontSize = 12.4.sp,
        lineHeight = 17.5.sp,
      )

      // Master Equation Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ObsidianBlack)
          .border(1.dp, PureWhite.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "LIVE FORMULA",
            color = SumZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.2.sp,
            letterSpacing = 0.8.sp,
          )
          Text(
            text = spec.equationBanner,
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.2.sp,
            lineHeight = 17.sp,
          )
        }
      }

      // Step-by-Step Inputs & Math List
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        spec.steps.forEach { row ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(SumZinc950)
              .border(1.dp, SumZinc800, RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
              verticalArrangement = Arrangement.spacedBy(2.5.dp),
            ) {
              Text(
                text = row.stepLabel,
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.2.sp,
              )
              Text(
                text = row.formulaOrInput,
                color = SumZinc400,
                fontFamily = InterFontFamily,
                fontSize = 11.0.sp,
                lineHeight = 15.sp,
              )
            }
            Text(
              text = row.resultValue,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
            )
          }
        }
      }

      Text(
        text = spec.scientificFootnote,
        color = SumZinc400,
        fontFamily = InterFontFamily,
        fontSize = 11.2.sp,
        lineHeight = 15.5.sp,
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(11.dp))
          .background(PureWhite)
          .clickable(role = Role.Button, onClick = onDismiss)
          .padding(vertical = 13.dp)
          .testTag("calculation_breakdown_got_it_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Got It",
          color = ObsidianBlack,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. DIRECT NUMBER EDIT DIALOG & SIMPLE REAL-LIFE HEALTH IMPACT MODAL
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun DirectTargetValueEditDialog(
  metricTitle: String,
  unitLabel: String,
  currentValue: Int,
  recommendedValue: Int,
  safeRangeLabel: String,
  minAllowed: Int,
  maxAllowed: Int,
  onDismiss: () -> Unit,
  onConfirmValue: (Int) -> Unit,
) {
  var textValue by remember(currentValue) { mutableStateOf(currentValue.toString()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val shape = RoundedCornerShape(16.dp)
    Column(
      modifier = Modifier
        .fillMaxWidth(0.90f)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF161922), Color(0xFF0A0B0E)),
          )
        )
        .border(1.2.dp, PureWhite.copy(alpha = 0.30f), shape)
        .padding(20.dp)
        .testTag("direct_target_edit_dialog"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "CUSTOM TARGET EDIT",
        color = SumZinc300,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        letterSpacing = 1.1.sp,
      )
      Text(
        text = "Set $metricTitle ($unitLabel)",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.5.sp,
      )
      Text(
        text = "Recommended: $recommendedValue $unitLabel (Healthy range: $safeRangeLabel)",
        color = SumZinc300,
        fontFamily = InterFontFamily,
        fontSize = 12.0.sp,
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ObsidianBlack)
          .border(1.2.dp, PureWhite.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
          .padding(horizontal = 14.dp, vertical = 12.dp),
      ) {
        BasicTextField(
          value = textValue,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(6)
            textValue = digits
          },
          textStyle = TextStyle(
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
          ),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          cursorBrush = SolidColor(PureWhite),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("direct_target_input_field"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(SumZinc850)
            .border(1.dp, SumZinc700, RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) {
              onConfirmValue(recommendedValue)
            }
            .padding(vertical = 12.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Reset ($recommendedValue)",
            color = SumZinc200,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(PureWhite)
            .clickable(role = Role.Button) {
              val parsed = textValue.toIntOrNull() ?: currentValue
              onConfirmValue(parsed.coerceIn(minAllowed, maxAllowed))
            }
            .padding(vertical = 12.5.dp)
            .testTag("direct_target_apply_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Save Target",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
          )
        }
      }
    }
  }
}

@Composable
internal fun HealthImpactDisclaimerDialog(
  alerts: List<HealthImpactAlert>,
  onSyncCaloriesFromMacros: () -> Unit,
  onRestoreRecommended: () -> Unit,
  onAcknowledgeAndKeepCustom: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val shape = RoundedCornerShape(18.dp)
    val scrollState = rememberScrollState()
    val hasCritical = alerts.any { it.severity == HealthAlertSeverity.CRITICAL }
    val hasSyncMismatch = alerts.any { it.id == "macro_calorie_mismatch" }
    val headerAccent = if (hasCritical) Color(0xFFF43F5E) else Color(0xFFFF8E4D)

    Column(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF171820), Color(0xFF0A0A0D)),
          )
        )
        .border(1.3.dp, PureWhite.copy(alpha = 0.32f), shape)
        .padding(18.dp)
        .testTag("health_impact_disclaimer_dialog"),
      verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = headerAccent)
          }
          Text(
            text = "HEALTH & ENERGY CHECK",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.8.sp,
            letterSpacing = 1.0.sp,
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(PureWhite)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
          Text(
            text = "${alerts.size} NOTICE${if (alerts.size > 1) "S" else ""}",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
          )
        }
      }

      Text(
        text = "How Your Custom Edits Affect Your Body",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.5.sp,
      )

      Text(
        text = "Here is a simple summary of how your custom numbers can feel day-to-day:",
        color = SumZinc300,
        fontFamily = InterFontFamily,
        fontSize = 12.2.sp,
        lineHeight = 17.sp,
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        alerts.forEach { alert ->
          val itemShape = RoundedCornerShape(11.dp)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(itemShape)
              .background(ObsidianBlack)
              .border(1.dp, SumZinc700, itemShape)
              .padding(12.5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = alert.metricName.uppercase(Locale.US),
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
              )
              Text(
                text = "${alert.userValueFormatted}  •  ${alert.recommendedRangeFormatted}",
                color = SumZinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.0.sp,
              )
            }

            Text(
              text = alert.headline,
              color = alert.severity.accentColor,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 13.2.sp,
            )

            Text(
              text = alert.physiologicalImpact,
              color = SumZinc200,
              fontFamily = InterFontFamily,
              fontSize = 12.0.sp,
              lineHeight = 16.8.sp,
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SumZinc950)
                .border(1.dp, SumZinc800, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.5.dp),
            ) {
              Text(
                text = alert.realLifeExamples,
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.6.sp,
                lineHeight = 16.2.sp,
              )
            }
          }
        }
      }

      if (hasSyncMismatch) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(SumZinc850)
            .border(1.dp, PureWhite, RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onSyncCaloriesFromMacros)
            .padding(vertical = 12.5.dp)
            .testTag("disclaimer_sync_calories_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Match Calories to My Macros",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
          )
        }
      }

      // Primary 1-Tap Restore Recommended Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(11.dp))
          .background(PureWhite)
          .clickable(role = Role.Button, onClick = onRestoreRecommended)
          .padding(vertical = 13.5.dp)
          .testTag("disclaimer_restore_recommended_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Use Recommended Healthy Targets",
          color = ObsidianBlack,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
        )
      }

      // Secondary Keep Custom Override Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(11.dp))
          .background(SumZinc900)
          .border(1.dp, SumZinc700, RoundedCornerShape(11.dp))
          .clickable(role = Role.Button, onClick = onAcknowledgeAndKeepCustom)
          .padding(vertical = 11.5.dp)
          .testTag("disclaimer_keep_custom_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Keep My Custom Numbers Anyway",
          color = SumZinc300,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.5.sp,
        )
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// SHARED SECTION HEADER & WELCOME-SCREEN-MATCHED CARD SURFACE MODIFIER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun SummarySectionHeader(
  title: String,
  badgeText: String,
  badgeColor: Color,
  metrics: BlueprintResponsiveMetrics,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f, fill = false),
    ) {
      BlueprintLuminousWhiteDot(
        size = (6.5f * metrics.scale).coerceIn(5.5f, 8f).dp,
      )
      Text(
        text = title,
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(14.5f, 13.0f, 16.5f),
        letterSpacing = (-0.2).sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = badgeText,
      color = badgeColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(9.4f, 8.4f, 10.6f),
      letterSpacing = 0.6.sp,
      maxLines = 1,
    )
  }
}

/**
 * Matches the Welcome Screen (`MetabolicTelemetryCard`) sculpted dark obsidian
 * gradient (`#161922 -> #0E1016 -> #090A0E`) and specular top-edge architectural border.
 */
internal fun Modifier.summaryCardSurface(
  cornerRadiusDp: Dp,
  borderFlashProvider: () -> Float,
): Modifier = this.drawWithCache {
  val cornerPx = cornerRadiusDp.toPx()
  val cornerRadius = CornerRadius(cornerPx, cornerPx)
  val bgBrush = Brush.verticalGradient(
    0.0f to SumCardTop,
    0.38f to SumCardMid,
    1.0f to SumCardBottom,
  )
  onDrawBehind {
    val flash = sin(borderFlashProvider().coerceIn(0f, 1f) * Math.PI.toFloat()).coerceIn(0f, 1f)
    drawRoundRect(
      brush = bgBrush,
      topLeft = Offset.Zero,
      size = size,
      cornerRadius = cornerRadius,
    )
    if (flash > 0.01f) {
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.10f * flash),
            Color.Transparent,
          ),
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius,
      )
    }
    drawRoundRect(
      brush = Brush.verticalGradient(
        0.0f to PureWhite.copy(alpha = 0.24f + 0.56f * flash),
        0.28f to Color(0xFF282E3D).copy(alpha = 0.85f + 0.15f * flash),
        0.80f to Color(0xFF181B24),
        1.0f to Color(0xFF0F1117),
      ),
      topLeft = Offset.Zero,
      size = size,
      cornerRadius = cornerRadius,
      style = Stroke(width = (1.2f + 0.5f * flash).dp.toPx()),
    )
  }
}
