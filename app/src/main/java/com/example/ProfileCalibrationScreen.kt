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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class ActiveEditSection {
  NONE,
  FULL_NAME,
  DOB,
}

// Strictly Monochrome Black, Zinc, Platinum & Pure White Palette
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
private val ChronoCardBgTop = Color(0xFF141417)
private val ChronoCardBgBottom = Color(0xFF0A0A0C)
private val MonochromeTrack = Color(0xFF1A1A1E)
private val MonochromeTrackBorder = Color(0xFF2E2E34)

// Multi-Tone White / Liquid Chrome / Pearl Variant Gradient
private val WhiteVariantMulticolorGradient = listOf(
  Color(0xFF7A7F87), // Gunmetal Platinum
  Color(0xFFAEC0CE), // Cool Liquid Silver
  Color(0xFFE6E9EF), // Satin Ice White
  Color(0xFFF7EFE6), // Warm Pearl Champagne Highlight
  Color(0xFFFFFFFF), // Pure Brilliant White Crest
)

private val CalibrationEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val MonthShortNames = listOf(
  "Jan", "Feb", "Mar", "Apr", "May", "Jun",
  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private val MonthFullNames = listOf(
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
)

private fun safeBlurRadius(radius: Float): Float = radius.coerceAtLeast(0.5f)

@Immutable
private data class ChronometricResult(
  val years: Int,
  val months: Int,
  val days: Int,
  val totalDays: Int,
  val nextAnniversaryDate: String,
  val nextAnniversaryDaysRemaining: Int,
  val calculatedAgeProgress: Float,
)

private fun calculateChronometry(
  selectedDay: Int,
  selectedMonthZeroBased: Int,
  selectedYear: Int,
): ChronometricResult {
  val now = Calendar.getInstance()
  val curYear = now.get(Calendar.YEAR)
  val curMonth = now.get(Calendar.MONTH)
  val curDay = now.get(Calendar.DAY_OF_MONTH)

  val safeMonth = selectedMonthZeroBased.coerceIn(0, 11)
  val birthCal = Calendar.getInstance().apply {
    set(Calendar.YEAR, selectedYear)
    set(Calendar.MONTH, safeMonth)
    set(Calendar.DAY_OF_MONTH, 1)
  }
  val maxDayInBirthMonth = birthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val safeBirthDay = selectedDay.coerceIn(1, maxDayInBirthMonth)
  birthCal.set(Calendar.DAY_OF_MONTH, safeBirthDay)
  birthCal.set(Calendar.HOUR_OF_DAY, 0)
  birthCal.set(Calendar.MINUTE, 0)
  birthCal.set(Calendar.SECOND, 0)
  birthCal.set(Calendar.MILLISECOND, 0)

  if (birthCal.timeInMillis > now.timeInMillis) {
    return ChronometricResult(
      years = 0,
      months = 0,
      days = 0,
      totalDays = 0,
      nextAnniversaryDate = "${MonthShortNames[safeMonth]} ${safeBirthDay.toString().padStart(2, '0')}",
      nextAnniversaryDaysRemaining = 365,
      calculatedAgeProgress = 0.05f,
    )
  }

  val diffMs = now.timeInMillis - birthCal.timeInMillis
  val totalDays = floor(diffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt().coerceAtLeast(0)

  var y = curYear - selectedYear
  var m = curMonth - safeMonth
  var d = curDay - safeBirthDay

  if (d < 0) {
    m -= 1
    val prevMonthCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, curYear)
      set(Calendar.MONTH, curMonth)
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.DAY_OF_MONTH, -1)
    }
    d += prevMonthCal.get(Calendar.DAY_OF_MONTH)
  }
  if (m < 0) {
    y -= 1
    m += 12
  }

  val nextBday = Calendar.getInstance().apply {
    set(Calendar.YEAR, curYear)
    set(Calendar.MONTH, safeMonth)
    val maxDayThisYear = getActualMaximum(Calendar.DAY_OF_MONTH)
    set(Calendar.DAY_OF_MONTH, safeBirthDay.coerceIn(1, maxDayThisYear))
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  if (nextBday.timeInMillis < now.timeInMillis) {
    nextBday.add(Calendar.YEAR, 1)
  }
  val bdayDiffMs = nextBday.timeInMillis - now.timeInMillis
  val bdayDaysRem = ceil(bdayDiffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt().coerceIn(1, 366)

  val nextDateStr = "${MonthShortNames[safeMonth]} ${safeBirthDay.toString().padStart(2, '0')}"

  val exactAgeYears = y.toFloat() + (m.toFloat() / 12f) + (d.toFloat() / 365.25f)
  val ageProgress = (exactAgeYears / 88f).coerceIn(0.06f, 0.96f)

  return ChronometricResult(
    years = y.coerceAtLeast(0),
    months = m.coerceAtLeast(0),
    days = d.coerceAtLeast(0),
    totalDays = totalDays,
    nextAnniversaryDate = nextDateStr,
    nextAnniversaryDaysRemaining = bdayDaysRem,
    calculatedAgeProgress = ageProgress,
  )
}

@Immutable
private data class CalibrationResponsiveMetrics(
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
  val inputHeight: Dp,
  val selectHeight: Dp,
  val toggleHeight: Dp,
  val chronoCardMinHeight: Dp,
  val chronoCardPadding: Dp,
  val chronoSectionSpacing: Dp,
  val ageYearsFontSize: TextUnit,
  val ageSubUnitFontSize: TextUnit,
  val ageUnitLabelFontSize: TextUnit,
  val ageBarHeight: Dp,
  val secondaryStatFontSize: TextUnit,
  val ctaHeight: Dp,
) {
  fun spScaled(base: Float, minSp: Float, maxSp: Float): TextUnit {
    return ((base * scale).coerceIn(minSp, maxSp) * fontCompensation).sp
  }
}

private fun computeCalibrationMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): CalibrationResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val rawScale = min(w / 385f, h / 825f).coerceIn(0.76f, 1.30f)

  // Normalize Android system fontScale so that even huge system font settings (e.g. 200%)
  // scale text smoothly without ever crashing, clipping, or forcing excess scroll.
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

  // Resolution-adaptive logical gaps between every section on the Profile page:
  // Automatically scales with screen height (h) so sections are clearly differentiated on every device.
  val titleToFirstFieldGap = (h * 0.023f).coerceIn(16f, 25f).dp
  val formFieldGap = (h * 0.021f).coerceIn(15f, 23f).dp
  val sectionToCardGap = (h * 0.025f).coerceIn(18f, 27f).dp
  val cardToCtaGap = (h * 0.024f).coerceIn(17f, 26f).dp

  return CalibrationResponsiveMetrics(
    scale = rawScale,
    fontCompensation = fontCompensation,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (20f * rawScale).coerceIn(16f, 28f).dp,
    topContentPadding = (12f * rawScale).coerceIn(8f, 18f).dp,
    bottomContentPadding = (8f * rawScale).coerceIn(6f, 12f).dp,
    headerHeight = (56f * rawScale).coerceIn(50f, 66f).dp,
    titleFontSize = scaledSp(23.5f, 19f, 28f),
    subtitleFontSize = scaledSp(12f, 10.2f, 14f),
    subtitleLineHeight = scaledSp(17.2f, 14.5f, 20f),
    labelToBoxGap = (7.5f * rawScale).coerceIn(6f, 10f).dp,
    titleToFirstFieldGap = titleToFirstFieldGap,
    formFieldGap = formFieldGap,
    sectionToCardGap = sectionToCardGap,
    cardToCtaGap = cardToCtaGap,
    inputHeight = (46f * rawScale).coerceIn(43f, 52f).dp,
    selectHeight = (45f * rawScale).coerceIn(42f, 51f).dp,
    toggleHeight = (39f * rawScale).coerceIn(36f, 45f).dp,
    chronoCardMinHeight = (h * 0.285f).coerceIn(216f, 286f).dp,
    chronoCardPadding = (20f * rawScale).coerceIn(17f, 26f).dp,
    chronoSectionSpacing = (16f * rawScale).coerceIn(13f, 22f).dp,
    ageYearsFontSize = scaledSp(42f, 32f, 50f),
    ageSubUnitFontSize = scaledSp(26f, 20f, 32f),
    ageUnitLabelFontSize = scaledSp(13.5f, 11f, 16f),
    ageBarHeight = (11.5f * rawScale).coerceIn(9.5f, 14f).dp,
    secondaryStatFontSize = scaledSp(18f, 15f, 22f),
    ctaHeight = (49f * rawScale).coerceIn(45f, 56f).dp,
  )
}

