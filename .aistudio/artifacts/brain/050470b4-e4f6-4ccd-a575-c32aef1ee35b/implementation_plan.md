# BioSync — Vault UI Overhaul, Native 6-Pillar Live HUD Notification, Widget Step Controls & Quick Action Engine Redesign

This plan delivers a complete visual and architectural upgrade across four core areas of **BioSync**: redesigning the **Settings Backup & Auto-Backup Vault**, overhauling the **6-Pillar Live Telemetry Notification** with full-height native XML layouts for razor-sharp typography, adding **+250 / +500 / +1,000 Step quick-add controls exclusively to the Home Screen Widget**, and sculpting a cleaner **Quick Action Engine** UI alongside background synchronization bug fixes.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices from Clarification Interview**
> - **Widget-Only Step Logging (`+250`, `+500`, `+1,000 Steps`)**: The Home Screen Widget will feature dedicated `+250`, `+500`, and `+1,000` step actuators alongside hydration (`+250ml`, `+500ml`) and `Open HUD`. **Notifications will strictly exclude step-adding buttons** and only expose hydration/open actions.
> - **Default Folder Location Inspector & File Health Telemetry**: Selecting or clicking **Default Folder** in Settings immediately displays the full on-device vault directory and file path (`.../files/BioSyncBackups/biosync_autobackup_vault.json`) with a 1-tap **Copy Path** control, an interactive Default Location details banner/modal, and a live **File Size & Integrity Health Badge** (e.g., `VERIFIED • 18.4 KB`).
> - **Recommended Bug Fixes Included**:
>   1. **Unified Live Telemetry & Native 6-Pillar Notification Window**: Replaces competing/blurry bitmap notifications with a unified, real-time **Native Custom `RemoteViews` 6-Pillar HUD Notification** (both collapsed and expanded full-height views) so text is 100% native and crystal-clear.
>   2. **Quick Action Engine Decluttering & Auto-Expand**: Merges the redundant double control bars into a single sculpted 3D command deck and auto-expands saved Meals and Single Items so newly created items are never hidden behind collapsed drawers.
>   3. **Reboot Auto-Backup Persistence**: Re-registers `BioSyncAutoBackupScheduler.syncSchedule(context)` inside `BioSyncBootReceiver` when the device restarts.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Sculpted 3D Settings Backup & Auto-Backup Vault**: Transforms the Settings backup card into an executive 3D Obsidian & Platinum console featuring a 3-pillar vault telemetry strip, a 2×2 tactile cadence matrix (`Every Log`, `Hourly`, `Weekly`, `24 Hours • Default`), an interactive **Default vs. Custom Folder Destination Deck** with copyable path inspection and file-size health badge, and high-contrast **Export / Import** actuators.
  2. **Expanded Window & Native Typography for 6-Pillar Live Notification**: Uses custom Android `RemoteViews` XML layouts (`collapsed` + full-height `expanded` 6-pillar bento grid) so Android gives the notification a full vertical window in the notification shade and renders every label, number, and progress bar at native screen DPI without bitmap blur.
  3. **Widget-Exclusive Step Actuators (`+250`, `+500`, `+1,000 Steps`)**: Upgrades the Home Screen Widget dock with a dual-row or segmented tactile action bar supporting `+250 STEPS`, `+500 STEPS`, and `+1,000 STEPS` plus `+250ML H₂O`, `+500ML H₂O`, and `OPEN HUD`.
  4. **Sculpted Quick Action Engine**: Streamlines the Dashboard Quick Action Engine and Settings Widget Studio Card with recessed obsidian tracks, top-lit specular borders, unified category/collapse controls, and expressive macro badges.
- **Target Audience / Persona**: High-performance athletes and daily protocol trackers who rely on zero-friction home screen widgets, glanceable status-bar telemetry, and bulletproof local backup vaults.
- **Key Value**: Eliminates blurry/cropped notification windows, prevents competing background notifications, surfaces exact backup file locations and health metrics, and makes logging steps from the Home Screen widget effortless.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Settings Backup Vault & Default Location Flow**:
   - User scrolls to **Protocol Backup & Auto-Vault** in Settings.
   - Tapping **Default Folder** sets the destination to the internal BioSync vault, highlights the active pill in Bio-Emerald/Platinum, and opens/reveals the **Default Vault Location Inspector** showing the exact file path, current `.json` file size in KB, last overwrite timestamp, and a **Copy Path** button.
   - Tapping **Choose Own Folder** launches the Android directory picker (`OPEN_DOCUMENT_TREE`) to bind a user-selected folder and overwrites `biosync_autobackup_vault.json` in place.
