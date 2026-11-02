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
import android.util.Log
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
 * Pure, deterministic reconciliation state between authoritative Google Fit / Health Connect
 * records and real-time hardware pedometer deltas (`TYPE_STEP_COUNTER` + `TYPE_STEP_DETECTOR`).
 */
data class StepReconciliationSnapshot(
  val dayKey: String = "",
  val syncedHealthConnectSteps: Int = 0,
  val syncedHealthConnectDistanceMeters: Float = 0f,
  val lastHcRecordEndEpochMs: Long = 0L,
  val sensorBaselineAtSync: Float = -1f,
  val lastSeenRawCounter: Float = -1f,
  val localStepsSinceSync: Int = 0,
  val pendingDetectorSteps: Int = 0,
) {
  val liveSensorBridgeSteps: Int
    get() = (localStepsSinceSync + pendingDetectorSteps).coerceAtLeast(0)

  val displayedSteps: Int
    get() = (syncedHealthConnectSteps + liveSensorBridgeSteps).coerceAtLeast(syncedHealthConnectSteps)
}

object GoogleFitStepReconciler {
  /**
   * Restores state from `BioSyncStoredState` without ever overwriting `syncedHealthConnectSteps`
   * with the combined `todaySyncedSteps` (preventing double-counting across screen transitions
   * or background service restarts).
   */
  fun onInitializeFromStorage(
    inMemory: StepReconciliationSnapshot,
    stored: BioSyncStoredState,
    today: String,
  ): StepReconciliationSnapshot {
    // If already initialized in memory for today with active state, preserve in-memory state
    if (
      inMemory.dayKey == today &&
      (inMemory.syncedHealthConnectSteps > 0 || inMemory.localStepsSinceSync > 0 || inMemory.sensorBaselineAtSync > 0f)
    ) {
      return inMemory
    }

    if (stored.stepSensorDayKey != today) {
      return StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
        sensorBaselineAtSync = -1f,
        lastSeenRawCounter = -1f,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    }

    val pureHcSteps = stored.lastHealthConnectWrittenSteps.coerceAtLeast(0)
    val storedBaseline = stored.stepSensorBaselineCounter
    val storedLastRaw = stored.stepSensorLastRawCounter

    return if (pureHcSteps > 0 || stored.todaySyncedSteps == 0) {
      val localDelta = if (storedBaseline > 0f && storedLastRaw >= storedBaseline) {
        (storedLastRaw - storedBaseline).toInt().coerceAtLeast(0)
      } else {
        0
      }
      StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = pureHcSteps,
        syncedHealthConnectDistanceMeters = stored.todayDistanceMeters.coerceAtLeast(0f),
        lastHcRecordEndEpochMs = stored.lastHcRecordEndEpochMillis.coerceAtLeast(0L),
        sensorBaselineAtSync = storedBaseline,
        lastSeenRawCounter = storedLastRaw,
        localStepsSinceSync = localDelta,
        pendingDetectorSteps = 0,
      )
    } else {
      // Legacy migration fallback when pureHcSteps was 0 but todaySyncedSteps > 0:
      // Anchor baseline to storedLastRaw so localStepsSinceSync is 0 and never double-counted.
      StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = stored.todaySyncedSteps.coerceAtLeast(0),
        syncedHealthConnectDistanceMeters = stored.todayDistanceMeters.coerceAtLeast(0f),
        lastHcRecordEndEpochMs = stored.lastHcRecordEndEpochMillis.coerceAtLeast(0L),
        sensorBaselineAtSync = if (storedLastRaw > 0f) storedLastRaw else storedBaseline,
        lastSeenRawCounter = storedLastRaw,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    }
  }

  /**
   * Handles cumulative hardware `Sensor.TYPE_STEP_COUNTER` events.
   * Reconciles any temporary `pendingDetectorSteps` into the authoritative hardware counter delta.
   */
  fun onHardwareStepCounterEvent(
    current: StepReconciliationSnapshot,
    rawCounter: Float,
    today: String,
  ): StepReconciliationSnapshot {
    if (rawCounter <= 0f) return current

    if (current.dayKey != today) {
      return StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
        sensorBaselineAtSync = rawCounter,
        lastSeenRawCounter = rawCounter,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    }

    if (current.sensorBaselineAtSync <= 0f) {
      return current.copy(
        sensorBaselineAtSync = rawCounter,
        lastSeenRawCounter = rawCounter,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    }

    return if (rawCounter >= current.sensorBaselineAtSync) {
      val delta = (rawCounter - current.sensorBaselineAtSync).toInt().coerceAtLeast(0)
      current.copy(
        lastSeenRawCounter = rawCounter,
        localStepsSinceSync = delta,
        pendingDetectorSteps = 0,
      )
    } else {
      // Hardware counter reset (device rebooted mid-day): preserve un-synced delta smoothly
      val preservedDelta = current.localStepsSinceSync.coerceAtLeast(0)
      val adjustedBaseline = (rawCounter - preservedDelta).coerceAtLeast(0f)
      current.copy(
        sensorBaselineAtSync = adjustedBaseline,
        lastSeenRawCounter = rawCounter,
        localStepsSinceSync = (rawCounter - adjustedBaseline).toInt().coerceAtLeast(0),
        pendingDetectorSteps = 0,
      )
    }
  }

  /**
   * Handles real-time `Sensor.TYPE_STEP_DETECTOR` footstep events for instant step-by-step UI
   * responsiveness before the hardware `TYPE_STEP_COUNTER` FIFO flushes.
   */
  fun onHardwareStepDetectorEvent(
    current: StepReconciliationSnapshot,
    stepsDetected: Int,
    today: String,
  ): StepReconciliationSnapshot {
    val safeSteps = stepsDetected.coerceAtLeast(1)
    if (current.dayKey != today) {
      return StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
        sensorBaselineAtSync = -1f,
        lastSeenRawCounter = -1f,
        localStepsSinceSync = 0,
        pendingDetectorSteps = safeSteps,
      )
    }
    return current.copy(
      pendingDetectorSteps = (current.pendingDetectorSteps + safeSteps).coerceAtMost(200),
    )
  }

  /**
   * Reconciles with Google Fit / Health Connect authoritative data:
   * - Never overwrites Google Fit / Health Connect values.
   * - When Google Fit / Health Connect reports new or updated step totals (`hcUpdated`), or when
   *   walking has settled (`!isActivelyWalkingNow` and `hcSteps > 0`), strictly snaps to `hcSteps`
   *   and re-anchors the hardware sensor baseline to `lastSeenRawCounter` with `localStepsSinceSync = 0`.
   * - While actively walking before Google Fit has flushed to Health Connect (`!hcUpdated` and
   *   `isActivelyWalkingNow`), preserves `localStepsSinceSync` so live walking steps never get erased.
   */
  fun onHealthConnectSync(
    current: StepReconciliationSnapshot,
    hcSteps: Int,
    hcDistanceMeters: Float?,
    hcLatestRecordEndEpochMs: Long,
    today: String,
    strideMeters: Float,
    force: Boolean,
    isActivelyWalkingNow: Boolean,
  ): StepReconciliationSnapshot {
    val safeHcSteps = hcSteps.coerceAtLeast(0)
    val baseState = if (current.dayKey != today) {
      StepReconciliationSnapshot(
        dayKey = today,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
        sensorBaselineAtSync = current.lastSeenRawCounter,
        lastSeenRawCounter = current.lastSeenRawCounter,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    } else {
      current
    }

    if (safeHcSteps > 0) {
      val hcUpdated = (safeHcSteps != baseState.syncedHealthConnectSteps) ||
        (hcLatestRecordEndEpochMs > baseState.lastHcRecordEndEpochMs && hcLatestRecordEndEpochMs > 0L)

      val shouldSnapStrictlyToHc = hcUpdated || !isActivelyWalkingNow || force
      val resolvedDist = if (hcDistanceMeters != null && hcDistanceMeters > 0f) {
        hcDistanceMeters
      } else {
        safeHcSteps * strideMeters
      }

      return if (shouldSnapStrictlyToHc) {
        baseState.copy(
          dayKey = today,
          syncedHealthConnectSteps = safeHcSteps,
          syncedHealthConnectDistanceMeters = resolvedDist,
          lastHcRecordEndEpochMs = maxOf(baseState.lastHcRecordEndEpochMs, hcLatestRecordEndEpochMs),
          sensorBaselineAtSync = if (baseState.lastSeenRawCounter > 0f) baseState.lastSeenRawCounter else -1f,
          localStepsSinceSync = 0,
          pendingDetectorSteps = 0,
        )
      } else {
        // Actively walking while Google Fit hasn't written the new batch to Health Connect yet:
        // keep syncedHealthConnectSteps and preserve the live walking delta until Google Fit flushes.
        baseState.copy(
          dayKey = today,
          syncedHealthConnectSteps = safeHcSteps,
          syncedHealthConnectDistanceMeters = resolvedDist,
          lastHcRecordEndEpochMs = maxOf(baseState.lastHcRecordEndEpochMs, hcLatestRecordEndEpochMs),
        )
      }
    } else {
      // Health Connect reports 0 steps for today
      return baseState.copy(
        dayKey = today,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
      )
    }
  }
}