/**
 * Shared modifier for text input boxes (`FullNameField`, `DAY`, and `YEAR`):
 * - While unfocused (`isFocused == false`): rests quietly with a crisp 1.dp `Zinc800` border on `ObsidianBlack`.
 * - While actively writing (`isFocused == true`): elevates with a floating 3D shadow and a continuous,
 *   dull/muted platinum-white flash wave orbiting around the border so the box feels like it is floating only while writing.
 */
@Composable
private fun Modifier.writingFloatingBorder(
  isFocused: Boolean,
  cornerRadiusDp: Dp = 8.dp,
): Modifier {
  val focusWeight by animateFloatAsState(
    targetValue = if (isFocused) 1f else 0f,
    animationSpec = tween(durationMillis = 240, easing = CalibrationEaseOut),
    label = "writing_focus_weight",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "writing_dull_flash_transition")
  val sweepPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "writing_border_sweep_phase",
  )
  val dullPulse by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "writing_border_dull_pulse",
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

      // 1. Floating Shadow & Dull Rear Halo (active ONLY while writing)
      if (fw > 0.01f && size.width > 1f && size.height > 1f) {
        floatingDropShadowPaint.asFrameworkPaint().alpha = (195f * fw).roundToInt().coerceIn(0, 220)
        dullAmbientRimPaint.asFrameworkPaint().alpha = ((32f + 24f * pulse) * fw).roundToInt().coerceIn(0, 75)

        drawIntoCanvas { canvas ->
          // Deep floating drop shadow beneath the input box
          canvas.drawRoundRect(
            2.dp.toPx(),
            6.dp.toPx(),
            size.width - 2.dp.toPx(),
            size.height + 7.dp.toPx(),
            cornerPx,
            cornerPx,
            floatingDropShadowPaint,
          )
          // Dull muted-silver levitation halo around the border
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

      // 2. Field Surface Background (architectural carbon gradient + subtle floating obsidian lift while writing)
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF131317),
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
              Color(0xFF18181E).copy(alpha = 0.85f * fw),
              Color(0xFF0C0C10).copy(alpha = 0.85f * fw),
            ),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
        )
      }

      // 3. Field Inner Content (Centered Cursor, Text, Placeholder, Icon)
      drawContent()

      // 4. Resting Base Border (top-lit specular architectural rim)
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.20f),
            Zinc700.copy(alpha = 0.72f),
            Zinc850,
          ),
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.dp.toPx()),
      )

      // 5. Constant Dull Flash-Up Around the Borders (ONLY while writing)
      if (fw > 0.01f && size.width > 1f && size.height > 1f) {
        val w = size.width
        val h = size.height
        val spanX = w * 0.48f
        val spanY = h * 0.90f
        // Ping-pong / traveling wave around the border
        val waveT = (sin(sweep * Math.PI.toFloat() * 2f) * 0.5f + 0.5f).coerceIn(0f, 1f)
        val cx = -spanX * 0.3f + (w + spanX * 0.6f) * waveT
        val cy = h * 0.5f

        // Dull ambient border brightening
        drawRoundRect(
          color = Zinc600.copy(alpha = (0.45f + 0.25f * pulse) * fw),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = 1.15.dp.toPx()),
        )

        // Traveling dull-white / satin-zinc flash wave along the border
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
          style = Stroke(width = 1.35.dp.toPx()),
        )
      }
    }
  }
}

