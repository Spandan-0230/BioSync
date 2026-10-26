package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaloriesEnd
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsEnd
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.FatsEnd
import com.example.ui.theme.FatsMid
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationEnd
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsEnd
import com.example.ui.theme.StepsMid
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class HeightUnit {
  CM,
  INCH,
}

enum class WeightUnit {
  KG,
  LBS,
}

enum class BmiZone {
  UNDERWEIGHT,
  OPTIMAL,
  OVERWEIGHT,
  OBESE,
}

private enum class PersonalActiveEditSection {
  NONE,
  HEIGHT,
  WEIGHT,
}

// Base Monochrome Obsidian, Zinc, Platinum & Pure White Palette
private val Zinc100 = Color(0xFFF4F4F5)
private val Zinc200 = Color(0xFFE4E4E7)
private val Zinc300 = Color(0xFFD4D4D8)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc500 = Color(0xFF71717A)
private val Zinc600 = Color(0xFF52525B)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc800 = Color(0xFF27272A)
private val Zinc850 = Color(0xFF1F1F23)
private val Zinc900 = Color(0xFF18181B)
private val Zinc950 = Color(0xFF09090B)
private val CardBgTop = Color(0xFF141417)
private val CardBgBottom = Color(0xFF0A0A0C)
private val MonochromeTrack = Color(0xFF151820)
private val MonochromeTrackBorder = Color(0xFF282D3A)

// Welcome Screen Progress Bar Color Spectrums Mapped to BMI Classification Zones:
// 1. UNDERWEIGHT (<18.5): Welcome Screen Hydration / Protein Luminous Sky-Cyan -> Azure Blue
private val BmiUnderweightPrimary = HydrationStart // #5BC0FF
private val BmiUnderweightSecondary = ProteinStart // #7BE3FF
private val BmiUnderweightDeep = HydrationMid // #0B76E0

// 2. OPTIMAL (18.5–24.9): Welcome Screen Steps Luminous Mint -> Vibrant Emerald -> Cyber Cyan
private val BmiOptimalPrimary = StepsStart // #7BF1A8
private val BmiOptimalSecondary = StepsMid // #10B981
private val BmiOptimalDeep = CyberCyan // #00F5D4

// 3. OVERWEIGHT (25.0–29.9): Welcome Screen Carbs / Calories Lemon-Gold -> Rich Amber -> Solar Orange
private val BmiOverweightPrimary = CarbsStart // #FFE97D
private val BmiOverweightSecondary = CarbsMid // #FBBF24
private val BmiOverweightDeep = CaloriesMid // #FF7A29

// 4. OBESE (30.0+): Welcome Screen Calories / Fats Coral-Pink -> Vibrant Magenta-Coral -> Deep Ruby
private val BmiObesePrimary = FatsStart // #FFA6C1
private val BmiObeseSecondary = FatsMid // #FB4570
private val BmiObeseDeep = CaloriesEnd // #FF2E63

// Full-spectrum horizontal gradient across the BMI gauge bar using Welcome Screen progress bar colors
private val WelcomeBmiSpectrumGradient = listOf(
  HydrationEnd,      // 0.00 - Deep Royal Cobalt
  HydrationMid,      // 0.12 - Rich Azure
  HydrationStart,    // 0.22 - Luminous Sky Cyan (Underweight crest)
  StepsStart,        // 0.30 - Luminous Mint (Optimal entry)
  StepsMid,          // 0.45 - Vibrant Emerald (Optimal core)
  CyberCyan,         // 0.56 - Cyber Cyan (Optimal ceiling)
  CaloriesStart,     // 0.64 - Luminous Gold (Overweight entry)
  CarbsMid,          // 0.72 - Rich Amber
  CaloriesMid,       // 0.79 - Solar Orange (Overweight ceiling)
  FatsMid,           // 0.88 - Vibrant Coral-Pink (Obese entry)
  CaloriesEnd,       // 0.95 - Vibrant Coral-Magenta
  FatsEnd,           // 1.00 - Deep Ruby-Crimson
)

private val PersonalEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private fun safeBlurRadius(radius: Float): Float = radius.coerceAtLeast(0.5f)

@Immutable
private data class BmiTelemetryResult(
  val bmi: Float,
  val bmiPrime: Float,
  val ponderalIndex: Float,
  val zone: BmiZone,
  val categoryLabel: String,
  val categoryCode: String,
  val statusSummary: String,
  val gaugeProgress: Float,
  val idealMinKg: Float,
  val idealMaxKg: Float,
  val deltaFromOptimalKg: Float,
  val restingBmrKcal: Int,
  val estimatedTdeeKcal: Int,
  val sexCalibrationLabel: String,
  val zonePrimaryColor: Color,
  val zoneSecondaryColor: Color,
  val zoneDeepColor: Color,
)

/**
 * Scientifically calibrated BMI & Resting Metabolic Rate computation:
 * - BMI = mass(kg) / height(m)^2
 * - Optimal Mass Range = [18.5 * h^2 .. 24.9 * h^2]
 * - Resting Metabolic Rate (RMR/BMR) via clinical Mifflin-St Jeor Equation:
 *   * MALE:   (10 * weight_kg) + (6.25 * height_cm) - (5 * age_years) + 5
 *   * FEMALE: (10 * weight_kg) + (6.25 * height_cm) - (5 * age_years) - 161
 *   Cross-validated for adult physiology (70–85% population variance accuracy prior to indirect calorimetry).
 */
private fun calculateBmiTelemetry(
  heightCm: Float,
  weightKg: Float,
  ageYears: Int,
  biologicalSex: String,
): BmiTelemetryResult {
  val safeCm = heightCm.coerceIn(90f, 250f)
  val safeKg = weightKg.coerceIn(25f, 260f)
  val heightM = safeCm / 100f
  val bmiRaw = safeKg / (heightM * heightM)
  val bmi = (bmiRaw * 10f).roundToInt() / 10f

  val bmiPrime = ((bmiRaw / 25.0f) * 100f).roundToInt() / 100f
  val ponderalIndex = ((safeKg / (heightM * heightM * heightM)) * 10f).roundToInt() / 10f

  val idealMinKg = ((18.5f * heightM * heightM) * 10f).roundToInt() / 10f
  val idealMaxKg = ((24.9f * heightM * heightM) * 10f).roundToInt() / 10f
  val optimalCenterKg = 21.7f * heightM * heightM
  val deltaFromOptimalKg = ((safeKg - optimalCenterKg) * 10f).roundToInt() / 10f

  // Map BMI [13.5 .. 37.0] onto normalized [0.04 .. 0.96] spectrum gauge
  // Zone thresholds on gauge:
  // 18.5 -> 0.25, 25.0 -> 0.58, 30.0 -> 0.80
  val gaugeProgress = when {
    bmiRaw <= 18.5f -> {
      val t = ((bmiRaw - 13.5f) / (18.5f - 13.5f)).coerceIn(0f, 1f)
      0.04f + t * 0.21f
    }
    bmiRaw <= 25.0f -> {
      val t = ((bmiRaw - 18.5f) / (25.0f - 18.5f)).coerceIn(0f, 1f)
      0.25f + t * 0.33f
    }
    bmiRaw <= 30.0f -> {
      val t = ((bmiRaw - 25.0f) / (30.0f - 25.0f)).coerceIn(0f, 1f)
      0.58f + t * 0.22f
    }
    else -> {
      val t = ((bmiRaw - 30.0f) / (37.0f - 30.0f)).coerceIn(0f, 1f)
      0.80f + t * 0.16f
    }
  }.coerceIn(0.04f, 0.96f)

  val zone: BmiZone
  val categoryLabel: String
  val categoryCode: String
  val statusSummary: String
  val zonePrimaryColor: Color
  val zoneSecondaryColor: Color
  val zoneDeepColor: Color

  when {
    bmiRaw < 18.5f -> {
      zone = BmiZone.UNDERWEIGHT
      categoryLabel = "UNDERWEIGHT"
      categoryCode = "SUB-OPTIMAL MASS"
      statusSummary = "Below baseline mass density • +${formatOneDecimal(idealMinKg - safeKg)} kg to optimal floor"
      zonePrimaryColor = BmiUnderweightPrimary
      zoneSecondaryColor = BmiUnderweightSecondary
      zoneDeepColor = BmiUnderweightDeep
    }
    bmiRaw < 25.0f -> {
      zone = BmiZone.OPTIMAL
      categoryLabel = "OPTIMAL RANGE"
      categoryCode = "PEAK CALIBRATION"
      statusSummary = "Somatic mass balanced within peak metabolic efficiency corridor"
      zonePrimaryColor = BmiOptimalPrimary
      zoneSecondaryColor = BmiOptimalSecondary
      zoneDeepColor = BmiOptimalDeep
    }
    bmiRaw < 30.0f -> {
      zone = BmiZone.OVERWEIGHT
      categoryLabel = "OVERWEIGHT"
      categoryCode = "ELEVATED MASS"
      statusSummary = "Above baseline corridor • -${formatOneDecimal(safeKg - idealMaxKg)} kg to optimal ceiling"
      zonePrimaryColor = BmiOverweightPrimary
      zoneSecondaryColor = BmiOverweightSecondary
      zoneDeepColor = BmiOverweightDeep
    }
    else -> {
      zone = BmiZone.OBESE
      categoryLabel = "OBESE CLASS"
      categoryCode = "HIGH MASS INDEX"
      statusSummary = "Elevated somatic load • -${formatOneDecimal(safeKg - idealMaxKg)} kg to optimal ceiling"
      zonePrimaryColor = BmiObesePrimary
      zoneSecondaryColor = BmiObeseSecondary
      zoneDeepColor = BmiObeseDeep
    }
  }

  // Scientific Mifflin-St Jeor Resting Metabolic Rate (RMR / BMR):
  // Men:   10 * weight(kg) + 6.25 * height(cm) - 5 * age(y) + 5
  // Women: 10 * weight(kg) + 6.25 * height(cm) - 5 * age(y) - 161
  val safeAge = ageYears.coerceIn(10, 110)
  val isMale = biologicalSex.trim().equals("MALE", ignoreCase = true)
  val sexConstant = if (isMale) 5f else -161f
  val sexCalibrationLabel = if (isMale) {
    "MALE (+5) • MIFFLIN-ST JEOR"
  } else {
    "FEMALE (-161) • MIFFLIN-ST JEOR"
  }
  val bmrRaw = (10f * safeKg) + (6.25f * safeCm) - (5f * safeAge) + sexConstant
  val bmr = bmrRaw.roundToInt().coerceAtLeast(if (isMale) 1050 else 880)
  val estimatedTdeeKcal = (bmr * 1.375f).roundToInt()

  return BmiTelemetryResult(
    bmi = bmi,
    bmiPrime = bmiPrime,
    ponderalIndex = ponderalIndex,
    zone = zone,
    categoryLabel = categoryLabel,
    categoryCode = categoryCode,
    statusSummary = statusSummary,
    gaugeProgress = gaugeProgress,
    idealMinKg = idealMinKg,
    idealMaxKg = idealMaxKg,
    deltaFromOptimalKg = deltaFromOptimalKg,
    restingBmrKcal = bmr,
    estimatedTdeeKcal = estimatedTdeeKcal,
    sexCalibrationLabel = sexCalibrationLabel,
    zonePrimaryColor = zonePrimaryColor,
    zoneSecondaryColor = zoneSecondaryColor,
    zoneDeepColor = zoneDeepColor,
  )
}

