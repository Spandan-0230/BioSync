package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.ProteinEnd
import com.example.ui.theme.ProteinMid
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsEnd
import com.example.ui.theme.StepsMid
import com.example.ui.theme.StepsStart
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val PSZinc100 = Color(0xFFF4F4F5)
private val PSZinc200 = Color(0xFFE4E4E7)
private val PSZinc300 = Color(0xFFD4D4D8)
private val PSZinc400 = Color(0xFFA1A1AA)
private val PSZinc500 = Color(0xFF71717A)
private val PSZinc600 = Color(0xFF52525B)
private val PSZinc700 = Color(0xFF3F3F46)
private val PSZinc800 = Color(0xFF27272A)
private val PSZinc850 = Color(0xFF18181C)
private val PSZinc900 = Color(0xFF121216)
private val PSZinc950 = Color(0xFF0A0A0D)

private val SummaryEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private fun safeSummaryBlur(radius: Float): Float = radius.coerceAtLeast(0.5f)

private enum class DirectEditMetricTarget {
  NONE,
  CALORIES,
  PROTEIN,
  CARBS,
  FATS,
  HYDRATION,
  STEPS,
}

private enum class FocusTargetMetric(
  val shortTag: String,
  val fullLabel: String,
  val gradientColors: List<Color>,
) {
  CALORIES("CAL", "Daily Calories", listOf(CaloriesStart, CaloriesMid, CaloriesEnd)),
  PROTEIN("PRO", "Daily Protein", listOf(ProteinStart, ProteinMid, ProteinEnd)),
  CARBS("CARB", "Daily Carbs", listOf(CarbsStart, CarbsMid, CarbsEnd)),
  FATS("FAT", "Daily Fats", listOf(FatsStart, FatsMid, FatsEnd)),
  HYDRATION("H2O", "Daily Water", listOf(HydrationStart, HydrationMid, HydrationEnd)),
  STEPS("STEPS", "Daily Steps", listOf(StepsStart, StepsMid, StepsEnd)),
}

