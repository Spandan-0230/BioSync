package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

// ─── Architectural Monochrome & Minimalist Category Palette ──────────────────
private val ExtZinc100 = Color(0xFFF4F4F5)
private val ExtZinc200 = Color(0xFFE4E4E7)
private val ExtZinc300 = Color(0xFFD4D4D8)
private val ExtZinc400 = Color(0xFFA1A1AA)
private val ExtZinc500 = Color(0xFF71717A)
private val ExtZinc600 = Color(0xFF52525B)
private val ExtZinc700 = Color(0xFF3F3F46)
private val ExtZinc800 = Color(0xFF27272A)
private val ExtZinc850 = Color(0xFF1C1C1F)
private val ExtZinc900 = Color(0xFF141417)
private val ExtZinc950 = Color(0xFF0B0B0E)
private val ExtCardBgTop = Color(0xFF141417)
private val ExtCardBgBottom = Color(0xFF0A0A0C)

/**
 * 1. Training Modality Option (Cardio, Strength Training, Mixed)
 */
enum class TrainingModalityOption(
  val shortLabel: String,
  val subtitle: String,
  val badgeCode: String,
  val accentColor: Color,
  val secondaryColor: Color,
) {
  CARDIO(
    shortLabel = "Cardio",
    subtitle = "VO2 & Endurance",
    badgeCode = "AEROBIC",
    accentColor = Color(0xFF38BDF8),
    secondaryColor = Color(0xFF0284C7),
  ),
  STRENGTH(
    shortLabel = "Strength",
    subtitle = "Force & Hypertrophy",
    badgeCode = "ANAEROBIC",
    accentColor = Color(0xFFFF7A29),
    secondaryColor = Color(0xFFE11D48),
  ),
  MIXED(
    shortLabel = "Mixed",
    subtitle = "Strength + Cardio",
    badgeCode = "HYBRID",
    accentColor = Color(0xFFA78BFA),
    secondaryColor = Color(0xFF7C3AED),
  ),
}

/**
 * 2. Workout Duration per Day (in hours)
 */
enum class WorkoutDurationOption(
  val hours: Float,
  val displayHours: String,
  val minutesTag: String,
  val intensityLabel: String,
  val sweatFluidMl: Int,
  val barFraction: Float,
  val accentColor: Color,
) {
  HR_0_5(
    hours = 0.5f,
    displayHours = "0.5h",
    minutesTag = "30 MIN",
    intensityLabel = "EXPRESS SESSION",
    sweatFluidMl = 250,
    barFraction = 0.20f,
    accentColor = Color(0xFF7BE3FF),
  ),
  HR_1_0(
    hours = 1.0f,
    displayHours = "1.0h",
    minutesTag = "60 MIN",
    intensityLabel = "STANDARD VOLUME",
    sweatFluidMl = 500,
    barFraction = 0.40f,
    accentColor = Color(0xFF34D399),
  ),
  HR_1_5(
    hours = 1.5f,
    displayHours = "1.5h",
    minutesTag = "90 MIN",
    intensityLabel = "HIGH OUTPUT",
    sweatFluidMl = 750,
    barFraction = 0.60f,
    accentColor = Color(0xFFFBBF24),
  ),
  HR_2_0(
    hours = 2.0f,
    displayHours = "2.0h",
    minutesTag = "120 MIN",
    intensityLabel = "ATHLETIC BLOCK",
    sweatFluidMl = 1000,
    barFraction = 0.80f,
    accentColor = Color(0xFFFF7A29),
  ),
  HR_2_5_PLUS(
    hours = 2.5f,
    displayHours = "2.5h+",
    minutesTag = "150+ MIN",
    intensityLabel = "PEAK ENDURANCE",
    sweatFluidMl = 1250,
    barFraction = 1.00f,
    accentColor = Color(0xFFF43F5E),
  ),
  ;

  companion object {
    fun fromHours(hours: Float): WorkoutDurationOption =
      entries.minByOrNull { kotlin.math.abs(it.hours - hours) } ?: HR_1_0
  }
}

/**
 * 3. Experience Level (Training Age)
 * Crucial for "Build Muscle" and "Lose Fat":
 * - Beginners can build muscle & lose fat simultaneously (body recomposition), allowing a standard macro split even in a deficit.
 * - Intermediate / Advanced users require a significantly higher protein bias to protect lean tissue during a cut.
 */
