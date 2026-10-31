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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val Zinc100 = Color(0xFFF4F4F5)
private val Zinc200 = Color(0xFFE4E4E7)
private val Zinc300 = Color(0xFFD4D4D8)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc500 = Color(0xFF71717A)
private val Zinc600 = Color(0xFF52525B)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc800 = Color(0xFF27272A)
private val Zinc850 = Color(0xFF1C1C20)
private val Zinc900 = Color(0xFF131317)
private val Zinc950 = Color(0xFF0A0A0D)
private val CardBgTop = Color(0xFF141418)
private val CardBgBottom = Color(0xFF0A0A0C)

private val SummaryEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val MonthShortLabels = listOf(
  "Jan", "Feb", "Mar", "Apr", "May", "Jun",
  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun safeBlurRadius(radius: Float): Float = radius.coerceAtLeast(0.5f)

@Immutable
private data class SummaryResponsiveMetrics(
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
  val sectionGap: Dp,
  val cardCornerRadius: Dp,
  val cardPadding: Dp,
  val ctaHeight: Dp,
) {
  fun spScaled(base: Float, minVal: Float, maxVal: Float): TextUnit =
    ((base * scale).coerceIn(minVal, maxVal) * fontCompensation).sp
}

private fun computeSummaryMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): SummaryResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val isExpanded = w >= 600f
  val widthFactor = (w / 392f).coerceIn(0.84f, 1.18f)
  val heightFactor = (h / 830f).coerceIn(0.76f, 1.12f)
  val scale = min(widthFactor, heightFactor * 1.05f).coerceIn(0.80f, 1.14f)
  val fontCompensation = if (fontScale > 1.12f) (1.12f / fontScale).coerceIn(0.76f, 1f) else 1f
  val sp: (Float, Float, Float) -> TextUnit = { base, minV, maxV ->
    ((base * scale).coerceIn(minV, maxV) * fontCompensation).sp
  }

  return SummaryResponsiveMetrics(
    scale = scale,
    fontCompensation = fontCompensation,
    contentMaxWidth = if (isExpanded) 580.dp else 480.dp,
    horizontalPadding = (20f * widthFactor).coerceIn(16f, 28f).dp,
    topContentPadding = (8f * heightFactor).coerceIn(5f, 14f).dp,
    bottomContentPadding = (18f * heightFactor).coerceIn(12f, 26f).dp,
    headerHeight = (46f * scale).coerceIn(42f, 54f).dp,
    titleFontSize = sp(25f, 21f, 29f),
    subtitleFontSize = sp(12.5f, 11f, 14f),
    subtitleLineHeight = sp(18f, 15.5f, 20.5f),
    sectionGap = (15f * heightFactor).coerceIn(12f, 20f).dp,
    cardCornerRadius = (14f * scale).coerceIn(12f, 16f).dp,
    cardPadding = (15f * scale).coerceIn(12f, 18f).dp,
    ctaHeight = (50f * scale).coerceIn(48f, 56f).dp,
  )
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
  heightUnit: HeightUnit,
  weightKg: Float,
  weightUnit: WeightUnit,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  onUpdateWorkoutDays: (Set<DayOfWeekOption>) -> Unit,
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  dayOne: DayOfWeekOption,
  mealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
  streakThreshold: StreakThresholdOption,
  climateBand: ClimateBandOption,
  bodyFatOverrideEnabled: Boolean,
  onToggleBodyFatOverride: (Boolean) -> Unit,
  bodyFatPercent: Float,
  onBodyFatPercentChange: (Float) -> Unit,
  isCustomTargetsMode: Boolean,
  onToggleCustomTargetsMode: (Boolean) -> Unit,
  customCaloriesKcal: Int,
  onCustomCaloriesChange: (Int) -> Unit,
  customProteinGrams: Int,
  onCustomProteinChange: (Int) -> Unit,
  customCarbsGrams: Int,
  onCustomCarbsChange: (Int) -> Unit,
  customFatsGrams: Int,
  onCustomFatsChange: (Int) -> Unit,
  customHydrationMl: Int,
  onCustomHydrationChange: (Int) -> Unit,
  customStepsCount: Int,
  onCustomStepsChange: (Int) -> Unit,
  protocolLockedIn: Boolean,
  onLockInProtocol: () -> Unit,
  onNavigateBack: () -> Unit,
  onQuickEditStep01: (focusSection: String?) -> Unit,
  onQuickEditStep02: (focusSection: String?) -> Unit,
  onQuickEditStep03: (focusSection: String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBack)

  val haptics = rememberBioSyncHaptics()
  val coroutineScope = rememberCoroutineScope()
  val headerStepPhase = remember { Animatable(0f) }
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  var inspectedTopic by remember { mutableStateOf<FormulaInspectionTopic?>(null) }
  var showDisclaimerModal by remember { mutableStateOf(false) }
  var isLockInDisclaimerFlow by remember { mutableStateOf(false) }
  var showWorkoutDaysPicker by remember { mutableStateOf(false) }
  var showHealthConnectDialog by remember { mutableStateOf(false) }
  var showAllowAllRequiredWarning by remember { mutableStateOf(false) }
  var acknowledgedAlertIds by remember { mutableStateOf(emptySet<String>()) }

  LaunchedEffect(Unit) {
    if (isCustomTargetsMode) {
      onToggleCustomTargetsMode(false)
    }
    headerStepPhase.snapTo(0f)
    headerStepPhase.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 720, easing = SummaryEaseOut),
    )
  }

  val report = remember(
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
    mealFrequency,
    caffeineAlcoholYes,
    sodiumFrequency,
    climateBand,
    bodyFatOverrideEnabled,
    bodyFatPercent,
    selectedWorkoutDays,
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
      mealFrequency = mealFrequency,
      caffeineAlcoholYes = caffeineAlcoholYes,
      sodiumFrequency = sodiumFrequency,
      climateBand = climateBand,
      bodyFatPercentOverride = if (bodyFatOverrideEnabled) bodyFatPercent else null,
      selectedWorkoutDays = selectedWorkoutDays,
    )
  }

  // Ensure custom values are initialized from recommended targets if they are 0 or uninitialized
  LaunchedEffect(
    report.recommendedCaloriesKcal,
    report.recommendedProteinGrams,
    report.recommendedCarbsGrams,
    report.recommendedFatsGrams,
    report.primaryRecommendedWaterMl,
    report.recommendedSteps,
  ) {
    if (!isCustomTargetsMode) {
      onCustomCaloriesChange(report.recommendedCaloriesKcal)
      onCustomProteinChange(report.recommendedProteinGrams)
      onCustomCarbsChange(report.recommendedCarbsGrams)
      onCustomFatsChange(report.recommendedFatsGrams)
      onCustomHydrationChange(report.primaryRecommendedWaterMl)
      onCustomStepsChange(report.recommendedSteps)
    } else {
      if (customCaloriesKcal <= 0) onCustomCaloriesChange(report.recommendedCaloriesKcal)
      if (customProteinGrams <= 0) onCustomProteinChange(report.recommendedProteinGrams)
      if (customCarbsGrams <= 0) onCustomCarbsChange(report.recommendedCarbsGrams)
      if (customFatsGrams <= 0) onCustomFatsChange(report.recommendedFatsGrams)
      if (customHydrationMl <= 0) onCustomHydrationChange(report.primaryRecommendedWaterMl)
      if (customStepsCount <= 0) onCustomStepsChange(report.recommendedSteps)
    }
  }

  val activeCalories = if (isCustomTargetsMode) customCaloriesKcal else report.recommendedCaloriesKcal
  val activeProtein = if (isCustomTargetsMode) customProteinGrams else report.recommendedProteinGrams
  val activeCarbs = if (isCustomTargetsMode) customCarbsGrams else report.recommendedCarbsGrams
  val activeFats = if (isCustomTargetsMode) customFatsGrams else report.recommendedFatsGrams
  val activeHydration = if (isCustomTargetsMode) customHydrationMl else report.primaryRecommendedWaterMl
  val activeSteps = if (isCustomTargetsMode) customStepsCount else report.recommendedSteps

  // Internal Health Connect, Google Fit & Mobile Step Sensor synchronization controller
  val stepSyncController = rememberBioSyncStepSyncController(
    decidedDailyStepsGoal = activeSteps,
    autoRequestPermissionsOnStart = false,
  )

  // Automatically complete lock-in if the user enables "Allow all" while the Health Connect modal is open
  LaunchedEffect(stepSyncController.state.isAllPermissionsAllowed, showHealthConnectDialog) {
    if (showHealthConnectDialog && stepSyncController.state.isAllPermissionsAllowed) {
      showHealthConnectDialog = false
      showAllowAllRequiredWarning = false
      haptics.confirm()
      onLockInProtocol()
    }
  }

  val attemptLockInWithHealthConnectGate: () -> Unit = {
    if (stepSyncController.state.isAllPermissionsAllowed) {
      showHealthConnectDialog = false
      showAllowAllRequiredWarning = false
      haptics.confirm()
      onLockInProtocol()
    } else {
      coroutineScope.launch {
        val isAllAllowed = stepSyncController.verifyAllPermissionsAllowed()
        if (isAllAllowed || stepSyncController.state.isAllPermissionsAllowed) {
          showHealthConnectDialog = false
          showAllowAllRequiredWarning = false
          haptics.confirm()
          onLockInProtocol()
        } else {
          showAllowAllRequiredWarning = false
          showHealthConnectDialog = true
        }
      }
    }
  }


  val customOverride = remember(
    isCustomTargetsMode,
    activeCalories,
    activeProtein,
    activeCarbs,
    activeFats,
    activeHydration,
    activeSteps,
  ) {
    CustomTargetsOverride(
      isCustomMode = isCustomTargetsMode,
      caloriesKcal = activeCalories,
      proteinGrams = activeProtein,
      carbsGrams = activeCarbs,
      fatsGrams = activeFats,
      hydrationMl = activeHydration,
      stepsCount = activeSteps,
    )
  }

  val healthAlerts = remember(report, customOverride) {
    DailyTargetsEngine.evaluateHealthImpacts(report, customOverride)
  }

  // Instantly pop up HealthImpactDisclaimerDialog the moment any custom target crosses outside its safe limit
  LaunchedEffect(healthAlerts, isCustomTargetsMode) {
    val activeIds = healthAlerts.map { it.id }.toSet()
    val stillActiveAcknowledged = acknowledgedAlertIds.intersect(activeIds)
    if (stillActiveAcknowledged.size != acknowledgedAlertIds.size) {
      acknowledgedAlertIds = stillActiveAcknowledged
    }
    if (isCustomTargetsMode && activeIds.isNotEmpty()) {
      val newlyTriggered = activeIds - stillActiveAcknowledged
      if (newlyTriggered.isNotEmpty()) {
        haptics.warning()
        acknowledgedAlertIds = stillActiveAcknowledged + newlyTriggered
        isLockInDisclaimerFlow = false
        showDisclaimerModal = true
      }
    } else if (!isCustomTargetsMode && acknowledgedAlertIds.isNotEmpty()) {
      acknowledgedAlertIds = emptySet()
    }
  }

  val restoreRecommendedTargets: () -> Unit = {
    haptics.confirm()
    acknowledgedAlertIds = emptySet()
    onCustomCaloriesChange(report.recommendedCaloriesKcal)
    onCustomProteinChange(report.recommendedProteinGrams)
    onCustomCarbsChange(report.recommendedCarbsGrams)
    onCustomFatsChange(report.recommendedFatsGrams)
    onCustomHydrationChange(report.primaryRecommendedWaterMl)
    onCustomStepsChange(report.recommendedSteps)
    onToggleCustomTargetsMode(false)
  }

  val fixSingleAlertMetric: (String) -> Unit = { alertId ->
    haptics.confirm()
    when {
      alertId.startsWith("calories_") -> onCustomCaloriesChange(report.recommendedCaloriesKcal)
      alertId.startsWith("protein_") -> onCustomProteinChange(report.recommendedProteinGrams)
      alertId.startsWith("carbs_") -> onCustomCarbsChange(report.recommendedCarbsGrams)
      alertId.startsWith("fats_") -> onCustomFatsChange(report.recommendedFatsGrams)
      alertId.startsWith("hydration_") -> onCustomHydrationChange(report.primaryRecommendedWaterMl)
      alertId.startsWith("steps_") -> onCustomStepsChange(report.recommendedSteps)
      alertId == "macro_calorie_mismatch" -> {
        val syncedKcal = ((4 * activeProtein + 4 * activeCarbs + 9 * activeFats) / 5f).roundToInt() * 5
        onCustomCaloriesChange(syncedKcal.coerceIn(900, 6500))
      }
    }
  }

  val headerStepPhaseProvider = remember(headerStepPhase) { { headerStepPhase.value } }

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
      val fontScale = LocalDensity.current.fontScale
      val metrics = remember(maxWidth, maxHeight, fontScale) {
        computeSummaryMetrics(maxWidth, maxHeight, fontScale)
      }
      val scrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .widthIn(max = metrics.contentMaxWidth)
          .fillMaxWidth()
          .verticalScroll(scrollState)
          .padding(
            start = metrics.horizontalPadding,
            end = metrics.horizontalPadding,
            top = (6f * metrics.scale).coerceIn(4f, 10f).dp,
            bottom = metrics.bottomContentPadding,
          )
          .testTag("protocol_summary_screen"),
      ) {
        // 1. Header Bar (BioSync + STEP 04 OF 04 + 100% Shiny White Bar)
        SummaryHeaderBar(
          onBackClick = {
            haptics.selection()
            onNavigateBack()
          },
          phaseProvider = headerStepPhaseProvider,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // 2. Eyebrow, Title & Subtitle
        Text(
          text = stringResource(R.string.summary_eyebrow),
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(11.2f, 9.5f, 13f),
          letterSpacing = 1.6.sp,
        )
        Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
        Text(
          text = stringResource(R.string.summary_title),
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.titleFontSize,
          letterSpacing = (-0.5).sp,
          modifier = Modifier.testTag("summary_title_text"),
        )
        Spacer(modifier = Modifier.height((5f * metrics.scale).coerceIn(3.5f, 7.5f).dp))
        Text(
          text = stringResource(R.string.summary_subtitle),
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.subtitleFontSize,
          lineHeight = metrics.subtitleLineHeight,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 3. Daily Targets Command Deck (Recommended vs Custom Edit)
        DailyTargetsCommandDeck(
          report = report,
          isCustomMode = isCustomTargetsMode,
          onSelectMode = { customMode ->
            if (customMode != isCustomTargetsMode) {
              haptics.selection()
              if (customMode) {
                acknowledgedAlertIds = emptySet()
                onCustomCaloriesChange(report.recommendedCaloriesKcal)
                onCustomProteinChange(report.recommendedProteinGrams)
                onCustomCarbsChange(report.recommendedCarbsGrams)
                onCustomFatsChange(report.recommendedFatsGrams)
                onCustomHydrationChange(report.primaryRecommendedWaterMl)
                onCustomStepsChange(report.recommendedSteps)
              }
              onToggleCustomTargetsMode(customMode)
            } else {
              haptics.tick()
            }
          },
          activeCalories = activeCalories,
          onCaloriesDelta = { delta ->
            onCustomCaloriesChange((activeCalories + delta).coerceIn(800, 6500))
          },
          activeProtein = activeProtein,
          onProteinDelta = { delta ->
            onCustomProteinChange((activeProtein + delta).coerceIn(15, 420))
          },
          activeCarbs = activeCarbs,
          onCarbsDelta = { delta ->
            onCustomCarbsChange((activeCarbs + delta).coerceIn(0, 900))
          },
          activeFats = activeFats,
          onFatsDelta = { delta ->
            onCustomFatsChange((activeFats + delta).coerceIn(10, 260))
          },
          activeHydration = activeHydration,
          onHydrationDelta = { delta ->
            onCustomHydrationChange((activeHydration + delta).coerceIn(500, 7500))
          },
          activeSteps = activeSteps,
          onStepsDelta = { delta ->
            onCustomStepsChange((activeSteps + delta).coerceIn(1000, 30000))
          },
          onSyncCaloriesToMacros = {
            haptics.confirm()
            val macroKcal = ((4 * activeProtein + 4 * activeCarbs + 9 * activeFats) / 5f).roundToInt() * 5
            onCustomCaloriesChange(macroKcal.coerceIn(800, 6500))
          },
          onBalanceCarbsToCalories = {
            haptics.confirm()
            val remCarbs = ((activeCalories - 4 * activeProtein - 9 * activeFats) / 4f).roundToInt().coerceAtLeast(0)
            onCustomCarbsChange(remCarbs)
          },
          onInspectFormula = { topic ->
            haptics.confirm()
            inspectedTopic = topic
          },
          onOpenAlertModal = {
            haptics.warning()
            isLockInDisclaimerFlow = false
            showDisclaimerModal = true
          },
          haptics = haptics,
          numberFormat = numberFormat,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. Clean Scientific Biometric Pipeline (BMI, Effective Weight, Resting BMR, Daily Burn TDEE + Body Fat % toggle)
        ScientificBiometricPipelineCard(
          report = report,
          bodyFatOverrideEnabled = bodyFatOverrideEnabled,
          onToggleBodyFatOverride = { enabled ->
            haptics.selection()
            onToggleBodyFatOverride(enabled)
          },
          bodyFatPercent = bodyFatPercent,
          onBodyFatPercentDelta = { delta ->
            val next = ((bodyFatPercent + delta) * 2f).roundToInt() / 2f
            onBodyFatPercentChange(next.coerceIn(4.0f, 55.0f))
          },
          onInspectFormula = { topic ->
            haptics.confirm()
            inspectedTopic = topic
          },
          haptics = haptics,
          numberFormat = numberFormat,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 5. Complete Onboarding Dossier & Instant Quick-Edit Teleportation (Steps 01, 02, 03)
        CompleteOnboardingDossierSection(
          subjectName = subjectName,
          selectedSex = selectedSex,
          birthDay = birthDay,
          birthMonthZeroBased = birthMonthZeroBased,
          birthYear = birthYear,
          ageYears = ageYears,
          heightCm = heightCm,
          heightUnit = heightUnit,
          weightKg = weightKg,
          weightUnit = weightUnit,
          bmi = report.bmi,
          bmiCategory = report.bmiCategoryStandard,
          activityLevel = activityLevel,
          selectedWorkoutDays = report.selectedWorkoutDays,
          trainingDaysPerWeek = report.trainingDaysPerWeek,
          restDaysPerWeek = report.restDaysPerWeek,
          onOpenWorkoutDaysDialog = {
            haptics.selection()
            showWorkoutDaysPicker = true
          },
          occupationLoad = occupationLoad,
          objective = objective,
          effectiveObjective = report.effectiveObjective,
          objectiveWasOverridden = report.objectiveWasOverridden,
          trainingModality = trainingModality,
          workoutDuration = workoutDuration,
          experienceLevel = experienceLevel,
          targetPace = targetPace,
          dayOne = dayOne,
          mealFrequency = mealFrequency,
          caffeineAlcoholYes = caffeineAlcoholYes,
          sodiumFrequency = sodiumFrequency,
          streakThreshold = streakThreshold,
          onQuickEditStep01 = { focus ->
            haptics.selection()
            onQuickEditStep01(focus)
          },
          onQuickEditStep02 = { focus ->
            haptics.selection()
            onQuickEditStep02(focus)
          },
          onQuickEditStep03 = { focus ->
            haptics.selection()
            onQuickEditStep03(focus)
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 6. Primary Lock In Protocol Button & Encrypted Footer
        SummaryLockInFooterSection(
          protocolLockedIn = protocolLockedIn,
          hasActiveHealthAlerts = healthAlerts.isNotEmpty(),
          alertCount = healthAlerts.size,
          onLockInClick = {
            if (healthAlerts.isNotEmpty()) {
              haptics.warning()
              isLockInDisclaimerFlow = true
              showDisclaimerModal = true
            } else {
              haptics.selection()
              attemptLockInWithHealthConnectGate()
            }
          },
          metrics = metrics,
        )
      }
    }
  }

  val currentInspectedTopic = inspectedTopic
  if (currentInspectedTopic != null) {
    FormulaInspectionDialog(
      topic = currentInspectedTopic,
      report = report,
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
      caffeineAlcoholYes = caffeineAlcoholYes,
      sodiumFrequency = sodiumFrequency,
      onDismiss = { inspectedTopic = null },
    )
  }

  if (showDisclaimerModal) {
    HealthImpactDisclaimerDialog(
      alerts = healthAlerts,
      isLockInConfirmation = isLockInDisclaimerFlow,
      onRestoreRecommended = {
        restoreRecommendedTargets()
        showDisclaimerModal = false
        if (isLockInDisclaimerFlow) {
          isLockInDisclaimerFlow = false
          attemptLockInWithHealthConnectGate()
        }
      },
      onConfirmProceedCustom = {
        haptics.selection()
        acknowledgedAlertIds = healthAlerts.map { it.id }.toSet()
        showDisclaimerModal = false
        isLockInDisclaimerFlow = false
        attemptLockInWithHealthConnectGate()
      },
      onDismiss = {
        acknowledgedAlertIds = healthAlerts.map { it.id }.toSet()
        showDisclaimerModal = false
        isLockInDisclaimerFlow = false
      },
      onFixSingleMetric = { alertId ->
        fixSingleAlertMetric(alertId)
      },
    )
  }

  if (showWorkoutDaysPicker && activityLevel != ActivityLevelOption.SEDENTARY) {
    WorkoutDaysPickerDialog(
      activityLevel = activityLevel,
      initialSelectedDays = report.selectedWorkoutDays,
      onConfirmDays = { chosenDays ->
        haptics.confirm()
        onUpdateWorkoutDays(chosenDays)
        showWorkoutDaysPicker = false
      },
      onDismiss = { showWorkoutDaysPicker = false },
    )
  }

  if (showHealthConnectDialog) {
    HealthConnectPermissionPromptDialog(
      dailyStepsGoal = activeSteps,
      isHealthConnectAllowAllGranted = stepSyncController.state.isHealthConnectAllowAllGranted,
      hasActivityRecognitionPermission = stepSyncController.state.hasActivityRecognitionPermission,
      isIgnoringBatteryOptimizations = stepSyncController.state.isIgnoringBatteryOptimizations,
      showAllowAllRequiredWarning = showAllowAllRequiredWarning,
      onConfirmAndConnect = {
        haptics.confirm()
        stepSyncController.requestAllowAllPermissions { allGranted ->
          if (allGranted) {
            showHealthConnectDialog = false
            showAllowAllRequiredWarning = false
            haptics.confirm()
            onLockInProtocol()
          } else {
            haptics.warning()
            showAllowAllRequiredWarning = true
          }
        }
      },
      onOpenHealthConnectSettings = {
        haptics.selection()
        stepSyncController.openHealthConnectAppOrSettings()
      },
      onRequestBatteryOptimizationExemption = {
        haptics.selection()
        stepSyncController.requestBatteryOptimizationExemption()
      },
      onDismiss = {
        haptics.selection()
        showHealthConnectDialog = false
      },
    )
  }
}



@Composable
private fun SummaryHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
  metrics: SummaryResponsiveMetrics,
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
          color = Zinc300,
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
        color = Zinc400,
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
      Spacer(
        modifier = Modifier
          .size((8.5f * metrics.scale).coerceIn(7f, 10.5f).dp)
          .drawWithCache {
            val glowPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(150, 255, 255, 255)
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
              drawCircle(color = PureWhite, radius = r, center = center)
            }
          },
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
            color = android.graphics.Color.argb(155, 255, 255, 255)
            maskFilter = BlurMaskFilter(safeBlurRadius(barH * 1.9f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val shinyCoreBrush = Brush.horizontalGradient(
          colors = listOf(Color(0xFFC8CCD4), Color(0xFFF2F4F7), PureWhite),
        )
        onDrawBehind {
          if (size.width <= 1f || size.height <= 0.5f) return@onDrawBehind
          val p = phaseProvider().coerceIn(0f, 1f)
          val activeFraction = 0.75f + (0.25f * p)
          val fillW = (size.width * activeFraction).coerceIn(barH, size.width)

          drawRoundRect(
            color = Zinc850,
            topLeft = Offset.Zero,
            size = Size(size.width, barH),
            cornerRadius = radius,
          )
          drawIntoCanvas { canvas ->
            canvas.drawRoundRect(0f, 0f, fillW, barH, barH / 2f, barH / 2f, outerGlowPaint)
          }
          drawRoundRect(
            brush = shinyCoreBrush,
            topLeft = Offset.Zero,
            size = Size(fillW, barH),
            cornerRadius = radius,
          )
        }
      },
  )
}

/**
 * Progress bar with an embedded Safe Range Zone (`[safeMin .. safeMax]`) and proportional scaling
 * so that 100% of the recommended target sits comfortably inside the bar (~72-80%) rather than
 * pegging to the right edge at 100%. No hexagon shapes or decorative clutter.
 */
@Composable
private fun SafeRangeTelemetryBar(
  currentValue: Int,
  safeMin: Int,
  safeMax: Int,
  gradientColors: List<Color>,
  barHeight: Dp,
  modifier: Modifier = Modifier,
) {
  val scaleMax = remember(safeMax, safeMin) {
    max((safeMax * 1.22f).roundToInt(), safeMin + 100).coerceAtLeast(1)
  }
  val targetFraction = (currentValue.toFloat() / scaleMax.toFloat()).coerceIn(0.03f, 1.0f)
  val animatedFraction by animateFloatAsState(
    targetValue = targetFraction,
    animationSpec = tween(durationMillis = 360, easing = SummaryEaseOut),
    label = "safe_range_bar_fill",
  )
  val safeStartFraction = (safeMin.toFloat() / scaleMax.toFloat()).coerceIn(0.0f, 0.92f)
  val safeEndFraction = (safeMax.toFloat() / scaleMax.toFloat()).coerceIn(safeStartFraction + 0.04f, 0.96f)
  val isWithinSafeRange = currentValue in safeMin..safeMax

  val accentColor = gradientColors.firstOrNull() ?: PureWhite

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(barHeight),
  ) {
    val w = size.width
    val h = size.height
    if (w <= 2f || h <= 1f) return@Canvas
    val corner = CornerRadius(h / 2f, h / 2f)

    // 1. Deep Obsidian Track
    drawRoundRect(
      color = Color(0xFF141418),
      topLeft = Offset.Zero,
      size = Size(w, h),
      cornerRadius = corner,
    )

    // 2. Safe Range Band inside Track
    val safeStartX = w * safeStartFraction
    val safeEndX = w * safeEndFraction
    val safeBandWidth = (safeEndX - safeStartX).coerceAtLeast(4f)
    drawRoundRect(
      color = PureWhite.copy(alpha = 0.09f),
      topLeft = Offset(safeStartX, 0f),
      size = Size(safeBandWidth, h),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
    )

    // 3. Active Progress Fill
    val fillW = (w * animatedFraction).coerceIn(h, w)
    val fillBrush = if (isWithinSafeRange) {
      Brush.horizontalGradient(
        colors = gradientColors + listOf(PureWhite.copy(alpha = 0.92f)),
        startX = 0f,
        endX = fillW,
      )
    } else {
      Brush.horizontalGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444), PureWhite),
        startX = 0f,
        endX = fillW,
      )
    }
    drawRoundRect(
      brush = fillBrush,
      topLeft = Offset.Zero,
      size = Size(fillW, h),
      cornerRadius = corner,
    )

    // 4. Crisp Safe Range Boundary Ticks (Min & Max)
    val tickStroke = 1.3.dp.toPx()
    drawLine(
      color = PureWhite.copy(alpha = 0.55f),
      start = Offset(safeStartX, 0f),
      end = Offset(safeStartX, h),
      strokeWidth = tickStroke,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = PureWhite.copy(alpha = 0.55f),
      start = Offset(safeEndX, 0f),
      end = Offset(safeEndX, h),
      strokeWidth = tickStroke,
      cap = StrokeCap.Round,
    )

    // 5. Track Border
    drawRoundRect(
      color = if (isWithinSafeRange) Zinc700.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.65f),
      topLeft = Offset.Zero,
      size = Size(w, h),
      cornerRadius = corner,
      style = Stroke(width = 0.8.dp.toPx()),
    )
  }
}

