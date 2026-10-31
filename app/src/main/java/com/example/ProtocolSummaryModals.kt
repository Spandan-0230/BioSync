package com.example

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.FatsMid
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProteinMid
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsMid
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ModalZinc100 = Color(0xFFF4F4F5)
private val ModalZinc300 = Color(0xFFD4D4D8)
private val ModalZinc400 = Color(0xFFA1A1AA)
private val ModalZinc500 = Color(0xFF71717A)
private val ModalZinc700 = Color(0xFF3F3F46)
private val ModalZinc800 = Color(0xFF27272A)
private val ModalZinc850 = Color(0xFF1C1C20)
private val ModalZinc900 = Color(0xFF131317)
private val ModalZinc950 = Color(0xFF0A0A0D)

enum class FormulaInspectionTopic(
  val title: String,
  val badge: String,
  val accentColor: Color,
) {
  CALORIES(
    title = "Daily Energy Goal (Calories)",
    badge = "ENERGY TARGET",
    accentColor = CaloriesStart,
  ),
  PROTEIN(
    title = "Daily Protein Target",
    badge = "MUSCLE & RECOVERY",
    accentColor = ProteinStart,
  ),
  CARBS(
    title = "Daily Carbohydrate Target",
    badge = "BRAIN & WORKOUT FUEL",
    accentColor = CarbsStart,
  ),
  FATS(
    title = "Daily Healthy Fats Target",
    badge = "HORMONES & VITAMins",
    accentColor = FatsStart,
  ),
  HYDRATION(
    title = "Daily Hydration Target",
    badge = "CELLULAR FLUID",
    accentColor = HydrationStart,
  ),
  STEPS(
    title = "Daily Steps Target",
    badge = "DAILY MOVEMENT",
    accentColor = StepsStart,
  ),
  BMI(
    title = "Body Mass Index (BMI)",
    badge = "ANTHROPOMETRIC RATIO",
    accentColor = ProteinStart,
  ),
  EFFECTIVE_WEIGHT(
    title = "Effective Metabolic Weight",
    badge = "DOSING MASS",
    accentColor = StepsStart,
  ),
  BMR(
    title = "Resting Metabolic Rate (BMR)",
    badge = "BASE ORGAN BURN",
    accentColor = CarbsStart,
  ),
  TDEE(
    title = "Total Daily Energy Burn (TDEE)",
    badge = "MAINTENANCE BURN",
    accentColor = CaloriesStart,
  ),
}

/**
 * Custom modifier that detects both a normal tap and a short press-and-hold (~420ms)
 * so the user doesn't have to wait 2-3 seconds for the calculation sheet to appear.
 * Uses requireUnconsumed = true so child stepper buttons (+ / −) never trigger formula popups.
 */
@Composable
fun Modifier.shortHoldOrTap(
  holdDurationMs: Long = 420L,
  onTap: (() -> Unit)? = null,
  onShortHold: () -> Unit,
): Modifier {
  val currentOnTap by rememberUpdatedState(onTap)
  val currentOnShortHold by rememberUpdatedState(onShortHold)

  return this.pointerInput(holdDurationMs) {
    coroutineScope {
      awaitEachGesture {
        awaitFirstDown(requireUnconsumed = true)
        var holdTriggered = false
        val holdJob = launch {
          delay(holdDurationMs)
          holdTriggered = true
          currentOnShortHold()
        }
        val up = waitForUpOrCancellation()
        holdJob.cancel()
        if (up != null && !holdTriggered) {
          currentOnTap?.invoke()
        }
      }
    }
  }
}

/**
 * Press-and-hold repeating stepper button with minimized initial delay (260ms)
 * and smooth acceleration so holding increases/decreases values rapidly without
 * ever triggering the parent formula calculation popup.
 * Uses a Pure White block for "+" and a Light Grey block for "−".
 */