private fun formatOneDecimal(value: Float): String {
  return String.format(Locale.US, "%.1f", value)
}

private fun formatCleanMetric(value: Float): String {
  val rounded = (value * 10f).roundToInt() / 10f
  return if (abs(rounded - rounded.roundToInt()) < 0.05f) {
    rounded.roundToInt().toString()
  } else {
    formatOneDecimal(rounded)
  }
}

private fun formatFeetInches(totalInches: Float): String {
  val safeIn = totalInches.coerceAtLeast(0f)
  val feet = floor(safeIn / 12f).toInt()
  val remInches = safeIn - (feet * 12f)
  return String.format(Locale.US, "%d' %04.1f\"", feet, remInches)
}

private fun formatStonesPounds(totalLbs: Float): String {
  val safeLbs = totalLbs.coerceAtLeast(0f)
  val stones = floor(safeLbs / 14f).toInt()
  val remLbs = safeLbs - (stones * 14f)
  return String.format(Locale.US, "%d st %.1f lb", stones, remLbs)
}

@Immutable
private data class PersonalResponsiveMetrics(
  val scale: Float,
  val fontCompensation: Float,
  val contentMaxWidth: Dp,
  val horizontalPadding: Dp,
  val topContentPadding: Dp,
  val bottomContentPadding: Dp,
  val headerHeight: Dp,
  val titleFontSize: TextUnit,
  val subtitleFontSize: TextUnit,
  val subtitleLineHeight: TextUnit,
  val labelToBoxGap: Dp,
  val titleToFirstFieldGap: Dp,
  val formFieldGap: Dp,
  val sectionToCardGap: Dp,
  val cardToCtaGap: Dp,
  val unifiedBlockMinHeight: Dp,
  val bmiCardMinHeight: Dp,
  val bmiCardPadding: Dp,
  val bmiSectionSpacing: Dp,
  val bmiPrimaryFontSize: TextUnit,
  val bmiUnitFontSize: TextUnit,
  val bmiBarHeight: Dp,
  val secondaryStatFontSize: TextUnit,
  val ctaHeight: Dp,
) {
  fun spScaled(base: Float, minSp: Float, maxSp: Float): TextUnit {
    return ((base * scale).coerceIn(minSp, maxSp) * fontCompensation).sp
  }
}

private fun computePersonalMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): PersonalResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val rawScale = min(w / 385f, h / 825f).coerceIn(0.76f, 1.30f)

  // Normalize Android system fontScale so large accessibility font sizes never crash or overflow
  val safeFontScale = fontScale.coerceIn(0.5f, 3.0f)
  val targetFontGrowth = if (safeFontScale >= 1f) {
    1f + (safeFontScale - 1f).coerceAtMost(1.2f) * 0.14f
  } else {
    1f - (1f - safeFontScale).coerceAtMost(0.5f) * 0.20f
  }
  val fontCompensation = targetFontGrowth / safeFontScale

  fun scaledSp(base: Float, minSp: Float, maxSp: Float): TextUnit {
    return ((base * rawScale).coerceIn(minSp, maxSp) * fontCompensation).sp
  }

  val contentMaxWidth = if (w >= 600f) {
    (w * 0.78f).coerceIn(450f, 580f).dp
  } else {
    w.dp
  }

  // Resolution-adaptive logical gaps matching Step 01 Profile Section
  val titleToFirstFieldGap = (h * 0.020f).coerceIn(14f, 22f).dp
  val formFieldGap = (h * 0.018f).coerceIn(13f, 20f).dp
  val sectionToCardGap = (h * 0.021f).coerceIn(15f, 24f).dp
  val cardToCtaGap = (h * 0.020f).coerceIn(14f, 23f).dp

  return PersonalResponsiveMetrics(
    scale = rawScale,
    fontCompensation = fontCompensation,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (20f * rawScale).coerceIn(16f, 28f).dp,
    topContentPadding = (11f * rawScale).coerceIn(8f, 16f).dp,
    bottomContentPadding = (8f * rawScale).coerceIn(6f, 12f).dp,
    headerHeight = (56f * rawScale).coerceIn(50f, 66f).dp,
    titleFontSize = scaledSp(23.5f, 19f, 28f),
    subtitleFontSize = scaledSp(12f, 10.2f, 14f),
    subtitleLineHeight = scaledSp(17.2f, 14.5f, 20f),
    labelToBoxGap = (7f * rawScale).coerceIn(5.5f, 9.5f).dp,
    titleToFirstFieldGap = titleToFirstFieldGap,
    formFieldGap = formFieldGap,
    sectionToCardGap = sectionToCardGap,
    cardToCtaGap = cardToCtaGap,
    unifiedBlockMinHeight = (96f * rawScale).coerceIn(86f, 110f).dp,
    bmiCardMinHeight = (h * 0.31f).coerceIn(242f, 324f).dp,
    bmiCardPadding = (19f * rawScale).coerceIn(16f, 25f).dp,
    bmiSectionSpacing = (14f * rawScale).coerceIn(11f, 18f).dp,
    bmiPrimaryFontSize = scaledSp(41f, 32f, 49f),
    bmiUnitFontSize = scaledSp(13.5f, 11f, 16f),
    bmiBarHeight = (12.5f * rawScale).coerceIn(10.5f, 15f).dp,
    secondaryStatFontSize = scaledSp(16.5f, 13.8f, 19.5f),
    ctaHeight = (49f * rawScale).coerceIn(45f, 56f).dp,
  )
}

/**
 * Floating dull border flash modifier while actively writing in Height or Weight inputs.
 * Matches the exact tactile behavior of Step 01 Profile Section inputs.
 */
@Composable
private fun Modifier.personalWritingFloatingBorder(
  isFocused: Boolean,
  cornerRadiusDp: Dp = 12.dp,
): Modifier {
  val focusWeight by animateFloatAsState(
    targetValue = if (isFocused) 1f else 0f,
    animationSpec = tween(durationMillis = 240, easing = PersonalEaseOut),
    label = "personal_writing_focus_weight",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "personal_writing_dull_flash")
  val sweepPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "personal_writing_sweep_phase",
  )
  val dullPulse by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "personal_writing_dull_pulse",
  )

  val focusWeightProvider = remember { { focusWeight } }
  val sweepPhaseProvider = remember { { sweepPhase } }
  val dullPulseProvider = remember { { dullPulse } }

  return this.drawWithCache {
    val cornerPx = cornerRadiusDp.toPx()
    val cornerRadius = CornerRadius(cornerPx, cornerPx)

    val floatingDropShadowPaint = Paint().apply {
      isAntiAlias = true
      asFrameworkPaint().apply {
        color = android.graphics.Color.argb(0, 0, 0, 0)
        maskFilter = BlurMaskFilter(safeBlurRadius(15.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
      }
    }
    val dullAmbientRimPaint = Paint().apply {
      isAntiAlias = true
      asFrameworkPaint().apply {
        color = android.graphics.Color.argb(0, 212, 212, 218)
        maskFilter = BlurMaskFilter(safeBlurRadius(10.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
      }
    }

    onDrawWithContent {
      val fw = focusWeightProvider().coerceIn(0f, 1f)
      val sweep = sweepPhaseProvider()
      val pulse = dullPulseProvider()

      if (fw > 0.01f && size.width > 1f && size.height > 1f) {
        floatingDropShadowPaint.asFrameworkPaint().alpha = (195f * fw).roundToInt().coerceIn(0, 220)
        dullAmbientRimPaint.asFrameworkPaint().alpha = ((32f + 24f * pulse) * fw).roundToInt().coerceIn(0, 75)

        drawIntoCanvas { canvas ->
          canvas.drawRoundRect(
            2.dp.toPx(),
            6.dp.toPx(),
            size.width - 2.dp.toPx(),
            size.height + 7.dp.toPx(),
            cornerPx,
            cornerPx,
            floatingDropShadowPaint,
          )
          canvas.drawRoundRect(
            -1.dp.toPx(),
            -1.dp.toPx(),
            size.width + 1.dp.toPx(),
            size.height + 2.dp.toPx(),
            cornerPx,
            cornerPx,
            dullAmbientRimPaint,
          )
        }
      }

      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF141419),
            Color(0xFF0A0A0D),
          ),
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius,
      )
      if (fw > 0.01f) {
        drawRoundRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF1A1A22).copy(alpha = 0.90f * fw),
              Color(0xFF0C0C10).copy(alpha = 0.90f * fw),
            ),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
        )
      }

      drawContent()

      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.16f),
            Zinc700.copy(alpha = 0.72f),
            Zinc850,
          ),
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx()),
      )

      if (fw > 0.01f && size.width > 1f && size.height > 1f) {
        val w = size.width
        val h = size.height
        val spanX = w * 0.48f
        val spanY = h * 0.90f
        val waveT = (sin(sweep * Math.PI.toFloat() * 2f) * 0.5f + 0.5f).coerceIn(0f, 1f)
        val cx = -spanX * 0.3f + (w + spanX * 0.6f) * waveT
        val cy = h * 0.5f

        drawRoundRect(
          color = Zinc600.copy(alpha = (0.45f + 0.25f * pulse) * fw),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = 1.15.dp.toPx()),
        )

        drawRoundRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.Transparent,
              Zinc500.copy(alpha = 0.38f * fw),
              Zinc300.copy(alpha = (0.58f + 0.18f * pulse) * fw),
              PureWhite.copy(alpha = (0.68f + 0.18f * pulse) * fw),
              Zinc300.copy(alpha = (0.58f + 0.18f * pulse) * fw),
              Zinc500.copy(alpha = 0.38f * fw),
              Color.Transparent,
            ),
            start = Offset(cx - spanX, cy - spanY),
            end = Offset(cx + spanX, cy + spanY),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = 1.4.dp.toPx()),
        )
      }
    }
  }
}

