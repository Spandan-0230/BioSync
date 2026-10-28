package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
 * Minimalist spectrum accents progress from light multi-color (Sky-Mint -> Mint-Citrine -> Amber-Gold -> Dense Burnished Gold)
 * while the cards and typography stay crisp Obsidian Black & Pure White.
 */
enum class ActivityLevelOption(
  val index: Int,
  val tierCode: String,
  val cadenceBadge: String,
  val activeBars: Int,
  val intensityFraction: Float,
  val spectrumStart: Color,
  val spectrumEnd: Color,
  val accentColor: Color,
) {
  SEDENTARY(
    index = 1,
    tierCode = "TIER 01 • SEDENTARY",
    cadenceBadge = "DESK / LOW",
    activeBars = 1,
    intensityFraction = 0.25f,
    spectrumStart = Color(0xFF7BE3FF),
    spectrumEnd = Color(0xFF7BF1A8),
    accentColor = Color(0xFF7BE3FF),
  ),
  LIGHT(
    index = 2,
    tierCode = "TIER 02 • LIGHT",
    cadenceBadge = "1–3 DAYS/WK",
    activeBars = 2,
    intensityFraction = 0.50f,
    spectrumStart = Color(0xFF7BF1A8),
    spectrumEnd = Color(0xFFFDE68A),
    accentColor = Color(0xFF6EE7B7),
  ),
  MODERATE(
    index = 3,
    tierCode = "TIER 03 • MODERATE",
    cadenceBadge = "3–5 DAYS/WK",
    activeBars = 3,
    intensityFraction = 0.75f,
    spectrumStart = Color(0xFFFDE047),
    spectrumEnd = Color(0xFFF59E0B),
    accentColor = Color(0xFFFBBF24),
  ),
  VERY_ACTIVE(
    index = 4,
    tierCode = "TIER 04 • VERY ACTIVE",
    cadenceBadge = "6–7 DAYS/WK",
    activeBars = 4,
    intensityFraction = 1.00f,
    spectrumStart = Color(0xFFFFD700),
    spectrumEnd = Color(0xFFD97706),
    accentColor = Color(0xFFFFC300),
  ),
}

/**
 * Section 2 — Primary Metabolic Objective options (Single-select).
 * Monochrome cards with dense/solid category color badges for Lose Fat, Maintain, Build Muscle.
 */
enum class BlueprintObjectiveOption(
  val index: Int,
  val codeLabel: String,
  val vectorBadge: String,
  val protocolSummary: String,
  val primaryColor: Color,
  val secondaryColor: Color,
) {
  LOSE_FAT(
    index = 1,
    codeLabel = "01",
    vectorBadge = "DEFICIT VECTOR",
    protocolSummary = "Prioritizes adipose mobilization while safeguarding lean tissue.",
    primaryColor = Color(0xFFFF6B35),
    secondaryColor = Color(0xFFE11D48),
  ),
  MAINTAIN(
    index = 2,
    codeLabel = "02",
    vectorBadge = "EQUILIBRIUM",
    protocolSummary = "Sustains baseline mass with balanced energy and recovery.",
    primaryColor = Color(0xFF10B981),
    secondaryColor = Color(0xFF059669),
  ),
  BUILD_MUSCLE(
    index = 3,
    codeLabel = "03",
    vectorBadge = "SURPLUS VECTOR",
    protocolSummary = "Drives lean tissue accretion and progressive performance.",
    primaryColor = Color(0xFF8B5CF6),
    secondaryColor = Color(0xFF6D28D9),
  ),
}

/**
 * Section 3 — Daily Streak Completion Threshold options (Single-select: 100%, 95%, 90%, 85%, 80%).
 * Each percentage option has a concise category sub-label and distinct minimalist color accent.
 */
enum class StreakThresholdOption(
  val percentage: Int,
  val shortTag: String,
  val tierTitle: String,
  val tierDescriptor: String,
  val barFillFraction: Float,
  val colorStart: Color,
  val colorEnd: Color,
  val accentColor: Color,
) {
  PERCENT_100(
    percentage = 100,
    shortTag = "PEAK",
    tierTitle = "ABSOLUTE PRECISION",
    tierDescriptor = "Requires 100% completion across daily macro, hydration & step targets to earn +1 Streak.",
    barFillFraction = 1.00f,
    colorStart = Color(0xFFFFE169),
    colorEnd = Color(0xFFF59E0B),
    accentColor = Color(0xFFFFD166),
  ),
  PERCENT_95(
    percentage = 95,
    shortTag = "ELITE",
    tierTitle = "ELITE DISCIPLINE",
    tierDescriptor = "Requires at least 95% of overall daily goals completed to count as +1 Streak.",
    barFillFraction = 0.80f,
    colorStart = Color(0xFFFF8E53),
    colorEnd = Color(0xFFFF2E63),
    accentColor = Color(0xFFFF7A29),
  ),
  PERCENT_90(
    percentage = 90,
    shortTag = "OPTIMAL",
    tierTitle = "OPTIMAL CONSISTENCY",
    tierDescriptor = "Requires at least 90% of overall daily goals completed to count as +1 Streak.",
    barFillFraction = 0.60f,
    colorStart = Color(0xFF6EE7B7),
    colorEnd = Color(0xFF10B981),
    accentColor = Color(0xFF34D399),
  ),
  PERCENT_85(
    percentage = 85,
    shortTag = "STRONG",
    tierTitle = "HIGH MOMENTUM",
    tierDescriptor = "Requires at least 85% of overall daily goals completed to count as +1 Streak.",
    barFillFraction = 0.40f,
    colorStart = Color(0xFF7BE3FF),
    colorEnd = Color(0xFF0B76E0),
    accentColor = Color(0xFF5BC0FF),
  ),
  PERCENT_80(
    percentage = 80,
    shortTag = "STEADY",
    tierTitle = "SUSTAINABLE RHYTHM",
    tierDescriptor = "Requires at least 80% of overall daily goals completed to count as +1 Streak.",
    barFillFraction = 0.20f,
    colorStart = Color(0xFFC084FC),
    colorEnd = Color(0xFF7C3AED),
    accentColor = Color(0xFFB794F4),
  ),
  ;

  companion object {
    fun fromPercent(percent: Int): StreakThresholdOption =
      entries.firstOrNull { it.percentage == percent } ?: PERCENT_90
  }
}

