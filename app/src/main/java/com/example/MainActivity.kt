package com.example

import android.graphics.BlurMaskFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FatsEnd
import com.example.ui.theme.FatsMid
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationEnd
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Neutral200
import com.example.ui.theme.Neutral400
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      MyApplicationTheme {
        BioSyncApp()
      }
    }
  }
}

private fun safeBlurRadius(radius: Float): Float = radius.coerceAtLeast(0.5f)

@Composable
fun BioSyncApp() {
  var showCalibration by rememberSaveable { mutableStateOf(false) }

  AnimatedContent(
    targetState = showCalibration,
    transitionSpec = {
      if (targetState) {
        (slideInHorizontally(
          animationSpec = tween(durationMillis = 360, easing = TelemetryEaseOut),
          initialOffsetX = { fullWidth -> fullWidth / 4 },
        ) + fadeIn(animationSpec = tween(durationMillis = 300))) togetherWith
          (slideOutHorizontally(
            animationSpec = tween(durationMillis = 320, easing = TelemetryEaseOut),
            targetOffsetX = { fullWidth -> -fullWidth / 5 },
          ) + fadeOut(animationSpec = tween(durationMillis = 240)))
      } else {
        (slideInHorizontally(
          animationSpec = tween(durationMillis = 340, easing = TelemetryEaseOut),
          initialOffsetX = { fullWidth -> -fullWidth / 5 },
        ) + fadeIn(animationSpec = tween(durationMillis = 280))) togetherWith
          (slideOutHorizontally(
            animationSpec = tween(durationMillis = 320, easing = TelemetryEaseOut),
            targetOffsetX = { fullWidth -> fullWidth / 4 },
          ) + fadeOut(animationSpec = tween(durationMillis = 240)))
      }
    },
    label = "biosync_screen_transition",
  ) { isCalibrationScreen ->
    if (isCalibrationScreen) {
      ProfileCalibrationScreen(
        onNavigateBack = { showCalibration = false },
      )
    } else {
      BioSyncWelcomeScreen(
        onNavigateToCalibration = { showCalibration = true },
      )
    }
  }
}

// Smooth, responsive cubic-bezier easing for high-frame-rate telemetry animations
private val TelemetryEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

@Immutable
private data class MacroMetricItem(
  val id: String,
  val labelRes: Int,
  val currentVal: Int,
  val maxVal: Int,
  val unitSuffix: String,
  val targetDisplay: String,
  val useCommaFormat: Boolean,
  val currentProgress: Float,
  val gradientColors: List<Color>,
)

@Immutable
private data class BioSyncResponsiveMetrics(
  val isSideBySide: Boolean,
  val scale: Float,
  val contentMaxWidth: Dp,
  val horizontalPadding: Dp,
  val topPadding: Dp,
  val bottomPadding: Dp,
  val topBarToDialGap: Dp,
  val dialToHeadlineGap: Dp,
  val headlineToSubtitleGap: Dp,
  val minSectionGap: Dp,
  val dialSize: Dp,
  val scoreFontSize: TextUnit,
  val headlineFontSize: TextUnit,
  val headlineLineHeight: TextUnit,
  val subtitleFontSize: TextUnit,
  val subtitleLineHeight: TextUnit,
  val subtitleMaxWidth: Dp,
  val brandIconSize: Dp,
  val brandFontSize: TextUnit,
  val cardPadding: Dp,
  val cardCornerRadius: Dp,
  val energyLabelFontSize: TextUnit,
  val energyValueFontSize: TextUnit,
  val energyTargetFontSize: TextUnit,
  val badgeFontSize: TextUnit,
  val remainingFontSize: TextUnit,
  val macroRowSpacing: Dp,
  val macroFontSize: TextUnit,
  val macroDotSize: Dp,
  val macroBarHeight: Dp,
  val caloriesBarHeight: Dp,
  val ctaVerticalPadding: Dp,
  val ctaFontSize: TextUnit,
  val ctaIconSize: Dp,
  val footerFontSize: TextUnit,
  val ctaToFooterGap: Dp,
)