/**
 * Immutable snapshot of telemetry synchronized with Google Fit & Android Hardware Step Counter:
 * - Live Total Steps (`liveTodaySteps`)
 * - Total Distance (`liveDistanceMeters`, `liveDistanceKm`)
 * - Sync and permissions state
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
      hasHealthConnectReadPermission
    } else {
      hasHealthConnectReadPermission || !isHealthConnectUpdateRequired
    }

  val isAllPermissionsAllowed: Boolean
    get() = isHealthConnectAllowAllGranted &&
      hasActivityRecognitionPermission &&
      isIgnoringBatteryOptimizations
}

/**
 * Controller handle exposed to Compose screens.
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
 * Live Google Fit & Health Connect Continuous Step Synchronization Engine:
 *
 * 1. Authoritative Google Fit & Health Connect Sync (`GoogleFitStepReconciler`):
 *    Strictly follows Google Fit (`com.google.android.apps.fitness`) and Health Connect step totals
 *    without ever overwriting or double-counting.
 * 2. Real-Time Walking Bridge (`TYPE_STEP_COUNTER` + `TYPE_STEP_DETECTOR`):
 *    Tracks live footsteps while walking and seamlessly snaps to Google Fit's authoritative count
 *    as soon as Google Fit flushes to Health Connect.
 * 3. Strictly READ-ONLY Health Connect:
 *    BioSync NEVER calls insertRecords or deleteRecords on Health Connect, so Google Fit data is 100% preserved.
 */
