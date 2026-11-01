package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.ChronoBirthdayStart
import com.example.ui.theme.ChronoDayStart
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val SettingsZinc200 = Color(0xFFE4E4E7)
private val SettingsZinc300 = Color(0xFFD4D4D8)
private val SettingsZinc400 = Color(0xFFA1A1AA)
private val SettingsZinc500 = Color(0xFF71717A)
private val SettingsZinc600 = Color(0xFF52525B)
private val SettingsZinc700 = Color(0xFF3F3F46)
private val SettingsZinc800 = Color(0xFF27272A)
private val SettingsZinc850 = Color(0xFF1C1C20)
private val SettingsZinc900 = Color(0xFF131317)
private val SettingsZinc950 = Color(0xFF0A0A0D)
private val SettingsCardTop = Color(0xFF15151A)
private val SettingsCardBottom = Color(0xFF0A0A0E)
private val DangerCrimson = Color(0xFFEF4444)
private val DangerCrimsonDeep = Color(0xFFB91C1C)

private val SettingsEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val SettingsMonthShortLabels = listOf(
  "Jan", "Feb", "Mar", "Apr", "May", "Jun",
  "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun safeSettingsBlur(radius: Float): Float = radius.coerceAtLeast(0.5f)

@Immutable
private data class SettingsResponsiveMetrics(
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
) {
  fun spScaled(base: Float, minVal: Float, maxVal: Float): TextUnit =
    ((base * scale).coerceIn(minVal, maxVal) * fontCompensation).sp
}

private fun computeSettingsMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): SettingsResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val isExpanded = w >= 600f
  val widthFactor = (w / 392f).coerceIn(0.82f, 1.18f)
  val heightFactor = (h / 830f).coerceIn(0.76f, 1.12f)
  val scale = min(widthFactor, heightFactor * 1.05f).coerceIn(0.80f, 1.14f)
  val safeFontScale = fontScale.coerceIn(0.5f, 3.0f)
  val fontCompensation = when {
    safeFontScale > 1.0f -> (1.0f + (safeFontScale - 1.0f) * 0.25f) / safeFontScale
    safeFontScale < 1.0f -> (1.0f - (1.0f - safeFontScale) * 0.30f) / safeFontScale
    else -> 1f
  }
  val sp: (Float, Float, Float) -> TextUnit = { base, minV, maxV ->
    ((base * scale).coerceIn(minV, maxV) * fontCompensation).sp
  }

  val contentMaxWidth = when {
    w >= 1100f -> (w * 0.86f).coerceIn(820f, 1060f).dp
    w >= 840f -> (w * 0.84f).coerceIn(680f, 860f).dp
    isExpanded -> (w * 0.82f).coerceIn(520f, 680f).dp
    else -> w.dp
  }

  return SettingsResponsiveMetrics(
    scale = scale,
    fontCompensation = fontCompensation,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (20f * widthFactor).coerceIn(14f, 28f).dp,
    topContentPadding = (8f * heightFactor).coerceIn(5f, 14f).dp,
    bottomContentPadding = (106f * heightFactor).coerceIn(88f, 126f).dp,
    headerHeight = (48f * scale).coerceIn(44f, 56f).dp,
    titleFontSize = sp(25f, 21f, 29f),
    subtitleFontSize = sp(12.4f, 11f, 14f),
    subtitleLineHeight = sp(18f, 15.5f, 20.5f),
    sectionGap = (15f * heightFactor).coerceIn(12f, 20f).dp,
    cardCornerRadius = (15f * scale).coerceIn(13f, 18f).dp,
    cardPadding = (15f * scale).coerceIn(12f, 18.5f).dp,
  )
}

/**
 * Dedicated Settings & Dossier Command Page:
 * - Preserves scroll position and state across step edits (`rememberSaveable(saver = ScrollState.Saver)`)
 * - Allows editing ALL user info across Step 01 (Profile), Step 02 (Personal Details), Step 03 (Blueprint),
 *   and Step 04 (Daily Targets & Body Fat %) with direct step teleportation and automatic return to Settings
 * - Includes Data Backup & Import Recovery Vault + Factory Reset protection with backup warning.
 */
