package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Immutable snapshot of live telemetry synchronized with Google Fit
 * (`com.google.android.apps.fitness`) via Health Connect + Live Hardware Step Counter bridge
 * so steps & distance update in real time even when the Google Fit app is not opened:
 * - Total Steps (`liveTodaySteps`)
 * - Total Distance (`liveDistanceMeters`, `liveDistanceKm`)
 */
@Immutable
data class BioSyncStepSyncState(
  val decidedDailyStepsGoal: Int = 8500,
  val liveTodaySteps: Int = 0,
  val healthConnectSteps: Int = 0,
  val hardwareSensorSteps: Int = 0,
  val isActivelyWalking: Boolean = false,
  val liveDistanceMeters: Float = 0f,
  val lastStepDetectedEpochMillis: Long = 0L,
  val isHealthConnectAvailable: Boolean = false,
  val isHealthConnectUpdateRequired: Boolean = false,
  val hasHealthConnectReadPermission: Boolean = false,
  val hasHealthConnectWritePermission: Boolean = false,
  val hasHealthConnectBackgroundReadPermission: Boolean = false,
  val hasActivityRecognitionPermission: Boolean = false,
  val isIgnoringBatteryOptimizations: Boolean = false,
  val hasHardwareStepSensor: Boolean = false,
  val isLiveSensorListening: Boolean = false,
  val connectedDataOrigins: Set<String> = emptySet(),
  val lastSyncedEpochMillis: Long = 0L,
) {
  val liveDistanceKm: Float
    get() = liveDistanceMeters / 1000f

  val goalCompletionFraction: Float
    get() = if (decidedDailyStepsGoal > 0) {
      (liveTodaySteps.toFloat() / decidedDailyStepsGoal.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }

  val remainingStepsToGoal: Int
    get() = (decidedDailyStepsGoal - liveTodaySteps).coerceAtLeast(0)

  val isHealthConnectAllowAllGranted: Boolean
    get() = if (isHealthConnectAvailable) {
      hasHealthConnectReadPermission && hasHealthConnectWritePermission
    } else {
      (hasHealthConnectReadPermission && hasHealthConnectWritePermission) ||
        !isHealthConnectUpdateRequired
    }

  /**
   * True when all required mobile permissions are granted:
   * 1. Health Connect "Allow all"
   * 2. Physical Activity sensor access (`ACTIVITY_RECOGNITION`)
   * 3. Unrestricted background battery execution (`isIgnoringBatteryOptimizations`)
   */
  val isAllPermissionsAllowed: Boolean
    get() = isHealthConnectAllowAllGranted &&
      hasActivityRecognitionPermission &&
      isIgnoringBatteryOptimizations
}

/**
 * Controller handle exposed to Compose screens so they can verify whether all required permissions
 * ("Allow all" in Health Connect, Physical Activity sensor, and Unrestricted Battery background run)
 * are granted, trigger the permission chain, and sync the decided daily steps goal.
 */
class BioSyncStepSyncController(
  val state: BioSyncStepSyncState,
  val verifyAllPermissionsAllowed: suspend () -> Boolean,
  val requestAllowAllPermissions: (onResult: (allGranted: Boolean) -> Unit) -> Unit,
  val requestBatteryOptimizationExemption: () -> Unit,
  val forceRefreshNow: () -> Unit,
  val openHealthConnectAppOrSettings: () -> Unit,
)

/**
 * Real-Time Google Fit (`com.google.android.apps.fitness`) + Hardware `TYPE_STEP_COUNTER` Synchronization Engine:
 *
 * Why Google Fit alone lags when closed:
 * - Google Fit reads the device's hardware `Sensor.TYPE_STEP_COUNTER`, but Google Fit often waits until
 *   you open the Google Fit app UI (or runs a delayed 15–60 min background job) before flushing its
 *   `StepsRecord` and `DistanceRecord` entries into Health Connect.
 *
 * How this engine solves it with 1:1 accuracy:
 * 1. **Authoritative Health Connect Base (`hcFitSteps`, `hcFitDistanceMeters`)**:
 *    Continuously reads Google Fit's de-duplicated `StepsRecord` and `DistanceRecord` totals from Health Connect.
 * 2. **Live Unflushed Hardware Step Bridge (`hcAnchorRawCounter` -> `lastRawCounter`)**:
 *    Tracks the exact hardware `Sensor.TYPE_STEP_COUNTER` value anchored to Google Fit's last Health Connect flush
 *    (`hcAnchorRawCounter`). Any new steps taken while Google Fit is closed immediately increment
 *    `unflushedHardwareSteps = lastRawCounter - hcAnchorRawCounter` (plus real-time `TYPE_STEP_DETECTOR` footstep
 *    ticks between sensor batches), so BioSync updates live on every single step **without needing to open Google Fit**.
 * 3. **Zero Double-Counting on Google Fit Flush**:
 *    The moment Google Fit flushes to Health Connect (either in the background or when the user opens Google Fit),
 *    `hcFitSteps` increases by `hcFlushedDelta` and `hcAnchorRawCounter` advances by the exact same `hcFlushedDelta`
 *    (or snaps to `lastRawCounter`), seamlessly handing off from unflushed hardware steps to Google Fit's flushed
 *    Health Connect records with zero jump and zero duplication.
 * 4. **On-Demand Hardware Counter Probe (`probeHardwareStepCounterNow`)**:
 *    Because `Sensor.TYPE_STEP_COUNTER` is an `ON_CHANGE` kernel sensor that immediately emits its current latched
 *    total whenever a listener registers, we fire a lightweight one-shot probe on resume, sync, and periodic ticks
 *    so steps walked while the screen was off appear the instant you look at BioSync—even while standing still.
 */
object BioSyncHealthStepManager {
  const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
  const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"
  private const val PERM_READ_STEPS = "android.permission.health.READ_STEPS"
  private const val PERM_WRITE_STEPS = "android.permission.health.WRITE_STEPS"
  private const val PERM_BG_HEALTH = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val hcSyncMutex = Mutex()
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _syncState = MutableStateFlow(BioSyncStepSyncState())
  val syncState: StateFlow<BioSyncStepSyncState> = _syncState.asStateFlow()

  private var sensorManager: SensorManager? = null
  private var activeStepCounterListener: SensorEventListener? = null
  private var activeStepDetectorListener: SensorEventListener? = null
  private var walkingDecayJob: Job? = null
  private var immediateHcReadJob: Job? = null
  private var persistJob: Job? = null

  @Volatile private var inMemoryInitialized: Boolean = false
  @Volatile private var inMemoryDayKey: String = ""
  @Volatile private var hasPurgedLegacySelfRecords: Boolean = false

  // User biomechanics for Google Fit-equivalent distance per unflushed step
  @Volatile private var cachedUserHeightCm: Float = 172f
  @Volatile private var cachedUserSex: String = "FEMALE"

  // 1. Authoritative Google Fit / Health Connect flushed totals for today
  @Volatile private var hcFitSteps: Int = 0
  @Volatile private var hcFitDistanceMeters: Float = 0f

  // 2. Hardware TYPE_STEP_COUNTER anchor & live counter for unflushed steps taken while Google Fit is closed
  @Volatile private var hcAnchorRawCounter: Float = -1f
  @Volatile private var lastRawCounter: Float = -1f
  @Volatile private var pendingDetectorSteps: Int = 0

  // 3. Monotonic live combined totals for today (hcFitSteps + unflushed hardware steps)
  @Volatile private var liveCombinedSteps: Int = 0
  @Volatile private var liveCombinedDistanceMeters: Float = 0f

  @Volatile private var lastStepRealtimeMs: Long = 0L
  @Volatile private var lastImmediateHcTriggerMs: Long = 0L
  @Volatile private var lastHardwareProbeElapsedMs: Long = 0L
  @Volatile private var isProbeInFlight: Boolean = false

  @get:SuppressLint("NewApi")
  val baseHealthPermissions: Set<String> by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
      )
    } else {
      emptySet()
    }
  }

  /**
   * Requests read/write permissions strictly for Steps, Distance, and Background Health Read
   * so toggling "Allow all" in Health Connect connects Google Fit's Steps and Distance streams.
   */
  @SuppressLint("NewApi")
  fun buildRequestedHealthPermissions(context: Context): Set<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptySet()
    val perms = mutableSetOf(
      HealthPermission.getReadPermission(StepsRecord::class),
      HealthPermission.getWritePermission(StepsRecord::class),
      HealthPermission.getReadPermission(DistanceRecord::class),
      HealthPermission.getWritePermission(DistanceRecord::class),
    )
    runCatching {
      if (getHealthConnectSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
        val client = HealthConnectClient.getOrCreate(context.applicationContext)
        val bgFeatureStatus = client.features.getFeatureStatus(
          HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
        )
        if (bgFeatureStatus == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) {
          perms.add(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
        }
      }
    }
    return perms
  }

  private fun currentDayKey(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }

  /**
   * Computes the exact meters-per-step factor used by Google Fit:
   * - If Google Fit has already flushed steps and distance today, uses Google Fit's exact empirical
   *   `hcFitDistanceMeters / hcFitSteps` ratio for this user.
   * - Otherwise uses Google Fit's anthropometric stride formula (`heightCm * 0.415` for male, `0.413` for female).
   */
  private fun effectiveMetersPerStep(): Float {
    if (hcFitSteps >= 30 && hcFitDistanceMeters > 10f) {
      return (hcFitDistanceMeters / hcFitSteps.toFloat()).coerceIn(0.45f, 1.15f)
    }
    val hCm = cachedUserHeightCm.coerceIn(120f, 230f)
    val coeff = if (cachedUserSex.equals("MALE", ignoreCase = true)) 0.415f else 0.413f
    return ((hCm * coeff) / 100f).coerceIn(0.50f, 1.05f)
  }

  fun hasActivityRecognitionPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACTIVITY_RECOGNITION,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
      ?: return true
    return runCatching {
      pm.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)
  }

  @SuppressLint("BatteryLife")
  fun createIgnoreBatteryOptimizationIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
      )
    } else {
      Intent(Settings.ACTION_SETTINGS)
    }
  }

  fun openBatteryOptimizationSettings(context: Context) {
    runCatching {
      val directIntent = createIgnoreBatteryOptimizationIntent(context).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(directIntent)
    }.onFailure {
      runCatching {
        val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(listIntent)
      }
    }
  }

  /**
   * Synchronously checks Android 14+ (API 34+) framework Health Connect permissions
   * so we immediately know if the user already enabled "Allow all" in system settings.
   */
  fun hasFrameworkHealthStepsAllowAll(context: Context): Pair<Boolean, Boolean> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      return false to false
    }
    val readGranted = ContextCompat.checkSelfPermission(
      context,
      PERM_READ_STEPS,
    ) == PackageManager.PERMISSION_GRANTED
    val writeGranted = ContextCompat.checkSelfPermission(
      context,
      PERM_WRITE_STEPS,
    ) == PackageManager.PERMISSION_GRANTED
    return readGranted to writeGranted
  }

  fun hasHardwareStepSensor(context: Context): Boolean {
    val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      ?: return false
    return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
      sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
  }

  fun isGoogleFitInstalled(context: Context): Boolean {
    return runCatching {
      context.packageManager.getPackageInfo(GOOGLE_FIT_PACKAGE, 0) != null
    }.getOrDefault(false)
  }

  @SuppressLint("NewApi")
  fun getHealthConnectSdkStatus(context: Context): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return HealthConnectClient.SDK_UNAVAILABLE
    }
    return runCatching {
      val defaultStatus = HealthConnectClient.getSdkStatus(context)
      if (defaultStatus == HealthConnectClient.SDK_AVAILABLE) {
        defaultStatus
      } else {
        HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
      }
    }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)
  }

  private fun ensureMidnightRollover(todayKey: String) {
    if (inMemoryDayKey.isNotEmpty() && inMemoryDayKey != todayKey) {
      inMemoryDayKey = todayKey
      hcFitSteps = 0
      hcFitDistanceMeters = 0f
      liveCombinedSteps = 0
      liveCombinedDistanceMeters = 0f
      pendingDetectorSteps = 0
      if (lastRawCounter >= 0f) {
        hcAnchorRawCounter = lastRawCounter
      } else {
        hcAnchorRawCounter = -1f
      }
    } else if (inMemoryDayKey.isEmpty()) {
      inMemoryDayKey = todayKey
    }
  }

  fun initializeFromLocalStore(context: Context, currentDecidedGoal: Int) {
    val appContext = context.applicationContext
    val stored = BioSyncLocalStore.load(appContext)
    cachedUserHeightCm = stored.heightCm
    cachedUserSex = stored.selectedSex

    val todayKey = currentDayKey()
    val isSameDay = stored.stepSensorDayKey == todayKey

    if (!inMemoryInitialized || inMemoryDayKey != todayKey) {
      inMemoryDayKey = todayKey
      if (isSameDay) {
        hcFitSteps = stored.lastHealthConnectWrittenSteps.coerceAtLeast(0)
        hcAnchorRawCounter = stored.stepSensorBaselineCounter
        lastRawCounter = stored.stepSensorLastRawCounter
        liveCombinedSteps = max(stored.todaySyncedSteps.coerceAtLeast(0), hcFitSteps)
        liveCombinedDistanceMeters = stored.todayDistanceMeters.coerceAtLeast(0f)

        val unflushedRestored = (liveCombinedSteps - hcFitSteps).coerceAtLeast(0)
        val estimatedUnflushedDist = unflushedRestored * effectiveMetersPerStep()
        hcFitDistanceMeters = (liveCombinedDistanceMeters - estimatedUnflushedDist).coerceAtLeast(0f)
      } else {
        hcFitSteps = 0
        hcFitDistanceMeters = 0f
        liveCombinedSteps = 0
        liveCombinedDistanceMeters = 0f
        pendingDetectorSteps = 0
        hcAnchorRawCounter = if (lastRawCounter >= 0f) lastRawCounter else -1f
      }
      inMemoryInitialized = true
    }

    val hcStatus = getHealthConnectSdkStatus(appContext)
    val activityGranted = hasActivityRecognitionPermission(appContext)
    val batteryUnrestricted = isIgnoringBatteryOptimizations(appContext)
    val (fwReadGranted, fwWriteGranted) = hasFrameworkHealthStepsAllowAll(appContext)
    val effectiveRead = fwReadGranted || stored.healthConnectPermissionsGranted || _syncState.value.hasHealthConnectReadPermission
    val effectiveWrite = fwWriteGranted || stored.healthConnectPermissionsGranted || _syncState.value.hasHealthConnectWritePermission
    val hasSensor = hasHardwareStepSensor(appContext)
    val effectiveGoal = if (currentDecidedGoal > 0) currentDecidedGoal else stored.decidedDailyStepsGoal

    _syncState.value = _syncState.value.copy(
      decidedDailyStepsGoal = effectiveGoal,
      liveTodaySteps = liveCombinedSteps,
      healthConnectSteps = hcFitSteps,
      hardwareSensorSteps = liveCombinedSteps,
      liveDistanceMeters = liveCombinedDistanceMeters,
      isHealthConnectAvailable = hcStatus == HealthConnectClient.SDK_AVAILABLE,
      isHealthConnectUpdateRequired = hcStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
      hasHealthConnectReadPermission = effectiveRead,
      hasHealthConnectWritePermission = effectiveWrite,
      hasActivityRecognitionPermission = activityGranted,
      isIgnoringBatteryOptimizations = batteryUnrestricted,
      hasHardwareStepSensor = hasSensor,
      lastSyncedEpochMillis = stored.lastStepSyncEpochMillis,
    )

    ioScope.launch {
      BioSyncLocalStore.saveStepTelemetrySnapshot(
        context = appContext,
        decidedDailyStepsGoal = effectiveGoal,
        healthConnectPermissionsGranted = effectiveRead && effectiveWrite,
        activityRecognitionGranted = activityGranted,
      )
    }
  }

  fun updateDecidedDailyStepsGoal(context: Context, goalSteps: Int) {
    val clampedGoal = goalSteps.coerceAtLeast(500)
    _syncState.value = _syncState.value.copy(decidedDailyStepsGoal = clampedGoal)
    val appContext = context.applicationContext
    ioScope.launch {
      BioSyncLocalStore.saveStepTelemetrySnapshot(
        context = appContext,
        decidedDailyStepsGoal = clampedGoal,
      )
    }
  }

  /**
   * Computes the current unflushed hardware steps taken since Google Fit's last Health Connect flush:
   * `unflushedSteps = max(0, lastRawCounter - hcAnchorRawCounter) + pendingDetectorSteps`
   */
  private fun currentUnflushedHardwareSteps(): Int {
    val counterDelta = if (lastRawCounter >= 0f && hcAnchorRawCounter >= 0f && lastRawCounter >= hcAnchorRawCounter) {
      (lastRawCounter - hcAnchorRawCounter).roundToInt().coerceAtLeast(0)
    } else {
      0
    }
    return (counterDelta + pendingDetectorSteps.coerceAtLeast(0)).coerceAtLeast(0)
  }

  /**
   * Recomputes the combined live steps & distance (`Google Fit Health Connect base + unflushed hardware steps`)
   * and emits them immediately to [syncState] and [BioSyncLocalStore].
   */
  private fun recalculateAndEmitLiveTelemetry(
    appContext: Context,
    nowEpochMs: Long,
    activelyWalking: Boolean = _syncState.value.isActivelyWalking,
    allowStepDecrease: Boolean = false,
  ) {
    val unflushedSteps = currentUnflushedHardwareSteps()
    val candidateSteps = (hcFitSteps + unflushedSteps).coerceAtLeast(0)
    liveCombinedSteps = if (allowStepDecrease) {
      candidateSteps
    } else {
      max(liveCombinedSteps, candidateSteps)
    }

    val effectiveUnflushedForDist = (liveCombinedSteps - hcFitSteps).coerceAtLeast(0)
    val candidateDistMeters = (hcFitDistanceMeters + effectiveUnflushedForDist * effectiveMetersPerStep()).coerceAtLeast(0f)
    liveCombinedDistanceMeters = if (allowStepDecrease) {
      candidateDistMeters
    } else {
      max(liveCombinedDistanceMeters, candidateDistMeters)
    }

    _syncState.value = _syncState.value.copy(
      liveTodaySteps = liveCombinedSteps,
      healthConnectSteps = hcFitSteps,
      hardwareSensorSteps = liveCombinedSteps,
      isActivelyWalking = activelyWalking,
      liveDistanceMeters = liveCombinedDistanceMeters,
      lastStepDetectedEpochMillis = if (activelyWalking) nowEpochMs else _syncState.value.lastStepDetectedEpochMillis,
      lastSyncedEpochMillis = nowEpochMs,
    )

    persistJob?.cancel()
    persistJob = ioScope.launch {
      delay(120L)
      BioSyncLocalStore.saveStepTelemetrySnapshot(
        context = appContext,
        todaySyncedSteps = liveCombinedSteps,
        todayDistanceMeters = liveCombinedDistanceMeters,
        lastStepSyncEpochMillis = nowEpochMs,
        stepSensorDayKey = inMemoryDayKey,
        stepSensorBaselineCounter = hcAnchorRawCounter,
        stepSensorLastRawCounter = lastRawCounter,
        lastHealthConnectWrittenSteps = hcFitSteps,
      )
    }
  }

  /**
   * Processes an authoritative hardware `Sensor.TYPE_STEP_COUNTER` reading (cumulative steps since boot).
   * Even when Google Fit is closed, any increase in `rawTotalSinceBoot` beyond `hcAnchorRawCounter`
   * immediately updates BioSync's live step and distance count!
   */
  private fun onHardwareStepCounterReading(
    appContext: Context,
    rawTotalSinceBoot: Float,
    nowEpochMs: Long,
    nowElapsedMs: Long,
  ) {
    if (rawTotalSinceBoot < 0f) return
    val todayKey = currentDayKey()
    ensureMidnightRollover(todayKey)

    // Case 1: Very first hardware counter reading of the day
    if (hcAnchorRawCounter < 0f || lastRawCounter < 0f) {
      val existingUnflushed = (liveCombinedSteps - hcFitSteps).coerceAtLeast(0)
      hcAnchorRawCounter = (rawTotalSinceBoot - existingUnflushed.toFloat()).coerceAtLeast(0f)
      lastRawCounter = rawTotalSinceBoot
      pendingDetectorSteps = 0
      recalculateAndEmitLiveTelemetry(
        appContext = appContext,
        nowEpochMs = nowEpochMs,
        activelyWalking = _syncState.value.isActivelyWalking,
      )
      return
    }

    // Case 2: Device rebooted mid-day (rawTotalSinceBoot reset below lastRawCounter)
    if (rawTotalSinceBoot < lastRawCounter) {
      val preservedUnflushed = (liveCombinedSteps - hcFitSteps).coerceAtLeast(0)
      hcAnchorRawCounter = (rawTotalSinceBoot - preservedUnflushed.toFloat()).coerceAtLeast(0f)
      lastRawCounter = rawTotalSinceBoot
      pendingDetectorSteps = 0
      recalculateAndEmitLiveTelemetry(
        appContext = appContext,
        nowEpochMs = nowEpochMs,
        activelyWalking = _syncState.value.isActivelyWalking,
      )
      return
    }

    // Case 3: Normal monotonic hardware step counter reading
    val stepDiff = (rawTotalSinceBoot - lastRawCounter).roundToInt().coerceAtLeast(0)
    lastRawCounter = rawTotalSinceBoot
    if (hcAnchorRawCounter > lastRawCounter) {
      hcAnchorRawCounter = lastRawCounter
    }

    // Absorb any temporary step-detector ticks now that the hardware counter has reported
    if (stepDiff > 0) {
      pendingDetectorSteps = (pendingDetectorSteps - stepDiff).coerceAtLeast(0)
      markWalkingAndTriggerHealthConnectSync(
        appContext = appContext,
        nowEpochMs = nowEpochMs,
        nowElapsedMs = nowElapsedMs,
      )
    }

    recalculateAndEmitLiveTelemetry(
      appContext = appContext,
      nowEpochMs = nowEpochMs,
      activelyWalking = stepDiff > 0 || _syncState.value.isActivelyWalking,
    )
  }

  /**
   * Processes an instant footstep event from `Sensor.TYPE_STEP_DETECTOR` so the UI ticks up
   * step-by-step with zero batching delay while the user is walking.
   */
  private fun onHardwareStepDetectorTick(
    appContext: Context,
    nowEpochMs: Long,
    nowElapsedMs: Long,
  ) {
    val todayKey = currentDayKey()
    ensureMidnightRollover(todayKey)
    pendingDetectorSteps = (pendingDetectorSteps + 1).coerceAtMost(250)

    markWalkingAndTriggerHealthConnectSync(
      appContext = appContext,
      nowEpochMs = nowEpochMs,
      nowElapsedMs = nowElapsedMs,
    )

    recalculateAndEmitLiveTelemetry(
      appContext = appContext,
      nowEpochMs = nowEpochMs,
      activelyWalking = true,
    )
  }

  private fun markWalkingAndTriggerHealthConnectSync(
    appContext: Context,
    nowEpochMs: Long,
    nowElapsedMs: Long,
  ) {
    lastStepRealtimeMs = nowElapsedMs
    walkingDecayJob?.cancel()
    walkingDecayJob = scope.launch {
      delay(2_800L)
      if (SystemClock.elapsedRealtime() - lastStepRealtimeMs >= 2_700L) {
        _syncState.value = _syncState.value.copy(isActivelyWalking = false)
      }
    }

    if (nowElapsedMs - lastImmediateHcTriggerMs >= 400L) {
      lastImmediateHcTriggerMs = nowElapsedMs
      immediateHcReadJob?.cancel()
      immediateHcReadJob = ioScope.launch {
        syncWithHealthConnect(appContext)
      }
    }
  }

  /**
   * Registers a one-shot probe listener on `Sensor.TYPE_STEP_COUNTER`.
   * Because `TYPE_STEP_COUNTER` is an `ON_CHANGE` sensor, Android immediately dispatches the latest
   * kernel pedometer count upon `registerListener()`—even if the user walked while the app was closed
   * and is now standing still without opening Google Fit!
   */
  fun probeHardwareStepCounterNow(context: Context) {
    val appContext = context.applicationContext
    if (!hasActivityRecognitionPermission(appContext)) return
    val nowElapsed = SystemClock.elapsedRealtime()
    if (isProbeInFlight || (nowElapsed - lastHardwareProbeElapsedMs < 350L)) return

    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    val stepCounterSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

    isProbeInFlight = true
    lastHardwareProbeElapsedMs = nowElapsed

    mainHandler.post {
      val probeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          isProbeInFlight = false
          runCatching { sm.unregisterListener(this) }
          val rawVal = event?.values?.firstOrNull() ?: return
          onHardwareStepCounterReading(
            appContext = appContext,
            rawTotalSinceBoot = rawVal,
            nowEpochMs = System.currentTimeMillis(),
            nowElapsedMs = SystemClock.elapsedRealtime(),
          )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

      val ok = runCatching {
        sm.registerListener(probeListener, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST)
      }.getOrDefault(false)

      if (!ok) {
        isProbeInFlight = false
      } else {
        // Safety timeout to unregister the one-shot probe if the sensor doesn't emit within 800ms
        mainHandler.postDelayed(
          {
            if (isProbeInFlight) {
              isProbeInFlight = false
              runCatching { sm.unregisterListener(probeListener) }
            }
          },
          800L,
        )
      }
    }
  }

  /**
   * Listens to BOTH `Sensor.TYPE_STEP_COUNTER` (authoritative hardware cumulative step counter)
   * and `Sensor.TYPE_STEP_DETECTOR` (instant per-step tick) so BioSync updates live on every step
   * without requiring the user to open the Google Fit app.
   */
  fun startLiveStepSensorTracking(context: Context) {
    val appContext = context.applicationContext
    if (!hasActivityRecognitionPermission(appContext)) {
      _syncState.value = _syncState.value.copy(
        hasActivityRecognitionPermission = false,
        isLiveSensorListening = false,
      )
      return
    }

    if (!inMemoryInitialized) {
      initializeFromLocalStore(appContext, _syncState.value.decidedDailyStepsGoal)
    }

    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    sensorManager = sm

    if (activeStepCounterListener != null || activeStepDetectorListener != null) {
      activeStepCounterListener?.let { runCatching { sm.flush(it) } }
      probeHardwareStepCounterNow(appContext)
      _syncState.value = _syncState.value.copy(
        hasActivityRecognitionPermission = true,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
        isLiveSensorListening = true,
      )
      return
    }

    val stepCounterSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    val stepDetectorSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    if (stepCounterSensor == null && stepDetectorSensor == null) {
      _syncState.value = _syncState.value.copy(
        hasActivityRecognitionPermission = true,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
        hasHardwareStepSensor = false,
        isLiveSensorListening = false,
      )
      return
    }

    var anyRegistered = false

    if (stepCounterSensor != null) {
      val counterListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          val rawVal = event?.values?.firstOrNull() ?: return
          onHardwareStepCounterReading(
            appContext = appContext,
            rawTotalSinceBoot = rawVal,
            nowEpochMs = System.currentTimeMillis(),
            nowElapsedMs = SystemClock.elapsedRealtime(),
          )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }
      val regCounter = runCatching {
        sm.registerListener(counterListener, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST, 0)
      }.getOrDefault(false)
      if (regCounter) {
        activeStepCounterListener = counterListener
        runCatching { sm.flush(counterListener) }
        anyRegistered = true
      }
    }

    if (stepDetectorSensor != null) {
      val detectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            onHardwareStepDetectorTick(
              appContext = appContext,
              nowEpochMs = System.currentTimeMillis(),
              nowElapsedMs = SystemClock.elapsedRealtime(),
            )
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }
      val regDetector = runCatching {
        sm.registerListener(detectorListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST, 0)
      }.getOrDefault(false)
      if (regDetector) {
        activeStepDetectorListener = detectorListener
        anyRegistered = true
      }
    }

    _syncState.value = _syncState.value.copy(
      hasActivityRecognitionPermission = true,
      isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
      hasHardwareStepSensor = true,
      isLiveSensorListening = anyRegistered,
    )
  }

  fun flushHardwareStepSensorNow(context: Context? = null) {
    activeStepCounterListener?.let { listener ->
      runCatching { sensorManager?.flush(listener) }
    }
    activeStepDetectorListener?.let { listener ->
      runCatching { sensorManager?.flush(listener) }
    }
    if (context != null) {
      probeHardwareStepCounterNow(context)
    }
  }

  fun stopLiveStepSensorTracking() {
    activeStepCounterListener?.let { listener ->
      runCatching { sensorManager?.unregisterListener(listener) }
    }
    activeStepDetectorListener?.let { listener ->
      runCatching { sensorManager?.unregisterListener(listener) }
    }
    activeStepCounterListener = null
    activeStepDetectorListener = null
    _syncState.value = _syncState.value.copy(
      isLiveSensorListening = false,
      isActivelyWalking = false,
    )
  }

  /**
   * Computes a de-duplicated total of step counts across potentially overlapping `StepsRecord`
   * intervals from a single origin, prorating any record that started before `startOfDayMs`.
   */
  @SuppressLint("NewApi")
  private fun computeDeduplicatedStepsFromRecords(
    records: List<StepsRecord>,
    startOfDayMs: Long,
    nowMs: Long,
  ): Int {
    if (records.isEmpty()) return 0
    data class Slice(val startMs: Long, val endMs: Long, val steps: Long)
    val slices = records.mapNotNull { rec ->
      val rawStart = rec.startTime.toEpochMilli()
      val rawEnd = max(rawStart + 1L, rec.endTime.toEpochMilli())
      val clippedStart = max(rawStart, startOfDayMs)
      val clippedEnd = min(rawEnd, nowMs + 60_000L)
      if (clippedEnd <= clippedStart || rec.count <= 0L) {
        null
      } else {
        val fraction = (clippedEnd - clippedStart).toDouble() / (rawEnd - rawStart).toDouble()
        val proratedSteps = (rec.count.toDouble() * fraction.coerceIn(0.0, 1.0)).roundToInt().toLong()
        Slice(clippedStart, clippedEnd, proratedSteps)
      }
    }.sortedWith(compareBy<Slice> { it.startMs }.thenByDescending { it.endMs })

    if (slices.isEmpty()) return 0
    var totalSteps = 0L
    var currentEndMs = -1L

    for (slice in slices) {
      if (slice.endMs <= currentEndMs) continue
      if (slice.startMs >= currentEndMs) {
        totalSteps += slice.steps
        currentEndMs = slice.endMs
      } else {
        val sliceSpan = (slice.endMs - slice.startMs).coerceAtLeast(1L)
        val nonOverlapSpan = (slice.endMs - currentEndMs).coerceAtLeast(0L)
        val tailSteps = (slice.steps.toDouble() * (nonOverlapSpan.toDouble() / sliceSpan.toDouble())).roundToInt().toLong()
        totalSteps += tailSteps
        currentEndMs = slice.endMs
      }
    }
    return totalSteps.coerceIn(0L, 200_000L).toInt()
  }

  /**
   * Computes a de-duplicated total distance in meters across potentially overlapping `DistanceRecord`
   * intervals from a single origin, prorating any record that started before `startOfDayMs`.
   */
  @SuppressLint("NewApi")
  private fun computeDeduplicatedDistanceMetersFromRecords(
    records: List<DistanceRecord>,
    startOfDayMs: Long,
    nowMs: Long,
  ): Float {
    if (records.isEmpty()) return 0f
    data class DistSlice(val startMs: Long, val endMs: Long, val meters: Double)
    val slices = records.mapNotNull { rec ->
      val rawStart = rec.startTime.toEpochMilli()
      val rawEnd = max(rawStart + 1L, rec.endTime.toEpochMilli())
      val clippedStart = max(rawStart, startOfDayMs)
      val clippedEnd = min(rawEnd, nowMs + 60_000L)
      val rawMeters = rec.distance.inMeters
      if (clippedEnd <= clippedStart || rawMeters <= 0.0) {
        null
      } else {
        val fraction = (clippedEnd - clippedStart).toDouble() / (rawEnd - rawStart).toDouble()
        DistSlice(clippedStart, clippedEnd, rawMeters * fraction.coerceIn(0.0, 1.0))
      }
    }.sortedWith(compareBy<DistSlice> { it.startMs }.thenByDescending { it.endMs })

    if (slices.isEmpty()) return 0f
    var totalMeters = 0.0
    var currentEndMs = -1L

    for (slice in slices) {
      if (slice.endMs <= currentEndMs) continue
      if (slice.startMs >= currentEndMs) {
        totalMeters += slice.meters
        currentEndMs = slice.endMs
      } else {
        val sliceSpan = (slice.endMs - slice.startMs).coerceAtLeast(1L)
        val nonOverlapSpan = (slice.endMs - currentEndMs).coerceAtLeast(0L)
        totalMeters += slice.meters * (nonOverlapSpan.toDouble() / sliceSpan.toDouble())
        currentEndMs = slice.endMs
      }
    }
    return totalMeters.toFloat().coerceAtLeast(0f)
  }

  /**
   * Reconciles Google Fit's Health Connect totals (`newHcSteps`, `newHcDistMeters`) with our live
   * hardware `Sensor.TYPE_STEP_COUNTER` bridge (`hcAnchorRawCounter`, `lastRawCounter`):
   * - While Google Fit is closed, `newHcSteps` stays constant and `lastRawCounter - hcAnchorRawCounter`
   *   provides real-time step updates on every single step.
   * - When Google Fit flushes to Health Connect (`newHcSteps > hcFitSteps`), `hcAnchorRawCounter`
   *   advances by the flushed delta (or snaps to `lastRawCounter`) so there is zero double-counting.
   */
  private fun reconcileHealthConnectWithHardwareBridge(
    newHcSteps: Int,
    newHcDistMeters: Float,
  ): Boolean {
    var allowDecrease = false

    if (newHcSteps > hcFitSteps) {
      // Google Fit just flushed new steps into Health Connect!
      val flushedDelta = newHcSteps - hcFitSteps
      hcFitSteps = newHcSteps
      if (newHcDistMeters > 0f) {
        hcFitDistanceMeters = newHcDistMeters
      } else {
        hcFitDistanceMeters = newHcSteps * effectiveMetersPerStep()
      }

      if (lastRawCounter >= 0f && hcAnchorRawCounter >= 0f) {
        // Advance the hardware anchor by the exact number of steps Google Fit just flushed
        hcAnchorRawCounter = (hcAnchorRawCounter + flushedDelta.toFloat()).coerceAtMost(lastRawCounter)
        // If Google Fit's flushed total caught up with (or exceeded) our live count, or user isn't mid-stride,
        // snap the anchor directly to lastRawCounter so BioSync matches Google Fit 1:1.
        if (newHcSteps >= liveCombinedSteps || !_syncState.value.isActivelyWalking) {
          hcAnchorRawCounter = lastRawCounter
          pendingDetectorSteps = 0
          if (newHcSteps < liveCombinedSteps && !_syncState.value.isActivelyWalking) {
            allowDecrease = true
          }
        }
      } else {
        pendingDetectorSteps = 0
      }
    } else if (newHcSteps == hcFitSteps) {
      // Google Fit has not flushed new steps since our last check; keep hcAnchorRawCounter as-is
      // so unflushed hardware steps continue adding seamlessly on top of hcFitSteps.
      if (newHcDistMeters > hcFitDistanceMeters) {
        hcFitDistanceMeters = newHcDistMeters
      }
    } else if (newHcSteps > 0 && newHcSteps < hcFitSteps && !_syncState.value.isActivelyWalking) {
      // User deleted an entry in Google Fit / Health Connect
      hcFitSteps = newHcSteps
      hcFitDistanceMeters = newHcDistMeters.coerceAtLeast(0f)
      if (lastRawCounter >= 0f) {
        hcAnchorRawCounter = lastRawCounter
      }
      pendingDetectorSteps = 0
      allowDecrease = true
    }

    return allowDecrease
  }

  /**
   * Synchronizes with Health Connect AND probes the hardware `TYPE_STEP_COUNTER` sensor
   * so that steps update live whether or not the user has opened the Google Fit app.
   */
  @SuppressLint("NewApi")
  suspend fun syncWithHealthConnect(context: Context): Boolean {
    val appContext = context.applicationContext
    if (!inMemoryInitialized) {
      initializeFromLocalStore(appContext, _syncState.value.decidedDailyStepsGoal)
    }

    // Always probe the hardware step counter so any steps walked while Google Fit was closed are captured
    if (hasActivityRecognitionPermission(appContext)) {
      probeHardwareStepCounterNow(appContext)
    }

    val activityGranted = hasActivityRecognitionPermission(appContext)
    val batteryUnrestricted = isIgnoringBatteryOptimizations(appContext)
    val (fwReadGranted, fwWriteGranted) = hasFrameworkHealthStepsAllowAll(appContext)
    val hcStatus = getHealthConnectSdkStatus(appContext)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || hcStatus != HealthConnectClient.SDK_AVAILABLE) {
      val updated = _syncState.value.copy(
        isHealthConnectAvailable = false,
        isHealthConnectUpdateRequired = hcStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
        hasHealthConnectReadPermission = fwReadGranted || _syncState.value.hasHealthConnectReadPermission,
        hasHealthConnectWritePermission = fwWriteGranted || _syncState.value.hasHealthConnectWritePermission,
        hasActivityRecognitionPermission = activityGranted,
        isIgnoringBatteryOptimizations = batteryUnrestricted,
      )
      _syncState.value = updated
      recalculateAndEmitLiveTelemetry(
        appContext = appContext,
        nowEpochMs = System.currentTimeMillis(),
      )
      if (activityGranted) {
        BioSyncStepBackgroundService.startIfAuthorized(appContext)
      }
      return updated.isAllPermissionsAllowed
    }

    return hcSyncMutex.withLock {
      withContext(Dispatchers.IO) {
        runCatching {
          val client = HealthConnectClient.getOrCreate(appContext)
          val granted = client.permissionController.getGrantedPermissions()
          val readStepsPerm = HealthPermission.getReadPermission(StepsRecord::class)
          val writeStepsPerm = HealthPermission.getWritePermission(StepsRecord::class)
          val hasRead = fwReadGranted || (readStepsPerm in granted) || (PERM_READ_STEPS in granted)
          val hasWrite = fwWriteGranted || (writeStepsPerm in granted) || (PERM_WRITE_STEPS in granted)
          val hasBgRead = (HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted) ||
            (PERM_BG_HEALTH in granted) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
              ContextCompat.checkSelfPermission(appContext, PERM_BG_HEALTH) == PackageManager.PERMISSION_GRANTED)

          val todayKey = currentDayKey()
          ensureMidnightRollover(todayKey)
          val nowMs = System.currentTimeMillis()
          val selfPackage = appContext.packageName
          val googleFitOrigin = DataOrigin(GOOGLE_FIT_PACKAGE)
          val externalOrigins = mutableSetOf<String>()
          var allowDecrease = false

          if (hasRead) {
            val zoneId = ZoneId.systemDefault()
            val startOfDayInstant = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
            val startOfDayMs = startOfDayInstant.toEpochMilli()
            val nowInstant = Instant.now()
            val endQueryInstant = nowInstant.plusSeconds(60)
            val todayTimeRange = TimeRangeFilter.between(startOfDayInstant, endQueryInstant)

            if (endQueryInstant.isAfter(startOfDayInstant)) {
              if (hasWrite && !hasPurgedLegacySelfRecords) {
                runCatching {
                  client.deleteRecords(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(nowInstant.plusSeconds(86400)),
                  )
                }
                runCatching {
                  client.deleteRecords(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(nowInstant.plusSeconds(86400)),
                  )
                }
                hasPurgedLegacySelfRecords = true
              }

              val googleFitStepRecords = mutableListOf<StepsRecord>()
              val fallbackStepRecordsByOrigin = mutableMapOf<String, MutableList<StepsRecord>>()

              runCatching {
                val stepsRecords = client.readRecords(
                  ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = todayTimeRange,
                    pageSize = 5000,
                  ),
                ).records

                for (rec in stepsRecords) {
                  val pkg = rec.metadata.dataOrigin.packageName
                  if (pkg == selfPackage || pkg.isBlank()) continue
                  externalOrigins.add(pkg)
                  if (pkg == GOOGLE_FIT_PACKAGE) {
                    googleFitStepRecords.add(rec)
                  } else {
                    fallbackStepRecordsByOrigin.getOrPut(pkg) { mutableListOf() }.add(rec)
                  }
                }
              }

              val aggFitSteps = runCatching {
                val resp = client.aggregate(
                  AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = todayTimeRange,
                    dataOriginFilter = setOf(googleFitOrigin),
                  ),
                )
                (resp[StepsRecord.COUNT_TOTAL] ?: 0L).coerceIn(0L, 200_000L).toInt()
              }.getOrDefault(0)

              val dedupFitSteps = computeDeduplicatedStepsFromRecords(
                records = googleFitStepRecords,
                startOfDayMs = startOfDayMs,
                nowMs = nowMs,
              )

              val resolvedHcSteps = if (aggFitSteps > 0 || googleFitStepRecords.isNotEmpty() || GOOGLE_FIT_PACKAGE in externalOrigins) {
                if (aggFitSteps > 0) aggFitSteps else dedupFitSteps
              } else if (fallbackStepRecordsByOrigin.isNotEmpty()) {
                val primaryOrigin = fallbackStepRecordsByOrigin.entries
                  .maxByOrNull { (_, recs) -> computeDeduplicatedStepsFromRecords(recs, startOfDayMs, nowMs) }
                  ?.key
                if (primaryOrigin != null) {
                  val aggPrimarySteps = runCatching {
                    val resp = client.aggregate(
                      AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = todayTimeRange,
                        dataOriginFilter = setOf(DataOrigin(primaryOrigin)),
                      ),
                    )
                    (resp[StepsRecord.COUNT_TOTAL] ?: 0L).coerceIn(0L, 200_000L).toInt()
                  }.getOrDefault(0)
                  val dedupPrimary = computeDeduplicatedStepsFromRecords(
                    records = fallbackStepRecordsByOrigin[primaryOrigin].orEmpty(),
                    startOfDayMs = startOfDayMs,
                    nowMs = nowMs,
                  )
                  if (aggPrimarySteps > 0) aggPrimarySteps else dedupPrimary
                } else {
                  0
                }
              } else {
                0
              }

              val googleFitDistRecords = mutableListOf<DistanceRecord>()
              val fallbackDistRecordsByOrigin = mutableMapOf<String, MutableList<DistanceRecord>>()

              runCatching {
                val distRecords = client.readRecords(
                  ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = todayTimeRange,
                    pageSize = 3000,
                  ),
                ).records

                for (rec in distRecords) {
                  val pkg = rec.metadata.dataOrigin.packageName
                  if (pkg == selfPackage || pkg.isBlank()) continue
                  externalOrigins.add(pkg)
                  if (pkg == GOOGLE_FIT_PACKAGE) {
                    googleFitDistRecords.add(rec)
                  } else {
                    fallbackDistRecordsByOrigin.getOrPut(pkg) { mutableListOf() }.add(rec)
                  }
                }
              }

              val aggFitDistMeters = runCatching {
                val resp = client.aggregate(
                  AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = todayTimeRange,
                    dataOriginFilter = setOf(googleFitOrigin),
                  ),
                )
                (resp[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0).toFloat()
              }.getOrDefault(0f)

              val dedupFitDistMeters = computeDeduplicatedDistanceMetersFromRecords(
                records = googleFitDistRecords,
                startOfDayMs = startOfDayMs,
                nowMs = nowMs,
              )

              val resolvedHcDistMeters = if (aggFitDistMeters > 0f || googleFitDistRecords.isNotEmpty() || GOOGLE_FIT_PACKAGE in externalOrigins) {
                if (aggFitDistMeters > 0f) aggFitDistMeters else dedupFitDistMeters
              } else if (fallbackDistRecordsByOrigin.isNotEmpty()) {
                val primaryDistOrigin = fallbackDistRecordsByOrigin.entries
                  .maxByOrNull { (_, recs) -> computeDeduplicatedDistanceMetersFromRecords(recs, startOfDayMs, nowMs) }
                  ?.key
                if (primaryDistOrigin != null) {
                  val aggPrimaryDist = runCatching {
                    val resp = client.aggregate(
                      AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = todayTimeRange,
                        dataOriginFilter = setOf(DataOrigin(primaryDistOrigin)),
                      ),
                    )
                    (resp[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0).toFloat()
                  }.getOrDefault(0f)
                  val dedupPrimaryDist = computeDeduplicatedDistanceMetersFromRecords(
                    records = fallbackDistRecordsByOrigin[primaryDistOrigin].orEmpty(),
                    startOfDayMs = startOfDayMs,
                    nowMs = nowMs,
                  )
                  if (aggPrimaryDist > 0f) aggPrimaryDist else dedupPrimaryDist
                } else {
                  0f
                }
              } else {
                0f
              }

              allowDecrease = reconcileHealthConnectWithHardwareBridge(
                newHcSteps = resolvedHcSteps,
                newHcDistMeters = resolvedHcDistMeters,
              )
            }
          }

          _syncState.value = _syncState.value.copy(
            isHealthConnectAvailable = true,
            isHealthConnectUpdateRequired = false,
            hasHealthConnectReadPermission = hasRead,
            hasHealthConnectWritePermission = hasWrite,
            hasHealthConnectBackgroundReadPermission = hasBgRead,
            hasActivityRecognitionPermission = activityGranted,
            isIgnoringBatteryOptimizations = batteryUnrestricted,
            connectedDataOrigins = externalOrigins,
          )

          recalculateAndEmitLiveTelemetry(
            appContext = appContext,
            nowEpochMs = nowMs,
            allowStepDecrease = allowDecrease,
          )

          BioSyncLocalStore.saveStepTelemetrySnapshot(
            context = appContext,
            healthConnectPermissionsGranted = hasRead && hasWrite,
            activityRecognitionGranted = activityGranted,
            stepSensorDayKey = todayKey,
          )

          if (activityGranted) {
            BioSyncStepBackgroundService.startIfAuthorized(appContext)
          }
          _syncState.value.isAllPermissionsAllowed
        }.getOrElse {
          val fallbackAllowed = (fwReadGranted && fwWriteGranted && activityGranted && batteryUnrestricted) ||
            _syncState.value.isAllPermissionsAllowed
          fallbackAllowed
        }
      }
    }
  }

  fun markHealthConnectAllowAllGranted(context: Context) {
    val appContext = context.applicationContext
    _syncState.value = _syncState.value.copy(
      hasHealthConnectReadPermission = true,
      hasHealthConnectWritePermission = true,
      isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
    )
    ioScope.launch {
      BioSyncLocalStore.saveStepTelemetrySnapshot(
        context = appContext,
        healthConnectPermissionsGranted = true,
      )
    }
  }

  /**
   * Opens the Health Connect management screen (or Play Store provider page if an update is required)
   * so the user can select "Allow all" for BioSync in Health Connect settings.
   */
  fun openHealthConnectSettings(context: Context) {
    val status = getHealthConnectSdkStatus(context)
    if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
      runCatching {
        val uri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
        val marketIntent = Intent(Intent.ACTION_VIEW, uri).apply {
          setPackage("com.android.vending")
          putExtra("overlay", true)
          putExtra("callerId", context.packageName)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
        return
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val manageAppInserted = runCatching {
        val intent = Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
          putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
      }.getOrDefault(false)
      if (manageAppInserted) return
    }

    val settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      "android.health.connect.action.HEALTH_HOME_SETTINGS"
    } else {
      HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
    }

    runCatching {
      val intent = Intent(settingsAction).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }.onFailure {
      runCatching {
        val fallbackIntent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
      }
    }
  }
}