@Composable
fun ProfileCalibrationScreen(
  onNavigateBack: () -> Unit,
  onNavigateToPersonalDetails: (subjectName: String, selectedSex: String, calculatedAgeYears: Int) -> Unit = { _, _, _ -> },
  initialName: String = "Elena Rostova",
  onNamePersist: (String) -> Unit = {},
  initialSex: String = "FEMALE",
  onSexPersist: (String) -> Unit = {},
  initialDay: Int = 24,
  onDayPersist: (Int) -> Unit = {},
  initialMonth: Int = 9,
  onMonthPersist: (Int) -> Unit = {},
  initialYear: Int = 1994,
  onYearPersist: (Int) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  var subjectName by rememberSaveable { mutableStateOf(initialName) }
  var selectedSex by rememberSaveable { mutableStateOf(initialSex) }
  var selectedDay by rememberSaveable { mutableIntStateOf(initialDay) }
  var selectedMonth by rememberSaveable { mutableIntStateOf(initialMonth) } // 9 = Oct (10)
  var selectedYear by rememberSaveable { mutableIntStateOf(initialYear) }
  var isTransitioning by remember { mutableStateOf(false) }

  LaunchedEffect(subjectName) { onNamePersist(subjectName) }
  LaunchedEffect(selectedSex) { onSexPersist(selectedSex) }
  LaunchedEffect(selectedDay) { onDayPersist(selectedDay) }
  LaunchedEffect(selectedMonth) { onMonthPersist(selectedMonth) }
  LaunchedEffect(selectedYear) { onYearPersist(selectedYear) }

  val chronometry = remember(selectedDay, selectedMonth, selectedYear) {
    calculateChronometry(
      selectedDay = selectedDay,
      selectedMonthZeroBased = selectedMonth,
      selectedYear = selectedYear,
    )
  }

  val coroutineScope = rememberCoroutineScope()
  val haptics = rememberBioSyncHaptics()
  val headerStepPhase = remember { Animatable(0f) }
  val chronoPhase = remember { Animatable(0f) }
  val borderFlashPhase = remember { Animatable(0f) }

  // Top STEP 01 OF 03 progress bar: animates ONLY when switching to the Profile Section page
  LaunchedEffect(Unit) {
    headerStepPhase.snapTo(0f)
    headerStepPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 720, easing = CalibrationEaseOut),
    )
  }

  // Chronometric Status section: animates on page entry AND whenever Date of Birth is edited
  LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
    chronoPhase.snapTo(0f)
    chronoPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 720, easing = CalibrationEaseOut),
    )
  }

  val onContinueClick: () -> Unit = {
    if (!isTransitioning) {
      haptics.confirm()
      isTransitioning = true
      coroutineScope.launch {
        borderFlashPhase.snapTo(0f)
        borderFlashPhase.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 480, easing = CalibrationEaseOut),
        )
        borderFlashPhase.snapTo(0f)
        onNavigateToPersonalDetails(subjectName, selectedSex, chronometry.years)
        isTransitioning = false
      }
    }
  }

  val headerStepPhaseProvider = remember(headerStepPhase) { { headerStepPhase.value } }
  val chronoPhaseProvider = remember(chronoPhase) { { chronoPhase.value } }
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
        computeCalibrationMetrics(
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          fontScale = fontScale,
        )
      }
      val scrollState = rememberScrollState()
      var activeEditSection by remember { mutableStateOf(ActiveEditSection.NONE) }
      var fullNameTopY by remember { mutableIntStateOf(0) }
      var bioSexTopY by remember { mutableIntStateOf(0) }

      // Always redirect to the very top of the page when entering the Profile Section
      LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
      }

      // Automatically scroll up while writing/editing so the active section and Chronometric Status
      // are framed cleanly above the keyboard (matching image_bff83314.png)
      LaunchedEffect(activeEditSection) {
        when (activeEditSection) {
          ActiveEditSection.DOB -> {
            snapshotFlow { scrollState.maxValue to bioSexTopY }
              .collectLatest { (maxVal, sexY) ->
                if (maxVal > 0) {
                  val targetScroll = if (sexY > 0) max(sexY - 12, maxVal) else maxVal
                  scrollState.animateScrollTo(
                    value = targetScroll.coerceIn(0, maxVal),
                    animationSpec = tween(durationMillis = 280, easing = CalibrationEaseOut),
                  )
                }
              }
          }
          ActiveEditSection.FULL_NAME -> {
            snapshotFlow { scrollState.maxValue to fullNameTopY }
              .collectLatest { (maxVal, nameY) ->
                if (maxVal > 0 && nameY > 0) {
                  val targetScroll = (nameY - 16).coerceIn(0, maxVal)
                  scrollState.animateScrollTo(
                    value = targetScroll,
                    animationSpec = tween(durationMillis = 280, easing = CalibrationEaseOut),
                  )
                }
              }
          }
          ActiveEditSection.NONE -> Unit
        }
      }

      // Single unified page: Header (BioSync + STEP 01 OF 03) and Body live on one seamless canvas
      // with no dividing line and scroll together smoothly without freezing at the top.
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
          .testTag("profile_calibration_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Header Bar (BioSync + STEP 01 OF 03 + Shiny Glowy White Step Bar) — part of the unified page
        CalibrationHeaderBar(
          onBackClick = {
            haptics.selection()
            onNavigateBack()
          },
          phaseProvider = headerStepPhaseProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // 1. Top Eyebrow, Section Title & Description under Profile Section
        Column(
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.calib_eyebrow),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
            letterSpacing = 1.6.sp,
          )
          Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
          Text(
            text = stringResource(R.string.calib_title),
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.titleFontSize,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.testTag("calib_title_text"),
          )
          Spacer(modifier = Modifier.height((5f * metrics.scale).coerceIn(3.5f, 7.5f).dp))
          Text(
            text = stringResource(R.string.calib_subtitle),
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.subtitleFontSize,
            lineHeight = metrics.subtitleLineHeight,
            modifier = Modifier.testTag("calib_subtitle_text"),
          )
        }

        Spacer(modifier = Modifier.height(metrics.titleToFirstFieldGap))

        // 2. Full Name Section (with centered cursor + floating dull border flash while writing)
        FullNameField(
          value = subjectName,
          onValueChange = {
            haptics.tick()
            subjectName = it
          },
          onFocusChange = { focused ->
            if (focused) {
              haptics.tick()
              activeEditSection = ActiveEditSection.FULL_NAME
            } else if (activeEditSection == ActiveEditSection.FULL_NAME) {
              activeEditSection = ActiveEditSection.NONE
            }
          },
          metrics = metrics,
          modifier = Modifier.onGloballyPositioned { coords ->
            fullNameTopY = coords.positionInParent().y.roundToInt()
          },
        )

        Spacer(modifier = Modifier.height(metrics.formFieldGap))

        // 3. Biological Sex Section
        BiologicalSexSelector(
          selectedSex = selectedSex,
          onSelectSex = { sex ->
            if (selectedSex != sex) {
              haptics.selection()
              selectedSex = sex
            } else {
              haptics.tick()
            }
          },
          metrics = metrics,
          modifier = Modifier.onGloballyPositioned { coords ->
            bioSexTopY = coords.positionInParent().y.roundToInt()
          },
        )

        Spacer(modifier = Modifier.height(metrics.formFieldGap))

        // 4. Date of Birth Section (Direct Day/Year typing with floating dull border flash + Floating 3D Month Picker)
        DateOfBirthSelectorRow(
          selectedDay = selectedDay,
          selectedMonth = selectedMonth,
          selectedYear = selectedYear,
          onDayChange = { selectedDay = it },
          onMonthChange = {
            haptics.selection()
            selectedMonth = it
          },
          onYearChange = { selectedYear = it },
          onDobFocusChange = { focused ->
            if (focused) {
              haptics.tick()
              activeEditSection = ActiveEditSection.DOB
            } else if (activeEditSection == ActiveEditSection.DOB) {
              activeEditSection = ActiveEditSection.NONE
            }
          },
          haptics = haptics,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionToCardGap))

        // 5. Architectural Chronometric Display Card
        ChronometricDisplayCard(
          chronometry = chronometry,
          phaseProvider = chronoPhaseProvider,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.cardToCtaGap))

        // 6. Primary Action Button & Encrypted Footer (Content ends right here with no trailing scroll space)
        CalibrationContinueSection(
          onContinueClick = onContinueClick,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )
      }
    }
  }
}

@Composable
private fun CalibrationHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
  metrics: CalibrationResponsiveMetrics,
) {
  val backDesc = stringResource(R.string.cd_back_button)
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
        .testTag("calib_back_button"),
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

    // Center Title + Step Subtitle + Shiny Glowy White Step Progress Bar right below STEP 01 OF 03
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
        text = stringResource(R.string.calib_header_step),
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10.2f, 9f, 12f),
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp))

      // Slight White Color Shiny Glowy Bar right below STEP 01 OF 03
      StepOneShinyGlowBar(
        phaseProvider = phaseProvider,
        modifier = Modifier
          .width((108f * metrics.scale).coerceIn(92f, 136f).dp)
          .height((4f * metrics.scale).coerceIn(3.5f, 5f).dp)
          .testTag("step_01_glow_bar"),
      )
    }

    // Right Luminous Status Dot (same PureWhite color as BioSync header title)
    Box(
      modifier = Modifier.size(48.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      LuminousWhiteDot(
        size = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
      )
    }
  }
}

@Composable
private fun LuminousWhiteDot(
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Spacer(
    modifier = modifier
      .size(size)
      .drawWithCache {
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(140, 255, 255, 255)
            maskFilter = BlurMaskFilter(
              safeBlurRadius(this@drawWithCache.size.minDimension * 0.65f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }
        onDrawBehind {
          val r = (this.size.minDimension / 2f).coerceAtLeast(1f)
          drawIntoCanvas { canvas ->
            canvas.drawCircle(center, r * 1.15f, glowPaint)
          }
          drawCircle(
            color = PureWhite,
            radius = r,
            center = center,
          )
        }
      },
  )
}

/**
 * Shiny, glowing pure-white step indicator bar directly below "STEP 01 OF 03".
 */
@Composable
private fun StepOneShinyGlowBar(
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
          val activeFraction = 0.36f * p

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
        }
      },
  )
}