enum class ExperienceLevelOption(
  val trainingAgeTag: String,
  val modeBadge: String,
  val proteinFactorGPerKg: Float,
  val accentColor: Color,
  val secondaryColor: Color,
) {
  BEGINNER(
    trainingAgeTag = "< 1 YR",
    modeBadge = "RECOMP READY",
    proteinFactorGPerKg = 1.8f,
    accentColor = Color(0xFF34D399),
    secondaryColor = Color(0xFF059669),
  ),
  INTERMEDIATE(
    trainingAgeTag = "1–3 YRS",
    modeBadge = "ADAPTIVE BIAS",
    proteinFactorGPerKg = 2.1f,
    accentColor = Color(0xFF38BDF8),
    secondaryColor = Color(0xFF0284C7),
  ),
  ADVANCED(
    trainingAgeTag = "3+ YRS",
    modeBadge = "MUSCLE GUARD",
    proteinFactorGPerKg = 2.4f,
    accentColor = Color(0xFFFBBF24),
    secondaryColor = Color(0xFFD97706),
  ),
  ;

  fun synergyHeadline(objective: BlueprintObjectiveOption): String =
    when (this) {
      BEGINNER -> when (objective) {
        BlueprintObjectiveOption.LOSE_FAT -> "BODY RECOMPOSITION WINDOW ACTIVE • BALANCED MACRO SPLIT"
        BlueprintObjectiveOption.MAINTAIN -> "NEURAL & LEAN RECOMPOSITION • STANDARD MACRO RATIO"
        BlueprintObjectiveOption.BUILD_MUSCLE -> "RAPID NOVICE HYPERTROPHY • CLEAN ANABOLIC PARTITIONING"
      }
      INTERMEDIATE -> when (objective) {
        BlueprintObjectiveOption.LOSE_FAT -> "ELEVATED PROTEIN BIAS (2.1G/KG) • ANTI-CATABOLIC CUT"
        BlueprintObjectiveOption.MAINTAIN -> "PERFORMANCE EQUILIBRIUM (2.0G/KG) • GLYCOGEN LOCK"
        BlueprintObjectiveOption.BUILD_MUSCLE -> "PROGRESSIVE OVERLOAD SURPLUS • LEAN MASS ACCRETION"
      }
      ADVANCED -> when (objective) {
        BlueprintObjectiveOption.LOSE_FAT -> "HIGH PROTEIN SHIELD (2.4G/KG) • PROTECTS MUSCLE IN DEFICIT"
        BlueprintObjectiveOption.MAINTAIN -> "DENSE TISSUE RETENTION (2.2G/KG) • PRECISION PARTITION"
        BlueprintObjectiveOption.BUILD_MUSCLE -> "MICRO-SURPLUS PROTOCOL • MINIMIZES ADIPOSE SPILLOVER"
      }
    }

  fun synergyDescription(objective: BlueprintObjectiveOption): String =
    when (this) {
      BEGINNER ->
        "Beginners can simultaneously build lean muscle and mobilize fat (body recomposition), allowing a balanced macro split even inside a caloric deficit."
      INTERMEDIATE ->
        when (objective) {
          BlueprintObjectiveOption.LOSE_FAT ->
            "Recomposition slows after year 1 — raises protein bias to 2.1g/kg to safeguard hard-earned muscle tissue during fat loss."
          else ->
            "Calibrates a 2.1g/kg protein anchor with periodized carbohydrate fueling to drive intermediate strength & hypertrophy."
        }
      ADVANCED ->
        when (objective) {
          BlueprintObjectiveOption.LOSE_FAT ->
            "Advanced athletes cannot easily recompose in a cut — enforces a high 2.4g/kg protein bias to prevent lean tissue catabolism."
          else ->
            "Locks in a high 2.4g/kg protein ceiling with tight caloric tolerances tailored for veteran neuromuscular efficiency."
        }
    }
}

/**
 * 4. Target Pace to Achieve Goal (TDEE)
 */
enum class TargetPaceOption(
  val title: String,
  val subLabel: String,
  val deficitOrSurplusPct: Int,
  val weeklyRateKg: String,
  val barFraction: Float,
  val accentColor: Color,
) {
  SLOW(
    title = "Slow",
    subLabel = "Sustainable",
    deficitOrSurplusPct = 12,
    weeklyRateKg = "~0.25 KG/WK",
    barFraction = 0.33f,
    accentColor = Color(0xFF38BDF8),
  ),
  NORMAL(
    title = "Normal",
    subLabel = "Optimal TDEE",
    deficitOrSurplusPct = 20,
    weeklyRateKg = "~0.50 KG/WK",
    barFraction = 0.66f,
    accentColor = Color(0xFF34D399),
  ),
  FAST(
    title = "Fast",
    subLabel = "Aggressive",
    deficitOrSurplusPct = 28,
    weeklyRateKg = "~0.85 KG/WK",
    barFraction = 1.00f,
    accentColor = Color(0xFFFF7A29),
  ),
  ;

  fun tdeeBadgeFor(objective: BlueprintObjectiveOption): String =
    when (objective) {
      BlueprintObjectiveOption.LOSE_FAT -> "-$deficitOrSurplusPct% TDEE DEFICIT"
      BlueprintObjectiveOption.MAINTAIN -> "±0% TDEE (${title.uppercase()} ADAPT)"
      BlueprintObjectiveOption.BUILD_MUSCLE -> "+${(deficitOrSurplusPct * 0.75f).roundToInt()}% TDEE SURPLUS"
    }
}

/**
 * 5. Day 1 Anchor (Monday through Sunday)
 */
enum class DayOfWeekOption(
  val shortCode: String,
  val fullName: String,
  val dayNumber: Int,
) {
  MONDAY("MON", "Monday", 1),
  TUESDAY("TUE", "Tuesday", 2),
  WEDNESDAY("WED", "Wednesday", 3),
  THURSDAY("THU", "Thursday", 4),
  FRIDAY("FRI", "Friday", 5),
  SATURDAY("SAT", "Saturday", 6),
  SUNDAY("SUN", "Sunday", 7),
}

/**
 * 7. Meal Frequency per Day
 */
enum class MealFrequencyOption(
  val count: Int,
  val displayCount: String,
  val unitLabel: String,
  val cadenceTag: String,
  val intervalNote: String,
  val accentColor: Color,
) {
  MEALS_2(
    count = 2,
    displayCount = "2",
    unitLabel = "MEALS/D",
    cadenceTag = "CONDENSED",
    intervalNote = "2× large boluses (~6–8h window) • Ideal for intermittent fasting.",
    accentColor = Color(0xFF7BE3FF),
  ),
  MEALS_3(
    count = 3,
    displayCount = "3",
    unitLabel = "MEALS/D",
    cadenceTag = "CLASSIC",
    intervalNote = "3× balanced feedings (~4–5h apart) • Optimal daily rhythm.",
    accentColor = Color(0xFF34D399),
  ),
  MEALS_4(
    count = 4,
    displayCount = "4",
    unitLabel = "MEALS/D",
    cadenceTag = "ATHLETIC",
    intervalNote = "4× protein synthesis pulses (~3.5h apart) • Maximizes MPS.",
    accentColor = Color(0xFFFBBF24),
  ),
  MEALS_5_PLUS(
    count = 5,
    displayCount = "5+",
    unitLabel = "MEALS/D",
    cadenceTag = "FREQUENT",
    intervalNote = "5+ metabolically spaced feedings (~2.5–3h apart) • High-volume fuel.",
    accentColor = Color(0xFFFF7A29),
  ),
  ;

  companion object {
    fun fromCount(count: Int): MealFrequencyOption =
      entries.firstOrNull { it.count == count } ?: MEALS_3
  }
}

