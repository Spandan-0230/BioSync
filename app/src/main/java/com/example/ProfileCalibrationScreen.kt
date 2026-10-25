package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch

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
  val fieldSpacing: Dp,
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
  val rawScale = min(w / 385f, h / 820f).coerceIn(0.76f, 1.30f)

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

  return CalibrationResponsiveMetrics(
    scale = rawScale,
    fontCompensation = fontCompensation,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (20f * rawScale).coerceIn(16f, 28f).dp,
    topContentPadding = (10f * rawScale).coerceIn(7f, 15f).dp,
    bottomContentPadding = (6f * rawScale).coerceIn(4f, 10f).dp,
    headerHeight = (56f * rawScale).coerceIn(50f, 66f).dp,
    titleFontSize = scaledSp(23.5f, 19f, 28f),
    subtitleFontSize = scaledSp(12f, 10.2f, 14f),
    subtitleLineHeight = scaledSp(17f, 14.5f, 20f),
    fieldSpacing = (h * 0.0135f).coerceIn(9f, 14.5f).dp,
    inputHeight = (45f * rawScale).coerceIn(41f, 50f).dp,
    selectHeight = (44f * rawScale).coerceIn(40f, 50f).dp,
    toggleHeight = (38f * rawScale).coerceIn(35f, 44f).dp,
    chronoCardMinHeight = (h * 0.295f).coerceIn(216f, 288f).dp,
    chronoCardPadding = (20f * rawScale).coerceIn(16f, 26f).dp,
    chronoSectionSpacing = (15.5f * rawScale).coerceIn(12f, 21f).dp,
    ageYearsFontSize = scaledSp(42f, 32f, 50f),
    ageSubUnitFontSize = scaledSp(26f, 20f, 32f),
    ageUnitLabelFontSize = scaledSp(13.5f, 11f, 16f),
    ageBarHeight = (11.5f * rawScale).coerceIn(9.5f, 14f).dp,
    secondaryStatFontSize = scaledSp(18f, 15f, 22f),
    ctaHeight = (49f * rawScale).coerceIn(45f, 56f).dp,
  )
}

