# Streamlined Notification Bar: Total Steps, Distance & Live Speed (Google Fit Sync)

Update the ongoing BioSync background telemetry notification so it displays strictly **Total Steps**, **Distance**, and **Live Speed** synchronized with Google Fit—removing the daily goal fraction (`/ goal`) and average speed fallback.

## User Review & Critical Decisions

> [!IMPORTANT]
> The notification bar format is locked to the three requested live metrics:

- **Confirmed Decision — 3-Metric Notification Format**:
  - **Title**: `BioSync • Synced with Google Fit`
  - **Content**: `<Total Steps> steps • <Distance> km • <Live Speed> km/h` (for example: `4,280 steps • 3.26 km • 4.8 km/h`, showing `0.0 km/h` when stationary so all three metrics are always visible at a glance).
  - **Real-Time Refresh**: Updates reactively whenever `liveTodaySteps`, `liveDistanceMeters`, or `liveSpeedMetersPerSec` changes.

---

## 1. Overview & Core Concept

- **What It Does**: Displays a clean, real-time notification in the Android status/notification bar showing only the user's current **Total Steps**, **Distance (`km`)**, and **Live Speed (`km/h`)** synced with Google Fit.
- **Target Audience / Persona**: Users monitoring their live movement metrics from the notification shade or lock screen while walking or running.
- **Key Value**: Removes clutter (such as `/ 8,500` step goal fractions or secondary average speed labels) so the three core live telemetry numbers are immediately readable.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Active Walking / Running**: As steps, distance, and speed update from Google Fit and the live sensor stream, the notification updates in real time (`1,420 steps • 1.08 km • 5.2 km/h`).
  2. **Stationary / Idle**: When the user stops moving, Live Speed settles cleanly to `0.0 km/h` while Total Steps and Distance remain pinned to the latest Google Fit synced values.
- **Visual Identity & Theme**:
  - Uses a silent, low-priority ongoing service notification (`NotificationCompat.PRIORITY_LOW`, `setSilent(true)`, `setOnlyAlertOnce(true)`) that never vibrates or interrupts the user.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Always Show All Three Metrics (Steps, Distance, Live Speed)**
  - *Chosen Approach*: Format the notification body as `$stepsFormatted steps • $distKmFormatted • $liveSpeedFormatted` where `liveSpeedFormatted` always renders `%.1f km/h` (including `0.0 km/h` when idle) and triggers notification updates whenever any of Steps, Distance, or Live Speed changes.
  - *Why*: Ensures all three requested metrics are consistently visible in the notification bar and that live speed changes immediately reflect in the notification shade.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌──────────────────────────────────────────────────────────────────────┐
│            BioSyncHealthStepManager.syncState (StateFlow)            │
│  • liveTodaySteps        (Int)                                       │
│  • liveDistanceKm        (Float)                                     │
│  • liveSpeedKmh          (Float)                                     │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │
                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│         BioSyncStepBackgroundService Notification Observer           │
│  • Observes steps, distance (0.01 km), and live speed (0.1 km/h)     │
│  • Renders: "<Steps> steps • <Distance> km • <Live Speed> km/h"      │
│  • Header:  "BioSync • Synced with Google Fit"                       │
└──────────────────────────────────────────────────────────────────────┘
```

- **State & Observer Mapping**:
  - `startLiveNotificationObserver()` in `BioSyncStepBackgroundService` tracks `liveTodaySteps`, rounded `liveDistanceKm`, and rounded `liveSpeedKmh` so any change in steps, distance, or live speed immediately updates the notification bar.
