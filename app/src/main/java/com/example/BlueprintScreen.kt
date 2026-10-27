package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

// ─── Architectural Palette (Obsidian & Monochrome matching Steps 01 & 02) ────
private val Zinc100 = Color(0xFFF4F4F5)
private val Zinc200 = Color(0xFFE4E4E7)
private val Zinc300 = Color(0xFFD4D4D8)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc500 = Color(0xFF71717A)
private val Zinc600 = Color(0xFF52525B)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc800 = Color(0xFF27272A)
private val Zinc850 = Color(0xFF1C1C1F)
private val Zinc900 = Color(0xFF141417)
private val Zinc950 = Color(0xFF0B0B0E)
private val CardBgTop = Color(0xFF141417)
private val CardBgBottom = Color(0xFF0A0A0C)

private val BlueprintEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private fun safeBlurRadius(radius: Float): Float = radius.coerceAtLeast(0.5f)

/**
 * Section 1 — Activity Level options (Single-select).
 */
enum class ActivityLevelOption(
  val index: Int,
  val tierCode: String,
  val cadenceBadge: String,
  val activeBars: Int,
  val intensityFraction: Float,
) {
  SEDENTARY(
    index = 1,
    tierCode = "TIER 01 • SEDENTARY",
    cadenceBadge = "DESK / LOW",
    activeBars = 1,
    intensityFraction = 0.25f,
  ),
  LIGHT(
    index = 2,
    tierCode = "TIER 02 • LIGHT",
    cadenceBadge = "1–3 DAYS/WK",
    activeBars = 2,
    intensityFraction = 0.50f,
  ),
  MODERATE(
    index = 3,
    tierCode = "TIER 03 • MODERATE",
    cadenceBadge = "3–5 DAYS/WK",
    activeBars = 3,
    intensityFraction = 0.75f,
  ),
  VERY_ACTIVE(
    index = 4,
    tierCode = "TIER 04 • VERY ACTIVE",
    cadenceBadge = "6–7 DAYS/WK",
    activeBars = 4,
    intensityFraction = 1.00f,
  ),
}

/**
 * Section 2 — Primary Metabolic Objective options (Single-select).
 */
enum class BlueprintObjectiveOption(
  val index: Int,
  val codeLabel: String,
  val vectorBadge: String,
  val protocolSummary: String,
) {
  LOSE_FAT(
    index = 1,
    codeLabel = "01",
    vectorBadge = "DEFICIT VECTOR",
    protocolSummary = "Prioritizes adipose mobilization while safeguarding lean tissue.",
  ),
  MAINTAIN(
    index = 2,
    codeLabel = "02",
    vectorBadge = "EQUILIBRIUM",
    protocolSummary = "Sustains baseline mass with balanced energy and recovery.",
  ),
  BUILD_MUSCLE(
    index = 3,
    codeLabel = "03",
    vectorBadge = "SURPLUS VECTOR",
    protocolSummary = "Drives lean tissue accretion and progressive performance.",
  ),
}

/**
 * Section 3 — Daily Streak Completion Threshold options (Single-select: 100%, 95%, 90%, 85%, 80%).
 */
enum class StreakThresholdOption(
  val percentage: Int,
  val tierTitle: String,
  val tierDescriptor: String,
) {
  PERCENT_100(
    percentage = 100,
    tierTitle = "ABSOLUTE PRECISION",
    tierDescriptor = "Requires 100% completion across daily macro, hydration & step targets to earn +1 Streak.",
  ),
  PERCENT_95(
    percentage = 95,
    tierTitle = "ELITE DISCIPLINE",
    tierDescriptor = "Requires at least 95% of overall daily goals completed to count as +1 Streak.",
  ),
  PERCENT_90(
    percentage = 90,
    tierTitle = "OPTIMAL CONSISTENCY",
    tierDescriptor = "Requires at least 90% of overall daily goals completed to count as +1 Streak.",
  ),
  PERCENT_85(
    percentage = 85,
    tierTitle = "HIGH MOMENTUM",
    tierDescriptor = "Requires at least 85% of overall daily goals completed to count as +1 Streak.",
  ),
  PERCENT_80(
    percentage = 80,
    tierTitle = "SUSTAINABLE RHYTHM",
    tierDescriptor = "Requires at least 80% of overall daily goals completed to count as +1 Streak.",
  ),
  ;

  companion object {
    fun fromPercent(percent: Int): StreakThresholdOption =
      entries.firstOrNull { it.percentage == percent } ?: PERCENT_90
  }
}

@Immutable
private data class BlueprintResponsiveMetrics(
  val scale: Float,
  val fontCompensation: Float,
  val isExpanded: Boolean,
  val contentMaxWidth: Dp,
  val horizontalPadding: Dp,
  val topContentPadding: Dp,
  val bottomContentPadding: Dp,
  val headerHeight: Dp,
  val titleFontSize: TextUnit,
  val subtitleFontSize: TextUnit,
  val subtitleLineHeight: TextUnit,
  val titleToFirstFieldGap: Dp,
  val sectionGap: Dp,
  val cardCornerRadius: Dp,
  val cardPadding: Dp,
  val ctaHeight: Dp,
  val cardToCtaGap: Dp,
) {
  fun spScaled(base: Float, minVal: Float, maxVal: Float): TextUnit =
    ((base * scale).coerceIn(minVal, maxVal) * fontCompensation).sp
}

private fun computeBlueprintMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): BlueprintResponsiveMetrics {
  val w = maxWidth.value
  val h = maxHeight.value
  val isExpanded = w >= 600f
  val widthFactor = (w / 392f).coerceIn(0.84f, 1.18f)
  val heightFactor = (h / 830f).coerceIn(0.76f, 1.12f)
  val scale = min(widthFactor, heightFactor * 1.05f).coerceIn(0.80f, 1.14f)
  val fontCompensation = if (fontScale > 1.12f) (1.12f / fontScale).coerceIn(0.76f, 1f) else 1f
  val sp: (Float, Float, Float) -> TextUnit = { base, minV, maxV ->
    ((base * scale).coerceIn(minV, maxV) * fontCompensation).sp
  }

  return BlueprintResponsiveMetrics(
    scale = scale,
    fontCompensation = fontCompensation,
    isExpanded = isExpanded,
    contentMaxWidth = if (isExpanded) 560.dp else 480.dp,
    horizontalPadding = (20f * widthFactor).coerceIn(16f, 28f).dp,
    topContentPadding = (8f * heightFactor).coerceIn(5f, 14f).dp,
    bottomContentPadding = (16f * heightFactor).coerceIn(12f, 24f).dp,
    headerHeight = (46f * scale).coerceIn(42f, 54f).dp,
    titleFontSize = sp(26f, 22f, 30f),
    subtitleFontSize = sp(12.8f, 11.2f, 14.5f),
    subtitleLineHeight = sp(18.5f, 16f, 21f),
    titleToFirstFieldGap = (15f * heightFactor).coerceIn(10f, 22f).dp,
    sectionGap = (16f * heightFactor).coerceIn(12f, 22f).dp,
    cardCornerRadius = (13f * scale).coerceIn(11f, 16f).dp,
    cardPadding = (14.5f * scale).coerceIn(12f, 18f).dp,
    ctaHeight = (50f * scale).coerceIn(48f, 56f).dp,
    cardToCtaGap = (16f * heightFactor).coerceIn(12f, 22f).dp,
  )
}