@Composable
fun PersonalDetailsScreen(
  subjectName: String,
  selectedSex: String,
  ageYears: Int,
  heightCm: Float,
  onHeightCmChange: (Float) -> Unit,
  selectedHeightUnit: HeightUnit,
  onSelectHeightUnit: (HeightUnit) -> Unit,
  weightKg: Float,
  onWeightKgChange: (Float) -> Unit,
  selectedWeightUnit: WeightUnit = WeightUnit.KG,
  onSelectWeightUnit: (WeightUnit) -> Unit = {},
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  val haptics = rememberBioSyncHaptics()

  val bmiTelemetry = remember(heightCm, weightKg, ageYears, selectedSex) {
    calculateBmiTelemetry(
      heightCm = heightCm,
      weightKg = weightKg,
      ageYears = ageYears,
      biologicalSex = selectedSex,
    )
  }

  val coroutineScope = rememberCoroutineScope()
  val headerStepPhase = remember { Animatable(0f) }
  val bmiEntryPhase = remember { Animatable(0f) }
  val borderFlashPhase = remember { Animatable(0f) }

  // Top STEP 02 OF 03 progress bar: animates ONLY when switching onto the Personal Details page
  LaunchedEffect(Unit) {
    headerStepPhase.snapTo(0f)
    headerStepPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 720, easing = PersonalEaseOut),
    )
  }

  // Initial entry sweep for the BMI telemetry card
  LaunchedEffect(Unit) {
    bmiEntryPhase.snapTo(0f)
    bmiEntryPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 760, easing = PersonalEaseOut),
    )
  }

  // Smooth lively animated values whenever height or weight changes
  val animatedBmiValue by animateFloatAsState(
    targetValue = bmiTelemetry.bmi,
    animationSpec = tween(durationMillis = 420, easing = PersonalEaseOut),
    label = "animated_live_bmi",
  )
  val animatedBmiPrime by animateFloatAsState(
    targetValue = bmiTelemetry.bmiPrime,
    animationSpec = tween(durationMillis = 420, easing = PersonalEaseOut),
    label = "animated_live_bmi_prime",
  )
  val animatedPonderal by animateFloatAsState(
    targetValue = bmiTelemetry.ponderalIndex,
    animationSpec = tween(durationMillis = 420, easing = PersonalEaseOut),
    label = "animated_live_ponderal",
  )
  val animatedGaugeProgress by animateFloatAsState(
    targetValue = bmiTelemetry.gaugeProgress,
    animationSpec = tween(durationMillis = 480, easing = PersonalEaseOut),
    label = "animated_bmi_gauge_progress",
  )
  val animatedBmr by animateFloatAsState(
    targetValue = bmiTelemetry.restingBmrKcal.toFloat(),
    animationSpec = tween(durationMillis = 420, easing = PersonalEaseOut),
    label = "animated_resting_bmr",
  )

  val onContinueClick: () -> Unit = {
    haptics.confirm()
    coroutineScope.launch {
      borderFlashPhase.snapTo(0f)
      borderFlashPhase.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 680, easing = PersonalEaseOut),
      )
      borderFlashPhase.snapTo(0f)
    }
  }

  val headerStepPhaseProvider = remember(headerStepPhase) { { headerStepPhase.value } }
  val bmiEntryPhaseProvider = remember(bmiEntryPhase) { { bmiEntryPhase.value } }
  val borderFlashProvider = remember(borderFlashPhase) { { borderFlashPhase.value } }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack),
    containerColor = ObsidianBlack,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBlack)
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.systemBars),
      contentAlignment = Alignment.TopCenter,
    ) {
      val viewportHeight = maxHeight
      val fontScale = LocalDensity.current.fontScale
      val metrics = remember(maxWidth, maxHeight, fontScale) {
        computePersonalMetrics(
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          fontScale = fontScale,
        )
      }
      val scrollState = rememberScrollState()
      var activeEditSection by remember { mutableStateOf(PersonalActiveEditSection.NONE) }
      var heightSectionTopY by remember { mutableIntStateOf(0) }
      var weightSectionTopY by remember { mutableIntStateOf(0) }

      // Always redirect to the very top of the page when entering Personal Details
      LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
      }

      // Automatically scroll up while writing/editing Height or Weight so the active field
      // and the Live Animated BMI Calculation section remain framed cleanly above the keyboard
      LaunchedEffect(activeEditSection) {
        when (activeEditSection) {
          PersonalActiveEditSection.HEIGHT -> {
            snapshotFlow { scrollState.maxValue to heightSectionTopY }
              .collectLatest { (maxVal, heightY) ->
                if (maxVal > 0 && heightY > 0) {
                  val targetScroll = (heightY - 16).coerceIn(0, maxVal)
                  scrollState.animateScrollTo(
                    value = targetScroll,
                    animationSpec = tween(durationMillis = 280, easing = PersonalEaseOut),
                  )
                }
              }
          }
          PersonalActiveEditSection.WEIGHT -> {
            snapshotFlow { scrollState.maxValue to weightSectionTopY }
              .collectLatest { (maxVal, weightY) ->
                if (maxVal > 0) {
                  val targetScroll = if (weightY > 0) max(weightY - 24, maxVal) else maxVal
                  scrollState.animateScrollTo(
                    value = targetScroll.coerceIn(0, maxVal),
                    animationSpec = tween(durationMillis = 280, easing = PersonalEaseOut),
                  )
                }
              }
          }
          PersonalActiveEditSection.NONE -> Unit
        }
      }

      // Single unified page: Header (BioSync + STEP 02 OF 03) and Body live on one seamless canvas
      Column(
        modifier = Modifier
          .widthIn(max = metrics.contentMaxWidth)
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(scrollState)
          .heightIn(min = viewportHeight)
          .padding(
            start = metrics.horizontalPadding,
            end = metrics.horizontalPadding,
            top = (6f * metrics.scale).coerceIn(4f, 10f).dp,
            bottom = metrics.bottomContentPadding,
          )
          .testTag("personal_details_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Header Bar (BioSync + STEP 02 OF 03 + Shiny Glowy White Step Bar)
        PersonalHeaderBar(
          onBackClick = {
            haptics.selection()
            onNavigateBack()
          },
          phaseProvider = headerStepPhaseProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // 1. Top Eyebrow, Title & Description under Personal Details
        Column(
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.personal_eyebrow),
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
              letterSpacing = 1.6.sp,
            )
            if (subjectName.isNotBlank()) {
              Text(
                text = "${subjectName.uppercase(Locale.US)} • ${selectedSex.uppercase(Locale.US)} • ${ageYears}Y",
                color = Zinc500,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.spScaled(9.8f, 8.2f, 11.2f),
                letterSpacing = 0.7.sp,
                maxLines = 1,
              )
            }
          }
          Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
          Text(
            text = stringResource(R.string.personal_title),
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.titleFontSize,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.testTag("personal_title_text"),
          )
          Spacer(modifier = Modifier.height((5f * metrics.scale).coerceIn(3.5f, 7.5f).dp))
          Text(
            text = stringResource(R.string.personal_subtitle),
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.subtitleFontSize,
            lineHeight = metrics.subtitleLineHeight,
            modifier = Modifier.testTag("personal_subtitle_text"),
          )
        }

        Spacer(modifier = Modifier.height(metrics.titleToFirstFieldGap))

        // 2. Single Unified Height Card (Primary Editable Metric + Clickable Bottom Conversion Swap)
        HeightUnifiedCardSection(
          heightCm = heightCm,
          selectedUnit = selectedHeightUnit,
          onSelectUnit = { newUnit ->
            haptics.selection()
            onSelectHeightUnit(newUnit)
          },
          onHeightCmChange = onHeightCmChange,
          onFocusChange = { focused ->
            if (focused) {
              haptics.tick()
              activeEditSection = PersonalActiveEditSection.HEIGHT
            } else if (activeEditSection == PersonalActiveEditSection.HEIGHT) {
              activeEditSection = PersonalActiveEditSection.NONE
            }
          },
          haptics = haptics,
          metrics = metrics,
          modifier = Modifier.onGloballyPositioned { coords ->
            heightSectionTopY = coords.positionInParent().y.roundToInt()
          },
        )

        Spacer(modifier = Modifier.height(metrics.formFieldGap))

        // 3. Single Unified Weight Card (Primary Editable Metric + Clickable Bottom Conversion Swap + Lively Steppers)
        WeightUnifiedCardSection(
          weightKg = weightKg,
          selectedUnit = selectedWeightUnit,
          onSelectUnit = { newUnit ->
            haptics.selection()
            onSelectWeightUnit(newUnit)
          },
          onWeightKgChange = onWeightKgChange,
          onFocusChange = { focused ->
            if (focused) {
              haptics.tick()
              activeEditSection = PersonalActiveEditSection.WEIGHT
            } else if (activeEditSection == PersonalActiveEditSection.WEIGHT) {
              activeEditSection = PersonalActiveEditSection.NONE
            }
          },
          haptics = haptics,
          metrics = metrics,
          modifier = Modifier.onGloballyPositioned { coords ->
            weightSectionTopY = coords.positionInParent().y.roundToInt()
          },
        )

        Spacer(modifier = Modifier.height(metrics.sectionToCardGap))

        // 4. Impressive Animated BMI & Scientific Metabolic Calculation Card (with Welcome Screen Progress Colors)
        BiometricMassIndexCard(
          telemetry = bmiTelemetry,
          animatedBmi = animatedBmiValue,
          animatedPrime = animatedBmiPrime,
          animatedPonderal = animatedPonderal,
          animatedGaugeProgress = animatedGaugeProgress,
          animatedBmr = animatedBmr.roundToInt(),
          entryPhaseProvider = bmiEntryPhaseProvider,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.cardToCtaGap))

        // 5. Primary Continue Button & Encrypted Footer
        PersonalContinueSection(
          onContinueClick = onContinueClick,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )
      }
    }
  }
}