2. **Home Screen Widget Step & Hydration Logging Flow**:
   - On the Android Home Screen Widget (and live in-app preview), the bottom action dock provides **Widget-Only Step Controls** (`+250 STEPS`, `+500 STEPS`, `+1K STEPS`) alongside `+250ML H₂O`, `+500ML H₂O`, and `OPEN HUD`.
   - Tapping any step button immediately increments today's step count, records a timeline entry, re-evaluates the 6-pillar streak and 2-day buffer, refreshes the widget, and updates the live notification.
3. **6-Pillar Live Telemetry Notification Flow**:
   - Pulling down the Android notification shade displays the **BioSync 6-Pillar Live Telemetry HUD**.
   - **Collapsed View**: Shows streak status, overall readiness percentage, and compact 6-pillar summary with razor-sharp native text.
   - **Expanded View**: Opens a generous, full-height 6-pillar bento grid (`CALORIES`, `PROTEIN`, `STEPS`, `CARBS`, `FATS`, `HYDRATION`) with native progress tracks, targets, percentages, and 1-tap hydration/open actions (**no step buttons in notifications**).
4. **Quick Action Engine Flow**:
   - On the Dashboard, the **Quick Actions Engine** displays a single unified 3D command bar (`+ ADD`, category pills `ALL / MEALS / ITEMS`, and quick search/sort toggle) with saved Meals and Single Items open and visible in a sculpted bento grid.

### Visual Identity & Theme
- **Aesthetic Direction**: Architectural 3D Obsidian Monolith — tactile dark surfaces with top-edge specular highlights, recessed telemetry wells, and high-contrast metallic accents.
- **Color Palette & Mood**:
  - `--background`: Deep Obsidian (`#070709` to `#101015`)
  - `--surface-elevated`: Sculpted Graphite (`#14151C` to `#1B1C24`)
  - `--primary`: Pure White (`#FFFFFF`) & Brushed Platinum (`#E4E4E7` / `#D4D4D8`)
  - `--accent-gold`: Warm Gold (`#FFD166` / `#FFB703`) for streak locks & vault badges
  - `--accent-emerald`: Bio-Emerald (`#10B981`) for active auto-backup & step telemetry
  - `--accent-cyan`: Hydration Cyan (`#06B6D4` / `#38BDF8`)
- **Typography & Hierarchy**:
  - `JetBrainsMonoFontFamily` (Bold/ExtraBold, `0.5sp–1.0sp` letter-spacing) for section headers, telemetry readout chips, file paths, and action pills.
  - `InterFontFamily` (SemiBold/ExtraBold) for primary titles, food card names, and numeric callouts.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native XML `RemoteViews` for the 6-Pillar Notification Instead of `BigPictureStyle`**
  - *Chosen Approach*: Build dedicated Android XML notification layouts (`collapsed` + `expanded` 2×3 / 3×2 grid using native `TextView` and `ProgressBar` elements styled in Obsidian dark colors) attached via `setCustomContentView` and `setCustomBigContentView`.
  - *Why*: `NotificationCompat.BigPictureStyle` forces a fixed banner aspect ratio that crops vertical space and rasterizes text into a bitmap that Android resizes, making small numbers look blurry. Native `RemoteViews` `TextView`s render via the system font rasterizer at full screen DPI and grant the full expanded notification height.
- **Decision 2: Unifying Foreground Step Service & 6-Pillar HUD Notification**
  - *Chosen Approach*: Route both `BioSyncStepBackgroundService` and `BioSyncWidgetAndNotificationEngine` through the unified 6-Pillar Live Telemetry builder so any step sensor tick, manual step log, hydration log, or meal log updates the same live notification in real time.
  - *Why*: Fixes the bug where background step updates only refreshed widgets and left the 6-Pillar notification stale or duplicated.