@Composable
fun BlueprintScreen(
  subjectName: String,
  selectedActivityLevel: ActivityLevelOption,
  onSelectActivityLevel: (ActivityLevelOption) -> Unit,
  selectedObjective: BlueprintObjectiveOption,
  onSelectObjective: (BlueprintObjectiveOption) -> Unit,
  selectedStreakThreshold: StreakThresholdOption,
  onSelectStreakThreshold: (StreakThresholdOption) -> Unit,
  onNavigateBack: () -> Unit,
  onCompleteBlueprint: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  val haptics = rememberBioSyncHaptics()
  val coroutineScope = rememberCoroutineScope()
  val headerStepPhase = remember { Animatable(0f) }
  val borderFlashPhase = remember { Animatable(0f) }

  // Top STEP 03 OF 03 progress bar: animates from Step 02 (0.68f) to Step 03 (1.00f) on entry
  LaunchedEffect(Unit) {
    headerStepPhase.snapTo(0f)
    headerStepPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 740, easing = BlueprintEaseOut),
    )
  }

  val onContinueClick: () -> Unit = {
    haptics.confirm()
    coroutineScope.launch {
      borderFlashPhase.snapTo(0f)
      borderFlashPhase.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 680, easing = BlueprintEaseOut),
      )
      borderFlashPhase.snapTo(0f)
      onCompleteBlueprint()
    }
  }

  val headerStepPhaseProvider = remember(headerStepPhase) { { headerStepPhase.value } }
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
        computeBlueprintMetrics(
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          fontScale = fontScale,
        )
      }
      val scrollState = rememberScrollState()

      LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
      }

      Column(
        modifier = Modifier
          .widthIn(max = metrics.contentMaxWidth)
          .fillMaxWidth()
          .verticalScroll(scrollState)
          .heightIn(min = viewportHeight)
          .padding(
            start = metrics.horizontalPadding,
            end = metrics.horizontalPadding,
            top = (6f * metrics.scale).coerceIn(4f, 10f).dp,
            bottom = metrics.bottomContentPadding,
          )
          .testTag("blueprint_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Header Bar (BioSync + STEP 03 OF 03 + Full Shiny Glowy White Step Bar)
        BlueprintHeaderBar(
          onBackClick = {
            haptics.selection()
            onNavigateBack()
          },
          phaseProvider = headerStepPhaseProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // 1. Top Eyebrow, Title & Description under Blueprint
        Column(
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.blueprint_eyebrow),
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
            letterSpacing = 1.6.sp,
          )
          Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
          Text(
            text = stringResource(R.string.blueprint_title),
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.titleFontSize,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.testTag("blueprint_title_text"),
          )
          Spacer(modifier = Modifier.height((5f * metrics.scale).coerceIn(3.5f, 7.5f).dp))
          Text(
            text = stringResource(R.string.blueprint_subtitle),
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.subtitleFontSize,
            lineHeight = metrics.subtitleLineHeight,
            modifier = Modifier.testTag("blueprint_subtitle_text"),
          )
        }

        Spacer(modifier = Modifier.height(metrics.titleToFirstFieldGap))

        // 2. Section 1: Activity Level (Single-select from 4 options)
        ActivityLevelSection(
          selectedLevel = selectedActivityLevel,
          onSelectLevel = { level ->
            if (level != selectedActivityLevel) {
              haptics.selection()
              onSelectActivityLevel(level)
            } else {
              haptics.tick()
            }
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 3. Section 2: Objective (Single-select from 3 options: Lose Fat, Maintain, Build Muscle)
        ObjectiveSection(
          selectedObjective = selectedObjective,
          onSelectObjective = { objective ->
            if (objective != selectedObjective) {
              haptics.selection()
              onSelectObjective(objective)
            } else {
              haptics.tick()
            }
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. Section 3: Daily Streak Completion Threshold (100% / 95% / 90% / 85% / 80%)
        StreakThresholdSection(
          selectedThreshold = selectedStreakThreshold,
          onSelectThreshold = { threshold ->
            if (threshold != selectedStreakThreshold) {
              haptics.selection()
              onSelectStreakThreshold(threshold)
            } else {
              haptics.tick()
            }
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 5. Live Blueprint Protocol Matrix Summary Card
        BlueprintSummaryMatrixCard(
          subjectName = subjectName,
          selectedActivityLevel = selectedActivityLevel,
          selectedObjective = selectedObjective,
          selectedStreakThreshold = selectedStreakThreshold,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.cardToCtaGap))

        // 6. Primary Complete Blueprint Button & Encrypted Footer
        BlueprintContinueSection(
          onContinueClick = onContinueClick,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )
      }
    }
  }
}

// ─── Header Bar & Step 03 Glow Bar ──────────────────────────────────────────

@Composable
private fun BlueprintHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val backDesc = stringResource(R.string.cd_back_to_personal_details)
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
        .testTag("blueprint_back_button"),
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

    // Center Title + STEP 03 OF 03 + Shiny Glowy White Step Progress Bar
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
        text = stringResource(R.string.blueprint_header_step),
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10.2f, 9f, 12f),
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp))

      StepThreeShinyGlowBar(
        phaseProvider = phaseProvider,
        modifier = Modifier
          .width((108f * metrics.scale).coerceIn(92f, 136f).dp)
          .height((4f * metrics.scale).coerceIn(3.5f, 5f).dp)
          .testTag("step_03_glow_bar"),
      )
    }

    // Right Luminous Status Dot (PureWhite matching BioSync header title)
    Box(
      modifier = Modifier.size(48.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      BlueprintLuminousWhiteDot(
        size = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
      )
    }
  }
}