@Composable
fun ProtocolSummaryScreen(
  subjectName: String,
  selectedSex: String,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  ageYears: Int,
  heightCm: Float,
  weightKg: Float,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption> = DailyTargetsEngine.defaultWorkoutDaysFor(activityLevel),
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  dayOne: DayOfWeekOption,
  scheduleCadence: ScheduleCadenceOption,
  onSelectScheduleCadence: (ScheduleCadenceOption) -> Unit,
  mealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
  streakThreshold: StreakThresholdOption,
  climateBand: ClimateBandOption,
  onSelectClimateBand: (ClimateBandOption) -> Unit,
  bodyFatOverrideEnabled: Boolean,
  bodyFatPercent: Float,
  onToggleBodyFatOverride: (Boolean) -> Unit,
  onBodyFatPercentChange: (Float) -> Unit,
  customOverride: CustomTargetsOverride,
  onUpdateCustomOverride: (CustomTargetsOverride) -> Unit,
  protocolLockedIn: Boolean,
  onLockInProtocol: () -> Unit,
  onTeleportToStep01: () -> Unit,
  onTeleportToStep02: () -> Unit,
  onTeleportToStep03: (focusSection: String?) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  val haptics = rememberBioSyncHaptics()
  val coroutineScope = rememberCoroutineScope()
  val headerStepPhase = remember { Animatable(0f) }
  val borderFlashPhase = remember { Animatable(0f) }

  val effectiveScheduleCadence = if (activityLevel == ActivityLevelOption.SEDENTARY) {
    ScheduleCadenceOption.UNIFORM_DAILY
  } else {
    scheduleCadence
  }

  // Tracks whether Workout Day or Rest Day (and optional specific weekday) is focused when Workout / Rest Split is active
  var splitDayFocus by remember { mutableStateOf(SplitDayFocus.WORKOUT_DAY) }
  var inspectedDayOfWeek by remember { mutableStateOf<DayOfWeekOption?>(null) }

  // Mathematical Calculation Report (Synced with Uniform vs Workout/Rest Split, selectedWorkoutDays & active SplitDayFocus)
  val report = remember(
    ageYears,
    selectedSex,
    heightCm,
    weightKg,
    activityLevel,
    selectedWorkoutDays,
    occupationLoad,
    objective,
    trainingModality,
    workoutDuration,
    experienceLevel,
    targetPace,
    effectiveScheduleCadence,
    splitDayFocus,
    mealFrequency,
    caffeineAlcoholYes,
    sodiumFrequency,
    climateBand,
    bodyFatOverrideEnabled,
    bodyFatPercent,
  ) {
    DailyTargetsEngine.calculate(
      ageYears = ageYears,
      biologicalSex = selectedSex,
      heightCm = heightCm,
      weightKg = weightKg,
      activityLevel = activityLevel,
      occupationLoad = occupationLoad,
      objective = objective,
      trainingModality = trainingModality,
      workoutDuration = workoutDuration,
      experienceLevel = experienceLevel,
      targetPace = targetPace,
      scheduleCadence = effectiveScheduleCadence,
      mealFrequency = mealFrequency,
      caffeineAlcoholYes = caffeineAlcoholYes,
      sodiumFrequency = sodiumFrequency,
      climateBand = climateBand,
      bodyFatPercentOverride = if (bodyFatOverrideEnabled) bodyFatPercent else null,
      splitDayFocus = splitDayFocus,
      selectedWorkoutDays = selectedWorkoutDays,
    )
  }

  // Evaluate out-of-range health impact alerts whenever Custom Mode is active
  val healthAlerts = remember(report, customOverride) {
    DailyTargetsEngine.evaluateHealthImpacts(
      report = report,
      custom = customOverride,
    )
  }

  var showDisclaimerModal by remember { mutableStateOf(false) }
  var activeDirectEditTarget by remember { mutableStateOf(DirectEditMetricTarget.NONE) }
  var selectedFocusMetric by remember { mutableStateOf(FocusTargetMetric.CALORIES) }
  var inspectedCalculationTopic by remember { mutableStateOf<CalculationTopic?>(null) }

  // Animate STEP 04 OF 04 header progress bar from 0.75f to 1.00f
  LaunchedEffect(Unit) {
    headerStepPhase.snapTo(0f)
    headerStepPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 760, easing = SummaryEaseOut),
    )
  }

  val triggerFlashAndLockIn: () -> Unit = {
    haptics.confirm()
    coroutineScope.launch {
      borderFlashPhase.snapTo(0f)
      borderFlashPhase.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 680, easing = SummaryEaseOut),
      )
      borderFlashPhase.snapTo(0f)
      onLockInProtocol()
    }
  }

  val onPrimaryCtaClick: () -> Unit = {
    if (customOverride.isCustomMode && healthAlerts.isNotEmpty()) {
      haptics.warning()
      showDisclaimerModal = true
    } else {
      triggerFlashAndLockIn()
    }
  }

  val resetToRecommended: () -> Unit = {
    haptics.confirm()
    onUpdateCustomOverride(
      CustomTargetsOverride(
        isCustomMode = false,
        caloriesKcal = report.recommendedCaloriesKcal,
        proteinGrams = report.recommendedProteinGrams,
        carbsGrams = report.recommendedCarbsGrams,
        fatsGrams = report.recommendedFatsGrams,
        hydrationMl = report.primaryRecommendedWaterMl,
        stepsCount = report.recommendedSteps,
      )
    )
    showDisclaimerModal = false
  }

  val switchToCustomMode: () -> Unit = {
    haptics.selection()
    if (!customOverride.isCustomMode) {
      onUpdateCustomOverride(
        CustomTargetsOverride(
          isCustomMode = true,
          caloriesKcal = report.recommendedCaloriesKcal,
          proteinGrams = report.recommendedProteinGrams,
          carbsGrams = report.recommendedCarbsGrams,
          fatsGrams = report.recommendedFatsGrams,
          hydrationMl = report.primaryRecommendedWaterMl,
          stepsCount = report.recommendedSteps,
        )
      )
    }
  }

  val headerStepPhaseProvider = remember(headerStepPhase) { { headerStepPhase.value } }
  val borderFlashProvider = remember(borderFlashPhase) { { borderFlashPhase.value } }

  // Active values displayed on the dashboard (Recommended vs Custom)
  val activeCalories = if (customOverride.isCustomMode) customOverride.caloriesKcal else report.recommendedCaloriesKcal
  val activeProtein = if (customOverride.isCustomMode) customOverride.proteinGrams else report.recommendedProteinGrams
  val activeCarbs = if (customOverride.isCustomMode) customOverride.carbsGrams else report.recommendedCarbsGrams
  val activeFats = if (customOverride.isCustomMode) customOverride.fatsGrams else report.recommendedFatsGrams
  val activeHydration = if (customOverride.isCustomMode) customOverride.hydrationMl else report.primaryRecommendedWaterMl
  val activeSteps = if (customOverride.isCustomMode) customOverride.stepsCount else report.recommendedSteps

  val syncCaloriesToMacrosAction: () -> Unit = {
    haptics.confirm()
    val syncedKcal = ((4 * activeProtein + 4 * activeCarbs + 9 * activeFats) / 5.0).roundToInt() * 5
    onUpdateCustomOverride(customOverride.copy(isCustomMode = true, caloriesKcal = syncedKcal))
  }

  val openCalculationBreakdown: (CalculationTopic) -> Unit = { topic ->
    haptics.confirm()
    inspectedCalculationTopic = topic
  }

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
        val w = maxWidth.value
        val h = maxHeight.value
        val isExpanded = w >= 600f
        val widthFactor = (w / 392f).coerceIn(0.86f, 1.20f)
        val heightFactor = (h / 830f).coerceIn(0.78f, 1.14f)
        val scale = min(widthFactor, heightFactor * 1.06f).coerceIn(0.82f, 1.16f)
        val fontComp = if (fontScale > 1.12f) (1.12f / fontScale).coerceIn(0.76f, 1f) else 1f
        BlueprintResponsiveMetrics(
          scale = scale,
          fontCompensation = fontComp,
          isExpanded = isExpanded,
          contentMaxWidth = if (isExpanded) 560.dp else 480.dp,
          horizontalPadding = (20f * widthFactor).coerceIn(16f, 28f).dp,
          topContentPadding = (8f * heightFactor).coerceIn(5f, 14f).dp,
          bottomContentPadding = (18f * heightFactor).coerceIn(14f, 26f).dp,
          headerHeight = (46f * scale).coerceIn(42f, 54f).dp,
          titleFontSize = ((26f * scale).coerceIn(22f, 30f) * fontComp).sp,
          subtitleFontSize = ((13.0f * scale).coerceIn(11.5f, 14.8f) * fontComp).sp,
          subtitleLineHeight = ((19.0f * scale).coerceIn(16.5f, 21.5f) * fontComp).sp,
          titleToFirstFieldGap = (15f * heightFactor).coerceIn(11f, 20f).dp,
          sectionGap = (16f * heightFactor).coerceIn(12f, 21f).dp,
          cardCornerRadius = (14f * scale).coerceIn(12f, 18f).dp,
          cardPadding = (15.5f * scale).coerceIn(13f, 20f).dp,
          ctaHeight = (52f * scale).coerceIn(48f, 56f).dp,
          cardToCtaGap = (18f * heightFactor).coerceIn(14f, 24f).dp,
        )
      }

      val scrollState = rememberScrollState()

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
          .testTag("protocol_summary_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // ── 1. Top Header Bar (BioSync + STEP 04 OF 04 + Full 4/4 Shiny Glow Bar) ──
        SummaryStepFourHeaderBar(
          onBackClick = {
            haptics.selection()
            onNavigateBack()
          },
          phaseProvider = headerStepPhaseProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // ── 2. Eyebrow, Title & Subtitle ──
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(R.string.summary_eyebrow),
            color = PSZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(11.0f, 9.6f, 12.6f),
            letterSpacing = 1.4.sp,
          )
          Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
          Text(
            text = stringResource(R.string.summary_title),
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.titleFontSize,
            letterSpacing = (-0.55).sp,
            modifier = Modifier.testTag("summary_title_text"),
          )
          Spacer(modifier = Modifier.height((5.5f * metrics.scale).coerceIn(4f, 8f).dp))
          Text(
            text = stringResource(R.string.summary_subtitle),
            color = PSZinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = metrics.subtitleFontSize,
            lineHeight = metrics.subtitleLineHeight,
          )
        }

        Spacer(modifier = Modifier.height(metrics.titleToFirstFieldGap))

        // ── 3. Safety Gate Notices (Teen 13–17, Under 13, BMI < 18.5, Calorie Floor) ──
        if (report.safetyNotices.isNotEmpty()) {
          SafetyGatesNoticeBannerList(
            notices = report.safetyNotices,
            metrics = metrics,
          )
          Spacer(modifier = Modifier.height(metrics.sectionGap))
        }

        // ── 4. Pure Black & White Recommended vs Custom Edit Mode Switcher ──
        TargetModeSelectorCard(
          isCustomMode = customOverride.isCustomMode,
          alertCount = healthAlerts.size,
          onSelectRecommended = resetToRecommended,
          onSelectCustom = switchToCustomMode,
          onInspectAlerts = {
            haptics.selection()
            showDisclaimerModal = true
          },
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // ── 5. Simple Real-Life Health Impact Advisory Banner (ONLY when Custom Edit values exit safe range) ──
        if (customOverride.isCustomMode && healthAlerts.isNotEmpty()) {
          OutOfRangeHealthAdvisoryBanner(
            alerts = healthAlerts,
            onOpenFullDisclaimer = {
              haptics.selection()
              showDisclaimerModal = true
            },
            onSyncCaloriesFromMacros = syncCaloriesToMacrosAction,
            onRestoreRecommended = resetToRecommended,
            metrics = metrics,
          )
          Spacer(modifier = Modifier.height(metrics.sectionGap))
        }

        // ── 6. Welcome-Screen-Styled Daily Targets Card (Custom Edit Dock ONLY in Custom Mode!) ──
        DailyTargetsCalibrationCard(
          report = report,
          scheduleCadence = effectiveScheduleCadence,
          splitDayFocus = splitDayFocus,
          inspectedDayOfWeek = inspectedDayOfWeek,
          isCustomMode = customOverride.isCustomMode,
          selectedFocusMetric = selectedFocusMetric,
          onSelectFocusMetric = { metric ->
            haptics.selection()
            selectedFocusMetric = metric
          },
          activeCalories = activeCalories,
          activeProtein = activeProtein,
          activeCarbs = activeCarbs,
          activeFats = activeFats,
          activeHydration = activeHydration,
          activeSteps = activeSteps,
          healthAlerts = healthAlerts,
          onStepCalories = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.caloriesKcal else report.recommendedCaloriesKcal
            val updated = (baseVal + delta).coerceIn(800, 6500)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = updated,
                proteinGrams = activeProtein,
                carbsGrams = activeCarbs,
                fatsGrams = activeFats,
                hydrationMl = activeHydration,
                stepsCount = activeSteps,
              )
            )
          },
          onStepProtein = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.proteinGrams else report.recommendedProteinGrams
            val updated = (baseVal + delta).coerceIn(20, 400)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = activeCalories,
                proteinGrams = updated,
                carbsGrams = activeCarbs,
                fatsGrams = activeFats,
                hydrationMl = activeHydration,
                stepsCount = activeSteps,
              )
            )
          },
          onStepCarbs = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.carbsGrams else report.recommendedCarbsGrams
            val updated = (baseVal + delta).coerceIn(10, 900)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = activeCalories,
                proteinGrams = activeProtein,
                carbsGrams = updated,
                fatsGrams = activeFats,
                hydrationMl = activeHydration,
                stepsCount = activeSteps,
              )
            )
          },
          onStepFats = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.fatsGrams else report.recommendedFatsGrams
            val updated = (baseVal + delta).coerceIn(10, 300)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = activeCalories,
                proteinGrams = activeProtein,
                carbsGrams = activeCarbs,
                fatsGrams = updated,
                hydrationMl = activeHydration,
                stepsCount = activeSteps,
              )
            )
          },
          onStepHydration = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.hydrationMl else report.primaryRecommendedWaterMl
            val updated = (baseVal + delta).coerceIn(500, 7500)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = activeCalories,
                proteinGrams = activeProtein,
                carbsGrams = activeCarbs,
                fatsGrams = activeFats,
                hydrationMl = updated,
                stepsCount = activeSteps,
              )
            )
          },
          onStepSteps = { delta ->
            haptics.tick()
            val baseVal = if (customOverride.isCustomMode) customOverride.stepsCount else report.recommendedSteps
            val updated = (baseVal + delta).coerceIn(1000, 30000)
            onUpdateCustomOverride(
              customOverride.copy(
                isCustomMode = true,
                caloriesKcal = activeCalories,
                proteinGrams = activeProtein,
                carbsGrams = activeCarbs,
                fatsGrams = activeFats,
                hydrationMl = activeHydration,
                stepsCount = updated,
              )
            )
          },
          onOpenDirectEdit = { target ->
            haptics.selection()
            if (!customOverride.isCustomMode) {
              switchToCustomMode()
            }
            activeDirectEditTarget = target
          },
          onInspectCalculation = openCalculationBreakdown,
          onSyncCaloriesFromMacros = syncCaloriesToMacrosAction,
          onOpenHealthDisclaimer = {
            haptics.selection()
            showDisclaimerModal = true
          },
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // ── 7. Schedule Cadence Split (Uniform vs Workout/Rest) & Per-Meal Breakdown ──
        ScheduleAndMealArchitectureCard(
          report = report,
          activityLevel = activityLevel,
          selectedWorkoutDays = report.selectedWorkoutDays,
          inspectedDayOfWeek = inspectedDayOfWeek,
          onSelectDayOfWeek = { day ->
            haptics.selection()
            inspectedDayOfWeek = day
            val isWorkDay = day in report.selectedWorkoutDays
            val nextFocus = if (isWorkDay) SplitDayFocus.WORKOUT_DAY else SplitDayFocus.REST_DAY
            splitDayFocus = nextFocus
            if (!customOverride.isCustomMode) {
              val nextReport = DailyTargetsEngine.calculate(
                ageYears = ageYears,
                biologicalSex = selectedSex,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                occupationLoad = occupationLoad,
                objective = objective,
                trainingModality = trainingModality,
                workoutDuration = workoutDuration,
                experienceLevel = experienceLevel,
                targetPace = targetPace,
                scheduleCadence = effectiveScheduleCadence,
                mealFrequency = mealFrequency,
                caffeineAlcoholYes = caffeineAlcoholYes,
                sodiumFrequency = sodiumFrequency,
                climateBand = climateBand,
                bodyFatPercentOverride = if (bodyFatOverrideEnabled) bodyFatPercent else null,
                splitDayFocus = nextFocus,
                selectedWorkoutDays = selectedWorkoutDays,
              )
              onUpdateCustomOverride(
                CustomTargetsOverride(
                  isCustomMode = false,
                  caloriesKcal = nextReport.recommendedCaloriesKcal,
                  proteinGrams = nextReport.recommendedProteinGrams,
                  carbsGrams = nextReport.recommendedCarbsGrams,
                  fatsGrams = nextReport.recommendedFatsGrams,
                  hydrationMl = nextReport.primaryRecommendedWaterMl,
                  stepsCount = nextReport.recommendedSteps,
                )
              )
            }
          },
          scheduleCadence = effectiveScheduleCadence,
          splitDayFocus = splitDayFocus,
          onSelectScheduleCadence = { cadence ->
            haptics.selection()
            val resolvedCadence = if (activityLevel == ActivityLevelOption.SEDENTARY) {
              ScheduleCadenceOption.UNIFORM_DAILY
            } else {
              cadence
            }
            if (resolvedCadence == ScheduleCadenceOption.REST_DAYS) {
              splitDayFocus = SplitDayFocus.WORKOUT_DAY
              inspectedDayOfWeek = report.selectedWorkoutDays.firstOrNull()
            } else {
              inspectedDayOfWeek = null
            }
            onSelectScheduleCadence(resolvedCadence)
            // Keep CustomOverride synced when switching schedule mode in Recommended Mode
            if (!customOverride.isCustomMode) {
              val nextReport = DailyTargetsEngine.calculate(
                ageYears = ageYears,
                biologicalSex = selectedSex,
                heightCm = heightCm,
                weightKg = weightKg,
                activityLevel = activityLevel,
                occupationLoad = occupationLoad,
                objective = objective,
                trainingModality = trainingModality,
                workoutDuration = workoutDuration,
                experienceLevel = experienceLevel,
                targetPace = targetPace,
                scheduleCadence = resolvedCadence,
                mealFrequency = mealFrequency,
                caffeineAlcoholYes = caffeineAlcoholYes,
                sodiumFrequency = sodiumFrequency,
                climateBand = climateBand,
                bodyFatPercentOverride = if (bodyFatOverrideEnabled) bodyFatPercent else null,
                splitDayFocus = if (resolvedCadence == ScheduleCadenceOption.REST_DAYS) SplitDayFocus.WORKOUT_DAY else splitDayFocus,
                selectedWorkoutDays = selectedWorkoutDays,
              )
              onUpdateCustomOverride(
                CustomTargetsOverride(
                  isCustomMode = false,
                  caloriesKcal = nextReport.recommendedCaloriesKcal,
                  proteinGrams = nextReport.recommendedProteinGrams,
                  carbsGrams = nextReport.recommendedCarbsGrams,
                  fatsGrams = nextReport.recommendedFatsGrams,
                  hydrationMl = nextReport.primaryRecommendedWaterMl,
                  stepsCount = nextReport.recommendedSteps,
                )
              )
            }
          },
          onSelectSplitDayFocus = { nextFocus ->
            haptics.selection()
            splitDayFocus = nextFocus
            inspectedDayOfWeek = if (nextFocus == SplitDayFocus.WORKOUT_DAY) {
              report.selectedWorkoutDays.firstOrNull()
            } else {
              report.selectedRestDays.firstOrNull()
            }
          },
          dayOne = dayOne,
          mealFrequency = mealFrequency,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // ── 8. Metabolic Baseline & Body Composition (Clean labels, 2s hold for math) ──
        ScientificMetabolicAuditCard(
          report = report,
          bodyFatOverrideEnabled = bodyFatOverrideEnabled,
          bodyFatPercent = bodyFatPercent,
          onToggleBodyFatOverride = { enabled ->
            haptics.selection()
            onToggleBodyFatOverride(enabled)
          },
          onBodyFatPercentChange = { bf ->
            haptics.tick()
            onBodyFatPercentChange(bf)
          },
          onInspectCalculation = openCalculationBreakdown,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // ── 9. Complete Cross-Device Selection Summary Cards (Steps 01, 02, 03) + 1-Tap Teleport Edit ──
        CrossDeviceSelectionsSummarySection(
          subjectName = subjectName,
          selectedSex = selectedSex,
          birthDay = birthDay,
          birthMonthZeroBased = birthMonthZeroBased,
          birthYear = birthYear,
          ageYears = ageYears,
          heightCm = heightCm,
          weightKg = weightKg,
          report = report,
          activityLevel = activityLevel,
          occupationLoad = occupationLoad,
          objective = objective,
          trainingModality = trainingModality,
          workoutDuration = workoutDuration,
          experienceLevel = experienceLevel,
          targetPace = targetPace,
          dayOne = dayOne,
          scheduleCadence = scheduleCadence,
          mealFrequency = mealFrequency,
          caffeineAlcoholYes = caffeineAlcoholYes,
          sodiumFrequency = sodiumFrequency,
          streakThreshold = streakThreshold,
          onTeleportToStep01 = {
            haptics.confirm()
            onTeleportToStep01()
          },
          onTeleportToStep02 = {
            haptics.confirm()
            onTeleportToStep02()
          },
          onTeleportToStep03 = { focusSection ->
            haptics.confirm()
            onTeleportToStep03(focusSection)
          },
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.cardToCtaGap))

        // ── 10. Primary Lock-In Protocol Button & Encrypted On-Device Footer ──
        SummaryLockInCtaSection(
          protocolLockedIn = protocolLockedIn,
          onLockInClick = onPrimaryCtaClick,
          borderFlashProvider = borderFlashProvider,
          metrics = metrics,
        )
      }
    }
  }

  // 2–3 Second Hold Calculation Breakdown Modal
  val activeTopic = inspectedCalculationTopic
  if (activeTopic != null) {
    val breakdownSpec = remember(
      activeTopic,
      report,
      ageYears,
      selectedSex,
      heightCm,
      weightKg,
      activityLevel,
      occupationLoad,
      objective,
      trainingModality,
      workoutDuration,
      experienceLevel,
      targetPace,
      scheduleCadence,
      mealFrequency,
      caffeineAlcoholYes,
      sodiumFrequency,
    ) {
      buildCalculationBreakdown(
        topic = activeTopic,
        report = report,
        ageYears = ageYears,
        selectedSex = selectedSex,
        heightCm = heightCm,
        weightKg = weightKg,
        activityLevel = activityLevel,
        occupationLoad = occupationLoad,
        objective = objective,
        trainingModality = trainingModality,
        workoutDuration = workoutDuration,
        experienceLevel = experienceLevel,
        targetPace = targetPace,
        scheduleCadence = scheduleCadence,
        mealFrequency = mealFrequency,
        caffeineAlcoholYes = caffeineAlcoholYes,
        sodiumFrequency = sodiumFrequency,
      )
    }
    CalculationBreakdownDialog(
      spec = breakdownSpec,
      onDismiss = { inspectedCalculationTopic = null },
    )
  }

  // Direct Numeric Value Input Dialog (when tapping any metric value in Custom Edit mode)
  if (activeDirectEditTarget != DirectEditMetricTarget.NONE) {
    when (activeDirectEditTarget) {
      DirectEditMetricTarget.CALORIES -> DirectTargetValueEditDialog(
        metricTitle = "Daily Calories",
        unitLabel = "kcal",
        currentValue = activeCalories,
        recommendedValue = report.recommendedCaloriesKcal,
        safeRangeLabel = "${report.calorieSafetyFloorKcal}–${(report.tdeeKcalRounded * 1.25f).roundToInt()} kcal",
        minAllowed = 600,
        maxAllowed = 8000,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = newVal,
            proteinGrams = activeProtein,
            carbsGrams = activeCarbs,
            fatsGrams = activeFats,
            hydrationMl = activeHydration,
            stepsCount = activeSteps,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.PROTEIN -> DirectTargetValueEditDialog(
        metricTitle = "Daily Protein",
        unitLabel = "g",
        currentValue = activeProtein,
        recommendedValue = report.recommendedProteinGrams,
        safeRangeLabel = "${report.proteinFloorGrams}g–${report.proteinCeilingGrams}g",
        minAllowed = 10,
        maxAllowed = 500,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = activeCalories,
            proteinGrams = newVal,
            carbsGrams = activeCarbs,
            fatsGrams = activeFats,
            hydrationMl = activeHydration,
            stepsCount = activeSteps,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.CARBS -> DirectTargetValueEditDialog(
        metricTitle = "Daily Carbs",
        unitLabel = "g",
        currentValue = activeCarbs,
        recommendedValue = report.recommendedCarbsGrams,
        safeRangeLabel = "${report.carbsFloorGrams}g–${report.carbsCeilingGrams}g",
        minAllowed = 0,
        maxAllowed = 1000,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = activeCalories,
            proteinGrams = activeProtein,
            carbsGrams = newVal,
            fatsGrams = activeFats,
            hydrationMl = activeHydration,
            stepsCount = activeSteps,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.FATS -> DirectTargetValueEditDialog(
        metricTitle = "Daily Fats",
        unitLabel = "g",
        currentValue = activeFats,
        recommendedValue = report.recommendedFatsGrams,
        safeRangeLabel = "${report.fatFloorGrams}g–${report.fatCeilingGrams}g",
        minAllowed = 5,
        maxAllowed = 350,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = activeCalories,
            proteinGrams = activeProtein,
            carbsGrams = activeCarbs,
            fatsGrams = newVal,
            hydrationMl = activeHydration,
            stepsCount = activeSteps,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.HYDRATION -> DirectTargetValueEditDialog(
        metricTitle = "Daily Water",
        unitLabel = "ml",
        currentValue = activeHydration,
        recommendedValue = report.primaryRecommendedWaterMl,
        safeRangeLabel = "${report.waterFloorMl}–${report.waterCeilingMl} ml",
        minAllowed = 300,
        maxAllowed = 9000,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = activeCalories,
            proteinGrams = activeProtein,
            carbsGrams = activeCarbs,
            fatsGrams = activeFats,
            hydrationMl = newVal,
            stepsCount = activeSteps,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.STEPS -> DirectTargetValueEditDialog(
        metricTitle = "Daily Steps",
        unitLabel = "steps",
        currentValue = activeSteps,
        recommendedValue = report.recommendedSteps,
        safeRangeLabel = "${report.stepsFloor}–${report.stepsCeiling} steps",
        minAllowed = 500,
        maxAllowed = 40000,
        onDismiss = { activeDirectEditTarget = DirectEditMetricTarget.NONE },
        onConfirmValue = { newVal ->
          val nextCustom = customOverride.copy(
            isCustomMode = true,
            caloriesKcal = activeCalories,
            proteinGrams = activeProtein,
            carbsGrams = activeCarbs,
            fatsGrams = activeFats,
            hydrationMl = activeHydration,
            stepsCount = newVal,
          )
          onUpdateCustomOverride(nextCustom)
          activeDirectEditTarget = DirectEditMetricTarget.NONE
          if (DailyTargetsEngine.evaluateHealthImpacts(report, nextCustom).isNotEmpty()) {
            haptics.warning()
            showDisclaimerModal = true
          } else {
            haptics.confirm()
          }
        },
      )

      DirectEditMetricTarget.NONE -> Unit
    }
  }

  // Simple Real-Life Health Impact & Disclaimer Popup Modal
  if (showDisclaimerModal && healthAlerts.isNotEmpty()) {
    HealthImpactDisclaimerDialog(
      alerts = healthAlerts,
      onSyncCaloriesFromMacros = syncCaloriesToMacrosAction,
      onRestoreRecommended = resetToRecommended,
      onAcknowledgeAndKeepCustom = {
        showDisclaimerModal = false
        triggerFlashAndLockIn()
      },
      onDismiss = { showDisclaimerModal = false },
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// HEADER BAR & STEP 04 OF 04 SHINY GLOW BAR
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryStepFourHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val backDesc = stringResource(R.string.cd_back_to_blueprint)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = metrics.headerHeight)
      .padding(vertical = (4f * metrics.scale).coerceIn(2f, 7f).dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .clickable(role = Role.Button, onClick = onBackClick)
        .semantics { contentDescription = backDesc }
        .testTag("summary_back_button"),
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
          color = PSZinc300,
          style = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }

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
        text = stringResource(R.string.summary_header_step),
        color = PSZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10.2f, 9f, 12f),
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp))

      StepFourShinyGlowBar(
        phaseProvider = phaseProvider,
        modifier = Modifier
          .width((108f * metrics.scale).coerceIn(92f, 136f).dp)
          .height((4f * metrics.scale).coerceIn(3.5f, 5f).dp)
          .testTag("step_04_glow_bar"),
      )
    }

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
private fun StepFourShinyGlowBar(
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
            color = android.graphics.Color.argb(160, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeSummaryBlur(barH * 1.9f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val innerHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(225, 245, 245, 250)
            maskFilter = BlurMaskFilter(safeSummaryBlur(barH * 0.75f), BlurMaskFilter.Blur.NORMAL)
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
          val activeFraction = 0.75f + (1.00f - 0.75f) * p

          drawRoundRect(
            color = PSZinc850,
            topLeft = Offset.Zero,
            size = Size(size.width, barH),
            cornerRadius = radius,
          )
          drawRoundRect(
            color = PSZinc700.copy(alpha = 0.45f),
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
            color = ObsidianBlack.copy(alpha = 0.40f),
            start = Offset(step1X, 0f),
            end = Offset(step1X, barH),
            strokeWidth = 1.0.dp.toPx(),
          )
          drawLine(
            color = ObsidianBlack.copy(alpha = 0.40f),
            start = Offset(step2X, 0f),
            end = Offset(step2X, barH),
            strokeWidth = 1.0.dp.toPx(),
          )
          drawLine(
            color = ObsidianBlack.copy(alpha = 0.40f),
            start = Offset(step3X, 0f),
            end = Offset(step3X, barH),
            strokeWidth = 1.0.dp.toPx(),
          )
        }
      },
  )
}

// ═════════════════════════════════════════════════════════════════════════════
// SAFETY GATES NOTICE BANNER & PURE B&W RECOMMENDED VS CUSTOM MODE SELECTOR
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SafetyGatesNoticeBannerList(
  notices: List<SafetyGateNotice>,
  metrics: BlueprintResponsiveMetrics,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("safety_gates_notice_list"),
    verticalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
  ) {
    notices.forEach { notice ->
      val shape = RoundedCornerShape(metrics.cardCornerRadius)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(shape)
          .background(PSZinc950)
          .border(1.1.dp, PSZinc700, shape)
          .padding(metrics.cardPadding),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Canvas(modifier = Modifier.size(6.5.dp)) {
            drawCircle(color = PureWhite)
          }
          Text(
            text = notice.badgeLabel,
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(9.4f, 8.4f, 10.6f),
            letterSpacing = 0.8.sp,
          )
        }
        Text(
          text = notice.title,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(13.4f, 12.0f, 15.0f),
        )
        Text(
          text = notice.explanation,
          color = PSZinc300,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
          lineHeight = metrics.spScaled(16.4f, 14.8f, 18.2f),
        )
      }
    }
  }
}