@Composable
private fun FullNameField(
  value: String,
  onValueChange: (String) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  metrics: CalibrationResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  var attemptedInvalidChar by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(isFocused) {
    onFocusChange(isFocused)
    if (!isFocused) {
      attemptedInvalidChar = false
    }
  }

  val trimmedValue = value.trim()
  val hasOnlyLettersAndSpaces = remember(trimmedValue) {
    trimmedValue.isNotEmpty() && trimmedValue.all { it.isLetter() || it == ' ' }
  }
  val letterCount = remember(trimmedValue) {
    trimmedValue.count { it.isLetter() }
  }
  val isNameVerified = hasOnlyLettersAndSpaces && letterCount >= 2 && !attemptedInvalidChar

  val fieldFontSize = metrics.spScaled(14.5f, 12.6f, 16.8f)
  val centeredTextStyle = remember(fieldFontSize) {
    TextStyle(
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = fieldFontSize,
      lineHeight = fieldFontSize * 1.15f,
      platformStyle = PlatformTextStyle(includeFontPadding = false),
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
      ),
    )
  }

  val subjectInitials = remember(value) {
    val parts = value
      .filter { it.isLetter() || it == ' ' }
      .trim()
      .split("\\s+".toRegex())
      .filter { it.isNotEmpty() }
    when {
      parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
      parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
      parts.size == 1 -> "${parts[0].first().uppercaseChar()}•"
      else -> "ID"
    }
  }

  val iconBadgeShape = RoundedCornerShape(10.dp)
  val monogramShape = RoundedCornerShape(8.dp)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.labelToBoxGap),
  ) {
    val nameTag = stringResource(R.string.calib_field_name_tag)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.calib_field_name_label),
        color = Zinc200,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.4f, 11.2f, 14.8f),
      )
      if (nameTag.isNotEmpty()) {
        Text(
          text = nameTag,
          color = Zinc500,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
          letterSpacing = 0.8.sp,
        )
      }
    }

    // Architectural Single-Block Identity Field (strictly letters & spaces; blocks numbers/symbols)
    BasicTextField(
      value = value,
      onValueChange = { rawInput ->
        val sanitized = rawInput.filter { it.isLetter() || it == ' ' }
        val hadInvalidCharacters = rawInput != sanitized
        attemptedInvalidChar = hadInvalidCharacters
        if (sanitized != value) {
          onValueChange(sanitized)
        }
      },
      singleLine = true,
      interactionSource = interactionSource,
      cursorBrush = SolidColor(PureWhite),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
      ),
      textStyle = centeredTextStyle,
      decorationBox = { innerTextField ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Left Sculpted Biometric Identity Emblem Box
          Box(
            modifier = Modifier
              .size((38f * metrics.scale).coerceIn(34f, 44f).dp)
              .clip(iconBadgeShape)
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(
                    Color(0xFF1E1E25),
                    Color(0xFF0E0E12),
                  ),
                ),
              )
              .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    PureWhite.copy(alpha = if (isFocused) 0.48f else 0.24f),
                    Zinc700.copy(alpha = 0.65f),
                    Zinc850,
                  ),
                ),
                shape = iconBadgeShape,
              ),
            contentAlignment = Alignment.Center,
          ) {
            BadgeVectorIcon(
              color = if (isNameVerified) {
                if (isFocused) PureWhite else Zinc300
              } else {
                Zinc500
              },
              size = (18f * metrics.scale).coerceIn(16f, 21f).dp,
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          // Center Editable Identity Stack
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (value.isEmpty()) {
                Text(
                  text = stringResource(R.string.calib_field_name_placeholder),
                  style = centeredTextStyle.copy(
                    color = if (isFocused) Zinc500.copy(alpha = 0.78f) else Zinc600,
                    fontWeight = FontWeight.Normal,
                  ),
                  maxLines = 1,
                )
              }
              innerTextField()
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = when {
                attemptedInvalidChar -> "SUBJECT RECORD • UNVERIFIED"
                isNameVerified -> "SUBJECT RECORD • VERIFIED"
                else -> "SUBJECT RECORD • UNVERIFIED"
              },
              color = when {
                attemptedInvalidChar -> Zinc300
                isNameVerified -> if (isFocused) Zinc400 else Zinc500
                else -> Zinc500
              },
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
              letterSpacing = 0.6.sp,
              maxLines = 1,
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Right Live Monogram Signature Pill
          Row(
            modifier = Modifier
              .clip(monogramShape)
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(
                    Color(0xFF1A1A21),
                    Color(0xFF101014),
                  ),
                ),
              )
              .border(
                width = 1.dp,
                color = if (isNameVerified && isFocused) {
                  PureWhite.copy(alpha = 0.36f)
                } else {
                  Zinc700.copy(alpha = 0.65f)
                },
                shape = monogramShape,
              )
              .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isNameVerified) PureWhite else Zinc600),
            )
            Text(
              text = if (isNameVerified) subjectInitials else "UNV",
              color = if (isNameVerified) PureWhite else Zinc500,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(10.2f, 9f, 11.8f),
              letterSpacing = 0.6.sp,
            )
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height((metrics.inputHeight * 1.18f).coerceIn(54.dp, 66.dp))
        .writingFloatingBorder(isFocused = isFocused, cornerRadiusDp = 12.dp)
        .testTag("profile_name_input"),
    )
  }
}

@Composable
private fun BadgeVectorIcon(
  color: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val s = this.size.minDimension / 24f
    val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(
      color = color,
      topLeft = Offset(3f * s, 6f * s),
      size = Size(18f * s, 14f * s),
      cornerRadius = CornerRadius(2f * s, 2f * s),
      style = stroke,
    )
    drawRoundRect(
      color = color,
      topLeft = Offset(10f * s, 3f * s),
      size = Size(4f * s, 4f * s),
      cornerRadius = CornerRadius(1f * s, 1f * s),
      style = stroke,
    )
    drawCircle(
      color = color,
      radius = 1.6f * s,
      center = Offset(8.5f * s, 11.5f * s),
    )
    drawLine(
      color = color,
      start = Offset(6.2f * s, 16f * s),
      end = Offset(10.8f * s, 16f * s),
      strokeWidth = 1.5f * s,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = color,
      start = Offset(13.5f * s, 11f * s),
      end = Offset(18f * s, 11f * s),
      strokeWidth = 1.5f * s,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = color,
      start = Offset(13.5f * s, 14.5f * s),
      end = Offset(17f * s, 14.5f * s),
      strokeWidth = 1.5f * s,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
private fun BiologicalSexSelector(
  selectedSex: String,
  onSelectSex: (String) -> Unit,
  metrics: CalibrationResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val containerShape = RoundedCornerShape(12.dp)
  val cardShape = RoundedCornerShape(9.dp)
  val emblemShape = RoundedCornerShape(8.dp)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.labelToBoxGap),
  ) {
    val sexTag = stringResource(R.string.calib_field_sex_tag)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.calib_field_sex_label),
        color = Zinc200,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.4f, 11.2f, 14.8f),
      )
      if (sexTag.isNotEmpty()) {
        Text(
          text = sexTag,
          color = Zinc500,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
          letterSpacing = 0.8.sp,
        )
      }
    }

    // Architectural Top-Lit Dual Somatic Selector Dock
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(containerShape)
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF131317),
              Color(0xFF0A0A0D),
            ),
          ),
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.20f),
              Zinc700.copy(alpha = 0.72f),
              Zinc850,
            ),
          ),
          shape = containerShape,
        )
        .padding(5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val options = listOf(
        Triple("MALE", R.string.calib_sex_male, "XY • SOMATIC"),
        Triple("FEMALE", R.string.calib_sex_female, "XX • SOMATIC"),
      )
      options.forEach { (key, labelRes, bioCode) ->
        val isMale = key == "MALE"
        val isSelected = selectedSex == key
        val accentStart = if (isMale) Color(0xFF7BE3FF) else Color(0xFFFFA6C1)
        val accentEnd = if (isMale) Color(0xFF0EA5E9) else Color(0xFFFB4570)
        val accentSolid = if (isMale) Color(0xFF38BDF8) else Color(0xFFFB7185)

        val bgColor by animateColorAsState(
          targetValue = if (isSelected) PureWhite else Color(0xFF121216),
          animationSpec = tween(durationMillis = 180),
          label = "sex_bg_$key",
        )
        val borderColor by animateColorAsState(
          targetValue = if (isSelected) PureWhite else accentSolid.copy(alpha = 0.28f),
          animationSpec = tween(durationMillis = 180),
          label = "sex_border_$key",
        )
        val primaryTextColor by animateColorAsState(
          targetValue = if (isSelected) ObsidianBlack else Zinc200,
          animationSpec = tween(durationMillis = 180),
          label = "sex_text_$key",
        )
        val subTextColor by animateColorAsState(
          targetValue = if (isSelected) accentEnd else Zinc500,
          animationSpec = tween(durationMillis = 180),
          label = "sex_subtext_$key",
        )
        val emblemIconColor by animateColorAsState(
          targetValue = if (isSelected) PureWhite else accentStart,
          animationSpec = tween(durationMillis = 180),
          label = "sex_emblem_icon_$key",
        )

        Row(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = (metrics.toggleHeight * 1.32f).coerceIn(50.dp, 60.dp))
            .clip(cardShape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = cardShape)
            .clickable(
              role = Role.RadioButton,
              onClick = { onSelectSex(key) },
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("gender_${key.lowercase()}"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            // Geometric Biometric Emblem Box with Male (Sky-Blue) / Female (Rose-Coral) color identity
            Box(
              modifier = Modifier
                .size((30f * metrics.scale).coerceIn(27f, 34f).dp)
                .clip(emblemShape)
                .background(
                  brush = if (isSelected) {
                    Brush.linearGradient(colors = listOf(accentStart, accentEnd))
                  } else {
                    Brush.verticalGradient(
                      colors = listOf(
                        accentSolid.copy(alpha = 0.16f),
                        accentEnd.copy(alpha = 0.08f),
                      ),
                    )
                  },
                )
                .border(
                  width = 0.85.dp,
                  color = if (isSelected) accentEnd else accentSolid.copy(alpha = 0.45f),
                  shape = emblemShape,
                ),
              contentAlignment = Alignment.Center,
            ) {
              BiologicalSexSymbolIcon(
                isMale = isMale,
                color = emblemIconColor,
                size = (15f * metrics.scale).coerceIn(13.5f, 17.5f).dp,
              )
            }

            Column(
              verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
              Text(
                text = stringResource(labelRes),
                color = primaryTextColor,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(12.8f, 11.4f, 14.8f),
                maxLines = 1,
              )
              Text(
                text = bioCode,
                color = subTextColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(8.4f, 7.4f, 9.5f),
                letterSpacing = 0.5.sp,
                maxLines = 1,
              )
            }
          }

          // Active Status Indicator Dot synced with Male / Female color
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(if (isSelected) accentEnd else accentSolid.copy(alpha = 0.40f)),
          )
        }
      }
    }
  }
}