@Composable
private fun BlueprintLuminousWhiteDot(
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
            color = android.graphics.Color.argb(145, 255, 255, 255)
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
 * Shiny, glowing pure-white step indicator bar directly below "STEP 03 OF 03".
 * Smoothly animates from Step 02 (0.68f) to Step 03 (1.00f — full bar!) when entering this page.
 */
@Composable
private fun StepThreeShinyGlowBar(
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
            color = android.graphics.Color.argb(155, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(barH * 1.9f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val innerHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(220, 245, 245, 250)
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
          // Animates from Step 2 (0.68f) to Step 3 (1.00f) on page transition
          val activeFraction = 0.68f + (1.00f - 0.68f) * p

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

            // Subtle step divider ticks at 01 and 02
            drawLine(
              color = ObsidianBlack.copy(alpha = 0.32f),
              start = Offset(step1X, 0f),
              end = Offset(step1X, barH),
              strokeWidth = 0.9.dp.toPx(),
            )
            drawLine(
              color = ObsidianBlack.copy(alpha = 0.32f),
              start = Offset(step2X, 0f),
              end = Offset(step2X, barH),
              strokeWidth = 0.9.dp.toPx(),
            )
          }
        }
      },
  )
}

// ─── Shared Section Header ──────────────────────────────────────────────────

@Composable
private fun BlueprintSectionHeaderRow(
  label: String,
  statusPillText: String,
  metrics: BlueprintResponsiveMetrics,
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
      Canvas(modifier = Modifier.size((6f * metrics.scale).coerceIn(5f, 7.5f).dp)) {
        drawCircle(
          color = PureWhite,
          radius = size.minDimension / 2f,
        )
      }
      Text(
        text = label,
        color = Zinc100,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(13.5f, 12f, 15.5f),
        letterSpacing = (-0.15).sp,
      )
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Zinc900)
        .border(0.8.dp, Zinc800, RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = statusPillText,
        color = Zinc300,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(9.4f, 8.2f, 10.8f),
        letterSpacing = 0.8.sp,
      )
    }
  }
}

// ─── Section 1: Activity Level (Single-select from 4 options) ───────────────

@Composable
private fun ActivityLevelSection(
  selectedLevel: ActivityLevelOption,
  onSelectLevel: (ActivityLevelOption) -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedIntensity by animateFloatAsState(
    targetValue = selectedLevel.intensityFraction,
    animationSpec = tween(durationMillis = 380, easing = BlueprintEaseOut),
    label = "activity_intensity_bar",
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_activity_section_label),
      statusPillText = selectedLevel.tierCode,
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height((7f * metrics.scale).coerceIn(5.5f, 10f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(CardBgTop, CardBgBottom),
          )
          val borderBrush = Brush.verticalGradient(
            0.0f to PureWhite.copy(alpha = 0.20f),
            0.35f to Zinc700.copy(alpha = 0.65f),
            1.0f to Zinc850,
          )
          onDrawBehind {
            drawRoundRect(
              brush = bgBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              brush = borderBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 1.dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding)
        .testTag("blueprint_activity_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 10.5f).dp),
      ) {
        // Top Kinetic Output Spectrum Bar inside the Activity card
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = (2f * metrics.scale).dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "KINETIC OUTPUT SPECTRUM",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(9.4f, 8.2f, 10.8f),
            letterSpacing = 1.0.sp,
          )
          Text(
            text = "0${selectedLevel.index} / 04 SELECTED",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(9.6f, 8.5f, 11f),
            letterSpacing = 0.7.sp,
          )
        }

        // Animated 4-segment kinetic bar
        KineticSpectrumGauge(
          progress = animatedIntensity,
          activeSegments = selectedLevel.activeBars,
          modifier = Modifier
            .fillMaxWidth()
            .height((5.5f * metrics.scale).coerceIn(4.5f, 7f).dp),
        )

        Spacer(modifier = Modifier.height((2f * metrics.scale).dp))

        // 4 Single-Select Activity Level Option Cards
        ActivityOptionRow(
          option = ActivityLevelOption.SEDENTARY,
          title = stringResource(R.string.blueprint_activity_sedentary_title),
          subtitle = stringResource(R.string.blueprint_activity_sedentary_sub),
          isSelected = selectedLevel == ActivityLevelOption.SEDENTARY,
          onClick = { onSelectLevel(ActivityLevelOption.SEDENTARY) },
          metrics = metrics,
          testTag = "activity_option_sedentary",
        )

        ActivityOptionRow(
          option = ActivityLevelOption.LIGHT,
          title = stringResource(R.string.blueprint_activity_light_title),
          subtitle = stringResource(R.string.blueprint_activity_light_sub),
          isSelected = selectedLevel == ActivityLevelOption.LIGHT,
          onClick = { onSelectLevel(ActivityLevelOption.LIGHT) },
          metrics = metrics,
          testTag = "activity_option_light",
        )

        ActivityOptionRow(
          option = ActivityLevelOption.MODERATE,
          title = stringResource(R.string.blueprint_activity_moderate_title),
          subtitle = stringResource(R.string.blueprint_activity_moderate_sub),
          isSelected = selectedLevel == ActivityLevelOption.MODERATE,
          onClick = { onSelectLevel(ActivityLevelOption.MODERATE) },
          metrics = metrics,
          testTag = "activity_option_moderate",
        )

        ActivityOptionRow(
          option = ActivityLevelOption.VERY_ACTIVE,
          title = stringResource(R.string.blueprint_activity_very_active_title),
          subtitle = stringResource(R.string.blueprint_activity_very_active_sub),
          isSelected = selectedLevel == ActivityLevelOption.VERY_ACTIVE,
          onClick = { onSelectLevel(ActivityLevelOption.VERY_ACTIVE) },
          metrics = metrics,
          testTag = "activity_option_very_active",
        )
      }
    }
  }
}