@Composable
private fun PersonalHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
  metrics: PersonalResponsiveMetrics,
) {
  val backDesc = stringResource(R.string.cd_back_to_profile)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = metrics.headerHeight)
      .padding(vertical = (4f * metrics.scale).coerceIn(2f, 7f).dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Return Button (<)
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .clickable(
          role = Role.Button,
          onClick = onBackClick,
        )
        .semantics { contentDescription = backDesc }
        .testTag("personal_back_button"),
      contentAlignment = Alignment.CenterStart,
    ) {
      Canvas(modifier = Modifier.size((18f * metrics.scale).coerceIn(16f, 22f).dp)) {
        val w = size.width
        val h = size.height
        val chevron = Path().apply {
          moveTo(w * 0.68f, h * 0.14f)
          lineTo(w * 0.24f, h * 0.50f)
          lineTo(w * 0.68f, h * 0.86f)
        }
        drawPath(
          path = chevron,
          color = Zinc300,
          style = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }

    // Center Title + STEP 02 OF 03 + Shiny Glowy White Step Progress Bar
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(R.string.calib_header_title),
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(15f, 13f, 17.5f),
        letterSpacing = (-0.2).sp,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = stringResource(R.string.personal_header_step),
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10.2f, 9f, 12f),
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp))

      StepTwoShinyGlowBar(
        phaseProvider = phaseProvider,
        modifier = Modifier
          .width((108f * metrics.scale).coerceIn(92f, 136f).dp)
          .height((4f * metrics.scale).coerceIn(3.5f, 5f).dp)
          .testTag("step_02_glow_bar"),
      )
    }

    // Right Luminous Status Dot
    Box(
      modifier = Modifier.size(48.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      PersonalLuminousColorDot(
        color = PureWhite,
        size = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
      )
    }
  }
}