@Composable
fun ProfileCalibrationScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  var subjectName by rememberSaveable { mutableStateOf("Elena Rostova") }
  var selectedSex by rememberSaveable { mutableStateOf("FEMALE") }
  var selectedDay by rememberSaveable { mutableIntStateOf(24) }
  var selectedMonth by rememberSaveable { mutableIntStateOf(9) } // 9 = Oct (10)
  var selectedYear by rememberSaveable { mutableIntStateOf(1994) }

  val chronometry = remember(selectedDay, selectedMonth, selectedYear) {
    calculateChronometry(
      selectedDay = selectedDay,
      selectedMonthZeroBased = selectedMonth,
      selectedYear = selectedYear,
    )
  }

  val coroutineScope = rememberCoroutineScope()
  // Entry animation phase [0f..1f]: animates personal chronometric values from 0 to their actual calculated values.
  val calibPhase = remember { Animatable(0f) }
  // One-time border flash & rear backlight reflection wave on ChronometricDisplayCard when Continue is pressed
  val borderFlashPhase = remember { Animatable(0f) }

  LaunchedEffect(selectedDay, selectedMonth, selectedYear) {
    calibPhase.snapTo(0f)
    calibPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 720, easing = CalibrationEaseOut),
    )
  }

  val onContinueClick: () -> Unit = {
    coroutineScope.launch {
      borderFlashPhase.snapTo(0f)
      borderFlashPhase.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 680, easing = CalibrationEaseOut),
      )
      borderFlashPhase.snapTo(0f)
    }
  }

  val phaseProvider = remember(calibPhase) { { calibPhase.value } }
  val borderFlashProvider = remember(borderFlashPhase) { { borderFlashPhase.value } }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack),
    containerColor = ObsidianBlack,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    // Use WindowInsets.systemBars on BoxWithConstraints so opening the soft keyboard
    // NEVER shrinks maxHeight or squeezes the screen layout.
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBlack)
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.systemBars),
      contentAlignment = Alignment.TopCenter,
    ) {
      val fontScale = LocalDensity.current.fontScale
      val metrics = remember(maxWidth, maxHeight, fontScale) {
        computeCalibrationMetrics(
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          fontScale = fontScale,
        )
      }
      val scrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .testTag("profile_calibration_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Sticky Top Header Bar with Shiny Glowy White Step Bar under STEP 01 OF 03
        CalibrationHeaderBar(
          onBackClick = onNavigateBack,
          phaseProvider = phaseProvider,
          metrics = metrics,
        )

        // Exact viewport measurement for the scrollable body area:
        // Fits 100% into the screen height with zero unnecessary scroll when space allows,
        // and scrolls smoothly only when needed (compact screens or when soft keyboard is open),
        // stopping immediately at "Encrypted on-device • Private & Secure".
        BoxWithConstraints(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .imePadding(),
          contentAlignment = Alignment.TopCenter,
        ) {
          val exactBodyHeight = maxHeight

          Column(
            modifier = Modifier
              .widthIn(max = metrics.contentMaxWidth)
              .fillMaxWidth()
              .verticalScroll(scrollState)
              .heightIn(min = exactBodyHeight)
              .padding(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = metrics.topContentPadding,
                bottom = metrics.bottomContentPadding,
              ),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
          // Upper Form Group
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(metrics.fieldSpacing),
          ) {
            // Top Eyebrow, Section Title & Description under Profile Section
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
              Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3f, 7f).dp))
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

            FullNameField(
              value = subjectName,
              onValueChange = { subjectName = it },
              metrics = metrics,
            )

            BiologicalSexSelector(
              selectedSex = selectedSex,
              onSelectSex = { selectedSex = it },
              metrics = metrics,
            )

            DateOfBirthSelectorRow(
              selectedDay = selectedDay,
              selectedMonth = selectedMonth,
              selectedYear = selectedYear,
              onDayChange = { selectedDay = it },
              onMonthChange = { selectedMonth = it },
              onYearChange = { selectedYear = it },
              metrics = metrics,
            )
          }

          Spacer(modifier = Modifier.height(metrics.fieldSpacing))

          // Enlarged & Taller Architectural Chronometric Display Card (with One-Time Border Flash & Multi-Tone White Progress Bar)
          ChronometricDisplayCard(
            chronometry = chronometry,
            phaseProvider = phaseProvider,
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.fieldSpacing))

          // Primary Action Button & Encrypted Footer (Content ends right here with no trailing scroll space)
          CalibrationContinueSection(
            onContinueClick = onContinueClick,
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
          }
        }
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
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(ObsidianBlack),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = metrics.headerHeight)
        .padding(
          horizontal = metrics.horizontalPadding,
          vertical = (6f * metrics.scale).coerceIn(4f, 9f).dp,
        ),
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

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc900,
    )
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

          // 1. Subtle 3-step background track with step tick dividers
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
  metrics: CalibrationResponsiveMetrics,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) Zinc400 else Zinc800,
    animationSpec = tween(durationMillis = 150),
    label = "name_border_color",
  )
  val fieldShape = RoundedCornerShape(8.dp)

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4f, 9f).dp),
  ) {
    val nameTag = stringResource(R.string.calib_field_name_tag)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.calib_field_name_label),
        color = Zinc300,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.2f, 11f, 14.5f),
      )
      if (nameTag.isNotEmpty()) {
        Text(
          text = nameTag,
          color = Zinc600,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
          letterSpacing = 0.8.sp,
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(metrics.inputHeight)
        .clip(fieldShape)
        .background(ObsidianBlack)
        .border(width = 1.dp, color = borderColor, shape = fieldShape)
        .padding(horizontal = 14.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(modifier = Modifier.weight(1f)) {
          if (value.isEmpty()) {
            Text(
              text = stringResource(R.string.calib_field_name_placeholder),
              color = Zinc700,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = metrics.spScaled(13.8f, 12f, 16f),
            )
          }
          BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(PureWhite),
            textStyle = TextStyle(
              color = Zinc100,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = metrics.spScaled(13.8f, 12f, 16f),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_name_input"),
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        BadgeVectorIcon(
          color = Zinc600,
          size = (18f * metrics.scale).coerceIn(16f, 22f).dp,
        )
      }
    }
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
) {
  val containerShape = RoundedCornerShape(8.dp)
  val pillShape = RoundedCornerShape(6.dp)

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4f, 9f).dp),
  ) {
    val sexTag = stringResource(R.string.calib_field_sex_tag)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.calib_field_sex_label),
        color = Zinc300,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(12.2f, 11f, 14.5f),
      )
      if (sexTag.isNotEmpty()) {
        Text(
          text = sexTag,
          color = Zinc600,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
          letterSpacing = 0.8.sp,
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(containerShape)
        .background(Zinc950)
        .border(width = 1.dp, color = Zinc850, shape = containerShape)
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val options = listOf(
        "MALE" to R.string.calib_sex_male,
        "FEMALE" to R.string.calib_sex_female,
      )
      options.forEach { (key, labelRes) ->
        val isSelected = selectedSex == key
        val bgColor by animateColorAsState(
          targetValue = if (isSelected) PureWhite else Color.Transparent,
          animationSpec = tween(durationMillis = 160),
          label = "sex_bg_$key",
        )
        val textColor by animateColorAsState(
          targetValue = if (isSelected) ObsidianBlack else Zinc400,
          animationSpec = tween(durationMillis = 160),
          label = "sex_text_$key",
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .height(metrics.toggleHeight)
            .clip(pillShape)
            .background(bgColor)
            .clickable(
              role = Role.RadioButton,
              onClick = { onSelectSex(key) },
            )
            .testTag("gender_${key.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(labelRes),
            color = textColor,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(12.2f, 11f, 14.5f),
          )
        }
      }
    }
  }
}

