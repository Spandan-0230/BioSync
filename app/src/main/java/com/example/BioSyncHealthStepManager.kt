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
 * 1. Hardware Step Counter Ledger (`StepLedger`):
 *    Tracks the device's hardware `Sensor.TYPE_STEP_COUNTER` relative to the saved midnight baseline.
 *    Counts steps in real-time in the background without needing to open Google Fit or BioSync!
 * 2. Seamless Google Fit Reconciliation (`onFitTotal`):
 *    When Google Fit syncs to Health Connect (or when the user opens Google Fit), BioSync reads
 *    Health Connect and adjusts the offset if Google Fit has additional steps (e.g. from smartwatch).
 *    Never drops below Google Fit's number, never double-counts, and never shows random jumps.
 * 3. Strictly READ-ONLY Health Connect:
 *    BioSync NEVER calls insertRecords or deleteRecords on Health Connect, so Google Fit data is 100% preserved.
 * 4. Safe Rate-Limiting:
 *    Enforces cooldown on Health Connect calls to prevent RateLimitExceededException.
 */
object BioSyncHealthStepManager {
  private const val TAG = "BioSyncHealthStepMgr"
  const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
  const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"
  private const val PERM_READ_STEPS = "android.permission.health.READ_STEPS"
  private const val PERM_READ_DISTANCE = "android.permission.health.READ_DISTANCE"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncMutex = Mutex()
  private val ledgerLock = Any()
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _syncState = MutableStateFlow(BioSyncStepSyncState())
  val syncState: StateFlow<BioSyncStepSyncState> = _syncState.asStateFlow()

  @Volatile private var currentLedger = StepLedger()
  @Volatile private var cachedUserHeightCm: Float = 172f
  @Volatile private var activeAppContext: Context? = null
  @Volatile private var isSensorRegistered = false
  @Volatile private var lastSyncAttemptElapsedMs: Long = 0L
  @Volatile private var lastSeenRawCounter: Float = -1f

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

  fun getBootCount(context: Context): Int {
    return try {
      Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    } catch (_: Exception) {
      -1
    }
  }

  fun getBootTimeMs(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

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

  /**
   * Thread-safe ledger update and live state publishing.
   */
  private fun updateLedger(ctx: Context, transform: (StepLedger) -> StepLedger): StepLedger =
    synchronized(ledgerLock) {
      val updated = transform(currentLedger)
      currentLedger = updated
      BioSyncLocalStore.saveLedger(ctx, updated)

      val totalSteps = StepLedgerMath.total(updated)
      val strideMeters = (cachedUserHeightCm * 0.415f) / 100f
      val distMeters = totalSteps * strideMeters

      _syncState.value = _syncState.value.copy(
        liveTodaySteps = totalSteps,
        healthConnectSteps = totalSteps,
        liveDistanceMeters = distMeters,
        lastSyncedEpochMillis = System.currentTimeMillis(),
      )
      updated
    }

  private val stepSensorListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
      if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
      val raw = event.values.firstOrNull() ?: return
      if (raw <= 0f) return
      lastSeenRawCounter = raw
      val appContext = activeAppContext ?: return
      val nowMs = System.currentTimeMillis()
      val today = currentDayKey()
      val zoneId = ZoneId.systemDefault()
      val startOfDayMs = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
      val boot = getBootCount(appContext)
      val bootTimeMs = getBootTimeMs()

      updateLedger(appContext) { l ->
        StepLedgerMath.onRaw(l, raw, nowMs, today, startOfDayMs, boot, bootTimeMs)
      }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
  }