private fun computeResponsiveMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): BioSyncResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)

  val isSideBySide = (w >= 620f && w > h * 1.05f) || (w >= 840f && h >= 500f)

  val rawScale = if (isSideBySide) {
    (min(w / 780f, h / 460f) * 1.04f).coerceIn(0.80f, 1.32f)
  } else {
    (min(w / 380f, h / 800f) * 1.02f).coerceIn(0.75f, 1.32f)
  }

  // Normalize system fontScale so even extreme mobile font settings (e.g. 200%)
  // scale text harmoniously without overflowing containers or forcing excessive scroll.
  val safeFontScale = fontScale.coerceIn(0.5f, 3.0f)
  val targetFontGrowth = if (safeFontScale >= 1f) {
    1f + (safeFontScale - 1f).coerceAtMost(1.2f) * 0.14f
  } else {
    1f - (1f - safeFontScale).coerceAtMost(0.5f) * 0.20f
  }
  val fontCompensation = targetFontGrowth / safeFontScale

  fun scaledSp(
    base: Float,
    minSp: Float,
    maxSp: Float,
    factor: Float = rawScale,
  ): TextUnit {
    return ((base * factor).coerceIn(minSp, maxSp) * fontCompensation).sp
  }

  val dialSizeDp = if (isSideBySide) {
    min(w * 0.30f, h * 0.48f).coerceIn(145f, 280f).dp
  } else {
    min(w * 0.54f, h * 0.245f).coerceIn(132f, 255f).dp
  }

  val contentMaxWidth = when {
    isSideBySide -> (w * 0.95f).coerceAtMost(1120f).dp
    w >= 600f -> (w * 0.82f).coerceIn(440f, 600f).dp
    else -> w.dp
  }

  val dialRatio = (dialSizeDp.value / 220f).coerceIn(0.68f, 1.35f)

  val topBarToDialGap = (h * 0.013f).coerceIn(7f, 15f).dp
  val dialToHeadlineGap = (h * 0.014f).coerceIn(8f, 16f).dp
  val headlineToSubtitleGap = (4.5f * rawScale).coerceIn(3f, 8f).dp
  val minSectionGap = (h * 0.014f).coerceIn(9f, 20f).dp

  return BioSyncResponsiveMetrics(
    isSideBySide = isSideBySide,
    scale = rawScale,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (18f * rawScale).coerceIn(14f, 26f).dp,
    topPadding = (8f * rawScale).coerceIn(6f, 14f).dp,
    bottomPadding = (8f * rawScale).coerceIn(6f, 12f).dp,
    topBarToDialGap = topBarToDialGap,
    dialToHeadlineGap = dialToHeadlineGap,
    headlineToSubtitleGap = headlineToSubtitleGap,
    minSectionGap = minSectionGap,
    dialSize = dialSizeDp,
    scoreFontSize = scaledSp(48f, 29f, 60f, dialRatio),
    headlineFontSize = scaledSp(22.5f, 17f, 28f),
    headlineLineHeight = scaledSp(27.5f, 21f, 34f),
    subtitleFontSize = scaledSp(12.5f, 10.5f, 15.5f),
    subtitleLineHeight = scaledSp(18f, 14.5f, 22f),
    subtitleMaxWidth = (330f * rawScale).coerceIn(265f, 450f).dp,
    brandIconSize = (21f * rawScale).coerceIn(17.5f, 26f).dp,
    brandFontSize = scaledSp(15.5f, 13.5f, 20f),
    cardPadding = (15.5f * rawScale).coerceIn(12.5f, 22f).dp,
    cardCornerRadius = (14f * rawScale).coerceIn(11f, 18f).dp,
    energyLabelFontSize = scaledSp(10.5f, 9f, 13f),
    energyValueFontSize = scaledSp(24f, 18.5f, 30f),
    energyTargetFontSize = scaledSp(12f, 10f, 15f),
    badgeFontSize = scaledSp(10.5f, 9f, 13.5f),
    remainingFontSize = scaledSp(10.2f, 8.8f, 12.5f),
    macroRowSpacing = (h * 0.0125f).coerceIn(8.5f, 14.5f).dp,
    macroFontSize = scaledSp(12.4f, 10.2f, 15f),
    macroDotSize = (7.2f * rawScale).coerceIn(6f, 9f).dp,
    macroBarHeight = (8f * rawScale).coerceIn(6.5f, 10.5f).dp,
    caloriesBarHeight = (9f * rawScale).coerceIn(7.5f, 11.5f).dp,
    ctaVerticalPadding = (14.5f * rawScale).coerceIn(12f, 18f).dp,
    ctaFontSize = scaledSp(14.8f, 13f, 18f),
    ctaIconSize = (17f * rawScale).coerceIn(14.5f, 20.5f).dp,
    footerFontSize = scaledSp(10.8f, 9.5f, 12.5f),
    ctaToFooterGap = (8.5f * rawScale).coerceIn(6f, 12f).dp,
  )
}

/**
 * Maps master animation phase [0f..2f] to an interpolated value:
 * - phase in [0f..1f]: Launch animation from 0 -> currentValue
 * - phase in [1f..2f]: "Get Started" animation from currentValue -> maxValue (100% completion)
 */
private fun interpolateTelemetry(
  phase: Float,
  currentValue: Float,
  maxValue: Float,
): Float {
  return when {
    phase <= 1f -> (currentValue * phase.coerceIn(0f, 1f))
    else -> {
      val t = (phase - 1f).coerceIn(0f, 1f)
      currentValue + (maxValue - currentValue) * t
    }
  }
}

private fun formatWithCommas(value: Int): String {
  return NumberFormat.getIntegerInstance(Locale.US).format(value)
}

@Composable
fun BioSyncWelcomeScreen(
  onNavigateToCalibration: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  // Master animation phase:
  // 0f -> 1f on app launch (0 to current telemetry values)
  // -> 2f on Get Started click (reaches 100% max limit across all numbers, bars, and dial blocks)
  val telemetryPhase = remember { Animatable(0f) }
  // One-time border flash & rear reflection wave animation on the progress bar block (0f -> 1f)
  val borderFlashPhase = remember { Animatable(0f) }
  var isTransitioning by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    telemetryPhase.snapTo(0f)
    borderFlashPhase.snapTo(0f)
    telemetryPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 820, easing = TelemetryEaseOut),
    )
  }

  val onGetStartedClick: () -> Unit = {
    if (!isTransitioning) {
      isTransitioning = true
      coroutineScope.launch {
        // Fire the one-time border flash reflection wave across the progress bar block
        // while simultaneously bringing all Welcome screen telemetry to 100% max levels,
        // then smoothly shift to the Profile Section.
        launch {
          borderFlashPhase.snapTo(0f)
          borderFlashPhase.animateTo(
            targetValue = 1f,
            animationSpec = tween(
              durationMillis = 420,
              easing = TelemetryEaseOut,
            ),
          )
        }
        launch {
          telemetryPhase.animateTo(
            targetValue = 2f,
            animationSpec = tween(
              durationMillis = 280,
              easing = TelemetryEaseOut,
            ),
          )
        }
        delay(320)
        onNavigateToCalibration()
        isTransitioning = false
      }
    }
  }

  val phaseProvider = remember(telemetryPhase) { { telemetryPhase.value } }
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
        computeResponsiveMetrics(
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          fontScale = fontScale,
        )
      }
      val scrollState = rememberScrollState()

      if (metrics.isSideBySide) {
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
              top = metrics.topPadding,
              bottom = metrics.bottomPadding,
            )
            .testTag("mobile_screen_wrapper"),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          BioSyncTopBar(
            metrics = metrics,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = metrics.minSectionGap),
            horizontalArrangement = Arrangement.spacedBy(metrics.horizontalPadding * 1.5f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier.weight(0.95f),
              contentAlignment = Alignment.Center,
            ) {
              BiometricHeroSection(
                phaseProvider = phaseProvider,
                metrics = metrics,
              )
            }

            Column(
              modifier = Modifier.weight(1.05f),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              MetabolicTelemetryCard(
                phaseProvider = phaseProvider,
                borderFlashProvider = borderFlashProvider,
                metrics = metrics,
              )
              Spacer(modifier = Modifier.height(metrics.minSectionGap))
              GetStartedSection(
                onClick = onGetStartedClick,
                metrics = metrics,
              )
            }
          }
        }
      } else {
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
              top = metrics.topPadding,
              bottom = metrics.bottomPadding,
            )
            .testTag("mobile_screen_wrapper"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // TOP SECTION: Brand Header + Hexagon Dial + Headline/Subtitle
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            BioSyncTopBar(
              metrics = metrics,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            )

            Spacer(modifier = Modifier.height(metrics.topBarToDialGap))

            BiometricHeroSection(
              phaseProvider = phaseProvider,
              metrics = metrics,
            )
          }

          // MIDDLE SECTION: Floating Progress Bar Card with One-Time Border Flash Reflection
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = metrics.minSectionGap),
          ) {
            MetabolicTelemetryCard(
              phaseProvider = phaseProvider,
              borderFlashProvider = borderFlashProvider,
              metrics = metrics,
            )
          }

          // BOTTOM SECTION: Get Started Button + "Encrypted on-device • Private & Secure"
          GetStartedSection(
            onClick = onGetStartedClick,
            metrics = metrics,
          )
        }
      }
    }
  }
}