@Composable
private fun PersonalLuminousColorDot(
  color: Color,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Spacer(
    modifier = modifier
      .size(size)
      .drawWithCache {
        val argb = color.copy(alpha = 0.65f).toArgb()
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            this.color = argb
            maskFilter = BlurMaskFilter(
              safeBlurRadius(this@drawWithCache.size.minDimension * 0.72f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }
        onDrawBehind {
          val r = (this.size.minDimension / 2f).coerceAtLeast(1f)
          drawIntoCanvas { canvas ->
            canvas.drawCircle(center, r * 1.25f, glowPaint)
          }
          drawCircle(
            color = color,
            radius = r,
            center = center,
          )
        }
      },
  )
}

/**
 * Shiny, glowing pure-white step indicator bar directly below "STEP 02 OF 03".
 * Smoothly animates from Step 01 (0.36f) to Step 02 (0.68f) ONLY when switching to this page.
 */
@Composable
private fun StepTwoShinyGlowBar(
  phaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  Spacer(
    modifier = modifier
      .graphicsLayer()
      .drawWithCache {
        val barH = size.height.coerceAtLeast(1f)
        val radius = CornerRadius(barH / 2f, barH / 2f)
        val outerGlowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(150, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(barH * 1.9f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val innerHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(215, 245, 245, 250)
            maskFilter = BlurMaskFilter(safeBlurRadius(barH * 0.75f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val shinyCoreBrush = Brush.horizontalGradient(
          colors = listOf(
            Color(0xFFC8CCD4),
            Color(0xFFF2F4F7),
            PureWhite,
          ),
        )

        onDrawBehind {
          if (size.width <= 1f || size.height <= 0.5f) return@onDrawBehind
          val p = phaseProvider().coerceIn(0f, 1f)
          // Animates from Step 1 (0.36f) to Step 2 (0.68f) on page transition
          val activeFraction = 0.36f + (0.68f - 0.36f) * p

          drawRoundRect(
            color = Zinc850,
            topLeft = Offset.Zero,
            size = Size(size.width, barH),
            cornerRadius = radius,
          )
          drawRoundRect(
            color = Zinc700.copy(alpha = 0.45f),
            topLeft = Offset.Zero,
            size = Size(size.width, barH),
            cornerRadius = radius,
            style = Stroke(width = 0.7.dp.toPx()),
          )

          val step1X = size.width * 0.36f
          val step2X = size.width * 0.68f

          if (activeFraction > 0.01f) {
            val minFill = barH.coerceAtMost(size.width)
            val fillW = (size.width * activeFraction).coerceIn(minFill, size.width)

            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                0f,
                -barH * 0.25f,
                fillW,
                barH * 1.25f,
                radius.x,
                radius.y,
                outerGlowPaint,
              )
              canvas.drawRoundRect(
                0f,
                0f,
                fillW,
                barH,
                radius.x,
                radius.y,
                innerHaloPaint,
              )
            }

            drawRoundRect(
              brush = shinyCoreBrush,
              topLeft = Offset.Zero,
              size = Size(fillW, barH),
              cornerRadius = radius,
            )

            drawRoundRect(
              color = PureWhite,
              topLeft = Offset(barH * 0.2f, 0f),
              size = Size((fillW - barH * 0.4f).coerceAtLeast(1f), barH * 0.45f),
              cornerRadius = radius,
            )
          }

          // Segment notches
          drawLine(
            color = ObsidianBlack.copy(alpha = 0.75f),
            start = Offset(step1X, 0f),
            end = Offset(step1X, barH),
            strokeWidth = 1.2.dp.toPx(),
          )
          drawLine(
            color = ObsidianBlack.copy(alpha = 0.75f),
            start = Offset(step2X, 0f),
            end = Offset(step2X, barH),
            strokeWidth = 1.2.dp.toPx(),
          )
        }
      },
  )
}

/**
 * Single Unified Height Section:
 * - Everything lives in ONE sleek block (no extra side-by-side conversion block, no external toggle bar,
 *   and no "FOCUS: INCH • LIVE CM" text).
 * - When CM is active (default), the primary top input edits CM (with lively - / + steppers),
 *   and the small bottom portion displays the converted feet/inches & inches (e.g. "5' 07.7" • 67.7 in").
 * - Tapping that small bottom portion swaps the active editable metric to INCH at the top,
 *   while CM ("172.0 cm • 1.72 m") moves down into the small clickable portion below.
 */
@Composable
private fun HeightUnifiedCardSection(
  heightCm: Float,
  selectedUnit: HeightUnit,
  onSelectUnit: (HeightUnit) -> Unit,
  onHeightCmChange: (Float) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  haptics: BioSyncHapticController,
  metrics: PersonalResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  var isHeightFocused by remember { mutableStateOf(false) }
  var hadFocus by remember { mutableStateOf(false) }

  LaunchedEffect(isHeightFocused) {
    onFocusChange(isHeightFocused)
  }

  val inchesFromCm = remember(heightCm) { heightCm / 2.54f }

  fun formatHeightForUnit(cmVal: Float, unit: HeightUnit): String {
    return when (unit) {
      HeightUnit.CM -> formatCleanMetric(cmVal)
      HeightUnit.INCH -> formatOneDecimal(cmVal / 2.54f)
    }
  }

  var inputText by rememberSaveable(selectedUnit) {
    mutableStateOf(formatHeightForUnit(heightCm, selectedUnit))
  }

  // Keep inputText synchronized when unfocused or when unit changes
  LaunchedEffect(heightCm, selectedUnit, isHeightFocused) {
    if (!isHeightFocused) {
      inputText = formatHeightForUnit(heightCm, selectedUnit)
    }
  }

  // Helper for stepper buttons so BOTH heightCm and inputText update immediately (even while focused)
  val applyHeightStep: (Float) -> Unit = { deltaCm ->
    haptics.step()
    val updatedCm = (heightCm + deltaCm).coerceIn(100f, 240f)
    onHeightCmChange(updatedCm)
    inputText = formatHeightForUnit(updatedCm, selectedUnit)
  }

  val commitHeightInput: () -> Unit = {
    val parsed = inputText.toFloatOrNull()
    if (parsed != null) {
      val newCm = when (selectedUnit) {
        HeightUnit.CM -> parsed.coerceIn(100f, 240f)
        HeightUnit.INCH -> (parsed * 2.54f).coerceIn(100f, 240f)
      }
      onHeightCmChange(newCm)
      inputText = formatHeightForUnit(newCm, selectedUnit)
    } else {
      inputText = formatHeightForUnit(heightCm, selectedUnit)
    }
  }

  val primaryFontSize = metrics.spScaled(24f, 20f, 28f)
  val centeredPrimaryStyle = remember(primaryFontSize) {
    TextStyle(
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = primaryFontSize,
      lineHeight = primaryFontSize * 1.12f,
      letterSpacing = (-0.5).sp,
      platformStyle = PlatformTextStyle(includeFontPadding = false),
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
      ),
    )
  }

  val primaryUnitSuffix = if (selectedUnit == HeightUnit.CM) "cm" else "in"
  val bottomConvertedReadout = if (selectedUnit == HeightUnit.CM) {
    "${formatFeetInches(inchesFromCm)}  •  ${formatOneDecimal(inchesFromCm)} in"
  } else {
    "${formatOneDecimal(heightCm)} cm  •  ${String.format(Locale.US, "%.2f", heightCm / 100f)} m"
  }
  val bottomSwapHint = if (selectedUnit == HeightUnit.CM) {
    "⇌ TAP FOR INCH"
  } else {
    "⇌ TAP FOR CM"
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.labelToBoxGap),
  ) {
    // Clean Section Label Row (Removed "FOCUS: INCH • LIVE CM" clutter)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.personal_height_label),
        color = Zinc200,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.5f, 11f, 14.5f),
      )
      Text(
        text = if (selectedUnit == HeightUnit.CM) "METRIC • CM" else "IMPERIAL • INCH",
        color = Zinc500,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.6f, 8.4f, 11f),
        letterSpacing = 0.8.sp,
      )
    }

    // ONE SINGLE UNIFIED HEIGHT BLOCK
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = metrics.unifiedBlockMinHeight)
        .personalWritingFloatingBorder(isFocused = isHeightFocused, cornerRadiusDp = 12.dp)
        .padding(horizontal = 15.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Row: Primary Editable Input + Active Unit Badge + Lively - / + Steppers
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Left: Primary Editable Height Value + Active Unit Pill
        BasicTextField(
          value = inputText,
          onValueChange = { raw ->
            haptics.tick()
            val filtered = raw.filter { it.isDigit() || it == '.' }.take(5)
            if (filtered.count { it == '.' } <= 1) {
              inputText = filtered
              val parsed = filtered.toFloatOrNull()
              if (parsed != null) {
                when (selectedUnit) {
                  HeightUnit.CM -> if (parsed in 90f..250f) onHeightCmChange(parsed)
                  HeightUnit.INCH -> if (parsed in 36f..98f) onHeightCmChange(parsed * 2.54f)
                }
              }
            }
          },
          singleLine = true,
          cursorBrush = SolidColor(PureWhite),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              haptics.confirm()
              commitHeightInput()
              focusManager.clearFocus()
            },
          ),
          textStyle = centeredPrimaryStyle,
          decorationBox = { innerTextField ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PersonalLuminousColorDot(
                color = if (isHeightFocused) CyberCyan else PureWhite,
                size = 6.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Box(
                modifier = Modifier.widthIn(min = 58.dp, max = 128.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                if (inputText.isEmpty()) {
                  Text(
                    text = if (selectedUnit == HeightUnit.CM) "172" else "67.7",
                    style = centeredPrimaryStyle.copy(
                      color = Zinc600,
                      fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                  )
                }
                innerTextField()
              }
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(PureWhite.copy(alpha = 0.12f))
                  .border(0.8.dp, PureWhite.copy(alpha = 0.34f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = primaryUnitSuffix,
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(11.8f, 10f, 13.8f),
                )
              }
            }
          },
          modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
              isHeightFocused = state.isFocused
              if (state.isFocused) {
                hadFocus = true
              } else if (hadFocus) {
                commitHeightInput()
              }
            }
            .testTag("height_primary_input"),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Right: Precision Tactile - / + Steppers (Lively updates both CM and INCH immediately)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MicroStepperButton(
            symbol = "−",
            onClick = {
              val deltaCm = if (selectedUnit == HeightUnit.CM) -1f else -1.27f
              applyHeightStep(deltaCm)
            },
            metrics = metrics,
            testTag = "height_decrement_button",
          )
          MicroStepperButton(
            symbol = "+",
            onClick = {
              val deltaCm = if (selectedUnit == HeightUnit.CM) 1f else 1.27f
              applyHeightStep(deltaCm)
            },
            metrics = metrics,
            testTag = "height_increment_button",
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      HorizontalDivider(
        thickness = 0.8.dp,
        color = Zinc800.copy(alpha = 0.75f),
      )

      Spacer(modifier = Modifier.height(7.dp))

      // Bottom Clickable Conversion Portion (Exact text size & color section where feet/inches is shown;
      // clicking this small portion swaps the active unit so the user can edit in that metric and the other comes down)
      val swapRowShape = RoundedCornerShape(6.dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(swapRowShape)
          .background(Color(0xFF121318).copy(alpha = 0.75f))
          .border(0.7.dp, Zinc800.copy(alpha = 0.9f), swapRowShape)
          .clickable(
            role = Role.Button,
            onClick = {
              val nextUnit = if (selectedUnit == HeightUnit.CM) HeightUnit.INCH else HeightUnit.CM
              onSelectUnit(nextUnit)
              inputText = formatHeightForUnit(heightCm, nextUnit)
            },
          )
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("height_secondary_conversion_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = bottomConvertedReadout,
          color = Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(10.8f, 9.4f, 12.4f),
          letterSpacing = 0.2.sp,
          modifier = Modifier.testTag("height_converted_value_text"),
        )
        Text(
          text = bottomSwapHint,
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(9.2f, 8f, 10.6f),
          letterSpacing = 0.6.sp,
        )
      }
    }
  }
}

/**
 * Single Unified Weight Section:
 * - Redesigned to match the sleek, high-contrast single-block architecture of Height.
 * - Supports primary KG (default) with live converted LBS & stones in the small clickable bottom portion.
 * - Tapping the small bottom conversion portion swaps LBS to the primary editable top field and moves KG down.
 * - Fixes the - / + stepper buttons so clicking - or + lively and immediately updates BOTH the primary weight input text
 *   and the secondary conversion readout (even while the input field is focused).
 */