/**
 * Date of Birth Section:
 * - DAY: Direct numeric input field (01..31)
 * - MONTH: Tapping opens a realistic, floating 3D monochrome Month Picker modal popup (Jan..Dec)
 * - YEAR: Direct numeric input field (1900..currentYear)
 */
@Composable
private fun DateOfBirthSelectorRow(
  selectedDay: Int,
  selectedMonth: Int,
  selectedYear: Int,
  onDayChange: (Int) -> Unit,
  onMonthChange: (Int) -> Unit,
  onYearChange: (Int) -> Unit,
  metrics: CalibrationResponsiveMetrics,
) {
  val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
  var showMonthPopup by rememberSaveable { mutableStateOf(false) }

  var dayInputText by rememberSaveable(selectedDay) {
    mutableStateOf(selectedDay.toString().padStart(2, '0'))
  }
  var yearInputText by rememberSaveable(selectedYear) {
    mutableStateOf(selectedYear.toString())
  }

  val dayFocusRequester = remember { FocusRequester() }
  val yearFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4f, 9f).dp),
  ) {
    Text(
      text = stringResource(R.string.calib_field_dob_label),
      color = Zinc300,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(12.2f, 11f, 14.5f),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // 1. DAY: Direct Numeric Input
      DirectNumberDobField(
        label = stringResource(R.string.calib_dob_day),
        value = dayInputText,
        placeholder = "DD",
        unitHint = "01-31",
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(2)
          dayInputText = digits
          val parsed = digits.toIntOrNull()
          if (parsed != null && parsed in 1..31) {
            onDayChange(parsed)
          }
        },
        onFocusLost = {
          val parsed = dayInputText.toIntOrNull()
          val validDay = (parsed ?: selectedDay).coerceIn(1, 31)
          onDayChange(validDay)
          dayInputText = validDay.toString().padStart(2, '0')
        },
        focusRequester = dayFocusRequester,
        imeAction = ImeAction.Next,
        onImeAction = {
          focusManager.clearFocus()
          showMonthPopup = true
        },
        metrics = metrics,
        testTag = "dob_day_selector",
        modifier = Modifier.weight(0.92f),
      )

      // 2. MONTH: Opens Realistic Floating 3D Monochrome Month Picker Modal
      MonthPopupTriggerField(
        label = stringResource(R.string.calib_dob_month),
        selectedMonthZeroBased = selectedMonth,
        isPopupOpen = showMonthPopup,
        onClick = {
          focusManager.clearFocus()
          showMonthPopup = true
        },
        metrics = metrics,
        testTag = "dob_month_selector",
        modifier = Modifier.weight(1.16f),
      )

      // 3. YEAR: Direct Numeric Input
      DirectNumberDobField(
        label = stringResource(R.string.calib_dob_year),
        value = yearInputText,
        placeholder = "YYYY",
        unitHint = "YEAR",
        onValueChange = { raw ->
          val digits = raw.filter { it.isDigit() }.take(4)
          yearInputText = digits
          val parsed = digits.toIntOrNull()
          if (digits.length == 4 && parsed != null && parsed in 1900..currentYear) {
            onYearChange(parsed)
          }
        },
        onFocusLost = {
          val parsed = yearInputText.toIntOrNull()
          val validYear = if (parsed != null && parsed in 1900..currentYear) parsed else selectedYear
          onYearChange(validYear)
          yearInputText = validYear.toString()
        },
        focusRequester = yearFocusRequester,
        imeAction = ImeAction.Done,
        onImeAction = { focusManager.clearFocus() },
        metrics = metrics,
        testTag = "dob_year_selector",
        modifier = Modifier.weight(0.96f),
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
private fun DirectNumberDobField(
  label: String,
  value: String,
  placeholder: String,
  unitHint: String,
  onValueChange: (String) -> Unit,
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
  val boxShape = RoundedCornerShape(8.dp)

  val borderColor by animateColorAsState(
    targetValue = if (isFocused) PureWhite.copy(alpha = 0.85f) else Zinc800,
    animationSpec = tween(durationMillis = 150),
    label = "direct_dob_border",
  )

  Column(modifier = modifier) {
    Text(
      text = label,
      color = Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(10f, 8.5f, 11.5f),
      letterSpacing = 0.5.sp,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(metrics.selectHeight)
        .clip(boxShape)
        .background(ObsidianBlack)
        .border(width = 1.dp, color = borderColor, shape = boxShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = { focusRequester.requestFocus() },
        )
        .padding(horizontal = 11.dp)
        .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (value.isEmpty()) {
          Text(
            text = placeholder,
            color = Zinc600,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(12.5f, 11f, 14.5f),
            maxLines = 1,
          )
        }
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
          textStyle = TextStyle(
            color = Zinc100,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(13f, 11.2f, 15f),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
              isFocused = state.isFocused
              if (state.isFocused) {
                hadFocus = true
              } else if (hadFocus) {
                onFocusLost()
              }
            }
            .testTag("${testTag}_input"),
        )
      }

      Text(
        text = unitHint,
        color = if (isFocused) Zinc400 else Zinc700,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = metrics.spScaled(8.8f, 7.5f, 10f),
      )
    }
  }
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
  val displayText = "${MonthShortNames[safeIdx]} (${(safeIdx + 1).toString().padStart(2, '0')})"
  val boxShape = RoundedCornerShape(8.dp)

  val borderColor by animateColorAsState(
    targetValue = if (isPopupOpen) PureWhite.copy(alpha = 0.90f) else Zinc700,
    animationSpec = tween(durationMillis = 150),
    label = "month_trigger_border",
  )

  Column(modifier = modifier) {
    Text(
      text = label,
      color = Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(10f, 8.5f, 11.5f),
      letterSpacing = 0.5.sp,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(metrics.selectHeight)
        .clip(boxShape)
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF141418),
              ObsidianBlack,
            ),
          ),
        )
        .border(width = 1.dp, color = borderColor, shape = boxShape)
        .clickable(
          role = Role.DropdownList,
          onClick = onClick,
        )
        .padding(horizontal = 11.dp)
        .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = displayText,
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(12.5f, 11f, 14.5f),
        maxLines = 1,
      )

      UnfoldMoreIcon(
        color = if (isPopupOpen) PureWhite else Zinc300,
        size = (16f * metrics.scale).coerceIn(14f, 19f).dp,
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
        // Rear backlight flash wave paint for the one-time border flash when Continue is pressed
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

          // 3. Base Architectural Monochrome Border (dims slightly as the flash wave passes for contrast)
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

            // Subtle diagonal surface reflection sheen across the card face
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

            // Brilliant traveling border flash reflection all over the card border
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

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Zinc900)
            .border(1.dp, Zinc700.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = stringResource(R.string.calib_active_index),
            color = Zinc200,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(10.2f, 8.8f, 12f),
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

          // 1. Recessed Monochrome Capsule Track
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

            // 2. Multi-Tone Liquid Chrome & Pearl-White Gradient Fill
            drawPath(
              path = fillPath,
              brush = Brush.horizontalGradient(
                colors = gradientColors,
                startX = 0f,
                endX = fillWidth.coerceAtLeast(1f),
              ),
            )

            // 3. Liquid-Silver Specular Folds & Dimensional Surface Lighting
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

            // 4. Crisp Pure-White Top Rim Highlight
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

            // 1. Soft Luminous White Outer Glow Aura
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

            // 2. Crisp Metallic White Button Surface
            drawRoundRect(
              brush = if (isPressed) pressedBrush else baseMetallicBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )

            // 3. Sweeping Specular Liquid-Chrome Shimmer Band
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

            // 4. Luminous Specular Rim Border
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
