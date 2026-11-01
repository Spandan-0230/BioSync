<div align="center">

# ⚡ BioSync
### Precision Biometric Telemetry, Metabolic Protocol Engine & Live Activity Tracker for Android

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/APK-GitHub%20Releases-00E676?style=for-the-badge&logo=github&logoColor=black)](../../releases)

**BioSync** is a local-first, high-precision personal health and metabolic optimization application for Android. Engineered with a cyber-biometric dark interface, BioSync transforms raw physiological metrics into an actionable daily blueprint—combining a mathematically rigorous **Daily Targets Engine**, **Live Background Step Telemetry (without opening Google Fit)**, **Macronutrient & Hydration Chambers**, and **Circadian Protocol Scheduling**.

[📲 **Download Latest APK**](../../releases) • [🧮 **Mathematical Engine**](#-daily-targets-engine--mathematical-models) • [🔄 **Live Step Sync Architecture**](#-live-step-sync--background-telemetry-architecture) • [📂 **Project Structure**](#-project-architecture--file-structure)

</div>

---

## 📲 Download & Installation (APK)

A pre-compiled, ready-to-install Android package (`.apk`) is available directly in the **Releases** section of this repository.

1. Navigate to the [**GitHub Releases**](../../releases) page.
2. Download the latest `BioSync.apk` asset under the latest release tag.
3. Open the downloaded `.apk` file on your Android device (Android $8.0+$ / API $26+$ recommended).
4. If prompted, allow installation from unknown sources.
5. On first launch, grant **Activity Recognition** (for live hardware step counting) and **Notification** permissions (for background telemetry & protocol reminders) to unlock the full BioSync experience.

---

## ✨ Comprehensive Feature Breakdown

### 1. 🧬 Multi-Stage Biometric Onboarding & Calibration
BioSync replaces generic calorie calculators with a structured, three-phase physiological calibration pipeline:
* **Personal Details (`PersonalDetailsScreen.kt`)**:
  * Captures foundational biometrics including biological sex, age, height (supports dual unit conversion between $\text{cm}$ and $\text{ft/in}$), total body weight ($\text{kg}$ / $\text{lbs}$), and optional body fat percentage ($\%$) for lean-mass-aware metabolic calculations.
* **Profile Calibration (`ProfileCalibrationScreen.kt`)**:
  * Fine-tunes daily energy expenditure through granular lifestyle activity tiers, weekly structured training frequency, and primary body composition goals (e.g., Aggressive Cut, Lean Recomposition, Maintenance, Lean Bulk).
* **Interactive Protocol Summary (`ProtocolSummaryScreen.kt` & `ProtocolSummaryModals.kt`)**:
  * Generates an immediate, auditable physiological protocol sheet before entering the main dashboard.
  * Includes interactive **Deep-Dive Modals** that explain the exact scientific rationale, metabolic equations, and safety bounds behind every calculated target.

### 2. 📊 Live Telemetry Dashboard & Quick-Action Logging
The central command center (`DashboardScreen.kt`, `DashboardModelsAndStore.kt`, `DashboardQuickActionsComponents.kt`) provides zero-latency visibility into your daily biological state:
* **Caloric & Macro Telemetry Chambers**:
  * High-contrast visual progress rings and segmented chambers tracking real-time intake against calculated daily targets for **Kilocalories ($\text{kcal}$)**, **Protein ($\text{g}$)**, **Carbohydrates ($\text{g}$)**, and **Dietary Fats ($\text{g}$)**.
* **Rapid Quick-Action Logging**:
  * One-tap bottom sheets and quick-increment controls (`DashboardQuickActionsComponents.kt`) to log meals, custom macro splits, hydration vessels, and activity bursts in under $3$ seconds.
* **Hydration Matrix**:
  * Tracks daily fluid intake ($\text{mL}$ / $\text{L}$) with dynamic adjustments for physical activity and sweat rate.
* **Protocol Streak & Compliance Scoring**:
  * Tracks consecutive days of protocol adherence with visual streak badges and daily completion percentages.

### 3. 👣 Zero-Friction Live Step Sync & Background Telemetry
Most fitness apps suffer from stale step counts unless the user manually opens third-party companion apps like Google Fit. BioSync solves this with a custom hybrid architecture (`BioSyncHealthStepManager.kt` & `BioSyncStepBackgroundService.kt`):
* **Direct Hardware Sensor Fusion**: Reads real-time step events directly from the device's low-power hardware pedometer (`Sensor.TYPE_STEP_COUNTER` and `Sensor.TYPE_STEP_DETECTOR`).
* **Autonomous Background Service**: Runs a lightweight, battery-efficient foreground/background service (`BioSyncStepBackgroundService.kt`) that captures steps continuously even when the app is swiped away or the screen is locked.
* **Seamless Google Fit / Health API Sync**: Automatically reconciles local hardware sensor deltas with Google Fit / Health Connect APIs in the background—updating your live dashboard and home screen widget **without ever requiring you to open Google Fit manually**.

### 4. 🗺️ The BioSync Blueprint (`BlueprintScreen.kt` & `BlueprintExtendedSections.kt`)
More than just numbers, the **Blueprint** tab translates your daily targets into a structured 24-hour physiological protocol:
* **Circadian & Meal Timing Windows**: Actionable recommendations for pre-workout fueling, post-workout protein synthesis windows, and evening metabolic wind-down.
* **Extended Protocol Sections**: Deep-dive cards covering micronutrient priorities, electrolyte balance, sleep hygiene alignment, and training recovery metrics.

### 5. 📈 Historical Analytics & Trend Telemetry (`StatsScreen.kt`)
* **Multi-Day & Weekly Visualizations**: Interactive charts and tabular readouts comparing actual intake vs. target baselines across calories, macronutrient ratios, water consumption, and daily step volume.
* **Consistency & Variance Auditing**: Identifies metabolic drift and weekend adherence gaps at a glance.

### 6. 📱 Custom Android Home Screen Widget
BioSync includes a dedicated, cyber-styled Home Screen Widget (`res/drawable/widget_*.xml`) featuring:
* **Multi-Chamber Progress Bars**: Dedicated visual indicators for $\text{kcal}$, Protein, Carbs, Fats, Water, and Steps.
* **Live Streak Flame Badge**: Displays your active protocol streak right on your launcher.
* **One-Tap Quick Action Button**: Launch directly into rapid logging from your home screen.

### 7. 📳 Precision Haptics, Smart Notifications & Local-First Storage
* **Tactile Haptic Engine (`BioSyncHaptics.kt`)**: Custom vibration waveforms and tick feedback when scrubbing calibration sliders, hitting daily macro thresholds, or logging hydration.
* **Day-One & Protocol Notifications (`BioSyncDayOneNotificationHelper.kt`)**: Intelligent, non-intrusive reminders for onboarding completion, hydration checkpoints, and daily streak protection.
* **100% Private Local Persistence (`BioSyncLocalStore.kt`)**: All biometric profiles, calibration parameters, daily logs, and historical telemetry are stored locally on-device with zero mandatory cloud accounts or external data selling.

---

## 🧮 Daily Targets Engine & Mathematical Models

Implemented in `DailyTargetsEngine.kt` (and documented in `Daily_Targets_Engine___Mathematical_Calculation_Report.pdf`), BioSync computes individualized physiological targets using peer-reviewed metabolic equations:

### 1. Basal Metabolic Rate ($\text{BMR}$)
* **When Body Fat Percentage ($BF\%$) is Known (Katch-McArdle Equation)**:
  First, Lean Body Mass ($\text{LBM}$) in $\text{kg}$ is derived from total weight $W$ ($\text{kg}$):
  $$\text{LBM} = W \times \left(1 - \frac{BF\%}{100}\right)$$
  $$\text{BMR} = 370 + \left(21.6 \times \text{LBM}\right)$$

* **Standard Biometric Mode (Mifflin-St Jeor Equation)**:
  For weight $W$ ($\text{kg}$), height $H$ ($\text{cm}$), and age $A$ (years):
  * **Male**:
    $$\text{BMR}_{\text{male}} = (10 \times W) + (6.25 \times H) - (5 \times A) + 5$$
  * **Female**:
    $$\text{BMR}_{\text{female}} = (10 \times W) + (6.25 \times H) - (5 \times A) - 161$$

### 2. Total Daily Energy Expenditure ($\text{TDEE}$) & Goal Target
$\text{BMR}$ is scaled by a calibrated Physical Activity Level ($\text{PAL}$) multiplier $\alpha \in [1.2, 1.9]$ and adjusted by the selected protocol delta $\Delta_{\text{goal}}$:
$$\text{TDEE} = \text{BMR} \times \alpha$$
$$E_{\text{target}} = \text{TDEE} + \Delta_{\text{goal}}$$

### 3. Macronutrient Partitioning
Macronutrients are allocated using lean-mass-preserving protein floors and essential lipid thresholds rather than arbitrary fixed percentages:
* **Protein ($P$)**: Scaled between $1.8\text{ g/kg}$ and $2.4\text{ g/kg}$ of body weight (or $\text{LBM}$ during caloric deficits), where $1\text{ g Protein} = 4\text{ kcal}$.
* **Dietary Fat ($F$)**: Set to support endocrine function ($0.8\text{ g/kg} - 1.0\text{ g/kg}$), where $1\text{ g Fat} = 9\text{ kcal}$.
* **Carbohydrates ($C$)**: Assigned the remaining energetic budget for glycolytic and training performance, where $1\text{ g Carbohydrate} = 4\text{ kcal}$:
  $$C_{\text{grams}} = \frac{E_{\text{target}} - (4 \times P_{\text{grams}}) - (9 \times F_{\text{grams}})}{4}$$

### 4. Hydration & Step Baselines
* **Daily Hydration ($V_{\text{water}}$)**:
  $$V_{\text{water}}\text{ (mL)} = (W \times 35) + \beta_{\text{activity}}$$
  where $\beta_{\text{activity}}$ accounts for training frequency and active sweat loss.

---

## 🔄 Live Step Sync & Background Telemetry Architecture

As detailed in the repository's engineering whitepapers (`BioSync_Live_Google_Fit_Sync_Without_Opening_Google_Fit.pdf`), BioSync implements a resilient **3-Layer Step Pipeline**:

```
┌─────────────────────────────────────────────────────────────┐
│                Hardware Pedometer Sensor                    │
│     (TYPE_STEP_COUNTER / TYPE_STEP_DETECTOR)                │
└──────────────────────────────┬──────────────────────────────┘
                               │ Real-time hardware interrupts
                               ▼
┌─────────────────────────────────────────────────────────────┐
│             BioSyncStepBackgroundService.kt                 │
│  • Computes midnight baseline offset & daily step delta     │
│  • Persists real-time increments to BioSyncLocalStore       │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│  BioSyncHealthStepManager.kt │ │  Dashboard & Home Widget   │
│  • Background Google Fit /   │ │  • Instant UI & Progress   │
│    Health API reconciliation │ │    Chamber updates         │
└──────────────────────────────┘ └────────────────────────────┘
```

* **Midnight Rollover Handling**: Because `Sensor.TYPE_STEP_COUNTER` reports cumulative steps since the last device reboot, `BioSyncLocalStore` records a daily anchor baseline at $00:00$ local time and accurately computes $\text{Steps}_{\text{today}} = \text{Steps}_{\text{sensor}} - \text{Steps}_{\text{anchor}}$ across app restarts and device reboots.

---

## 🎨 Design System & Typography

BioSync is crafted around a bespoke **Dark Biometric Telemetry** design language (`DESIGN_76b5e704.md` & `ui/theme/`):
* **Color Palette (`Color.kt`)**: Deep obsidian/carbon backgrounds (`#0A0D12` / `#11161F`) paired with high-visibility semantic neon accents for distinct biological channels (Metabolic Emerald, Protein Cyan, Carb Amber, Lipid Violet, and Hydration Azure).
* **Dual-Font Hierarchy (`res/font/`)**:
  * **Inter (`inter.ttf`)**: Used for clean, high-legibility UI headers, protocol descriptions, and navigation labels.
  * **JetBrains Mono (`jetbrains_mono.ttf`)**: Enforced for all numeric readouts, macro counters, percentages, and tabular telemetry so digits never jitter during live updates.

---

## 📂 Project Architecture & File Structure

```text
BioSync/
├── app/
│   ├── build.gradle.kts                         # App-level Gradle configuration & Compose dependencies
│   ├── proguard-rules.pro                       # R8 / ProGuard optimization rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml              # Permissions, MainActivity & Background Step Service declarations
│           ├── java/com/example/
│           │   ├── MainActivity.kt              # Root navigation host, permission orchestration & lifecycle sync
│           │   ├── PersonalDetailsScreen.kt     # Phase 1 Onboarding: Biometrics & unit conversion
│           │   ├── ProfileCalibrationScreen.kt  # Phase 2 Onboarding: Activity multipliers & goal selection
│           │   ├── ProtocolSummaryScreen.kt     # Phase 3 Onboarding: Calculated targets overview
│           │   ├── ProtocolSummaryModals.kt     # Scientific explanation modals & formula breakdowns
│           │   ├── DailyTargetsEngine.kt        # Core BMR, TDEE, Macro, Hydration & Step calculation engine
│           │   ├── DashboardScreen.kt           # Primary live telemetry UI, rings, macro bars & logs
│           │   ├── DashboardModelsAndStore.kt   # State holders and reactive data models for the dashboard
│           │   ├── DashboardQuickActionsComponents.kt # Rapid meal, macro, water & step logging components
│           │   ├── BlueprintScreen.kt           # Daily circadian protocol & optimization schedule
│           │   ├── BlueprintExtendedSections.kt # Extended physiological guidance & recovery modules
│           │   ├── StatsScreen.kt               # Historical charts, adherence streaks & trend analytics
│           │   ├── SettingsScreen.kt            # Recalibration triggers, sensor toggles & data management
│           │   ├── BioSyncHealthStepManager.kt  # Live step sensor & Google Fit / Health sync coordinator
│           │   ├── BioSyncStepBackgroundService.kt # Persistent service for 24/7 step counting
│           │   ├── BioSyncLocalStore.kt         # On-device persistence for profiles, logs & daily anchors
│           │   ├── BioSyncDayOneNotificationHelper.kt # Protocol & onboarding notification manager
│           │   ├── BioSyncHaptics.kt            # Custom tactile feedback & vibration controller
│           │   └── ui/theme/
│           │       ├── Color.kt                 # Biometric dark theme color tokens
│           │       ├── Theme.kt                 # Material 3 Compose theme wrapper
│           │       └── Type.kt                  # Inter & JetBrains Mono typography definitions
│           └── res/
│               ├── drawable/                    # Vector icons & Home Screen Widget progress drawables
│               ├── font/                        # Embedded Inter & JetBrains Mono TrueType fonts
│               ├── mipmap-*/                    # Adaptive launcher icons
│               └── values/                      # Strings, colors & base XML themes
├── gradle/
│   └── libs.versions.toml                       # Centralized Gradle Version Catalog
├── public/assets/aistudio/                      # Architectural PDFs, design specs & UI reference screens
├── build.gradle.kts                             # Root Gradle build script
└── settings.gradle.kts                          # Project module settings
```

---

## 🛠️ Building from Source

If you prefer to build **BioSync** from source rather than installing the pre-built APK from [GitHub Releases](../../releases):

### Prerequisites
* **Android Studio** Ladybug ($2024.2.1$) or newer
* **JDK** $17$ or higher
* **Android SDK** API Level $34+$ (Minimum SDK: $26$)

### Build Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/<your-username>/BioSync.git
   cd BioSync
   ```
2. **Build Debug APK via Gradle Wrapper**:
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. **Run Unit & Robolectric Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🔒 Privacy & Permissions

BioSync is designed with a **strict local-first privacy model**:
* `android.permission.ACTIVITY_RECOGNITION`: Required to read real-time step counts from your device's hardware pedometer sensor.
* `android.permission.FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_HEALTH`: Ensures uninterrupted step synchronization in the background without requiring third-party apps to remain open.
* `android.permission.POST_NOTIFICATIONS`: Used for optional live telemetry status and daily protocol reminders.

---

<div align="center">

**Built with precision for human performance optimization.**  
⭐ *If you find BioSync useful, consider starring the repository and downloading the latest [Release APK](../../releases)!*

</div>