@Composable
private fun BiologicalSexSymbolIcon(
  isMale: Boolean,
  color: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val strokeW = 1.65.dp.toPx()
    val stroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (isMale) {
      val r = w * 0.27f
      val cx = w * 0.40f
      val cy = h * 0.60f
      drawCircle(
        color = color,
        radius = r,
        center = Offset(cx, cy),
        style = stroke,
      )
      // Diagonal arrow to top-right
      val startX = cx + r * 0.707f
      val startY = cy - r * 0.707f
      val tipX = w * 0.82f
      val tipY = h * 0.18f
      drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(tipX, tipY),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(tipX - w * 0.24f, tipY),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(tipX, tipY + h * 0.24f),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
    } else {
      val r = w * 0.26f
      val cx = w * 0.50f
      val cy = h * 0.37f
      drawCircle(
        color = color,
        radius = r,
        center = Offset(cx, cy),
        style = stroke,
      )
      // Vertical stem + crossbar below
      val stemTop = cy + r
      val stemBottom = h * 0.88f
      drawLine(
        color = color,
        start = Offset(cx, stemTop),
        end = Offset(cx, stemBottom),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = color,
        start = Offset(cx - w * 0.19f, h * 0.73f),
        end = Offset(cx + w * 0.19f, h * 0.73f),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
    }
  }
}

/**
 * Date of Birth Section:
 * - Architectural unified block with sculpted cells for DAY, MONTH (3D popup trigger), and YEAR,
 *   plus an integrated bottom chronometric calibration scale strip matching the Height & Weight blocks.
 */
@Composable
private fun DateOfBirthSelectorRow(
  selectedDay: Int,
  selectedMonth: Int,
  selectedYear: Int,
  onDayChange: (Int) -> Unit,
  onMonthChange: (Int) -> Unit,
  onYearChange: (Int) -> Unit,
  onDobFocusChange: (Boolean) -> Unit,
  haptics: BioSyncHapticController,
  metrics: CalibrationResponsiveMetrics,
) {
  val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
  var showMonthPopup by rememberSaveable { mutableStateOf(false) }
  var isDayFocused by remember { mutableStateOf(false) }
  var isYearFocused by remember { mutableStateOf(false) }

  LaunchedEffect(isDayFocused, isYearFocused) {
    onDobFocusChange(isDayFocused || isYearFocused)
  }

  var dayInputText by rememberSaveable {
    mutableStateOf(selectedDay.toString().padStart(2, '0'))
  }
  var yearInputText by rememberSaveable {
    mutableStateOf(selectedYear.toString())
  }

  val dayFocusRequester = remember { FocusRequester() }
  val yearFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  val isAnyDobActive = isDayFocused || isYearFocused || showMonthPopup
  val safeMonthIdx = selectedMonth.coerceIn(0, 11)
  val formattedIsoDate = remember(selectedDay, safeMonthIdx, selectedYear) {
    "${selectedYear}-${(safeMonthIdx + 1).toString().padStart(2, '0')}-${selectedDay.toString().padStart(2, '0')}"
  }
  val dayOfYearProgress = remember(selectedDay, safeMonthIdx) {
    ((safeMonthIdx * 30.4f + selectedDay) / 365.25f).coerceIn(0.03f, 0.98f)
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(metrics.labelToBoxGap),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.calib_field_dob_label),
        color = Zinc200,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.4f, 11.2f, 14.8f),
      )
      Text(
        text = "ISO • $formattedIsoDate",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
        letterSpacing = 0.6.sp,
      )
    }

    // Unified Architectural DOB Block (matching Step 02 Height/Weight blocks)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .writingFloatingBorder(isFocused = isAnyDobActive, cornerRadiusDp = 12.dp)
        .padding(horizontal = 12.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // 1. DAY: Direct Numeric Input Cell
        DirectNumberDobField(
          label = stringResource(R.string.calib_dob_day),
          value = dayInputText,
          placeholder = "DD",
          unitHint = "01-31",
          onValueChange = { raw ->
            haptics.tick()
            val digits = raw.filter { it.isDigit() }.take(2)
            dayInputText = digits
            val parsed = digits.toIntOrNull()
            if (parsed != null && parsed in 1..31) {
              onDayChange(parsed)
            }
          },
          onFocusChange = { isDayFocused = it },
          onFocusLost = {
            val parsed = dayInputText.toIntOrNull()
            val validDay = (parsed ?: selectedDay).coerceIn(1, 31)
            onDayChange(validDay)
            dayInputText = validDay.toString().padStart(2, '0')
          },
          focusRequester = dayFocusRequester,
          imeAction = ImeAction.Next,
          onImeAction = {
            haptics.selection()
            focusManager.clearFocus()
            showMonthPopup = true
          },
          metrics = metrics,
          testTag = "dob_day_selector",
          modifier = Modifier.weight(0.90f),
        )

        // 2. MONTH: Opens Realistic Floating 3D Monochrome Month Picker Modal
        MonthPopupTriggerField(
          label = stringResource(R.string.calib_dob_month),
          selectedMonthZeroBased = selectedMonth,
          isPopupOpen = showMonthPopup,
          onClick = {
            haptics.selection()
            focusManager.clearFocus()
            showMonthPopup = true
          },
          metrics = metrics,
          testTag = "dob_month_selector",
          modifier = Modifier.weight(1.18f),
        )

        // 3. YEAR: Direct Numeric Input Cell
        DirectNumberDobField(
          label = stringResource(R.string.calib_dob_year),
          value = yearInputText,
          placeholder = "YYYY",
          unitHint = "YEAR",
          onValueChange = { raw ->
            haptics.tick()
            val digits = raw.filter { it.isDigit() }.take(4)
            yearInputText = digits
            val parsed = digits.toIntOrNull()
            if (digits.length == 4 && parsed != null && parsed in 1900..currentYear) {
              onYearChange(parsed)
            }
          },
          onFocusChange = { isYearFocused = it },
          onFocusLost = {
            val parsed = yearInputText.toIntOrNull()
            val validYear = if (parsed != null && parsed in 1900..currentYear) parsed else selectedYear
            onYearChange(validYear)
            yearInputText = validYear.toString()
          },
          focusRequester = yearFocusRequester,
          imeAction = ImeAction.Done,
          onImeAction = {
            haptics.confirm()
            focusManager.clearFocus()
          },
          metrics = metrics,
          testTag = "dob_year_selector",
          modifier = Modifier.weight(0.98f),
        )
      }

      // Integrated Bottom Chronometric Vernier Scale Strip (mirrors Height & Weight blocks)
      DobChronometricScaleStrip(
        progress = dayOfYearProgress,
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp),
      )
    }
  }

  if (showMonthPopup) {
    FloatingMonthPickerDialog(
      selectedMonthZeroBased = selectedMonth,
      onSelectMonth = { chosenMonthIndex ->
        onMonthChange(chosenMonthIndex)
        showMonthPopup = false
      },
      onDismiss = { showMonthPopup = false },
      metrics = metrics,
    )
  }
}