@Composable
private fun KineticSpectrumGauge(
  progress: Float,
  activeSegments: Int,
  modifier: Modifier = Modifier,
) {
  Spacer(
    modifier = modifier.drawWithCache {
      val glowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(135, 255, 255, 255)
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.height * 1.4f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }
      onDrawBehind {
        if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
        val gap = 6.dp.toPx()
        val segWidth = ((size.width - gap * 3f) / 4f).coerceAtLeast(2f)
        val r = CornerRadius(size.height / 2f, size.height / 2f)

        for (i in 0 until 4) {
          val startX = i * (segWidth + gap)
          val segStartFraction = i * 0.25f
          val localFill = ((progress - segStartFraction) / 0.25f).coerceIn(0f, 1f)

          // Base track segment
          drawRoundRect(
            color = Zinc850,
            topLeft = Offset(startX, 0f),
            size = Size(segWidth, size.height),
            cornerRadius = r,
          )
          drawRoundRect(
            color = if (i < activeSegments) Zinc600 else Zinc800,
            topLeft = Offset(startX, 0f),
            size = Size(segWidth, size.height),
            cornerRadius = r,
            style = Stroke(width = 0.7.dp.toPx()),
          )

          if (localFill > 0.01f) {
            val fillW = (segWidth * localFill).coerceIn(size.height.coerceAtMost(segWidth), segWidth)
            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                startX,
                0f,
                startX + fillW,
                size.height,
                r.x,
                r.y,
                glowPaint,
              )
            }
            drawRoundRect(
              brush = Brush.horizontalGradient(
                colors = listOf(Zinc200, PureWhite),
                startX = startX,
                endX = startX + fillW,
              ),
              topLeft = Offset(startX, 0f),
              size = Size(fillW, size.height),
              cornerRadius = r,
            )
          }
        }
      }
    },
  )
}

@Composable
private fun ActivityOptionRow(
  option: ActivityLevelOption,
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  testTag: String,
) {
  val rowCorner = (10f * metrics.scale).coerceIn(8.5f, 12.5f).dp
  val rowShape = RoundedCornerShape(rowCorner)

  val selectionProgress by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "activity_row_select_${option.name}",
  )

  val titleColor by animateColorAsState(
    targetValue = if (isSelected) PureWhite else Zinc200,
    animationSpec = tween(durationMillis = 220),
    label = "activity_title_color_${option.name}",
  )
  val subtitleColor by animateColorAsState(
    targetValue = if (isSelected) Zinc300 else Zinc500,
    animationSpec = tween(durationMillis = 220),
    label = "activity_sub_color_${option.name}",
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = (58f * metrics.scale).coerceIn(52f, 68f).dp)
      .clip(rowShape)
      .drawWithCache {
        val cornerPx = rowCorner.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val activeGlowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(65, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(10.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        onDrawBehind {
          val p = selectionProgress.coerceIn(0f, 1f)
          // Row surface
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF18181D).copy(alpha = 0.55f + 0.45f * p),
                Color(0xFF0E0E12).copy(alpha = 0.70f + 0.30f * p),
              ),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
          )

          if (p > 0.02f) {
            activeGlowPaint.asFrameworkPaint().alpha = (55 * p).roundToInt().coerceIn(0, 75)
            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerPx,
                cornerPx,
                activeGlowPaint,
              )
            }
          }

          // Border
          drawRoundRect(
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.08f + 0.68f * p),
              1.0f to Zinc700.copy(alpha = 0.45f + 0.40f * p),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = (0.9f + 0.45f * p).dp.toPx()),
          )

          // Left vertical luminous accent bar when selected
          if (p > 0.01f) {
            val barH = size.height * (0.46f * p)
            val barW = 3.dp.toPx()
            drawRoundRect(
              color = PureWhite.copy(alpha = p),
              topLeft = Offset(0f, (size.height - barH) / 2f),
              size = Size(barW, barH),
              cornerRadius = CornerRadius(barW, barW),
            )
          }
        }
      }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics { selected = isSelected }
      .padding(
        horizontal = (12f * metrics.scale).coerceIn(10f, 15f).dp,
        vertical = (10f * metrics.scale).coerceIn(8f, 13f).dp,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((11f * metrics.scale).coerceIn(9f, 14f).dp),
      ) {
        // Left Architectural Signal Box (4 ascending bars showing the activity level)
        Box(
          modifier = Modifier
            .size((38f * metrics.scale).coerceIn(34f, 44f).dp)
            .clip(RoundedCornerShape((9f * metrics.scale).coerceIn(7.5f, 11f).dp))
            .background(if (isSelected) PureWhite else Zinc900)
            .border(
              width = 1.dp,
              color = if (isSelected) PureWhite else Zinc700.copy(alpha = 0.7f),
              shape = RoundedCornerShape((9f * metrics.scale).coerceIn(7.5f, 11f).dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(
            modifier = Modifier.size((18f * metrics.scale).coerceIn(15f, 21f).dp),
          ) {
            val w = size.width
            val h = size.height
            val barCount = 4
            val gap = w * 0.10f
            val barW = (w - gap * (barCount - 1)) / barCount
            val heights = floatArrayOf(0.34f, 0.54f, 0.76f, 1.0f)

            for (i in 0 until barCount) {
              val isBarLit = (i + 1) <= option.activeBars
              val barH = h * heights[i]
              val x = i * (barW + gap)
              val y = h - barH
              val barColor = when {
                isSelected && isBarLit -> ObsidianBlack
                isSelected && !isBarLit -> ObsidianBlack.copy(alpha = 0.22f)
                !isSelected && isBarLit -> PureWhite
                else -> Zinc700
              }
              drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
              )
            }
          }
        }

        // Title + Subtitle ("Sedentary" / "Work, Little exercise", etc.)
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = title,
              color = titleColor,
              fontFamily = InterFontFamily,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
              fontSize = metrics.spScaled(14.5f, 13f, 16.5f),
              letterSpacing = (-0.2).sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // Cadence pill badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) PureWhite.copy(alpha = 0.14f) else Zinc900)
                .border(
                  width = 0.6.dp,
                  color = if (isSelected) PureWhite.copy(alpha = 0.35f) else Zinc800,
                  shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 5.5.dp, vertical = 1.5.dp),
            ) {
              Text(
                text = "0${option.index}",
                color = if (isSelected) PureWhite else Zinc500,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(8.6f, 7.6f, 10f),
                letterSpacing = 0.5.sp,
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = subtitle,
            color = subtitleColor,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(12f, 10.6f, 13.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right Precision Radio Indicator
      BlueprintRadioLockIndicator(
        selectionProgress = selectionProgress,
        size = (20f * metrics.scale).coerceIn(18f, 23f).dp,
      )
    }
  }
}

@Composable
private fun BlueprintRadioLockIndicator(
  selectionProgress: Float,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val r = this.size.minDimension / 2f
    val p = selectionProgress.coerceIn(0f, 1f)

    // Outer ring
    drawCircle(
      color = if (p > 0.5f) PureWhite else Zinc600,
      radius = r - 1.dp.toPx(),
      style = Stroke(width = (1.3f + 0.4f * p).dp.toPx()),
    )

    if (p > 0.02f) {
      // Inner filled precision core
      drawCircle(
        color = PureWhite.copy(alpha = p),
        radius = (r * 0.54f) * p,
        center = center,
      )
    }
  }
}