@Composable
private fun BioSyncTopBar(
  metrics: BioSyncResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val brandLogoDesc = stringResource(R.string.cd_brand_logo)
  Row(
    modifier = modifier.testTag("app_brand_header"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
  ) {
    Canvas(
      modifier = Modifier
        .size(metrics.brandIconSize)
        .semantics { contentDescription = brandLogoDesc },
    ) {
      val sx = size.width / 24f
      val sy = size.height / 24f
      val pulsePath = Path().apply {
        moveTo(22f * sx, 12f * sy)
        lineTo(18f * sx, 12f * sy)
        lineTo(15f * sx, 21f * sy)
        lineTo(9f * sx, 3f * sy)
        lineTo(6f * sx, 12f * sy)
        lineTo(2f * sx, 12f * sy)
      }
      drawPath(
        path = pulsePath,
        color = CyberCyan,
        style = Stroke(
          width = 2.2f * sx,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )
    }
    Spacer(modifier = Modifier.width((8f * metrics.scale).coerceIn(6f, 12f).dp))
    Text(
      text = stringResource(R.string.brand_name),
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.brandFontSize,
      letterSpacing = (-0.35).sp,
    )
  }
}

@Composable
private fun BiometricHeroSection(
  phaseProvider: () -> Float,
  metrics: BioSyncResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val dialDesc = stringResource(R.string.cd_biometric_dial)
  val displayScore by remember(phaseProvider) {
    derivedStateOf {
      interpolateTelemetry(
        phase = phaseProvider(),
        currentValue = 94f,
        maxValue = 100f,
      ).roundToInt().coerceIn(0, 100)
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("biometric_hero"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Responsive Precision Biometric Core Dial
    Box(
      modifier = Modifier
        .size(metrics.dialSize)
        .semantics { contentDescription = dialDesc }
        .testTag("biometric_core_dial"),
      contentAlignment = Alignment.Center,
    ) {
      CyberneticTelemetryDial(
        phaseProvider = phaseProvider,
        modifier = Modifier.fillMaxSize(),
      )

      // Central Telemetry Readout Value: 0 -> 94 on launch, -> 100 on Get Started
      Text(
        text = displayScore.toString(),
        style = TextStyle(
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.scoreFontSize,
          lineHeight = metrics.scoreFontSize,
          letterSpacing = (-1.15).sp,
          color = PureWhite,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(0f, 4f),
            blurRadius = 20f,
          ),
        ),
        modifier = Modifier.testTag("biometric_score_text"),
      )
    }

    Spacer(modifier = Modifier.height(metrics.dialToHeadlineGap))

    // Swiss / Grotesque Editorial Headline & Subtitle
    Column(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.hero_headline),
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.headlineFontSize,
        lineHeight = metrics.headlineLineHeight,
        letterSpacing = (-0.55).sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("hero_headline"),
      )
      Spacer(modifier = Modifier.height(metrics.headlineToSubtitleGap))
      Text(
        text = stringResource(R.string.hero_subtitle),
        color = Neutral400,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = metrics.subtitleFontSize,
        lineHeight = metrics.subtitleLineHeight,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .widthIn(max = metrics.subtitleMaxWidth)
          .testTag("hero_subtitle"),
      )
    }
  }
}

@Composable
private fun CyberneticTelemetryDial(
  phaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "oct_telemetry_transition")

  val primaryDashOffset by infiniteTransition.animateFloat(
    initialValue = 640f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oct_orbit_primary",
  )

  val secondaryDashOffset by infiniteTransition.animateFloat(
    initialValue = -480f,
    targetValue = 480f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 6000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oct_orbit_secondary",
  )

  val microDashOffset by infiniteTransition.animateFloat(
    initialValue = 640f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oct_orbit_micro",
  )

  val spinAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oct_spin_layer",
  )

  val counterSpinAngle by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 16000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oct_counter_layer",
  )

  Spacer(
    modifier = modifier
      .graphicsLayer()
      .drawWithCache {
        val scale = (size.minDimension / 200f).coerceAtLeast(0.1f)
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        fun pt(x: Float, y: Float): Offset = Offset(
          x = centerOffset.x + (x - 100f) * scale,
          y = centerOffset.y + (y - 100f) * scale,
        )

        fun buildOctagon(points: List<Pair<Float, Float>>): Path = Path().apply {
          points.forEachIndexed { index, (x, y) ->
            val p = pt(x, y)
            if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
          }
          close()
        }

        val outerOctagon = buildOctagon(
          listOf(
            70f to 18f, 130f to 18f, 182f to 70f, 182f to 130f,
            130f to 182f, 70f to 182f, 18f to 130f, 18f to 70f,
          ),
        )
        val spinOctagon = buildOctagon(
          listOf(
            72f to 24f, 128f to 24f, 176f to 72f, 176f to 128f,
            128f to 176f, 72f to 176f, 24f to 128f, 24f to 72f,
          ),
        )
        val secondaryOctagon = buildOctagon(
          listOf(
            74f to 28f, 126f to 28f, 172f to 74f, 172f to 126f,
            126f to 172f, 74f to 172f, 28f to 126f, 28f to 74f,
          ),
        )
        val microOctagon = buildOctagon(
          listOf(
            79f to 42f, 121f to 42f, 158f to 79f, 158f to 121f,
            121f to 158f, 79f to 158f, 42f to 121f, 42f to 79f,
          ),
        )
        val innerGuideOctagon = buildOctagon(
          listOf(
            84f to 54f, 116f to 54f, 146f to 84f, 146f to 116f,
            116f to 146f, 84f to 146f, 54f to 116f, 54f to 84f,
          ),
        )
        val topCaliper = Path().apply {
          val p1 = pt(70f, 18f)
          val p2 = pt(130f, 18f)
          val p3 = pt(182f, 70f)
          moveTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
        }
        val bottomCaliper = Path().apply {
          val p1 = pt(130f, 182f)
          val p2 = pt(70f, 182f)
          val p3 = pt(18f, 130f)
          moveTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
        }

        val axisDash = PathEffect.dashPathEffect(floatArrayOf(2f * scale, 4f * scale), 0f)
        val spinCircleDash = PathEffect.dashPathEffect(floatArrayOf(1f * scale, 7f * scale), 0f)
        val spinOctDash = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 12f * scale), 0f)
        val counterCircleDash = PathEffect.dashPathEffect(floatArrayOf(2f * scale, 6f * scale), 0f)

        onDrawBehind {
          val phase = phaseProvider()
          val launchFactor = phase.coerceIn(0f, 1f)
          val fullFillFactor = (phase - 1f).coerceIn(0f, 1f)

          // 1. Precision Orthogonal Crosshair Axis Guides
          val axisColor = Color.White.copy(alpha = 0.07f * launchFactor)
          drawLine(
            color = axisColor,
            start = pt(100f, 10f),
            end = pt(100f, 190f),
            strokeWidth = 1f * scale,
            pathEffect = axisDash,
          )
          drawLine(
            color = axisColor,
            start = pt(10f, 100f),
            end = pt(190f, 100f),
            strokeWidth = 1f * scale,
            pathEffect = axisDash,
          )

          // 2. Corner Calibrated Standoff Marks
          val standoffColor = Color.White.copy(alpha = 0.25f * launchFactor)
          drawLine(standoffColor, pt(40f, 40f), pt(48f, 48f), strokeWidth = 1f * scale)
          drawLine(standoffColor, pt(160f, 40f), pt(152f, 48f), strokeWidth = 1f * scale)
          drawLine(standoffColor, pt(160f, 160f), pt(152f, 152f), strokeWidth = 1f * scale)
          drawLine(standoffColor, pt(40f, 160f), pt(48f, 152f), strokeWidth = 1f * scale)

          // 3. Main Outer Polygonal Chassis Base
          drawPath(
            path = outerOctagon,
            color = Color.Black.copy(alpha = 0.7f),
          )
          drawPath(
            path = outerOctagon,
            color = Color.White.copy(alpha = (0.12f + 0.20f * fullFillFactor) * launchFactor),
            style = Stroke(width = 1f * scale),
          )

          // 4. Rotating Hairline Concentric Dash Ring (.oct-spin-layer)
          rotate(degrees = spinAngle * launchFactor, pivot = centerOffset) {
            drawCircle(
              color = Color.White.copy(alpha = 0.15f * launchFactor),
              radius = 88f * scale,
              center = centerOffset,
              style = Stroke(width = 1f * scale, pathEffect = spinCircleDash),
            )
            drawPath(
              path = spinOctagon,
              color = CyberCyan.copy(alpha = (0.2f + 0.35f * fullFillFactor) * launchFactor),
              style = Stroke(width = 1f * scale, pathEffect = spinOctDash),
            )
          }

          // 5. Outer Crisp Segmented White Framing Caliper Arms (sweep in from 0 on launch)
          val caliperTotalLen = (135f * scale).coerceAtLeast(1f)
          val caliperDrawLen = (caliperTotalLen * launchFactor).coerceAtLeast(0.5f)
          val caliperEffect = if (launchFactor < 0.999f) {
            PathEffect.dashPathEffect(floatArrayOf(caliperDrawLen, caliperTotalLen), 0f)
          } else {
            null
          }
          drawPath(
            path = topCaliper,
            color = PureWhite.copy(alpha = launchFactor),
            style = Stroke(
              width = 3f * scale,
              cap = StrokeCap.Square,
              join = StrokeJoin.Miter,
              pathEffect = caliperEffect,
            ),
          )
          drawPath(
            path = bottomCaliper,
            color = PureWhite.copy(alpha = launchFactor),
            style = Stroke(
              width = 3f * scale,
              cap = StrokeCap.Square,
              join = StrokeJoin.Miter,
              pathEffect = caliperEffect,
            ),
          )

          // 6. Counter-rotating Concentric Telemetry Dial (.oct-counter-layer)
          rotate(degrees = counterSpinAngle * launchFactor, pivot = centerOffset) {
            drawCircle(
              color = Color.White.copy(alpha = 0.1f * launchFactor),
              radius = 70f * scale,
              center = centerOffset,
              style = Stroke(width = 1f * scale, pathEffect = counterCircleDash),
            )
            drawLine(CyberCyan.copy(alpha = launchFactor), pt(100f, 26f), pt(100f, 32f), strokeWidth = 1.5f * scale)
            drawLine(ElectricBlue.copy(alpha = launchFactor), pt(174f, 100f), pt(168f, 100f), strokeWidth = 1.5f * scale)
            drawLine(CyberCyan.copy(alpha = launchFactor), pt(100f, 174f), pt(100f, 168f), strokeWidth = 1.5f * scale)
            drawLine(ElectricBlue.copy(alpha = launchFactor), pt(26f, 100f), pt(32f, 100f), strokeWidth = 1.5f * scale)
          }

          if (launchFactor > 0.005f) {
            // 7. Kinetic Path Layer 1: High Velocity Neon-Cyan Perimeter Tracer
            val primaryOn = interpolateTelemetry(phase, currentValue = 140f, maxValue = 500f).coerceAtLeast(1f) * scale
            val primaryOff = ((180f * (1f - fullFillFactor)).coerceAtLeast(0.1f)) * scale
            drawPath(
              path = outerOctagon,
              color = CyberCyan,
              style = Stroke(
                width = (2.5f + 0.5f * fullFillFactor) * scale,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter,
                pathEffect = PathEffect.dashPathEffect(
                  intervals = floatArrayOf(primaryOn, primaryOff),
                  phase = primaryDashOffset * scale,
                ),
              ),
            )

            // 8. Kinetic Path Layer 2: Intermediate Chamfer Electric-Blue Arc Tracer
            val secondaryOn = interpolateTelemetry(phase, currentValue = 80f, maxValue = 450f).coerceAtLeast(1f) * scale
            val secondaryOff = ((120f * (1f - fullFillFactor)).coerceAtLeast(0.1f)) * scale
            drawPath(
              path = secondaryOctagon,
              color = ElectricBlue,
              style = Stroke(
                width = (2f + 0.4f * fullFillFactor) * scale,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter,
                pathEffect = PathEffect.dashPathEffect(
                  intervals = floatArrayOf(secondaryOn, secondaryOff),
                  phase = secondaryDashOffset * scale,
                ),
              ),
            )

            // 9. Kinetic Path Layer 3: Inner Micro Precision Dashed Sweeper
            val microOn = interpolateTelemetry(phase, currentValue = 20f, maxValue = 380f).coerceAtLeast(1f) * scale
            val microOff = ((50f * (1f - fullFillFactor)).coerceAtLeast(0.1f)) * scale
            drawPath(
              path = microOctagon,
              color = PureWhite,
              style = Stroke(
                width = 1.5f * scale,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter,
                pathEffect = PathEffect.dashPathEffect(
                  intervals = floatArrayOf(microOn, microOff),
                  phase = microDashOffset * scale,
                ),
              ),
            )
          }

          // 10. Internal Structural Polygon Guide
          drawPath(
            path = innerGuideOctagon,
            color = Color.White.copy(alpha = (0.08f + 0.22f * fullFillFactor) * launchFactor),
            style = Stroke(width = 1f * scale),
          )

          // 11. Calibrated Cardinal Vertex Notches
          val notchColor = PureWhite.copy(alpha = launchFactor)
          drawLine(notchColor, pt(100f, 15f), pt(100f, 22f), strokeWidth = 2f * scale)
          drawLine(notchColor, pt(100f, 178f), pt(100f, 185f), strokeWidth = 2f * scale)
          drawLine(notchColor, pt(15f, 100f), pt(22f, 100f), strokeWidth = 2f * scale)
          drawLine(notchColor, pt(178f, 100f), pt(185f, 100f), strokeWidth = 2f * scale)

          // 12. Perimeter Synchronized Telemetry Status Pips
          val pipRadius = (2f + 0.6f * fullFillFactor) * scale * launchFactor
          if (pipRadius > 0.1f) {
            drawCircle(CyberCyan, radius = pipRadius, center = pt(70f, 18f))
            drawCircle(CyberCyan, radius = pipRadius, center = pt(130f, 18f))
            drawCircle(CyberCyan, radius = pipRadius, center = pt(182f, 70f))
            drawCircle(PureWhite, radius = pipRadius, center = pt(182f, 130f))
            drawCircle(ElectricBlue, radius = pipRadius, center = pt(130f, 182f))
            drawCircle(PureWhite, radius = pipRadius, center = pt(70f, 182f))
            drawCircle(ElectricBlue, radius = pipRadius, center = pt(18f, 130f))
            drawCircle(CyberCyan, radius = pipRadius, center = pt(18f, 70f))
          }
        }
      },
  )
}