@Composable
private fun WeightUnifiedCardSection(
  weightKg: Float,
  selectedUnit: WeightUnit,
  onSelectUnit: (WeightUnit) -> Unit,
  onWeightKgChange: (Float) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  haptics: BioSyncHapticController,
  metrics: PersonalResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  var isWeightFocused by remember { mutableStateOf(false) }
  var hadFocus by remember { mutableStateOf(false) }

  LaunchedEffect(isWeightFocused) {
    onFocusChange(isWeightFocused)
  }

  val weightLbs = remember(weightKg) { weightKg * 2.20462f }

  fun formatWeightForUnit(kgVal: Float, unit: WeightUnit): String {
    return when (unit) {
      WeightUnit.KG -> formatOneDecimal(kgVal)
      WeightUnit.LBS -> formatOneDecimal(kgVal * 2.20462f)
    }
  }

  var weightInputText by rememberSaveable(selectedUnit) {
    mutableStateOf(formatWeightForUnit(weightKg, selectedUnit))
  }

  // Keep weightInputText synchronized when unfocused or when unit changes
  LaunchedEffect(weightKg, selectedUnit, isWeightFocused) {
    if (!isWeightFocused) {
      weightInputText = formatWeightForUnit(weightKg, selectedUnit)
    }
  }

  // Lively stepper helper: immediately updates BOTH weightKg state AND weightInputText even while focused
  val applyWeightStep: (Float) -> Unit = { deltaInSelectedUnit ->
    haptics.step()
    val updatedKg = when (selectedUnit) {
      WeightUnit.KG -> (weightKg + deltaInSelectedUnit).coerceIn(25f, 260f)
      WeightUnit.LBS -> {
        val nextLbs = (weightKg * 2.20462f) + deltaInSelectedUnit
        (nextLbs / 2.20462f).coerceIn(25f, 260f)
      }
    }
    onWeightKgChange(updatedKg)
    weightInputText = formatWeightForUnit(updatedKg, selectedUnit)
  }

  val commitWeightInput: () -> Unit = {
    val parsed = weightInputText.toFloatOrNull()
    if (parsed != null) {
      val validKg = when (selectedUnit) {
        WeightUnit.KG -> parsed.coerceIn(25f, 260f)
        WeightUnit.LBS -> (parsed / 2.20462f).coerceIn(25f, 260f)
      }
      onWeightKgChange(validKg)
      weightInputText = formatWeightForUnit(validKg, selectedUnit)
    } else {
      weightInputText = formatWeightForUnit(weightKg, selectedUnit)
    }
  }

  val primaryFontSize = metrics.spScaled(24f, 20f, 28f)
  val centeredWeightStyle = remember(primaryFontSize) {
    TextStyle(
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = primaryFontSize,
      lineHeight = primaryFontSize * 1.12f,
      letterSpacing = (-0.5).sp,
      platformStyle = PlatformTextStyle(includeFontPadding = false),
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
      ),
    )
  }

  val primaryUnitSuffix = if (selectedUnit == WeightUnit.KG) "kg" else "lbs"
  val bottomConvertedReadout = if (selectedUnit == WeightUnit.KG) {
    "${formatOneDecimal(weightLbs)} lbs  •  ${formatStonesPounds(weightLbs)}"
  } else {
    "${formatOneDecimal(weightKg)} kg  •  ${formatStonesPounds(weightLbs)}"
  }
  val bottomSwapHint = if (selectedUnit == WeightUnit.KG) {
    "⇌ TAP FOR LBS"
  } else {
    "⇌ TAP FOR KG"
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.labelToBoxGap),
  ) {
    // Section Label Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.personal_weight_label),
        color = Zinc200,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.5f, 11f, 14.5f),
      )
      Text(
        text = if (selectedUnit == WeightUnit.KG) "METRIC • KG" else "IMPERIAL • LBS",
        color = Zinc500,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.6f, 8.4f, 11f),
        letterSpacing = 0.8.sp,
      )
    }

    // ONE SINGLE UNIFIED WEIGHT BLOCK
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = metrics.unifiedBlockMinHeight)
        .personalWritingFloatingBorder(isFocused = isWeightFocused, cornerRadiusDp = 12.dp)
        .padding(horizontal = 15.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Row: Primary Editable Weight Input + Active Unit Pill + Lively - / + Steppers
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        BasicTextField(
          value = weightInputText,
          onValueChange = { raw ->
            haptics.tick()
            val filtered = raw.filter { it.isDigit() || it == '.' }.take(5)
            if (filtered.count { it == '.' } <= 1) {
              weightInputText = filtered
              val parsed = filtered.toFloatOrNull()
              if (parsed != null) {
                when (selectedUnit) {
                  WeightUnit.KG -> if (parsed in 25f..260f) onWeightKgChange(parsed)
                  WeightUnit.LBS -> if (parsed in 55f..573f) onWeightKgChange(parsed / 2.20462f)
                }
              }
            }
          },
          singleLine = true,
          cursorBrush = SolidColor(PureWhite),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              haptics.confirm()
              commitWeightInput()
              focusManager.clearFocus()
            },
          ),
          textStyle = centeredWeightStyle,
          decorationBox = { innerTextField ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PersonalLuminousColorDot(
                color = if (isWeightFocused) CyberCyan else PureWhite,
                size = 6.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Box(
                modifier = Modifier.widthIn(min = 58.dp, max = 128.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                if (weightInputText.isEmpty()) {
                  Text(
                    text = if (selectedUnit == WeightUnit.KG) "62.0" else "136.7",
                    style = centeredWeightStyle.copy(
                      color = Zinc600,
                      fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                  )
                }
                innerTextField()
              }
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(PureWhite.copy(alpha = 0.12f))
                  .border(0.8.dp, PureWhite.copy(alpha = 0.34f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = primaryUnitSuffix,
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(11.8f, 10f, 13.8f),
                )
              }
            }
          },
          modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
              isWeightFocused = state.isFocused
              if (state.isFocused) {
                hadFocus = true
              } else if (hadFocus) {
                commitWeightInput()
              }
            }
            .testTag("weight_kg_input"),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Right: Precision - / + Steppers (Immediately updates both primary input text & converted readout)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MicroStepperButton(
            symbol = "−",
            onClick = {
              val stepDelta = if (selectedUnit == WeightUnit.KG) -0.5f else -1.0f
              applyWeightStep(stepDelta)
            },
            metrics = metrics,
            testTag = "weight_decrement_button",
          )
          MicroStepperButton(
            symbol = "+",
            onClick = {
              val stepDelta = if (selectedUnit == WeightUnit.KG) 0.5f else 1.0f
              applyWeightStep(stepDelta)
            },
            metrics = metrics,
            testTag = "weight_increment_button",
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      HorizontalDivider(
        thickness = 0.8.dp,
        color = Zinc800.copy(alpha = 0.75f),
      )

      Spacer(modifier = Modifier.height(7.dp))

      // Bottom Clickable Conversion Portion (Swaps between KG and LBS when clicked)
      val swapRowShape = RoundedCornerShape(6.dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(swapRowShape)
          .background(Color(0xFF121318).copy(alpha = 0.75f))
          .border(0.7.dp, Zinc800.copy(alpha = 0.9f), swapRowShape)
          .clickable(
            role = Role.Button,
            onClick = {
              val nextUnit = if (selectedUnit == WeightUnit.KG) WeightUnit.LBS else WeightUnit.KG
              onSelectUnit(nextUnit)
              weightInputText = formatWeightForUnit(weightKg, nextUnit)
            },
          )
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("weight_lbs_conversion_text"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = bottomConvertedReadout,
          color = Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(10.8f, 9.4f, 12.4f),
          letterSpacing = 0.2.sp,
        )
        Text(
          text = bottomSwapHint,
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(9.2f, 8f, 10.6f),
          letterSpacing = 0.6.sp,
        )
      }
    }
  }
}

@Composable
private fun MicroStepperButton(
  symbol: String,
  onClick: () -> Unit,
  metrics: PersonalResponsiveMetrics,
  testTag: String,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(7.dp)
  Box(
    modifier = Modifier
      .size((28f * metrics.scale).coerceIn(24f, 32f).dp)
      .clip(shape)
      .background(
        if (isPressed) PureWhite.copy(alpha = 0.22f) else Zinc900,
      )
      .border(
        width = 1.dp,
        color = if (isPressed) PureWhite.copy(alpha = 0.65f) else Zinc700,
        shape = shape,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(color = PureWhite),
        role = Role.Button,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = symbol,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(14f, 12f, 16.5f),
    )
  }
}

/**
 * Impressive Animated BMI & Scientific Metabolic Calculation Card:
 * - Uses the Welcome Screen's progress bar colors (Hydration Sky-Cyan for Underweight,
 *   Steps Mint-Emerald & CyberCyan for Optimal, Carbs/Calories Gold-Orange for Overweight,
 *   Calories/Fats Coral-Magenta-Crimson for Obese) across the spectrum gauge, category badge,
 *   reticle, and zone indicators.
 * - Features a crystal-clear Optimal Mass Range pill presentation (Min TO Max) without an ambiguous "-" dash.
 * - Computes sex-calibrated Resting Metabolic Rate (Mifflin-St Jeor for Male +5 / Female -161)
 *   and displays the 70–85% accuracy & fitness coach / medical professional consultation note.
 */