@Composable
fun BioSyncSettingsScreen(
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
  bodyFatPercent: Float,
  activeCaloriesKcal: Int,
  activeProteinGrams: Int,
  activeCarbsGrams: Int,
  activeFatsGrams: Int,
  activeHydrationMl: Int,
  activeStepsCount: Int,
  dayOneAnchorStartEpochMillis: Long = 0L,
  dayOneInitialCountdownCompleted: Boolean = false,
  showStatsOption: Boolean = true,
  onNavigateBackToDashboard: () -> Unit,
  onNavigateToStats: () -> Unit = {},
  onEditStep01FromSettings: (focusSection: String?) -> Unit,
  onEditStep02FromSettings: (focusSection: String?) -> Unit,
  onEditStep03FromSettings: (focusSection: String?) -> Unit,
  onEditStep04FromSettings: () -> Unit,
  onBackupRestored: (BioSyncFullBackupBundle, Boolean) -> Unit = { _, _ -> },
  onFactoryResetConfirmed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateBackToDashboard)

  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
  val headerBarAnim = remember { Animatable(0f) }

  // Preserve scroll position so returning from Step 01/02/03/04 restores the exact current state in Settings
  val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

  var showWorkoutDaysPicker by rememberSaveable { mutableStateOf(false) }
  var showFactoryResetModal by rememberSaveable { mutableStateOf(false) }
  var showImportBackupDialog by rememberSaveable { mutableStateOf(false) }
  var vaultMeta by remember { mutableStateOf(BioSyncBackupManager.getRecoveryVaultMeta(context)) }
  var backupStatusBanner by remember { mutableStateOf<String?>(null) }
  var pendingExportJson by remember { mutableStateOf("") }
  var liveNowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  val exportDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null && pendingExportJson.isNotBlank()) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(pendingExportJson.toByteArray(Charsets.UTF_8))
        }
        backupStatusBanner = "Backup file exported & saved to Recovery Vault."
      }.onFailure {
        backupStatusBanner = "Saved to Recovery Vault & Clipboard."
      }
    }
  }

  LaunchedEffect(Unit) {
    headerBarAnim.snapTo(0f)
    headerBarAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 680, easing = SettingsEaseOut),
    )
  }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000L)
      liveNowEpochMs = System.currentTimeMillis()
    }
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

  val subjectIdBadge = remember(subjectName) {
    BioSyncLocalStore.computeSubjectIdBadge(subjectName)
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
      val fontScale = LocalDensity.current.fontScale
      val metrics = remember(maxWidth, maxHeight, fontScale) {
        computeSettingsMetrics(maxWidth, maxHeight, fontScale)
      }

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
          .testTag("settings_screen_root"),
      ) {
        // 1. Top Command Header Bar (Back to Dashboard + BioSync Settings + Automatic Subject ID Badge)
        SettingsTopHeaderBar(
          subjectIdBadge = subjectIdBadge,
          phaseProvider = { headerBarAnim.value },
          onBackClick = {
            haptics.selection()
            onNavigateBackToDashboard()
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.topContentPadding))

        // 2. Eyebrow, Title & Description
        Text(
          text = "SYSTEM & BIOMETRIC CONTROL",
          color = SettingsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(11f, 9.5f, 12.6f),
          letterSpacing = 1.6.sp,
        )
        Spacer(modifier = Modifier.height((4f * metrics.scale).dp))
        Text(
          text = "Settings",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.titleFontSize,
          letterSpacing = (-0.6).sp,
          modifier = Modifier.testTag("settings_title_text"),
        )
        Spacer(modifier = Modifier.height((5f * metrics.scale).coerceIn(3.5f, 7.5f).dp))
        Text(
          text = "Tap any section below to jump directly to that calibration step, edit your information, and return straight back to your current place in Settings.",
          color = SettingsZinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = metrics.subtitleFontSize,
          lineHeight = metrics.subtitleLineHeight,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 3. Subject Identity & Live Chronometric Summary Card
        SettingsIdentityHeroCard(
          subjectName = subjectName,
          subjectIdBadge = subjectIdBadge,
          selectedSex = selectedSex,
          birthDay = birthDay,
          birthMonthZeroBased = birthMonthZeroBased,
          birthYear = birthYear,
          ageYears = ageYears,
          nowEpochMs = liveNowEpochMs,
          onQuickEditIdentity = {
            haptics.selection()
            onEditStep01FromSettings("name")
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. Complete All-Info Editor Matrix (Steps 01, 02, 03 & 04)
        SettingsAllInfoEditorSection(
          subjectName = subjectName,
          subjectIdBadge = subjectIdBadge,
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
          bodyFatOverrideEnabled = bodyFatOverrideEnabled,
          bodyFatPercent = bodyFatPercent,
          activeCaloriesKcal = activeCaloriesKcal,
          activeProteinGrams = activeProteinGrams,
          activeCarbsGrams = activeCarbsGrams,
          activeFatsGrams = activeFatsGrams,
          activeHydrationMl = activeHydrationMl,
          activeStepsCount = activeStepsCount,
          numberFormat = numberFormat,
          onEditStep01 = { focus ->
            haptics.selection()
            onEditStep01FromSettings(focus)
          },
          onEditStep02 = { focus ->
            haptics.selection()
            onEditStep02FromSettings(focus)
          },
          onEditStep03 = { focus ->
            haptics.selection()
            onEditStep03FromSettings(focus)
          },
          onEditStep04 = {
            haptics.selection()
            onEditStep04FromSettings()
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 5. Data Backup & Import Recovery Vault Card
        SettingsBackupVaultCard(
          subjectName = subjectName,
          subjectIdBadge = subjectIdBadge,
          vaultMeta = vaultMeta,
          statusBanner = backupStatusBanner,
          onCreateBackup = {
            haptics.confirm()
            val json = BioSyncBackupManager.exportFullBackupJson(context)
            val meta = BioSyncBackupManager.saveSnapshotToRecoveryVault(context)
            vaultMeta = meta
            pendingExportJson = json
            runCatching {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              clipboard?.setPrimaryClip(ClipData.newPlainText("BioSync Backup JSON", json))
            }
            backupStatusBanner = "Full database backup saved to Recovery Vault & copied to Clipboard."
            runCatching {
              exportDocumentLauncher.launch("biosync_backup_${subjectIdBadge.lowercase(Locale.US)}.json")
            }
          },
          onOpenImportDialog = {
            haptics.selection()
            showImportBackupDialog = true
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 6. Factory Reset & Database Wipe Card
        SettingsFactoryResetCard(
          onRequestFactoryReset = {
            haptics.warning()
            showFactoryResetModal = true
          },
          metrics = metrics,
        )
      }

      val isWaitingForInitialDayOneBottomBar = !dayOneInitialCountdownCompleted &&
        dayOneAnchorStartEpochMillis > 0L &&
        liveNowEpochMs < dayOneAnchorStartEpochMillis
      val effectiveShowStats = showStatsOption && !isWaitingForInitialDayOneBottomBar

      // Centered Floating Bottom Navigation Bar (Home • Stats • Settings; Stats locked during Day 01 Clock Timer)
      BioSyncFloatingBottomBar(
        activeDestination = BioSyncFloatingNavDestination.SETTINGS,
        showStatsOption = effectiveShowStats,
        onSelectHome = onNavigateBackToDashboard,
        onSelectStats = {
          if (effectiveShowStats) {
            onNavigateToStats()
          }
        },
        onSelectSettings = {},
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
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

  if (showImportBackupDialog) {
    BioSyncBackupImportDialog(
      onRestoreComplete = { restoredBundle, jumpDirectlyToDashboard ->
        haptics.confirm()
        vaultMeta = BioSyncBackupManager.getRecoveryVaultMeta(context)
        showImportBackupDialog = false
        onBackupRestored(restoredBundle, jumpDirectlyToDashboard)
      },
      onDismiss = {
        haptics.selection()
        showImportBackupDialog = false
      },
    )
  }

  if (showFactoryResetModal) {
    FactoryResetConfirmationDialog(
      subjectName = subjectName,
      subjectIdBadge = subjectIdBadge,
      onKeepBackupAndReset = {
        haptics.confirm()
        BioSyncBackupManager.saveSnapshotToRecoveryVault(context)
        showFactoryResetModal = false
        onFactoryResetConfirmed()
      },
      onResetWithoutBackup = {
        haptics.warning()
        BioSyncBackupManager.clearRecoveryVault(context)
        showFactoryResetModal = false
        onFactoryResetConfirmed()
      },
      onDismiss = {
        haptics.selection()
        showFactoryResetModal = false
      },
    )
  }
}

@Composable
private fun SettingsTopHeaderBar(
  subjectIdBadge: String,
  phaseProvider: () -> Float,
  onBackClick: () -> Unit,
  metrics: SettingsResponsiveMetrics,
) {
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
        .testTag("settings_back_button"),
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
          color = SettingsZinc300,
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
        text = "SETTINGS • PROTOCOL CONTROL",
        color = SettingsZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = metrics.spScaled(9.8f, 8.8f, 11.4f),
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height((4.5f * metrics.scale).coerceIn(3.5f, 6.5f).dp))

      Spacer(
        modifier = Modifier
          .width((108f * metrics.scale).coerceIn(92f, 136f).dp)
          .height((4f * metrics.scale).coerceIn(3.5f, 5f).dp)
          .graphicsLayer()
          .drawWithCache {
            val barH = size.height.coerceAtLeast(1f)
            val radius = CornerRadius(barH / 2f, barH / 2f)
            val shinyCoreBrush = Brush.horizontalGradient(
              colors = listOf(Color(0xFFC8CCD4), Color(0xFFF2F4F7), PureWhite),
            )
            onDrawBehind {
              val p = phaseProvider().coerceIn(0f, 1f)
              val fillW = (size.width * (0.35f + 0.65f * p)).coerceIn(barH, size.width)
              drawRoundRect(
                color = SettingsZinc850,
                size = Size(size.width, barH),
                cornerRadius = radius,
              )
              drawRoundRect(
                brush = shinyCoreBrush,
                size = Size(fillW, barH),
                cornerRadius = radius,
              )
            }
          },
      )
    }

    // Top-Right Automatic ID Badge Pill
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(9.dp))
        .background(Color(0xFF17171E))
        .border(1.dp, PureWhite.copy(alpha = 0.30f), RoundedCornerShape(9.dp))
        .padding(horizontal = 9.dp, vertical = 5.dp)
        .testTag("settings_header_id_badge"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(StepsStart),
        )
        Text(
          text = subjectIdBadge,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(11f, 9.8f, 12.5f),
          letterSpacing = 0.6.sp,
        )
      }
    }
  }
}

@Composable
private fun SettingsIdentityHeroCard(
  subjectName: String,
  subjectIdBadge: String,
  selectedSex: String,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  ageYears: Int,
  nowEpochMs: Long,
  onQuickEditIdentity: () -> Unit,
  metrics: SettingsResponsiveMetrics,
) {
  val monthLabel = SettingsMonthShortLabels.getOrElse(birthMonthZeroBased.coerceIn(0, 11)) { "Oct" }
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  // Live End-of-Day & Next Birthday calculation inside Settings Hero Card
  val endOfDayCal = Calendar.getInstance().apply {
    timeInMillis = nowEpochMs
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
  }
  val remDaySecs = ((endOfDayCal.timeInMillis - nowEpochMs + 1L) / 1000L).toInt().coerceAtLeast(0)
  val dayH = remDaySecs / 3600
  val dayM = (remDaySecs % 3600) / 60
  val dayS = remDaySecs % 60

  val safeMonth = birthMonthZeroBased.coerceIn(0, 11)
  val nextBdayCal = Calendar.getInstance().apply {
    timeInMillis = nowEpochMs
    set(Calendar.MONTH, safeMonth)
    val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
    set(Calendar.DAY_OF_MONTH, birthDay.coerceIn(1, maxD))
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  if (nextBdayCal.timeInMillis <= nowEpochMs) {
    nextBdayCal.add(Calendar.YEAR, 1)
  }
  val bdayDaysLeft = ((nextBdayCal.timeInMillis - nowEpochMs).coerceAtLeast(0L) / 86_400_000L).toInt()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .background(Brush.verticalGradient(colors = listOf(SettingsCardTop, SettingsCardBottom)))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.28f), SettingsZinc700.copy(alpha = 0.65f), SettingsZinc850),
        ),
        shape = cardShape,
      )
      .padding(metrics.cardPadding)
      .testTag("settings_identity_hero_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f),
      ) {
        // Sculpted 3D Monogram ID Emblem
        Box(
          modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(PureWhite, Color(0xFFDADDE3)),
              ),
            )
            .border(1.dp, PureWhite, RoundedCornerShape(13.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = subjectIdBadge,
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              letterSpacing = 0.8.sp,
            )
            Text(
              text = "ID",
              color = Color(0xFF3F3F46),
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 7.5.sp,
              letterSpacing = 0.8.sp,
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(5.5.dp)
                .clip(CircleShape)
                .background(StepsStart),
            )
            Text(
              text = "VERIFIED BIOMETRIC SUBJECT",
              color = StepsStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(9.2f, 8.2f, 10.4f),
              letterSpacing = 0.8.sp,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subjectName.ifBlank { "Elena Rostova" },
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(19f, 16.5f, 22f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(5.5.dp))
                .background(
                  brush = Brush.horizontalGradient(
                    colors = listOf(PureWhite, Color(0xFFE4E4E7)),
                  ),
                )
                .padding(horizontal = 6.5.dp, vertical = 2.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "$ageYears YRS OLD",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
                letterSpacing = 0.5.sp,
              )
            }
            Text(
              text = "${selectedSex.lowercase().replaceFirstChar { it.uppercase() }} • DOB ${birthDay.toString().padStart(2, '0')} ${monthLabel.uppercase()} $birthYear",
              color = SettingsZinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = metrics.spScaled(9.6f, 8.4f, 10.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(SettingsZinc900)
          .border(1.dp, SettingsZinc700, RoundedCornerShape(8.dp))
          .clickable(role = Role.Button, onClick = onQuickEditIdentity)
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("settings_quick_edit_identity_btn"),
      ) {
        Text(
          text = "EDIT ↗",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(9.5f, 8.5f, 10.8f),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = SettingsZinc850, thickness = 0.8.dp)
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(9.dp))
          .background(ObsidianBlack)
          .border(1.dp, SettingsZinc850, RoundedCornerShape(9.dp))
          .padding(horizontal = (10f * metrics.scale).coerceIn(8f, 12f).dp, vertical = 8.dp),
      ) {
        Text(
          text = "DAY REMAINING",
          color = ChronoDayStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = String.format(Locale.US, "%02dh %02dm %02ds", dayH, dayM, dayS),
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(13f, 11f, 15f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(9.dp))
          .background(ObsidianBlack)
          .border(1.dp, SettingsZinc850, RoundedCornerShape(9.dp))
          .padding(horizontal = (10f * metrics.scale).coerceIn(8f, 12f).dp, vertical = 8.dp),
      ) {
        Text(
          text = "NEXT BIRTHDAY",
          color = ChronoBirthdayStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "$bdayDaysLeft Days (${monthLabel.uppercase()} ${birthDay.toString().padStart(2, '0')})",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(13f, 11f, 15f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SettingsAllInfoEditorSection(
  subjectName: String,
  subjectIdBadge: String,
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
  bodyFatOverrideEnabled: Boolean,
  bodyFatPercent: Float,
  activeCaloriesKcal: Int,
  activeProteinGrams: Int,
  activeCarbsGrams: Int,
  activeFatsGrams: Int,
  activeHydrationMl: Int,
  activeStepsCount: Int,
  numberFormat: NumberFormat,
  onEditStep01: (String?) -> Unit,
  onEditStep02: (String?) -> Unit,
  onEditStep03: (String?) -> Unit,
  onEditStep04: () -> Unit,
  metrics: SettingsResponsiveMetrics,
) {
  val monthLabel = SettingsMonthShortLabels.getOrElse(birthMonthZeroBased.coerceIn(0, 11)) { "Oct" }
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

  val workoutScheduleSub = remember(activityLevel, selectedWorkoutDays, trainingDaysPerWeek, restDaysPerWeek) {
    if (activityLevel == ActivityLevelOption.SEDENTARY) {
      "DAILY BASELINE MOVEMENT • DESK / LOW"
    } else {
      val orderedDays = DayOfWeekOption.entries.filter { it in selectedWorkoutDays }.joinToString(",") { it.shortCode }
      "${trainingDaysPerWeek}W / ${restDaysPerWeek}R • $orderedDays"
    }
  }

  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .background(Brush.verticalGradient(colors = listOf(SettingsCardTop, SettingsCardBottom)))
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.24f), SettingsZinc700.copy(alpha = 0.55f), SettingsZinc850),
        ),
        shape = cardShape,
      )
      .padding(metrics.cardPadding)
      .testTag("settings_all_info_editor_section"),
    verticalArrangement = Arrangement.spacedBy(13.dp),
  ) {
    // Section Header
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
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(PureWhite),
          )
          Text(
            text = "COMPLETE CALIBRATION DOSSIER",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(11f, 9.5f, 12.5f),
            letterSpacing = 1.1.sp,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Select any field to jump to its step, edit, and return to this exact place",
          color = SettingsZinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10.6f, 9.4f, 11.8f),
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(PureWhite)
          .padding(horizontal = 8.5.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = "ALL 4 STEPS",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
          letterSpacing = 0.5.sp,
        )
      }
    }

    HorizontalDivider(color = SettingsZinc850, thickness = 0.8.dp)

    // ── STEP 01: PROFILE & CHRONOMETRY ────────────────────────────────────────
    SettingsStepGroupCard(
      stepNumber = "01",
      stepTitle = "PROFILE & CHRONOMETRY",
      stepSubtitle = "Full name, automatic ID badge, biological sex & date of birth",
      onEditStep = { onEditStep01(null) },
      editTestTag = "settings_edit_step_01_btn",
      metrics = metrics,
    ) {
      SettingsMatrixTile(
        label = "SUBJECT FULL NAME & AUTOMATIC ID",
        value = "${subjectName.ifBlank { "Elena Rostova" }}  •  ID: $subjectIdBadge",
        subValue = "STEP 01 • TAP TO EDIT FULL NAME",
        dotColor = PureWhite,
        onClick = { onEditStep01("name") },
        metrics = metrics,
        modifier = Modifier.fillMaxWidth(),
        testTag = "settings_edit_name_tile",
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SettingsMatrixTile(
          label = "BIOLOGICAL SEX",
          value = selectedSex.lowercase().replaceFirstChar { it.uppercase() },
          subValue = "HORMONE & BMR BASE",
          dotColor = ProteinStart,
          onClick = { onEditStep01("sex") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_sex_tile",
        )
        SettingsMatrixTile(
          label = "CHRONOMETRY & AGE",
          value = "$ageYears Years",
          subValue = "DOB: ${birthDay.toString().padStart(2, '0')} ${monthLabel.uppercase()} $birthYear",
          dotColor = HydrationStart,
          onClick = { onEditStep01("dob") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_dob_tile",
        )
      }
    }

    // ── STEP 02: PERSONAL ANTHROPOMETRICS ─────────────────────────────────────
    SettingsStepGroupCard(
      stepNumber = "02",
      stepTitle = "PERSONAL ANTHROPOMETRICS",
      stepSubtitle = "Height stature, body weight mass & somatic BMI index",
      onEditStep = { onEditStep02(null) },
      editTestTag = "settings_edit_step_02_btn",
      metrics = metrics,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SettingsMatrixTile(
          label = "STATURE (HEIGHT)",
          value = heightFormatted,
          subValue = heightSubFormatted,
          dotColor = ProteinStart,
          onClick = { onEditStep02("height") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_height_tile",
        )
        SettingsMatrixTile(
          label = "BODY MASS (WEIGHT)",
          value = weightFormatted,
          subValue = weightSubFormatted,
          dotColor = StepsStart,
          onClick = { onEditStep02("weight") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_weight_tile",
        )
      }
      SettingsMatrixTile(
        label = "SOMATIC BMI CLASSIFICATION",
        value = "${String.format(Locale.US, "%.1f", bmi)} kg/m² • $bmiCategory",
        subValue = "TAP TO RECALIBRATE STATURE OR MASS",
        dotColor = PureWhite,
        onClick = { onEditStep02(null) },
        metrics = metrics,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    // ── STEP 03: METABOLIC BLUEPRINT & LIFESTYLE ──────────────────────────────
    SettingsStepGroupCard(
      stepNumber = "03",
      stepTitle = "METABOLIC BLUEPRINT",
      stepSubtitle = "Activity schedule, occupation, objective, training, pace & streak rule",
      onEditStep = { onEditStep03(null) },
      editTestTag = "settings_edit_step_03_btn",
      metrics = metrics,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SettingsMatrixTile(
          label = "ACTIVITY LEVEL",
          value = activityTitle,
          subValue = workoutScheduleSub,
          dotColor = activityLevel.accentColor,
          onClick = { onEditStep03("activity") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_activity_tile",
        )
        SettingsMatrixTile(
          label = "OCCUPATION LOAD",
          value = occupationLoad.title,
          subValue = "WORKPAL • ${occupationLoad.badgeCode}",
          dotColor = activityLevel.accentColor,
          onClick = { onEditStep03("activity") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_occupation_tile",
        )
      }

      if (activityLevel != ActivityLevelOption.SEDENTARY) {
        val dayBoxSize = (21f * metrics.scale).coerceIn(18f, 24f).dp
        val dayBoxGap = (4f * metrics.scale).coerceIn(3f, 5.5f).dp
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(SettingsZinc950)
            .border(0.8.dp, SettingsZinc800, RoundedCornerShape(9.dp))
            .padding(horizontal = (10f * metrics.scale).coerceIn(8f, 12f).dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(dayBoxGap),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            DayOfWeekOption.entries.forEach { dayOption ->
              val isWorkoutDay = dayOption in selectedWorkoutDays
              Box(
                modifier = Modifier
                  .size(dayBoxSize)
                  .clip(RoundedCornerShape(5.dp))
                  .background(if (isWorkoutDay) PureWhite else SettingsZinc900)
                  .border(
                    width = 0.8.dp,
                    color = if (isWorkoutDay) PureWhite else SettingsZinc800,
                    shape = RoundedCornerShape(5.dp),
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = dayOption.shortCode.first().toString(),
                  color = if (isWorkoutDay) ObsidianBlack else SettingsZinc500,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = metrics.spScaled(9.2f, 8f, 10.5f),
                )
              }
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(PureWhite)
              .clickable(onClick = onOpenWorkoutDaysDialog)
              .padding(horizontal = (8.5f * metrics.scale).coerceIn(7f, 10f).dp, vertical = 5.dp)
              .testTag("settings_pick_workout_days_btn"),
          ) {
            Text(
              text = "PICK WORKOUT DAYS",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(8.4f, 7.2f, 9.6f),
              letterSpacing = 0.3.sp,
              maxLines = 1,
            )
          }
        }
      }

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
        SettingsMatrixTile(
          label = "PRIMARY OBJECTIVE",
          value = objectiveTitle,
          subValue = objectiveSub,
          dotColor = effectiveObjective.primaryColor,
          onClick = { onEditStep03("objective") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_objective_tile",
        )
        SettingsMatrixTile(
          label = "TRAINING",
          value = "${trainingModality.shortLabel} • ${workoutDuration.displayHours}",
          subValue = "${experienceLevel.name} • ${experienceLevel.trainingAgeTag}",
          dotColor = trainingModality.accentColor,
          onClick = { onEditStep03("training") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_training_tile",
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SettingsMatrixTile(
          label = "PACE & CYCLE",
          value = "${targetPace.title} • D1 ${dayOne.shortCode}",
          subValue = "${targetPace.weeklyRateKg} • DAY 01 = ${dayOne.fullName.uppercase()}",
          dotColor = targetPace.accentColor,
          onClick = { onEditStep03("pace") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_pace_tile",
        )
        SettingsMatrixTile(
          label = "NUTRITION",
          value = "${mealFrequency.count} Meals • ${sodiumFrequency.title}",
          subValue = "CAFF/ALC: ${if (caffeineAlcoholYes) "YES" else "OFF"} • ${mealFrequency.cadenceTag}",
          dotColor = sodiumFrequency.accentColor,
          onClick = { onEditStep03("nutrition") },
          metrics = metrics,
          modifier = Modifier.weight(1f),
          testTag = "settings_edit_nutrition_tile",
        )
      }

      SettingsMatrixTile(
        label = "DAILY STREAK COMPLETION RULE",
        value = "${streakThreshold.percentage}% Daily Target (${streakThreshold.tierTitle})",
        subValue = "${streakThreshold.shortTag} DISCIPLINE • +1 STREAK",
        dotColor = streakThreshold.accentColor,
        onClick = { onEditStep03("streak") },
        metrics = metrics,
        modifier = Modifier.fillMaxWidth(),
        testTag = "settings_edit_streak_tile",
      )
    }

    // ── STEP 04: DAILY TARGETS ENGINE & BODY FAT CALIBRATION ──────────────────
    SettingsStepGroupCard(
      stepNumber = "04",
      stepTitle = "DAILY TARGETS & FORMULA ENGINE",
      stepSubtitle = "Active daily macro, hydration, step goals & body fat % override",
      onEditStep = onEditStep04,
      editTestTag = "settings_edit_step_04_btn",
      metrics = metrics,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SettingsMatrixTile(
          label = "CALORIES & STEPS",
          value = "${numberFormat.format(activeCaloriesKcal)} kcal • ${numberFormat.format(activeStepsCount)} steps",
          subValue = "ENERGY & KINETIC OUTPUT TARGETS",
          dotColor = CaloriesStart,
          onClick = onEditStep04,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        SettingsMatrixTile(
          label = "MACROS & HYDRATION",
          value = "P:${activeProteinGrams}g C:${activeCarbsGrams}g F:${activeFatsGrams}g",
          subValue = "H₂O: ${numberFormat.format(activeHydrationMl)} ML • ${if (bodyFatOverrideEnabled) "BF ${String.format(Locale.US, "%.1f", bodyFatPercent)}%" else "STANDARD BMR"}",
          dotColor = HydrationStart,
          onClick = onEditStep04,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "* Step counter is not fully accurate yet — we will improve it in future updates.",
        color = SettingsZinc400.copy(alpha = 0.85f),
        fontFamily = InterFontFamily,
        fontSize = metrics.spScaled(9.2f, 8.2f, 10.2f),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_steps_accuracy_disclaimer"),
      )
    }
  }
}

@Composable
private fun SettingsStepGroupCard(
  stepNumber: String,
  stepTitle: String,
  stepSubtitle: String,
  onEditStep: () -> Unit,
  editTestTag: String,
  metrics: SettingsResponsiveMetrics,
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
          colors = listOf(PureWhite.copy(alpha = 0.18f), SettingsZinc850),
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
            color = SettingsZinc400,
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
          .background(SettingsZinc900)
          .border(1.dp, SettingsZinc700, RoundedCornerShape(7.dp))
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
          letterSpacing = 0.5.sp,
        )
      }
    }

    content()
  }
}

@Composable
private fun SettingsMatrixTile(
  label: String,
  value: String,
  subValue: String,
  dotColor: Color,
  onClick: () -> Unit,
  metrics: SettingsResponsiveMetrics,
  modifier: Modifier = Modifier,
  testTag: String? = null,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(9.5.dp))
      .background(SettingsZinc950)
      .border(
        width = 0.85.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.14f), SettingsZinc800),
        ),
        shape = RoundedCornerShape(9.5.dp),
      )
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 10.5.dp, vertical = 9.dp)
      .let { m -> if (testTag != null) m.testTag(testTag) else m },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier = Modifier
            .size(5.5.dp)
            .clip(CircleShape)
            .background(dotColor),
        )
        Text(
          text = label,
          color = SettingsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(8.8f, 7.8f, 10f),
          letterSpacing = 0.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = "EDIT ↗",
        color = SettingsZinc300,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(8.4f, 7.5f, 9.5f),
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = value,
      color = PureWhite,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = metrics.spScaled(13.2f, 11.6f, 14.8f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = subValue,
      color = SettingsZinc500,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
      letterSpacing = 0.3.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SettingsFactoryResetCard(
  onRequestFactoryReset: () -> Unit,
  metrics: SettingsResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val roseTint = Color(0xFFFDA4AF)
  val crimsonAccent = Color(0xFFF43F5E)
  val deepGarnet = Color(0xFF881337)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .drawWithCache {
        val cornerPx = metrics.cardCornerRadius.toPx()
        val bodyBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to Color(0xFF1F0D12),
            0.42f to Color(0xFF11080B),
            1.0f to Color(0xFF070406),
          ),
        )
        val topRadialGlow = Brush.radialGradient(
          colors = listOf(
            DangerCrimson.copy(alpha = 0.20f),
            crimsonAccent.copy(alpha = 0.06f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.22f, 0f),
          radius = size.width * 0.88f,
        )
        val borderBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to roseTint.copy(alpha = 0.65f),
            0.3f to DangerCrimson.copy(alpha = 0.50f),
            0.75f to Color(0xFF3F1621),
            1.0f to Color(0xFF1F0C12),
          ),
        )
        onDrawBehind {
          drawRoundRect(
            brush = bodyBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )
          drawRoundRect(
            brush = topRadialGlow,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )
          drawRoundRect(
            brush = borderBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = 1.2.dp.toPx()),
          )
          // Top specular crimson-platinum rim highlight
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                roseTint.copy(alpha = 0.82f),
                PureWhite.copy(alpha = 0.55f),
                DangerCrimson.copy(alpha = 0.65f),
                Color.Transparent,
              ),
            ),
            start = Offset(cornerPx * 0.85f, 0.9.dp.toPx()),
            end = Offset(size.width - cornerPx * 0.85f, 0.9.dp.toPx()),
            strokeWidth = 1.1.dp.toPx(),
          )
        }
      }
      .padding(metrics.cardPadding)
      .testTag("settings_factory_reset_card"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Header Row: 3D Hazard Emblem + Dual-Line Title + Sculpted Dual-Tone DANGER ZONE Badge
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        // 3D Crimson Warning Monolith Node
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
              brush = Brush.linearGradient(
                colors = listOf(roseTint, DangerCrimson, deepGarnet),
              ),
            )
            .border(0.9.dp, roseTint.copy(alpha = 0.9f), RoundedCornerShape(7.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.size(13.dp)) {
            val w = size.width
            val h = size.height
            val triPath = Path().apply {
              moveTo(w * 0.5f, h * 0.10f)
              lineTo(w * 0.92f, h * 0.88f)
              lineTo(w * 0.08f, h * 0.88f)
              close()
            }
            drawPath(
              path = triPath,
              color = ObsidianBlack,
              style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
              ),
            )
            drawLine(
              color = ObsidianBlack,
              start = Offset(w * 0.5f, h * 0.38f),
              end = Offset(w * 0.5f, h * 0.62f),
              strokeWidth = 1.5.dp.toPx(),
              cap = StrokeCap.Round,
            )
            drawCircle(
              color = ObsidianBlack,
              radius = 1.0.dp.toPx(),
              center = Offset(w * 0.5f, h * 0.75f),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
          Text(
            text = "SYSTEM DATABASE & FACTORY RESET",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.4f, 9.0f, 11.8f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "IRREVERSIBLE LOCAL VAULT PURGE PROTOCOL",
            color = roseTint.copy(alpha = 0.85f),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(7.5f, 6.6f, 8.6f),
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Sculpted Dual-Tone 3D "DANGER ZONE" Security Interlock Pill
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(Color(0xFF2D0A12), Color(0xFF4C0D19)),
            ),
          )
          .border(
            width = 0.95.dp,
            brush = Brush.horizontalGradient(
              colors = listOf(roseTint.copy(alpha = 0.85f), DangerCrimson.copy(alpha = 0.7f)),
            ),
            shape = RoundedCornerShape(7.dp),
          )
          .padding(start = 5.dp, end = 8.dp, top = 3.5.dp, bottom = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DangerCrimson)
            .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "LVL•0",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(6.8f, 6.0f, 7.8f),
            letterSpacing = 0.3.sp,
          )
        }
        Text(
          text = "DANGER ZONE",
          color = roseTint,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          letterSpacing = 0.5.sp,
        )
      }
    }

    // 2. Sculpted Written Dossier Chamber + 6-Domain Wipe Matrix
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0507), Color(0xFF050304)),
          ),
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            colors = listOf(DangerCrimson.copy(alpha = 0.38f), Color(0xFF261016)),
          ),
          shape = RoundedCornerShape(12.dp),
        )
        .padding(horizontal = 12.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Executive Summary Callout with Vertical Crimson Laser Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .width(3.dp)
            .height((36f * metrics.scale).coerceIn(30f, 44f).dp)
            .clip(CircleShape)
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(roseTint, DangerCrimson, deepGarnet),
              ),
            ),
        )
        Text(
          text = "Permanently erases your on-device BioSync database across all six calibrated telemetry modules and re-initializes the application on the Welcome Home screen.",
          color = PureWhite.copy(alpha = 0.92f),
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = metrics.spScaled(11.3f, 10.0f, 12.6f),
          lineHeight = metrics.spScaled(16.2f, 14.4f, 18.2f),
        )
      }

      // 6-Domain Purge Target Grid (2 rows x 3 columns)
      val purgeTargets = remember {
        listOf(
          Triple("01", "IDENTITY", "Subject & Bio-ID"),
          Triple("02", "SOMATIC", "Stature & Mass"),
          Triple("03", "BLUEPRINT", "PAL & Split"),
          Triple("04", "FORMULA", "Macro Targets"),
          Triple("05", "TIMELINE", "Logs & Meals"),
          Triple("06", "STREAKS", "History & Day 01"),
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        purgeTargets.chunked(3).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            rowItems.forEach { (code, title, subtitle) ->
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    brush = Brush.verticalGradient(
                      colors = listOf(Color(0xFF170B0E), Color(0xFF0D0608)),
                    ),
                  )
                  .border(
                    width = 0.75.dp,
                    color = Color(0xFF38161E),
                    shape = RoundedCornerShape(8.dp),
                  )
                  .padding(horizontal = 7.5.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = code,
                    color = DangerCrimson,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = metrics.spScaled(7.5f, 6.6f, 8.5f),
                  )
                  Text(
                    text = title,
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = subtitle,
                  color = roseTint.copy(alpha = 0.72f),
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = metrics.spScaled(8.4f, 7.4f, 9.4f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = Color(0xFF281117), thickness = 0.8.dp)

      // Bottom Dossier Telemetry Footer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        ) {
          Box(
            modifier = Modifier
              .size(5.5.dp)
              .clip(CircleShape)
              .background(roseTint),
          )
          Text(
            text = "POST-WIPE DESTINATION",
            color = SettingsZinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
            letterSpacing = 0.5.sp,
          )
        }
        Text(
          text = "WELCOME HOME • STEP 00",
          color = roseTint,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          letterSpacing = 0.5.sp,
        )
      }
    }

    // 3. Sculpted 3D Crimson Actuator Button
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .clip(RoundedCornerShape(12.dp))
        .drawWithCache {
          val rPx = 12.dp.toPx()
          val btnBrush = Brush.horizontalGradient(
            colors = listOf(
              Color(0xFFBE123C),
              DangerCrimson,
              Color(0xFFE11D48),
            ),
          )
          onDrawBehind {
            drawRoundRect(
              brush = btnBrush,
              cornerRadius = CornerRadius(rPx, rPx),
            )
            // Top specular highlight
            drawLine(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  PureWhite.copy(alpha = 0.55f),
                  roseTint.copy(alpha = 0.70f),
                  Color.Transparent,
                ),
              ),
              start = Offset(rPx, 1.dp.toPx()),
              end = Offset(size.width - rPx, 1.dp.toPx()),
              strokeWidth = 1.1.dp.toPx(),
            )
          }
        }
        .border(1.dp, roseTint.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
        .clickable(
          role = Role.Button,
          onClick = onRequestFactoryReset,
        )
        .testTag("settings_factory_reset_button"),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ObsidianBlack.copy(alpha = 0.35f))
            .border(0.7.dp, PureWhite.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.5.dp, vertical = 2.dp),
        ) {
          Text(
            text = "PURGE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
            letterSpacing = 0.5.sp,
          )
        }
        Text(
          text = "FACTORY RESET & WIPE DATABASE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(11.8f, 10.4f, 13.4f),
          letterSpacing = 0.8.sp,
        )
      }
    }
  }
}