@Composable
fun HoldRepeatStepperButton(
  symbol: String,
  accentColor: Color,
  onStep: (multiplier: Int) -> Unit,
  haptics: BioSyncHapticController,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 36.dp,
  testTag: String = "",
) {
  val currentOnStep by rememberUpdatedState(onStep)
  var isPressed by remember { mutableStateOf(false) }
  val isMinus = symbol == "−" || symbol == "-"
  val symbolTextColor = if (isMinus) PureWhite else ObsidianBlack

  val bgBrush = when {
    isMinus && isPressed -> Brush.verticalGradient(
      colors = listOf(
        Color(0xFF2E2E2E),
        Color(0xFF1F1F1F),
      )
    )
    isMinus -> Brush.verticalGradient(
      colors = listOf(
        Color(0xFF242424),
        Color(0xFF242424),
      )
    )
    isPressed -> Brush.verticalGradient(
      colors = listOf(
        Color(0xFFE4E4E7),
        Color(0xFFD4D4D8),
      )
    )
    else -> Brush.verticalGradient(
      colors = listOf(
        PureWhite,
        Color(0xFFF4F4F5),
      )
    )
  }
  val borderColor by animateColorAsState(
    targetValue = when {
      isPressed -> accentColor
      isMinus -> Color(0xFF36363A)
      else -> PureWhite
    },
    animationSpec = tween(durationMillis = 120),
    label = "stepper_border_color",
  )

  Box(
    modifier = modifier
      .size(buttonSize.coerceAtLeast(36.dp))
      .clip(RoundedCornerShape(9.dp))
      .background(bgBrush)
      .border(1.dp, borderColor, RoundedCornerShape(9.dp))
      .pointerInput(Unit) {
        coroutineScope {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            isPressed = true
            haptics.step()
            currentOnStep(1)
            val repeatJob = launch {
              delay(260L)
              var tick = 0
              while (true) {
                val stepMultiplier = when {
                  tick > 18 -> 4
                  tick > 8 -> 2
                  else -> 1
                }
                haptics.step()
                currentOnStep(stepMultiplier)
                tick++
                val nextDelay = when {
                  tick > 18 -> 42L
                  tick > 8 -> 65L
                  else -> 95L
                }
                delay(nextDelay)
              }
            }
            val up = waitForUpOrCancellation()
            up?.consume()
            repeatJob.cancel()
            isPressed = false
          }
        }
      }
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = symbol,
      color = symbolTextColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun FormulaInspectionDialog(
  topic: FormulaInspectionTopic,
  report: DailyTargetsCalculationReport,
  ageYears: Int,
  biologicalSex: String,
  heightCm: Float,
  weightKg: Float,
  activityLevel: ActivityLevelOption,
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
  onDismiss: () -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
  val heightM = (heightCm / 100f).coerceAtLeast(1.0f)

  val primaryValueText: String
  val safeRangeText: String
  val formulaEquation: String
  val stepByStepLines: List<Pair<String, String>>
  val simpleExplanation: String

  when (topic) {
    FormulaInspectionTopic.CALORIES -> {
      primaryValueText = "${numberFormat.format(report.recommendedCaloriesKcal)} kcal/day"
      safeRangeText = "${numberFormat.format(report.calorieSafetyFloorKcal)} – ${numberFormat.format(report.calorieSafeCeilingKcal)} kcal"
      val deltaSign = if (report.calorieDeltaFromTdeeKcal >= 0) "+" else ""
      formulaEquation = "Daily Goal = Daily Burn (TDEE) ${if (report.calorieDeltaFromTdeeKcal >= 0) "+" else "−"} Goal Adjustment"
      stepByStepLines = listOf(
        "Resting BMR" to "${numberFormat.format(report.bmrKcalRounded)} kcal/day",
        "Activity Multiplier" to "×${String.format(Locale.US, "%.3f", report.totalActivityFactor)} (${activityLevel.name.lowercase().replaceFirstChar { it.uppercase() }})",
        "Daily Burn (TDEE)" to "${numberFormat.format(report.tdeeKcalRounded)} kcal/day",
        "Objective & Pace (${report.effectiveObjective.name.replace('_', ' ')})" to "$deltaSign${report.calorieDeltaFromTdeeKcal} kcal/day",
        "Minimum Safe Floor" to "${numberFormat.format(report.calorieSafetyFloorKcal)} kcal/day",
      )
      simpleExplanation = "Your daily calorie goal starts with how much energy your body burns at rest (BMR), multiplies it by your weekly movement and workout schedule (TDEE), and adjusts for your ${report.effectiveObjective.name.replace('_', ' ').lowercase()} goal while staying above your safe minimum energy floor."
    }

    FormulaInspectionTopic.PROTEIN -> {
      primaryValueText = "${report.recommendedProteinGrams}g / day (${report.proteinCalorieSharePct}%)"
      safeRangeText = "${report.proteinFloorGrams}g – ${report.proteinCeilingGrams}g / day"
      formulaEquation = "Protein = Effective Weight × ${String.format(Locale.US, "%.2f", report.proteinFactorGPerKgEff)} g/kg"
      stepByStepLines = listOf(
        "Effective Dosing Weight" to "${String.format(Locale.US, "%.1f", report.effectiveWeightKg)} kg",
        "Goal & Experience Factor" to "${String.format(Locale.US, "%.2f", report.proteinFactorGPerKgEff)} g/kg (${experienceLevel.name.lowercase().replaceFirstChar { it.uppercase() }})",
        "Per-Meal Protein (${report.mealCount} meals)" to "~${report.proteinPerMealGrams}g / meal",
        "Safe Scientific Range" to "${report.proteinFloorGrams}g to ${report.proteinCeilingGrams}g",
      )
      simpleExplanation = "Protein is scaled to your effective body weight, training experience, and age group so your muscles recover properly and stay protected without overloading your digestion."
    }

    FormulaInspectionTopic.CARBS -> {
      primaryValueText = "${report.recommendedCarbsGrams}g / day (${report.carbsCalorieSharePct}%)"
      safeRangeText = "${report.carbsFloorGrams}g – ${report.carbsCeilingGrams}g / day"
      formulaEquation = "Carbs = (Calories − Protein kcal − Fat kcal) ÷ 4"
      stepByStepLines = listOf(
        "Daily Calorie Goal" to "${numberFormat.format(report.recommendedCaloriesKcal)} kcal",
        "Minus Protein (${report.recommendedProteinGrams}g × 4)" to "−${report.recommendedProteinGrams * 4} kcal",
        "Minus Healthy Fats (${report.recommendedFatsGrams}g × 9)" to "−${report.recommendedFatsGrams * 9} kcal",
        "Remaining Carb Energy" to "${report.recommendedCarbsGrams * 4} kcal ÷ 4 = ${report.recommendedCarbsGrams}g",
        "Brain Glucose Floor" to "Minimum 130g / day",
      )
      simpleExplanation = "Carbohydrates fill your remaining daily energy budget after protein and essential fats are locked in, keeping at least 130g/day to fuel your brain and muscle glycogen."
    }

    FormulaInspectionTopic.FATS -> {
      primaryValueText = "${report.recommendedFatsGrams}g / day (${report.fatCalorieSharePct}%)"
      safeRangeText = "${report.fatFloorGrams}g – ${report.fatCeilingGrams}g / day"
      formulaEquation = "Fats = (${report.fatTargetSharePct}% × Daily Calories) ÷ 9 kcal/g"
      stepByStepLines = listOf(
        "Target Energy Share" to "${report.fatTargetSharePct}% of ${numberFormat.format(report.recommendedCaloriesKcal)} kcal",
        "Calorie Density" to "9 kcal per gram of fat",
        "Hormone Protection Floor" to "${report.fatFloorGrams}g minimum",
        "Upper Safe Ceiling" to "${report.fatCeilingGrams}g maximum",
      )
      simpleExplanation = "Healthy fats are set to ${report.fatTargetSharePct}% of your daily calories (with a strict floor for ${biologicalSex.lowercase()} hormone health and vitamin absorption)."
    }

    FormulaInspectionTopic.HYDRATION -> {
      primaryValueText = "${numberFormat.format(report.primaryRecommendedWaterMl)} ml / day"
      safeRangeText = "${numberFormat.format(report.waterFloorMl)} – ${numberFormat.format(report.waterCeilingMl)} ml"
      formulaEquation = "Water = Base Body Need + Work & Lifestyle + Workout Sweat"
      val weeklyAvgExerciseMl = ((report.trainingDaysPerWeek / 7f) * report.exerciseWaterAddOnWorkoutDayMl).toInt()
      stepByStepLines = listOf(
        "Base Body Fluid (${String.format(Locale.US, "%.1f", report.effectiveWeightKg)} kg)" to "${numberFormat.format(report.baseWaterMl)} ml",
        "Occupation & Environment" to "+${report.occupationWaterAddMl + report.climateWaterAddMl} ml",
        "Caffeine/Alcohol & Sodium Flush" to "+${report.caffeineWaterAddMl + report.sodiumWaterAddMl} ml",
        "Workout Sweat Allowance (${report.trainingDaysPerWeek}d/wk)" to "+$weeklyAvgExerciseMl ml/day avg",
        "Daily Sip Rhythm" to "~${String.format(Locale.US, "%.1f", report.waterGlasses250Ml)} glasses (${report.wakingHourlyReminderMl} ml/hr)",
      )
      simpleExplanation = "Your hydration goal combines your body-weight fluid baseline with extra water for your job activity, workout sweat rate (${trainingModality.shortLabel}, ${workoutDuration.displayHours}), caffeine/alcohol, and salty meals."
    }

    FormulaInspectionTopic.STEPS -> {
      primaryValueText = "${numberFormat.format(report.recommendedSteps)} steps / day"
      safeRangeText = "${numberFormat.format(report.stepsFloor)} – ${numberFormat.format(report.stepsCeiling)} steps"
      formulaEquation = "Steps = Activity Tier Base + Goal Adjustment"
      stepByStepLines = listOf(
        "Activity Tier (${activityLevel.name})" to "Base tier cadence",
        "Goal Adjustment (${report.effectiveObjective.name.replace('_', ' ')})" to "Tuned for recovery & burn",
        "Estimated Stride Length" to "${String.format(Locale.US, "%.2f", report.strideLengthMeters)} m",
        "Estimated Walking Distance" to "${String.format(Locale.US, "%.1f", report.estimatedWalkingKm)} km / day (~${report.informationalWalkingKcal} kcal)",
      )
      simpleExplanation = "Daily steps keep your non-exercise calorie burn steady and improve blood sugar control without over-fatiguing your legs before workouts."
    }

    FormulaInspectionTopic.BMI -> {
      primaryValueText = "${String.format(Locale.US, "%.1f", report.bmi)} kg/m²"
      safeRangeText = "18.5 – 24.9 kg/m² (Standard)"
      formulaEquation = "BMI = Weight (kg) ÷ [Height (m)]²"
      stepByStepLines = listOf(
        "Body Weight" to "${String.format(Locale.US, "%.1f", weightKg)} kg",
        "Height in Meters" to "${String.format(Locale.US, "%.2f", heightM)} m (${String.format(Locale.US, "%.1f", heightCm)} cm)",
        "Standard Classification" to report.bmiCategoryStandard,
        "Asian WHO Classification" to report.bmiCategoryAsianWho,
      )
      simpleExplanation = "BMI measures your weight relative to height to help calibrate safe deficit/surplus rates and determine whether effective weight adjustment is needed."
    }

    FormulaInspectionTopic.EFFECTIVE_WEIGHT -> {
      primaryValueText = "${String.format(Locale.US, "%.1f", report.effectiveWeightKg)} kg"
      safeRangeText = "Actual: ${String.format(Locale.US, "%.1f", report.actualWeightKg)} kg"
      formulaEquation = if (report.isEffectiveWeightAdjusted) {
        "W_eff = W_ref + 0.25 × (Actual Weight − W_ref)"
      } else {
        "W_eff = Actual Body Weight"
      }
      stepByStepLines = listOf(
        "Actual Weight" to "${String.format(Locale.US, "%.1f", report.actualWeightKg)} kg",
        "Reference Weight (BMI 25)" to "${String.format(Locale.US, "%.1f", report.referenceWeightKg)} kg",
        "Adjustment Status" to if (report.isEffectiveWeightAdjusted) "Adjusted (BMI > 27)" else "Direct 1:1 Dosing",
      )
      simpleExplanation = "Protein and hydration needs are driven primarily by lean tissue rather than stored fat. When BMI is above 27, effective weight prevents over-prescribing protein and water."
    }

    FormulaInspectionTopic.BMR -> {
      primaryValueText = "${numberFormat.format(report.bmrKcalRounded)} kcal / day"
      safeRangeText = report.bmrDescriptor
      formulaEquation = if (report.leanBodyMassKg != null) {
        "BMR = 370 + (21.6 × Lean Body Mass kg)"
      } else {
        "BMR = (10 × Weight) + (6.25 × Height) − (5 × Age) ${if (biologicalSex.equals("MALE", true)) "+ 5" else "− 161"}"
      }
      stepByStepLines = listOf(
        "Biological Sex & Age" to "$biologicalSex • $ageYears yrs",
        "Stature & Mass" to "${String.format(Locale.US, "%.1f", heightCm)} cm • ${String.format(Locale.US, "%.1f", weightKg)} kg",
        "Calculated Resting Burn" to "${numberFormat.format(report.bmrKcalRounded)} kcal/day",
      )
      simpleExplanation = "Your Resting Metabolic Rate (BMR) is the energy your brain, heart, lungs, and cells burn every 24 hours just to keep you alive at complete rest."
    }

    FormulaInspectionTopic.TDEE -> {
      primaryValueText = "${numberFormat.format(report.tdeeKcalRounded)} kcal / day"
      safeRangeText = "Multiplier ×${String.format(Locale.US, "%.3f", report.totalActivityFactor)}"
      formulaEquation = "TDEE = Resting BMR × Total Activity Factor"
      stepByStepLines = listOf(
        "Resting BMR" to "${numberFormat.format(report.bmrKcalRounded)} kcal/day",
        "Activity Tier (${activityLevel.name})" to "${String.format(Locale.US, "%.3f", report.basePal)} (${report.trainingDaysPerWeek}W / ${report.restDaysPerWeek}R)",
        "Occupation Add-On (${occupationLoad.title})" to "+${String.format(Locale.US, "%.2f", report.occupationPalAddOn)}",
        "Total Activity Factor" to "×${String.format(Locale.US, "%.3f", report.totalActivityFactor)}",
      )
      simpleExplanation = "Total Daily Energy Expenditure (TDEE) combines your resting organ burn with your weekly workout schedule and daily job movement to estimate your true maintenance calories."
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(RoundedCornerShape(18.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(ModalZinc900, ModalZinc950)
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              topic.accentColor.copy(alpha = 0.65f),
              ModalZinc800,
            )
          ),
          shape = RoundedCornerShape(18.dp),
        )
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
        .testTag("formula_inspection_dialog"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(topic.accentColor),
          )
          Text(
            text = topic.badge,
            color = topic.accentColor,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            letterSpacing = 1.2.sp,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ModalZinc850)
            .border(1.dp, ModalZinc700, RoundedCornerShape(6.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
          Text(
            text = "CLOSE",
            color = ModalZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = topic.title,
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Target + Safe Range Highlight Box
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianBlack)
          .border(1.dp, ModalZinc800, RoundedCornerShape(12.dp))
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "CALCULATED VALUE",
            color = ModalZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = primaryValueText,
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "SAFE RANGE",
            color = ModalZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.5.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = safeRangeText,
            color = topic.accentColor,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Formula Box
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ModalZinc850.copy(alpha = 0.65f))
          .border(1.dp, ModalZinc800, RoundedCornerShape(10.dp))
          .padding(12.dp),
      ) {
        Text(
          text = "HOW IT IS CALCULATED",
          color = ModalZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 9.5.sp,
          letterSpacing = 1.0.sp,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = formulaEquation,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          lineHeight = 17.sp,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Step-by-step inputs
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ObsidianBlack)
          .border(1.dp, ModalZinc800, RoundedCornerShape(10.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        stepByStepLines.forEachIndexed { idx, (label, value) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = label,
              color = ModalZinc400,
              fontFamily = InterFontFamily,
              fontSize = 12.sp,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = value,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              textAlign = TextAlign.End,
            )
          }
          if (idx < stepByStepLines.lastIndex) {
            HorizontalDivider(color = ModalZinc850, thickness = 0.8.dp)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = simpleExplanation,
        color = ModalZinc300,
        fontFamily = InterFontFamily,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(PureWhite)
          .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Got It",
          color = ObsidianBlack,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
        )
      }
    }
  }
}

/**
 * Pop-up Health Impact & Disclaimer Modal in plain, everyday language with real-life examples.
 * Shown when user taps "Health Impact Guide" or when locking in custom targets that are out of safe range.
 */
@Composable
fun HealthImpactDisclaimerDialog(
  alerts: List<HealthImpactAlert>,
  isLockInConfirmation: Boolean,
  onRestoreRecommended: () -> Unit,
  onConfirmProceedCustom: () -> Unit,
  onDismiss: () -> Unit,
  onFixSingleMetric: ((String) -> Unit)? = null,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.93f)
        .heightIn(max = 620.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(ModalZinc900, ModalZinc950)
          )
        )
        .border(
          width = 1.2.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              if (alerts.isNotEmpty()) Color(0xFFF97316).copy(alpha = 0.8f) else StepsStart.copy(alpha = 0.7f),
              ModalZinc800,
            )
          ),
          shape = RoundedCornerShape(18.dp),
        )
        .padding(20.dp)
        .testTag("health_impact_disclaimer_dialog"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (alerts.isNotEmpty()) "HEALTH IMPACT DISCLAIMER" else "SAFE RANGE HEALTH GUIDE",
          color = if (alerts.isNotEmpty()) Color(0xFFFBBF24) else StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 1.2.sp,
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ModalZinc850)
            .border(1.dp, ModalZinc700, RoundedCornerShape(6.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
          Text(
            text = "CLOSE",
            color = ModalZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (alerts.isNotEmpty()) {
          "Your Custom Targets Are Outside the Safe Range"
        } else {
          "How Too High or Too Low Targets Affect Your Body"
        },
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = if (alerts.isNotEmpty()) {
          "Here is what your current custom targets can do to your daily energy, mood, and health in simple words:"
        } else {
          "Your targets are currently in the safe zone. Here is why staying inside the recommended range matters:"
        },
        color = ModalZinc400,
        fontFamily = InterFontFamily,
        fontSize = 12.5.sp,
        lineHeight = 17.5.sp,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (alerts.isNotEmpty()) {
          alerts.forEach { alert ->
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBlack)
                .border(
                  width = 1.dp,
                  color = alert.severity.accentColor.copy(alpha = 0.55f),
                  shape = RoundedCornerShape(12.dp),
                )
                .padding(13.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = alert.metricName.uppercase(Locale.US),
                    color = alert.severity.accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                  )
                  Text(
                    text = "${alert.userValueFormatted} (${alert.recommendedRangeFormatted})",
                    color = ModalZinc300,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                  )
                }
                if (onFixSingleMetric != null) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(PureWhite)
                      .clickable { onFixSingleMetric(alert.id) }
                      .padding(horizontal = 9.dp, vertical = 4.dp),
                  ) {
                    Text(
                      text = "Fix to Safe",
                      color = ObsidianBlack,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.5.sp,
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(5.dp))
              Text(
                text = alert.headline,
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = alert.physiologicalImpact,
                color = ModalZinc300,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
              )
              Spacer(modifier = Modifier.height(5.dp))
              Text(
                text = alert.realLifeExamples,
                color = alert.severity.accentColor.copy(alpha = 0.95f),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 17.sp,
              )
            }
          }
        } else {
          // Reference guide when user opens guide while in safe range
          val referenceItems = listOf(
            Triple(
              "Carbohydrates (Too High vs Too Low)",
              "Over carbs causes big blood sugar swings; too few carbs drains brain & muscle fuel.",
              "Real-life feel: Over-carbs makes you sleepy and drowsy after meals (food coma). Low carbs causes brain fog, headaches, and flat workouts.",
            ),
            Triple(
              "Protein (Too High vs Too Low)",
              "Too little protein breaks down muscle tissue; extreme protein strains digestion.",
              "Real-life feel: Low protein leaves you sore for days and hungry. Over-protein causes stomach bloating, heavy digestion, and thirst.",
            ),
            Triple(
              "Calories (Too High vs Too Low)",
              "Cutting too hard burns muscle and slows metabolism; overeating stores body fat.",
              "Real-life feel: Too low causes dizziness, cold hands, and night cravings. Too high causes sluggishness and unwanted belly fat.",
            ),
            Triple(
              "Fats & Hydration (Out of Balance)",
              "Low fat hurts hormones & skin; low water thickens blood and slows recovery.",
              "Real-life feel: Low fat causes dry skin and low mood. Low water causes afternoon headaches, muscle cramps, and fatigue.",
            ),
          )
          referenceItems.forEach { (heading, body, example) ->
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBlack)
                .border(1.dp, ModalZinc800, RoundedCornerShape(12.dp))
                .padding(12.dp),
            ) {
              Text(
                text = heading,
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = body,
                color = ModalZinc400,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = example,
                color = ModalZinc100,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (alerts.isNotEmpty()) {
        // Primary action: Restore Safe Recommended Targets
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onRestoreRecommended)
            .testTag("disclaimer_restore_recommended_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Use Recommended Safe Targets",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ModalZinc850)
            .border(1.dp, ModalZinc700, RoundedCornerShape(10.dp))
            .clickable(
              role = Role.Button,
              onClick = if (isLockInConfirmation) onConfirmProceedCustom else onDismiss,
            )
            .testTag("disclaimer_keep_custom_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (isLockInConfirmation) {
              "I Understand • Lock In Custom Targets Anyway"
            } else {
              "I Understand • Keep Editing Custom Targets"
            },
            color = ModalZinc300,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp,
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onDismiss),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Got It",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
          )
        }
      }
    }
  }
}