@Composable
private fun MetabolicTelemetryCard(
  phaseProvider: () -> Float,
  borderFlashProvider: () -> Float,
  metrics: BioSyncResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val caloriesGradient = remember {
    listOf(CaloriesStart, CaloriesMid, CaloriesEnd)
  }

  val macroItems = remember {
    listOf(
      MacroMetricItem(
        id = "protein",
        labelRes = R.string.macro_protein,
        currentVal = 148,
        maxVal = 180,
        unitSuffix = "g",
        targetDisplay = "180g",
        useCommaFormat = false,
        currentProgress = 0.82f,
        gradientColors = listOf(ProteinStart, ProteinMid, ProteinEnd),
      ),
      MacroMetricItem(
        id = "carbs",
        labelRes = R.string.macro_carbs,
        currentVal = 195,
        maxVal = 240,
        unitSuffix = "g",
        targetDisplay = "240g",
        useCommaFormat = false,
        currentProgress = 0.81f,
        gradientColors = listOf(CarbsStart, CarbsMid, CarbsEnd),
      ),
      MacroMetricItem(
        id = "fats",
        labelRes = R.string.macro_fats,
        currentVal = 52,
        maxVal = 72,
        unitSuffix = "g",
        targetDisplay = "72g",
        useCommaFormat = false,
        currentProgress = 0.72f,
        gradientColors = listOf(FatsStart, FatsMid, FatsEnd),
      ),
      MacroMetricItem(
        id = "hydration",
        labelRes = R.string.macro_hydration,
        currentVal = 2150,
        maxVal = 3000,
        unitSuffix = " ml",
        targetDisplay = "3,000 ml",
        useCommaFormat = true,
        currentProgress = 0.71f,
        gradientColors = listOf(HydrationStart, HydrationMid, HydrationEnd),
      ),
      MacroMetricItem(
        id = "steps",
        labelRes = R.string.macro_steps,
        currentVal = 8420,
        maxVal = 10000,
        unitSuffix = "",
        targetDisplay = "10,000 steps",
        useCommaFormat = true,
        currentProgress = 0.84f,
        gradientColors = listOf(StepsStart, StepsMid, StepsEnd),
      ),
    )
  }

  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  // Animated Calories values: 0 -> 1,840 (84%) on launch, -> 2,200 (100%) on Get Started
  val animatedCalories by remember(phaseProvider) {
    derivedStateOf {
      interpolateTelemetry(
        phase = phaseProvider(),
        currentValue = 1840f,
        maxValue = 2200f,
      ).roundToInt().coerceIn(0, 2200)
    }
  }
  val animatedCaloriesPercent by remember(phaseProvider) {
    derivedStateOf {
      interpolateTelemetry(
        phase = phaseProvider(),
        currentValue = 84f,
        maxValue = 100f,
      ).roundToInt().coerceIn(0, 100)
    }
  }
  val animatedCaloriesRemaining by remember(phaseProvider) {
    derivedStateOf {
      (2200 - animatedCalories).coerceAtLeast(0)
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 2.dp, vertical = 2.dp)
      .drawWithCache {
        val shadowCorner = metrics.cardCornerRadius.toPx()
        val cornerRadius = CornerRadius(shadowCorner, shadowCorner)
        val ambientPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(72, 26, 42, 68)
            maskFilter = BlurMaskFilter(safeBlurRadius(24.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val rimShadowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(36, 0, 210, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(16.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val contactPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(210, 0, 0, 0)
            maskFilter = BlurMaskFilter(safeBlurRadius(12.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        // Rear flash wave halo paint (for the one-time backlight flash animation on Get Started)
        val rearFlashHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(0, 235, 248, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(22.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val baseBorderBrush = Brush.verticalGradient(
          0.0f to PureWhite.copy(alpha = 0.24f),
          0.25f to Color(0xFF282E3D),
          0.80f to Color(0xFF181B24),
          1.0f to Color(0xFF0F1117),
        )

        onDrawWithContent {
          val flashProgress = borderFlashProvider().coerceIn(0f, 1f)
          val flashEnvelope = if (flashProgress > 0.001f && flashProgress < 0.999f) {
            sin(flashProgress * Math.PI.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }

          // 1. Behind-the-Card Floating Shadows + Traveling Rear Flash Wave
          drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
              4.dp.toPx(),
              10.dp.toPx(),
              size.width - 4.dp.toPx(),
              size.height + 12.dp.toPx(),
              shadowCorner,
              shadowCorner,
              ambientPaint,
            )
            canvas.drawRoundRect(
              10.dp.toPx(),
              12.dp.toPx(),
              size.width - 10.dp.toPx(),
              size.height + 8.dp.toPx(),
              shadowCorner,
              shadowCorner,
              rimShadowPaint,
            )
            canvas.drawRoundRect(
              2.dp.toPx(),
              6.dp.toPx(),
              size.width - 2.dp.toPx(),
              size.height + 6.dp.toPx(),
              shadowCorner,
              shadowCorner,
              contactPaint,
            )

            // One-time rear flash wave moving behind the border when triggered
            if (flashEnvelope > 0.01f) {
              rearFlashHaloPaint.asFrameworkPaint().alpha = (165f * flashEnvelope).roundToInt().coerceIn(0, 200)
              val waveCx = size.width * flashProgress
              val waveCy = size.height * flashProgress
              val haloSpanX = size.width * 0.55f
              val haloSpanY = size.height * 0.55f
              canvas.drawRoundRect(
                (waveCx - haloSpanX).coerceAtLeast(-6.dp.toPx()),
                (waveCy - haloSpanY).coerceAtLeast(-6.dp.toPx()),
                (waveCx + haloSpanX).coerceAtMost(size.width + 6.dp.toPx()),
                (waveCy + haloSpanY).coerceAtMost(size.height + 6.dp.toPx()),
                shadowCorner,
                shadowCorner,
                rearFlashHaloPaint,
              )
            }
          }

          // 2. Card Content
          drawContent()

          // 3. Base Architectural Border (momentarily dims slightly as the flash wave approaches for high contrast)
          val baseBorderAlpha = (1f - 0.35f * flashEnvelope).coerceIn(0.45f, 1f)
          drawRoundRect(
            brush = baseBorderBrush,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            alpha = baseBorderAlpha,
            style = Stroke(width = 1.2.dp.toPx()),
          )

          // 4. One-Time Traveling Border Flash & Specular Reflection Wave
          if (flashEnvelope > 0.01f) {
            val diagW = size.width.coerceAtLeast(1f)
            val diagH = size.height.coerceAtLeast(1f)
            val bandSpanX = diagW * 0.42f
            val bandSpanY = diagH * 0.42f
            val centerX = -bandSpanX + (diagW + bandSpanX * 2f) * flashProgress
            val centerY = -bandSpanY + (diagH + bandSpanY * 2f) * flashProgress

            // Subtle surface reflection wave across the card face
            drawRoundRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  CyberCyan.copy(alpha = 0.05f * flashEnvelope),
                  PureWhite.copy(alpha = 0.11f * flashEnvelope),
                  ElectricBlue.copy(alpha = 0.05f * flashEnvelope),
                  Color.Transparent,
                ),
                start = Offset(centerX - bandSpanX, centerY - bandSpanY),
                end = Offset(centerX + bandSpanX, centerY + bandSpanY),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )

            // Intense traveling border flash reflection all around the perimeter
            drawRoundRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0xFF64748B).copy(alpha = 0.35f * flashEnvelope),
                  CyberCyan.copy(alpha = 0.85f * flashEnvelope),
                  PureWhite.copy(alpha = 1.0f * flashEnvelope),
                  Color(0xFFE2E8F0).copy(alpha = 0.90f * flashEnvelope),
                  ElectricBlue.copy(alpha = 0.70f * flashEnvelope),
                  Color.Transparent,
                ),
                start = Offset(centerX - bandSpanX * 0.85f, centerY - bandSpanY * 0.85f),
                end = Offset(centerX + bandSpanX * 0.85f, centerY + bandSpanY * 0.85f),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = (1.9f + 0.8f * flashEnvelope).dp.toPx()),
            )
          }
        }
      },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(
          brush = Brush.verticalGradient(
            0.0f to Color(0xFF161922),
            0.35f to Color(0xFF0E1016),
            1.0f to Color(0xFF090A0E),
          ),
        )
        .padding(metrics.cardPadding)
        .testTag("daily_metabolism_card"),
    ) {
      // Section Header + Multicolor Calories Progress Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = (12f * metrics.scale).coerceIn(9f, 16f).dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              MulticolorCategoryPip(
                gradientColors = caloriesGradient,
                size = metrics.macroDotSize,
              )
              Spacer(modifier = Modifier.width((7f * metrics.scale).coerceIn(5.5f, 10f).dp))
              Text(
                text = stringResource(R.string.todays_energy_label),
                color = Neutral400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.energyLabelFontSize,
                letterSpacing = 1.1.sp,
              )
            }
            Spacer(modifier = Modifier.height((4f * metrics.scale).coerceIn(2.5f, 6.5f).dp))
            Row(
              verticalAlignment = Alignment.Bottom,
            ) {
              Text(
                text = formatWithCommas(animatedCalories),
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = metrics.energyValueFontSize,
                lineHeight = metrics.energyValueFontSize,
                letterSpacing = (-0.6).sp,
                modifier = Modifier.testTag("energy_current_value"),
              )
              Spacer(modifier = Modifier.width((6f * metrics.scale).coerceIn(4f, 10f).dp))
              Text(
                text = stringResource(R.string.energy_target),
                color = Neutral400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = metrics.energyTargetFontSize,
                modifier = Modifier.padding(bottom = 2.dp),
              )
            }
          }

          Column(
            horizontalAlignment = Alignment.End,
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(PureWhite)
                .padding(
                  horizontal = (8.5f * metrics.scale).coerceIn(6.5f, 12.5f).dp,
                  vertical = (3f * metrics.scale).coerceIn(2f, 4.5f).dp,
                )
                .testTag("energy_target_badge"),
            ) {
              Text(
                text = "${animatedCaloriesPercent}% TARGET",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.badgeFontSize,
                letterSpacing = (-0.25).sp,
              )
            }
            Spacer(modifier = Modifier.height((4f * metrics.scale).coerceIn(2.5f, 6.5f).dp))
            Text(
              text = "${formatWithCommas(animatedCaloriesRemaining)} kcal remaining",
              color = Neutral400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = metrics.remainingFontSize,
            )
          }
        }

        Spacer(modifier = Modifier.height((9.5f * metrics.scale).coerceIn(7.5f, 13f).dp))

        // Multicolor Lit Calories Progress Bar (0 -> 84% on launch, -> 100% on Get Started)
        MulticolorLitProgressBar(
          progressProvider = {
            interpolateTelemetry(
              phase = phaseProvider(),
              currentValue = 0.84f,
              maxValue = 1.0f,
            ).coerceIn(0f, 1f)
          },
          gradientColors = caloriesGradient,
          barHeight = metrics.caloriesBarHeight,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("calories_progress_bar"),
        )
      }

      HorizontalDivider(
        thickness = 1.dp,
        color = Color(0xFF222733),
      )

      // Multicolor Lit Macro Breakdown
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = (12f * metrics.scale).coerceIn(9f, 16f).dp),
        verticalArrangement = Arrangement.spacedBy(metrics.macroRowSpacing),
      ) {
        macroItems.forEach { item ->
          MacroMetricRow(
            item = item,
            phaseProvider = phaseProvider,
            metrics = metrics,
          )
        }
      }
    }
  }
}