@Composable
private fun TargetModeSelectorCard(
  isCustomMode: Boolean,
  alertCount: Int,
  onSelectRecommended: () -> Unit,
  onSelectCustom: () -> Unit,
  onInspectAlerts: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val shape = RoundedCornerShape(metrics.cardCornerRadius)
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .summaryCardSurface(metrics.cardCornerRadius, borderFlashProvider)
      .padding((12.5f * metrics.scale).dp),
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
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          BlueprintLuminousWhiteDot(size = (6f * metrics.scale).dp)
          Text(
            text = "CALIBRATION MODE",
            color = PSZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
            letterSpacing = 0.9.sp,
          )
        }
        if (isCustomMode && alertCount > 0) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0xFFF43F5E).copy(alpha = 0.18f))
              .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(5.dp))
              .clickable(role = Role.Button, onClick = onInspectAlerts)
              .padding(horizontal = 8.dp, vertical = 3.dp)
              .testTag("mode_header_alert_pill"),
          ) {
            Text(
              text = "⚠ $alertCount HEALTH TIP${if (alertCount > 1) "S" else ""}",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
            )
          }
        } else {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(PureWhite)
              .padding(horizontal = 7.dp, vertical = 2.5.dp),
          ) {
            Text(
              text = if (isCustomMode) "CUSTOM OVERRIDE" else "AUTO-CALIBRATED",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((8.5f * metrics.scale).dp),
      ) {
        val pillShape = RoundedCornerShape((10f * metrics.scale).dp)
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(pillShape)
            .background(if (!isCustomMode) PureWhite else PSZinc950)
            .border(if (!isCustomMode) 1.4.dp else 1.dp, if (!isCustomMode) PureWhite else PSZinc800, pillShape)
            .clickable(role = Role.RadioButton, onClick = onSelectRecommended)
            .padding(vertical = (10.5f * metrics.scale).dp, horizontal = 10.dp)
            .testTag("target_mode_recommended_button"),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Recommended",
              color = if (!isCustomMode) ObsidianBlack else PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(13.2f, 11.8f, 14.8f),
            )
            Text(
              text = "Matched to Your Body",
              color = if (!isCustomMode) PSZinc600 else PSZinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
            )
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(pillShape)
            .background(if (isCustomMode) PureWhite else PSZinc950)
            .border(if (isCustomMode) 1.4.dp else 1.dp, if (isCustomMode) PureWhite else PSZinc800, pillShape)
            .clickable(role = Role.RadioButton, onClick = onSelectCustom)
            .padding(vertical = (10.5f * metrics.scale).dp, horizontal = 10.dp)
            .testTag("target_mode_custom_button"),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Custom Edit",
              color = if (isCustomMode) ObsidianBlack else PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(13.2f, 11.8f, 14.8f),
            )
            Text(
              text = "Adjust Any Target",
              color = if (isCustomMode) PSZinc600 else PSZinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OutOfRangeHealthAdvisoryBanner(
  alerts: List<HealthImpactAlert>,
  onOpenFullDisclaimer: () -> Unit,
  onSyncCaloriesFromMacros: () -> Unit,
  onRestoreRecommended: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  val hasCritical = alerts.any { it.severity == HealthAlertSeverity.CRITICAL }
  val hasSyncAlert = alerts.any { it.id == "macro_calorie_mismatch" }
  val bannerAccent = if (hasCritical) Color(0xFFF43F5E) else Color(0xFFFF8E4D)
  val shape = RoundedCornerShape(metrics.cardCornerRadius)
  val primaryAlert = alerts.first()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(Color(0xFF151215))
      .border(1.2.dp, bannerAccent.copy(alpha = 0.72f), shape)
      .padding(metrics.cardPadding)
      .testTag("out_of_range_advisory_banner"),
    verticalArrangement = Arrangement.spacedBy((8.5f * metrics.scale).dp),
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
        Canvas(modifier = Modifier.size(7.dp)) {
          drawCircle(color = bannerAccent)
        }
        Text(
          text = "BODY & ENERGY IMPACT NOTICE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
          letterSpacing = 0.7.sp,
        )
      }
      Text(
        text = "${alerts.size} NOTICE${if (alerts.size > 1) "S" else ""}",
        color = PSZinc300,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
      )
    }

    Text(
      text = "${primaryAlert.metricName}: ${primaryAlert.headline}",
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(13.2f, 11.8f, 14.8f),
    )

    Text(
      text = primaryAlert.realLifeExamples,
      color = PSZinc200,
      fontFamily = InterFontFamily,
      fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
      lineHeight = metrics.spScaled(16.4f, 14.8f, 18.2f),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(9.dp))
          .background(PSZinc850)
          .border(1.dp, PSZinc600, RoundedCornerShape(9.dp))
          .clickable(role = Role.Button, onClick = onOpenFullDisclaimer)
          .padding(vertical = 10.dp)
          .testTag("inspect_health_impact_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "See Full Impact (${alerts.size})",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
        )
      }

      if (hasSyncAlert && alerts.size == 1) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onSyncCaloriesFromMacros)
            .padding(vertical = 10.dp)
            .testTag("banner_sync_calories_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Match Calories",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
          )
        }
      } else {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onRestoreRecommended)
            .padding(vertical = 10.dp)
            .testTag("banner_restore_recommended_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Reset to Healthy",
            color = ObsidianBlack,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(11.6f, 10.4f, 13.0f),
          )
        }
      }
    }
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// WELCOME-SCREEN-INSPIRED DAILY TARGETS CALIBRATION CARD
// - Uses exact Welcome Screen 3-stop category gradients + Hexagon/Circle gauges
// - Custom Edit (-/+) tuner dock is ONLY visible when `isCustomMode == true`!
// - Holding any category for ~2 seconds opens its Calculation Breakdown modal!
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DailyTargetsCalibrationCard(
  report: DailyTargetsCalculationReport,
  scheduleCadence: ScheduleCadenceOption,
  splitDayFocus: SplitDayFocus,
  inspectedDayOfWeek: DayOfWeekOption? = null,
  isCustomMode: Boolean,
  selectedFocusMetric: FocusTargetMetric,
  onSelectFocusMetric: (FocusTargetMetric) -> Unit,
  activeCalories: Int,
  activeProtein: Int,
  activeCarbs: Int,
  activeFats: Int,
  activeHydration: Int,
  activeSteps: Int,
  healthAlerts: List<HealthImpactAlert>,
  onStepCalories: (Int) -> Unit,
  onStepProtein: (Int) -> Unit,
  onStepCarbs: (Int) -> Unit,
  onStepFats: (Int) -> Unit,
  onStepHydration: (Int) -> Unit,
  onStepSteps: (Int) -> Unit,
  onOpenDirectEdit: (DirectEditMetricTarget) -> Unit,
  onInspectCalculation: (CalculationTopic) -> Unit,
  onSyncCaloriesFromMacros: () -> Unit,
  onOpenHealthDisclaimer: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val totalMacroKcal = (4 * activeProtein + 4 * activeCarbs + 9 * activeFats).coerceAtLeast(1)
  val pSharePct = ((4f * activeProtein / totalMacroKcal) * 100f).roundToInt().coerceIn(0, 100)
  val fSharePct = ((9f * activeFats / totalMacroKcal) * 100f).roundToInt().coerceIn(0, 100)
  val cSharePct = (100 - pSharePct - fSharePct).coerceIn(0, 100)

  val calAlert = healthAlerts.firstOrNull { it.id.startsWith("calories_") || it.id == "macro_calorie_mismatch" }
  val proAlert = healthAlerts.firstOrNull { it.id.startsWith("protein_") }
  val carbAlert = healthAlerts.firstOrNull { it.id.startsWith("carbs_") }
  val fatAlert = healthAlerts.firstOrNull { it.id.startsWith("fats_") }
  val h2oAlert = healthAlerts.firstOrNull { it.id.startsWith("hydration_") }
  val stepAlert = healthAlerts.firstOrNull { it.id.startsWith("steps_") }

  val isSplitActive = scheduleCadence == ScheduleCadenceOption.REST_DAYS && report.trainingDaysPerWeek > 0
  val dayPrefix = inspectedDayOfWeek?.shortCode?.let { "$it • " } ?: ""
  val badgeLabel = when {
    isCustomMode -> "CUSTOM EDIT • HOLD FOR MATH"
    isSplitActive && splitDayFocus == SplitDayFocus.WORKOUT_DAY -> "${dayPrefix}WORKOUT DAY • HOLD FOR MATH"
    isSplitActive && splitDayFocus == SplitDayFocus.REST_DAY -> "${dayPrefix}REST DAY • HOLD FOR MATH"
    else -> "HOLD ANY METRIC FOR MATH"
  }

  val caloriesGradient = remember { listOf(CaloriesStart, CaloriesMid, CaloriesEnd) }
  val proteinGradient = remember { listOf(ProteinStart, ProteinMid, ProteinEnd) }
  val carbsGradient = remember { listOf(CarbsStart, CarbsMid, CarbsEnd) }
  val fatsGradient = remember { listOf(FatsStart, FatsMid, FatsEnd) }
  val hydrationGradient = remember { listOf(HydrationStart, HydrationMid, HydrationEnd) }
  val stepsGradient = remember { listOf(StepsStart, StepsMid, StepsEnd) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("daily_targets_calibration_card"),
  ) {
    SummarySectionHeader(
      title = if (isCustomMode) "Custom Daily Targets" else "Recommended Daily Targets",
      badgeText = badgeLabel,
      badgeColor = PSZinc300,
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
        verticalArrangement = Arrangement.spacedBy((12f * metrics.scale).coerceIn(10f, 15f).dp),
      ) {
        // ── 1. HERO CALORIES SECTION (Unified Horizontal Safe-Range Progress Bar, No Hexagon) ──
        var calHoldProgress by remember { mutableFloatStateOf(0f) }
        val isCalFocused = isCustomMode && selectedFocusMetric == FocusTargetMetric.CALORIES
        val calHeroShape = RoundedCornerShape((11f * metrics.scale).dp)
        val calBorderColor = when {
          calHoldProgress > 0.02f -> PureWhite
          calAlert != null -> calAlert.severity.accentColor
          isCalFocused -> PureWhite
          else -> Color(0xFF232834)
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(calHeroShape)
            .background(Color(0xFF0A0B0F))
            .border(if (isCalFocused || calHoldProgress > 0.02f) 1.5.dp else 1.dp, calBorderColor, calHeroShape)
            .tapOrHoldForCalculation(
              onTap = {
                onSelectFocusMetric(FocusTargetMetric.CALORIES)
              },
              onHoldComplete = {
                onInspectCalculation(CalculationTopic.CALORIES)
              },
              onHoldProgress = { calHoldProgress = it },
            )
            .padding((13f * metrics.scale).dp),
          verticalArrangement = Arrangement.spacedBy((9f * metrics.scale).dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
              ) {
                WelcomeCategoryPip(
                  gradientColors = caloriesGradient,
                  size = (8f * metrics.scale).dp,
                )
                Text(
                  text = "DAILY CALORIE GOAL",
                  color = PSZinc300,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(10.5f, 9.2f, 12.0f),
                  letterSpacing = 1.0.sp,
                )
                if (calHoldProgress > 0.02f) {
                  Text(
                    text = "MATH ${(calHoldProgress * 100).roundToInt()}%",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(9.0f, 8.0f, 10.0f),
                  )
                }
              }

              Spacer(modifier = Modifier.height((5f * metrics.scale).dp))

              Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable(role = Role.Button) {
                    onSelectFocusMetric(FocusTargetMetric.CALORIES)
                    if (isCustomMode) {
                      onOpenDirectEdit(DirectEditMetricTarget.CALORIES)
                    }
                  }
                  .testTag("summary_calories_value_button"),
              ) {
                Text(
                  text = formatSummaryInt(activeCalories),
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = metrics.spScaled(26f, 22f, 30f),
                  lineHeight = metrics.spScaled(26f, 22f, 30f),
                  letterSpacing = (-0.6).sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "kcal / day",
                  color = PSZinc400,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Normal,
                  fontSize = metrics.spScaled(12.2f, 10.8f, 13.8f),
                  modifier = Modifier.padding(bottom = 2.dp),
                )
              }
            }

            // Right Side: Clean Monochrome Goal Badge & Macro Split Share (No Hexagon)
            Column(horizontalAlignment = Alignment.End) {
              val deltaFromTdee = activeCalories - report.tdeeKcalRounded
              val deltaSign = if (deltaFromTdee > 0) "+" else ""
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(PureWhite)
                  .padding(horizontal = 8.dp, vertical = 3.dp),
              ) {
                Text(
                  text = if (abs(deltaFromTdee) <= 15) "MAINTAIN" else "${deltaSign}${deltaFromTdee} KCAL",
                  color = ObsidianBlack,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
                  letterSpacing = (-0.2).sp,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "P ${pSharePct}% • C ${cSharePct}% • F ${fSharePct}%",
                color = PSZinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = metrics.spScaled(9.6f, 8.6f, 10.8f),
              )
            }
          }

          // Unified Safe-Range Progress Bar for Calories (~64% fill at Recommended Goal)
          SafeRangeCategoryProgressBar(
            currentValue = activeCalories,
            recommendedValue = report.recommendedCaloriesKcal,
            safeMin = report.calorieSafetyFloorKcal,
            safeMax = report.calorieSafeCeilingKcal,
            safeRangeText = "${formatSummaryInt(report.calorieSafetyFloorKcal)}–${formatSummaryInt(report.calorieSafeCeilingKcal)} kcal",
            gradientColors = caloriesGradient,
            barHeight = (9f * metrics.scale).coerceIn(7.5f, 11f).dp,
            holdProgress = calHoldProgress,
            metrics = metrics,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("summary_calories_progress_bar"),
          )
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF222733),
        )

        // ── 2. UNIFIED MACRO & HABIT ROWS (Identical Horizontal Capsule Shape + Embedded Safe Ranges) ──
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy((10f * metrics.scale).coerceIn(8.5f, 13f).dp),
        ) {
          // PROTEIN ROW
          WelcomeStyledCategoryMetricCard(
            title = stringResource(R.string.macro_protein),
            valueFormatted = "${activeProtein}g",
            targetFormatted = "Rec ${report.recommendedProteinGrams}g",
            detailBadge = "${pSharePct}% of calories • ${activeProtein * 4} kcal",
            currentValue = activeProtein,
            recommendedValue = report.recommendedProteinGrams,
            safeMin = report.proteinFloorGrams,
            safeMax = report.proteinCeilingGrams,
            safeRangeText = "${report.proteinFloorGrams}g–${report.proteinCeilingGrams}g",
            gradientColors = proteinGradient,
            isSelected = isCustomMode && selectedFocusMetric == FocusTargetMetric.PROTEIN,
            alert = proAlert,
            onTap = { onSelectFocusMetric(FocusTargetMetric.PROTEIN) },
            onValueTap = {
              onSelectFocusMetric(FocusTargetMetric.PROTEIN)
              if (isCustomMode) {
                onOpenDirectEdit(DirectEditMetricTarget.PROTEIN)
              }
            },
            onHoldComplete = { onInspectCalculation(CalculationTopic.PROTEIN) },
            testTagPrefix = "summary_protein",
            metrics = metrics,
          )

          // CARBOHYDRATES ROW
          WelcomeStyledCategoryMetricCard(
            title = stringResource(R.string.macro_carbs),
            valueFormatted = "${activeCarbs}g",
            targetFormatted = "Rec ${report.recommendedCarbsGrams}g",
            detailBadge = "${cSharePct}% of calories • ${activeCarbs * 4} kcal",
            currentValue = activeCarbs,
            recommendedValue = report.recommendedCarbsGrams,
            safeMin = report.carbsFloorGrams,
            safeMax = report.carbsCeilingGrams,
            safeRangeText = "${report.carbsFloorGrams}g–${report.carbsCeilingGrams}g",
            gradientColors = carbsGradient,
            isSelected = isCustomMode && selectedFocusMetric == FocusTargetMetric.CARBS,
            alert = carbAlert,
            onTap = { onSelectFocusMetric(FocusTargetMetric.CARBS) },
            onValueTap = {
              onSelectFocusMetric(FocusTargetMetric.CARBS)
              if (isCustomMode) {
                onOpenDirectEdit(DirectEditMetricTarget.CARBS)
              }
            },
            onHoldComplete = { onInspectCalculation(CalculationTopic.CARBS) },
            testTagPrefix = "summary_carbs",
            metrics = metrics,
          )

          // FATS ROW
          WelcomeStyledCategoryMetricCard(
            title = stringResource(R.string.macro_fats),
            valueFormatted = "${activeFats}g",
            targetFormatted = "Rec ${report.recommendedFatsGrams}g",
            detailBadge = "${fSharePct}% of calories • ${activeFats * 9} kcal",
            currentValue = activeFats,
            recommendedValue = report.recommendedFatsGrams,
            safeMin = report.fatFloorGrams,
            safeMax = report.fatCeilingGrams,
            safeRangeText = "${report.fatFloorGrams}g–${report.fatCeilingGrams}g",
            gradientColors = fatsGradient,
            isSelected = isCustomMode && selectedFocusMetric == FocusTargetMetric.FATS,
            alert = fatAlert,
            onTap = { onSelectFocusMetric(FocusTargetMetric.FATS) },
            onValueTap = {
              onSelectFocusMetric(FocusTargetMetric.FATS)
              if (isCustomMode) {
                onOpenDirectEdit(DirectEditMetricTarget.FATS)
              }
            },
            onHoldComplete = { onInspectCalculation(CalculationTopic.FATS) },
            testTagPrefix = "summary_fats",
            metrics = metrics,
          )

          // HYDRATION & DAILY STEPS (Same Unified Horizontal Capsule Progress Bar Shape, No Hexagon/Circle)
          val activeLiters = String.format(Locale.US, "%.2f L", activeHydration / 1000f)
          val activeGlasses = String.format(Locale.US, "%.0f gls", activeHydration / 250f)
          val walkKm = (activeSteps * report.strideLengthMeters) / 1000f
          val walkKcal = (activeSteps * report.actualWeightKg * 0.0004f).roundToInt()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8.5f * metrics.scale).dp),
          ) {
            WelcomeStyledHabitBentoTile(
              title = stringResource(R.string.macro_hydration),
              valueFormatted = "${formatSummaryInt(activeHydration)} ml",
              subFormatted = "$activeLiters ($activeGlasses)",
              recFormatted = "Rec ${formatSummaryInt(report.primaryRecommendedWaterMl)} ml",
              currentValue = activeHydration,
              recommendedValue = report.primaryRecommendedWaterMl,
              safeMin = report.waterFloorMl,
              safeMax = report.waterCeilingMl,
              safeRangeText = "${String.format(Locale.US, "%.1f", report.waterFloorMl / 1000f)}–${String.format(Locale.US, "%.1f", report.waterCeilingMl / 1000f)}L",
              gradientColors = hydrationGradient,
              isSelected = isCustomMode && selectedFocusMetric == FocusTargetMetric.HYDRATION,
              alert = h2oAlert,
              onTap = { onSelectFocusMetric(FocusTargetMetric.HYDRATION) },
              onValueTap = {
                onSelectFocusMetric(FocusTargetMetric.HYDRATION)
                if (isCustomMode) {
                  onOpenDirectEdit(DirectEditMetricTarget.HYDRATION)
                }
              },
              onHoldComplete = { onInspectCalculation(CalculationTopic.HYDRATION) },
              testTagPrefix = "summary_hydration",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )

            WelcomeStyledHabitBentoTile(
              title = stringResource(R.string.macro_steps),
              valueFormatted = formatSummaryInt(activeSteps),
              subFormatted = String.format(Locale.US, "~%.1f km • ~%d kcal", walkKm, walkKcal),
              recFormatted = "Rec ${formatSummaryInt(report.recommendedSteps)}",
              currentValue = activeSteps,
              recommendedValue = report.recommendedSteps,
              safeMin = report.stepsFloor,
              safeMax = report.stepsCeiling,
              safeRangeText = "${String.format(Locale.US, "%.1fk", report.stepsFloor / 1000f)}–${String.format(Locale.US, "%.1fk", report.stepsCeiling / 1000f)}",
              gradientColors = stepsGradient,
              isSelected = isCustomMode && selectedFocusMetric == FocusTargetMetric.STEPS,
              alert = stepAlert,
              onTap = { onSelectFocusMetric(FocusTargetMetric.STEPS) },
              onValueTap = {
                onSelectFocusMetric(FocusTargetMetric.STEPS)
                if (isCustomMode) {
                  onOpenDirectEdit(DirectEditMetricTarget.STEPS)
                }
              },
              onHoldComplete = { onInspectCalculation(CalculationTopic.STEPS) },
              testTagPrefix = "summary_steps",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
          }
        }

        // ── 3. CUSTOM EDIT PRECISION TUNER CONSOLE (STRICTLY VISIBLE ONLY IN CUSTOM EDIT MODE!) ──
        AnimatedVisibility(
          visible = isCustomMode,
          enter = expandVertically(animationSpec = tween(260)) + fadeIn(animationSpec = tween(220)),
          exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180)),
        ) {
          UnifiedPrecisionTunerDock(
            selectedFocusMetric = selectedFocusMetric,
            onSelectFocusMetric = onSelectFocusMetric,
            report = report,
            isCustomMode = isCustomMode,
            activeCalories = activeCalories,
            activeProtein = activeProtein,
            activeCarbs = activeCarbs,
            activeFats = activeFats,
            activeHydration = activeHydration,
            activeSteps = activeSteps,
            totalMacroKcal = totalMacroKcal,
            calAlert = calAlert,
            proAlert = proAlert,
            carbAlert = carbAlert,
            fatAlert = fatAlert,
            h2oAlert = h2oAlert,
            stepAlert = stepAlert,
            onStepCalories = onStepCalories,
            onStepProtein = onStepProtein,
            onStepCarbs = onStepCarbs,
            onStepFats = onStepFats,
            onStepHydration = onStepHydration,
            onStepSteps = onStepSteps,
            onOpenDirectEdit = onOpenDirectEdit,
            onSyncCaloriesFromMacros = onSyncCaloriesFromMacros,
            onOpenHealthDisclaimer = onOpenHealthDisclaimer,
            metrics = metrics,
          )
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF222733),
        )

        // ── 4. SLIM MONOCHROME MICRONUTRIENT & FIBRE STRIP (Hold for Math) ──
        var microHoldProgress by remember { mutableFloatStateOf(0f) }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .tapOrHoldForCalculation(
              onTap = {},
              onHoldComplete = { onInspectCalculation(CalculationTopic.MICRONUTRIENTS) },
              onHoldProgress = { microHoldProgress = it },
            ),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((7.5f * metrics.scale).dp),
          ) {
            CompanionNutrientPill(
              label = "DIETARY FIBRE",
              value = "${report.recommendedFibreGrams}g / day",
              sub = "Gut & Fullness",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
            CompanionNutrientPill(
              label = "FREE SUGARS",
              value = "< ${report.maxFreeSugarGrams}g / day",
              sub = "Daily Limit",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
            CompanionNutrientPill(
              label = "SODIUM CAP",
              value = "< ${report.maxSodiumMg} mg",
              sub = "Daily Limit",
              metrics = metrics,
              modifier = Modifier.weight(1f),
            )
          }
          if (microHoldProgress > 0.02f) {
            WelcomeCategoryLitProgressBar(
              fraction = microHoldProgress,
              gradientColors = stepsGradient,
              barHeight = 4.dp,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WelcomeStyledCategoryMetricCard(
  title: String,
  valueFormatted: String,
  targetFormatted: String,
  detailBadge: String,
  currentValue: Int,
  recommendedValue: Int,
  safeMin: Int,
  safeMax: Int,
  safeRangeText: String,
  gradientColors: List<Color>,
  isSelected: Boolean,
  alert: HealthImpactAlert?,
  onTap: () -> Unit,
  onValueTap: () -> Unit,
  onHoldComplete: () -> Unit,
  testTagPrefix: String,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  var holdProgress by remember { mutableFloatStateOf(0f) }
  val shape = RoundedCornerShape((10.5f * metrics.scale).dp)
  val borderColor = when {
    holdProgress > 0.02f -> PureWhite
    alert != null -> alert.severity.accentColor
    isSelected -> PureWhite
    else -> Color(0xFF222733)
  }
  val bgColor = if (isSelected) Color(0xFF151822) else Color(0xFF0B0C10)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(bgColor)
      .border(if (isSelected || holdProgress > 0.02f) 1.4.dp else 1.dp, borderColor, shape)
      .tapOrHoldForCalculation(
        onTap = onTap,
        onHoldComplete = onHoldComplete,
        onHoldProgress = { holdProgress = it },
      )
      .padding(horizontal = (12f * metrics.scale).dp, vertical = (10.5f * metrics.scale).dp)
      .testTag("${testTagPrefix}_card"),
    verticalArrangement = Arrangement.spacedBy((7f * metrics.scale).dp),
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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          WelcomeCategoryPip(
            gradientColors = gradientColors,
            size = (7.5f * metrics.scale).dp,
          )
          Text(
            text = title,
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(13.8f, 12.4f, 15.4f),
            maxLines = 1,
          )
          if (holdProgress > 0.02f) {
            Text(
              text = "MATH ${(holdProgress * 100).roundToInt()}%",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
            )
          }
        }
        Text(
          text = detailBadge,
          color = PSZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = metrics.spScaled(10.2f, 9.0f, 11.4f),
          maxLines = 1,
        )
      }

      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .clickable(role = Role.Button, onClick = onValueTap)
          .padding(start = 6.dp)
          .testTag("${testTagPrefix}_value_button"),
      ) {
        Text(
          text = valueFormatted,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(18.5f, 16.5f, 21.0f),
          maxLines = 1,
        )
        Text(
          text = targetFormatted,
          color = PSZinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
          maxLines = 1,
        )
      }
    }

    // Unified Horizontal Capsule Progress Bar with Embedded Safe Range Corridor
    SafeRangeCategoryProgressBar(
      currentValue = currentValue,
      recommendedValue = recommendedValue,
      safeMin = safeMin,
      safeMax = safeMax,
      safeRangeText = safeRangeText,
      gradientColors = gradientColors,
      barHeight = (7.5f * metrics.scale).coerceIn(6.5f, 9.5f).dp,
      holdProgress = holdProgress,
      metrics = metrics,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun WelcomeStyledHabitBentoTile(
  title: String,
  valueFormatted: String,
  subFormatted: String,
  recFormatted: String,
  currentValue: Int,
  recommendedValue: Int,
  safeMin: Int,
  safeMax: Int,
  safeRangeText: String,
  gradientColors: List<Color>,
  isSelected: Boolean,
  alert: HealthImpactAlert?,
  onTap: () -> Unit,
  onValueTap: () -> Unit,
  onHoldComplete: () -> Unit,
  testTagPrefix: String,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  var holdProgress by remember { mutableFloatStateOf(0f) }
  val shape = RoundedCornerShape((10.5f * metrics.scale).dp)
  val borderColor = when {
    holdProgress > 0.02f -> PureWhite
    alert != null -> alert.severity.accentColor
    isSelected -> PureWhite
    else -> Color(0xFF222733)
  }
  val bgColor = if (isSelected) Color(0xFF151822) else Color(0xFF0B0C10)

  Column(
    modifier = modifier
      .clip(shape)
      .background(bgColor)
      .border(if (isSelected || holdProgress > 0.02f) 1.4.dp else 1.dp, borderColor, shape)
      .tapOrHoldForCalculation(
        onTap = onTap,
        onHoldComplete = onHoldComplete,
        onHoldProgress = { holdProgress = it },
      )
      .padding((11.5f * metrics.scale).dp)
      .testTag("${testTagPrefix}_card"),
    verticalArrangement = Arrangement.spacedBy((7f * metrics.scale).dp),
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
          size = (7f * metrics.scale).dp,
        )
        Text(
          text = title,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(12.2f, 10.8f, 13.6f),
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

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClick = onValueTap)
        .testTag("${testTagPrefix}_value_button"),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = valueFormatted,
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.spScaled(17.5f, 15.5f, 19.5f),
        maxLines = 1,
      )
      Text(
        text = subFormatted,
        color = PSZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = recFormatted,
        color = PSZinc300,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.6f, 8.6f, 10.8f),
        maxLines = 1,
      )
    }

    // Unified Horizontal Capsule Progress Bar with Embedded Safe Range Corridor (No Hexagon/Circle)
    SafeRangeCategoryProgressBar(
      currentValue = currentValue,
      recommendedValue = recommendedValue,
      safeMin = safeMin,
      safeMax = safeMax,
      safeRangeText = safeRangeText,
      gradientColors = gradientColors,
      barHeight = (7f * metrics.scale).coerceIn(6f, 8.5f).dp,
      holdProgress = holdProgress,
      metrics = metrics,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun UnifiedPrecisionTunerDock(
  selectedFocusMetric: FocusTargetMetric,
  onSelectFocusMetric: (FocusTargetMetric) -> Unit,
  report: DailyTargetsCalculationReport,
  isCustomMode: Boolean,
  activeCalories: Int,
  activeProtein: Int,
  activeCarbs: Int,
  activeFats: Int,
  activeHydration: Int,
  activeSteps: Int,
  totalMacroKcal: Int,
  calAlert: HealthImpactAlert?,
  proAlert: HealthImpactAlert?,
  carbAlert: HealthImpactAlert?,
  fatAlert: HealthImpactAlert?,
  h2oAlert: HealthImpactAlert?,
  stepAlert: HealthImpactAlert?,
  onStepCalories: (Int) -> Unit,
  onStepProtein: (Int) -> Unit,
  onStepCarbs: (Int) -> Unit,
  onStepFats: (Int) -> Unit,
  onStepHydration: (Int) -> Unit,
  onStepSteps: (Int) -> Unit,
  onOpenDirectEdit: (DirectEditMetricTarget) -> Unit,
  onSyncCaloriesFromMacros: () -> Unit,
  onOpenHealthDisclaimer: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  val dockShape = RoundedCornerShape((11f * metrics.scale).dp)
  val activeAlert = when (selectedFocusMetric) {
    FocusTargetMetric.CALORIES -> calAlert
    FocusTargetMetric.PROTEIN -> proAlert
    FocusTargetMetric.CARBS -> carbAlert
    FocusTargetMetric.FATS -> fatAlert
    FocusTargetMetric.HYDRATION -> h2oAlert
    FocusTargetMetric.STEPS -> stepAlert
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(dockShape)
      .background(Color(0xFF0A0B0F))
      .border(1.2.dp, activeAlert?.severity?.accentColor ?: PureWhite.copy(alpha = 0.35f), dockShape)
      .padding((12f * metrics.scale).dp)
      .testTag("unified_precision_tuner_dock"),
    verticalArrangement = Arrangement.spacedBy((9f * metrics.scale).dp),
  ) {
    // 6-Tab Quick Metric Selector Strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy((5.5f * metrics.scale).dp),
    ) {
      FocusTargetMetric.entries.forEach { item ->
        val isFocused = item == selectedFocusMetric
        val tabShape = RoundedCornerShape((7f * metrics.scale).dp)
        val tabBg by animateColorAsState(
          targetValue = if (isFocused) PureWhite else PSZinc900,
          animationSpec = tween(160),
          label = "tuner_tab_bg_${item.name}",
        )
        val tabTxt by animateColorAsState(
          targetValue = if (isFocused) ObsidianBlack else PSZinc200,
          animationSpec = tween(160),
          label = "tuner_tab_txt_${item.name}",
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(tabShape)
            .background(tabBg)
            .border(1.dp, if (isFocused) PureWhite else PSZinc800, tabShape)
            .clickable(role = Role.Tab) { onSelectFocusMetric(item) }
            .padding(vertical = (7f * metrics.scale).dp),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (!isFocused) {
              WelcomeCategoryPip(
                gradientColors = item.gradientColors,
                size = 5.5.dp,
              )
            }
            Text(
              text = item.shortTag,
              color = tabTxt,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(9.4f, 8.4f, 10.5f),
              maxLines = 1,
            )
          }
        }
      }
    }

    // Active Metric Header + Hold-to-Repeat Stepper Controls + Type Value Button
    val currentValLabel: String
    val safeCorridorLabel: String
    val minusLargeLabel: String
    val minusSmallLabel: String
    val plusSmallLabel: String
    val plusLargeLabel: String
    val onMinusLarge: () -> Unit
    val onMinusSmall: () -> Unit
    val onPlusSmall: () -> Unit
    val onPlusLarge: () -> Unit
    val directTarget: DirectEditMetricTarget

    when (selectedFocusMetric) {
      FocusTargetMetric.CALORIES -> {
        currentValLabel = "${formatSummaryInt(activeCalories)} kcal"
        safeCorridorLabel = "Safe range: ${report.calorieSafetyFloorKcal}–${(report.tdeeKcalRounded * 1.25f).roundToInt()} kcal"
        minusLargeLabel = "-50"
        minusSmallLabel = "-5"
        plusSmallLabel = "+5"
        plusLargeLabel = "+50"
        onMinusLarge = { onStepCalories(-50) }
        onMinusSmall = { onStepCalories(-5) }
        onPlusSmall = { onStepCalories(5) }
        onPlusLarge = { onStepCalories(50) }
        directTarget = DirectEditMetricTarget.CALORIES
      }
      FocusTargetMetric.PROTEIN -> {
        currentValLabel = "${activeProtein}g"
        safeCorridorLabel = "Safe range: ${report.proteinFloorGrams}g–${report.proteinCeilingGrams}g"
        minusLargeLabel = "-5g"
        minusSmallLabel = "-1g"
        plusSmallLabel = "+1g"
        plusLargeLabel = "+5g"
        onMinusLarge = { onStepProtein(-5) }
        onMinusSmall = { onStepProtein(-1) }
        onPlusSmall = { onStepProtein(1) }
        onPlusLarge = { onStepProtein(5) }
        directTarget = DirectEditMetricTarget.PROTEIN
      }
      FocusTargetMetric.CARBS -> {
        currentValLabel = "${activeCarbs}g"
        safeCorridorLabel = "Safe range: ${report.carbsFloorGrams}g–${report.carbsCeilingGrams}g"
        minusLargeLabel = "-10g"
        minusSmallLabel = "-1g"
        plusSmallLabel = "+1g"
        plusLargeLabel = "+10g"
        onMinusLarge = { onStepCarbs(-10) }
        onMinusSmall = { onStepCarbs(-1) }
        onPlusSmall = { onStepCarbs(1) }
        onPlusLarge = { onStepCarbs(10) }
        directTarget = DirectEditMetricTarget.CARBS
      }
      FocusTargetMetric.FATS -> {
        currentValLabel = "${activeFats}g"
        safeCorridorLabel = "Safe range: ${report.fatFloorGrams}g–${report.fatCeilingGrams}g"
        minusLargeLabel = "-5g"
        minusSmallLabel = "-1g"
        plusSmallLabel = "+1g"
        plusLargeLabel = "+5g"
        onMinusLarge = { onStepFats(-5) }
        onMinusSmall = { onStepFats(-1) }
        onPlusSmall = { onStepFats(1) }
        onPlusLarge = { onStepFats(5) }
        directTarget = DirectEditMetricTarget.FATS
      }
      FocusTargetMetric.HYDRATION -> {
        currentValLabel = "${formatSummaryInt(activeHydration)} ml"
        safeCorridorLabel = "Safe range: ${report.waterFloorMl}–${report.waterCeilingMl} ml"
        minusLargeLabel = "-250"
        minusSmallLabel = "-50"
        plusSmallLabel = "+50"
        plusLargeLabel = "+250"
        onMinusLarge = { onStepHydration(-250) }
        onMinusSmall = { onStepHydration(-50) }
        onPlusSmall = { onStepHydration(50) }
        onPlusLarge = { onStepHydration(250) }
        directTarget = DirectEditMetricTarget.HYDRATION
      }
      FocusTargetMetric.STEPS -> {
        currentValLabel = "${formatSummaryInt(activeSteps)} steps"
        safeCorridorLabel = "Safe range: ${formatSummaryInt(report.stepsFloor)}–${formatSummaryInt(report.stepsCeiling)}"
        minusLargeLabel = "-1000"
        minusSmallLabel = "-500"
        plusSmallLabel = "+500"
        plusLargeLabel = "+1000"
        onMinusLarge = { onStepSteps(-1000) }
        onMinusSmall = { onStepSteps(-500) }
        onPlusSmall = { onStepSteps(500) }
        onPlusLarge = { onStepSteps(1000) }
        directTarget = DirectEditMetricTarget.STEPS
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${selectedFocusMetric.fullLabel}: $currentValLabel",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(13.4f, 12.0f, 15.0f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "$safeCorridorLabel • Hold -/+ to fast-tune",
          color = PSZinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10.4f, 9.2f, 11.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(PureWhite)
          .clickable(role = Role.Button) { onOpenDirectEdit(directTarget) }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("tuner_type_exact_button"),
      ) {
        Text(
          text = "✎ Type",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(9.8f, 8.8f, 11.0f),
        )
      }
    }

    // 4 Hold-to-Repeat Stepper Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy((7f * metrics.scale).dp),
    ) {
      HoldRepeatableStepperButton(
        label = minusLargeLabel,
        onStep = onMinusLarge,
        metrics = metrics,
        modifier = Modifier.weight(1f),
        testTag = "tuner_minus_large",
      )
      HoldRepeatableStepperButton(
        label = minusSmallLabel,
        onStep = onMinusSmall,
        metrics = metrics,
        modifier = Modifier.weight(1f),
        testTag = "tuner_minus_small",
      )
      HoldRepeatableStepperButton(
        label = plusSmallLabel,
        onStep = onPlusSmall,
        metrics = metrics,
        modifier = Modifier.weight(1f),
        testTag = "tuner_plus_small",
      )
      HoldRepeatableStepperButton(
        label = plusLargeLabel,
        onStep = onPlusLarge,
        metrics = metrics,
        modifier = Modifier.weight(1f),
        testTag = "tuner_plus_large",
      )
    }

    // Sleek, attractive Auto-Match Calories pill (only shown when Custom macros & calories differ)
    val macroMismatch = abs(totalMacroKcal - activeCalories)
    if (isCustomMode && macroMismatch > 25) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(PSZinc900)
          .border(1.dp, PureWhite.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
          .clickable(role = Role.Button, onClick = onSyncCaloriesFromMacros)
          .padding(horizontal = 11.dp, vertical = 8.dp)
          .testTag("sync_calories_from_macros_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Macro food energy equals ${formatSummaryInt(totalMacroKcal)} kcal",
          color = PSZinc200,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(11.0f, 9.8f, 12.2f),
        )
        Text(
          text = "Match Calories ↗",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(10.0f, 9.0f, 11.2f),
        )
      }
    }

    if (activeAlert != null) {
      InlineMetricWarningChip(
        alert = activeAlert,
        onClick = onOpenHealthDisclaimer,
        metrics = metrics,
      )
    }
  }
}