// ─── Section 2: Objective (Single-select from 3 options) ────────────────────

@Composable
private fun ObjectiveSection(
  selectedObjective: BlueprintObjectiveOption,
  onSelectObjective: (BlueprintObjectiveOption) -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_objective_section_label),
      statusPillText = selectedObjective.vectorBadge,
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height((7f * metrics.scale).coerceIn(5.5f, 10f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(CardBgTop, CardBgBottom),
          )
          val borderBrush = Brush.verticalGradient(
            0.0f to PureWhite.copy(alpha = 0.20f),
            0.35f to Zinc700.copy(alpha = 0.65f),
            1.0f to Zinc850,
          )
          onDrawBehind {
            drawRoundRect(
              brush = bgBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              brush = borderBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 1.dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding)
        .testTag("blueprint_objective_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((10f * metrics.scale).coerceIn(8f, 13f).dp),
      ) {
        // 3 Sculpted Objective Cards side-by-side: 1) Lose Fat, 2) Maintain, 3) Build Muscle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 11f).dp),
        ) {
          ObjectiveTileCard(
            option = BlueprintObjectiveOption.LOSE_FAT,
            title = stringResource(R.string.blueprint_objective_lose_fat_title),
            subtitle = stringResource(R.string.blueprint_objective_lose_fat_sub),
            isSelected = selectedObjective == BlueprintObjectiveOption.LOSE_FAT,
            onClick = { onSelectObjective(BlueprintObjectiveOption.LOSE_FAT) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "objective_option_lose_fat",
          )

          ObjectiveTileCard(
            option = BlueprintObjectiveOption.MAINTAIN,
            title = stringResource(R.string.blueprint_objective_maintain_title),
            subtitle = stringResource(R.string.blueprint_objective_maintain_sub),
            isSelected = selectedObjective == BlueprintObjectiveOption.MAINTAIN,
            onClick = { onSelectObjective(BlueprintObjectiveOption.MAINTAIN) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "objective_option_maintain",
          )

          ObjectiveTileCard(
            option = BlueprintObjectiveOption.BUILD_MUSCLE,
            title = stringResource(R.string.blueprint_objective_build_muscle_title),
            subtitle = stringResource(R.string.blueprint_objective_build_muscle_sub),
            isSelected = selectedObjective == BlueprintObjectiveOption.BUILD_MUSCLE,
            onClick = { onSelectObjective(BlueprintObjectiveOption.BUILD_MUSCLE) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "objective_option_build_muscle",
          )
        }

        // Live Selected Objective Telemetry Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp))
            .background(Zinc950)
            .border(
              width = 0.8.dp,
              color = Zinc800,
              shape = RoundedCornerShape((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp),
            )
            .padding(
              horizontal = (11f * metrics.scale).coerceIn(9f, 14f).dp,
              vertical = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(PureWhite)
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(
              text = "ACTIVE",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
              letterSpacing = 0.6.sp,
            )
          }

          Text(
            text = selectedObjective.protocolSummary,
            color = Zinc300,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.spScaled(11.4f, 10f, 12.8f),
            lineHeight = metrics.spScaled(15.5f, 13.8f, 17.5f),
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun ObjectiveTileCard(
  option: BlueprintObjectiveOption,
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
  testTag: String,
) {
  val tileCorner = (11f * metrics.scale).coerceIn(9f, 13.5f).dp
  val tileShape = RoundedCornerShape(tileCorner)

  val selectionProgress by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "objective_tile_select_${option.name}",
  )

  Box(
    modifier = modifier
      .heightIn(min = (126f * metrics.scale).coerceIn(112f, 146f).dp)
      .clip(tileShape)
      .drawWithCache {
        val cornerPx = tileCorner.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(75, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(12.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        onDrawBehind {
          val p = selectionProgress.coerceIn(0f, 1f)
          // Card fill: transitions from deep zinc to luminous platinum-tinted obsidian
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFF1D1D23).copy(alpha = 0.55f + 0.45f * p),
                Color(0xFF0E0E12).copy(alpha = 0.80f + 0.20f * p),
              ),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
          )

          if (p > 0.02f) {
            glowPaint.asFrameworkPaint().alpha = (65 * p).roundToInt().coerceIn(0, 85)
            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerPx,
                cornerPx,
                glowPaint,
              )
            }
          }

          // Top specular highlight line when selected
          if (p > 0.01f) {
            val topSpanW = size.width * (0.55f * p)
            drawRoundRect(
              color = PureWhite.copy(alpha = p),
              topLeft = Offset((size.width - topSpanW) / 2f, 0f),
              size = Size(topSpanW, 2.4.dp.toPx()),
              cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
          }

          drawRoundRect(
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.10f + 0.78f * p),
              1.0f to Zinc700.copy(alpha = 0.45f + 0.45f * p),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = (0.95f + 0.55f * p).dp.toPx()),
          )
        }
      }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics { selected = isSelected }
      .padding(
        horizontal = (10f * metrics.scale).coerceIn(8f, 13f).dp,
        vertical = (11f * metrics.scale).coerceIn(9f, 14f).dp,
      )
      .testTag(testTag),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top row: Vector Icon Box + Index Number ("01", "02", "03")
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier
            .size((32f * metrics.scale).coerceIn(28f, 36f).dp)
            .clip(RoundedCornerShape((8f * metrics.scale).coerceIn(6.5f, 9.5f).dp))
            .background(if (isSelected) PureWhite else Zinc900)
            .border(
              width = 0.9.dp,
              color = if (isSelected) PureWhite else Zinc700,
              shape = RoundedCornerShape((8f * metrics.scale).coerceIn(6.5f, 9.5f).dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          ObjectiveVectorGlyph(
            option = option,
            tint = if (isSelected) ObsidianBlack else PureWhite,
            modifier = Modifier.size((16f * metrics.scale).coerceIn(14f, 18.5f).dp),
          )
        }

        Text(
          text = option.codeLabel,
          color = if (isSelected) PureWhite else Zinc500,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
          letterSpacing = 0.6.sp,
        )
      }

      Spacer(modifier = Modifier.height((12f * metrics.scale).coerceIn(8f, 15f).dp))

      // Bottom Title & Subtitle
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = title,
          color = if (isSelected) PureWhite else Zinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(13.2f, 11.6f, 15f),
          letterSpacing = (-0.2).sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = subtitle,
          color = if (isSelected) Zinc300 else Zinc500,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
          lineHeight = metrics.spScaled(13.6f, 12f, 15.2f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ObjectiveVectorGlyph(
  option: BlueprintObjectiveOption,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = (w * 0.115f).coerceAtLeast(1.5f)

    when (option) {
      BlueprintObjectiveOption.LOSE_FAT -> {
        // Precision descending metabolic trend arrow with target baseline
        val path = Path().apply {
          moveTo(w * 0.16f, h * 0.24f)
          lineTo(w * 0.46f, h * 0.54f)
          lineTo(w * 0.62f, h * 0.38f)
          lineTo(w * 0.84f, h * 0.68f)
          moveTo(w * 0.58f, h * 0.68f)
          lineTo(w * 0.84f, h * 0.68f)
          lineTo(w * 0.84f, h * 0.42f)
        }
        drawPath(
          path = path,
          color = tint,
          style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
          color = tint.copy(alpha = 0.55f),
          start = Offset(w * 0.16f, h * 0.84f),
          end = Offset(w * 0.84f, h * 0.84f),
          strokeWidth = strokeW * 0.85f,
          cap = StrokeCap.Round,
        )
      }

      BlueprintObjectiveOption.MAINTAIN -> {
        // Precision equilibrium balance / dual harmonic waves
        drawLine(
          color = tint,
          start = Offset(w * 0.15f, h * 0.36f),
          end = Offset(w * 0.85f, h * 0.36f),
          strokeWidth = strokeW,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = tint,
          start = Offset(w * 0.15f, h * 0.64f),
          end = Offset(w * 0.85f, h * 0.64f),
          strokeWidth = strokeW,
          cap = StrokeCap.Round,
        )
        drawCircle(
          color = tint,
          radius = strokeW * 1.15f,
          center = Offset(w * 0.36f, h * 0.36f),
        )
        drawCircle(
          color = tint,
          radius = strokeW * 1.15f,
          center = Offset(w * 0.64f, h * 0.64f),
        )
      }

      BlueprintObjectiveOption.BUILD_MUSCLE -> {
        // Ascending anabolic vector arrow with peak marker
        val path = Path().apply {
          moveTo(w * 0.16f, h * 0.74f)
          lineTo(w * 0.44f, h * 0.46f)
          lineTo(w * 0.60f, h * 0.60f)
          lineTo(w * 0.84f, h * 0.24f)
          moveTo(w * 0.58f, h * 0.24f)
          lineTo(w * 0.84f, h * 0.24f)
          lineTo(w * 0.84f, h * 0.50f)
        }
        drawPath(
          path = path,
          color = tint,
          style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
          color = tint.copy(alpha = 0.55f),
          start = Offset(w * 0.16f, h * 0.86f),
          end = Offset(w * 0.84f, h * 0.86f),
          strokeWidth = strokeW * 0.85f,
          cap = StrokeCap.Round,
        )
      }
    }
  }
}

// ─── Section 3: Daily Streak Completion Threshold (100% / 95% / 90% / 85% / 80%) ─

@Composable
private fun StreakThresholdSection(
  selectedThreshold: StreakThresholdOption,
  onSelectThreshold: (StreakThresholdOption) -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedPercent by animateFloatAsState(
    targetValue = selectedThreshold.percentage.toFloat(),
    animationSpec = tween(durationMillis = 360, easing = BlueprintEaseOut),
    label = "streak_threshold_percent",
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_streak_section_label),
      statusPillText = "STREAK RULE • ${selectedThreshold.percentage}%",
      metrics = metrics,
    )

    Spacer(modifier = Modifier.height((7f * metrics.scale).coerceIn(5.5f, 10f).dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .drawWithCache {
          val cornerPx = metrics.cardCornerRadius.toPx()
          val cornerRadius = CornerRadius(cornerPx, cornerPx)
          val bgBrush = Brush.verticalGradient(
            colors = listOf(CardBgTop, CardBgBottom),
          )
          val borderBrush = Brush.verticalGradient(
            0.0f to PureWhite.copy(alpha = 0.22f),
            0.35f to Zinc700.copy(alpha = 0.65f),
            1.0f to Zinc850,
          )
          onDrawBehind {
            drawRoundRect(
              brush = bgBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              brush = borderBrush,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 1.dp.toPx()),
            )
          }
        }
        .padding(metrics.cardPadding)
        .testTag("blueprint_streak_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((11f * metrics.scale).coerceIn(8.5f, 14f).dp),
      ) {
        // Top Header inside Streak Card: Flame Emblem + Live Readout + 6 Tracked Pillars
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((11f * metrics.scale).coerceIn(9f, 13f).dp),
          ) {
            // Left Streak Emblem Box
            Box(
              modifier = Modifier
                .size((42f * metrics.scale).coerceIn(36f, 48f).dp)
                .clip(RoundedCornerShape((10f * metrics.scale).coerceIn(8f, 12f).dp))
                .background(PureWhite)
                .border(
                  width = 1.dp,
                  color = PureWhite,
                  shape = RoundedCornerShape((10f * metrics.scale).coerceIn(8f, 12f).dp),
                ),
              contentAlignment = Alignment.Center,
            ) {
              StreakFlameGlyph(
                tint = ObsidianBlack,
                modifier = Modifier.size((21f * metrics.scale).coerceIn(18f, 24f).dp),
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "+1 DAILY STREAK",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(11.5f, 10f, 13f),
                  letterSpacing = 0.8.sp,
                )
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Zinc850)
                    .border(0.6.dp, Zinc700, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.5.dp, vertical = 1.5.dp),
                ) {
                  Text(
                    text = selectedThreshold.tierTitle,
                    color = Zinc200,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = metrics.spScaled(8.4f, 7.5f, 9.6f),
                    letterSpacing = 0.5.sp,
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))

              Text(
                text = stringResource(R.string.blueprint_streak_section_subtitle),
                color = Zinc400,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = metrics.spScaled(11.2f, 9.8f, 12.6f),
                lineHeight = metrics.spScaled(15f, 13.2f, 17f),
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Right Large Animated Percentage Display
          Column(
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = "${animatedPercent.roundToInt()}%",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(26f, 22f, 30f),
              letterSpacing = (-0.8).sp,
              modifier = Modifier.testTag("streak_selected_percentage_text"),
            )
            Text(
              text = "GOAL TARGET",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
              letterSpacing = 0.8.sp,
            )
          }
        }

        // 5-Option Single-Select Percentage Dock: 100% | 95% | 90% | 85% | 80%
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((10.5f * metrics.scale).coerceIn(9f, 13f).dp))
            .background(Zinc950)
            .border(
              width = 0.9.dp,
              color = Zinc800,
              shape = RoundedCornerShape((10.5f * metrics.scale).coerceIn(9f, 13f).dp),
            )
            .padding((4.5f * metrics.scale).coerceIn(3.5f, 6f).dp),
          horizontalArrangement = Arrangement.spacedBy((5f * metrics.scale).coerceIn(4f, 7f).dp),
        ) {
          StreakThresholdOption.entries.forEach { option ->
            val isSelected = option == selectedThreshold
            StreakPercentPillButton(
              option = option,
              isSelected = isSelected,
              onClick = { onSelectThreshold(option) },
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Animated Threshold Calibration Bar + 6 Tracked Daily Goals Pills
        StreakThresholdPrecisionBar(
          animatedPercent = animatedPercent,
          modifier = Modifier
            .fillMaxWidth()
            .height((8f * metrics.scale).coerceIn(6.5f, 10f).dp),
        )

        // Tracked Daily Pillars row (Calories • Protein • Carbs • Fats • Hydration • Steps)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          val pillars = remember {
            listOf("CAL", "PRO", "CARB", "FAT", "H2O", "STEPS")
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy((4.5f * metrics.scale).coerceIn(3.5f, 6f).dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            pillars.forEach { pillar ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Zinc900)
                  .border(0.6.dp, Zinc800, RoundedCornerShape(4.dp))
                  .padding(horizontal = 5.5.dp, vertical = 2.dp),
              ) {
                Text(
                  text = pillar,
                  color = Zinc300,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = metrics.spScaled(8.3f, 7.4f, 9.5f),
                  letterSpacing = 0.4.sp,
                )
              }
            }
          }

          Text(
            text = "≥ ${selectedThreshold.percentage}% EACH DAY",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
            letterSpacing = 0.5.sp,
          )
        }
      }
    }
  }
}