/**
 * 8. High-Sodium / Fast Food Frequency (Rarely, Sometimes, Often)
 */
enum class SodiumFrequencyOption(
  val title: String,
  val subLabel: String,
  val extraWaterMl: Int,
  val flushBadge: String,
  val flushExplanation: String,
  val accentColor: Color,
) {
  RARELY(
    title = "Rarely",
    subLabel = "0–1× / wk",
    extraWaterMl = 0,
    flushBadge = "+0 ML H2O",
    flushExplanation = "Clean whole-food sodium baseline — standard daily hydration target.",
    accentColor = Color(0xFF34D399),
  ),
  SOMETIMES(
    title = "Sometimes",
    subLabel = "2–3× / wk",
    extraWaterMl = 250,
    flushBadge = "+250 ML BUFFER",
    flushExplanation = "Moderate restaurant sodium — auto-adds +250 mL H2O to stabilize fluid balance.",
    accentColor = Color(0xFFFBBF24),
  ),
  OFTEN(
    title = "Often",
    subLabel = "4+× / wk",
    extraWaterMl = 500,
    flushBadge = "+500 ML FLUSH",
    flushExplanation = "High restaurant sodium load — auto-bumps daily H2O by +500 mL to flush excess salt and prevent water-weight scale spikes.",
    accentColor = Color(0xFFFB7185),
  ),
}

// ─── Calculated Daily Hydration & Protein Protocol Telemetry ─────────────────

internal data class HydrationMacroCalibration(
  val baselineWaterMl: Int,
  val workoutWaterMl: Int,
  val diureticWaterMl: Int,
  val sodiumFlushWaterMl: Int,
  val totalWaterMl: Int,
  val totalWaterLitersFormatted: String,
  val proteinTargetGrams: Int,
  val proteinFactorFormatted: String,
)