@Composable
private fun MacroMetricRow(
  item: MacroMetricItem,
  phaseProvider: () -> Float,
  metrics: BioSyncResponsiveMetrics,
) {
  val animatedAmount by remember(item, phaseProvider) {
    derivedStateOf {
      interpolateTelemetry(
        phase = phaseProvider(),
        currentValue = item.currentVal.toFloat(),
        maxValue = item.maxVal.toFloat(),
      ).roundToInt().coerceIn(0, item.maxVal)
    }
  }
  val animatedPct by remember(item, phaseProvider) {
    derivedStateOf {
      interpolateTelemetry(
        phase = phaseProvider(),
        currentValue = item.currentProgress * 100f,
        maxValue = 100f,
      ).roundToInt().coerceIn(0, 100)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("macro_row_${item.id}"),
    verticalArrangement = Arrangement.spacedBy((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false),
      ) {
        MulticolorCategoryPip(
          gradientColors = item.gradientColors,
          size = metrics.macroDotSize,
        )
        Spacer(modifier = Modifier.width((8f * metrics.scale).coerceIn(6f, 11.5f).dp))
        Text(
          text = stringResource(item.labelRes),
          color = Neutral200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.macroFontSize,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      val formattedCurrent = if (item.useCommaFormat) {
        "${formatWithCommas(animatedAmount)}${item.unitSuffix}"
      } else {
        "${animatedAmount}${item.unitSuffix}"
      }

      Text(
        text = buildAnnotatedString {
          withStyle(
            SpanStyle(
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.macroFontSize,
            ),
          ) {
            append(formattedCurrent)
          }
          withStyle(
            SpanStyle(
              color = Neutral400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = metrics.macroFontSize,
            ),
          ) {
            append(" / ${item.targetDisplay}  ")
            append("(${animatedPct}%)")
          }
        },
      )
    }

    // Zero-recomposition Multicolor Progress Bar (reads progress directly in Draw phase)
    MulticolorLitProgressBar(
      progressProvider = {
        interpolateTelemetry(
          phase = phaseProvider(),
          currentValue = item.currentProgress,
          maxValue = 1.0f,
        ).coerceIn(0f, 1f)
      },
      gradientColors = item.gradientColors,
      barHeight = metrics.macroBarHeight,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun MulticolorCategoryPip(
  gradientColors: List<Color>,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier.size(size),
  ) {
    val radius = this.size.minDimension * 0.34f
    drawRoundRect(
      brush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset.Zero,
        end = Offset(this.size.width.coerceAtLeast(1f), this.size.height.coerceAtLeast(1f)),
      ),
      cornerRadius = CornerRadius(radius, radius),
    )
  }
}

@Composable
private fun MulticolorLitProgressBar(
  progressProvider: () -> Float,
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
            gradientColors.first().copy(alpha = 0.22f),
            ObsidianTrackBorder,
            gradientColors.last().copy(alpha = 0.20f),
          ),
        )
        val verticalLightingBrush = Brush.verticalGradient(
          0.0f to PureWhite.copy(alpha = 0.24f),
          0.42f to Color.Transparent,
          1.0f to ObsidianBlack.copy(alpha = 0.18f),
          startY = 0f,
          endY = trackHeight,
        )
        val topRimBrush = Brush.verticalGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.30f),
            Color.Transparent,
          ),
          startY = 0f,
          endY = (trackHeight * 0.65f).coerceAtLeast(1f),
        )
        val fillPath = Path()

        onDrawBehind {
          if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
          val progress = progressProvider().coerceIn(0f, 1f)

          // 1. Subtle Recessed Capsule Track with Category-Tinted Hairline Rim
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

            // 2. Smooth Multicolor Horizontal Gradient Fill
            drawPath(
              path = fillPath,
              brush = Brush.horizontalGradient(
                colors = gradientColors,
                startX = 0f,
                endX = fillWidth.coerceAtLeast(1f),
              ),
            )

            // 3. Category-Tuned Dimensional Surface Lighting
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
                    PureWhite.copy(alpha = 0.14f),
                    Color.Transparent,
                  ),
                  start = Offset(sheenCenter - sheenSpan, 0f),
                  end = Offset(sheenCenter + sheenSpan, trackHeight),
                ),
                topLeft = Offset.Zero,
                size = Size(fillWidth, trackHeight),
              )
            }

            // 4. Crisp Inner Capsule Rim
            drawRoundRect(
              brush = topRimBrush,
              topLeft = Offset(0.5f, 0.5f),
              size = Size((fillWidth - 1f).coerceAtLeast(1f), (trackHeight - 1f).coerceAtLeast(1f)),
              cornerRadius = cornerRadius,
              style = Stroke(width = 0.8.dp.toPx()),
            )
          }
        }
      },
  )
}