@Composable
private fun StreakPercentPillButton(
  option: StreakThresholdOption,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val pillCorner = (8f * metrics.scale).coerceIn(6.5f, 10f).dp
  val pillShape = RoundedCornerShape(pillCorner)

  val selectProgress by animateFloatAsState(
    targetValue = if (isSelected) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "streak_pill_${option.percentage}",
  )

  Box(
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(pillShape)
      .drawWithCache {
        val cornerPx = pillCorner.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(90, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(8.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        onDrawBehind {
          val p = selectProgress.coerceIn(0f, 1f)
          if (p > 0.02f) {
            glowPaint.asFrameworkPaint().alpha = (85 * p).roundToInt().coerceIn(0, 105)
            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerPx,
                cornerPx,
                glowPaint,
              )
            }
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  PureWhite.copy(alpha = p),
                  Color(0xFFE4E6EB).copy(alpha = p),
                ),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
          } else {
            drawRoundRect(
              color = Zinc900.copy(alpha = 0.65f),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              color = Zinc800,
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 0.7.dp.toPx()),
            )
          }
        }
      }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics {
        selected = isSelected
        contentDescription = "${option.percentage} percent daily goal streak threshold"
      }
      .testTag("streak_option_${option.percentage}"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "${option.percentage}%",
        color = if (isSelected) ObsidianBlack else Zinc200,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        fontSize = metrics.spScaled(13.2f, 11.5f, 15f),
        letterSpacing = (-0.3).sp,
      )
      Spacer(modifier = Modifier.height(1.dp))
      Text(
        text = if (isSelected) "ACTIVE" else "STREAK",
        color = if (isSelected) ObsidianBlack.copy(alpha = 0.72f) else Zinc500,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(7.8f, 7f, 8.8f),
        letterSpacing = 0.5.sp,
      )
    }
  }
}