internal fun computeHydrationMacroCalibration(
  weightKg: Float,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  objective: BlueprintObjectiveOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
): HydrationMacroCalibration {
  val safeWeight = weightKg.coerceIn(35f, 220f)
  val baselineWaterMl = ((safeWeight * 35f) / 50f).roundToInt() * 50
  val workoutWaterMl = workoutDuration.sweatFluidMl
  val diureticWaterMl = if (caffeineAlcoholYes) 350 else 0
  val sodiumFlushWaterMl = sodiumFrequency.extraWaterMl
  val totalWaterMl = baselineWaterMl + workoutWaterMl + diureticWaterMl + sodiumFlushWaterMl
  val liters = totalWaterMl / 1000f
  val totalWaterLitersFormatted = String.format(Locale.US, "%.2f L", liters)

  // Adjust protein factor slightly higher when in Lose Fat deficit for Intermediate/Advanced
  val effectiveProteinFactor = when (objective) {
    BlueprintObjectiveOption.LOSE_FAT -> experienceLevel.proteinFactorGPerKg
    BlueprintObjectiveOption.MAINTAIN -> (experienceLevel.proteinFactorGPerKg - 0.1f).coerceAtLeast(1.8f)
    BlueprintObjectiveOption.BUILD_MUSCLE -> (experienceLevel.proteinFactorGPerKg - 0.05f).coerceAtLeast(1.8f)
  }
  val proteinTargetGrams = (safeWeight * effectiveProteinFactor).roundToInt()
  val proteinFactorFormatted = String.format(Locale.US, "%.1fg/kg", effectiveProteinFactor)

  return HydrationMacroCalibration(
    baselineWaterMl = baselineWaterMl,
    workoutWaterMl = workoutWaterMl,
    diureticWaterMl = diureticWaterMl,
    sodiumFlushWaterMl = sodiumFlushWaterMl,
    totalWaterMl = totalWaterMl,
    totalWaterLitersFormatted = totalWaterLitersFormatted,
    proteinTargetGrams = proteinTargetGrams,
    proteinFactorFormatted = proteinFactorFormatted,
  )
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION A: TRAINING ARCHITECTURE (Modality + Workout Duration + Experience)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun TrainingArchitectureSection(
  selectedModality: TrainingModalityOption,
  onSelectModality: (TrainingModalityOption) -> Unit,
  selectedDuration: WorkoutDurationOption,
  onSelectDuration: (WorkoutDurationOption) -> Unit,
  selectedExperience: ExperienceLevelOption,
  onSelectExperience: (ExperienceLevelOption) -> Unit,
  selectedObjective: BlueprintObjectiveOption,
  weightKg: Float,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedModalityAccent by animateColorAsState(
    targetValue = selectedModality.accentColor,
    animationSpec = tween(durationMillis = 300),
    label = "training_modality_accent",
  )
  val animatedDurationFraction by animateFloatAsState(
    targetValue = selectedDuration.barFraction,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "workout_duration_bar_fraction",
  )
  val animatedDurationColor by animateColorAsState(
    targetValue = selectedDuration.accentColor,
    animationSpec = tween(durationMillis = 280),
    label = "workout_duration_color",
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("blueprint_training_section"),
  ) {
    // Section Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BlueprintLuminousWhiteDot(
          size = (6.5f * metrics.scale).coerceIn(5.5f, 8f).dp,
        )
        Text(
          text = stringResource(R.string.blueprint_section_training),
          color = ExtZinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(13.2f, 11.5f, 15f),
          letterSpacing = (-0.1).sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((8.5f * metrics.scale).coerceIn(6f, 12f).dp))

    // Main Obsidian Card Container with synchronized borderFlashProvider
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(ExtCardBgTop, ExtCardBgBottom),
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
                    PureWhite.copy(alpha = 0.08f * flash),
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
                0.0f to PureWhite.copy(alpha = 0.22f + 0.56f * flash),
                0.45f to ExtZinc700.copy(alpha = 0.55f + 0.35f * flash),
                1.0f to ExtZinc850,
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = (1.0f + 0.5f * flash).dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(9.5f, 15f).dp),
      ) {
        // ── Sub-block 1: Training Modality (Cardio | Strength | Mixed) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7f * metrics.scale).coerceIn(5.5f, 9f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.blueprint_training_modality_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
            Text(
              text = selectedModality.subtitle.uppercase(),
              color = animatedModalityAccent,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
              letterSpacing = 0.6.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10.5f).dp),
          ) {
            TrainingModalityOption.entries.forEach { modality ->
              TrainingModalityCard(
                option = modality,
                isSelected = modality == selectedModality,
                onClick = { onSelectModality(modality) },
                metrics = metrics,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        HorizontalDivider(color = ExtZinc850, thickness = 0.8.dp)

        // ── Sub-block 2: Workout Duration (in hr) per day ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Text(
              text = stringResource(R.string.blueprint_workout_duration_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
          }

          // 5-Pill Duration Selector (0.5h, 1.0h, 1.5h, 2.0h, 2.5h+)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4.5f, 8f).dp),
          ) {
            WorkoutDurationOption.entries.forEach { duration ->
              val isSelected = duration == selectedDuration
              val pillShape = RoundedCornerShape((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(pillShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.2.dp else 0.8.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = pillShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectDuration(duration) },
                  )
                  .padding(
                    vertical = (7.5f * metrics.scale).coerceIn(6f, 9.5f).dp,
                    horizontal = 2.dp,
                  )
                  .testTag("workout_duration_${duration.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = duration.displayHours,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(11.8f, 10.2f, 13.4f),
                  letterSpacing = (-0.3).sp,
                  maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Canvas(modifier = Modifier.size(4.dp)) {
                    drawCircle(
                      color = if (isSelected) ObsidianBlack.copy(alpha = 0.75f) else duration.accentColor,
                    )
                  }
                  Text(
                    text = duration.minutesTag.replace(" MIN", "M"),
                    color = if (isSelected) ObsidianBlack.copy(alpha = 0.78f) else ExtZinc400,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = metrics.spScaled(7.4f, 6.6f, 8.4f),
                    maxLines = 1,
                  )
                }
              }
            }
          }

          // Minimalist Duration Spectrum Progress Bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height((5f * metrics.scale).coerceIn(4f, 6.5f).dp)
              .clip(CircleShape)
              .background(ExtZinc900)
              .border(0.5.dp, ExtZinc800, CircleShape),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(animatedDurationFraction.coerceIn(0.08f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      PureWhite.copy(alpha = 0.88f),
                      animatedDurationColor,
                    ),
                  ),
                ),
            )
          }
        }

        HorizontalDivider(color = ExtZinc850, thickness = 0.8.dp)

        // ── Sub-block 3: Experience Level (Training Age: Beginner | Intermediate | Advanced) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Text(
              text = stringResource(R.string.blueprint_experience_level_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10.5f).dp),
          ) {
            ExperienceLevelOption.entries.forEach { exp ->
              val isSelected = exp == selectedExperience
              val expTitle = when (exp) {
                ExperienceLevelOption.BEGINNER -> stringResource(R.string.blueprint_exp_beginner)
                ExperienceLevelOption.INTERMEDIATE -> stringResource(R.string.blueprint_exp_intermediate)
                ExperienceLevelOption.ADVANCED -> stringResource(R.string.blueprint_exp_advanced)
              }
              val tileShape = RoundedCornerShape((9.5f * metrics.scale).coerceIn(8f, 11.5f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(tileShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.2.dp else 0.8.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = tileShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectExperience(exp) },
                  )
                  .padding(
                    horizontal = (8f * metrics.scale).coerceIn(6.5f, 10f).dp,
                    vertical = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
                  )
                  .testTag("experience_level_${exp.name.lowercase()}"),
                horizontalAlignment = Alignment.Start,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = exp.trainingAgeTag,
                    color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else exp.accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(8.2f, 7.2f, 9.4f),
                    letterSpacing = 0.5.sp,
                  )
                  Canvas(modifier = Modifier.size(5.5.dp)) {
                    drawCircle(
                      color = if (isSelected) ObsidianBlack else exp.accentColor,
                    )
                  }
                }
                Spacer(modifier = Modifier.height(3.5.dp))
                Text(
                  text = expTitle,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(12.2f, 10.6f, 13.8f),
                  letterSpacing = (-0.2).sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
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
private fun TrainingModalityCard(
  option: TrainingModalityOption,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape((10f * metrics.scale).coerceIn(8.5f, 12f).dp)
  val title = when (option) {
    TrainingModalityOption.CARDIO -> stringResource(R.string.blueprint_modality_cardio)
    TrainingModalityOption.STRENGTH -> stringResource(R.string.blueprint_modality_strength)
    TrainingModalityOption.MIXED -> stringResource(R.string.blueprint_modality_mixed)
  }

  Column(
    modifier = modifier
      .clip(shape)
      .background(if (isSelected) PureWhite else ExtZinc950)
      .border(
        width = if (isSelected) 1.3.dp else 0.85.dp,
        color = if (isSelected) PureWhite else ExtZinc800,
        shape = shape,
      )
      .semantics { selected = isSelected }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .padding(
        horizontal = (9f * metrics.scale).coerceIn(7f, 11.5f).dp,
        vertical = (9.5f * metrics.scale).coerceIn(8f, 12f).dp,
      )
      .testTag("training_modality_${option.name.lowercase()}"),
    horizontalAlignment = Alignment.Start,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Custom Vector Modality Icon Badge
      Box(
        modifier = Modifier
          .size((26f * metrics.scale).coerceIn(22f, 30f).dp)
          .clip(RoundedCornerShape(6.5.dp))
          .background(
            if (isSelected) ObsidianBlack else option.accentColor.copy(alpha = 0.14f),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(
          modifier = Modifier.size((14.5f * metrics.scale).coerceIn(12f, 17f).dp),
        ) {
          val strokeColor = if (isSelected) option.accentColor else option.accentColor
          val sx = size.width / 24f
          val sy = size.height / 24f
          when (option) {
            TrainingModalityOption.CARDIO -> {
              // ECG Pulse Waveform
              val path = Path().apply {
                moveTo(2f * sx, 12f * sy)
                lineTo(6.5f * sx, 12f * sy)
                lineTo(9.5f * sx, 4.5f * sy)
                lineTo(14f * sx, 19.5f * sy)
                lineTo(17.2f * sx, 12f * sy)
                lineTo(22f * sx, 12f * sy)
              }
              drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.3f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
            }
            TrainingModalityOption.STRENGTH -> {
              // Precision Barbell Vector
              drawLine(
                color = strokeColor,
                start = Offset(3f * sx, 12f * sy),
                end = Offset(21f * sx, 12f * sy),
                strokeWidth = 2.2f * sx,
                cap = StrokeCap.Round,
              )
              drawLine(
                color = strokeColor,
                start = Offset(6.5f * sx, 6.5f * sy),
                end = Offset(6.5f * sx, 17.5f * sy),
                strokeWidth = 2.5f * sx,
                cap = StrokeCap.Round,
              )
              drawLine(
                color = strokeColor,
                start = Offset(17.5f * sx, 6.5f * sy),
                end = Offset(17.5f * sx, 17.5f * sy),
                strokeWidth = 2.5f * sx,
                cap = StrokeCap.Round,
              )
            }
            TrainingModalityOption.MIXED -> {
              // Dual Intersecting Kinetic Nodes
              drawCircle(
                color = strokeColor,
                radius = 5.5f * sx,
                center = Offset(9.5f * sx, 12f * sy),
                style = Stroke(width = 2.1f * sx),
              )
              drawCircle(
                color = strokeColor,
                radius = 5.5f * sx,
                center = Offset(14.5f * sx, 12f * sy),
                style = Stroke(width = 2.1f * sx),
              )
            }
          }
        }
      }

      // Minimalist Category Accent Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(
            if (isSelected) ObsidianBlack.copy(alpha = 0.10f) else option.accentColor.copy(alpha = 0.14f),
          )
          .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
      ) {
        Text(
          text = option.badgeCode,
          color = if (isSelected) ObsidianBlack else option.accentColor,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(7.0f, 6.2f, 8.0f),
          letterSpacing = 0.4.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((7f * metrics.scale).coerceIn(5.5f, 9f).dp))

    Text(
      text = title,
      color = if (isSelected) ObsidianBlack else PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(12.8f, 11.2f, 14.4f),
      letterSpacing = (-0.2).sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = option.subtitle,
      color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else ExtZinc400,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(9.6f, 8.4f, 11f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION B: METABOLIC PACE & CYCLE (Target Pace TDEE + Day 1 Anchor)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun MetabolicPaceCycleSection(
  selectedPace: TargetPaceOption,
  onSelectPace: (TargetPaceOption) -> Unit,
  selectedDayOne: DayOfWeekOption,
  onSelectDayOne: (DayOfWeekOption) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("blueprint_pace_cycle_section"),
  ) {
    // Section Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BlueprintLuminousWhiteDot(
          size = (6.5f * metrics.scale).coerceIn(5.5f, 8f).dp,
        )
        Text(
          text = stringResource(R.string.blueprint_section_pace_cycle),
          color = ExtZinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(13.2f, 11.5f, 15f),
          letterSpacing = (-0.1).sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((8.5f * metrics.scale).coerceIn(6f, 12f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(ExtCardBgTop, ExtCardBgBottom),
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
                    PureWhite.copy(alpha = 0.08f * flash),
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
                0.0f to PureWhite.copy(alpha = 0.22f + 0.56f * flash),
                0.45f to ExtZinc700.copy(alpha = 0.55f + 0.35f * flash),
                1.0f to ExtZinc850,
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = (1.0f + 0.5f * flash).dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(9.5f, 15f).dp),
      ) {
        // ── Sub-block 1: Target Pace (Slow, Normal, Fast) to Achieve Goal (TDEE) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Text(
              text = stringResource(R.string.blueprint_target_pace_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10.5f).dp),
          ) {
            TargetPaceOption.entries.forEach { pace ->
              val isSelected = pace == selectedPace
              val paceTitle = when (pace) {
                TargetPaceOption.SLOW -> stringResource(R.string.blueprint_pace_slow)
                TargetPaceOption.NORMAL -> stringResource(R.string.blueprint_pace_normal)
                TargetPaceOption.FAST -> stringResource(R.string.blueprint_pace_fast)
              }
              val tileShape = RoundedCornerShape((9.5f * metrics.scale).coerceIn(8f, 11.5f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(tileShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.25.dp else 0.85.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = tileShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectPace(pace) },
                  )
                  .padding(
                    horizontal = (9f * metrics.scale).coerceIn(7f, 11f).dp,
                    vertical = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
                  )
                  .testTag("target_pace_${pace.name.lowercase()}"),
                horizontalAlignment = Alignment.Start,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = pace.weeklyRateKg,
                    color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else pace.accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
                    letterSpacing = 0.4.sp,
                  )
                  Canvas(modifier = Modifier.size(5.5.dp)) {
                    drawCircle(color = if (isSelected) ObsidianBlack else pace.accentColor)
                  }
                }
                Spacer(modifier = Modifier.height(3.5.dp))
                Text(
                  text = paceTitle,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(12.6f, 11f, 14.2f),
                  letterSpacing = (-0.2).sp,
                  maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = pace.subLabel,
                  color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else ExtZinc400,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
                  maxLines = 1,
                )
              }
            }
          }
        }

        HorizontalDivider(color = ExtZinc850, thickness = 0.8.dp)

        // ── Sub-block 2: Protocol Day 01 Anchor (Monday - Sunday) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.blueprint_day_one_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
            Text(
              text = "DAY 01 = ${selectedDayOne.fullName.uppercase()}",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
              letterSpacing = 0.5.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((5f * metrics.scale).coerceIn(3.5f, 7f).dp),
          ) {
            DayOfWeekOption.entries.forEach { day ->
              val isSelected = day == selectedDayOne
              val dayShape = RoundedCornerShape((8f * metrics.scale).coerceIn(6.5f, 10f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(dayShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.2.dp else 0.8.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = dayShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectDayOne(day) },
                  )
                  .padding(vertical = (7.5f * metrics.scale).coerceIn(6f, 9.5f).dp)
                  .testTag("day_one_${day.shortCode.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = if (isSelected) "D1" else "0${day.dayNumber}",
                  color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else ExtZinc500,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(7.2f, 6.4f, 8.2f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = day.shortCode,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(9.8f, 8.6f, 11f),
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION C: NUTRITION & HYDRATION CALIBRATION
// (Meal Frequency + Caffeine/Alcohol Diuretic Toggle + High-Sodium/Fast Food)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun NutritionHydrationSection(
  selectedMealFrequency: MealFrequencyOption,
  onSelectMealFrequency: (MealFrequencyOption) -> Unit,
  caffeineAlcoholYes: Boolean,
  onToggleCaffeineAlcohol: (Boolean) -> Unit,
  selectedSodiumFrequency: SodiumFrequencyOption,
  onSelectSodiumFrequency: (SodiumFrequencyOption) -> Unit,
  calibration: HydrationMacroCalibration,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("blueprint_nutrition_hydration_section"),
  ) {
    // Section Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BlueprintLuminousWhiteDot(
          size = (6.5f * metrics.scale).coerceIn(5.5f, 8f).dp,
        )
        Text(
          text = stringResource(R.string.blueprint_section_nutrition_hydration),
          color = ExtZinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(13.2f, 11.5f, 15f),
          letterSpacing = (-0.1).sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((8.5f * metrics.scale).coerceIn(6f, 12f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(ExtCardBgTop, ExtCardBgBottom),
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
                    PureWhite.copy(alpha = 0.08f * flash),
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
                0.0f to PureWhite.copy(alpha = 0.22f + 0.56f * flash),
                0.45f to ExtZinc700.copy(alpha = 0.55f + 0.35f * flash),
                1.0f to ExtZinc850,
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = (1.0f + 0.5f * flash).dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(9.5f, 15f).dp),
      ) {
        // ── Sub-block 1: Meal Frequency per Day (2, 3, 4, 5+) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Text(
              text = stringResource(R.string.blueprint_meal_frequency_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
              letterSpacing = 0.9.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((7f * metrics.scale).coerceIn(5.5f, 9.5f).dp),
          ) {
            MealFrequencyOption.entries.forEach { meal ->
              val isSelected = meal == selectedMealFrequency
              val pillShape = RoundedCornerShape((9f * metrics.scale).coerceIn(7.5f, 11f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(pillShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.25.dp else 0.85.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = pillShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectMealFrequency(meal) },
                  )
                  .padding(
                    vertical = (8f * metrics.scale).coerceIn(6.5f, 10.5f).dp,
                    horizontal = 4.dp,
                  )
                  .testTag("meal_frequency_${meal.count}"),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = meal.displayCount,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(14f, 12f, 16f),
                  letterSpacing = (-0.3).sp,
                )
                Text(
                  text = meal.unitLabel,
                  color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else ExtZinc400,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(7.4f, 6.5f, 8.4f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = meal.cadenceTag,
                  color = if (isSelected) ObsidianBlack.copy(alpha = 0.85f) else meal.accentColor,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(7.0f, 6.2f, 8.0f),
                  letterSpacing = 0.3.sp,
                )
              }
            }
          }

          Text(
            text = selectedMealFrequency.intervalNote,
            color = ExtZinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
          )
        }

        HorizontalDivider(color = ExtZinc850, thickness = 0.8.dp)

        // ── Sub-block 2: Caffeine / Alcohol Intake (Yes/No Diuretic Toggle) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7f * metrics.scale).coerceIn(5.5f, 9f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.5.dp),
            ) {
              Text(
                text = stringResource(R.string.blueprint_diuretic_title),
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(12.6f, 11f, 14.2f),
                letterSpacing = (-0.2).sp,
              )
              Text(
                text = stringResource(R.string.blueprint_diuretic_subtitle),
                color = ExtZinc400,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = metrics.spScaled(10.0f, 8.8f, 11.4f),
                lineHeight = metrics.spScaled(13.8f, 12.2f, 15.5f),
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // High-Contrast Yes / No Segmented Toggle
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(ExtZinc950)
                .border(0.9.dp, ExtZinc800, RoundedCornerShape(9.dp))
                .padding(3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              DiureticTogglePill(
                label = "NO",
                isSelected = !caffeineAlcoholYes,
                accentDotColor = ExtZinc500,
                onClick = { onToggleCaffeineAlcohol(false) },
                metrics = metrics,
                testTag = "caffeine_alcohol_no",
              )
              DiureticTogglePill(
                label = "YES",
                isSelected = caffeineAlcoholYes,
                accentDotColor = Color(0xFF38BDF8),
                onClick = { onToggleCaffeineAlcohol(true) },
                metrics = metrics,
                testTag = "caffeine_alcohol_yes",
              )
            }
          }
        }

        HorizontalDivider(color = ExtZinc850, thickness = 0.8.dp)

        // ── Sub-block 3: High-Sodium / Fast Food Frequency (Rarely, Sometimes, Often) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).coerceIn(6f, 9.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Text(
              text = stringResource(R.string.blueprint_sodium_label),
              color = ExtZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.2f, 8.0f, 10.4f),
              letterSpacing = 0.8.sp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10.5f).dp),
          ) {
            SodiumFrequencyOption.entries.forEach { sodium ->
              val isSelected = sodium == selectedSodiumFrequency
              val sodiumTitle = when (sodium) {
                SodiumFrequencyOption.RARELY -> stringResource(R.string.blueprint_sodium_rarely)
                SodiumFrequencyOption.SOMETIMES -> stringResource(R.string.blueprint_sodium_sometimes)
                SodiumFrequencyOption.OFTEN -> stringResource(R.string.blueprint_sodium_often)
              }
              val tileShape = RoundedCornerShape((9.5f * metrics.scale).coerceIn(8f, 11.5f).dp)
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(tileShape)
                  .background(if (isSelected) PureWhite else ExtZinc950)
                  .border(
                    width = if (isSelected) 1.25.dp else 0.85.dp,
                    color = if (isSelected) PureWhite else ExtZinc800,
                    shape = tileShape,
                  )
                  .semantics { selected = isSelected }
                  .clickable(
                    role = Role.RadioButton,
                    onClick = { onSelectSodiumFrequency(sodium) },
                  )
                  .padding(
                    horizontal = (9f * metrics.scale).coerceIn(7f, 11f).dp,
                    vertical = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
                  )
                  .testTag("sodium_frequency_${sodium.name.lowercase()}"),
                horizontalAlignment = Alignment.Start,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = sodium.subLabel.uppercase(),
                    color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else sodium.accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(7.6f, 6.8f, 8.6f),
                    letterSpacing = 0.4.sp,
                  )
                  Canvas(modifier = Modifier.size(5.5.dp)) {
                    drawCircle(color = if (isSelected) ObsidianBlack else sodium.accentColor)
                  }
                }
                Spacer(modifier = Modifier.height(3.5.dp))
                Text(
                  text = sodiumTitle,
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(12.2f, 10.6f, 13.8f),
                  letterSpacing = (-0.2).sp,
                  maxLines = 1,
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
private fun DiureticTogglePill(
  label: String,
  isSelected: Boolean,
  accentDotColor: Color,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  testTag: String,
) {
  val pillShape = RoundedCornerShape(6.5.dp)
  Row(
    modifier = Modifier
      .clip(pillShape)
      .background(if (isSelected) PureWhite else Color.Transparent)
      .semantics { selected = isSelected }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .padding(
        horizontal = (10f * metrics.scale).coerceIn(8f, 12.5f).dp,
        vertical = (6.5f * metrics.scale).coerceIn(5.5f, 8.5f).dp,
      )
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.5.dp),
  ) {
    Canvas(modifier = Modifier.size(5.dp)) {
      drawCircle(color = if (isSelected) ObsidianBlack else accentDotColor)
    }
    Text(
      text = label,
      color = if (isSelected) ObsidianBlack else ExtZinc300,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(9.6f, 8.4f, 10.8f),
      letterSpacing = 0.6.sp,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// WORKOUT / REST DAYS SELECTION POPUP MODAL
// (FOR LIGHT 1–3D/WK, MODERATE 3–5D/WK & VERY ACTIVE 6–7D/WK)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun WorkoutDaysPickerDialog(
  activityLevel: ActivityLevelOption,
  initialSelectedDays: Set<DayOfWeekOption>,
  onConfirmDays: (Set<DayOfWeekOption>) -> Unit,
  onDismiss: () -> Unit,
) {
  val minDays = when (activityLevel) {
    ActivityLevelOption.LIGHT -> 1
    ActivityLevelOption.MODERATE -> 3
    ActivityLevelOption.VERY_ACTIVE -> 6
    ActivityLevelOption.SEDENTARY -> 0
  }
  val maxDays = when (activityLevel) {
    ActivityLevelOption.LIGHT -> 3
    ActivityLevelOption.MODERATE -> 5
    ActivityLevelOption.VERY_ACTIVE -> 7
    ActivityLevelOption.SEDENTARY -> 0
  }
  val defaultDays = remember(activityLevel) {
    DailyTargetsEngine.defaultWorkoutDaysFor(activityLevel)
  }
  var draftDays by remember(activityLevel, initialSelectedDays) {
    val seed = if (initialSelectedDays.size in minDays..maxDays) {
      initialSelectedDays
    } else {
      defaultDays
    }
    mutableStateOf(seed)
  }

  val workoutCount = draftDays.size
  val restCount = (7 - workoutCount).coerceAtLeast(0)
  val tierLabel = when (activityLevel) {
    ActivityLevelOption.LIGHT -> "LIGHT ACTIVITY • 1–3 DAYS / WEEK"
    ActivityLevelOption.MODERATE -> "MODERATE ACTIVITY • 3–5 DAYS / WEEK"
    ActivityLevelOption.VERY_ACTIVE -> "VERY ACTIVE • 6–7 DAYS / WEEK"
    ActivityLevelOption.SEDENTARY -> "SEDENTARY"
  }
  val dialogTitle = when (activityLevel) {
    ActivityLevelOption.VERY_ACTIVE -> "Which days will you work out or rest?"
    else -> "Which days will you work out?"
  }
  val recommendationNote = when (activityLevel) {
    ActivityLevelOption.LIGHT ->
      "Defaulted to 3 workout days & 4 rest days. Tap any day below to select 1 to 3 workout days."
    ActivityLevelOption.MODERATE ->
      "Select 3 to 5 workout days (2 to 4 rest days). Tap any day below to customize your weekly training schedule."
    ActivityLevelOption.VERY_ACTIVE ->
      "Select 6 or 7 workout days (0 or 1 rest day). Tap any day below to set your rest day or full 7-day training week."
    ActivityLevelOption.SEDENTARY -> ""
  }
  val defaultBtnLabel = when (activityLevel) {
    ActivityLevelOption.VERY_ACTIVE -> "DEFAULT (6W / 1R)"
    else -> "MAX DEFAULT (${maxDays}D)"
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF0B0C10))
        .border(1.3.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
        .padding(18.dp)
        .testTag("workout_days_picker_dialog"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Canvas(modifier = Modifier.size(7.dp)) {
              drawCircle(color = PureWhite)
            }
            Text(
              text = tierLabel,
              color = ExtZinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp,
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(PureWhite)
              .padding(horizontal = 7.dp, vertical = 2.5.dp),
          ) {
            Text(
              text = "${workoutCount}W / ${restCount}R",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 9.5.sp,
            )
          }
        }

        Text(
          text = dialogTitle,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 19.sp,
          letterSpacing = (-0.4).sp,
        )

        Text(
          text = recommendationNote,
          color = ExtZinc300,
          fontFamily = InterFontFamily,
          fontSize = 12.2.sp,
          lineHeight = 17.2.sp,
        )

        // 7-Day Selector Grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        ) {
          DayOfWeekOption.entries.forEach { day ->
            val isSelected = day in draftDays
            val pillShape = RoundedCornerShape(9.dp)
            Column(
              modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .background(if (isSelected) PureWhite else ExtZinc950)
                .border(
                  width = if (isSelected) 1.4.dp else 1.dp,
                  color = if (isSelected) PureWhite else ExtZinc800,
                  shape = pillShape,
                )
                .clickable(role = Role.Checkbox) {
                  draftDays = if (isSelected) {
                    if (draftDays.size > minDays) {
                      draftDays - day
                    } else if (activityLevel == ActivityLevelOption.VERY_ACTIVE) {
                      // In Very Active (min 6 workout days = max 1 rest day), tapping a different day to make it REST
                      // automatically switches the single rest day to this newly tapped day!
                      (DayOfWeekOption.entries.toSet() - day)
                    } else {
                      draftDays
                    }
                  } else {
                    if (draftDays.size < maxDays) {
                      draftDays + day
                    } else {
                      // Replace earliest day so user can smoothly switch days even at max
                      val trimmed = draftDays.drop(1).toSet()
                      trimmed + day
                    }
                  }
                }
                .padding(vertical = 10.dp)
                .testTag("workout_day_toggle_${day.shortCode.lowercase()}"),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Text(
                text = day.shortCode,
                color = if (isSelected) ObsidianBlack else PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.5.sp,
              )
              Text(
                text = if (isSelected) "WORK" else "REST",
                color = if (isSelected) ObsidianBlack.copy(alpha = 0.78f) else ExtZinc500,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 7.5.sp,
              )
            }
          }
        }

        // Summary Strip of Selected Workout vs Rest Days
        val workoutNames = DayOfWeekOption.entries.filter { it in draftDays }.joinToString(", ") { it.shortCode }
        val restDaysList = DayOfWeekOption.entries.filter { it !in draftDays }
        val restNames = if (restDaysList.isEmpty()) "NONE (7/7 ACTIVE)" else restDaysList.joinToString(", ") { it.shortCode }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ExtZinc950)
            .border(1.dp, ExtZinc800, RoundedCornerShape(10.dp))
            .padding(11.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "WORKOUT DAYS ($workoutCount): $workoutNames",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
          )
          Text(
            text = "REST DAYS ($restCount): $restNames",
            color = ExtZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.0.sp,
          )
        }

        // Action Buttons (Use Default vs Confirm Days)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(ExtZinc900)
              .border(1.dp, ExtZinc700, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                draftDays = defaultDays
              }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = defaultBtnLabel,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.5.sp,
            )
          }

          Box(
            modifier = Modifier
              .weight(1.25f)
              .clip(RoundedCornerShape(10.dp))
              .background(PureWhite)
              .clickable(role = Role.Button) {
                onConfirmDays(draftDays)
              }
              .padding(vertical = 12.dp)
              .testTag("confirm_workout_days_button"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "APPLY WORKOUT DAYS",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
            )
          }
        }
      }
    }
  }
}