object BioSyncHealthStepManager {
  private const val TAG = "BioSyncHealthStepMgr"
  const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
  const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"
  private const val PERM_READ_STEPS = "android.permission.health.READ_STEPS"
  private const val PERM_READ_DISTANCE = "android.permission.health.READ_DISTANCE"
  private const val ACTIVE_WALKING_WINDOW_MS = 45_000L

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncMutex = Mutex()
  private val stateLock = Any()
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _syncState = MutableStateFlow(BioSyncStepSyncState())
  val syncState: StateFlow<BioSyncStepSyncState> = _syncState.asStateFlow()

  @Volatile private var cachedUserHeightCm: Float = 172f
  @Volatile private var activeAppContext: Context? = null
  @Volatile private var isSensorRegistered = false
  @Volatile private var lastSyncAttemptElapsedMs: Long = 0L
  @Volatile private var lastSurfacePushElapsedMs: Long = 0L
  @Volatile private var postWalkSyncJob: Job? = null
  @Volatile private var walkingStatusResetJob: Job? = null

  @Volatile private var reconciliationState = StepReconciliationSnapshot()

  @get:SuppressLint("NewApi")
  val baseHealthPermissions: Set<String> by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
      )
    } else {
      emptySet()
    }
  }

  @SuppressLint("NewApi")
  fun buildRequestedHealthPermissions(context: Context): Set<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptySet()
    val perms = mutableSetOf(
      HealthPermission.getReadPermission(StepsRecord::class),
      HealthPermission.getReadPermission(DistanceRecord::class),
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
    return pm.isIgnoringBatteryOptimizations(context.packageName)
  }

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

  fun isHealthConnectAvailable(context: Context): Boolean {
    return getHealthConnectSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
  }

  fun openHealthConnectSettings(context: Context) {
    val directAction = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
    val intent = Intent(directAction).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      context.startActivity(intent)
    }.onFailure {
      runCatching {
        val playStoreIntent = Intent(
          Intent.ACTION_VIEW,
          Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE"),
        ).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(playStoreIntent)
      }
    }
  }

  fun markHealthConnectAllowAllGranted(context: Context) {
    BioSyncLocalStore.saveStepTelemetrySnapshot(
      context = context,
      healthConnectPermissionsGranted = true,
    )
    _syncState.value = _syncState.value.copy(
      hasHealthConnectReadPermission = true,
      hasHealthConnectWritePermission = false,
    )
  }

  fun updateDecidedDailyStepsGoal(context: Context, goal: Int) {
    val safeGoal = goal.coerceAtLeast(1000)
    BioSyncLocalStore.saveStepTelemetrySnapshot(
      context = context,
      decidedDailyStepsGoal = safeGoal,
    )
    _syncState.value = _syncState.value.copy(decidedDailyStepsGoal = safeGoal)
  }

  private fun schedulePostWalkHealthConnectSync(appContext: Context) {
    postWalkSyncJob?.cancel()
    postWalkSyncJob = scope.launch {
      delay(3_500L)
      runCatching {
        syncWithHealthConnect(appContext, force = false)
      }
      delay(8_000L)
      runCatching {
        syncWithHealthConnect(appContext, force = false)
      }
    }
    walkingStatusResetJob?.cancel()
    walkingStatusResetJob = scope.launch {
      delay(ACTIVE_WALKING_WINDOW_MS)
      _syncState.value = _syncState.value.copy(isActivelyWalking = false)
      runCatching {
        syncWithHealthConnect(appContext, force = false)
      }
    }
  }

  private fun notifyExternalSurfacesIfNeeded(
    appContext: Context,
    displayedSteps: Int,
    forceImmediate: Boolean,
  ) {
    val nowElapsed = SystemClock.elapsedRealtime()
    if (!forceImmediate && (nowElapsed - lastSurfacePushElapsedMs) < 1_500L) {
      return
    }
    lastSurfacePushElapsedMs = nowElapsed
    scope.launch {
      runCatching {
        DashboardLocalStore.syncLiveStepsInBackground(appContext, displayedSteps)
        BioSyncWidgetAndNotificationEngine.refreshAllWidgets(appContext)
        BioSyncWidgetAndNotificationEngine.refreshLiveTelemetryNotificationIfActive(appContext)
      }
    }
  }

  /**
   * Hardware step sensor event listener (`TYPE_STEP_COUNTER` + `TYPE_STEP_DETECTOR`).
   * Tracks incremental live steps while walking and reconciles with Google Fit / Health Connect.
   */
  private val stepSensorListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
      val sensorType = event?.sensor?.type ?: return
      if (sensorType != Sensor.TYPE_STEP_COUNTER && sensorType != Sensor.TYPE_STEP_DETECTOR) return
      val appContext = activeAppContext ?: return
      val today = currentDayKey()
      val nowMs = System.currentTimeMillis()

      var shouldPushSurfaces = false
      var latestDisplayedSteps = 0

      synchronized(stateLock) {
        val prevSteps = reconciliationState.displayedSteps
        reconciliationState = when (sensorType) {
          Sensor.TYPE_STEP_COUNTER -> {
            val raw = event.values.firstOrNull() ?: return
            if (raw <= 0f) return
            GoogleFitStepReconciler.onHardwareStepCounterEvent(
              current = reconciliationState,
              rawCounter = raw,
              today = today,
            )
          }
          Sensor.TYPE_STEP_DETECTOR -> {
            val count = event.values.firstOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
            GoogleFitStepReconciler.onHardwareStepDetectorEvent(
              current = reconciliationState,
              stepsDetected = count,
              today = today,
            )
          }
          else -> reconciliationState
        }

        val snap = reconciliationState
        val displayedSteps = snap.displayedSteps
        latestDisplayedSteps = displayedSteps
        shouldPushSurfaces = displayedSteps != prevSteps
        val strideMeters = (cachedUserHeightCm * 0.415f) / 100f
        val displayedDistMeters = if (snap.syncedHealthConnectDistanceMeters > 0f) {
          snap.syncedHealthConnectDistanceMeters + (snap.liveSensorBridgeSteps * strideMeters)
        } else {
          displayedSteps * strideMeters
        }

        BioSyncLocalStore.saveStepTelemetrySnapshot(
          context = appContext,
          todaySyncedSteps = displayedSteps,
          todayDistanceMeters = displayedDistMeters,
          stepSensorDayKey = today,
          stepSensorBaselineCounter = snap.sensorBaselineAtSync,
          stepSensorLastRawCounter = snap.lastSeenRawCounter,
          lastHealthConnectWrittenSteps = snap.syncedHealthConnectSteps,
          lastHcRecordEndEpochMillis = snap.lastHcRecordEndEpochMs,
        )

        _syncState.value = _syncState.value.copy(
          liveTodaySteps = displayedSteps,
          healthConnectSteps = snap.syncedHealthConnectSteps,
          hardwareSensorSteps = snap.liveSensorBridgeSteps,
          liveDistanceMeters = displayedDistMeters,
          lastStepDetectedEpochMillis = nowMs,
          isActivelyWalking = true,
        )
      }

      if (shouldPushSurfaces) {
        notifyExternalSurfacesIfNeeded(
          appContext = appContext,
          displayedSteps = latestDisplayedSteps,
          forceImmediate = false,
        )
      }
      schedulePostWalkHealthConnectSync(appContext)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
  }

  fun startLiveStepSensorTracking(context: Context) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    if (isSensorRegistered) return

    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    val counterSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    val detectorSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    if (counterSensor == null && detectorSensor == null) return

    try {
      var anyRegistered = false
      if (counterSensor != null) {
        val okCounter = sm.registerListener(
          stepSensorListener,
          counterSensor,
          SensorManager.SENSOR_DELAY_FASTEST,
          0,
        )
        anyRegistered = anyRegistered || okCounter
      }
      if (detectorSensor != null) {
        val okDetector = sm.registerListener(
          stepSensorListener,
          detectorSensor,
          SensorManager.SENSOR_DELAY_FASTEST,
          0,
        )
        anyRegistered = anyRegistered || okDetector
      }
      isSensorRegistered = anyRegistered
      _syncState.value = _syncState.value.copy(
        isLiveSensorListening = anyRegistered,
        hasActivityRecognitionPermission = hasActivityRecognitionPermission(appContext),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
        hasHardwareStepSensor = true,
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed to register step counter/detector listener", e)
    }
  }

  fun stopLiveStepSensorTracking() {
    val sm = activeAppContext?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    if (sm != null && isSensorRegistered) {
      runCatching { sm.unregisterListener(stepSensorListener) }
      isSensorRegistered = false
      _syncState.value = _syncState.value.copy(isLiveSensorListening = false)
    }
  }

  /**
   * Flushes any batched hardware step events from the sensor FIFO and probes the current counter.
   */
  fun probeHardwareStepCounterNow(context: Context) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    if (isSensorRegistered) {
      runCatching { sm.flush(stepSensorListener) }
    }
    val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

    val oneShotListener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
          stepSensorListener.onSensorChanged(event)
          runCatching { sm.unregisterListener(this) }
        }
      }
      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    try {
      sm.registerListener(oneShotListener, sensor, SensorManager.SENSOR_DELAY_FASTEST, 0)
      mainHandler.postDelayed({
        runCatching { sm.unregisterListener(oneShotListener) }
      }, 600L)
    } catch (e: Exception) {
      Log.d(TAG, "probeHardwareStepCounterNow error", e)
    }
  }

  fun flushHardwareStepSensorNow(context: Context) {
    probeHardwareStepCounterNow(context)
  }

  fun resetForNewDay(context: Context, newDayKey: String) {
    val appContext = context.applicationContext
    var lastRaw = -1f
    synchronized(stateLock) {
      lastRaw = reconciliationState.lastSeenRawCounter
      reconciliationState = StepReconciliationSnapshot(
        dayKey = newDayKey,
        syncedHealthConnectSteps = 0,
        syncedHealthConnectDistanceMeters = 0f,
        lastHcRecordEndEpochMs = 0L,
        sensorBaselineAtSync = lastRaw,
        lastSeenRawCounter = lastRaw,
        localStepsSinceSync = 0,
        pendingDetectorSteps = 0,
      )
    }
    BioSyncLocalStore.saveStepTelemetrySnapshot(
      context = appContext,
      todaySyncedSteps = 0,
      todayDistanceMeters = 0f,
      stepSensorDayKey = newDayKey,
      stepSensorBaselineCounter = lastRaw,
      stepSensorLastRawCounter = lastRaw,
      lastHealthConnectWrittenSteps = 0,
      lastHcRecordEndEpochMillis = 0L,
    )
    _syncState.value = _syncState.value.copy(
      liveTodaySteps = 0,
      healthConnectSteps = 0,
      hardwareSensorSteps = 0,
      liveDistanceMeters = 0f,
      lastStepDetectedEpochMillis = 0L,
      isActivelyWalking = false,
    )
    runCatching {
      BioSyncAppWidgetProvider.updateAllWidgets(appContext)
    }
  }

  fun initializeFromLocalStore(context: Context, currentDecidedGoal: Int) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    val stored = BioSyncLocalStore.load(appContext)
    cachedUserHeightCm = stored.heightCm

    val today = currentDayKey()
    var snap: StepReconciliationSnapshot
    synchronized(stateLock) {
      reconciliationState = GoogleFitStepReconciler.onInitializeFromStorage(
        inMemory = reconciliationState,
        stored = stored,
        today = today,
      )
      snap = reconciliationState
    }

    val displayedSteps = snap.displayedSteps
    val strideMeters = (cachedUserHeightCm * 0.415f) / 100f
    val displayedDistMeters = if (snap.syncedHealthConnectDistanceMeters > 0f) {
      snap.syncedHealthConnectDistanceMeters + (snap.liveSensorBridgeSteps * strideMeters)
    } else {
      displayedSteps * strideMeters
    }

    val sdkStatus = getHealthConnectSdkStatus(appContext)
    val isHcAvailable = sdkStatus == HealthConnectClient.SDK_AVAILABLE
    val isHcUpdateReq = sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    _syncState.value = _syncState.value.copy(
      decidedDailyStepsGoal = currentDecidedGoal.coerceAtLeast(1000),
      liveTodaySteps = displayedSteps,
      healthConnectSteps = snap.syncedHealthConnectSteps,
      hardwareSensorSteps = snap.liveSensorBridgeSteps,
      liveDistanceMeters = displayedDistMeters,
      isHealthConnectAvailable = isHcAvailable,
      isHealthConnectUpdateRequired = isHcUpdateReq,
      hasHealthConnectReadPermission = stored.healthConnectPermissionsGranted,
      hasActivityRecognitionPermission = hasActivityRecognitionPermission(appContext),
      isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
      hasHardwareStepSensor = hasHardwareStepSensor(appContext),
    )

    if (hasActivityRecognitionPermission(appContext)) {
      startLiveStepSensorTracking(appContext)
      probeHardwareStepCounterNow(appContext)
    }
  }

  fun areAllMandatoryPermissionsGrantedSync(context: Context): Boolean {
    val appContext = context.applicationContext
    val hasActivity = hasActivityRecognitionPermission(appContext)
    val isBatteryIgnored = isIgnoringBatteryOptimizations(appContext)
    val stored = BioSyncLocalStore.load(appContext)
    val sdkStatus = getHealthConnectSdkStatus(appContext)
    val hcOk = when (sdkStatus) {
      HealthConnectClient.SDK_AVAILABLE ->
        _syncState.value.hasHealthConnectReadPermission && stored.healthConnectPermissionsGranted
      HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> false
      else -> true
    }
    return hasActivity && isBatteryIgnored && hcOk
  }

  fun triggerImmediateAsyncSync(context: Context) {
    scope.launch {
      syncWithHealthConnect(context, force = true)
    }
  }

  /**
   * Synchronizes with Health Connect & Google Fit.
   * Strictly follows Google Fit (`com.google.android.apps.fitness`) and external Health Connect
   * providers (excluding any self-origin records), reconciles the live hardware step baseline,
   * and updates UI, Widgets, Live Notification, and `DashboardLocalStore`.
   */
  suspend fun syncWithHealthConnect(
    context: Context,
    force: Boolean = false,
  ): Boolean = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    val nowElapsed = SystemClock.elapsedRealtime()

    if (!force && (nowElapsed - lastSyncAttemptElapsedMs) < 1_500L) {
      return@withContext _syncState.value.isAllPermissionsAllowed
    }
    lastSyncAttemptElapsedMs = nowElapsed

    // Flush and probe hardware sensor so fresh kernel steps are latched before reconciliation
    probeHardwareStepCounterNow(appContext)

    syncMutex.withLock {
      val hasActivity = hasActivityRecognitionPermission(appContext)
      val isBatteryIgnored = isIgnoringBatteryOptimizations(appContext)
      val hasHardware = hasHardwareStepSensor(appContext)
      val sdkStatus = getHealthConnectSdkStatus(appContext)
      val isHcAvailable = sdkStatus == HealthConnectClient.SDK_AVAILABLE
      val isHcUpdateReq = sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

      if (!isHcAvailable) {
        _syncState.value = _syncState.value.copy(
          isHealthConnectAvailable = false,
          isHealthConnectUpdateRequired = isHcUpdateReq,
          hasActivityRecognitionPermission = hasActivity,
          isIgnoringBatteryOptimizations = isBatteryIgnored,
          hasHardwareStepSensor = hasHardware,
        )
        return@withLock _syncState.value.isAllPermissionsAllowed
      }

      val client = try {
        HealthConnectClient.getOrCreate(appContext)
      } catch (e: Exception) {
        Log.w(TAG, "HealthConnectClient.getOrCreate failed", e)
        return@withLock false
      }

      val grantedPermissions = try {
        client.permissionController.getGrantedPermissions()
      } catch (e: Exception) {
        Log.w(TAG, "getGrantedPermissions failed", e)
        emptySet()
      }

      val readStepsGranted = grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))
      val readDistGranted = grantedPermissions.contains(HealthPermission.getReadPermission(DistanceRecord::class))
      val hcAllowAllGranted = readStepsGranted && readDistGranted
      val bgReadGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        grantedPermissions.contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
      } else {
        true
      }

      if (!hcAllowAllGranted) {
        BioSyncLocalStore.saveStepTelemetrySnapshot(
          context = appContext,
          healthConnectPermissionsGranted = false,
        )
        _syncState.value = _syncState.value.copy(
          isHealthConnectAvailable = true,
          isHealthConnectUpdateRequired = false,
          hasHealthConnectReadPermission = false,
          hasHealthConnectWritePermission = false,
          hasHealthConnectBackgroundReadPermission = bgReadGranted,
          hasActivityRecognitionPermission = hasActivity,
          isIgnoringBatteryOptimizations = isBatteryIgnored,
          hasHardwareStepSensor = hasHardware,
        )
        return@withLock false
      }

      val zoneId = ZoneId.systemDefault()
      val today = LocalDate.now(zoneId)
      val startOfDay = today.atStartOfDay(zoneId).toInstant()
      val nowInstant = Instant.now()
      val timeRange = TimeRangeFilter.between(startOfDay, nowInstant)
      val selfPackage = appContext.packageName

      // 1. Inspect today's StepsRecord entries to identify external origins (Google Fit, wearables)
      //    and find the latest record end timestamp while ignoring any self-package records.
      var latestHcRecordEndEpochMs = 0L
      val externalOrigins = mutableSetOf<String>()
      try {
        val recordsResponse = client.readRecords(
          ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = timeRange,
          ),
        )
        for (record in recordsResponse.records) {
          val originPkg = record.metadata.dataOrigin.packageName
          if (originPkg != selfPackage) {
            externalOrigins.add(originPkg)
            val endMs = record.endTime.toEpochMilli()
            if (endMs > latestHcRecordEndEpochMs) {
              latestHcRecordEndEpochMs = endMs
            }
          }
        }
      } catch (e: Exception) {
        Log.d(TAG, "Health Connect readRecords(StepsRecord) fallback to aggregate", e)
      }

      // 2. Query authoritative Google Fit steps specifically + deduplicated external aggregate
      var googleFitSteps = 0
      try {
        val fitStepAgg = client.aggregate(
          AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = timeRange,
            dataOriginFilter = setOf(DataOrigin(GOOGLE_FIT_PACKAGE)),
          ),
        )
        googleFitSteps = fitStepAgg[StepsRecord.COUNT_TOTAL]?.toInt()?.coerceAtLeast(0) ?: 0
      } catch (_: Exception) {}

      var totalExternalSteps = 0
      try {
        val originFilter = if (externalOrigins.isNotEmpty()) {
          externalOrigins.map { DataOrigin(it) }.toSet()
        } else {
          emptySet()
        }
        val totalStepAgg = client.aggregate(
          AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = timeRange,
            dataOriginFilter = originFilter,
          ),
        )
        totalExternalSteps = totalStepAgg[StepsRecord.COUNT_TOTAL]?.toInt()?.coerceAtLeast(0) ?: 0
      } catch (e: Exception) {
        Log.w(TAG, "Health Connect steps aggregate failed", e)
      }

      val hcSteps = if (googleFitSteps > 0) {
        maxOf(googleFitSteps, totalExternalSteps)
      } else {
        totalExternalSteps
      }

      // 3. Query Distance if granted (preferring Google Fit & external origins)
      var resolvedDistanceMeters: Float? = null
      if (readDistGranted) {
        try {
          var fitDistMeters = 0f
          runCatching {
            val fitDistAgg = client.aggregate(
              AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = timeRange,
                dataOriginFilter = setOf(DataOrigin(GOOGLE_FIT_PACKAGE)),
              ),
            )
            val fitLen = fitDistAgg[DistanceRecord.DISTANCE_TOTAL]
            if (fitLen != null && fitLen.inMeters > 0) {
              fitDistMeters = fitLen.inMeters.toFloat()
            }
          }

          val originFilter = if (externalOrigins.isNotEmpty()) {
            externalOrigins.map { DataOrigin(it) }.toSet()
          } else {
            emptySet()
          }
          val totalDistAgg = client.aggregate(
            AggregateRequest(
              metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
              timeRangeFilter = timeRange,
              dataOriginFilter = originFilter,
            ),
          )
          val distLen = totalDistAgg[DistanceRecord.DISTANCE_TOTAL]
          val extDistMeters = if (distLen != null && distLen.inMeters > 0) {
            distLen.inMeters.toFloat()
          } else {
            0f
          }
          val bestDist = maxOf(fitDistMeters, extDistMeters)
          if (bestDist > 0f) {
            resolvedDistanceMeters = bestDist
          }
        } catch (_: Exception) {}
      }

      val todayStr = currentDayKey()
      val nowWallMs = System.currentTimeMillis()
      var displayedStepsAfterSync = 0

      synchronized(stateLock) {
        val lastStepMs = _syncState.value.lastStepDetectedEpochMillis
        val isWalkingNow = lastStepMs > 0L && (nowWallMs - lastStepMs) < ACTIVE_WALKING_WINDOW_MS
        val strideMeters = (cachedUserHeightCm * 0.415f) / 100f

        reconciliationState = GoogleFitStepReconciler.onHealthConnectSync(
          current = reconciliationState,
          hcSteps = hcSteps,
          hcDistanceMeters = resolvedDistanceMeters,
          hcLatestRecordEndEpochMs = latestHcRecordEndEpochMs,
          today = todayStr,
          strideMeters = strideMeters,
          force = force,
          isActivelyWalkingNow = isWalkingNow,
        )

        val snap = reconciliationState
        val displayedSteps = snap.displayedSteps
        displayedStepsAfterSync = displayedSteps
        val displayedDistMeters = if (snap.syncedHealthConnectDistanceMeters > 0f) {
          snap.syncedHealthConnectDistanceMeters + (snap.liveSensorBridgeSteps * strideMeters)
        } else {
          displayedSteps * strideMeters
        }

        BioSyncLocalStore.saveStepTelemetrySnapshot(
          context = appContext,
          todaySyncedSteps = displayedSteps,
          todayDistanceMeters = displayedDistMeters,
          stepSensorDayKey = todayStr,
          stepSensorBaselineCounter = snap.sensorBaselineAtSync,
          stepSensorLastRawCounter = snap.lastSeenRawCounter,
          lastHealthConnectWrittenSteps = snap.syncedHealthConnectSteps,
          lastHcRecordEndEpochMillis = snap.lastHcRecordEndEpochMs,
          lastStepSyncEpochMillis = nowWallMs,
          healthConnectPermissionsGranted = true,
        )

        _syncState.value = _syncState.value.copy(
          liveTodaySteps = displayedSteps,
          healthConnectSteps = snap.syncedHealthConnectSteps,
          hardwareSensorSteps = snap.liveSensorBridgeSteps,
          liveDistanceMeters = displayedDistMeters,
          connectedDataOrigins = externalOrigins,
          lastSyncedEpochMillis = nowWallMs,
          isHealthConnectAvailable = true,
          isHealthConnectUpdateRequired = false,
          hasHealthConnectReadPermission = true,
          hasHealthConnectWritePermission = false,
          hasHealthConnectBackgroundReadPermission = bgReadGranted,
          hasActivityRecognitionPermission = hasActivity,
          isIgnoringBatteryOptimizations = isBatteryIgnored,
          hasHardwareStepSensor = hasHardware,
        )
      }

      notifyExternalSurfacesIfNeeded(
        appContext = appContext,
        displayedSteps = displayedStepsAfterSync,
        forceImmediate = true,
      )

      return@withLock _syncState.value.isAllPermissionsAllowed
    }
  }
}