@Composable
private fun SettingsBackupVaultCard(
  subjectName: String,
  subjectIdBadge: String,
  vaultMeta: BioSyncBackupMeta?,
  statusBanner: String?,
  onCreateBackup: () -> Unit,
  onOpenImportDialog: () -> Unit,
  metrics: SettingsResponsiveMetrics,
) {
  val cardShape = RoundedCornerShape(metrics.cardCornerRadius)
  val platinumLight = Color(0xFFF4F4F5)
  val platinumMid = Color(0xFFD4D4D8)
  val goldAccent = Color(0xFFFFD166)
  val emeraldAccent = Color(0xFF10B981)
  val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(cardShape)
      .drawWithCache {
        val cornerPx = metrics.cardCornerRadius.toPx()
        val bodyBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to Color(0xFF18181B),
            0.42f to Color(0xFF101013),
            1.0f to Color(0xFF070709),
          ),
        )
        val topRadialGlow = Brush.radialGradient(
          colors = listOf(
            PureWhite.copy(alpha = 0.14f),
            goldAccent.copy(alpha = 0.05f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.25f, 0f),
          radius = size.width * 0.90f,
        )
        val borderBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to PureWhite.copy(alpha = 0.68f),
            0.35f to platinumMid.copy(alpha = 0.38f),
            0.75f to Color(0xFF27272A),
            1.0f to Color(0xFF141417),
          ),
        )
        onDrawBehind {
          drawRoundRect(
            brush = bodyBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )
          drawRoundRect(
            brush = topRadialGlow,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )
          drawRoundRect(
            brush = borderBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = 1.25.dp.toPx()),
          )
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                PureWhite.copy(alpha = 0.92f),
                goldAccent.copy(alpha = 0.65f),
                PureWhite.copy(alpha = 0.85f),
                Color.Transparent,
              ),
            ),
            start = Offset(cornerPx * 0.85f, 1.dp.toPx()),
            end = Offset(size.width - cornerPx * 0.85f, 1.dp.toPx()),
            strokeWidth = 1.2.dp.toPx(),
          )
        }
      }
      .padding(metrics.cardPadding)
      .testTag("settings_backup_vault_card"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Architectural Vault Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // 3D Sculpted Brushed-Platinum Vault Crest
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              brush = Brush.linearGradient(
                colors = listOf(PureWhite, platinumMid, Color(0xFFA1A1AA)),
              ),
            )
            .border(1.dp, PureWhite, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.size(15.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
              color = ObsidianBlack,
              topLeft = Offset(w * 0.12f, h * 0.14f),
              size = Size(w * 0.76f, h * 0.72f),
              cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
              style = Stroke(width = 1.6.dp.toPx()),
            )
            drawCircle(
              color = ObsidianBlack,
              radius = w * 0.17f,
              center = Offset(w * 0.50f, h * 0.50f),
              style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
              color = ObsidianBlack,
              radius = w * 0.05f,
              center = Offset(w * 0.50f, h * 0.50f),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "CRYPTIC DATA VAULT & SNAPSHOTS",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.6f, 9.2f, 12.0f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "STEPS 01–04 • TIMELINE LOGS • CALENDAR • SAVED FOODS",
            color = SettingsZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(7.5f, 6.6f, 8.5f),
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(if (vaultMeta != null) Color(0xFF0A1F17) else Color(0xFF141418))
          .border(
            1.dp,
            if (vaultMeta != null) emeraldAccent.copy(alpha = 0.75f) else SettingsZinc700,
            RoundedCornerShape(7.dp),
          )
          .padding(horizontal = 8.5.dp, vertical = 4.dp),
      ) {
        Text(
          text = if (vaultMeta != null) "● VAULT ARMED" else "○ STANDBY",
          color = if (vaultMeta != null) emeraldAccent else SettingsZinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
          letterSpacing = 0.5.sp,
        )
      }
    }

    // 2. Sculpted 3D Snapshot Telemetry Deck
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF09090C))
        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (vaultMeta != null) {
        val vaultBadge = BioSyncLocalStore.computeSubjectIdBadge(vaultMeta.subjectName)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(goldAccent),
            )
            Text(
              text = "ACTIVE SNAPSHOT • ${vaultMeta.subjectName.uppercase()} ($vaultBadge)",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = dateFormat.format(Date(vaultMeta.createdAtEpochMs)),
            color = goldAccent,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(7.8f, 6.9f, 8.8f),
          )
        }

        // 3 Architectural Metric Pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          VaultTelemetryMetricChip(
            label = "TIMELINE LOGS",
            value = vaultMeta.totalTimelineLogsCount.toString(),
            accent = PureWhite,
            modifier = Modifier.weight(1f),
          )
          VaultTelemetryMetricChip(
            label = "CALENDAR DAYS",
            value = vaultMeta.historicalDaysCount.toString(),
            accent = goldAccent,
            modifier = Modifier.weight(1f),
          )
          VaultTelemetryMetricChip(
            label = "SAVED FOODS",
            value = vaultMeta.savedQuickItemsCount.toString(),
            accent = emeraldAccent,
            modifier = Modifier.weight(1f),
          )
        }
      } else {
        Text(
          text = "Create an encrypted local JSON snapshot of your entire BioSync database (Steps 01–04 calibration, custom targets, timeline entries, and calendar history) for instant 1-tap recovery.",
          color = SettingsZinc300,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10.8f, 9.6f, 12.0f),
          lineHeight = metrics.spScaled(15.2f, 13.5f, 16.8f),
        )
      }

      if (!statusBanner.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF071A12))
            .border(1.dp, emeraldAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "✓ $statusBanner",
            color = emeraldAccent,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
          )
        }
      }
    }

    // 3. High-Contrast Dual Action Buttons: BACKUP DATA (Pure White 3D) & IMPORT BACKUP (Obsidian + Platinum)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .clip(RoundedCornerShape(11.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(PureWhite, platinumLight, platinumMid),
            ),
          )
          .border(1.2.dp, PureWhite, RoundedCornerShape(11.dp))
          .clickable(role = Role.Button, onClick = onCreateBackup)
          .testTag("settings_backup_data_button"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "▲",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = metrics.spScaled(9.5f, 8.5f, 10.8f),
          )
          Text(
            text = "EXPORT BACKUP",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.6f, 9.4f, 12.0f),
            letterSpacing = 0.6.sp,
          )
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .clip(RoundedCornerShape(11.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF1F1F24), Color(0xFF121216)),
            ),
          )
          .border(1.2.dp, PureWhite.copy(alpha = 0.55f), RoundedCornerShape(11.dp))
          .clickable(role = Role.Button, onClick = onOpenImportDialog)
          .testTag("settings_import_backup_button"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "▼",
            color = goldAccent,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = metrics.spScaled(9.5f, 8.5f, 10.8f),
          )
          Text(
            text = "IMPORT BACKUP",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.6f, 9.4f, 12.0f),
            letterSpacing = 0.6.sp,
          )
        }
      }
    }
  }
}