@Composable
private fun DobChronometricScaleStrip(
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val animatedProgress by animateFloatAsState(
    targetValue = progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 240, easing = CalibrationEaseOut),
    label = "dob_scale_progress",
  )

  Spacer(
    modifier = modifier.drawWithCache {
      val glowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(145, 255, 255, 255)
          maskFilter = BlurMaskFilter(safeBlurRadius(4.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      onDrawBehind {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@onDrawBehind

        val tickCount = 24
        val activeX = (w * animatedProgress).coerceIn(0f, w)

        drawLine(
          color = Zinc850,
          start = Offset(0f, h * 0.55f),
          end = Offset(w, h * 0.55f),
          strokeWidth = 1.dp.toPx(),
        )
        if (activeX > 1f) {
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Zinc600.copy(alpha = 0.45f),
                Zinc300.copy(alpha = 0.85f),
                PureWhite,
              ),
              startX = 0f,
              endX = activeX,
            ),
            start = Offset(0f, h * 0.55f),
            end = Offset(activeX, h * 0.55f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        for (i in 0..tickCount) {
          val x = (w / tickCount.toFloat()) * i
          val isMajor = i % 6 == 0
          val isPassed = x <= activeX + 0.5f
          val tickH = if (isMajor) h * 0.72f else h * 0.38f
          val topY = (h - tickH) * 0.55f
          drawLine(
            color = when {
              isPassed && isMajor -> PureWhite.copy(alpha = 0.78f)
              isPassed -> Zinc400.copy(alpha = 0.55f)
              isMajor -> Zinc700
              else -> Zinc800.copy(alpha = 0.65f)
            },
            start = Offset(x, topY),
            end = Offset(x, topY + tickH),
            strokeWidth = if (isMajor) 1.15.dp.toPx() else 0.85.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        drawIntoCanvas { canvas ->
          canvas.drawCircle(Offset(activeX, h * 0.55f), 2.8.dp.toPx(), glowPaint)
        }
        drawCircle(
          color = PureWhite,
          radius = 2.1.dp.toPx(),
          center = Offset(activeX, h * 0.55f),
        )
      }
    },
  )
}

@Composable
private fun DirectNumberDobField(
  label: String,
  value: String,
  placeholder: String,
  unitHint: String,
  onValueChange: (String) -> Unit,
  onFocusChange: (Boolean) -> Unit,
  onFocusLost: () -> Unit,
  focusRequester: FocusRequester,
  imeAction: ImeAction,
  onImeAction: () -> Unit,
  metrics: CalibrationResponsiveMetrics,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  var hadFocus by remember { mutableStateOf(false) }
  LaunchedEffect(isFocused) {
    onFocusChange(isFocused)
  }
  val fieldFontSize = metrics.spScaled(14.2f, 12.4f, 16.5f)
  val centeredMonoStyle = remember(fieldFontSize) {
    TextStyle(
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = fieldFontSize,
      lineHeight = fieldFontSize * 1.15f,
      platformStyle = PlatformTextStyle(includeFontPadding = false),
      lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
      ),
    )
  }
  val cellShape = RoundedCornerShape(9.dp)
  val cellBorderColor by animateColorAsState(
    targetValue = if (isFocused) PureWhite.copy(alpha = 0.85f) else Zinc700.copy(alpha = 0.68f),
    animationSpec = tween(durationMillis = 160),
    label = "dob_cell_border_$label",
  )

  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    singleLine = true,
    cursorBrush = SolidColor(PureWhite),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Number,
      imeAction = imeAction,
    ),
    keyboardActions = KeyboardActions(
      onNext = { onImeAction() },
      onDone = { onImeAction() },
    ),
    textStyle = centeredMonoStyle,
    decorationBox = { innerTextField ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(cellShape)
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(
                if (isFocused) Color(0xFF1D1D25) else Color(0xFF17171D),
                Color(0xFF0D0D11),
              ),
            ),
          )
          .border(width = 1.dp, color = cellBorderColor, shape = cellShape)
          .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = label,
            color = if (isFocused) PureWhite else Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
            letterSpacing = 0.5.sp,
          )
          Text(
            text = unitHint,
            color = if (isFocused) Zinc300 else Zinc600,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(8.0f, 7.0f, 9.2f),
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 22.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (value.isEmpty()) {
            Text(
              text = placeholder,
              style = centeredMonoStyle.copy(
                color = Zinc600,
                fontWeight = FontWeight.Normal,
              ),
              maxLines = 1,
            )
          }
          innerTextField()
        }
      }
    },
    modifier = modifier
      .focusRequester(focusRequester)
      .onFocusChanged { state ->
        isFocused = state.isFocused
        if (state.isFocused) {
          hadFocus = true
        } else if (hadFocus) {
          onFocusLost()
        }
      }
      .testTag(testTag),
  )
}

@Composable
private fun MonthPopupTriggerField(
  label: String,
  selectedMonthZeroBased: Int,
  isPopupOpen: Boolean,
  onClick: () -> Unit,
  metrics: CalibrationResponsiveMetrics,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val safeIdx = selectedMonthZeroBased.coerceIn(0, 11)
  val monthShort = MonthShortNames[safeIdx]
  val monthNum = (safeIdx + 1).toString().padStart(2, '0')
  val boxShape = RoundedCornerShape(9.dp)

  val borderColor by animateColorAsState(
    targetValue = if (isPopupOpen) PureWhite.copy(alpha = 0.90f) else Zinc600.copy(alpha = 0.80f),
    animationSpec = tween(durationMillis = 150),
    label = "month_trigger_border",
  )

  Column(
    modifier = modifier
      .clip(boxShape)
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            if (isPopupOpen) Color(0xFF202029) else Color(0xFF191920),
            Color(0xFF0E0E13),
          ),
        ),
      )
      .border(width = 1.dp, color = borderColor, shape = boxShape)
      .clickable(
        role = Role.DropdownList,
        onClick = onClick,
      )
      .padding(horizontal = 10.dp, vertical = 7.dp)
      .testTag(testTag),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        color = if (isPopupOpen) PureWhite else Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
        letterSpacing = 0.5.sp,
      )
      Text(
        text = "M-$monthNum",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(8.2f, 7.2f, 9.4f),
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 22.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "$monthShort ($monthNum)",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(13.5f, 11.8f, 15.6f),
        maxLines = 1,
      )

      UnfoldMoreIcon(
        color = if (isPopupOpen) PureWhite else Zinc300,
        size = (15f * metrics.scale).coerceIn(13.5f, 18f).dp,
      )
    }
  }
}