/**
 * Composable hook that manages:
 * - Verifying whether all required mobile permissions are granted:
 *   1. Health Connect "Allow all" (Steps, Distance, & Background Read)
 *   2. Physical Activity & live background service notifications (`ACTIVITY_RECOGNITION`, `POST_NOTIFICATIONS`)
 *   3. Unrestricted background battery execution (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
 * - Launching the complete permission sequence when the user taps "Confirm & Connect".
 * - Continuous 500ms live synchronization with Google Fit via Health Connect + Hardware `TYPE_STEP_COUNTER` bridge.
 */
@Composable
fun rememberBioSyncStepSyncController(
  decidedDailyStepsGoal: Int,
  autoRequestPermissionsOnStart: Boolean = false,
): BioSyncStepSyncController {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val syncState by BioSyncHealthStepManager.syncState.collectAsState()

  var pendingResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
  var hcLaunchTimestampMs by remember { mutableStateOf(0L) }
  var awaitingSettingsReturn by remember { mutableStateOf(false) }
  var hasAutoRequestedThisSession by remember { mutableStateOf(false) }
  val latestGoal by rememberUpdatedState(decidedDailyStepsGoal)

  // Step 3 in chain: Unrestricted Battery Optimization Exemption Launcher
  val batteryOptimizationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { _ ->
    coroutineScope.launch {
      val allAllowed = BioSyncHealthStepManager.syncWithHealthConnect(context)
      if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
        BioSyncHealthStepManager.startLiveStepSensorTracking(context)
        BioSyncStepBackgroundService.startIfAuthorized(context)
      }
      pendingResultCallback?.invoke(allAllowed)
      pendingResultCallback = null
    }
  }

  val launchBatteryExemptionOrFinish: () -> Unit = {
    coroutineScope.launch {
      val allAllowedNow = BioSyncHealthStepManager.syncWithHealthConnect(context)
      val batteryOk = BioSyncHealthStepManager.isIgnoringBatteryOptimizations(context)
      if (!batteryOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        runCatching {
          val intent = BioSyncHealthStepManager.createIgnoreBatteryOptimizationIntent(context)
          batteryOptimizationLauncher.launch(intent)
        }.onFailure {
          awaitingSettingsReturn = true
          BioSyncHealthStepManager.openBatteryOptimizationSettings(context)
        }
      } else {
        if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
          BioSyncStepBackgroundService.startIfAuthorized(context)
        }
        pendingResultCallback?.invoke(allAllowedNow)
        pendingResultCallback = null
      }
    }
  }

  // Step 2 in chain: Health Connect Permission Launcher
  val healthPermissionContract = remember {
    PermissionController.createRequestPermissionResultContract()
  }
  val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
    contract = healthPermissionContract,
  ) { grantedSet ->
    val elapsedMs = SystemClock.elapsedRealtime() - hcLaunchTimestampMs
    coroutineScope.launch {
      val basePerms = BioSyncHealthStepManager.baseHealthPermissions
      val grantedAllFromCallback = (basePerms.isNotEmpty() && grantedSet.containsAll(basePerms)) ||
        (grantedSet.any { it.contains("READ_STEPS") } && grantedSet.any { it.contains("WRITE_STEPS") })
      if (grantedAllFromCallback) {
        BioSyncHealthStepManager.markHealthConnectAllowAllGranted(context)
      }
      BioSyncHealthStepManager.syncWithHealthConnect(context)
      if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
        BioSyncHealthStepManager.startLiveStepSensorTracking(context)
      }
      val hcDone = BioSyncHealthStepManager.syncState.value.isHealthConnectAllowAllGranted || grantedAllFromCallback
      if (hcDone) {
        BioSyncHealthStepManager.markHealthConnectAllowAllGranted(context)
        launchBatteryExemptionOrFinish()
      } else if (elapsedMs < 320L) {
        awaitingSettingsReturn = true
        BioSyncHealthStepManager.openHealthConnectSettings(context)
      } else {
        launchBatteryExemptionOrFinish()
      }
    }
  }

  val launchHealthConnectOrNextStep: () -> Unit = {
    val hcStatus = BioSyncHealthStepManager.getHealthConnectSdkStatus(context)
    val perms = BioSyncHealthStepManager.buildRequestedHealthPermissions(context)
    val alreadyHcAllowed = BioSyncHealthStepManager.syncState.value.isHealthConnectAllowAllGranted
    if (alreadyHcAllowed) {
      launchBatteryExemptionOrFinish()
    } else if (hcStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
      awaitingSettingsReturn = true
      BioSyncHealthStepManager.openHealthConnectSettings(context)
    } else if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      hcStatus == HealthConnectClient.SDK_AVAILABLE &&
      perms.isNotEmpty()
    ) {
      hcLaunchTimestampMs = SystemClock.elapsedRealtime()
      runCatching {
        healthConnectPermissionLauncher.launch(perms)
      }.onFailure {
        awaitingSettingsReturn = true
        BioSyncHealthStepManager.openHealthConnectSettings(context)
      }
    } else {
      launchBatteryExemptionOrFinish()
    }
  }

  // Step 1 in chain: Android Runtime Permissions (`ACTIVITY_RECOGNITION` + `POST_NOTIFICATIONS` on Android 13+)
  val runtimePermissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { results ->
    val activityGranted = results[Manifest.permission.ACTIVITY_RECOGNITION]
      ?: BioSyncHealthStepManager.hasActivityRecognitionPermission(context)
    BioSyncLocalStore.saveStepTelemetrySnapshot(
      context = context,
      activityRecognitionGranted = activityGranted,
    )
    if (activityGranted) {
      BioSyncHealthStepManager.startLiveStepSensorTracking(context)
      BioSyncStepBackgroundService.startIfAuthorized(context)
    }
    launchHealthConnectOrNextStep()
  }

  val requestAllowAllPermissions: (onResult: (Boolean) -> Unit) -> Unit = remember(context) {
    { onResult ->
      pendingResultCallback = onResult
      BioSyncHealthStepManager.updateDecidedDailyStepsGoal(context, latestGoal)

      val runtimePermsToRequest = mutableListOf<String>()
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !BioSyncHealthStepManager.hasActivityRecognitionPermission(context)
      ) {
        runtimePermsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
      }
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        runtimePermsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }

      if (runtimePermsToRequest.isNotEmpty()) {
        runCatching {
          runtimePermissionsLauncher.launch(runtimePermsToRequest.toTypedArray())
        }.onFailure {
          launchHealthConnectOrNextStep()
        }
      } else {
        BioSyncHealthStepManager.startLiveStepSensorTracking(context)
        BioSyncStepBackgroundService.startIfAuthorized(context)
        launchHealthConnectOrNextStep()
      }
    }
  }

  // Keep decided daily steps goal synchronized in storage whenever the user adjusts or switches modes
  LaunchedEffect(decidedDailyStepsGoal) {
    BioSyncHealthStepManager.initializeFromLocalStore(context, decidedDailyStepsGoal)
    BioSyncHealthStepManager.updateDecidedDailyStepsGoal(context, decidedDailyStepsGoal)
  }

  // Observe lifecycle to attach/probe hardware step sensors immediately and re-verify permissions on resume
  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
            BioSyncHealthStepManager.startLiveStepSensorTracking(context)
            BioSyncHealthStepManager.flushHardwareStepSensorNow(context)
            BioSyncStepBackgroundService.startIfAuthorized(context)
          }
          coroutineScope.launch {
            val allAllowed = BioSyncHealthStepManager.syncWithHealthConnect(context)
            if (awaitingSettingsReturn) {
              awaitingSettingsReturn = false
              if (!BioSyncHealthStepManager.isIgnoringBatteryOptimizations(context)) {
                launchBatteryExemptionOrFinish()
              } else {
                pendingResultCallback?.invoke(allAllowed)
                pendingResultCallback = null
              }
            }
          }
        }
        Lifecycle.Event.ON_DESTROY -> {
          if (!BioSyncHealthStepManager.isIgnoringBatteryOptimizations(context)) {
            BioSyncHealthStepManager.stopLiveStepSensorTracking()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // High-frequency 500ms live sync loop so Steps & Distance update without needing to open Google Fit
  LaunchedEffect(context) {
    val allAllowed = BioSyncHealthStepManager.syncWithHealthConnect(context)
    if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
      BioSyncHealthStepManager.startLiveStepSensorTracking(context)
      BioSyncStepBackgroundService.startIfAuthorized(context)
    }
    if (autoRequestPermissionsOnStart && !hasAutoRequestedThisSession && !allAllowed) {
      hasAutoRequestedThisSession = true
      requestAllowAllPermissions {}
    }
    while (isActive) {
      delay(500L)
      BioSyncHealthStepManager.flushHardwareStepSensorNow(context)
      BioSyncHealthStepManager.syncWithHealthConnect(context)
    }
  }

  return remember(syncState, requestAllowAllPermissions) {
    BioSyncStepSyncController(
      state = syncState,
      verifyAllPermissionsAllowed = {
        BioSyncHealthStepManager.syncWithHealthConnect(context)
      },
      requestAllowAllPermissions = requestAllowAllPermissions,
      requestBatteryOptimizationExemption = {
        launchBatteryExemptionOrFinish()
      },
      forceRefreshNow = {
        coroutineScope.launch {
          BioSyncHealthStepManager.flushHardwareStepSensorNow(context)
          BioSyncHealthStepManager.syncWithHealthConnect(context)
          if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
            BioSyncHealthStepManager.startLiveStepSensorTracking(context)
            BioSyncStepBackgroundService.startIfAuthorized(context)
          }
        }
      },
      openHealthConnectAppOrSettings = {
        awaitingSettingsReturn = true
        BioSyncHealthStepManager.openHealthConnectSettings(context)
      },
    )
  }
}