@Composable
private fun StreakThresholdPrecisionBar(
  animatedPercent: Float,
  modifier: Modifier = Modifier,
) {
  Spacer(
    modifier = modifier.drawWithCache {
      val glowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(145, 255, 255, 255)
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.height * 1.3f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }
      onDrawBehind {
        if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
        val trackH = size.height
        val r = CornerRadius(trackH / 2f, trackH / 2f)

        // Track background
        drawRoundRect(
          color = Zinc850,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = r,
        )
        drawRoundRect(
          color = Zinc700.copy(alpha = 0.55f),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = r,
          style = Stroke(width = 0.8.dp.toPx()),
        )

        // Fill width proportional to animatedPercent (mapped cleanly across the bar)
        val normalized = (animatedPercent / 100f).coerceIn(0.80f, 1.00f)
        val fillW = (size.width * normalized).coerceIn(trackH, size.width)

        drawIntoCanvas { canvas ->
          canvas.drawRoundRect(
            0f,
            0f,
            fillW,
            trackH,
            r.x,
            r.y,
            glowPaint,
          )
        }

        drawRoundRect(
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color(0xFFB8BDC7),
              Color(0xFFE8ECF2),
              PureWhite,
            ),
            startX = 0f,
            endX = fillW,
          ),
          topLeft = Offset.Zero,
          size = Size(fillW, trackH),
          cornerRadius = r,
        )

        // Tick marks at 80%, 85%, 90%, 95%, 100%
        val thresholds = floatArrayOf(0.80f, 0.85f, 0.90f, 0.95f, 1.00f)
        thresholds.forEach { t ->
          val tx = (size.width * t).coerceIn(trackH / 2f, size.width - trackH / 2f)
          drawCircle(
            color = if (t <= normalized + 0.005f) ObsidianBlack.copy(alpha = 0.55f) else Zinc600,
            radius = (trackH * 0.22f).coerceAtLeast(1.2f),
            center = Offset(tx, trackH / 2f),
          )
        }
      }
    },
  )
}