@Composable
private fun GetStartedSection(
  onClick: () -> Unit,
  metrics: BioSyncResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    GetStartedButton(
      onClick = onClick,
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height(metrics.ctaToFooterGap))

    Text(
      text = stringResource(R.string.calib_footer_encrypted),
      color = Color(0xFF71717A),
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = metrics.footerFontSize,
      letterSpacing = 0.2.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("welcome_footer_encrypted"),
    )
  }
}

@Composable
private fun GetStartedButton(
  onClick: () -> Unit,
  metrics: BioSyncResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val arrowDesc = stringResource(R.string.cd_arrow_forward)
  val buttonShape = RoundedCornerShape(metrics.cardCornerRadius)

  val infiniteTransition = rememberInfiniteTransition(label = "get_started_glow_transition")
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "get_started_shimmer_phase",
  )
  val pulseProvider = remember { { glowPulse } }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .drawWithCache {
        val cornerPx = metrics.cardCornerRadius.toPx()
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
            color = android.graphics.Color.argb(135, 225, 238, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(7.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val baseMetallicBrush = Brush.verticalGradient(
          0.0f to PureWhite,
          0.55f to Color(0xFFF3F5F8),
          1.0f to Color(0xFFE2E7F0),
        )
        val pressedBrush = Brush.verticalGradient(
          0.0f to Color(0xFFDCE1EA),
          1.0f to Color(0xFFC8D0DC),
        )

        onDrawBehind {
          if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
          val phase = pulseProvider()
          val breath = sin(phase * Math.PI.toFloat())
          outerAuraPaint.asFrameworkPaint().alpha = (60 + (55 * breath).roundToInt()).coerceIn(40, 130)

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

          // 3. Sweeping Specular Liquid-Silver Shimmer Band
          clipPath(buttonClipPath) {
            val sweepSpan = (size.width * 0.38f).coerceAtLeast(1f)
            val sweepCenter = -sweepSpan + (size.width + sweepSpan * 2f) * phase
            drawRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  PureWhite.copy(alpha = 0.75f),
                  Color(0xFFD8E4F8).copy(alpha = 0.35f),
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
        onClick = onClick,
      )
      .padding(vertical = metrics.ctaVerticalPadding, horizontal = 24.dp)
      .testTag("cta_activate_button"),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(R.string.cta_get_started),
        color = ObsidianBlack,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.ctaFontSize,
        letterSpacing = (-0.25).sp,
      )
      Spacer(modifier = Modifier.width((8f * metrics.scale).coerceIn(6f, 12f).dp))
      Canvas(
        modifier = Modifier
          .size(metrics.ctaIconSize)
          .semantics { contentDescription = arrowDesc },
      ) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val arrowPath = Path().apply {
          moveTo(5f * sx, 12f * sy)
          lineTo(19f * sx, 12f * sy)
          moveTo(13f * sx, 6f * sy)
          lineTo(19f * sx, 12f * sy)
          lineTo(13f * sx, 18f * sy)
        }
        drawPath(
          path = arrowPath,
          color = ObsidianBlack,
          style = Stroke(
            width = 2.5f * sx,
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter,
          ),
        )
      }
    }
  }
}

@Preview(name = "Phone Portrait", showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, heightDp = 844)
@Preview(name = "Small Compact Phone", showBackground = true, backgroundColor = 0xFF000000, widthDp = 320, heightDp = 568)
@Preview(name = "Tablet / Foldable Wide", showBackground = true, backgroundColor = 0xFF000000, widthDp = 840, heightDp = 560)
@Composable
fun BioSyncWelcomeScreenPreview() {
  MyApplicationTheme {
    BioSyncWelcomeScreen()
  }
}