private val ModalGlowEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Pure Black & White Obsidian modal displayed when the user attempts to lock in their protocol
 * on Step 04 without having enabled "Allow all" in Health Connect settings.
 * Blocks entry to the Dashboard until "Allow all" is granted.
 */
@Composable
fun HealthConnectPermissionPromptDialog(
  dailyStepsGoal: Int,
  isHealthConnectAllowAllGranted: Boolean,
  hasActivityRecognitionPermission: Boolean,
  isIgnoringBatteryOptimizations: Boolean,
  showAllowAllRequiredWarning: Boolean,
  onConfirmAndConnect: () -> Unit,
  onOpenHealthConnectSettings: () -> Unit,
  onRequestBatteryOptimizationExemption: () -> Unit,
  onDismiss: () -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
  val coroutineScope = rememberCoroutineScope()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val borderFlashPhase = remember { Animatable(0f) }
  val buttonCorner = 10.dp
  val buttonShape = RoundedCornerShape(buttonCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "hc_confirm_connect_glow")
  val shimmerPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "hc_confirm_connect_shimmer",
  )
  val shimmerProvider = remember { { shimmerPhase } }
  val borderFlashProvider = remember { { borderFlashPhase.value } }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
      usePlatformDefaultWidth = false,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(RoundedCornerShape(18.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF111114),
              ObsidianBlack,
            ),
          ),
        )
        .border(
          width = 1.2.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.38f),
              Color(0xFF27272A),
            ),
          ),
          shape = RoundedCornerShape(18.dp),
        )
        .padding(20.dp)
        .testTag("health_connect_permission_dialog"),
    ) {
      // Top Eyebrow Bar (Pure Black & White Monochrome)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Canvas(modifier = Modifier.size(7.dp)) {
            drawCircle(color = PureWhite)
          }
          Text(
            text = "HEALTH CONNECT & BACKGROUND SYNC",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.2.sp,
            letterSpacing = 1.1.sp,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF242424))
            .border(1.dp, Color(0xFF36363A), RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("health_connect_dialog_close_btn"),
        ) {
          Text(
            text = "CLOSE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.8.sp,
            letterSpacing = 0.6.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Headline
      Text(
        text = "Connect to Health Connect",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = (-0.3).sp,
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Primary Message requested by user
      Text(
        text = "To get all the accurate data, we need to connect to your Health Connect app. Select \"Allow all\" in the settings to share the data and track it live inside the BioSync app.",
        color = ModalZinc300,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Monochrome 3-Item Permission & Background Sync Matrix
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianBlack)
          .border(1.dp, ModalZinc800, RoundedCornerShape(12.dp))
          .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MonochromeInstructionRow(
          stepCode = "01",
          title = "Health Connect • Select \"Allow all\"",
          subtitle = "Enable Allow all in settings to sync your ${numberFormat.format(dailyStepsGoal)} daily steps goal with Google Fit and mobile health apps.",
          isGranted = isHealthConnectAllowAllGranted,
        )

        HorizontalDivider(thickness = 0.5.dp, color = ModalZinc850)

        MonochromeInstructionRow(
          stepCode = "02",
          title = "Physical Activity & Live Sensors",
          subtitle = "Grants access to your mobile device's hardware step counter for real-time tracking.",
          isGranted = hasActivityRecognitionPermission,
        )

        HorizontalDivider(thickness = 0.5.dp, color = ModalZinc850)

        MonochromeInstructionRow(
          stepCode = "03",
          title = "Unrestricted Background & Battery",
          subtitle = "Allows BioSync to avoid battery optimization restrictions and collect live data continuously in the background.",
          isGranted = isIgnoringBatteryOptimizations,
        )
      }

      if (showAllowAllRequiredWarning) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16161A))
            .border(1.dp, PureWhite.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        ) {
          Text(
            text = "ACTION REQUIRED • PERMISSIONS INCOMPLETE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.8.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Dashboard entry is locked until Health Connect \"Allow all\", physical activity sensors, and unrestricted background battery access are enabled.",
            color = ModalZinc300,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (!isHealthConnectAllowAllGranted) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF242424))
                  .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                  .clickable(role = Role.Button, onClick = onOpenHealthConnectSettings)
                  .padding(vertical = 8.dp, horizontal = 8.dp)
                  .testTag("open_hc_settings_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "Health Connect Settings",
                  color = PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.5.sp,
                  textAlign = TextAlign.Center,
                )
              }
            }
            if (!isIgnoringBatteryOptimizations) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF242424))
                  .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                  .clickable(role = Role.Button, onClick = onRequestBatteryOptimizationExemption)
                  .padding(vertical = 8.dp, horizontal = 8.dp)
                  .testTag("open_battery_settings_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "Allow Background Battery",
                  color = PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.5.sp,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Glowing Continue-Style Metallic White CTA Button: "Confirm & Connect"
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .minimumInteractiveComponentSize()
          .drawWithCache {
            val cornerPx = buttonCorner.toPx()
            val cornerRadius = CornerRadius(cornerPx, cornerPx)
            val buttonClipPath = Path().apply {
              addRoundRect(
                RoundRect(
                  rect = Rect(0f, 0f, size.width, size.height),
                  cornerRadius = cornerRadius,
                ),
              )
            }
            val outerAuraPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(95, 255, 255, 255)
                maskFilter = BlurMaskFilter(
                  18.dp.toPx().coerceAtLeast(0.5f),
                  BlurMaskFilter.Blur.NORMAL,
                )
              }
            }
            val innerRimGlowPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(140, 242, 245, 250)
                maskFilter = BlurMaskFilter(
                  7.dp.toPx().coerceAtLeast(0.5f),
                  BlurMaskFilter.Blur.NORMAL,
                )
              }
            }
            val baseMetallicBrush = Brush.verticalGradient(
              0.0f to PureWhite,
              0.55f to Color(0xFFF4F5F7),
              1.0f to Color(0xFFE4E6EB),
            )
            val pressedBrush = Brush.verticalGradient(
              0.0f to Color(0xFFDADDE3),
              1.0f to Color(0xFFC8CCD4),
            )

            onDrawBehind {
              if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
              val phase = shimmerProvider()
              val clickBoost = borderFlashProvider()
              val breath = sin(phase * Math.PI.toFloat())
              val boostAlpha = (sin(clickBoost * Math.PI.toFloat()).coerceIn(0f, 1f) * 85f).roundToInt()
              outerAuraPaint.asFrameworkPaint().alpha =
                (60 + (55 * breath).roundToInt() + boostAlpha).coerceIn(45, 195)

              drawIntoCanvas { canvas ->
                canvas.drawRoundRect(
                  2.dp.toPx(),
                  2.dp.toPx(),
                  size.width - 2.dp.toPx(),
                  size.height + 3.dp.toPx(),
                  cornerPx,
                  cornerPx,
                  outerAuraPaint,
                )
                canvas.drawRoundRect(
                  0f,
                  0f,
                  size.width,
                  size.height,
                  cornerPx,
                  cornerPx,
                  innerRimGlowPaint,
                )
              }

              drawRoundRect(
                brush = if (isPressed) pressedBrush else baseMetallicBrush,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cornerRadius,
              )

              clipPath(buttonClipPath) {
                val activePhase = if (clickBoost > 0.01f) clickBoost else phase
                val sweepSpan = (size.width * 0.38f).coerceAtLeast(1f)
                val sweepCenter = -sweepSpan + (size.width + sweepSpan * 2f) * activePhase
                drawRect(
                  brush = Brush.linearGradient(
                    colors = listOf(
                      Color.Transparent,
                      PureWhite.copy(alpha = 0.80f),
                      Color(0xFFF6EFE8).copy(alpha = 0.35f),
                      Color.Transparent,
                    ),
                    start = Offset(sweepCenter - sweepSpan * 0.6f, 0f),
                    end = Offset(sweepCenter + sweepSpan * 0.6f, size.height.coerceAtLeast(1f)),
                  ),
                  topLeft = Offset.Zero,
                  size = size,
                )
              }

              drawRoundRect(
                color = PureWhite,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.2.dp.toPx()),
              )
            }
          }
          .clip(buttonShape)
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = ObsidianBlack),
            role = Role.Button,
            onClick = {
              coroutineScope.launch {
                borderFlashPhase.snapTo(0f)
                borderFlashPhase.animateTo(
                  targetValue = 1f,
                  animationSpec = tween(durationMillis = 420, easing = ModalGlowEaseOut),
                )
              }
              onConfirmAndConnect()
            },
          )
          .testTag("confirm_and_connect_hc_button"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "Confirm & Connect",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            letterSpacing = (-0.2).sp,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Canvas(modifier = Modifier.size(16.dp)) {
            val sx = size.width / 24f
            val sy = size.height / 24f
            val arrowPath = Path().apply {
              moveTo(5f * sx, 12f * sy)
              lineTo(19f * sx, 12f * sy)
              moveTo(13f * sx, 6f * sy)
              lineTo(19f * sx, 12f * sy)
              moveTo(13f * sx, 18f * sy)
              lineTo(19f * sx, 12f * sy)
            }
            drawPath(
              path = arrowPath,
              color = ObsidianBlack,
              style = Stroke(
                width = 2.3f * sx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
              ),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MonochromeInstructionRow(
  stepCode: String,
  title: String,
  subtitle: String,
  isGranted: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(5.dp))
        .background(if (isGranted) PureWhite else Color(0xFF242424))
        .border(
          width = 1.dp,
          color = if (isGranted) PureWhite else Color(0xFF3F3F46),
          shape = RoundedCornerShape(5.dp),
        )
        .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = if (isGranted) "✓" else stepCode,
        color = if (isGranted) ObsidianBlack else PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.6.sp,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isGranted) "ALLOWED" else "REQUIRED",
          color = if (isGranted) PureWhite else ModalZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 8.8.sp,
          letterSpacing = 0.6.sp,
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = ModalZinc400,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.2.sp,
        lineHeight = 15.5.sp,
      )
    }
  }
}