/**
 * Realistic, Floating 3D Monochrome Architectural Month Picker Popup.
 * Features deep elevation shadows, a specular liquid-silver rim, and a 3x4 tactile month matrix.
 */
@Composable
private fun FloatingMonthPickerDialog(
  selectedMonthZeroBased: Int,
  onSelectMonth: (Int) -> Unit,
  onDismiss: () -> Unit,
  metrics: CalibrationResponsiveMetrics,
) {
  val entryAnim = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    entryAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 280, easing = CalibrationEaseOut),
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val modalCorner = 20.dp
    val modalShape = RoundedCornerShape(modalCorner)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBlack.copy(alpha = 0.72f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss,
        )
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 390.dp)
          .fillMaxWidth()
          .graphicsLayer {
            val p = entryAnim.value
            alpha = p
            scaleX = 0.92f + 0.08f * p
            scaleY = 0.92f + 0.08f * p
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          )
          .drawWithCache {
            val cornerPx = modalCorner.toPx()
            val deepShadowPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(235, 0, 0, 0)
                maskFilter = BlurMaskFilter(safeBlurRadius(34.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
              }
            }
            val silverHaloPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(70, 240, 244, 255)
                maskFilter = BlurMaskFilter(safeBlurRadius(18.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
              }
            }
            onDrawBehind {
              drawIntoCanvas { canvas ->
                canvas.drawRoundRect(
                  4.dp.toPx(),
                  14.dp.toPx(),
                  size.width - 4.dp.toPx(),
                  size.height + 16.dp.toPx(),
                  cornerPx,
                  cornerPx,
                  deepShadowPaint,
                )
                canvas.drawRoundRect(
                  0f,
                  2.dp.toPx(),
                  size.width,
                  size.height + 4.dp.toPx(),
                  cornerPx,
                  cornerPx,
                  silverHaloPaint,
                )
              }
            }
          }
          .clip(modalShape)
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFF18181D),
                Color(0xFF0E0E12),
                Color(0xFF08080A),
              ),
            ),
          )
          .border(
            width = 1.4.dp,
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.55f),
              0.30f to Zinc600.copy(alpha = 0.65f),
              0.75f to Zinc850,
              1.0f to Zinc900,
            ),
            shape = modalShape,
          )
          .padding(20.dp)
          .testTag("floating_month_picker_dialog"),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              LuminousWhiteDot(size = 7.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "CHRONOMETRIC MONTH",
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
                letterSpacing = 1.4.sp,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Select Birth Month",
              color = PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(18f, 15.5f, 21f),
              letterSpacing = (-0.3).sp,
            )
          }

          // Active Month Pill Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Zinc900)
              .border(1.dp, Zinc700, RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp),
          ) {
            val activeIdx = selectedMonthZeroBased.coerceIn(0, 11)
            Text(
              text = "${MonthShortNames[activeIdx].uppercase(Locale.US)} • ${(activeIdx + 1).toString().padStart(2, '0')}",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(10.5f, 9f, 12f),
              letterSpacing = 0.8.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(thickness = 1.dp, color = Zinc850)

        Spacer(modifier = Modifier.height(14.dp))

        // 3 x 4 Architectural Month Grid
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          for (rowIdx in 0 until 4) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              for (colIdx in 0 until 3) {
                val monthIndex = rowIdx * 3 + colIdx
                val isSelected = monthIndex == selectedMonthZeroBased
                val cellShape = RoundedCornerShape(10.dp)

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(cellShape)
                    .background(
                      brush = if (isSelected) {
                        Brush.verticalGradient(
                          colors = listOf(
                            PureWhite,
                            Color(0xFFE6E8EE),
                          ),
                        )
                      } else {
                        Brush.verticalGradient(
                          colors = listOf(
                            Color(0xFF16161B),
                            Color(0xFF0F0F13),
                          ),
                        )
                      },
                    )
                    .border(
                      width = if (isSelected) 1.4.dp else 1.dp,
                      color = if (isSelected) PureWhite else Zinc800,
                      shape = cellShape,
                    )
                    .clickable(
                      role = Role.Button,
                      onClick = { onSelectMonth(monthIndex) },
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("month_option_${monthIndex + 1}"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = (monthIndex + 1).toString().padStart(2, '0'),
                        color = if (isSelected) ObsidianBlack.copy(alpha = 0.65f) else Zinc500,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = metrics.spScaled(9.5f, 8.5f, 11f),
                      )
                      if (isSelected) {
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                      text = MonthShortNames[monthIndex].uppercase(Locale.US),
                      color = if (isSelected) ObsidianBlack else PureWhite,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = metrics.spScaled(14f, 12f, 16f),
                      letterSpacing = (-0.2).sp,
                    )
                    Text(
                      text = MonthFullNames[monthIndex],
                      color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else Zinc500,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.Normal,
                      fontSize = metrics.spScaled(9.8f, 8.5f, 11f),
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
  }
}

@Composable
private fun UnfoldMoreIcon(
  color: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val upDownPath = Path().apply {
      moveTo(w * 0.30f, h * 0.38f)
      lineTo(w * 0.50f, h * 0.18f)
      lineTo(w * 0.70f, h * 0.38f)
      moveTo(w * 0.30f, h * 0.62f)
      lineTo(w * 0.50f, h * 0.82f)
      lineTo(w * 0.70f, h * 0.62f)
    }
    drawPath(path = upDownPath, color = color, style = stroke)
  }
}

@Composable
private fun ChronometricDisplayCard(
  chronometry: ChronometricResult,
  phaseProvider: () -> Float,
  borderFlashProvider: () -> Float,
  metrics: CalibrationResponsiveMetrics,
) {
  val cardCorner = (18f * metrics.scale).coerceIn(14f, 22f).dp
  val cardShape = RoundedCornerShape(cardCorner)

  val animatedYears by remember(chronometry, phaseProvider) {
    derivedStateOf {
      val factor = phaseProvider().coerceIn(0f, 1f)
      (chronometry.years * factor).roundToInt()
    }
  }
  val animatedMonths by remember(chronometry, phaseProvider) {
    derivedStateOf {
      val factor = phaseProvider().coerceIn(0f, 1f)
      (chronometry.months * factor).roundToInt()
    }
  }
  val animatedDays by remember(chronometry, phaseProvider) {
    derivedStateOf {
      val factor = phaseProvider().coerceIn(0f, 1f)
      (chronometry.days * factor).roundToInt()
    }
  }
  val animatedTotalDays by remember(chronometry, phaseProvider) {
    derivedStateOf {
      val factor = phaseProvider().coerceIn(0f, 1f)
      (chronometry.totalDays * factor).roundToInt()
    }
  }
  val animatedBdayRem by remember(chronometry, phaseProvider) {
    derivedStateOf {
      val factor = phaseProvider().coerceIn(0f, 1f)
      max(1, (chronometry.nextAnniversaryDaysRemaining * factor).roundToInt())
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = metrics.chronoCardMinHeight)
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
        val silverAmbientShadowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(46, 225, 230, 240)
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
          0.0f to PureWhite.copy(alpha = 0.30f),
          0.28f to Zinc700.copy(alpha = 0.65f),
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

          // 1. Deep 3D Floating Shadow + One-Time Traveling Rear Backlight Flash
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
              silverAmbientShadowPaint,
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

          // 3. Base Architectural Monochrome Border
          val baseBorderAlpha = (1f - 0.38f * flashEnvelope).coerceIn(0.42f, 1f)
          drawRoundRect(
            brush = baseBorderBrush,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            alpha = baseBorderAlpha,
            style = Stroke(width = 1.25.dp.toPx()),
          )

          // 4. One-Time Traveling Border Flash & Liquid-Chrome Reflection Wave
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
                  Zinc300.copy(alpha = 0.05f * flashEnvelope),
                  PureWhite.copy(alpha = 0.12f * flashEnvelope),
                  Color(0xFFF7EFE6).copy(alpha = 0.06f * flashEnvelope),
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
                  Zinc500.copy(alpha = 0.40f * flashEnvelope),
                  Color(0xFFD4D8E2).copy(alpha = 0.85f * flashEnvelope),
                  PureWhite.copy(alpha = 1.0f * flashEnvelope),
                  Color(0xFFF7EFE6).copy(alpha = 0.90f * flashEnvelope),
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
            ChronoCardBgTop,
            ChronoCardBgBottom,
          ),
        ),
      )
      .padding(metrics.chronoCardPadding)
      .testTag("chronometric_status_card"),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // 1. Header / Label (with PureWhite dot matching BioSync title color)
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = (13f * metrics.scale).coerceIn(10f, 18f).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LuminousWhiteDot(
            size = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.calib_chrono_status),
            color = Zinc100,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(12f, 10.2f, 14f),
            letterSpacing = 1.1.sp,
          )
        }
      }

      HorizontalDivider(
        thickness = 1.dp,
        color = Zinc800.copy(alpha = 0.85f),
      )
    }

    Spacer(modifier = Modifier.height(metrics.chronoSectionSpacing))

    // 2. Enlarged Primary Stat: Calculated Age + Multi-Tone White / Liquid-Chrome Progress Bar
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("calculated_age_section"),
    ) {
      Text(
        text = stringResource(R.string.calib_calculated_age),
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(11.8f, 10f, 13.5f),
        letterSpacing = 1.3.sp,
      )

      Spacer(modifier = Modifier.height((9f * metrics.scale).coerceIn(7f, 13f).dp))

      Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth(),
      ) {
        // Years
        Text(
          text = animatedYears.toString().padStart(2, '0'),
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.ageYearsFontSize,
          lineHeight = metrics.ageYearsFontSize,
          letterSpacing = (-1.0).sp,
          modifier = Modifier.testTag("chrono_years_text"),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.calib_unit_years),
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.ageUnitLabelFontSize,
          modifier = Modifier.padding(bottom = 4.dp, end = 14.dp),
        )

        // Months
        Text(
          text = animatedMonths.toString().padStart(2, '0'),
          color = Zinc200,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.ageSubUnitFontSize,
          lineHeight = metrics.ageSubUnitFontSize,
          modifier = Modifier
            .padding(bottom = 2.dp)
            .testTag("chrono_months_text"),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = stringResource(R.string.calib_unit_months),
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.ageUnitLabelFontSize,
          modifier = Modifier.padding(bottom = 4.dp, end = 14.dp),
        )

        // Days
        Text(
          text = animatedDays.toString().padStart(2, '0'),
          color = Zinc200,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.ageSubUnitFontSize,
          lineHeight = metrics.ageSubUnitFontSize,
          modifier = Modifier
            .padding(bottom = 2.dp)
            .testTag("chrono_days_text"),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = stringResource(R.string.calib_unit_days),
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.ageUnitLabelFontSize,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }

      Spacer(modifier = Modifier.height((15f * metrics.scale).coerceIn(12f, 20f).dp))

      // Multi-Tone White / Liquid Chrome Capsule Progress Bar
      CalculatedAgeProgressBar(
        progressProvider = {
          chronometry.calculatedAgeProgress * phaseProvider().coerceIn(0f, 1f)
        },
        shimmerBoostProvider = borderFlashProvider,
        gradientColors = WhiteVariantMulticolorGradient,
        barHeight = metrics.ageBarHeight,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("calculated_age_progress_bar"),
      )
    }

    Spacer(modifier = Modifier.height(metrics.chronoSectionSpacing))

    // 3. Enlarged Secondary Metrics Grid (Total Duration & Next Birthday)
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      HorizontalDivider(
        thickness = 1.dp,
        color = Zinc800.copy(alpha = 0.85f),
      )

      Spacer(modifier = Modifier.height((15f * metrics.scale).coerceIn(12f, 20f).dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        // Total Duration
        Column(
          verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4f, 8f).dp),
        ) {
          Text(
            text = stringResource(R.string.calib_total_duration),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
            letterSpacing = 1.0.sp,
          )
          Row(
            verticalAlignment = Alignment.Bottom,
          ) {
            Text(
              text = NumberFormat.getIntegerInstance(Locale.US).format(animatedTotalDays),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.secondaryStatFontSize,
              modifier = Modifier.testTag("chrono_total_days_text"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.calib_unit_days),
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(13f, 11f, 15.5f),
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
        }

        // Next Birthday
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4f, 8f).dp),
        ) {
          Text(
            text = stringResource(R.string.calib_next_anniversary),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
            letterSpacing = 1.0.sp,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = chronometry.nextAnniversaryDate,
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.secondaryStatFontSize,
            )
            Text(
              text = " • ",
              color = Zinc500,
              fontFamily = InterFontFamily,
              fontSize = metrics.spScaled(14f, 12f, 17f),
            )
            Text(
              text = "${animatedBdayRem}d",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.secondaryStatFontSize,
            )
          }
        }
      }
    }
  }
}