@Composable
private fun BiometricMassIndexCard(
  telemetry: BmiTelemetryResult,
  animatedBmi: Float,
  animatedPrime: Float,
  animatedPonderal: Float,
  animatedGaugeProgress: Float,
  animatedBmr: Int,
  entryPhaseProvider: () -> Float,
  borderFlashProvider: () -> Float,
  metrics: PersonalResponsiveMetrics,
) {
  val cardCorner = (18f * metrics.scale).coerceIn(14f, 22f).dp
  val cardShape = RoundedCornerShape(cardCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "bmi_live_pulse_transition")
  val livePulse by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "bmi_live_pulse",
  )

  val animatedZonePrimary by animateColorAsState(
    targetValue = telemetry.zonePrimaryColor,
    animationSpec = tween(durationMillis = 360),
    label = "bmi_zone_primary_color",
  )
  val animatedZoneSecondary by animateColorAsState(
    targetValue = telemetry.zoneSecondaryColor,
    animationSpec = tween(durationMillis = 360),
    label = "bmi_zone_secondary_color",
  )

  val displayBmi by remember(animatedBmi, entryPhaseProvider) {
    derivedStateOf {
      val factor = entryPhaseProvider().coerceIn(0f, 1f)
      animatedBmi * factor
    }
  }
  val displayPrime by remember(animatedPrime, entryPhaseProvider) {
    derivedStateOf {
      val factor = entryPhaseProvider().coerceIn(0f, 1f)
      animatedPrime * factor
    }
  }
  val displayPonderal by remember(animatedPonderal, entryPhaseProvider) {
    derivedStateOf {
      val factor = entryPhaseProvider().coerceIn(0f, 1f)
      animatedPonderal * factor
    }
  }
  val displayBmr by remember(animatedBmr, entryPhaseProvider) {
    derivedStateOf {
      val factor = entryPhaseProvider().coerceIn(0f, 1f)
      (animatedBmr * factor).roundToInt()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = metrics.bmiCardMinHeight)
      .drawWithCache {
        val cornerPx = cardCorner.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val deepDropShadowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(205, 0, 0, 0)
            maskFilter = BlurMaskFilter(safeBlurRadius(26.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val zoneAmbientShadowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = animatedZonePrimary.copy(alpha = 0.16f).toArgb()
            maskFilter = BlurMaskFilter(safeBlurRadius(20.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val rearFlashHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(0, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(24.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val baseBorderBrush = Brush.verticalGradient(
          0.0f to animatedZonePrimary.copy(alpha = 0.42f),
          0.32f to Zinc700.copy(alpha = 0.65f),
          0.75f to Zinc850,
          1.0f to Zinc950,
        )

        onDrawWithContent {
          val flashProgress = borderFlashProvider().coerceIn(0f, 1f)
          val flashEnvelope = if (flashProgress > 0.001f && flashProgress < 0.999f) {
            sin(flashProgress * Math.PI.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }

          // 1. Deep 3D Floating Shadow + Category-Tinted Ambient Aura
          drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
              6.dp.toPx(),
              12.dp.toPx(),
              size.width - 6.dp.toPx(),
              size.height + 12.dp.toPx(),
              cornerPx,
              cornerPx,
              deepDropShadowPaint,
            )
            canvas.drawRoundRect(
              2.dp.toPx(),
              6.dp.toPx(),
              size.width - 2.dp.toPx(),
              size.height + 6.dp.toPx(),
              cornerPx,
              cornerPx,
              zoneAmbientShadowPaint,
            )

            if (flashEnvelope > 0.01f) {
              rearFlashHaloPaint.asFrameworkPaint().alpha = (180f * flashEnvelope).roundToInt().coerceIn(0, 215)
              val waveCx = size.width * flashProgress
              val waveCy = size.height * flashProgress
              val haloSpanX = size.width * 0.55f
              val haloSpanY = size.height * 0.55f
              canvas.drawRoundRect(
                (waveCx - haloSpanX).coerceAtLeast(-6.dp.toPx()),
                (waveCy - haloSpanY).coerceAtLeast(-6.dp.toPx()),
                (waveCx + haloSpanX).coerceAtMost(size.width + 6.dp.toPx()),
                (waveCy + haloSpanY).coerceAtMost(size.height + 6.dp.toPx()),
                cornerPx,
                cornerPx,
                rearFlashHaloPaint,
              )
            }
          }

          // 2. Card Content
          drawContent()

          // 3. Base Architectural Border with Subtle Zone Accent Rim
          val baseBorderAlpha = (1f - 0.38f * flashEnvelope).coerceIn(0.42f, 1f)
          drawRoundRect(
            brush = baseBorderBrush,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            alpha = baseBorderAlpha,
            style = Stroke(width = 1.25.dp.toPx()),
          )

          // 4. One-Time Traveling Border Flash & Reflection Wave
          if (flashEnvelope > 0.01f) {
            val diagW = size.width.coerceAtLeast(1f)
            val diagH = size.height.coerceAtLeast(1f)
            val bandSpanX = diagW * 0.42f
            val bandSpanY = diagH * 0.42f
            val centerX = -bandSpanX + (diagW + bandSpanX * 2f) * flashProgress
            val centerY = -bandSpanY + (diagH + bandSpanY * 2f) * flashProgress

            drawRoundRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  animatedZonePrimary.copy(alpha = 0.08f * flashEnvelope),
                  PureWhite.copy(alpha = 0.14f * flashEnvelope),
                  animatedZoneSecondary.copy(alpha = 0.08f * flashEnvelope),
                  Color.Transparent,
                ),
                start = Offset(centerX - bandSpanX, centerY - bandSpanY),
                end = Offset(centerX + bandSpanX, centerY + bandSpanY),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )

            drawRoundRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  animatedZoneSecondary.copy(alpha = 0.55f * flashEnvelope),
                  animatedZonePrimary.copy(alpha = 0.90f * flashEnvelope),
                  PureWhite.copy(alpha = 1.0f * flashEnvelope),
                  animatedZonePrimary.copy(alpha = 0.90f * flashEnvelope),
                  Zinc400.copy(alpha = 0.55f * flashEnvelope),
                  Color.Transparent,
                ),
                start = Offset(centerX - bandSpanX * 0.85f, centerY - bandSpanY * 0.85f),
                end = Offset(centerX + bandSpanX * 0.85f, centerY + bandSpanY * 0.85f),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = (2.0f + 0.8f * flashEnvelope).dp.toPx()),
            )
          }
        }
      }
      .clip(cardShape)
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            CardBgTop,
            CardBgBottom,
          ),
        ),
      )
      .padding(metrics.bmiCardPadding)
      .testTag("bmi_calculation_card"),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // 1. Header Row: Category-Colored Luminous Dot + BIOMETRIC MASS INDEX + Color-Coded Classification Pill
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = (11f * metrics.scale).coerceIn(8f, 15f).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PersonalLuminousColorDot(
            color = animatedZonePrimary,
            size = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.personal_bmi_header),
            color = Zinc100,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(11.6f, 10f, 13.5f),
            letterSpacing = 1.1.sp,
          )
        }

        // Dynamic Welcome-Screen-Colored Category Badge Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  animatedZonePrimary.copy(alpha = 0.18f),
                  animatedZoneSecondary.copy(alpha = 0.12f),
                ),
              ),
            )
            .border(
              width = 1.dp,
              color = animatedZonePrimary.copy(alpha = 0.55f + 0.28f * livePulse),
              shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("bmi_category_badge"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(animatedZonePrimary),
            )
            Text(
              text = telemetry.categoryLabel,
              color = animatedZonePrimary,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(9.6f, 8.4f, 11.2f),
              letterSpacing = 0.8.sp,
            )
          }
        }
      }

      HorizontalDivider(
        thickness = 1.dp,
        color = Zinc800.copy(alpha = 0.85f),
      )
    }

    Spacer(modifier = Modifier.height(metrics.bmiSectionSpacing))

    // 2. Primary Live BMI Readout + Somatic Index Telemetry + Welcome-Screen-Colored Spectrum Gauge
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("calculated_bmi_section"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.personal_bmi_label),
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(11f, 9.5f, 12.8f),
          letterSpacing = 1.2.sp,
        )
        Text(
          text = telemetry.categoryCode,
          color = animatedZonePrimary,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(10f, 8.5f, 11.5f),
          letterSpacing = 0.9.sp,
        )
      }

      Spacer(modifier = Modifier.height((7f * metrics.scale).coerceIn(5f, 11f).dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        // Enormous Animated BMI Score
        Row(
          verticalAlignment = Alignment.Bottom,
        ) {
          Text(
            text = formatOneDecimal(displayBmi),
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.bmiPrimaryFontSize,
            lineHeight = metrics.bmiPrimaryFontSize,
            letterSpacing = (-1.0).sp,
            modifier = Modifier.testTag("bmi_value_text"),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.personal_bmi_unit),
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.bmiUnitFontSize,
            modifier = Modifier.padding(bottom = 5.dp),
          )
        }

        // Right-side Live BMI Prime & Ponderal Readout
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.padding(bottom = 3.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "PRIME ",
              color = Zinc500,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(10f, 8.5f, 11.5f),
            )
            Text(
              text = String.format(Locale.US, "%.2f", displayPrime),
              color = animatedZonePrimary,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(13.5f, 11.5f, 15.5f),
              modifier = Modifier.testTag("bmi_prime_text"),
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${formatOneDecimal(displayPonderal)} kg/m³ PI",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(10.2f, 8.8f, 11.8f),
          )
        }
      }

      Spacer(modifier = Modifier.height((12f * metrics.scale).coerceIn(9f, 16f).dp))

      // Lively Multi-Zone Welcome-Screen-Colored BMI Spectrum Bar with Gliding Reticle Marker
      AnimatedBmiSpectrumGauge(
        progressProvider = {
          animatedGaugeProgress * entryPhaseProvider().coerceIn(0f, 1f)
        },
        livePulseProvider = { livePulse },
        shimmerBoostProvider = borderFlashProvider,
        spectrumColors = WelcomeBmiSpectrumGradient,
        activeMarkerColor = animatedZonePrimary,
        barHeight = metrics.bmiBarHeight,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("bmi_spectrum_gauge"),
      )

      Spacer(modifier = Modifier.height((8f * metrics.scale).coerceIn(6f, 11f).dp))

      // Color-Coded Welcome Screen Spectrum Zone Legend Pills (Underweight, Optimal, Overweight, Obese)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BmiZoneScaleItem(
          label = "<18.5 UNDER",
          dotColor = BmiUnderweightPrimary,
          isActive = telemetry.zone == BmiZone.UNDERWEIGHT,
          metrics = metrics,
        )
        BmiZoneScaleItem(
          label = "18.5–24.9 OPTIMAL",
          dotColor = BmiOptimalPrimary,
          isActive = telemetry.zone == BmiZone.OPTIMAL,
          metrics = metrics,
        )
        BmiZoneScaleItem(
          label = "25–29.9 OVER",
          dotColor = BmiOverweightSecondary,
          isActive = telemetry.zone == BmiZone.OVERWEIGHT,
          metrics = metrics,
        )
        BmiZoneScaleItem(
          label = "30+ OBESE",
          dotColor = BmiObeseSecondary,
          isActive = telemetry.zone == BmiZone.OBESE,
          metrics = metrics,
        )
      }
    }

    Spacer(modifier = Modifier.height(metrics.bmiSectionSpacing))

    // 3. Secondary Somatic Metrics Grid (Distinct Optimal Mass Range & Scientific Sex-Calibrated Metabolic Rate)
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      HorizontalDivider(
        thickness = 1.dp,
        color = Zinc800.copy(alpha = 0.85f),
      )

      Spacer(modifier = Modifier.height((13f * metrics.scale).coerceIn(10f, 16f).dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        // Left: Optimal Mass Range (Structured Min -> TO -> Max Badges without an ambiguous "-" dash)
        Column(
          modifier = Modifier.weight(1.08f),
          verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4.5f, 8f).dp),
        ) {
          Text(
            text = stringResource(R.string.personal_ideal_weight),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(10.2f, 8.8f, 11.8f),
            letterSpacing = 0.8.sp,
          )

          // Clear Visual Differentiation of Lower and Upper Optimal Mass Bounds
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.testTag("bmi_ideal_weight_range_text"),
          ) {
            // Min Bound Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF14181F))
                .border(0.8.dp, BmiOptimalPrimary.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp),
            ) {
              Text(
                text = formatOneDecimal(telemetry.idealMinKg),
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(14.5f, 12.2f, 17f),
              )
            }

            // Distinct "TO" Connector Pill instead of ambiguous "-" dash
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(BmiOptimalPrimary.copy(alpha = 0.14f))
                .padding(horizontal = 4.5.dp, vertical = 2.dp),
            ) {
              Text(
                text = "TO",
                color = BmiOptimalPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(8.6f, 7.5f, 10f),
                letterSpacing = 0.5.sp,
              )
            }

            // Max Bound Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF14181F))
                .border(0.8.dp, BmiOptimalPrimary.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp),
            ) {
              Text(
                text = formatOneDecimal(telemetry.idealMaxKg),
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(14.5f, 12.2f, 17f),
              )
            }

            Text(
              text = stringResource(R.string.personal_weight_unit),
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(11.5f, 10f, 13.5f),
            )
          }

          Text(
            text = "BMI 18.5 TO 24.9 TARGET",
            color = Zinc500,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(8.8f, 7.6f, 10.2f),
            letterSpacing = 0.4.sp,
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Scientifically Calibrated Resting Metabolic Rate (Men / Women Mifflin-St Jeor)
        Column(
          modifier = Modifier.weight(0.96f),
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy((5f * metrics.scale).coerceIn(4f, 7f).dp),
        ) {
          Text(
            text = stringResource(R.string.personal_resting_bmr),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(10.2f, 8.8f, 11.8f),
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.End,
          )
          Row(
            verticalAlignment = Alignment.Bottom,
          ) {
            Text(
              text = NumberFormat.getIntegerInstance(Locale.US).format(displayBmr),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.secondaryStatFontSize,
              modifier = Modifier.testTag("bmi_resting_bmr_text"),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "kcal/d",
              color = CaloriesStart,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(11.8f, 10f, 13.8f),
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
          Text(
            text = telemetry.sexCalibrationLabel,
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(8.6f, 7.5f, 10f),
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height((12f * metrics.scale).coerceIn(9f, 15f).dp))

      // 4. Scientific Accuracy (70–85%) & Fitness Coach / Medical Consultation Banner
      val advisoryShape = RoundedCornerShape(8.dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(advisoryShape)
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF13161E),
                Color(0xFF0E1016),
              ),
            ),
          )
          .border(
            width = 0.8.dp,
            color = Zinc800,
            shape = advisoryShape,
          )
          .padding(horizontal = 10.dp, vertical = 7.5.dp)
          .testTag("bmi_metabolic_accuracy_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CaloriesStart.copy(alpha = 0.14f))
            .border(0.7.dp, CaloriesStart.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = "70–85% ACCURACY",
            color = CaloriesStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
            letterSpacing = 0.4.sp,
          )
        }

        Text(
          text = "Consult a fitness coach or medical professional for best clinical precision.",
          color = Zinc300,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(9.8f, 8.6f, 11.2f),
          lineHeight = metrics.spScaled(13.2f, 11.5f, 15f),
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun BmiZoneScaleItem(
  label: String,
  dotColor: Color,
  isActive: Boolean,
  metrics: PersonalResponsiveMetrics,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(if (isActive) 6.dp else 4.5.dp)
        .clip(CircleShape)
        .background(if (isActive) dotColor else dotColor.copy(alpha = 0.45f)),
    )
    Text(
      text = label,
      color = if (isActive) dotColor else Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
      fontSize = metrics.spScaled(8.7f, 7.5f, 10.2f),
    )
  }
}