@Immutable
internal data class BlueprintResponsiveMetrics(
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
  ageYears: Int = 31,
  weightKg: Float = 62.0f,
  selectedActivityLevel: ActivityLevelOption,
  onSelectActivityLevel: (ActivityLevelOption) -> Unit,
  selectedWorkoutDays: Set<DayOfWeekOption> = DailyTargetsEngine.defaultWorkoutDaysFor(selectedActivityLevel),
  onUpdateWorkoutDays: (Set<DayOfWeekOption>) -> Unit = {},
  selectedOccupationLoad: OccupationLoadOption = OccupationLoadOption.DESK,
  onSelectOccupationLoad: (OccupationLoadOption) -> Unit = {},
  selectedObjective: BlueprintObjectiveOption,
  onSelectObjective: (BlueprintObjectiveOption) -> Unit,
  selectedTrainingModality: TrainingModalityOption = TrainingModalityOption.MIXED,
  onSelectTrainingModality: (TrainingModalityOption) -> Unit = {},
  selectedWorkoutDuration: WorkoutDurationOption = WorkoutDurationOption.HR_1_0,
  onSelectWorkoutDuration: (WorkoutDurationOption) -> Unit = {},
  selectedExperienceLevel: ExperienceLevelOption = ExperienceLevelOption.INTERMEDIATE,
  onSelectExperienceLevel: (ExperienceLevelOption) -> Unit = {},
  selectedTargetPace: TargetPaceOption = TargetPaceOption.NORMAL,
  onSelectTargetPace: (TargetPaceOption) -> Unit = {},
  selectedDayOne: DayOfWeekOption = DayOfWeekOption.MONDAY,
  onSelectDayOne: (DayOfWeekOption) -> Unit = {},
  selectedScheduleCadence: ScheduleCadenceOption = ScheduleCadenceOption.UNIFORM_DAILY,
  onSelectScheduleCadence: (ScheduleCadenceOption) -> Unit = {},
  selectedMealFrequency: MealFrequencyOption = MealFrequencyOption.MEALS_3,
  onSelectMealFrequency: (MealFrequencyOption) -> Unit = {},
  caffeineAlcoholYes: Boolean = false,
  onToggleCaffeineAlcohol: (Boolean) -> Unit = {},
  selectedSodiumFrequency: SodiumFrequencyOption = SodiumFrequencyOption.RARELY,
  onSelectSodiumFrequency: (SodiumFrequencyOption) -> Unit = {},
  selectedStreakThreshold: StreakThresholdOption,
  onSelectStreakThreshold: (StreakThresholdOption) -> Unit,
  onNavigateBack: () -> Unit,
  onCompleteBlueprint: () -> Unit = {},
  isQuickEditMode: Boolean = false,
  initialFocusSection: String? = null,
  onQuickEditReturnToSummary: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  val haptics = rememberBioSyncHaptics()
  val coroutineScope = rememberCoroutineScope()
  val headerStepPhase = remember { Animatable(0f) }
  val borderFlashPhase = remember { Animatable(0f) }
  var workoutDaysDialogActivity by remember { mutableStateOf<ActivityLevelOption?>(null) }

  // Top STEP 03 OF 04 progress bar: animates from Step 02 (0.50f) to Step 03 (0.75f) on entry
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
      if (isQuickEditMode) {
        onQuickEditReturnToSummary()
      } else {
        onCompleteBlueprint()
      }
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

      // Track section Y positions inside the scrollable Column for smooth scroll-to-section
      var activitySectionY by remember { mutableIntStateOf(0) }
      var objectiveSectionY by remember { mutableIntStateOf(0) }
      var trainingSectionY by remember { mutableIntStateOf(0) }
      var paceCycleSectionY by remember { mutableIntStateOf(0) }
      var nutritionSectionY by remember { mutableIntStateOf(0) }
      var streakSectionY by remember { mutableIntStateOf(0) }

      val calibration = remember(
        weightKg,
        selectedWorkoutDuration,
        selectedExperienceLevel,
        selectedObjective,
        caffeineAlcoholYes,
        selectedSodiumFrequency,
      ) {
        computeHydrationMacroCalibration(
          weightKg = weightKg,
          workoutDuration = selectedWorkoutDuration,
          experienceLevel = selectedExperienceLevel,
          objective = selectedObjective,
          caffeineAlcoholYes = caffeineAlcoholYes,
          sodiumFrequency = selectedSodiumFrequency,
        )
      }

      val scrollToSection: (Int) -> Unit = { targetY ->
        haptics.selection()
        coroutineScope.launch {
          val offset = (targetY - 18).coerceAtLeast(0)
          scrollState.animateScrollTo(
            value = offset,
            animationSpec = tween(durationMillis = 520, easing = BlueprintEaseOut),
          )
        }
      }

      LaunchedEffect(
        initialFocusSection,
        activitySectionY,
        objectiveSectionY,
        trainingSectionY,
        paceCycleSectionY,
        nutritionSectionY,
        streakSectionY,
      ) {
        when (initialFocusSection) {
          "activity" -> if (activitySectionY > 0) scrollState.animateScrollTo((activitySectionY - 18).coerceAtLeast(0))
          "objective" -> if (objectiveSectionY > 0) scrollState.animateScrollTo((objectiveSectionY - 18).coerceAtLeast(0))
          "training" -> if (trainingSectionY > 0) scrollState.animateScrollTo((trainingSectionY - 18).coerceAtLeast(0))
          "pace" -> if (paceCycleSectionY > 0) scrollState.animateScrollTo((paceCycleSectionY - 18).coerceAtLeast(0))
          "nutrition" -> if (nutritionSectionY > 0) scrollState.animateScrollTo((nutritionSectionY - 18).coerceAtLeast(0))
          "streak" -> if (streakSectionY > 0) scrollState.animateScrollTo((streakSectionY - 18).coerceAtLeast(0))
          else -> scrollState.scrollTo(0)
        }
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
        // Top Header Bar (BioSync + STEP 03 OF 04 + Shiny Glowy White Step Bar)
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

        // 2. Section 1: Activity Level & Occupation Physical Load
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              activitySectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
          ActivityLevelSection(
            selectedLevel = selectedActivityLevel,
            selectedWorkoutDays = selectedWorkoutDays,
            onOpenWorkoutDaysPicker = {
              haptics.selection()
              workoutDaysDialogActivity = selectedActivityLevel
            },
            onSelectLevel = { level ->
              haptics.selection()
              if (level != selectedActivityLevel) {
                onSelectActivityLevel(level)
                when (level) {
                  ActivityLevelOption.SEDENTARY -> {
                    onSelectScheduleCadence(ScheduleCadenceOption.UNIFORM_DAILY)
                    onUpdateWorkoutDays(emptySet())
                  }
                  ActivityLevelOption.LIGHT -> {
                    val defaultLightDays = DailyTargetsEngine.defaultWorkoutDaysFor(ActivityLevelOption.LIGHT)
                    if (selectedWorkoutDays.size !in 1..3) {
                      onUpdateWorkoutDays(defaultLightDays)
                    }
                    workoutDaysDialogActivity = ActivityLevelOption.LIGHT
                  }
                  ActivityLevelOption.MODERATE -> {
                    val defaultModDays = DailyTargetsEngine.defaultWorkoutDaysFor(ActivityLevelOption.MODERATE)
                    if (selectedWorkoutDays.size !in 3..5) {
                      onUpdateWorkoutDays(defaultModDays)
                    }
                    workoutDaysDialogActivity = ActivityLevelOption.MODERATE
                  }
                  ActivityLevelOption.VERY_ACTIVE -> {
                    onUpdateWorkoutDays(DailyTargetsEngine.defaultWorkoutDaysFor(ActivityLevelOption.VERY_ACTIVE))
                  }
                }
              } else if (level == ActivityLevelOption.LIGHT || level == ActivityLevelOption.MODERATE) {
                workoutDaysDialogActivity = level
              }
            },
            selectedOccupationLoad = selectedOccupationLoad,
            onSelectOccupationLoad = { occ ->
              if (occ != selectedOccupationLoad) {
                haptics.selection()
                onSelectOccupationLoad(occ)
              } else {
                haptics.tick()
              }
            },
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 3. Section 2: Objective (Monochrome cards with solid/dense colored badges)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              objectiveSectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
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
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. Section 3: Training Architecture (Cardio/Strength/Mixed + Workout Duration + Experience Level)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              trainingSectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
          TrainingArchitectureSection(
            selectedModality = selectedTrainingModality,
            onSelectModality = { modality ->
              if (modality != selectedTrainingModality) {
                haptics.selection()
                onSelectTrainingModality(modality)
              } else {
                haptics.tick()
              }
            },
            selectedDuration = selectedWorkoutDuration,
            onSelectDuration = { duration ->
              if (duration != selectedWorkoutDuration) {
                haptics.selection()
                onSelectWorkoutDuration(duration)
              } else {
                haptics.tick()
              }
            },
            selectedExperience = selectedExperienceLevel,
            onSelectExperience = { exp ->
              if (exp != selectedExperienceLevel) {
                haptics.selection()
                onSelectExperienceLevel(exp)
              } else {
                haptics.tick()
              }
            },
            selectedObjective = selectedObjective,
            weightKg = weightKg,
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 5. Section 4: Metabolic Pace & Cycle (Target Pace TDEE + Day 01 Anchor + Uniform/Rest Days)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              paceCycleSectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
          MetabolicPaceCycleSection(
            selectedPace = selectedTargetPace,
            onSelectPace = { pace ->
              if (pace != selectedTargetPace) {
                haptics.selection()
                onSelectTargetPace(pace)
              } else {
                haptics.tick()
              }
            },
            selectedDayOne = selectedDayOne,
            onSelectDayOne = { day ->
              if (day != selectedDayOne) {
                haptics.selection()
                onSelectDayOne(day)
              } else {
                haptics.tick()
              }
            },
            selectedCadence = if (selectedActivityLevel == ActivityLevelOption.SEDENTARY) {
              ScheduleCadenceOption.UNIFORM_DAILY
            } else {
              selectedScheduleCadence
            },
            onSelectCadence = { cadence ->
              if (selectedActivityLevel == ActivityLevelOption.SEDENTARY) {
                onSelectScheduleCadence(ScheduleCadenceOption.UNIFORM_DAILY)
              } else if (cadence != selectedScheduleCadence) {
                haptics.selection()
                onSelectScheduleCadence(cadence)
              } else {
                haptics.tick()
              }
            },
            selectedActivityLevel = selectedActivityLevel,
            selectedWorkoutDays = selectedWorkoutDays,
            onOpenWorkoutDaysPicker = {
              haptics.selection()
              workoutDaysDialogActivity = selectedActivityLevel
            },
            selectedObjective = selectedObjective,
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 6. Section 5: Nutrition & Hydration Calibration (Meal Frequency + Caffeine/Alcohol + Sodium Flush)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              nutritionSectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
          NutritionHydrationSection(
            selectedMealFrequency = selectedMealFrequency,
            onSelectMealFrequency = { freq ->
              if (freq != selectedMealFrequency) {
                haptics.selection()
                onSelectMealFrequency(freq)
              } else {
                haptics.tick()
              }
            },
            caffeineAlcoholYes = caffeineAlcoholYes,
            onToggleCaffeineAlcohol = { enabled ->
              if (enabled != caffeineAlcoholYes) {
                haptics.selection()
                onToggleCaffeineAlcohol(enabled)
              } else {
                haptics.tick()
              }
            },
            selectedSodiumFrequency = selectedSodiumFrequency,
            onSelectSodiumFrequency = { sodium ->
              if (sodium != selectedSodiumFrequency) {
                haptics.selection()
                onSelectSodiumFrequency(sodium)
              } else {
                haptics.tick()
              }
            },
            calibration = calibration,
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 7. Section 6: Daily Streak Completion Threshold (100% / 95% / 90% / 85% / 80%)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              streakSectionY = coords.positionInParent().y.roundToInt()
            },
        ) {
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
            borderFlashProvider = borderFlashProvider,
            metrics = metrics,
          )
        }

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 8. Live Blueprint Protocol Matrix Summary Card (Clickable tiles scroll to sections)
        BlueprintSummaryMatrixCard(
          subjectName = subjectName,
          ageYears = ageYears,
          selectedActivityLevel = selectedActivityLevel,
          selectedWorkoutDays = selectedWorkoutDays,
          selectedObjective = selectedObjective,
          selectedTrainingModality = selectedTrainingModality,
          selectedWorkoutDuration = selectedWorkoutDuration,
          selectedExperienceLevel = selectedExperienceLevel,
          selectedTargetPace = selectedTargetPace,
          selectedDayOne = selectedDayOne,
          selectedScheduleCadence = if (selectedActivityLevel == ActivityLevelOption.SEDENTARY) {
            ScheduleCadenceOption.UNIFORM_DAILY
          } else {
            selectedScheduleCadence
          },
          selectedMealFrequency = selectedMealFrequency,
          caffeineAlcoholYes = caffeineAlcoholYes,
          selectedSodiumFrequency = selectedSodiumFrequency,
          selectedStreakThreshold = selectedStreakThreshold,
          onClickActivityTile = { scrollToSection(activitySectionY) },
          onClickObjectiveTile = { scrollToSection(objectiveSectionY) },
          onClickTrainingTile = { scrollToSection(trainingSectionY) },
          onClickPaceCycleTile = { scrollToSection(paceCycleSectionY) },
          onClickNutritionTile = { scrollToSection(nutritionSectionY) },
          onClickStreakTile = { scrollToSection(streakSectionY) },
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.cardToCtaGap))

        // 9. Primary Complete Blueprint Button & Encrypted Footer
        BlueprintContinueSection(
          onContinueClick = onContinueClick,
          isQuickEditMode = isQuickEditMode,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )
      }
    }
  }

  val activeDialogActivity = workoutDaysDialogActivity
  if (activeDialogActivity != null &&
    (activeDialogActivity == ActivityLevelOption.LIGHT || activeDialogActivity == ActivityLevelOption.MODERATE)
  ) {
    WorkoutDaysPickerDialog(
      activityLevel = activeDialogActivity,
      initialSelectedDays = selectedWorkoutDays,
      onConfirmDays = { chosenDays ->
        haptics.confirm()
        onUpdateWorkoutDays(chosenDays)
        workoutDaysDialogActivity = null
      },
      onDismiss = {
        workoutDaysDialogActivity = null
      },
    )
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
internal fun BlueprintLuminousWhiteDot(
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
          val activeFraction = 0.50f + (0.75f - 0.50f) * p

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

          val step1X = size.width * 0.25f
          val step2X = size.width * 0.50f
          val step3X = size.width * 0.75f

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
          drawLine(
            color = ObsidianBlack.copy(alpha = 0.75f),
            start = Offset(step3X, 0f),
            end = Offset(step3X, barH),
            strokeWidth = 1.2.dp.toPx(),
          )
        }
      },
  )
}

