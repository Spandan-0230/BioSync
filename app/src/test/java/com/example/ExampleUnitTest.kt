package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `AutoBackupConfigState defaults to enabled with 24hrs DAILY_24H and DEFAULT_FOLDER and supports all 4 cadences`() {
    val defaultConfig = AutoBackupConfigState()
    assertTrue(defaultConfig.enabled)
    assertEquals(AutoBackupFrequencyOption.DAILY_24H, defaultConfig.frequency)
    assertEquals(AutoBackupFolderMode.DEFAULT_FOLDER, defaultConfig.folderMode)
    assertTrue(defaultConfig.defaultFolderAbsolutePath.contains("BioSyncBackups/biosync_autobackup_vault.json"))
    assertTrue(defaultConfig.defaultInternalMirrorPath.contains("BioSyncBackups/biosync_autobackup_vault.json"))
    assertEquals("0.0 KB", defaultConfig.lastBackupFileSizeFormatted)
    assertEquals("VERIFIED • READY", defaultConfig.backupHealthBadge)
    assertEquals("4.5 KB", BioSyncBackupManager.formatFileSizeKb(4608L))
    assertEquals(24L * 60L * 60L * 1000L, AutoBackupFrequencyOption.DAILY_24H.intervalMillis)
    assertEquals(0L, AutoBackupFrequencyOption.EVERY_LOG.intervalMillis)
    assertEquals(60L * 60L * 1000L, AutoBackupFrequencyOption.HOURLY.intervalMillis)
    assertEquals(7L * 24L * 60L * 60L * 1000L, AutoBackupFrequencyOption.WEEKLY.intervalMillis)

    assertEquals(AutoBackupFrequencyOption.EVERY_LOG, AutoBackupFrequencyOption.fromCode("EVERY_LOG"))
    assertEquals(AutoBackupFrequencyOption.HOURLY, AutoBackupFrequencyOption.fromCode("HOURLY"))
    assertEquals(AutoBackupFrequencyOption.WEEKLY, AutoBackupFrequencyOption.fromCode("WEEKLY"))
    assertEquals(AutoBackupFrequencyOption.DAILY_24H, AutoBackupFrequencyOption.fromCode("DAILY_24H"))
    assertEquals(AutoBackupFrequencyOption.DAILY_24H, AutoBackupFrequencyOption.fromCode(null))

    assertEquals("com.example.action.WIDGET_LOG_STEPS_250", BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_250)
    assertEquals("com.example.action.WIDGET_LOG_STEPS_500", BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_500)
    assertEquals("com.example.action.WIDGET_LOG_STEPS_1000", BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_1000)
  }

  @Test
  fun `BioSyncStreakEngine preserves streak within 2-day grace buffer and resets after 2 missed days`() {
    val dayPlus1 = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = 10,
      lastCompletedDayKey = "2026-11-01",
      todayKey = "2026-11-02",
      todayQualified = false,
    )
    assertEquals(10, dayPlus1.streakDays)
    assertFalse(dayPlus1.isBufferExpired)

    val dayPlus2 = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = 10,
      lastCompletedDayKey = "2026-11-01",
      todayKey = "2026-11-03",
      todayQualified = false,
    )
    assertEquals(10, dayPlus2.streakDays)
    assertTrue(dayPlus2.isInBufferGrace)

    val recovered = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = 10,
      lastCompletedDayKey = "2026-11-01",
      todayKey = "2026-11-03",
      todayQualified = true,
    )
    assertEquals(11, recovered.streakDays)
    assertEquals("2026-11-03", recovered.lastCompletedDayKey)

    val expired = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = 10,
      lastCompletedDayKey = "2026-11-01",
      todayKey = "2026-11-04",
      todayQualified = false,
    )
    assertEquals(0, expired.streakDays)
    assertTrue(expired.isBufferExpired)
  }

  @Test
  fun `FairNutrientEstimator derives calories from macros when calories left blank and estimates fairly when Dont Know enabled`() {
    val resolvedManual = FairNutrientEstimator.resolveDraft(
      rawCaloriesText = "",
      rawProteinText = "45",
      rawCarbsText = "50",
      rawFatsText = "10",
      caloriesDontKnow = false,
      proteinDontKnow = false,
      carbsDontKnow = false,
      fatsDontKnow = false,
      targetCalories = 2400,
      targetProtein = 180,
      targetCarbs = 240,
      targetFats = 70,
      mealCount = 3,
      isMealMode = true,
    )
    assertEquals(470, resolvedManual.caloriesKcal)
    assertEquals(45, resolvedManual.proteinGrams)
    assertEquals(50, resolvedManual.carbsGrams)
    assertEquals(10, resolvedManual.fatsGrams)

    val resolvedAuto = FairNutrientEstimator.resolveDraft(
      rawCaloriesText = "",
      rawProteinText = "",
      rawCarbsText = "",
      rawFatsText = "",
      caloriesDontKnow = true,
      proteinDontKnow = true,
      carbsDontKnow = true,
      fatsDontKnow = true,
      targetCalories = 2400,
      targetProtein = 180,
      targetCarbs = 240,
      targetFats = 70,
      mealCount = 3,
      isMealMode = true,
    )
    assertTrue(resolvedAuto.caloriesKcal >= 400)
    assertTrue(resolvedAuto.proteinGrams >= 25)
  }

  @Test
  fun `StepLedgerMath handles normal delta, reboot carry, and Google Fit reconciliation`() {
    val startOfTodayMs = 1700000000000L
    val bootTimeBeforeTodayMs = startOfTodayMs - 50_000_000L

    val firstBoot = StepLedgerMath.onRaw(
      l = StepLedger(),
      raw = 5000f,
      nowMs = startOfTodayMs + 1000L,
      today = "2026-11-02",
      startOfTodayMs = startOfTodayMs,
      boot = 1,
      bootTimeMs = bootTimeBeforeTodayMs,
    )
    assertEquals(0, StepLedgerMath.total(firstBoot))

    val afterWalk = StepLedgerMath.onRaw(
      l = firstBoot,
      raw = 6250f,
      nowMs = startOfTodayMs + 60_000L,
      today = "2026-11-02",
      startOfTodayMs = startOfTodayMs,
      boot = 1,
      bootTimeMs = bootTimeBeforeTodayMs,
    )
    assertEquals(1250, StepLedgerMath.total(afterWalk))

    val afterReboot = StepLedgerMath.onRaw(
      l = afterWalk,
      raw = 300f,
      nowMs = startOfTodayMs + 120_000L,
      today = "2026-11-02",
      startOfTodayMs = startOfTodayMs,
      boot = 2,
      bootTimeMs = startOfTodayMs + 100_000L,
    )
    assertEquals(1550, StepLedgerMath.total(afterReboot))

    val afterFit = StepLedgerMath.onFitTotal(afterReboot, 1800)
    assertEquals(1800, StepLedgerMath.total(afterFit))
  }

  @Test
  fun `GoogleFitStepReconciler strictly follows Google Fit Health Connect without double-counting or wiping live walks`() {
    val today = "2026-11-02"
    val stride = 0.7138f

    // 1. First hardware counter reading of the session + initial Google Fit / Health Connect sync (2,000 steps)
    var state = GoogleFitStepReconciler.onHardwareStepCounterEvent(
      current = StepReconciliationSnapshot(),
      rawCounter = 10_000f,
      today = today,
    )
    state = GoogleFitStepReconciler.onHealthConnectSync(
      current = state,
      hcSteps = 2000,
      hcDistanceMeters = 1420f,
      hcLatestRecordEndEpochMs = 1_700_000_000_000L,
      today = today,
      strideMeters = stride,
      force = false,
      isActivelyWalkingNow = false,
    )
    assertEquals(2000, state.displayedSteps)
    assertEquals(2000, state.syncedHealthConnectSteps)
    assertEquals(0, state.liveSensorBridgeSteps)

    // 2. User starts walking: step detector fires immediately (+5 steps), then hardware counter flushes (+80 steps)
    state = GoogleFitStepReconciler.onHardwareStepDetectorEvent(state, 5, today)
    assertEquals(2005, state.displayedSteps)

    state = GoogleFitStepReconciler.onHardwareStepCounterEvent(state, 10_080f, today)
    assertEquals(2080, state.displayedSteps)
    assertEquals(80, state.localStepsSinceSync)
    assertEquals(0, state.pendingDetectorSteps)

    // 3. Background Health Connect poll while still walking BEFORE Google Fit flushes (hcSteps still 2,000):
    //    Must NOT wipe the 80 live walking steps!
    state = GoogleFitStepReconciler.onHealthConnectSync(
      current = state,
      hcSteps = 2000,
      hcDistanceMeters = 1420f,
      hcLatestRecordEndEpochMs = 1_700_000_000_000L,
      today = today,
      strideMeters = stride,
      force = false,
      isActivelyWalkingNow = true,
    )
    assertEquals(2080, state.displayedSteps)
    assertEquals(80, state.localStepsSinceSync)

    // 4. Navigating across screens / re-initializing from storage must NEVER double-count the 80 steps
    val simulatedStored = BioSyncStoredState(
      stepSensorDayKey = today,
      todaySyncedSteps = state.displayedSteps, // 2080
      lastHealthConnectWrittenSteps = state.syncedHealthConnectSteps, // 2000
      stepSensorBaselineCounter = state.sensorBaselineAtSync, // 10000f
      stepSensorLastRawCounter = state.lastSeenRawCounter, // 10080f
      lastHcRecordEndEpochMillis = state.lastHcRecordEndEpochMs,
    )
    val coldReloaded = GoogleFitStepReconciler.onInitializeFromStorage(
      inMemory = StepReconciliationSnapshot(),
      stored = simulatedStored,
      today = today,
    )
    assertEquals(2000, coldReloaded.syncedHealthConnectSteps)
    assertEquals(80, coldReloaded.localStepsSinceSync)
    assertEquals(2080, coldReloaded.displayedSteps)

    // Next hardware step (10,081f) must increment to 2081, NOT 2161!
    val afterOneMoreStep = GoogleFitStepReconciler.onHardwareStepCounterEvent(coldReloaded, 10_081f, today)
    assertEquals(2081, afterOneMoreStep.displayedSteps)

    // 5. Google Fit flushes to Health Connect (reports authoritative 2,078 steps):
    //    BioSync strictly snaps to 2,078 and re-anchors baseline to 10,081f with 0 local delta
    val afterGoogleFitFlush = GoogleFitStepReconciler.onHealthConnectSync(
      current = afterOneMoreStep,
      hcSteps = 2078,
      hcDistanceMeters = 1480f,
      hcLatestRecordEndEpochMs = 1_700_000_060_000L,
      today = today,
      strideMeters = stride,
      force = false,
      isActivelyWalkingNow = true,
    )
    assertEquals(2078, afterGoogleFitFlush.displayedSteps)
    assertEquals(2078, afterGoogleFitFlush.syncedHealthConnectSteps)
    assertEquals(0, afterGoogleFitFlush.liveSensorBridgeSteps)
    assertEquals(10_081f, afterGoogleFitFlush.sensorBaselineAtSync, 0.01f)
  }
}