/**
 * Lively Multi-Zone BMI Spectrum Gauge using the Welcome Screen's Progress Bar Colors:
 * - Subtle full-width recessed 4-zone color track so Underweight (Cyan-Blue), Optimal (Mint-Emerald),
 *   Overweight (Gold-Orange), and Obese (Coral-Crimson) zones are always visually distinguishable.
 * - Vibrant lit multicolor fill up to the live BMI coordinate with category-colored glowing reticle.
 */
@Composable
private fun AnimatedBmiSpectrumGauge(
  progressProvider: () -> Float,
  livePulseProvider: () -> Float,
  shimmerBoostProvider: () -> Float,
  spectrumColors: List<Color>,
  activeMarkerColor: Color,
  barHeight: Dp,
  modifier: Modifier = Modifier,
) {
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
            BmiUnderweightPrimary.copy(alpha = 0.30f),
            BmiOptimalPrimary.copy(alpha = 0.35f),
            BmiOverweightSecondary.copy(alpha = 0.30f),
            BmiObeseSecondary.copy(alpha = 0.32f),
          ),
        )
        val recessedSpectrumBrush = Brush.horizontalGradient(
          colors = spectrumColors.map { it.copy(alpha = 0.22f) },
        )
        val fullSpectrumBrush = Brush.horizontalGradient(
          colors = spectrumColors,
          startX = 0f,
          endX = size.width.coerceAtLeast(1f),
        )
        val verticalLightingBrush = Brush.verticalGradient(
          0.0f to PureWhite.copy(alpha = 0.32f),
          0.42f to Color.Transparent,
          1.0f to ObsidianBlack.copy(alpha = 0.22f),
          startY = 0f,
          endY = trackHeight,
        )
        val markerHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = activeMarkerColor.toArgb()
            maskFilter = BlurMaskFilter(safeBlurRadius(trackHeight * 1.25f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val fillPath = Path()

        onDrawBehind {
          if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
          val progress = progressProvider().coerceIn(0f, 1f)
          val pulse = livePulseProvider().coerceIn(0f, 1f)
          val boost = shimmerBoostProvider().coerceIn(0f, 1f)

          // 1. Track Chassis with Subtle Welcome-Screen-Colored Zone Preview
          drawRoundRect(
            color = MonochromeTrack,
            topLeft = Offset.Zero,
            size = Size(size.width, trackHeight),
            cornerRadius = cornerRadius,
          )
          drawRoundRect(
            brush = recessedSpectrumBrush,
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

          // 2. Vibrant Welcome-Screen-Colored Spectrum Fill up to current BMI position
          if (progress > 0.002f) {
            val minFill = (trackHeight * 0.5f).coerceAtMost(size.width)
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
              brush = fullSpectrumBrush,
            )

            clipPath(fillPath) {
              drawRect(
                brush = verticalLightingBrush,
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )

              val sheenRatio = if (boost > 0.01f) boost else (0.35f + 0.30f * pulse)
              val sheenCenter = fillWidth * sheenRatio
              val sheenSpan = (fillWidth * 0.28f).coerceAtLeast(trackHeight * 2.2f)
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color.Transparent,
                    PureWhite.copy(alpha = 0.36f),
                    Color(0xFFF7EFE6).copy(alpha = 0.20f),
                    Color.Transparent,
                  ),
                  start = Offset(sheenCenter - sheenSpan, 0f),
                  end = Offset(sheenCenter + sheenSpan, trackHeight),
                ),
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )
            }
          }

          // 3. Zone Divider Tick Notches (18.5 at 25%, 25.0 at 58%, 30.0 at 80%)
          val zoneFractions = listOf(0.25f, 0.58f, 0.80f)
          zoneFractions.forEach { frac ->
            val tickX = size.width * frac
            drawLine(
              color = ObsidianBlack.copy(alpha = 0.78f),
              start = Offset(tickX, 0f),
              end = Offset(tickX, trackHeight),
              strokeWidth = 1.5.dp.toPx(),
            )
          }

          // 4. Lively Glowing Category-Colored BMI Marker Reticle at Fill Crest
          if (progress > 0.002f) {
            val minFill = (trackHeight * 0.5f).coerceAtMost(size.width)
            val fillWidth = (size.width * progress).coerceIn(minFill, size.width)
            val markerX = fillWidth.coerceIn(radius, size.width - radius)
            val markerCenter = Offset(markerX, trackHeight / 2f)
            markerHaloPaint.asFrameworkPaint().alpha = (150f + 85f * pulse).roundToInt().coerceIn(110, 240)
            drawIntoCanvas { canvas ->
              canvas.drawCircle(markerCenter, trackHeight * 0.82f, markerHaloPaint)
            }
            drawCircle(
              color = PureWhite,
              radius = (trackHeight * 0.46f).coerceAtLeast(2f),
              center = markerCenter,
            )
            drawCircle(
              color = activeMarkerColor,
              radius = (trackHeight * 0.28f).coerceAtLeast(1.4f),
              center = markerCenter,
            )
            drawCircle(
              color = ObsidianBlack,
              radius = (trackHeight * 0.12f).coerceAtLeast(0.8f),
              center = markerCenter,
            )
          }
        }
      },
  )
}

@Composable
private fun PersonalContinueSection(
  onContinueClick: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: PersonalResponsiveMetrics,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val buttonCorner = (10f * metrics.scale).coerceIn(8f, 13f).dp
  val buttonShape = RoundedCornerShape(buttonCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "personal_continue_glow_transition")
  val shimmerPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "personal_continue_shimmer_phase",
  )
  val shimmerProvider = remember { { shimmerPhase } }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(metrics.ctaHeight)
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
              maskFilter = BlurMaskFilter(safeBlurRadius(18.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
            }
          }
          val innerRimGlowPaint = Paint().apply {
            isAntiAlias = true
            asFrameworkPaint().apply {
              color = android.graphics.Color.argb(140, 242, 245, 250)
              maskFilter = BlurMaskFilter(safeBlurRadius(7.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
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
            outerAuraPaint.asFrameworkPaint().alpha = (60 + (55 * breath).roundToInt() + boostAlpha).coerceIn(45, 195)

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
          onClick = onContinueClick,
        )
        .testTag("personal_continue_button"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(R.string.calib_cta_continue),
          color = ObsidianBlack,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(14.5f, 13f, 17f),
          letterSpacing = (-0.2).sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Canvas(
          modifier = Modifier.size((16.5f * metrics.scale).coerceIn(14.5f, 19.5f).dp),
        ) {
          val sx = size.width / 24f
          val sy = size.height / 24f
          val arrow = Path().apply {
            moveTo(5f * sx, 12f * sy)
            lineTo(19f * sx, 12f * sy)
            moveTo(13f * sx, 6f * sy)
            lineTo(19f * sx, 12f * sy)
            lineTo(13f * sx, 18f * sy)
          }
          drawPath(
            path = arrow,
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

    Spacer(modifier = Modifier.height((8.5f * metrics.scale).coerceIn(6f, 12f).dp))

    Text(
      text = stringResource(R.string.calib_footer_encrypted),
      color = Zinc500,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = metrics.spScaled(10.8f, 9.5f, 12.5f),
      letterSpacing = 0.2.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("personal_footer_encrypted_text"),
    )
  }
}