@Composable
private fun StreakFlameGlyph(
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = (w * 0.105f).coerceAtLeast(1.5f)

    // Sculpted geometric flame / streak bolt emblem
    val outerFlame = Path().apply {
      moveTo(w * 0.52f, h * 0.10f)
      cubicTo(w * 0.58f, h * 0.28f, w * 0.84f, h * 0.38f, w * 0.80f, h * 0.63f)
      cubicTo(w * 0.77f, h * 0.82f, w * 0.64f, h * 0.92f, w * 0.50f, h * 0.92f)
      cubicTo(w * 0.34f, h * 0.92f, w * 0.20f, h * 0.79f, w * 0.21f, h * 0.60f)
      cubicTo(w * 0.22f, h * 0.44f, w * 0.34f, h * 0.35f, w * 0.40f, h * 0.24f)
      cubicTo(w * 0.44f, h * 0.18f, w * 0.48f, h * 0.14f, w * 0.52f, h * 0.10f)
      close()
    }
    drawPath(
      path = outerFlame,
      color = tint,
      style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    val innerCore = Path().apply {
      moveTo(w * 0.50f, h * 0.46f)
      cubicTo(w * 0.60f, h * 0.56f, w * 0.63f, h * 0.66f, w * 0.58f, h * 0.77f)
      cubicTo(w * 0.55f, h * 0.83f, w * 0.45f, h * 0.83f, w * 0.42f, h * 0.77f)
      cubicTo(w * 0.37f, h * 0.66f, w * 0.42f, h * 0.56f, w * 0.50f, h * 0.46f)
      close()
    }
    drawPath(
      path = innerCore,
      color = tint,
    )
  }
}

// ─── Live Blueprint Protocol Matrix Summary Card ────────────────────────────

@Composable
private fun BlueprintSummaryMatrixCard(
  subjectName: String,
  selectedActivityLevel: ActivityLevelOption,
  selectedObjective: BlueprintObjectiveOption,
  selectedStreakThreshold: StreakThresholdOption,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val activityLabel = when (selectedActivityLevel) {
    ActivityLevelOption.SEDENTARY -> stringResource(R.string.blueprint_activity_sedentary_title)
    ActivityLevelOption.LIGHT -> stringResource(R.string.blueprint_activity_light_title)
    ActivityLevelOption.MODERATE -> stringResource(R.string.blueprint_activity_moderate_title)
    ActivityLevelOption.VERY_ACTIVE -> stringResource(R.string.blueprint_activity_very_active_title)
  }
  val objectiveLabel = when (selectedObjective) {
    BlueprintObjectiveOption.LOSE_FAT -> stringResource(R.string.blueprint_objective_lose_fat_title)
    BlueprintObjectiveOption.MAINTAIN -> stringResource(R.string.blueprint_objective_maintain_title)
    BlueprintObjectiveOption.BUILD_MUSCLE -> stringResource(R.string.blueprint_objective_build_muscle_title)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .drawWithCache {
        val cornerPx = metrics.cardCornerRadius.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val bgBrush = Brush.verticalGradient(
          colors = listOf(CardBgTop, CardBgBottom),
        )
        onDrawBehind {
          val flash = sin(borderFlashProvider().coerceIn(0f, 1f) * Math.PI.toFloat()).coerceIn(0f, 1f)
          drawRoundRect(
            brush = bgBrush,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
          )
          drawRoundRect(
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.24f + 0.55f * flash),
              0.5f to Zinc600.copy(alpha = 0.55f + 0.35f * flash),
              1.0f to Zinc850,
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = (1.1f + 0.5f * flash).dp.toPx()),
          )
        }
      }
      .padding(metrics.cardPadding)
      .testTag("blueprint_summary_matrix_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy((10f * metrics.scale).coerceIn(8f, 13f).dp),
    ) {
      // Top Row: Luminous Dot + BLUEPRINT PROTOCOL MATRIX + Subject Lock Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          BlueprintLuminousWhiteDot(
            size = (7f * metrics.scale).coerceIn(6f, 8.5f).dp,
          )
          Text(
            text = stringResource(R.string.blueprint_summary_header),
            color = Zinc200,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(10.2f, 9f, 11.8f),
            letterSpacing = 1.1.sp,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(PureWhite)
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = "CALIBRATED",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
            letterSpacing = 0.7.sp,
          )
        }
      }

      HorizontalDivider(color = Zinc850, thickness = 0.8.dp)

      // 3-Column Live Telemetry Readout (Activity | Objective | Streak Rule)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10f).dp),
      ) {
        SummaryMetricTile(
          label = "ACTIVITY",
          value = activityLabel,
          subValue = selectedActivityLevel.cadenceBadge,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        SummaryMetricTile(
          label = "OBJECTIVE",
          value = objectiveLabel,
          subValue = selectedObjective.vectorBadge,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        SummaryMetricTile(
          label = "STREAK RULE",
          value = "${selectedStreakThreshold.percentage}% GOAL",
          subValue = "+1 DAY STREAK",
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
      }

      // Subject footer note
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "SUBJECT: ${subjectName.ifBlank { "UNVERIFIED" }.uppercase()}",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          letterSpacing = 0.6.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "STEP 03 / 03 READY",
          color = Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          letterSpacing = 0.6.sp,
        )
      }
    }
  }
}

@Composable
private fun SummaryMetricTile(
  label: String,
  value: String,
  subValue: String,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp))
      .background(Zinc950)
      .border(
        width = 0.8.dp,
        color = Zinc800,
        shape = RoundedCornerShape((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp),
      )
      .padding(
        horizontal = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
        vertical = (8f * metrics.scale).coerceIn(6.5f, 10f).dp,
      ),
  ) {
    Text(
      text = label,
      color = Zinc500,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
      letterSpacing = 0.7.sp,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = value,
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(12.2f, 10.8f, 13.8f),
      letterSpacing = (-0.2).sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = subValue,
      color = Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(8.2f, 7.2f, 9.4f),
      letterSpacing = 0.4.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ─── Primary Complete Blueprint CTA Section ─────────────────────────────────

@Composable
private fun BlueprintContinueSection(
  onContinueClick: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val buttonCorner = (10f * metrics.scale).coerceIn(8f, 13f).dp
  val buttonShape = RoundedCornerShape(buttonCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "blueprint_continue_glow_transition")
  val shimmerPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "blueprint_continue_shimmer_phase",
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
        .testTag("blueprint_continue_button"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(R.string.blueprint_cta_complete),
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
      modifier = Modifier.testTag("blueprint_footer_encrypted_text"),
    )
  }
}