@Composable
private fun DailyTargetsCommandDeck(
  report: DailyTargetsCalculationReport,
  isCustomMode: Boolean,
  onSelectMode: (Boolean) -> Unit,
  activeCalories: Int,
  onCaloriesDelta: (Int) -> Unit,
  activeProtein: Int,
  onProteinDelta: (Int) -> Unit,
  activeCarbs: Int,
  onCarbsDelta: (Int) -> Unit,
  activeFats: Int,
  onFatsDelta: (Int) -> Unit,
  activeHydration: Int,
  onHydrationDelta: (Int) -> Unit,
  activeSteps: Int,
  onStepsDelta: (Int) -> Unit,
  onSyncCaloriesToMacros: () -> Unit,
  onBalanceCarbsToCalories: () -> Unit,
  onInspectFormula: (FormulaInspectionTopic) -> Unit,
  onOpenAlertModal: () -> Unit,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  metrics: SummaryResponsiveMetrics,
) {
  val totalMacroKcal = 4 * activeProtein + 4 * activeCarbs + 9 * activeFats
  val macroCalorieDiff = totalMacroKcal - activeCalories
  val isMacroOutOfSync = isCustomMode && abs(macroCalorieDiff) > 60

  val safeTotalMacroKcal = totalMacroKcal.coerceAtLeast(1)
  val proteinPct = ((4f * activeProtein / safeTotalMacroKcal) * 100f).roundToInt()
  val fatPct = ((9f * activeFats / safeTotalMacroKcal) * 100f).roundToInt()
  val carbsPct = (100 - proteinPct - fatPct).coerceIn(0, 100)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(metrics.cardCornerRadius))
      .background(Brush.verticalGradient(colors = listOf(CardBgTop, CardBgBottom)))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.22f), Zinc800),
        ),
        shape = RoundedCornerShape(metrics.cardCornerRadius),
      )
      .padding(metrics.cardPadding)
      .testTag("daily_targets_command_deck"),
  ) {
    // Top Header Row: DAILY TARGETS + Hold Hint
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(PureWhite),
        )
        Text(
          text = "DAILY TARGETS ENGINE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(11f, 9.5f, 12.5f),
          letterSpacing = 1.2.sp,
        )
      }

      Text(
        text = "HOLD METRIC FOR FORMULA",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.2f, 8.2f, 10.5f),
        letterSpacing = 0.6.sp,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Segmented Mode Switcher: RECOMMENDED vs CUSTOM EDIT
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(ObsidianBlack)
        .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val recBg = if (!isCustomMode) PureWhite else Color.Transparent
      val recText = if (!isCustomMode) ObsidianBlack else Zinc400
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(7.dp))
          .background(recBg)
          .clickable(role = Role.Tab) { onSelectMode(false) }
          .testTag("mode_tab_recommended"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.summary_mode_recommended),
          color = recText,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(10.8f, 9.5f, 12f),
          letterSpacing = 0.8.sp,
        )
      }

      val custBg = if (isCustomMode) PureWhite else Color.Transparent
      val custText = if (isCustomMode) ObsidianBlack else Zinc400
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(7.dp))
          .background(custBg)
          .clickable(role = Role.Tab) { onSelectMode(true) }
          .testTag("mode_tab_custom"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.summary_mode_custom),
          color = custText,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(10.8f, 9.5f, 12f),
          letterSpacing = 0.8.sp,
        )
      }
    }

    // Subtle instruction visible ONLY in Custom Edit mode (RESET TO RECOMMENDED block removed as requested)
    AnimatedVisibility(
      visible = isCustomMode,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Tap or hold − / + to adjust any goal",
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(11f, 9.8f, 12.2f),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ── A. Hero Daily Energy Goal (Calories) ─────────────────────────────────
    val calInSafeRange = activeCalories in report.calorieSafetyFloorKcal..report.calorieSafeCeilingKcal
    val deltaFromTdee = activeCalories - report.tdeeKcalRounded
    val deltaText = when {
      abs(deltaFromTdee) <= 15 -> "Balanced Maintenance (${numberFormat.format(report.tdeeKcalRounded)} kcal TDEE)"
      deltaFromTdee < 0 -> "${deltaFromTdee} kcal Deficit • ~${String.format(Locale.US, "%.2f", abs(deltaFromTdee) * 7f / 7700f)} kg/wk"
      else -> "+${deltaFromTdee} kcal Surplus • ~+${String.format(Locale.US, "%.2f", deltaFromTdee * 7f / 7700f)} kg/wk"
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(ObsidianBlack)
        .border(
          width = 1.dp,
          color = if (calInSafeRange) Zinc800 else Color(0xFFF59E0B).copy(alpha = 0.7f),
          shape = RoundedCornerShape(12.dp),
        )
        .padding(13.dp)
        .testTag("target_card_calories"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .shortHoldOrTap(
            holdDurationMs = 420L,
            onTap = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
            onShortHold = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
          ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          // Clean vertical pill indicator
          Box(
            modifier = Modifier
              .width(4.dp)
              .height(14.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Brush.verticalGradient(listOf(CaloriesStart, CaloriesMid))),
          )
          Text(
            text = stringResource(R.string.summary_daily_energy_label),
            color = Zinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(10.8f, 9.5f, 12f),
            letterSpacing = 1.0.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        // Left metric info column has shortHoldOrTap; steppers on the right do NOT
        Column(
          modifier = Modifier
            .weight(1f)
            .shortHoldOrTap(
              holdDurationMs = 420L,
              onTap = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
              onShortHold = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
            ),
        ) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = numberFormat.format(activeCalories),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(27f, 22f, 31f),
              letterSpacing = (-0.8).sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "kcal / day",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(12.5f, 11f, 14f),
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = deltaText,
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(11.2f, 9.8f, 12.5f),
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Safe ${numberFormat.format(report.calorieSafetyFloorKcal)}–${numberFormat.format(report.calorieSafeCeilingKcal)} kcal",
            color = if (calInSafeRange) CaloriesStart else Color(0xFFFBBF24),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(9.8f, 8.8f, 11f),
          )
        }

        // Steppers visible ONLY in Custom Edit mode — isolated from shortHoldOrTap
        if (isCustomMode) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            HoldRepeatStepperButton(
              symbol = "−",
              accentColor = CaloriesStart,
              onStep = { mult -> onCaloriesDelta(-25 * mult) },
              haptics = haptics,
              buttonSize = 38.dp,
              testTag = "calories_minus_btn",
            )
            HoldRepeatStepperButton(
              symbol = "+",
              accentColor = CaloriesStart,
              onStep = { mult -> onCaloriesDelta(25 * mult) },
              haptics = haptics,
              buttonSize = 38.dp,
              testTag = "calories_plus_btn",
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Straight-line Progress Bar with Safe Range inside (NO hexagon!)
      SafeRangeTelemetryBar(
        currentValue = activeCalories,
        safeMin = report.calorieSafetyFloorKcal,
        safeMax = report.calorieSafeCeilingKcal,
        gradientColors = listOf(CaloriesEnd, CaloriesMid, CaloriesStart),
        barHeight = 9.dp,
        modifier = Modifier.shortHoldOrTap(
          holdDurationMs = 420L,
          onTap = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
          onShortHold = { onInspectFormula(FormulaInspectionTopic.CALORIES) },
        ),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // ── B. Compact Macro Grid (Protein, Carbs, Fats — Uniform Pill Shape) ────
    if (!isCustomMode) {
      // In Recommended Mode: sleek 3-column bento row for concise vertical height
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CompactMacroBentoTile(
          title = "Protein",
          grams = activeProtein,
          sharePct = proteinPct,
          safeMin = report.proteinFloorGrams,
          safeMax = report.proteinCeilingGrams,
          gradientColors = listOf(ProteinEnd, ProteinMid, ProteinStart),
          accentColor = ProteinStart,
          onInspect = { onInspectFormula(FormulaInspectionTopic.PROTEIN) },
          metrics = metrics,
          modifier = Modifier
            .weight(1f)
            .testTag("target_card_protein"),
        )
        CompactMacroBentoTile(
          title = "Carbs",
          grams = activeCarbs,
          sharePct = carbsPct,
          safeMin = report.carbsFloorGrams,
          safeMax = report.carbsCeilingGrams,
          gradientColors = listOf(CarbsEnd, CarbsMid, CarbsStart),
          accentColor = CarbsStart,
          onInspect = { onInspectFormula(FormulaInspectionTopic.CARBS) },
          metrics = metrics,
          modifier = Modifier
            .weight(1f)
            .testTag("target_card_carbs"),
        )
        CompactMacroBentoTile(
          title = "Fats",
          grams = activeFats,
          sharePct = fatPct,
          safeMin = report.fatFloorGrams,
          safeMax = report.fatCeilingGrams,
          gradientColors = listOf(FatsEnd, FatsMid, FatsStart),
          accentColor = FatsStart,
          onInspect = { onInspectFormula(FormulaInspectionTopic.FATS) },
          metrics = metrics,
          modifier = Modifier
            .weight(1f)
            .testTag("target_card_fats"),
        )
      }
    } else {
      // In Custom Edit Mode: interactive rows with hold-to-repeat − / + steppers
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CustomEditableMacroRow(
          title = "Protein",
          grams = activeProtein,
          sharePct = proteinPct,
          safeMin = report.proteinFloorGrams,
          safeMax = report.proteinCeilingGrams,
          gradientColors = listOf(ProteinEnd, ProteinMid, ProteinStart),
          accentColor = ProteinStart,
          onStepDelta = { mult -> onProteinDelta(2 * mult) },
          onInspect = { onInspectFormula(FormulaInspectionTopic.PROTEIN) },
          haptics = haptics,
          metrics = metrics,
          minusTestTag = "protein_minus_btn",
          plusTestTag = "protein_plus_btn",
        )
        CustomEditableMacroRow(
          title = "Carbohydrates",
          grams = activeCarbs,
          sharePct = carbsPct,
          safeMin = report.carbsFloorGrams,
          safeMax = report.carbsCeilingGrams,
          gradientColors = listOf(CarbsEnd, CarbsMid, CarbsStart),
          accentColor = CarbsStart,
          onStepDelta = { mult -> onCarbsDelta(5 * mult) },
          onInspect = { onInspectFormula(FormulaInspectionTopic.CARBS) },
          haptics = haptics,
          metrics = metrics,
          minusTestTag = "carbs_minus_btn",
          plusTestTag = "carbs_plus_btn",
        )
        CustomEditableMacroRow(
          title = "Healthy Fats",
          grams = activeFats,
          sharePct = fatPct,
          safeMin = report.fatFloorGrams,
          safeMax = report.fatCeilingGrams,
          gradientColors = listOf(FatsEnd, FatsMid, FatsStart),
          accentColor = FatsStart,
          onStepDelta = { mult -> onFatsDelta(2 * mult) },
          onInspect = { onInspectFormula(FormulaInspectionTopic.FATS) },
          haptics = haptics,
          metrics = metrics,
          minusTestTag = "fats_minus_btn",
          plusTestTag = "fats_plus_btn",
        )

        // Friendly Macro-to-Calorie Alignment Bar (NO "4P+4C+9F" math jargon!)
        AnimatedVisibility(
          visible = isMacroOutOfSync,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1A150E))
              .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
              .padding(11.dp),
          ) {
            Text(
              text = if (macroCalorieDiff > 0) {
                "Macro grams equal ${numberFormat.format(totalMacroKcal)} kcal (+${macroCalorieDiff} kcal above your Calorie goal)"
              } else {
                "Macro grams equal ${numberFormat.format(totalMacroKcal)} kcal (${abs(macroCalorieDiff)} kcal below your Calorie goal)"
              },
              color = Color(0xFFFDE68A),
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(11.5f, 10f, 12.8f),
            )
            Spacer(modifier = Modifier.height(7.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(7.dp))
                  .background(PureWhite)
                  .clickable(onClick = onSyncCaloriesToMacros)
                  .padding(vertical = 7.dp)
                  .testTag("sync_calories_to_macros_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "Match Calories",
                  color = ObsidianBlack,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(11f, 9.8f, 12.2f),
                )
              }
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(7.dp))
                  .background(Zinc850)
                  .border(1.dp, Zinc700, RoundedCornerShape(7.dp))
                  .clickable(onClick = onBalanceCarbsToCalories)
                  .padding(vertical = 7.dp)
                  .testTag("balance_carbs_to_calories_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "Auto-Balance Carbs",
                  color = PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = metrics.spScaled(11f, 9.8f, 12.2f),
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // ── C. 2-Column Hydration & Daily Steps Strip (No Hexagon/Circle Clutter) ─
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      VitalTelemetryCard(
        title = "Hydration",
        primaryValue = "${numberFormat.format(activeHydration)} ml",
        subDetail = "~${String.format(Locale.US, "%.1f", activeHydration / 250f)} glasses • ${((activeHydration / 16f) / 10f).roundToInt() * 10} ml/hr",
        safeRangeLabel = "Safe ${numberFormat.format(report.waterFloorMl)}–${numberFormat.format(report.waterCeilingMl)} ml",
        currentValue = activeHydration,
        safeMin = report.waterFloorMl,
        safeMax = report.waterCeilingMl,
        gradientColors = listOf(HydrationEnd, HydrationMid, HydrationStart),
        accentColor = HydrationStart,
        isCustomMode = isCustomMode,
        onStepDelta = { mult -> onHydrationDelta(50 * mult) },
        onInspect = { onInspectFormula(FormulaInspectionTopic.HYDRATION) },
        onOpenAlertModal = onOpenAlertModal,
        haptics = haptics,
        metrics = metrics,
        minusTestTag = "hydration_minus_btn",
        plusTestTag = "hydration_plus_btn",
        modifier = Modifier
          .weight(1f)
          .testTag("target_card_hydration"),
      )

      val estKm = (activeSteps * report.strideLengthMeters) / 1000f
      val estStepKcal = (activeSteps * report.actualWeightKg * 0.0004f).roundToInt()
      VitalTelemetryCard(
        title = "Daily Steps",
        primaryValue = numberFormat.format(activeSteps),
        subDetail = "~${String.format(Locale.US, "%.1f", estKm)} km • ~${estStepKcal} kcal",
        safeRangeLabel = "Safe ${numberFormat.format(report.stepsFloor)}–${numberFormat.format(report.stepsCeiling)}",
        currentValue = activeSteps,
        safeMin = report.stepsFloor,
        safeMax = report.stepsCeiling,
        gradientColors = listOf(StepsEnd, StepsMid, StepsStart),
        accentColor = StepsStart,
        isCustomMode = isCustomMode,
        onStepDelta = { mult -> onStepsDelta(250 * mult) },
        onInspect = { onInspectFormula(FormulaInspectionTopic.STEPS) },
        onOpenAlertModal = onOpenAlertModal,
        haptics = haptics,
        metrics = metrics,
        minusTestTag = "steps_minus_btn",
        plusTestTag = "steps_plus_btn",
        modifier = Modifier
          .weight(1f)
          .testTag("target_card_steps"),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ── D. Elevated Per-Meal Allocation & Daily Essentials Telemetry Cards ────
    val perMealKcal = (activeCalories.toFloat() / report.mealCount.toFloat()).roundToInt()
    val perMealP = (activeProtein.toFloat() / report.mealCount.toFloat()).roundToInt()
    val perMealC = (activeCarbs.toFloat() / report.mealCount.toFloat()).roundToInt()
    val perMealF = (activeFats.toFloat() / report.mealCount.toFloat()).roundToInt()

    PerMealAndEssentialsSection(
      mealCount = report.mealCount,
      perMealKcal = perMealKcal,
      perMealProtein = perMealP,
      perMealCarbs = perMealC,
      perMealFats = perMealF,
      recommendedFibreGrams = report.recommendedFibreGrams,
      maxFreeSugarGrams = report.maxFreeSugarGrams,
      numberFormat = numberFormat,
      metrics = metrics,
    )
  }
}

@Composable
private fun PerMealAndEssentialsSection(
  mealCount: Int,
  perMealKcal: Int,
  perMealProtein: Int,
  perMealCarbs: Int,
  perMealFats: Int,
  recommendedFibreGrams: Int,
  maxFreeSugarGrams: Int,
  numberFormat: NumberFormat,
  metrics: SummaryResponsiveMetrics,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("per_meal_and_essentials_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1. PER-MEAL ALLOCATION CARD (4-column balanced telemetry bento)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(ObsidianBlack)
        .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
        .padding(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Box(
            modifier = Modifier
              .width(3.5.dp)
              .height(13.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Brush.verticalGradient(listOf(PureWhite, Zinc400))),
          )
          Text(
            text = "PER MEAL TARGET",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(11f, 9.8f, 12.4f),
            letterSpacing = 0.9.sp,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PureWhite)
            .border(1.dp, PureWhite, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.5.dp, vertical = 3.dp),
        ) {
          Text(
            text = "${mealCount}× MEALS / DAY",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(9.5f, 8.5f, 10.8f),
            letterSpacing = 0.6.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(9.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        PerMealMacroChip(
          label = "Energy",
          value = "~${numberFormat.format(perMealKcal)}",
          unit = "kcal",
          accentColor = CaloriesStart,
          metrics = metrics,
          modifier = Modifier.weight(1.15f),
        )
        PerMealMacroChip(
          label = "Protein",
          value = "$perMealProtein",
          unit = "g",
          accentColor = ProteinStart,
          metrics = metrics,
          modifier = Modifier.weight(0.95f),
        )
        PerMealMacroChip(
          label = "Carbs",
          value = "$perMealCarbs",
          unit = "g",
          accentColor = CarbsStart,
          metrics = metrics,
          modifier = Modifier.weight(0.95f),
        )
        PerMealMacroChip(
          label = "Fats",
          value = "$perMealFats",
          unit = "g",
          accentColor = FatsStart,
          metrics = metrics,
          modifier = Modifier.weight(0.95f),
        )
      }
    }

    // 2. DAILY ESSENTIALS CARD (3-column balanced micro-target cards)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(ObsidianBlack)
        .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
        .padding(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Box(
            modifier = Modifier
              .width(3.5.dp)
              .height(13.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Brush.verticalGradient(listOf(StepsStart, HydrationStart))),
          )
          Text(
            text = "DAILY ESSENTIALS",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(11f, 9.8f, 12.4f),
            letterSpacing = 0.9.sp,
          )
        }

        Text(
          text = "MICRO & GLYCEMIC GUARDS",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(9.2f, 8.2f, 10.5f),
          letterSpacing = 0.5.sp,
        )
      }

      Spacer(modifier = Modifier.height(9.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        EssentialMicroTile(
          title = "Dietary Fibre",
          value = "${recommendedFibreGrams}g",
          subTag = "Gut & Satiety",
          accentColor = StepsStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        EssentialMicroTile(
          title = "Free Sugar",
          value = "<${maxFreeSugarGrams}g",
          subTag = "Insulin Guard",
          accentColor = CarbsStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        EssentialMicroTile(
          title = "Sodium Cap",
          value = "<2,000mg",
          subTag = "Fluid Balance",
          accentColor = HydrationStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun PerMealMacroChip(
  label: String,
  value: String,
  unit: String,
  accentColor: Color,
  metrics: SummaryResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .background(Zinc950)
      .border(1.dp, Zinc850, RoundedCornerShape(9.dp))
      .padding(horizontal = 8.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(
        modifier = Modifier
          .size(5.dp)
          .clip(CircleShape)
          .background(accentColor),
      )
      Text(
        text = label,
        color = Zinc300,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(10.2f, 9f, 11.4f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = value,
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(14.5f, 12.5f, 16.5f),
        maxLines = 1,
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = unit,
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(10f, 8.8f, 11.2f),
        modifier = Modifier.padding(bottom = 1.dp),
      )
    }
  }
}

@Composable
private fun EssentialMicroTile(
  title: String,
  value: String,
  subTag: String,
  accentColor: Color,
  metrics: SummaryResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .background(Zinc950)
      .border(1.dp, Zinc850, RoundedCornerShape(9.dp))
      .padding(horizontal = 9.dp, vertical = 8.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(10.dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(accentColor),
      )
      Text(
        text = title,
        color = Zinc300,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(10.4f, 9.2f, 11.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(14.5f, 12.5f, 16.5f),
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = subTag,
      color = accentColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun CompactMacroBentoTile(
  title: String,
  grams: Int,
  sharePct: Int,
  safeMin: Int,
  safeMax: Int,
  gradientColors: List<Color>,
  accentColor: Color,
  onInspect: () -> Unit,
  metrics: SummaryResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  val inSafe = grams in safeMin..safeMax
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(11.dp))
      .background(ObsidianBlack)
      .border(
        width = 1.dp,
        color = if (inSafe) Zinc800 else Color(0xFFF59E0B).copy(alpha = 0.65f),
        shape = RoundedCornerShape(11.dp),
      )
      .shortHoldOrTap(
        holdDurationMs = 420L,
        onTap = onInspect,
        onShortHold = onInspect,
      )
      .padding(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        // Uniform pill indicator shape across Protein, Carbs, and Fats
        Box(
          modifier = Modifier
            .width(3.5.dp)
            .height(11.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(accentColor),
        )
        Text(
          text = title,
          color = Zinc300,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(11.5f, 10f, 13f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = "$sharePct%",
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(10f, 8.8f, 11.5f),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "$grams",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(20f, 17f, 23f),
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = "g",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = metrics.spScaled(11.5f, 10f, 13f),
        modifier = Modifier.padding(bottom = 2.dp),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    SafeRangeTelemetryBar(
      currentValue = grams,
      safeMin = safeMin,
      safeMax = safeMax,
      gradientColors = gradientColors,
      barHeight = 6.5.dp,
    )

    Spacer(modifier = Modifier.height(5.dp))

    Text(
      text = "Safe $safeMin–${safeMax}g",
      color = if (inSafe) accentColor else Color(0xFFFBBF24),
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(9f, 8f, 10.2f),
      maxLines = 1,
    )
  }
}

@Composable
private fun CustomEditableMacroRow(
  title: String,
  grams: Int,
  sharePct: Int,
  safeMin: Int,
  safeMax: Int,
  gradientColors: List<Color>,
  accentColor: Color,
  onStepDelta: (Int) -> Unit,
  onInspect: () -> Unit,
  haptics: BioSyncHapticController,
  metrics: SummaryResponsiveMetrics,
  minusTestTag: String,
  plusTestTag: String,
) {
  val inSafe = grams in safeMin..safeMax
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(11.dp))
      .background(ObsidianBlack)
      .border(
        width = 1.dp,
        color = if (inSafe) Zinc800 else Color(0xFFF59E0B).copy(alpha = 0.7f),
        shape = RoundedCornerShape(11.dp),
      )
      .padding(11.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Only the left info column responds to shortHoldOrTap; − / + buttons on the right do NOT
      Column(
        modifier = Modifier
          .weight(1f)
          .shortHoldOrTap(
            holdDurationMs = 420L,
            onTap = onInspect,
            onShortHold = onInspect,
          ),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .width(3.5.dp)
              .height(12.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(accentColor),
          )
          Text(
            text = title,
            color = Zinc200,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(12.5f, 11f, 14f),
          )
          Text(
            text = "• $sharePct% (${grams * if (title.contains("Fat")) 9 else 4} kcal)",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = metrics.spScaled(10f, 9f, 11.5f),
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "${grams}g",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(19f, 16.5f, 22f),
          )
          Text(
            text = "Safe $safeMin–${safeMax}g",
            color = if (inSafe) accentColor else Color(0xFFFBBF24),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(10f, 9f, 11.2f),
            modifier = Modifier.padding(bottom = 2.dp),
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HoldRepeatStepperButton(
          symbol = "−",
          accentColor = accentColor,
          onStep = { mult -> onStepDelta(-mult) },
          haptics = haptics,
          buttonSize = 36.dp,
          testTag = minusTestTag,
        )
        HoldRepeatStepperButton(
          symbol = "+",
          accentColor = accentColor,
          onStep = { mult -> onStepDelta(mult) },
          haptics = haptics,
          buttonSize = 36.dp,
          testTag = plusTestTag,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    SafeRangeTelemetryBar(
      currentValue = grams,
      safeMin = safeMin,
      safeMax = safeMax,
      gradientColors = gradientColors,
      barHeight = 7.dp,
      modifier = Modifier.shortHoldOrTap(
        holdDurationMs = 420L,
        onTap = onInspect,
        onShortHold = onInspect,
      ),
    )
  }
}

@Composable
private fun VitalTelemetryCard(
  title: String,
  primaryValue: String,
  subDetail: String,
  safeRangeLabel: String,
  currentValue: Int,
  safeMin: Int,
  safeMax: Int,
  gradientColors: List<Color>,
  accentColor: Color,
  isCustomMode: Boolean,
  onStepDelta: (Int) -> Unit,
  onInspect: () -> Unit,
  onOpenAlertModal: () -> Unit,
  haptics: BioSyncHapticController,
  metrics: SummaryResponsiveMetrics,
  minusTestTag: String,
  plusTestTag: String,
  modifier: Modifier = Modifier,
) {
  val inSafe = currentValue in safeMin..safeMax
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(11.dp))
      .background(ObsidianBlack)
      .border(
        width = 1.dp,
        color = if (inSafe) Zinc800 else Color(0xFFF59E0B).copy(alpha = 0.7f),
        shape = RoundedCornerShape(11.dp),
      )
      .padding(11.dp),
  ) {
    // Inner Column holds shortHoldOrTap so steppers below never trigger formula calculation popup
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shortHoldOrTap(
          holdDurationMs = 420L,
          onTap = onInspect,
          onShortHold = onInspect,
        ),
    ) {
      // Clean title without hexagon, circle shapes, or redundant SAFE badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title.uppercase(Locale.US),
          color = Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
          letterSpacing = 0.8.sp,
        )
        if (!inSafe) {
          Text(
            text = "ALERT",
            color = Color(0xFFFBBF24),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(9f, 8f, 10.2f),
            modifier = Modifier.clickable { onOpenAlertModal() },
          )
        }
      }

      Spacer(modifier = Modifier.height(5.dp))

      Text(
        text = primaryValue,
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(19f, 16f, 22f),
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = subDetail,
        color = Zinc400,
        fontFamily = InterFontFamily,
        fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(7.dp))

      SafeRangeTelemetryBar(
        currentValue = currentValue,
        safeMin = safeMin,
        safeMax = safeMax,
        gradientColors = gradientColors,
        barHeight = 6.5.dp,
      )

      Spacer(modifier = Modifier.height(5.dp))

      // Respective category accent color for Safe section text (HydrationStart / StepsStart)
      Text(
        text = safeRangeLabel,
        color = if (inSafe) accentColor else Color(0xFFFBBF24),
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
      )
    }

    if (isCustomMode) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        HoldRepeatStepperButton(
          symbol = "−",
          accentColor = accentColor,
          onStep = { mult -> onStepDelta(-mult) },
          haptics = haptics,
          buttonSize = 34.dp,
          modifier = Modifier.weight(1f),
          testTag = minusTestTag,
        )
        HoldRepeatStepperButton(
          symbol = "+",
          accentColor = accentColor,
          onStep = { mult -> onStepDelta(mult) },
          haptics = haptics,
          buttonSize = 34.dp,
          modifier = Modifier.weight(1f),
          testTag = plusTestTag,
        )
      }
    }
  }
}

// ─── Upgraded Metabolic Baseline Pipeline Section ────────────────────────────

@Composable
private fun ScientificBiometricPipelineCard(
  report: DailyTargetsCalculationReport,
  bodyFatOverrideEnabled: Boolean,
  onToggleBodyFatOverride: (Boolean) -> Unit,
  bodyFatPercent: Float,
  onBodyFatPercentDelta: (Float) -> Unit,
  onInspectFormula: (FormulaInspectionTopic) -> Unit,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  metrics: SummaryResponsiveMetrics,
) {
  val bmiProgress = ((report.bmi - 14f) / 22f).coerceIn(0.12f, 0.96f)
  val massRatioProgress = (report.effectiveWeightKg / report.actualWeightKg.coerceAtLeast(1f)).coerceIn(0.25f, 1.0f)
  val bmrShareOfTdee = (report.bmrKcalRounded.toFloat() / report.tdeeKcalRounded.coerceAtLeast(1).toFloat()).coerceIn(0.45f, 0.90f)
  val tdeeProgress = ((report.totalActivityFactor - 1.0f) / 1.15f).coerceIn(0.22f, 0.96f)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(metrics.cardCornerRadius))
      .background(Brush.verticalGradient(colors = listOf(CardBgTop, CardBgBottom)))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.20f), Zinc800),
        ),
        shape = RoundedCornerShape(metrics.cardCornerRadius),
      )
      .padding(metrics.cardPadding)
      .testTag("scientific_pipeline_card"),
  ) {
    // Top Header (TAP OR HOLD NODE block removed as requested)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(ProteinStart),
        )
        Text(
          text = "METABOLIC BASELINE PIPELINE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(11f, 9.5f, 12.4f),
          letterSpacing = 1.1.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height(9.dp))

    // 4-Stage Sequential Pipeline Flow Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianBlack)
        .border(1.dp, Zinc850, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PipelineStageChip("01", "BMI", ProteinStart, metrics)
      Text("›", color = Zinc600, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
      PipelineStageChip("02", "MASS", StepsStart, metrics)
      Text("›", color = Zinc600, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
      PipelineStageChip("03", "BMR", CarbsStart, metrics)
      Text("›", color = Zinc600, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
      PipelineStageChip("04", "TDEE", CaloriesStart, metrics)
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Row 1: BMI & Effective Weight
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PipelineMetricTile(
        stageIndex = "01",
        label = "BMI INDEX",
        value = "${String.format(Locale.US, "%.1f", report.bmi)} kg/m²",
        badgeText = report.bmiCategoryStandard,
        subLabel = "Stature-to-Mass",
        accentColor = ProteinStart,
        secondaryColor = ProteinMid,
        progressFraction = bmiProgress,
        onClick = { onInspectFormula(FormulaInspectionTopic.BMI) },
        metrics = metrics,
        modifier = Modifier.weight(1f),
      )
      PipelineMetricTile(
        stageIndex = "02",
        label = "EFFECTIVE WEIGHT",
        value = "${String.format(Locale.US, "%.1f", report.effectiveWeightKg)} kg",
        badgeText = if (report.isEffectiveWeightAdjusted) "Adjusted" else "1:1 Direct",
        subLabel = if (report.isEffectiveWeightAdjusted) "Lean-Tissue Mass" else "Actual ${String.format(Locale.US, "%.1f", report.actualWeightKg)} kg",
        accentColor = StepsStart,
        secondaryColor = StepsMid,
        progressFraction = massRatioProgress,
        onClick = { onInspectFormula(FormulaInspectionTopic.EFFECTIVE_WEIGHT) },
        metrics = metrics,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Row 2: Resting BMR & Daily Burn (TDEE)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PipelineMetricTile(
        stageIndex = "03",
        label = "RESTING BMR",
        value = "${numberFormat.format(report.bmrKcalRounded)} kcal",
        badgeText = "${(bmrShareOfTdee * 100).roundToInt()}% of Burn",
        subLabel = report.bmrDescriptor,
        accentColor = CarbsStart,
        secondaryColor = CarbsMid,
        progressFraction = bmrShareOfTdee,
        onClick = { onInspectFormula(FormulaInspectionTopic.BMR) },
        metrics = metrics,
        modifier = Modifier.weight(1f),
      )
      PipelineMetricTile(
        stageIndex = "04",
        label = "DAILY BURN (TDEE)",
        value = "${numberFormat.format(report.tdeeKcalRounded)} kcal",
        badgeText = "×${String.format(Locale.US, "%.2f", report.totalActivityFactor)}",
        subLabel = "Maintenance Burn",
        accentColor = CaloriesStart,
        secondaryColor = CaloriesMid,
        progressFraction = tdeeProgress,
        onClick = { onInspectFormula(FormulaInspectionTopic.TDEE) },
        metrics = metrics,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Optional Body Fat % Calibration (OFF by default as requested)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(11.dp))
        .background(ObsidianBlack)
        .border(
          width = 1.dp,
          color = if (bodyFatOverrideEnabled) ProteinStart.copy(alpha = 0.55f) else Zinc850,
          shape = RoundedCornerShape(11.dp),
        )
        .padding(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .width(3.5.dp)
              .height(26.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(
                if (bodyFatOverrideEnabled) {
                  Brush.verticalGradient(listOf(ProteinStart, StepsStart))
                } else {
                  Brush.verticalGradient(listOf(Zinc600, Zinc800))
                }
              ),
          )
          Column {
            Text(
              text = "Known Body Fat % Calibration",
              color = PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(12.2f, 10.8f, 13.6f),
            )
            Text(
              text = if (bodyFatOverrideEnabled) {
                "Active: Lean Mass BMR (${String.format(Locale.US, "%.1f", bodyFatPercent)}% body fat)"
              } else {
                "Optional — enable if you know your exact body fat %"
              },
              color = if (bodyFatOverrideEnabled) ProteinStart else Zinc400,
              fontFamily = InterFontFamily,
              fontSize = metrics.spScaled(10.5f, 9.2f, 12f),
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (bodyFatOverrideEnabled) PureWhite else Zinc850)
            .border(1.dp, if (bodyFatOverrideEnabled) PureWhite else Zinc700, RoundedCornerShape(7.dp))
            .clickable { onToggleBodyFatOverride(!bodyFatOverrideEnabled) }
            .padding(horizontal = 12.dp, vertical = 5.5.dp)
            .testTag("toggle_body_fat_button"),
        ) {
          Text(
            text = if (bodyFatOverrideEnabled) "ON" else "OFF",
            color = if (bodyFatOverrideEnabled) ObsidianBlack else Zinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
          )
        }
      }

      AnimatedVisibility(
        visible = bodyFatOverrideEnabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "CALIBRATED BODY FAT",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = metrics.spScaled(9f, 8f, 10f),
            )
            Text(
              text = "${String.format(Locale.US, "%.1f", bodyFatPercent)}%",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(16f, 14f, 18f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HoldRepeatStepperButton(
              symbol = "−",
              accentColor = ProteinStart,
              onStep = { mult -> onBodyFatPercentDelta(-0.5f * mult) },
              haptics = haptics,
              buttonSize = 36.dp,
            )
            HoldRepeatStepperButton(
              symbol = "+",
              accentColor = ProteinStart,
              onStep = { mult -> onBodyFatPercentDelta(0.5f * mult) },
              haptics = haptics,
              buttonSize = 36.dp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PipelineStageChip(
  index: String,
  label: String,
  accentColor: Color,
  metrics: SummaryResponsiveMetrics,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(5.dp)
        .clip(CircleShape)
        .background(accentColor),
    )
    Text(
      text = index,
      color = accentColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(9f, 8f, 10f),
    )
    Text(
      text = label,
      color = Zinc300,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(9f, 8f, 10f),
    )
  }
}

@Composable
private fun PipelineMetricTile(
  stageIndex: String,
  label: String,
  value: String,
  badgeText: String,
  subLabel: String,
  accentColor: Color,
  secondaryColor: Color,
  progressFraction: Float,
  onClick: () -> Unit,
  metrics: SummaryResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(11.dp))
      .background(ObsidianBlack)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(accentColor.copy(alpha = 0.38f), Zinc850),
        ),
        shape = RoundedCornerShape(11.dp),
      )
      .shortHoldOrTap(
        holdDurationMs = 420L,
        onTap = onClick,
        onShortHold = onClick,
      )
      .padding(11.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .width(3.dp)
            .height(11.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(accentColor),
        )
        Text(
          text = label,
          color = Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(9.4f, 8.4f, 10.6f),
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = stageIndex,
        color = accentColor.copy(alpha = 0.85f),
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(9f, 8f, 10.2f),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = value,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = metrics.spScaled(17.5f, 15f, 20f),
      maxLines = 1,
    )

    Spacer(modifier = Modifier.height(4.dp))

    // Clean colored text without any surrounding box (matching "Gut & Satiety" / "Insulin Guard")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = subLabel,
        color = Zinc400,
        fontFamily = InterFontFamily,
        fontSize = metrics.spScaled(9.6f, 8.6f, 10.8f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = badgeText,
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
        maxLines = 1,
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    // Mini Telemetry Spectrum Bar
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp),
    ) {
      val r = CornerRadius(size.height / 2f, size.height / 2f)
      drawRoundRect(
        color = Zinc900,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = r,
      )
      val fillW = (size.width * progressFraction.coerceIn(0.08f, 1f)).coerceAtLeast(size.height)
      drawRoundRect(
        brush = Brush.horizontalGradient(
          colors = listOf(secondaryColor, accentColor, PureWhite),
          startX = 0f,
          endX = fillW,
        ),
        topLeft = Offset.Zero,
        size = Size(fillW, size.height),
        cornerRadius = r,
      )
    }
  }
}

// ─── Upgraded Complete Protocol Dossier (Obsidian & Monochrome Matrix Architecture) ─

@Composable
private fun CompleteOnboardingDossierSection(
  subjectName: String,
  selectedSex: String,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  ageYears: Int,
  heightCm: Float,
  heightUnit: HeightUnit,
  weightKg: Float,
  weightUnit: WeightUnit,
  bmi: Float,
  bmiCategory: String,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  trainingDaysPerWeek: Int,
  restDaysPerWeek: Int,
  onOpenWorkoutDaysDialog: () -> Unit,
  occupationLoad: OccupationLoadOption,
  objective: BlueprintObjectiveOption,
  effectiveObjective: BlueprintObjectiveOption,
  objectiveWasOverridden: Boolean,
  trainingModality: TrainingModalityOption,
  workoutDuration: WorkoutDurationOption,
  experienceLevel: ExperienceLevelOption,
  targetPace: TargetPaceOption,
  dayOne: DayOfWeekOption,
  mealFrequency: MealFrequencyOption,
  caffeineAlcoholYes: Boolean,
  sodiumFrequency: SodiumFrequencyOption,
  streakThreshold: StreakThresholdOption,
  onQuickEditStep01: (String?) -> Unit,
  onQuickEditStep02: (String?) -> Unit,
  onQuickEditStep03: (String?) -> Unit,
  metrics: SummaryResponsiveMetrics,
) {
  val monthLabel = MonthShortLabels.getOrElse(birthMonthZeroBased.coerceIn(0, 11)) { "Jan" }
  val heightFormatted = remember(heightCm, heightUnit) {
    when (heightUnit) {
      HeightUnit.CM -> "${String.format(Locale.US, "%.1f", heightCm)} cm"
      HeightUnit.INCH -> "${String.format(Locale.US, "%.1f", heightCm / 2.54f)} in"
      HeightUnit.FEET -> "${String.format(Locale.US, "%.2f", heightCm / 30.48f)} ft"
    }
  }
  val heightSubFormatted = remember(heightCm, heightUnit) {
    when (heightUnit) {
      HeightUnit.CM -> "${String.format(Locale.US, "%.2f", heightCm / 100f)}M STATURE"
      HeightUnit.INCH, HeightUnit.FEET -> "${String.format(Locale.US, "%.0f", heightCm)} CM METRIC"
    }
  }
  val weightFormatted = remember(weightKg, weightUnit) {
    when (weightUnit) {
      WeightUnit.KG -> "${String.format(Locale.US, "%.1f", weightKg)} kg"
      WeightUnit.LBS -> "${String.format(Locale.US, "%.1f", weightKg * 2.20462f)} lbs"
    }
  }
  val weightSubFormatted = remember(weightKg, weightUnit) {
    when (weightUnit) {
      WeightUnit.KG -> "${String.format(Locale.US, "%.1f", weightKg * 2.20462f)} LBS EQUIV"
      WeightUnit.LBS -> "${String.format(Locale.US, "%.1f", weightKg)} KG METRIC"
    }
  }

  val activityTitle = when (activityLevel) {
    ActivityLevelOption.SEDENTARY -> stringResource(R.string.blueprint_activity_sedentary_title)
    ActivityLevelOption.LIGHT -> stringResource(R.string.blueprint_activity_light_title)
    ActivityLevelOption.MODERATE -> stringResource(R.string.blueprint_activity_moderate_title)
    ActivityLevelOption.VERY_ACTIVE -> stringResource(R.string.blueprint_activity_very_active_title)
  }

  // Schedule display: if Sedentary, do NOT mention workout/rest split!
  val workoutScheduleSub = remember(activityLevel, selectedWorkoutDays, trainingDaysPerWeek, restDaysPerWeek) {
    if (activityLevel == ActivityLevelOption.SEDENTARY) {
      "DAILY BASELINE MOVEMENT • DESK / LOW"
    } else {
      val orderedDays = DayOfWeekOption.entries.filter { it in selectedWorkoutDays }.joinToString(",") { it.shortCode }
      "${trainingDaysPerWeek}W / ${restDaysPerWeek}R • $orderedDays"
    }
  }

  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

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
        val glassSheenPath = Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width * 0.76f, 0f)
          lineTo(size.width * 0.24f, size.height * 0.26f)
          lineTo(0f, size.height * 0.26f)
          close()
        }
        onDrawBehind {
          drawRoundRect(
            brush = bgBrush,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
          )
          drawPath(
            path = glassSheenPath,
            brush = Brush.linearGradient(
              colors = listOf(
                PureWhite.copy(alpha = 0.055f),
                PureWhite.copy(alpha = 0.015f),
                Color.Transparent,
              ),
              start = Offset.Zero,
              end = Offset(size.width * 0.55f, size.height * 0.28f),
            ),
          )
          drawRoundRect(
            brush = Brush.verticalGradient(
              0.0f to PureWhite.copy(alpha = 0.26f),
              0.48f to Zinc600.copy(alpha = 0.55f),
              1.0f to Zinc850,
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.1.dp.toPx()),
          )
        }
      }
      .padding(metrics.cardPadding)
      .testTag("onboarding_dossier_section"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Header Row: Luminous White Glow Dot + COMPLETE PROTOCOL DOSSIER + PureWhite Synced Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            DossierLuminousWhiteDot(
              size = (7f * metrics.scale).coerceIn(6f, 8.5f).dp,
            )
            Text(
              text = "COMPLETE PROTOCOL DOSSIER",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(11f, 9.5f, 12.5f),
              letterSpacing = 1.1.sp,
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Tap any tile to jump directly to that step, edit, and return here",
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(10.6f, 9.4f, 11.8f),
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PureWhite)
            .border(1.dp, PureWhite, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.5.dp, vertical = 3.5.dp),
        ) {
          Text(
            text = "3 STEPS SYNCED",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
            letterSpacing = 0.5.sp,
          )
        }
      }

      HorizontalDivider(color = Zinc850, thickness = 0.8.dp)

      // ── STEP 01 MATRIX CHAMBER (Profile & Chronometry) ──────────────────────
      DossierStepGroupCard(
        stepNumber = "01",
        stepTitle = "PROFILE & CHRONOMETRY",
        stepSubtitle = "Subject identity, biological sex & age",
        onEditStep = { onQuickEditStep01(null) },
        editTestTag = "quick_edit_step_01_btn",
        metrics = metrics,
      ) {
        DossierMatrixTile(
          label = "SUBJECT FULL NAME",
          value = subjectName.ifBlank { "Unassigned Subject" },
          subValue = "STEP 01 • BIOMETRIC IDENTITY RECORD",
          dotColor = PureWhite,
          onClick = { onQuickEditStep01("name") },
          metrics = metrics,
          modifier = Modifier.fillMaxWidth(),
          testTag = "quick_edit_name_row",
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DossierMatrixTile(
            label = "BIOLOGICAL SEX",
            value = selectedSex.lowercase().replaceFirstChar { it.uppercase() },
            subValue = "HORMONE & BMR BASE",
            dotColor = ProteinStart,
            onClick = { onQuickEditStep01("sex") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_sex_row",
          )
          DossierMatrixTile(
            label = "CHRONOMETRY & AGE",
            value = "$ageYears Years",
            subValue = "DOB: ${birthDay.toString().padStart(2, '0')} ${monthLabel.uppercase()} $birthYear",
            dotColor = HydrationStart,
            onClick = { onQuickEditStep01("dob") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_dob_row",
          )
        }
      }

      // ── STEP 02 MATRIX CHAMBER (Personal Anthropometrics) ───────────────────
      DossierStepGroupCard(
        stepNumber = "02",
        stepTitle = "PERSONAL ANTHROPOMETRICS",
        stepSubtitle = "Stature, body mass & somatic index",
        onEditStep = { onQuickEditStep02(null) },
        editTestTag = "quick_edit_step_02_btn",
        metrics = metrics,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DossierMatrixTile(
            label = "STATURE (HEIGHT)",
            value = heightFormatted,
            subValue = heightSubFormatted,
            dotColor = ProteinStart,
            onClick = { onQuickEditStep02("height") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_height_row",
          )
          DossierMatrixTile(
            label = "BODY MASS (WEIGHT)",
            value = weightFormatted,
            subValue = weightSubFormatted,
            dotColor = StepsStart,
            onClick = { onQuickEditStep02("weight") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_weight_row",
          )
        }
        DossierMatrixTile(
          label = "SOMATIC BMI CLASSIFICATION",
          value = "${String.format(Locale.US, "%.1f", bmi)} kg/m² • $bmiCategory",
          subValue = "ANTHROPOMETRIC RATIO VERIFIED",
          dotColor = PureWhite,
          onClick = { onQuickEditStep02(null) },
          metrics = metrics,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // ── STEP 03 MATRIX CHAMBER (Metabolic Blueprint) ────────────────────────
      DossierStepGroupCard(
        stepNumber = "03",
        stepTitle = "METABOLIC BLUEPRINT",
        stepSubtitle = "Activity schedule, objective, pace & nutrition",
        onEditStep = { onQuickEditStep03(null) },
        editTestTag = "quick_edit_step_03_btn",
        metrics = metrics,
      ) {
        // Row 1: Activity Level & Occupation Load
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DossierMatrixTile(
            label = "ACTIVITY LEVEL",
            value = activityTitle,
            subValue = workoutScheduleSub,
            dotColor = activityLevel.accentColor,
            onClick = { onQuickEditStep03("activity") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_activity_row",
          )
          DossierMatrixTile(
            label = "OCCUPATION LOAD",
            value = occupationLoad.title,
            subValue = "WORKPAL • ${occupationLoad.badgeCode}",
            dotColor = activityLevel.accentColor,
            onClick = { onQuickEditStep03("activity") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }

        // Interactive 7-Day Workout Schedule Matrix Strip (shown when non-sedentary)
        if (activityLevel != ActivityLevelOption.SEDENTARY) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(9.dp))
              .background(Zinc950)
              .border(0.8.dp, Zinc800, RoundedCornerShape(9.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              DayOfWeekOption.entries.forEach { dayOption ->
                val isWorkoutDay = dayOption in selectedWorkoutDays
                Box(
                  modifier = Modifier
                    .size(23.dp)
                    .clip(RoundedCornerShape(5.5.dp))
                    .background(if (isWorkoutDay) PureWhite else Zinc900)
                    .border(
                      width = 0.8.dp,
                      color = if (isWorkoutDay) PureWhite else Zinc800,
                      shape = RoundedCornerShape(5.5.dp),
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = dayOption.shortCode.first().toString(),
                    color = if (isWorkoutDay) ObsidianBlack else Zinc500,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PureWhite)
                .clickable(onClick = onOpenWorkoutDaysDialog)
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("summary_pick_workout_days_btn"),
            ) {
              Text(
                text = "PICK WORKOUT DAYS",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
                letterSpacing = 0.4.sp,
              )
            }
          }
        }

        // Row 2: Primary Objective & Training Architecture
        val objectiveTitle = when (effectiveObjective) {
          BlueprintObjectiveOption.LOSE_FAT -> stringResource(R.string.blueprint_objective_lose_fat_title)
          BlueprintObjectiveOption.MAINTAIN -> stringResource(R.string.blueprint_objective_maintain_title)
          BlueprintObjectiveOption.BUILD_MUSCLE -> stringResource(R.string.blueprint_objective_build_muscle_title)
        }
        val objectiveSub = if (objectiveWasOverridden) {
          "SAFETY GUARD • ${effectiveObjective.vectorBadge}"
        } else {
          effectiveObjective.vectorBadge
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DossierMatrixTile(
            label = "PRIMARY OBJECTIVE",
            value = objectiveTitle,
            subValue = objectiveSub,
            dotColor = effectiveObjective.primaryColor,
            onClick = { onQuickEditStep03("objective") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
            testTag = "quick_edit_objective_row",
          )
          DossierMatrixTile(
            label = "TRAINING",
            value = "${trainingModality.shortLabel} • ${workoutDuration.displayHours}",
            subValue = "${experienceLevel.name} • ${experienceLevel.trainingAgeTag}",
            dotColor = trainingModality.accentColor,
            onClick = { onQuickEditStep03("training") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }

        // Row 3: Pace & Day 01 | Nutrition & Streak
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DossierMatrixTile(
            label = "PACE & CYCLE",
            value = "${targetPace.title} • D1 ${dayOne.shortCode}",
            subValue = "${targetPace.weeklyRateKg} • DAY 01 = ${dayOne.fullName.uppercase()}",
            dotColor = targetPace.accentColor,
            onClick = { onQuickEditStep03("pace") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
          DossierMatrixTile(
            label = "NUTRITION",
            value = "${mealFrequency.count} Meals • ${sodiumFrequency.title}",
            subValue = "CAFF/ALC: ${if (caffeineAlcoholYes) "YES" else "OFF"} • ${mealFrequency.cadenceTag}",
            dotColor = sodiumFrequency.accentColor,
            onClick = { onQuickEditStep03("nutrition") },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }

        // Row 4: Daily Streak Rule (Full Width)
        DossierMatrixTile(
          label = "DAILY STREAK COMPLETION RULE",
          value = "${streakThreshold.percentage}% Daily Target (${streakThreshold.tierTitle})",
          subValue = "${streakThreshold.shortTag} DISCIPLINE • +1 STREAK",
          dotColor = streakThreshold.accentColor,
          onClick = { onQuickEditStep03("streak") },
          metrics = metrics,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      HorizontalDivider(color = Zinc850, thickness = 0.8.dp)

      // Bottom Subject & Verification Footer (matching BlueprintSummaryMatrixCard)
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
          text = "AGE: $ageYears YRS • ON-DEVICE",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          letterSpacing = 0.6.sp,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun DossierLuminousWhiteDot(
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
          drawCircle(color = PureWhite, radius = r, center = center)
        }
      },
  )
}

@Composable
private fun DossierStepGroupCard(
  stepNumber: String,
  stepTitle: String,
  stepSubtitle: String,
  onEditStep: () -> Unit,
  editTestTag: String,
  metrics: SummaryResponsiveMetrics,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianBlack)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.18f), Zinc850),
        ),
        shape = RoundedCornerShape(12.dp),
      )
      .padding(11.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.weight(1f),
      ) {
        // High-contrast PureWhite Step Index Block (matching core monochrome design)
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(PureWhite)
            .border(1.dp, PureWhite, RoundedCornerShape(7.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stepNumber,
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(10.8f, 9.5f, 12f),
          )
        }
        Column {
          Text(
            text = "STEP $stepNumber • $stepTitle",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(10.2f, 9f, 11.5f),
            letterSpacing = 0.7.sp,
          )
          Text(
            text = stepSubtitle,
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(9.8f, 8.8f, 11f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(Zinc900)
          .border(1.dp, Zinc700, RoundedCornerShape(7.dp))
          .clickable(role = Role.Button, onClick = onEditStep)
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag(editTestTag),
      ) {
        Text(
          text = "EDIT STEP ↗",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
          letterSpacing = 0.4.sp,
        )
      }
    }

    HorizontalDivider(color = Zinc900, thickness = 0.8.dp)

    content()
  }
}

@Composable
private fun DossierMatrixTile(
  label: String,
  value: String,
  subValue: String,
  dotColor: Color,
  onClick: () -> Unit,
  metrics: SummaryResponsiveMetrics,
  modifier: Modifier = Modifier,
  testTag: String = "",
) {
  val animatedDot by animateColorAsState(
    targetValue = dotColor,
    animationSpec = tween(durationMillis = 260),
    label = "dossier_tile_dot_$label",
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
        horizontal = (9f * metrics.scale).coerceIn(7.5f, 11.5f).dp,
        vertical = (8.5f * metrics.scale).coerceIn(7f, 10.5f).dp,
      )
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
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
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Canvas(modifier = Modifier.size(5.dp)) {
          drawCircle(color = animatedDot)
        }
        Text(
          text = "↗",
          color = Zinc500,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.5f, 7.5f, 9.8f),
        )
      }
    }
    Spacer(modifier = Modifier.height(3.5.dp))
    Text(
      text = value,
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(12.4f, 11f, 14f),
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

// ─── Primary Lock In Daily Protocol CTA with Metallic Shimmer & Aura ────────

@Composable
private fun SummaryLockInFooterSection(
  protocolLockedIn: Boolean,
  hasActiveHealthAlerts: Boolean,
  alertCount: Int,
  onLockInClick: () -> Unit,
  metrics: SummaryResponsiveMetrics,
) {
  val coroutineScope = rememberCoroutineScope()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val borderFlashPhase = remember { Animatable(0f) }
  val buttonCorner = (10f * metrics.scale).coerceIn(8.5f, 13f).dp
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
  val borderFlashProvider = remember { { borderFlashPhase.value } }

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
          onClick = {
            coroutineScope.launch {
              borderFlashPhase.snapTo(0f)
              borderFlashPhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = SummaryEaseOut),
              )
            }
            onLockInClick()
          },
        )
        .testTag("lock_in_protocol_button"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = when {
            hasActiveHealthAlerts -> "Review $alertCount Alert${if (alertCount > 1) "s" else ""} & Lock In Protocol"
            protocolLockedIn -> stringResource(R.string.summary_cta_protocol_locked)
            else -> stringResource(R.string.summary_cta_lock_protocol)
          },
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
          val iconPath = Path().apply {
            if (protocolLockedIn && !hasActiveHealthAlerts) {
              moveTo(5f * sx, 12.5f * sy)
              lineTo(10f * sx, 17.5f * sy)
              lineTo(19.5f * sx, 7f * sy)
            } else {
              moveTo(5f * sx, 12f * sy)
              lineTo(19f * sx, 12f * sy)
              moveTo(13f * sx, 6f * sy)
              lineTo(19f * sx, 12f * sy)
              moveTo(13f * sx, 18f * sy)
              lineTo(19f * sx, 12f * sy)
            }
          }
          drawPath(
            path = iconPath,
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
    )
  }
}