@Composable
private fun InlineMetricWarningChip(
  alert: HealthImpactAlert,
  onClick: () -> Unit,
  metrics: BlueprintResponsiveMetrics,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(7.dp))
      .background(alert.severity.accentColor.copy(alpha = 0.14f))
      .border(1.dp, alert.severity.accentColor.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "⚠ ${alert.headline}",
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(10.8f, 9.6f, 12.0f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "WHY ↗",
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(9.4f, 8.4f, 10.4f),
    )
  }
}

@Composable
private fun CompanionNutrientPill(
  label: String,
  value: String,
  sub: String,
  metrics: BlueprintResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape((9f * metrics.scale).dp)
  Column(
    modifier = modifier
      .clip(shape)
      .background(Color(0xFF0B0C10))
      .border(1.dp, Color(0xFF222733), shape)
      .padding(horizontal = (10f * metrics.scale).dp, vertical = (8.5f * metrics.scale).dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = label,
      color = PSZinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
      maxLines = 1,
    )
    Text(
      text = value,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(13.0f, 11.6f, 14.5f),
      maxLines = 1,
    )
    Text(
      text = sub,
      color = PSZinc400,
      fontFamily = InterFontFamily,
      fontSize = metrics.spScaled(9.6f, 8.6f, 10.8f),
      maxLines = 1,
    )
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// PRIMARY LOCK-IN PROTOCOL CTA SECTION
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryLockInCtaSection(
  protocolLockedIn: Boolean,
  onLockInClick: () -> Unit,
  borderFlashProvider: () -> Float,
  metrics: BlueprintResponsiveMetrics,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val buttonCorner = (10f * metrics.scale).coerceIn(8f, 13f).dp
  val buttonShape = RoundedCornerShape(buttonCorner)

  val infiniteTransition = rememberInfiniteTransition(label = "summary_lockin_glow_transition")
  val shimmerPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "summary_lockin_shimmer_phase",
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
              maskFilter = BlurMaskFilter(safeSummaryBlur(18.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
            }
          }
          val innerRimGlowPaint = Paint().apply {
            isAntiAlias = true
            asFrameworkPaint().apply {
              color = android.graphics.Color.argb(140, 242, 245, 250)
              maskFilter = BlurMaskFilter(safeSummaryBlur(7.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
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
          onClick = onLockInClick,
        )
        .testTag("summary_lock_in_button"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = if (protocolLockedIn) {
            "Protocol Locked & Active ✓"
          } else {
            stringResource(R.string.summary_cta_lock_protocol)
          },
          color = ObsidianBlack,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(14.5f, 13f, 17f),
          letterSpacing = (-0.2).sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((8.5f * metrics.scale).coerceIn(6f, 12f).dp))

    Text(
      text = stringResource(R.string.calib_footer_encrypted),
      color = PSZinc500,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = metrics.spScaled(10.8f, 9.5f, 12.5f),
      letterSpacing = 0.2.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("summary_footer_encrypted_text"),
    )
  }
}