// ─── Shared Section Header (Monochrome Pure White Dot & Title) ──────────────

@Composable
private fun BlueprintSectionHeaderRow(
  label: String,
  metrics: BlueprintResponsiveMetrics,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
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
  }
}

// ─── Section 1: Activity Level (Monochrome Base + Minimalist Spectrum Bar/Badges) ─

@Composable
private fun ActivityLevelSection(
  selectedLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  onOpenWorkoutDaysPicker: () -> Unit,
  onSelectLevel: (ActivityLevelOption) -> Unit,
  selectedOccupationLoad: OccupationLoadOption,
  onSelectOccupationLoad: (OccupationLoadOption) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedIntensity by animateFloatAsState(
    targetValue = selectedLevel.intensityFraction,
    animationSpec = tween(durationMillis = 380, easing = BlueprintEaseOut),
    label = "activity_intensity_bar",
  )
  val animatedAccent by animateColorAsState(
    targetValue = selectedLevel.accentColor,
    animationSpec = tween(durationMillis = 280),
    label = "activity_spectrum_accent",
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_activity_section_label),
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
        .testTag("blueprint_activity_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 10.5f).dp),
      ) {
        // Top Kinetic Output Spectrum Header inside the Activity card
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.5.dp),
          ) {
            Canvas(modifier = Modifier.size(5.5.dp)) {
              drawCircle(color = animatedAccent)
            }
            Text(
              text = selectedLevel.cadenceBadge,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(9.4f, 8.4f, 10.8f),
              letterSpacing = 0.7.sp,
            )
          }
        }

        // Animated 4-segment kinetic bar progressing from light multi-color to dense gold
        KineticSpectrumGauge(
          progress = animatedIntensity,
          activeSegments = selectedLevel.activeBars,
          modifier = Modifier
            .fillMaxWidth()
            .height((6f * metrics.scale).coerceIn(5f, 7.5f).dp),
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

        if (selectedLevel == ActivityLevelOption.LIGHT || selectedLevel == ActivityLevelOption.MODERATE) {
          val orderedDays = DayOfWeekOption.entries.filter { it in selectedWorkoutDays }
          val wCount = orderedDays.size
          val rCount = (7 - wCount).coerceAtLeast(0)
          val dayCodes = if (orderedDays.isEmpty()) "NONE" else orderedDays.joinToString(" • ") { it.shortCode }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape((9.5f * metrics.scale).dp))
              .background(Zinc950)
              .border(1.dp, Zinc800, RoundedCornerShape((9.5f * metrics.scale).dp))
              .clickable(role = Role.Button, onClick = onOpenWorkoutDaysPicker)
              .padding(horizontal = (11f * metrics.scale).dp, vertical = (9.5f * metrics.scale).dp)
              .testTag("activity_workout_days_picker_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "SELECTED WORKOUT DAYS ($wCount WORKOUT • $rCount REST)",
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
                letterSpacing = 0.6.sp,
              )
              Text(
                text = dayCodes,
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(11.2f, 10.0f, 12.4f),
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PureWhite)
                .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
              Text(
                text = "EDIT DAYS",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
              )
            }
          }
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = Zinc800,
          modifier = Modifier.padding(vertical = (3f * metrics.scale).dp),
        )

        // Occupation Physical Load Selector (Step 5 & Step 11 of PDF)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((6.5f * metrics.scale).coerceIn(5f, 8.5f).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.blueprint_occupation_label),
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.2f, 8.0f, 10.5f),
              letterSpacing = 0.9.sp,
            )
            Text(
              text = selectedOccupationLoad.badgeCode,
              color = selectedOccupationLoad.accentColor,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(9.0f, 8.0f, 10.2f),
              letterSpacing = 0.6.sp,
            )
          }

          val occOptions = remember { OccupationLoadOption.entries }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4.5f, 8f).dp),
          ) {
            occOptions.take(2).forEach { occ ->
              OccupationOptionPill(
                option = occ,
                isSelected = occ == selectedOccupationLoad,
                onClick = { onSelectOccupationLoad(occ) },
                metrics = metrics,
                modifier = Modifier.weight(1f),
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((6f * metrics.scale).coerceIn(4.5f, 8f).dp),
          ) {
            occOptions.drop(2).forEach { occ ->
              OccupationOptionPill(
                option = occ,
                isSelected = occ == selectedOccupationLoad,
                onClick = { onSelectOccupationLoad(occ) },
                metrics = metrics,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OccupationOptionPill(
  option: OccupationLoadOption,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val pillCorner = (9f * metrics.scale).coerceIn(7.5f, 11.5f).dp
  val pillShape = RoundedCornerShape(pillCorner)
  val bgColor by animateColorAsState(
    targetValue = if (isSelected) PureWhite else Zinc900,
    animationSpec = tween(durationMillis = 200),
    label = "occ_pill_bg_${option.name}",
  )
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) PureWhite else Zinc800,
    animationSpec = tween(durationMillis = 200),
    label = "occ_pill_border_${option.name}",
  )
  val titleColor by animateColorAsState(
    targetValue = if (isSelected) ObsidianBlack else PureWhite,
    animationSpec = tween(durationMillis = 200),
    label = "occ_pill_title_${option.name}",
  )
  val subColor by animateColorAsState(
    targetValue = if (isSelected) Zinc600 else Zinc400,
    animationSpec = tween(durationMillis = 200),
    label = "occ_pill_sub_${option.name}",
  )

  Column(
    modifier = modifier
      .clip(pillShape)
      .background(bgColor)
      .border(width = 1.dp, color = borderColor, shape = pillShape)
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics { selected = isSelected }
      .padding(
        horizontal = (10f * metrics.scale).coerceIn(8f, 12.5f).dp,
        vertical = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
      )
      .testTag("occupation_option_${option.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = option.title,
        color = titleColor,
        fontFamily = InterFontFamily,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        fontSize = metrics.spScaled(11.8f, 10.4f, 13.4f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Canvas(modifier = Modifier.size(5.5.dp)) {
        drawCircle(color = if (isSelected) ObsidianBlack else option.accentColor)
      }
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = option.subtitle,
      color = subColor,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = metrics.spScaled(9.8f, 8.6f, 11.0f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * 4-segment Kinetic Output Spectrum bar that transitions from light multi-color
 * (Sky-Mint -> Mint-Citrine -> Amber-Gold -> Dense Burnished Gold) on a clean monochrome track.
 */
@Composable
private fun KineticSpectrumGauge(
  progress: Float,
  activeSegments: Int,
  modifier: Modifier = Modifier,
) {
  val levels = remember { ActivityLevelOption.entries }
  Spacer(
    modifier = modifier.drawWithCache {
      val segmentGlowPaints = levels.map { level ->
        Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = level.accentColor.copy(alpha = 0.55f).toArgb()
            maskFilter = BlurMaskFilter(
              safeBlurRadius(size.height * 1.35f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }
      }
      onDrawBehind {
        if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
        val gap = 6.dp.toPx()
        val segWidth = ((size.width - gap * 3f) / 4f).coerceAtLeast(2f)
        val r = CornerRadius(size.height / 2f, size.height / 2f)

        for (i in 0 until 4) {
          val level = levels[i]
          val startX = i * (segWidth + gap)
          val segStartFraction = i * 0.25f
          val localFill = ((progress - segStartFraction) / 0.25f).coerceIn(0f, 1f)

          // Clean monochrome base track segment
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
                segmentGlowPaints[i],
              )
            }
            drawRoundRect(
              brush = Brush.horizontalGradient(
                colors = listOf(level.spectrumStart, level.spectrumEnd),
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
          // Monochrome obsidian/zinc row surface
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

          // Monochrome PureWhite / Zinc border
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

          // Minimalist left vertical spectrum accent bar when selected
          if (p > 0.01f) {
            val barH = size.height * (0.48f * p)
            val barW = 3.dp.toPx()
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  option.spectrumStart.copy(alpha = p),
                  option.spectrumEnd.copy(alpha = p),
                ),
              ),
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
        // Left Architectural Signal Box synced with this Activity Level's spectrum color
        val badgeCorner = (9f * metrics.scale).coerceIn(7.5f, 11f).dp
        val badgeShape = RoundedCornerShape(badgeCorner)
        Box(
          modifier = Modifier
            .size((38f * metrics.scale).coerceIn(34f, 44f).dp)
            .clip(badgeShape)
            .background(
              brush = if (isSelected) {
                Brush.linearGradient(
                  colors = listOf(option.spectrumStart, option.spectrumEnd),
                )
              } else {
                Brush.verticalGradient(
                  colors = listOf(Zinc900, Zinc950),
                )
              },
            )
            .border(
              width = 1.dp,
              color = if (isSelected) option.spectrumStart else option.accentColor.copy(alpha = 0.40f),
              shape = badgeShape,
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
                !isSelected && isBarLit -> option.accentColor
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

        // Clean Black & White Title + Subtitle (01/02/03/04 blocks removed)
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center,
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

      // Right Precision Radio Indicator (White outer ring, synced spectrum inner dot)
      BlueprintRadioLockIndicator(
        selectionProgress = selectionProgress,
        activeColor = option.accentColor,
        size = (20f * metrics.scale).coerceIn(18f, 23f).dp,
      )
    }
  }
}

@Composable
private fun BlueprintRadioLockIndicator(
  selectionProgress: Float,
  activeColor: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val r = this.size.minDimension / 2f
    val p = selectionProgress.coerceIn(0f, 1f)

    // Monochrome outer ring
    drawCircle(
      color = if (p > 0.5f) PureWhite else Zinc600,
      radius = r - 1.dp.toPx(),
      style = Stroke(width = (1.3f + 0.4f * p).dp.toPx()),
    )

    if (p > 0.02f) {
      // Minimalist spectrum-synced inner core
      drawCircle(
        color = activeColor.copy(alpha = p),
        radius = (r * 0.52f) * p,
        center = center,
      )
    }
  }
}

// ─── Section 2: Objective (Monochrome Cards + Solid/Dense Colored Badges) ───

@Composable
private fun ObjectiveSection(
  selectedObjective: BlueprintObjectiveOption,
  onSelectObjective: (BlueprintObjectiveOption) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedPrimary by animateColorAsState(
    targetValue = selectedObjective.primaryColor,
    animationSpec = tween(durationMillis = 260),
    label = "objective_primary_color",
  )
  val animatedSecondary by animateColorAsState(
    targetValue = selectedObjective.secondaryColor,
    animationSpec = tween(durationMillis = 260),
    label = "objective_secondary_color",
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_objective_section_label),
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

        // Monochrome Telemetry Strip with solid colored category badge
        val stripCorner = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp
        val stripShape = RoundedCornerShape(stripCorner)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(stripShape)
            .background(Zinc950)
            .border(
              width = 0.8.dp,
              color = Zinc800,
              shape = stripShape,
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
              .background(
                brush = Brush.horizontalGradient(
                  colors = listOf(animatedPrimary, animatedSecondary),
                ),
              )
              .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
          ) {
            Text(
              text = selectedObjective.vectorBadge,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.0f, 7.2f, 9.2f),
              letterSpacing = 0.5.sp,
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
      .heightIn(min = (118f * metrics.scale).coerceIn(106f, 136f).dp)
      .clip(tileShape)
      .drawWithCache {
        val cornerPx = tileCorner.toPx()
        val cornerRadius = CornerRadius(cornerPx, cornerPx)
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(65, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(11.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        onDrawBehind {
          val p = selectionProgress.coerceIn(0f, 1f)
          // Clean Obsidian/Zinc card surface
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFF1C1C22).copy(alpha = 0.60f + 0.40f * p),
                Color(0xFF0E0E12).copy(alpha = 0.80f + 0.20f * p),
              ),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
          )

          if (p > 0.02f) {
            glowPaint.asFrameworkPaint().alpha = (60 * p).roundToInt().coerceIn(0, 80)
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

          // Minimalist category-colored top accent line when selected
          if (p > 0.01f) {
            val topSpanW = size.width * (0.58f * p)
            drawRoundRect(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  option.primaryColor.copy(alpha = p),
                  option.secondaryColor.copy(alpha = p),
                ),
              ),
              topLeft = Offset((size.width - topSpanW) / 2f, 0f),
              size = Size(topSpanW, 2.5.dp.toPx()),
              cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
          }

          // Monochrome PureWhite / Zinc border
          drawRoundRect(
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.12f + 0.72f * p),
              1.0f to Zinc700.copy(alpha = 0.45f + 0.40f * p),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = (0.95f + 0.50f * p).dp.toPx()),
          )
        }
      }
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics { selected = isSelected }
      .padding(
        horizontal = (10f * metrics.scale).coerceIn(8.5f, 13f).dp,
        vertical = (11f * metrics.scale).coerceIn(9.5f, 14f).dp,
      )
      .testTag(testTag),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top row: Solid/Dense Colored Vector Icon Box + Minimalist Status Dot
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val iconCorner = (8.5f * metrics.scale).coerceIn(7f, 10f).dp
        val iconShape = RoundedCornerShape(iconCorner)
        Box(
          modifier = Modifier
            .size((34f * metrics.scale).coerceIn(30f, 38f).dp)
            .clip(iconShape)
            .background(
              brush = Brush.linearGradient(
                colors = listOf(option.primaryColor, option.secondaryColor),
              ),
            )
            .border(
              width = 1.dp,
              color = if (isSelected) PureWhite.copy(alpha = 0.75f) else option.primaryColor.copy(alpha = 0.55f),
              shape = iconShape,
            ),
          contentAlignment = Alignment.Center,
        ) {
          ObjectiveVectorGlyph(
            option = option,
            tint = PureWhite,
            modifier = Modifier.size((17f * metrics.scale).coerceIn(14.5f, 19.5f).dp),
          )
        }

        // Minimalist status dot synced with category color
        Canvas(modifier = Modifier.size((7.5f * metrics.scale).coerceIn(6.5f, 9f).dp)) {
          val r = size.minDimension / 2f
          if (isSelected) {
            drawCircle(
              color = option.primaryColor,
              radius = r,
            )
          } else {
            drawCircle(
              color = Zinc700,
              radius = r * 0.78f,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height((12f * metrics.scale).coerceIn(9f, 15f).dp))

      // Clean Black & White Title & Concise Subtitle
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
        Spacer(modifier = Modifier.height(2.5.dp))
        Text(
          text = subtitle,
          color = if (isSelected) Zinc300 else Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.spScaled(11f, 9.8f, 12.4f),
          lineHeight = metrics.spScaled(13.5f, 12f, 15f),
          maxLines = 1,
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
    val strokeW = (w * 0.12f).coerceAtLeast(1.6f)

    when (option) {
      BlueprintObjectiveOption.LOSE_FAT -> {
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
          color = tint.copy(alpha = 0.65f),
          start = Offset(w * 0.16f, h * 0.84f),
          end = Offset(w * 0.84f, h * 0.84f),
          strokeWidth = strokeW * 0.85f,
          cap = StrokeCap.Round,
        )
      }

      BlueprintObjectiveOption.MAINTAIN -> {
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
          color = tint.copy(alpha = 0.65f),
          start = Offset(w * 0.16f, h * 0.86f),
          end = Offset(w * 0.84f, h * 0.86f),
          strokeWidth = strokeW * 0.85f,
          cap = StrokeCap.Round,
        )
      }
    }
  }
}

// ─── Section 3: Daily Streak Completion Threshold (Monochrome + Targeted Color Accents) ─

@Composable
private fun StreakThresholdSection(
  selectedThreshold: StreakThresholdOption,
  onSelectThreshold: (StreakThresholdOption) -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val animatedPercent by animateFloatAsState(
    targetValue = selectedThreshold.percentage.toFloat(),
    animationSpec = tween(durationMillis = 360, easing = BlueprintEaseOut),
    label = "streak_threshold_percent",
  )
  val animatedBarFraction by animateFloatAsState(
    targetValue = selectedThreshold.barFillFraction,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "streak_threshold_bar_fraction",
  )
  val animatedAccent by animateColorAsState(
    targetValue = selectedThreshold.accentColor,
    animationSpec = tween(durationMillis = 260),
    label = "streak_threshold_accent",
  )
  val animatedStartColor by animateColorAsState(
    targetValue = selectedThreshold.colorStart,
    animationSpec = tween(durationMillis = 260),
    label = "streak_threshold_start",
  )
  val animatedEndColor by animateColorAsState(
    targetValue = selectedThreshold.colorEnd,
    animationSpec = tween(durationMillis = 260),
    label = "streak_threshold_end",
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    BlueprintSectionHeaderRow(
      label = stringResource(R.string.blueprint_streak_section_label),
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
        .testTag("blueprint_streak_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((11f * metrics.scale).coerceIn(9f, 14f).dp),
      ) {
        // Top Header inside Streak Card: Animated Fire Emblem + +1 DAILY STREAK + White Percentage Readout
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
            // Left Lively Animated Fire Streak Badge in an Obsidian frame
            AnimatedFireStreakBadge(
              modifier = Modifier
                .size((44f * metrics.scale).coerceIn(38f, 50f).dp)
                .testTag("streak_fire_badge"),
            )

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "+1 DAILY STREAK",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(11.8f, 10.2f, 13.2f),
                letterSpacing = 0.8.sp,
                maxLines = 1,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = stringResource(R.string.blueprint_streak_section_subtitle),
                color = Zinc400,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = metrics.spScaled(11.1f, 9.8f, 12.5f),
                lineHeight = metrics.spScaled(15f, 13.2f, 17f),
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Right Large Animated Percentage Display in PureWhite
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

        // 5-Option Percentage Dock: 100% | 95% | 90% | 85% | 80%
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

        // Full-Range Animated Threshold Calibration Bar (20% -> 40% -> 60% -> 80% -> 100% span)
        StreakThresholdPrecisionBar(
          animatedFraction = animatedBarFraction,
          activeStartColor = animatedStartColor,
          activeEndColor = animatedEndColor,
          activeAccentColor = animatedAccent,
          modifier = Modifier
            .fillMaxWidth()
            .height((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp),
        )
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
            color = option.accentColor.copy(alpha = 0.45f).toArgb()
            maskFilter = BlurMaskFilter(safeBlurRadius(8.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
          }
        }
        onDrawBehind {
          val p = selectProgress.coerceIn(0f, 1f)
          if (p > 0.02f) {
            glowPaint.asFrameworkPaint().alpha = (75 * p).roundToInt().coerceIn(0, 95)
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
            // Crisp PureWhite selected block with subtle bottom color bar
            drawRoundRect(
              brush = Brush.verticalGradient(
                colors = listOf(
                  PureWhite.copy(alpha = p),
                  Color(0xFFE6E8EE).copy(alpha = p),
                ),
              ),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              color = option.accentColor.copy(alpha = p),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 1.2.dp.toPx()),
            )
          } else {
            drawRoundRect(
              color = Zinc900.copy(alpha = 0.75f),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
            )
            drawRoundRect(
              color = option.accentColor.copy(alpha = 0.28f),
              topLeft = Offset.Zero,
              size = size,
              cornerRadius = cornerRadius,
              style = Stroke(width = 0.75.dp.toPx()),
            )
          }

          // Minimalist bottom indicator bar showing each block's distinct color
          val barW = size.width * (0.42f + 0.18f * p)
          val barH = 2.4.dp.toPx()
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(option.colorStart, option.colorEnd),
            ),
            topLeft = Offset((size.width - barW) / 2f, size.height - barH - 2.5.dp.toPx()),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(barH, barH),
          )
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
      modifier = Modifier.padding(bottom = 3.dp),
    ) {
      Text(
        text = "${option.percentage}%",
        color = if (isSelected) ObsidianBlack else Zinc100,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        fontSize = metrics.spScaled(13.2f, 11.5f, 15f),
        letterSpacing = (-0.3).sp,
      )
      Spacer(modifier = Modifier.height(1.dp))
      Text(
        text = option.shortTag,
        color = if (isSelected) ObsidianBlack.copy(alpha = 0.78f) else option.accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(7.5f, 6.8f, 8.5f),
        letterSpacing = 0.4.sp,
      )
    }
  }
}

/**
 * Full-Range Progress Bar for 80%, 85%, 90%, 95%, 100%.
 * Maps 80% -> 20%, 85% -> 40%, 90% -> 60%, 95% -> 80%, 100% -> 100% of the bar width
 * on a clean monochrome track with a minimalist color-synced fill tip.
 */
@Composable
private fun StreakThresholdPrecisionBar(
  animatedFraction: Float,
  activeStartColor: Color,
  activeEndColor: Color,
  activeAccentColor: Color,
  modifier: Modifier = Modifier,
) {
  val stops = remember {
    listOf(
      StreakThresholdOption.PERCENT_80,
      StreakThresholdOption.PERCENT_85,
      StreakThresholdOption.PERCENT_90,
      StreakThresholdOption.PERCENT_95,
      StreakThresholdOption.PERCENT_100,
    )
  }
  Spacer(
    modifier = modifier.drawWithCache {
      val glowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = activeAccentColor.copy(alpha = 0.50f).toArgb()
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.height * 1.3f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }
      onDrawBehind {
        if (size.width <= 1f || size.height <= 1f) return@onDrawBehind
        val trackH = size.height * 0.74f
        val trackTop = (size.height - trackH) / 2f
        val r = CornerRadius(trackH / 2f, trackH / 2f)

        // Monochrome track background
        drawRoundRect(
          color = Zinc850,
          topLeft = Offset(0f, trackTop),
          size = Size(size.width, trackH),
          cornerRadius = r,
        )
        drawRoundRect(
          color = Zinc700.copy(alpha = 0.55f),
          topLeft = Offset(0f, trackTop),
          size = Size(size.width, trackH),
          cornerRadius = r,
          style = Stroke(width = 0.8.dp.toPx()),
        )

        // Wide, responsive fill width mapped from 0.20f (80%) to 1.00f (100%)
        val normalized = animatedFraction.coerceIn(0.20f, 1.00f)
        val fillW = (size.width * normalized).coerceIn(trackH, size.width)

        drawIntoCanvas { canvas ->
          canvas.drawRoundRect(
            0f,
            trackTop,
            fillW,
            trackTop + trackH,
            r.x,
            r.y,
            glowPaint,
          )
        }

        drawRoundRect(
          brush = Brush.horizontalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.90f),
              activeStartColor,
              activeEndColor,
            ),
            startX = 0f,
            endX = fillW,
          ),
          topLeft = Offset(0f, trackTop),
          size = Size(fillW, trackH),
          cornerRadius = r,
        )

        // 5 evenly spaced calibration stop markers at 20%, 40%, 60%, 80%, 100%
        stops.forEach { stop ->
          val stopX = (size.width * stop.barFillFraction).coerceIn(trackH / 2f, size.width - trackH / 2f)
          val isReached = stop.barFillFraction <= normalized + 0.01f
          drawCircle(
            color = if (isReached) ObsidianBlack.copy(alpha = 0.60f) else Zinc600,
            radius = (trackH * 0.22f).coerceAtLeast(1.3f),
            center = Offset(stopX, size.height / 2f),
          )
        }

        // Crisp white leading indicator node with active accent core
        val thumbX = fillW.coerceIn(size.height / 2f, size.width - size.height / 2f)
        val thumbCenter = Offset(thumbX, size.height / 2f)
        drawCircle(
          color = PureWhite,
          radius = size.height * 0.44f,
          center = thumbCenter,
        )
        drawCircle(
          color = activeAccentColor,
          radius = size.height * 0.22f,
          center = thumbCenter,
        )
      }
    },
  )
}

/**
 * Precision Obsidian-Glass Chamber housing a Realistic Volumetric Multi-Tongue Flame Emblem.
 * Combines a micro-burner ignition base, subtle oxygen-blue ionization rim, deep crimson-amber
 * aerodynamic S-curve mantle, solar-gold plasma body, incandescent white-hot heart, and rising micro-embers.
 */
@Composable
private fun AnimatedFireStreakBadge(
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "streak_fire_badge_animation")
  val flameFlicker by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 860, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "flame_flicker",
  )
  val flameSway by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1340, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "flame_sway",
  )
  val secondaryWave by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1020, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "secondary_wave",
  )
  val emberRise by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1750, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "ember_rise",
  )

  Spacer(
    modifier = modifier.drawWithCache {
      val cornerPx = (size.minDimension * 0.24f).coerceAtLeast(7f)
      val cornerRadius = CornerRadius(cornerPx, cornerPx)
      val badgeClipPath = Path().apply {
        addRoundRect(
          RoundRect(
            rect = Rect(0f, 0f, size.width, size.height),
            cornerRadius = cornerRadius,
          ),
        )
      }

      val outerCoronaPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = Color(0xFFFF4500).copy(alpha = 0.42f).toArgb()
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.minDimension * 0.28f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }
      val innerSolarBloomPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = Color(0xFFFFAA00).copy(alpha = 0.58f).toArgb()
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.minDimension * 0.14f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }
      val whiteHeartBloomPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = Color(0xFFFFF6CC).copy(alpha = 0.72f).toArgb()
          maskFilter = BlurMaskFilter(
            safeBlurRadius(size.minDimension * 0.08f),
            BlurMaskFilter.Blur.NORMAL,
          )
        }
      }

      onDrawBehind {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@onDrawBehind

        val flicker = flameFlicker
        val sway = flameSway
        val wave2 = secondaryWave

        // 1. Deep Obsidian-Carbon Chamber Background
        drawRoundRect(
          brush = Brush.verticalGradient(
            0.0f to Color(0xFF151519),
            0.55f to Color(0xFF0E0D11),
            1.0f to Color(0xFF181110),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
        )

        clipPath(badgeClipPath) {
          // Subtle ambient floor reflection from the combustion core
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFFFF5500).copy(alpha = 0.26f + 0.06f * flicker),
                Color(0xFFB91C1C).copy(alpha = 0.10f),
                Color.Transparent,
              ),
              center = Offset(w * 0.50f, h * 0.82f),
              radius = w * 0.58f,
            ),
            radius = w * 0.58f,
            center = Offset(w * 0.50f, h * 0.82f),
          )

          // Precision instrument calibration ring in the background
          drawCircle(
            color = PureWhite.copy(alpha = 0.04f),
            radius = w * 0.36f,
            center = Offset(w * 0.50f, h * 0.54f),
            style = Stroke(width = 0.7.dp.toPx()),
          )

          // Volumetric thermal bloom behind the flame
          drawIntoCanvas { canvas ->
            canvas.drawOval(
              w * 0.20f,
              h * 0.22f,
              w * 0.80f,
              h * 0.86f,
              outerCoronaPaint,
            )
            canvas.drawCircle(
              Offset(w * (0.50f + 0.015f * sway), h * 0.60f),
              w * (0.21f + 0.025f * flicker),
              innerSolarBloomPaint,
            )
          }

          // 2. Sculpted Left Secondary Flame Tongue (peels off and curls upward)
          val leftTipX = w * (0.25f - 0.020f * wave2)
          val leftTipY = h * (0.37f - 0.030f * flicker)
          val leftTongue = Path().apply {
            moveTo(w * 0.44f, h * 0.78f)
            cubicTo(
              w * 0.30f,
              h * 0.74f,
              w * 0.19f,
              h * 0.61f,
              w * (0.21f + 0.015f * sway),
              h * 0.48f,
            )
            cubicTo(
              w * 0.22f,
              h * 0.42f,
              w * 0.24f,
              h * 0.39f,
              leftTipX,
              leftTipY,
            )
            cubicTo(
              w * 0.28f,
              h * 0.43f,
              w * 0.34f,
              h * 0.48f,
              w * 0.39f,
              h * 0.56f,
            )
            close()
          }
          drawPath(
            path = leftTongue,
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFFFBBF24),
                Color(0xFFEA580C),
                Color(0xFF9F1239),
              ),
              startY = leftTipY,
              endY = h * 0.79f,
            ),
          )

          // 3. Sculpted Right Aerodynamic Crest Tongue
          val rightTipX = w * (0.76f + 0.022f * sway)
          val rightTipY = h * (0.41f + 0.025f * wave2)
          val rightTongue = Path().apply {
            moveTo(w * 0.56f, h * 0.78f)
            cubicTo(
              w * 0.71f,
              h * 0.74f,
              w * 0.81f,
              h * 0.62f,
              w * (0.79f + 0.015f * wave2),
              h * 0.50f,
            )
            cubicTo(
              w * 0.78f,
              h * 0.45f,
              w * 0.77f,
              h * 0.43f,
              rightTipX,
              rightTipY,
            )
            cubicTo(
              w * 0.72f,
              h * 0.47f,
              w * 0.66f,
              h * 0.52f,
              w * 0.60f,
              h * 0.60f,
            )
            close()
          }
          drawPath(
            path = rightTongue,
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFFF59E0B),
                Color(0xFFD9460F),
                Color(0xFF881337),
              ),
              startY = rightTipY,
              endY = h * 0.79f,
            ),
          )

          // 4. Primary Outer S-Curve Flame Mantle (Deep Ruby -> Tangelo -> Solar Gold Tip)
          val mainTipX = w * (0.52f + 0.038f * sway)
          val mainTipY = h * (0.13f - 0.032f * flicker)
          val outerMantle = Path().apply {
            moveTo(mainTipX, mainTipY)
            // Right windward S-curve descending to the right belly
            cubicTo(
              w * (0.59f + 0.025f * sway),
              h * 0.23f,
              w * (0.76f + 0.018f * flicker),
              h * 0.43f,
              w * 0.73f,
              h * 0.64f,
            )
            // Right lower belly curving into the burner base
            cubicTo(
              w * 0.70f,
              h * 0.77f,
              w * 0.61f,
              h * 0.83f,
              w * 0.50f,
              h * 0.83f,
            )
            // Left lower belly curving upward
            cubicTo(
              w * 0.39f,
              h * 0.83f,
              w * 0.28f,
              h * 0.75f,
              w * 0.27f,
              h * 0.60f,
            )
            // Left S-curve waist sweeping up toward the tapered apex
            cubicTo(
              w * (0.26f - 0.018f * flicker),
              h * 0.44f,
              w * (0.40f + 0.020f * sway),
              h * 0.31f,
              w * (0.46f + 0.030f * sway),
              h * 0.21f,
            )
            cubicTo(
              w * (0.49f + 0.034f * sway),
              h * 0.16f,
              w * (0.50f + 0.036f * sway),
              h * 0.14f,
              mainTipX,
              mainTipY,
            )
            close()
          }
          drawPath(
            path = outerMantle,
            brush = Brush.verticalGradient(
              0.0f to Color(0xFFFFE57F),
              0.24f to Color(0xFFFF9E00),
              0.62f to Color(0xFFFF4800),
              0.88f to Color(0xFFD90429),
              1.0f to Color(0xFF7F1D1D),
              startY = mainTipY,
              endY = h * 0.84f,
            ),
          )

          // 5. Mid-Layer Solar Plasma Plume (Nested S-Curve with counter-harmonic motion)
          val midTipX = w * (0.49f - 0.024f * wave2 + 0.015f * sway)
          val midTipY = h * (0.26f - 0.028f * flicker)
          val midPlasma = Path().apply {
            moveTo(midTipX, midTipY)
            cubicTo(
              w * (0.58f + 0.015f * sway),
              h * 0.36f,
              w * 0.66f,
              h * 0.52f,
              w * 0.63f,
              h * 0.69f,
            )
            cubicTo(
              w * 0.61f,
              h * 0.78f,
              w * 0.55f,
              h * 0.82f,
              w * 0.50f,
              h * 0.82f,
            )
            cubicTo(
              w * 0.44f,
              h * 0.82f,
              w * 0.36f,
              h * 0.76f,
              w * 0.35f,
              h * 0.65f,
            )
            cubicTo(
              w * 0.34f,
              h * 0.50f,
              w * (0.43f + 0.015f * sway),
              h * 0.38f,
              midTipX,
              midTipY,
            )
            close()
          }
          drawPath(
            path = midPlasma,
            brush = Brush.verticalGradient(
              0.0f to Color(0xFFFFF8CC),
              0.38f to Color(0xFFFFD166),
              0.76f to Color(0xFFFF8500),
              1.0f to Color(0xFFE85D04),
              startY = midTipY,
              endY = h * 0.82f,
            ),
          )

          // 6. Subtle Oxygen-Blue Ionization Cushion at the Immediate Combustion Base
          val ionBase = Path().apply {
            moveTo(w * 0.37f, h * 0.76f)
            cubicTo(
              w * 0.39f,
              h * 0.83f,
              w * 0.61f,
              h * 0.83f,
              w * 0.63f,
              h * 0.76f,
            )
            cubicTo(
              w * 0.59f,
              h * 0.80f,
              w * 0.41f,
              h * 0.80f,
              w * 0.37f,
              h * 0.76f,
            )
            close()
          }
          drawPath(
            path = ionBase,
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.45f),
                Color(0xFF60A5FA).copy(alpha = 0.70f),
                Color(0xFF38BDF8).copy(alpha = 0.45f),
              ),
              startX = w * 0.36f,
              endX = w * 0.64f,
            ),
          )

          // 7. Incandescent White-Hot Ignition Core
          val coreTipX = w * (0.50f + 0.014f * sway)
          val coreTipY = h * (0.45f - 0.022f * flicker)
          val innerHeart = Path().apply {
            moveTo(coreTipX, coreTipY)
            cubicTo(
              w * 0.56f,
              h * 0.54f,
              w * 0.58f,
              h * 0.66f,
              w * 0.55f,
              h * 0.76f,
            )
            cubicTo(
              w * 0.53f,
              h * 0.80f,
              w * 0.47f,
              h * 0.80f,
              w * 0.45f,
              h * 0.76f,
            )
            cubicTo(
              w * 0.42f,
              h * 0.66f,
              w * 0.44f,
              h * 0.54f,
              coreTipX,
              coreTipY,
            )
            close()
          }
          drawIntoCanvas { canvas ->
            canvas.drawCircle(
              Offset(w * 0.50f, h * 0.68f),
              w * (0.11f + 0.015f * flicker),
              whiteHeartBloomPaint,
            )
          }
          drawPath(
            path = innerHeart,
            brush = Brush.verticalGradient(
              0.0f to Color(0xFFFFFBEB),
              0.45f to PureWhite,
              0.85f to Color(0xFFFFE599),
              1.0f to Color(0xFFBAE6FD),
              startY = coreTipY,
              endY = h * 0.80f,
            ),
          )

          // 8. Delicate Specular Rim Highlight along the primary flame's windward contour
          val specularRim = Path().apply {
            moveTo(mainTipX, mainTipY + h * 0.02f)
            cubicTo(
              w * (0.46f + 0.028f * sway),
              h * 0.21f,
              w * (0.28f - 0.015f * flicker),
              h * 0.43f,
              w * 0.29f,
              h * 0.61f,
            )
          }
          drawPath(
            path = specularRim,
            brush = Brush.verticalGradient(
              colors = listOf(
                PureWhite.copy(alpha = 0.72f),
                Color(0xFFFFD166).copy(alpha = 0.35f),
                Color.Transparent,
              ),
              startY = mainTipY,
              endY = h * 0.62f,
            ),
            style = Stroke(width = (w * 0.022f).coerceAtLeast(0.9f), cap = StrokeCap.Round),
          )

          // 9. Staggered Rising Micro-Embers (3 particles along organic convection paths)
          val p1 = emberRise
          val p2 = (emberRise + 0.37f) % 1f
          val p3 = (emberRise + 0.71f) % 1f

          val e1Alpha = sin(p1 * Math.PI.toFloat()).coerceIn(0f, 1f)
          drawCircle(
            color = Color(0xFFFFE57F).copy(alpha = e1Alpha * 0.88f),
            radius = (w * 0.024f * (1f - 0.35f * p1)).coerceAtLeast(0.8f),
            center = Offset(
              x = w * (0.37f + 0.05f * sin(p1 * 4.5f) + 0.02f * sway),
              y = h * (0.44f - 0.28f * p1),
            ),
          )

          val e2Alpha = sin(p2 * Math.PI.toFloat()).coerceIn(0f, 1f)
          drawCircle(
            color = Color(0xFFFF9E00).copy(alpha = e2Alpha * 0.82f),
            radius = (w * 0.021f * (1f - 0.35f * p2)).coerceAtLeast(0.7f),
            center = Offset(
              x = w * (0.65f - 0.04f * sin(p2 * 4.2f) + 0.02f * wave2),
              y = h * (0.48f - 0.30f * p2),
            ),
          )

          val e3Alpha = sin(p3 * Math.PI.toFloat()).coerceIn(0f, 1f)
          drawCircle(
            color = PureWhite.copy(alpha = e3Alpha * 0.75f),
            radius = (w * 0.017f).coerceAtLeast(0.6f),
            center = Offset(
              x = w * (0.51f + 0.03f * sway),
              y = h * (0.28f - 0.16f * p3),
            ),
          )

          // 10. Precision Combustion Burner Pedestal at Base (grounds the flame physically)
          val pedestalW = w * 0.46f
          val pedestalH = (h * 0.055f).coerceAtLeast(2f)
          val pedestalTop = h * 0.835f
          val pedestalLeft = (w - pedestalW) / 2f
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Zinc800,
                Zinc500,
                Zinc800,
              ),
              startX = pedestalLeft,
              endX = pedestalLeft + pedestalW,
            ),
            topLeft = Offset(pedestalLeft, pedestalTop),
            size = Size(pedestalW, pedestalH),
            cornerRadius = CornerRadius(pedestalH / 2f, pedestalH / 2f),
          )
          // Glowing molten ignition slot on top of the burner pedestal
          val slotW = pedestalW * 0.68f
          val slotLeft = (w - slotW) / 2f
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                Color(0xFFFF9E00),
                PureWhite,
                Color(0xFFFF9E00),
                Color.Transparent,
              ),
              startX = slotLeft,
              endX = slotLeft + slotW,
            ),
            start = Offset(slotLeft, pedestalTop),
            end = Offset(slotLeft + slotW, pedestalTop),
            strokeWidth = (h * 0.022f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
          )

          // 11. Diagonal Sapphire-Crystal Glass Sheen across the upper chamber
          val glassSheen = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.78f, 0f)
            lineTo(w * 0.28f, h * 0.42f)
            lineTo(0f, h * 0.42f)
            close()
          }
          drawPath(
            path = glassSheen,
            brush = Brush.linearGradient(
              colors = listOf(
                PureWhite.copy(alpha = 0.09f),
                PureWhite.copy(alpha = 0.02f),
                Color.Transparent,
              ),
              start = Offset.Zero,
              end = Offset(w * 0.55f, h * 0.42f),
            ),
          )
        }

        // 12. Outer Dual-Tone Metallic & Thermal Rim Bezel
        drawRoundRect(
          brush = Brush.linearGradient(
            0.0f to PureWhite.copy(alpha = 0.30f),
            0.45f to Zinc600.copy(alpha = 0.55f),
            1.0f to Color(0xFFFF7A29).copy(alpha = 0.50f),
            start = Offset.Zero,
            end = Offset(w, h),
          ),
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = 1.05.dp.toPx()),
        )
      }
    },
  )
}

