# On-Device App Risk Auditor (PS-01)

[![Android CI](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-brightgreen.svg)]()
[![Pure Kotlin Core](https://img.shields.io/badge/Engine-Pure_Kotlin_(Zero_Android_Deps)-blue.svg)]()
[![Architecture](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-purple.svg)]()
[![Offline First](https://img.shields.io/badge/Privacy-100%25_On--Device_Offline-success.svg)]()

> **Find suspicious Android apps through permissions, special access detection, and an explainable risk scoring engine.**  
> Designed for Hackathon Problem Statement **PS-01: On-Device App Risk Auditor**.

---

## 1. Architectural Overview: The Three-Stage Pipeline

```
  ┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
  │   SCAN (:app / scanner) │  ─►  │  ANALYZE (:core / engine)│  ─►  │   PRESENT (Compose UI)  │
  ├─────────────────────────┤      ├─────────────────────────┤      ├─────────────────────────┤
  │ • QUERY_ALL_PACKAGES    │      │ • Pure Kotlin AppFacts   │      │ • Dashboard Screen (Q1) │
  │ • AppOpsManager Checks  │      │ • 9-Category Weight Caps│      │ • Why Flagged Panel (Q2)│
  │ • Service & Receiver BIND│     │ • 7 Combination Rules   │      │ • Remediation Links (Q3)│
  │ • Sideload / ADB Origin │      │ • Context Mismatches    │      │ • Real-Time Diff Log    │
  └─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
```

### Module Separation
* **`:core` (Pure Kotlin JVM library):**
  Houses the data models (`AppFacts`, `Finding`, `RiskResult`), categorizer, and additive rule engine (`RiskEngine`, `ScoringRubric`). Contains **zero Android imports**, enabling hundreds of unit tests to execute on any development machine in milliseconds without Android Studio or emulators.
* **`:app` (Android Application):**
  Houses the Android hardware/API scanner (`AppScanner`, `SpecialAccessDetector`), Room snapshot database (`PermissionDiffEngine`), foreground monitoring service (`PackageMonitorService`), PDF generator, and modern Jetpack Compose Material 3 UI.

---

## 2. Answering the Three Core Questions

| Question | Where It Is Answered | User-Facing Implementation |
|---|---|---|
| **Q1: Which of my apps can do the most harm?** | **Dashboard & Sorted App List** | Top 5 Riskiest Apps cards, risk score gauges (0–100), risk band badges (Critical, High, Medium, Low), and 9-category device capability summary. |
| **Q2: Why is each one flagged?** | **App Detail: "Why Flagged" Panel** | Plain-language justifications derived directly from rule findings. Guarantees **$\ge 3$ reasons for every High/Critical app**. |
| **Q3: What can I do about it right now?** | **App Detail: Action Center** | Ordered remediation: direct deep-links to Android Settings (Accessibility, Overlays, Unknown Apps, Notification Access, App Info) + safe Uninstall flow with active Device Admin warning protection. |

---

## 3. The Nine Permission Categories

1. **Special access (Highest Weight - Cap 60):** Accessibility Services, Screen Overlay (`SYSTEM_ALERT_WINDOW`), Device Admin, Notification Listener, `REQUEST_INSTALL_PACKAGES`, `PACKAGE_USAGE_STATS`.
2. **SMS (Cap 25):** `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `RECEIVE_MMS`.
3. **Contacts and call logs (Cap 15):** `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALL_LOG`.
4. **Camera and microphone (Cap 14):** `CAMERA`, `RECORD_AUDIO`.
5. **Location (Cap 14):** `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
6. **Phone and identity (Cap 10):** `READ_PHONE_STATE`, `READ_PHONE_NUMBERS`, `CALL_PHONE`.
7. **Storage and media (Cap 12):** `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`.
8. **Background and persistence (Cap 8):** `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`.
9. **Network (Cap 2):** `INTERNET`, `ACCESS_NETWORK_STATE`.

*Full weights, formulas, and category caps are documented in [`SCORING_RUBRIC.md`](SCORING_RUBRIC.md).*

---

## 4. Key Security Capabilities & Combinations

* **Banking Trojan Detection:** Flags simultaneous presence of Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`) and Screen Overlays (`SYSTEM_ALERT_WINDOW`) used to draw fake login overlays over legitimate banking apps (+25 pts).
* **Sideloaded OTP Interceptor:** Flags SMS read/receive combined with `INTERNET` and boot persistence when installed outside Google Play (+25 pts).
* **Dropper Pattern Detection:** Flags sideloaded apps with `REQUEST_INSTALL_PACKAGES` and `INTERNET` (+20 pts).
* **Context Mismatches:** Detects simple utilities (torch, calculator, wallpaper, notes) declaring intrusive privileges (SMS, Contacts, Call Logs) (+20 pts).
* **Permission Escalation Diff (Room DB):** Stores snapshot baselines on each scan. Notifies immediately when an app update adds sensitive permissions.
* **100% Offline & Private:** Zero network requests made by the auditor app; no `android.permission.INTERNET` declared in auditor's manifest.

---

## 5. Three-Minute Live Demo Script (For Hackathon Judges)

### `0:00 – 0:30`: Dashboard Answers Q1
* Launch the app. Explain the **Dashboard** displaying the device-wide risk posture.
* Point out the risk band counters (**Critical, High, Medium, Low**) and the **9-Category Device Capability Grid**.
* Show the **Top 5 Riskiest Apps** section answering Q1: *"Which apps can do the most harm?"*

### `0:30 – 1:30`: Inspecting a Critical App (Answers Q2)
* Tap on the top Critical application.
* Showcase the **Risk Gauge (Score 90 / Critical)** and origin tag (e.g., *Sideloaded / ADB*).
* Walk through the **"Why Flagged" panel**:
  * Show explainable reasons (e.g., *Banking Trojan Pattern: Screen Reading & Overlay*).
  * Demonstrate the **$\ge 3$ reasons invariant** (every High/Critical app has 3+ plain-language reasons).
  * Show the categorized permission list highlighting **Declared vs. Granted** states.

### `1:30 – 2:15`: Remediation in Action (Answers Q3)
* Scroll to the **"What can I do right now?"** panel.
* Tap **Manage Overlay Permission** or **Accessibility Settings** to demonstrate immediate deep-linking into Android's native settings screen.
* Tap **Uninstall Application**. Show the safeguard that detects active Device Administrators and instructs the user to deactivate admin rights first.

### `2:15 – 2:45`: Real-Time Install Monitoring & Permission Diff
* In ADB terminal, install a test APK (`adb install test_torch.apk`).
* Show the real-time high-priority system notification generated by `PackageMonitorService`.
* Open the **Diff Log** tab in the app showing newly escalated permissions compared against the Room baseline snapshot.

### `2:45 – 3:00`: Privacy & Offline Guarantees
* Switch the device to Airplane mode. Re-run scan to demonstrate 100% offline functionality.
* Show `AndroidManifest.xml` confirming zero `INTERNET` permission in the auditor app.
* Tap **Export PDF** to show the shareable on-device audit report.

---

## 6. How to Build & Run

### Prerequisites
* JDK 17+
* Android SDK (API 34, Min API 26)

### Command-Line Build
```bash
# Run pure Kotlin unit tests (Analyzer, Categorizer, Invariants)
./gradlew :core:test

# Build the Debug APK
./gradlew :app:assembleDebug

# Install APK onto connected device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### ADB Testing Commands for Judges
```bash
# 1. Install without installer attribution (simulates sideloaded organizer test-kit)
adb install app.apk

# 2. Grant SMS permissions to simulate active risk
adb shell pm grant <package> android.permission.READ_SMS

# 3. Enable overlay via AppOps
adb shell appops set <package> SYSTEM_ALERT_WINDOW allow
```