/**
 * Multi-Tone White / Liquid-Chrome Capsule Progress Bar
 * with dimensional surface lighting, pearl-silver specular reflections, and smooth entry animation.
 */
@Composable
private fun CalculatedAgeProgressBar(
  progressProvider: () -> Float,
  shimmerBoostProvider: () -> Float,
  gradientColors: List<Color>,
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
            Zinc600.copy(alpha = 0.35f),
            MonochromeTrackBorder,
            PureWhite.copy(alpha = 0.25f),
          ),
        )
        val verticalLightingBrush = Brush.verticalGradient(
          0.0f to PureWhite.copy(alpha = 0.38f),
          0.42f to Color.Transparent,
          1.0f to ObsidianBlack.copy(alpha = 0.24f),
          startY = 0f,
          endY = trackHeight,
        )
        val topRimBrush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.45f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = (trackHeight * 0.65f).coerceAtLeast(1f),
        )
        val fillPath = Path()

        onDrawBehind {
          if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
          val progress = progressProvider().coerceIn(0f, 1f)
          val boost = shimmerBoostProvider().coerceIn(0f, 1f)

          drawRoundRect(
            color = MonochromeTrack,
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

              val sheenRatio = if (boost > 0.01f) boost else 0.38f
              val sheenCenter = fillWidth * sheenRatio
              val sheenSpan = (fillWidth * 0.28f).coerceAtLeast(trackHeight * 2.2f)
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color.Transparent,
                    PureWhite.copy(alpha = 0.34f),
                    Color(0xFFF7EFE6).copy(alpha = 0.22f),
                    Color.Transparent,
                  ),
                  start = Offset(sheenCenter - sheenSpan, 0f),
                  end = Offset(sheenCenter + sheenSpan, trackHeight),
                ),
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )
            }

            drawRoundRect(
              brush = topRimBrush,
              topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
              size = Size(
                (fillWidth - 1.dp.toPx()).coerceAtLeast(1f),
                (trackHeight - 1.dp.toPx()).coerceAtLeast(1f),
              ),
              cornerRadius = cornerRadius,
              style = Stroke(width = 0.85.dp.toPx()),
            )
          }
        }
      },
  )
}

@Composable
private fun CalibrationContinueSection(
  onContinueClick: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: CalibrationResponsiveMetrics,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val buttonCorner = (10f * metrics.scale).coerceIn(8f, 13f).dp
  val buttonShape = RoundedCornerShape(buttonCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "continue_button_glow_transition")
  val shimmerPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "continue_shimmer_phase",
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
        .testTag("calib_continue_button"),
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
      modifier = Modifier.testTag("calib_footer_encrypted_text"),
    )
  }
}