/**
 * Main Jetpack Compose controller that manages permissions flow and observes
 * live step sync.
 */
@Composable
fun rememberBioSyncStepSyncController(
  decidedDailyStepsGoal: Int = 8500,
  autoRequestPermissionsOnStart: Boolean = false,
  context: Context = LocalContext.current,
): BioSyncStepSyncController {
  val syncState by BioSyncHealthStepManager.syncState.collectAsState()
  val coroutineScope = rememberCoroutineScope()
  val lifecycleOwner = LocalLifecycleOwner.current

  var pendingResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
  var awaitingSettingsReturn by remember { mutableStateOf(false) }
  var hasAutoRequestedThisSession by remember { mutableStateOf(false) }
  val latestGoal by rememberUpdatedState(decidedDailyStepsGoal)

  val batteryOptimizationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { _ ->
    coroutineScope.launch {
      val allAllowed = BioSyncHealthStepManager.syncWithHealthConnect(context, force = true)
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
      val allAllowedNow = BioSyncHealthStepManager.syncWithHealthConnect(context, force = true)
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
          BioSyncHealthStepManager.startLiveStepSensorTracking(context)
          BioSyncStepBackgroundService.startIfAuthorized(context)
        }
        pendingResultCallback?.invoke(allAllowedNow)
        pendingResultCallback = null
      }
    }
  }

  val healthPermissionContract = remember {
    PermissionController.createRequestPermissionResultContract()
  }
  val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
    contract = healthPermissionContract,
  ) { grantedSet ->
    coroutineScope.launch {
      val readGranted = grantedSet.any { it.contains("READ_STEPS") }
      if (readGranted) {
        BioSyncHealthStepManager.markHealthConnectAllowAllGranted(context)
      }
      BioSyncHealthStepManager.syncWithHealthConnect(context, force = true)
      if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
        BioSyncHealthStepManager.startLiveStepSensorTracking(context)
        BioSyncStepBackgroundService.startIfAuthorized(context)
      }
      launchBatteryExemptionOrFinish()
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

  LaunchedEffect(decidedDailyStepsGoal) {
    BioSyncHealthStepManager.initializeFromLocalStore(context, decidedDailyStepsGoal)
    BioSyncHealthStepManager.updateDecidedDailyStepsGoal(context, decidedDailyStepsGoal)
  }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          if (BioSyncHealthStepManager.hasActivityRecognitionPermission(context)) {
            BioSyncHealthStepManager.startLiveStepSensorTracking(context)
            BioSyncHealthStepManager.probeHardwareStepCounterNow(context)
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
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

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
      delay(10_000L)
      BioSyncHealthStepManager.syncWithHealthConnect(context)
    }
  }

  return remember(syncState, requestAllowAllPermissions) {
    BioSyncStepSyncController(
      state = syncState,
      verifyAllPermissionsAllowed = {
        BioSyncHealthStepManager.syncWithHealthConnect(context, force = true)
      },
      requestAllowAllPermissions = requestAllowAllPermissions,
      requestBatteryOptimizationExemption = {
        launchBatteryExemptionOrFinish()
      },
      forceRefreshNow = {
        coroutineScope.launch {
          BioSyncHealthStepManager.probeHardwareStepCounterNow(context)
          BioSyncHealthStepManager.syncWithHealthConnect(context, force = true)
          BioSyncStepBackgroundService.startIfAuthorized(context)
        }
      },
      openHealthConnectAppOrSettings = {
        awaitingSettingsReturn = true
        BioSyncHealthStepManager.openHealthConnectSettings(context)
      },
    )
  }
}