  fun startLiveStepSensorTracking(context: Context) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    if (isSensorRegistered) return

    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
    val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
    try {
      val ok = sm.registerListener(stepSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
      isSensorRegistered = ok
      _syncState.value = _syncState.value.copy(
        isLiveSensorListening = ok,
        hasActivityRecognitionPermission = hasActivityRecognitionPermission(appContext),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(appContext),
        hasHardwareStepSensor = true,
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed to register step counter listener", e)
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
   * One-shot hardware sensor probe to instantly latch the kernel step count
   * even when stationary.
   */
  fun probeHardwareStepCounterNow(context: Context) {
    val appContext = context.applicationContext
    val sm = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
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
      sm.registerListener(oneShotListener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
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

  fun initializeFromLocalStore(context: Context, currentDecidedGoal: Int) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    val stored = BioSyncLocalStore.load(appContext)
    cachedUserHeightCm = stored.heightCm

    val today = currentDayKey()
    val loadedLedger = BioSyncLocalStore.loadLedger(appContext)
    val zoneId = ZoneId.systemDefault()
    val startOfDayMs = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val boot = getBootCount(appContext)
    val bootTimeMs = getBootTimeMs()

    val rolledLedger = if (loadedLedger.dayKey != today && loadedLedger.dayKey.isNotEmpty()) {
      StepLedgerMath.onRaw(
        l = loadedLedger,
        raw = if (loadedLedger.lastRaw > 0f) loadedLedger.lastRaw else 0f,
        nowMs = System.currentTimeMillis(),
        today = today,
        startOfTodayMs = startOfDayMs,
        boot = boot,
        bootTimeMs = bootTimeMs,
      )
    } else {
      loadedLedger
    }

    synchronized(ledgerLock) {
      currentLedger = rolledLedger
      if (rolledLedger != loadedLedger) {
        BioSyncLocalStore.saveLedger(appContext, rolledLedger)
      }
    }

    val totalSteps = StepLedgerMath.total(rolledLedger)
    val strideMeters = (cachedUserHeightCm * 0.415f) / 100f
    val distMeters = totalSteps * strideMeters

    _syncState.value = _syncState.value.copy(
      decidedDailyStepsGoal = currentDecidedGoal.coerceAtLeast(1000),
      liveTodaySteps = totalSteps,
      healthConnectSteps = totalSteps,
      liveDistanceMeters = distMeters,
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

  fun triggerImmediateAsyncSync(context: Context) {
    scope.launch {
      syncWithHealthConnect(context, force = true)
    }
  }

  /**
   * Synchronizes with Health Connect. Reads Google Fit's synced steps,
   * passes them into StepLedgerMath.onFitTotal, and updates distance.
   */
  suspend fun syncWithHealthConnect(
    context: Context,
    force: Boolean = false,
  ): Boolean = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    activeAppContext = appContext
    val nowElapsed = SystemClock.elapsedRealtime()

    if (!force && (nowElapsed - lastSyncAttemptElapsedMs) < 2500L) {
      return@withContext _syncState.value.isAllPermissionsAllowed
    }
    lastSyncAttemptElapsedMs = nowElapsed

    // Always probe hardware sensor on sync so fresh steps are latched
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
        return@withLock false
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
      val bgReadGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        grantedPermissions.contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
      } else {
        true
      }

      if (!readStepsGranted) {
        _syncState.value = _syncState.value.copy(
          isHealthConnectAvailable = true,
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
      val fitOrigin = DataOrigin(GOOGLE_FIT_PACKAGE)

      var fitSteps: Int? = null
      val originsFound = mutableSetOf<String>()

      // 1. Query Steps: First check Google Fit package specifically
      try {
        val fitStepAgg = client.aggregate(
          AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = timeRange,
            dataOriginFilter = setOf(fitOrigin),
          ),
        )
        val count = fitStepAgg[StepsRecord.COUNT_TOTAL]?.toInt()
        if (count != null && count > 0) {
          fitSteps = count
          originsFound.add(GOOGLE_FIT_PACKAGE)
        }
      } catch (e: Exception) {
        Log.d(TAG, "Fit package aggregate check", e)
      }

      // Fallback: General Health Connect steps
      if (fitSteps == null || fitSteps == 0) {
        try {
          val totalStepAgg = client.aggregate(
            AggregateRequest(
              metrics = setOf(StepsRecord.COUNT_TOTAL),
              timeRangeFilter = timeRange,
            ),
          )
          val totalCount = totalStepAgg[StepsRecord.COUNT_TOTAL]?.toInt()
          if (totalCount != null && totalCount > 0) {
            fitSteps = totalCount
          }
        } catch (e: Exception) {
          Log.d(TAG, "General aggregate check", e)
        }
      }

      // Reconcile Fit steps with ledger
      if (fitSteps != null && fitSteps > 0) {
        updateLedger(appContext) { l ->
          StepLedgerMath.onFitTotal(l, fitSteps)
        }
      }

      // Query Distance if available
      var resolvedDistanceMeters: Float? = null
      if (readDistGranted) {
        try {
          val fitDistAgg = client.aggregate(
            AggregateRequest(
              metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
              timeRangeFilter = timeRange,
              dataOriginFilter = setOf(fitOrigin),
            ),
          )
          val distLen = fitDistAgg[DistanceRecord.DISTANCE_TOTAL]
          if (distLen != null && distLen.inMeters > 0) {
            resolvedDistanceMeters = distLen.inMeters.toFloat()
          }
        } catch (_: Exception) {}

        if (resolvedDistanceMeters == null) {
          try {
            val totalDistAgg = client.aggregate(
              AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = timeRange,
              ),
            )
            val distLen = totalDistAgg[DistanceRecord.DISTANCE_TOTAL]
            if (distLen != null && distLen.inMeters > 0) {
              resolvedDistanceMeters = distLen.inMeters.toFloat()
            }
          } catch (_: Exception) {}
        }
      }

      val totalSteps = StepLedgerMath.total(currentLedger)
      val strideMeters = (cachedUserHeightCm * 0.415f) / 100f
      val finalDistanceMeters = resolvedDistanceMeters ?: (totalSteps * strideMeters)

      _syncState.value = _syncState.value.copy(
        liveTodaySteps = totalSteps,
        healthConnectSteps = totalSteps,
        liveDistanceMeters = finalDistanceMeters,
        lastSyncedEpochMillis = System.currentTimeMillis(),
        isHealthConnectAvailable = true,
        isHealthConnectUpdateRequired = false,
        hasHealthConnectReadPermission = true,
        hasHealthConnectWritePermission = false,
        hasHealthConnectBackgroundReadPermission = bgReadGranted,
        hasActivityRecognitionPermission = hasActivity,
        isIgnoringBatteryOptimizations = isBatteryIgnored,
        hasHardwareStepSensor = hasHardware,
        connectedDataOrigins = originsFound,
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
      delay(30_000L)
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