@Composable
private fun VaultTelemetryMetricChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF121216))
      .border(1.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = value,
      color = accent,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 12.5.sp,
    )
    Text(
      text = label,
      color = SettingsZinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 7.0.sp,
      letterSpacing = 0.4.sp,
      maxLines = 1,
    )
  }
}

/**
 * Shared Welcome Screen Backup Check Modal:
 * Shown immediately after clicking Continue ("Initialize Calibration") on the Welcome screen.
 * Asks the user whether they have a backup to restore or want to start fresh.
 */
@Composable
internal fun WelcomeBackupCheckModal(
  onStartFreshCalibration: () -> Unit,
  onOpenImportBackupFlow: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val vaultMeta = remember { BioSyncBackupManager.getRecoveryVaultMeta(context) }
  val platinumMid = Color(0xFFD4D4D8)
  val goldAccent = Color(0xFFFFD166)
  val emeraldAccent = Color(0xFF10B981)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 450.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF18181C), Color(0xFF0D0D10), Color(0xFF060608)),
            ),
          )
          .border(
            width = 1.3.dp,
            brush = Brush.verticalGradient(
              colors = listOf(PureWhite.copy(alpha = 0.78f), platinumMid.copy(alpha = 0.35f), SettingsZinc800),
            ),
            shape = RoundedCornerShape(22.dp),
          )
          .padding(22.dp)
          .testTag("welcome_backup_prompt_dialog"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
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
                .background(PureWhite),
            )
            Text(
              text = "BIOSYNC VAULT • ONBOARDING CHECK",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.4.sp,
              letterSpacing = 0.9.sp,
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF141418))
              .border(1.dp, SettingsZinc700, RoundedCornerShape(6.dp))
              .clickable(role = Role.Button, onClick = onDismiss)
              .padding(horizontal = 8.dp, vertical = 3.dp),
          ) {
            Text(
              text = "ESC",
              color = SettingsZinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp,
            )
          }
        }

        Text(
          text = "Restore Existing Protocol Vault?",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.5.sp,
          letterSpacing = (-0.4).sp,
        )

        Text(
          text = "If you previously exported a BioSync database backup or saved a pre-reset vault snapshot, you can restore 100% of your Steps 01–04 calibration, custom targets, timeline entries, and calendar history immediately.",
          color = SettingsZinc300,
          fontFamily = InterFontFamily,
          fontSize = 12.5.sp,
          lineHeight = 18.sp,
        )

        if (vaultMeta != null) {
          val vaultBadge = BioSyncLocalStore.computeSubjectIdBadge(vaultMeta.subjectName)
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF0A0A0D))
              .border(1.dp, goldAccent.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "LOCAL VAULT DETECTED • ${vaultMeta.subjectName.uppercase()} ($vaultBadge)",
                color = goldAccent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.2.sp,
                letterSpacing = 0.5.sp,
              )
              Text(
                text = "READY",
                color = emeraldAccent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.4.sp,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              VaultTelemetryMetricChip(
                label = "TIMELINE LOGS",
                value = vaultMeta.totalTimelineLogsCount.toString(),
                accent = PureWhite,
                modifier = Modifier.weight(1f),
              )
              VaultTelemetryMetricChip(
                label = "CALENDAR DAYS",
                value = vaultMeta.historicalDaysCount.toString(),
                accent = goldAccent,
                modifier = Modifier.weight(1f),
              )
              VaultTelemetryMetricChip(
                label = "SAVED FOODS",
                value = vaultMeta.savedQuickItemsCount.toString(),
                accent = emeraldAccent,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Primary CTA: YES — IMPORT / RESTORE BACKUP (Pure White 3D Monolith)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(PureWhite, Color(0xFFE4E4E7)),
              ),
            )
            .border(1.2.dp, PureWhite, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onOpenImportBackupFlow)
            .testTag("welcome_has_backup_yes_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (vaultMeta != null) "YES — RESTORE FROM VAULT OR .JSON FILE" else "YES — IMPORT BACKUP DATA",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.6.sp,
            letterSpacing = 0.7.sp,
          )
        }

        // Secondary CTA: NO — START FRESH CALIBRATION (Obsidian + Platinum Border)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131317))
            .border(1.dp, PureWhite.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onStartFreshCalibration)
            .testTag("welcome_has_backup_no_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "NO — INITIALIZE NEW CALIBRATION (STEP 01)",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.0.sp,
            letterSpacing = 0.6.sp,
          )
        }
      }
    }
  }
}