// ─── Live Blueprint Protocol Matrix Summary Card (Monochrome + Interactive Scroll) ─

@Composable
private fun BlueprintSummaryMatrixCard(
  subjectName: String,
  ageYears: Int,
  selectedActivityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  selectedObjective: BlueprintObjectiveOption,
  selectedTrainingModality: TrainingModalityOption,
  selectedWorkoutDuration: WorkoutDurationOption,
  selectedExperienceLevel: ExperienceLevelOption,
  selectedTargetPace: TargetPaceOption,
  selectedDayOne: DayOfWeekOption,
  selectedScheduleCadence: ScheduleCadenceOption,
  selectedMealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  selectedSodiumFrequency: SodiumFrequencyOption,
  selectedStreakThreshold: StreakThresholdOption,
  onClickActivityTile: () -> Unit,
  onClickObjectiveTile: () -> Unit,
  onClickTrainingTile: () -> Unit,
  onClickPaceCycleTile: () -> Unit,
  onClickNutritionTile: () -> Unit,
  onClickStreakTile: () -> Unit,
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
  val activitySubValue = when (selectedActivityLevel) {
    ActivityLevelOption.SEDENTARY -> "0W / 7R • SEDENTARY"
    ActivityLevelOption.LIGHT, ActivityLevelOption.MODERATE -> {
      val wCount = selectedWorkoutDays.size.coerceIn(1, 6)
      "${wCount}W / ${7 - wCount}R • ${DayOfWeekOption.entries.filter { it in selectedWorkoutDays }.joinToString(",") { it.shortCode }}"
    }
    ActivityLevelOption.VERY_ACTIVE -> "6W / 1R • ${selectedActivityLevel.cadenceBadge}"
  }
  val objectiveLabel = when (selectedObjective) {
    BlueprintObjectiveOption.LOSE_FAT -> stringResource(R.string.blueprint_objective_lose_fat_title)
    BlueprintObjectiveOption.MAINTAIN -> stringResource(R.string.blueprint_objective_maintain_title)
    BlueprintObjectiveOption.BUILD_MUSCLE -> stringResource(R.string.blueprint_objective_build_muscle_title)
  }
  val experienceLabel = when (selectedExperienceLevel) {
    ExperienceLevelOption.BEGINNER -> stringResource(R.string.blueprint_exp_beginner)
    ExperienceLevelOption.INTERMEDIATE -> stringResource(R.string.blueprint_exp_intermediate)
    ExperienceLevelOption.ADVANCED -> stringResource(R.string.blueprint_exp_advanced)
  }
  val cadenceLabel = if (selectedActivityLevel == ActivityLevelOption.SEDENTARY) {
    stringResource(R.string.blueprint_cadence_uniform)
  } else {
    when (selectedScheduleCadence) {
      ScheduleCadenceOption.UNIFORM_DAILY -> stringResource(R.string.blueprint_cadence_uniform)
      ScheduleCadenceOption.REST_DAYS -> stringResource(R.string.blueprint_cadence_rest_days)
    }
  }
  val sodiumLabel = when (selectedSodiumFrequency) {
    SodiumFrequencyOption.RARELY -> stringResource(R.string.blueprint_sodium_rarely)
    SodiumFrequencyOption.SOMETIMES -> stringResource(R.string.blueprint_sodium_sometimes)
    SodiumFrequencyOption.OFTEN -> stringResource(R.string.blueprint_sodium_often)
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
      verticalArrangement = Arrangement.spacedBy((8.5f * metrics.scale).coerceIn(7f, 11.5f).dp),
    ) {
      // Top Row: Luminous White Dot + BLUEPRINT PROTOCOL MATRIX
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
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
      }

      HorizontalDivider(color = Zinc850, thickness = 0.8.dp)

      // Row 1: Activity Level | Objective (2-Column Precision Layout)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 10.5f).dp),
      ) {
        SummaryMetricTile(
          label = "ACTIVITY LEVEL",
          value = activityLabel,
          subValue = activitySubValue,
          dotColor = selectedActivityLevel.accentColor,
          onClick = onClickActivityTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_activity",
        )
        SummaryMetricTile(
          label = "OBJECTIVE",
          value = objectiveLabel,
          subValue = selectedObjective.vectorBadge,
          dotColor = selectedObjective.primaryColor,
          onClick = onClickObjectiveTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_objective",
        )
      }

      // Row 2: Training Architecture | Metabolic Pace & Cycle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 10.5f).dp),
      ) {
        SummaryMetricTile(
          label = "TRAINING",
          value = "${selectedTrainingModality.shortLabel} • ${selectedWorkoutDuration.displayHours}",
          subValue = "${experienceLabel.uppercase()} • ${selectedExperienceLevel.trainingAgeTag}",
          dotColor = selectedTrainingModality.accentColor,
          onClick = onClickTrainingTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_training",
        )
        SummaryMetricTile(
          label = "PACE & CYCLE",
          value = "${selectedTargetPace.title} • D1 ${selectedDayOne.shortCode}",
          subValue = cadenceLabel.uppercase(),
          dotColor = selectedTargetPace.accentColor,
          onClick = onClickPaceCycleTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_pace_cycle",
        )
      }

      // Row 3: Nutrition & Hydration | Daily Streak Rule
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6.5f, 10.5f).dp),
      ) {
        val diureticStatus = if (caffeineAlcoholYes) "YES" else "NO"
        SummaryMetricTile(
          label = "NUTRITION",
          value = "${selectedMealFrequency.displayCount} Meals • $sodiumLabel",
          subValue = "CAFF/ALC: $diureticStatus • ${selectedMealFrequency.cadenceTag}",
          dotColor = selectedSodiumFrequency.accentColor,
          onClick = onClickNutritionTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_nutrition",
        )
        SummaryMetricTile(
          label = "STREAK RULE",
          value = "${selectedStreakThreshold.percentage}% Daily Goal",
          subValue = "${selectedStreakThreshold.shortTag} • +1 STREAK",
          dotColor = selectedStreakThreshold.accentColor,
          onClick = onClickStreakTile,
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "matrix_tile_streak",
        )
      }

      HorizontalDivider(color = Zinc850, thickness = 0.8.dp)

      // Subject (left) & Age (right) footer row in identical format
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "AGE: $ageYears YRS",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          letterSpacing = 0.6.sp,
          maxLines = 1,
          modifier = Modifier.testTag("matrix_footer_age"),
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
  dotColor: Color,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
  testTag: String,
) {
  val animatedDot by animateColorAsState(
    targetValue = dotColor,
    animationSpec = tween(durationMillis = 260),
    label = "summary_tile_dot_$label",
  )
  val tileCorner = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp
  val tileShape = RoundedCornerShape(tileCorner)

  Column(
    modifier = modifier
      .clip(tileShape)
      .background(Zinc950)
      .border(
        width = 0.8.dp,
        color = Zinc800,
        shape = tileShape,
      )
      .clickable(
        role = Role.Button,
        onClick = onClick,
      )
      .padding(
        horizontal = (8.5f * metrics.scale).coerceIn(7f, 11f).dp,
        vertical = (8f * metrics.scale).coerceIn(6.5f, 10f).dp,
      )
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        color = Zinc500,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(8.2f, 7.2f, 9.4f),
        letterSpacing = 0.6.sp,
        maxLines = 1,
      )
      Canvas(modifier = Modifier.size(5.dp)) {
        drawCircle(color = animatedDot)
      }
    }
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
      fontSize = metrics.spScaled(8.1f, 7.1f, 9.2f),
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
  isQuickEditMode: Boolean,
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
          text = stringResource(
            if (isQuickEditMode) R.string.calib_cta_return_summary else R.string.blueprint_cta_complete,
          ),
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