- **Decision 3: Strict Separation of Widget vs. Notification Actions**
  - *Chosen Approach*: Add `ACTION_WIDGET_LOG_STEPS_250`, `ACTION_WIDGET_LOG_STEPS_500`, and `ACTION_WIDGET_LOG_STEPS_1000` exclusively to the Home Screen Widget layout and receiver, while keeping Notification actions strictly limited to `+250ml H₂O`, `+500ml H₂O`, and `Open HUD`.
  - *Why*: Matches the exact user requirement to allow adding steps from widgets only and never from notifications.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram
```
┌────────────────────────────────────────────────────────────────────────────┐
│                        BIOSYNC TELEMETRY & VAULT CORE                      │
│  ┌──────────────────────────┐        ┌──────────────────────────────────┐  │
│  │   DashboardLocalStore    │◄──────►│   BioSyncAutoBackupManager &     │  │
│  │  (Steps, Macros, Water,  │        │   BioSyncAutoBackupScheduler     │  │
│  │   Streak, Saved Foods)   │        │ (Every Log / 1h / 24h / Weekly)  │  │
│  └────────────┬─────────────┘        └─────────────────┬────────────────┘  │
└───────────────┼────────────────────────────────────────┼───────────────────┘
                │                                        │
      ┌─────────┴─────────────────────────┐              ▼
      ▼                                   ▼     ┌────────────────────────────┐
┌───────────────────────────────┐ ┌───────────┐ │ SettingsBackupVaultCard    │
│ BioSyncWidgetAndNotification  │ │ Dashboard │ │ • 3D Vault Telemetry Strip │
│            Engine             │ │ Quick     │ │ • 2×2 Cadence Matrix       │
│ • Native XML Notification     │ │ Action    │ │ • Default Path Inspector & │
│   (Collapsed + Full 6-Pillar) │ │ Engine    │ │   Copyable Readout Modal   │
│   [Water + Open Actions ONLY] │ │ • Unified │ │ • File Size & Health Badge │
│ • Home Screen Widget Renderer │ │   3D Deck │ └────────────────────────────┘
│   [+250 / +500 / +1K Steps +  │ │ • Auto-   │
│    +250ml / +500ml H2O]       │ │   Expand  │
└───────────────────────────────┘ └───────────┘
```

### Interactive Component & State Mapping
1. **Home Screen Widget Step Logging (`BioSyncWidgetActionReceiver`)**:
   - Handles `ACTION_WIDGET_LOG_STEPS_250`, `ACTION_WIDGET_LOG_STEPS_500`, and `ACTION_WIDGET_LOG_STEPS_1000`.
   - Increments `manualStepsToday` in `DashboardLocalStore`, appends a `STEPS` entry to `todayTimelineEntries`, recalculates the 6-pillar streak, triggers `BioSyncAutoBackupManager.onUserLogMutated(context)` (if `EVERY_LOG` is active), refreshes all placed widgets, and updates the live 6-Pillar notification.
2. **Unified Live Telemetry Notification (`BioSyncWidgetAndNotificationEngine` + `BioSyncStepBackgroundService`)**:
   - Populates native `RemoteViews` for all 6 pillars (`CALORIES`, `PROTEIN`, `STEPS`, `CARBS`, `FATS`, `HYDRATION`), streak badge, readiness percentage, and native progress bars.
   - Automatically invoked when `DashboardLocalStore.save(...)` or `BioSyncStepBackgroundService` records new telemetry while live notifications are enabled.
3. **Settings Backup Vault & Default Folder Path Inspector (`SettingsScreen.kt`)**:
   - Clicking **Default Folder** updates `AutoBackupConfig.folderMode = DEFAULT_FOLDER`, reveals the full default file path (`context.filesDir/BioSyncBackups/biosync_autobackup_vault.json`), displays the live file size in KB + integrity status badge, and provides a 1-tap **Copy Path** button and **Inspect Default Location** modal.
4. **Boot Persistence (`BioSyncBootReceiver`)**:
   - Calls `BioSyncAutoBackupScheduler.syncSchedule(context)` alongside starting `BioSyncStepBackgroundService` on `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`.