/**
 * Full Backup Import & Restore Dialog + Post-Import Destination Selector:
 * - Supports 1-tap restore from Recovery Vault (if present), importing a `.json` backup file,
 *   or pasting backup JSON from clipboard.
 * - Automatically persists all restored data into `BioSyncLocalStore` and `DashboardLocalStore`.
 * - After successful import, asks the user whether they want to:
 *   1) Go through all calibration steps again (Step 01–04) with their restored data pre-filled
 *   2) Jump directly to the Live Dashboard (Step 05)
 */
@Composable
internal fun BioSyncBackupImportDialog(
  onRestoreComplete: (restoredBundle: BioSyncFullBackupBundle, jumpDirectlyToDashboard: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val platinumMid = Color(0xFFD4D4D8)
  val goldAccent = Color(0xFFFFD166)
  val emeraldAccent = Color(0xFF10B981)

  val vaultMeta = remember { BioSyncBackupManager.getRecoveryVaultMeta(context) }
  var rawJsonInput by rememberSaveable { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var importedBundle by remember { mutableStateOf<BioSyncFullBackupBundle?>(null) }

  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (text.isNotBlank()) {
          rawJsonInput = text
          val restored = BioSyncBackupManager.importAndPersistBackupJson(context, text).getOrNull()
          if (restored != null) {
            haptics.confirm()
            errorMessage = null
            importedBundle = restored
          } else {
            haptics.warning()
            errorMessage = "Selected file is not a valid BioSync backup JSON."
          }
        }
      }.onFailure {
        haptics.warning()
        errorMessage = "Unable to read selected backup file."
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF18181C), Color(0xFF0D0D10), Color(0xFF060608)),
            ),
          )
          .border(
            width = 1.3.dp,
            brush = Brush.verticalGradient(
              colors = listOf(PureWhite.copy(alpha = 0.80f), platinumMid.copy(alpha = 0.35f), SettingsZinc800),
            ),
            shape = RoundedCornerShape(22.dp),
          )
          .padding(22.dp)
          .verticalScroll(rememberScrollState())
          .testTag("backup_import_dialog"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
      ) {
        val activeRestored = importedBundle
        if (activeRestored == null) {
          // PHASE 1: SELECT OR PASTE BACKUP SOURCE
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
                  .background(PureWhite),
              )
              Text(
                text = "DATABASE RESTORE ENGINE",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.6.sp,
                letterSpacing = 0.8.sp,
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF141418))
                .border(1.dp, SettingsZinc700, RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
              Text(
                text = "CLOSE",
                color = SettingsZinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
              )
            }
          }

          Text(
            text = "Import & Synchronize Vault",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = (-0.4).sp,
          )

          // Option A: 1-Tap Recovery Vault Restore (if available)
          if (vaultMeta != null) {
            val vaultBadge = BioSyncLocalStore.computeSubjectIdBadge(vaultMeta.subjectName)
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF09090C))
                .border(1.dp, goldAccent.copy(alpha = 0.65f), RoundedCornerShape(13.dp))
                .padding(13.dp),
              verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "RECOVERY VAULT • ${vaultMeta.subjectName.uppercase()} ($vaultBadge)",
                  color = goldAccent,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.4.sp,
                  letterSpacing = 0.5.sp,
                )
                Text(
                  text = "VERIFIED",
                  color = emeraldAccent,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.2.sp,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                VaultTelemetryMetricChip(
                  label = "TIMELINE LOGS",
                  value = vaultMeta.totalTimelineLogsCount.toString(),
                  accent = PureWhite,
                  modifier = Modifier.weight(1f),
                )
                VaultTelemetryMetricChip(
                  label = "CALENDAR DAYS",
                  value = vaultMeta.historicalDaysCount.toString(),
                  accent = goldAccent,
                  modifier = Modifier.weight(1f),
                )
                VaultTelemetryMetricChip(
                  label = "SAVED FOODS",
                  value = vaultMeta.savedQuickItemsCount.toString(),
                  accent = emeraldAccent,
                  modifier = Modifier.weight(1f),
                )
              }

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    brush = Brush.verticalGradient(
                      colors = listOf(PureWhite, Color(0xFFE4E4E7)),
                    ),
                  )
                  .border(1.dp, PureWhite, RoundedCornerShape(10.dp))
                  .clickable(
                    role = Role.Button,
                    onClick = {
                      val vaultJson = BioSyncBackupManager.getRecoveryVaultJson(context)
                      if (!vaultJson.isNullOrBlank()) {
                        val restored = BioSyncBackupManager.importAndPersistBackupJson(context, vaultJson).getOrNull()
                        if (restored != null) {
                          haptics.confirm()
                          errorMessage = null
                          importedBundle = restored
                        } else {
                          errorMessage = "Vault snapshot could not be parsed."
                        }
                      }
                    },
                  )
                  .testTag("restore_from_vault_button"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "1-TAP RESTORE FROM RECOVERY VAULT",
                  color = ObsidianBlack,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.6.sp,
                  letterSpacing = 0.6.sp,
                )
              }
            }
          }

          // Option B: Pick JSON file or Paste from Clipboard
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141418))
                .border(1.dp, PureWhite.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                .clickable(
                  role = Role.Button,
                  onClick = {
                    haptics.selection()
                    runCatching {
                      openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                  },
                )
                .testTag("import_backup_pick_file_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "SELECT .JSON FILE",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141418))
                .border(1.dp, SettingsZinc700, RoundedCornerShape(10.dp))
                .clickable(
                  role = Role.Button,
                  onClick = {
                    haptics.selection()
                    runCatching {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                      val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                      if (pasted.isNotBlank()) {
                        rawJsonInput = pasted
                        errorMessage = null
                      }
                    }
                  },
                )
                .testTag("import_backup_paste_clipboard_btn"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "PASTE CLIPBOARD",
                color = SettingsZinc200,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
              )
            }
          }

          // Option C: Backup JSON Text Box
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(105.dp)
              .clip(RoundedCornerShape(11.dp))
              .background(Color(0xFF070709))
              .border(1.dp, Color(0xFF27272A), RoundedCornerShape(11.dp))
              .padding(11.dp),
          ) {
            if (rawJsonInput.isEmpty()) {
              Text(
                text = "Or paste your BioSync JSON backup payload here...",
                color = SettingsZinc500,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.5.sp,
              )
            }
            BasicTextField(
              value = rawJsonInput,
              onValueChange = {
                rawJsonInput = it
                errorMessage = null
              },
              textStyle = TextStyle(
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.5.sp,
              ),
              cursorBrush = SolidColor(PureWhite),
              modifier = Modifier
                .fillMaxSize()
                .testTag("import_backup_json_input"),
            )
          }

          if (!errorMessage.isNullOrBlank()) {
            Text(
              text = errorMessage!!,
              color = DangerCrimson,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(11.dp))
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(PureWhite, Color(0xFFE4E4E7)),
                ),
              )
              .border(1.2.dp, PureWhite, RoundedCornerShape(11.dp))
              .clickable(
                role = Role.Button,
                onClick = {
                  val restored = BioSyncBackupManager.importAndPersistBackupJson(context, rawJsonInput).getOrNull()
                  if (restored != null) {
                    haptics.confirm()
                    errorMessage = null
                    importedBundle = restored
                  } else {
                    haptics.warning()
                    errorMessage = "Invalid backup JSON. Use Recovery Vault, pick a valid .json file, or paste a full backup."
                  }
                },
              )
              .testTag("import_backup_verify_apply_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "VERIFY & SYNCHRONIZE DATABASE",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.4.sp,
              letterSpacing = 0.6.sp,
            )
          }
        } else {
          // PHASE 2: DATA SUCCESSFULLY IMPORTED -> ASK USER WHETHER TO GO THROUGH ALL STEPS AGAIN OR JUMP TO DASHBOARD
          val restoredBadge = BioSyncLocalStore.computeSubjectIdBadge(activeRestored.meta.subjectName)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(emeraldAccent),
            )
            Text(
              text = "VAULT SYNCHRONIZATION COMPLETE",
              color = emeraldAccent,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.8.sp,
              letterSpacing = 0.8.sp,
            )
          }

          Text(
            text = "All Database Records Restored",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = (-0.4).sp,
          )

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF09090C))
              .border(1.dp, emeraldAccent.copy(alpha = 0.60f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "RESTORED SUBJECT • ${activeRestored.meta.subjectName.uppercase()} ($restoredBadge)",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 9.5.sp,
              letterSpacing = 0.5.sp,
            )
            Text(
              text = "Calibrated targets (${activeRestored.storedState.customCaloriesKcal} kcal • ${activeRestored.storedState.customProteinGrams}g P • ${activeRestored.storedState.customHydrationMl} ml H₂O), ${activeRestored.meta.totalTimelineLogsCount} timeline logs, and ${activeRestored.meta.historicalDaysCount} calendar days are now active.",
              color = SettingsZinc200,
              fontFamily = InterFontFamily,
              fontSize = 11.8.sp,
              lineHeight = 16.8.sp,
            )
          }

          Text(
            text = "Select your launch destination:",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
          )

          // Option 1: Jump Directly to Dashboard
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(PureWhite, Color(0xFFE4E4E7)),
                ),
              )
              .border(1.2.dp, PureWhite, RoundedCornerShape(12.dp))
              .clickable(
                role = Role.Button,
                onClick = { onRestoreComplete(activeRestored, true) },
              )
              .testTag("post_import_jump_dashboard_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "JUMP DIRECTLY TO DASHBOARD",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.6.sp,
              letterSpacing = 0.7.sp,
            )
          }

          // Option 2: Go Through All Calibration Steps Again (Step 01-04)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF141418))
              .border(1.dp, PureWhite.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
              .clickable(
                role = Role.Button,
                onClick = { onRestoreComplete(activeRestored, false) },
              )
              .testTag("post_import_review_steps_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "REVIEW CALIBRATION STEPS (STEP 01–04)",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 11.0.sp,
              letterSpacing = 0.6.sp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FactoryResetConfirmationDialog(
  subjectName: String,
  subjectIdBadge: String,
  onKeepBackupAndReset: () -> Unit,
  onResetWithoutBackup: () -> Unit,
  onDismiss: () -> Unit,
) {
  val roseTint = Color(0xFFFDA4AF)
  val goldAccent = Color(0xFFFFD166)
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 450.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF1A1014), Color(0xFF0C090B), Color(0xFF060506)),
            ),
          )
          .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
              colors = listOf(roseTint.copy(alpha = 0.72f), DangerCrimson.copy(alpha = 0.48f), SettingsZinc800),
            ),
            shape = RoundedCornerShape(20.dp),
          )
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
          .testTag("factory_reset_confirm_dialog"),
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
                .background(DangerCrimson),
            )
            Text(
              text = "CONFIRM FACTORY RESET",
              color = roseTint,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.2.sp,
              letterSpacing = 0.9.sp,
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(SettingsZinc900)
              .border(1.dp, SettingsZinc700, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp),
          ) {
            Text(
              text = "ID • $subjectIdBadge",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 9.5.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Purge Active Database Records?",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          letterSpacing = (-0.4).sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF080406))
            .border(1.dp, Color(0xFF38161E), RoundedCornerShape(11.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "SUBJECT TARGET • ${subjectName.ifBlank { "CURRENT SUBJECT" }.uppercase()}",
            color = roseTint,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.2.sp,
            letterSpacing = 0.6.sp,
          )
          Text(
            text = "This action erases your active protocol database — including Steps 01–04 calibration, custom targets, saved foods, timeline logs, and streaks — and restarts BioSync on the Welcome screen.",
            color = SettingsZinc300,
            fontFamily = InterFontFamily,
            fontSize = 12.2.sp,
            lineHeight = 17.5.sp,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggestion / Warning Box to keep a Backup before Factory Reset (Obsidian & Gold/White)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF0B0B0E))
            .border(1.dp, goldAccent.copy(alpha = 0.62f), RoundedCornerShape(11.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "RECOMMENDED • ACCIDENTAL RESET SAFEGUARD",
            color = goldAccent,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.2.sp,
            letterSpacing = 0.5.sp,
          )
          Text(
            text = "Keep an encrypted Recovery Vault snapshot before resetting. If you reset by mistake, you can restore 100% of your database in 1 tap from the Welcome screen.",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontSize = 11.6.sp,
            lineHeight = 16.5.sp,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Option 1: Keep Backup & Factory Reset (Pure White 3D Primary Action)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(PureWhite, Color(0xFFE4E4E7)),
              ),
            )
            .border(1.2.dp, PureWhite, RoundedCornerShape(11.dp))
            .clickable(role = Role.Button, onClick = onKeepBackupAndReset)
            .testTag("factory_reset_keep_backup_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "KEEP BACKUP & FACTORY RESET",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.4.sp,
            letterSpacing = 0.6.sp,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(SettingsZinc900)
              .border(1.dp, SettingsZinc700, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button, onClick = onDismiss)
              .testTag("factory_reset_cancel_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Cancel",
              color = PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
            )
          }

          Box(
            modifier = Modifier
              .weight(1.25f)
              .height(44.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                brush = Brush.horizontalGradient(
                  colors = listOf(DangerCrimsonDeep, DangerCrimson),
                ),
              )
              .border(1.dp, roseTint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
              .clickable(role = Role.Button, onClick = onResetWithoutBackup)
              .testTag("factory_reset_confirm_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Reset Without Backup",
              color = PureWhite,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
            )
          }
        }
      }
    }
  }
}


