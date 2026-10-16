# On-Device App Risk Auditor: Scoring Rubric

This scoring rubric defines the deterministic, explainable rule engine used to compute an application's risk score (0–100) and generate plain-language justifications for end users and security auditors.

---

## 1. Core Architecture & Design Choice

* **Scoring Model:** Additive rule engine with per-category caps, combination boosts, and context mismatch modifiers.
* **Single Source of Truth:** Every point awarded is bound to a `Finding(ruleId, points, severity, title, plainExplanation)`. The numerical score and the human-readable explanations can never diverge.
* **Declared vs. Granted:**
  * **Granted (Active):** 100% weight.
  * **Declared Only (Inactive):** 30% weight ($\times 0.3$). Represents latent potential capability without active permission.
* **Risk Score Formula:**
  $$\text{Score} = \min\left(100, \sum \text{CappedCategoryPoints} + \sum \text{ComboBonuses} + \sum \text{MismatchBonuses} + \sum \text{Modifiers}\right)$$

---

## 2. The Nine Permission Categories & Category Caps

| # | Category | Why It Matters | Base Weights (Granted / Declared) | Category Cap |
|---|---|---|---|---|
| **01** | **Special Access** | Highest risk system privileges granted through dedicated system settings screens. Abuse vector for banking trojans. | Accessibility (35/8)<br>Device Admin (30/6)<br>Notification Listener (25/6)<br>Overlay (20/8)<br>Install Unknown Apps (15/8)<br>Usage Stats (10/4) | **60 pts** |
| **02** | **SMS** | Interception of one-time passwords (OTPs), transaction verification codes, premium SMS fraud. | READ_SMS (15/5)<br>SEND_SMS (12/4)<br>RECEIVE_SMS (10/3)<br>RECEIVE_MMS (8/2) | **25 pts** |
| **03** | **Contacts & Call Logs** | Harvesting social graphs for extortion, impersonation, and phishing scams. | READ_CALL_LOG (10/3)<br>READ_CONTACTS (8/2)<br>WRITE_CALL_LOG (6/2)<br>WRITE_CONTACTS (5/2) | **15 pts** |
| **04** | **Camera & Microphone** | Covert audio eavesdropping and unauthorized video recording. | CAMERA (7/2)<br>RECORD_AUDIO (7/2) | **14 pts** |
| **05** | **Location** | Physical real-time tracking, routine stalking, and habit profiling. | ACCESS_BACKGROUND_LOCATION (8/2)<br>ACCESS_FINE_LOCATION (6/2)<br>ACCESS_COARSE_LOCATION (3/1) | **14 pts** |
| **06** | **Phone & Identity** | Hardware device fingerprinting, IMSI/IMEI tracking, rogue calling. | CALL_PHONE (5/2)<br>READ_PHONE_NUMBERS (4/1)<br>READ_PHONE_STATE (3/1) | **10 pts** |
| **07** | **Storage & Media** | Theft of personal photos, identity documents, and ransomware file encryption. | MANAGE_EXTERNAL_STORAGE (12/4)<br>READ_MEDIA_IMAGES (4/1)<br>READ_MEDIA_VIDEO (4/1)<br>READ_MEDIA_AUDIO (4/1)<br>READ_EXTERNAL_STORAGE (4/1) | **12 pts** |
| **08** | **Background & Persistence** | Surviving device reboots and running stealthily in the background. | RECEIVE_BOOT_COMPLETED (3/1)<br>REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (3/1)<br>FOREGROUND_SERVICE (2/1) | **8 pts** |
| **09** | **Network** | The exfiltration channel; low risk alone, high in combination with sensitive permissions. | INTERNET (1/0)<br>ACCESS_NETWORK_STATE (1/0) | **2 pts** |

---

## 3. High-Impact Combination Rules

These combination rules differentiate a true behavioral risk detector from a naive permission counter:

| Rule ID | Combination | Bonus | Threat Scenario & Justification |
|---|---|---|---|
| `COMBO_ACCESSIBILITY_OVERLAY` | Accessibility (declared or enabled) + Screen Overlay (`SYSTEM_ALERT_WINDOW`) | **+25** | **Banking Trojan Pattern:** Detects when banking apps are launched, reads screen content, and projects deceptive credential phishing overlay windows. |
| `COMBO_SMS_EXFILTRATION` | SMS Read/Receive + `INTERNET` + `RECEIVE_BOOT_COMPLETED` (Non-Play install) | **+25** | **Sideloaded OTP Interceptor:** Intercepts 2FA authentication tokens immediately upon device reboot and exfiltrates them off-device. |
| `COMBO_DEVICE_ADMIN_NON_MDM` | Active or Declared Device Admin on non-MDM / non-security package | **+20** | **Anti-Uninstall Persistence:** Prevents ordinary users from uninstalling the application via system settings. |
| `COMBO_NOTIFICATION_EXFILTRATION` | Notification Listener + `INTERNET` | **+20** | **Notification Snooper:** Intercepts sensitive incoming chats (WhatsApp, Signal) and push 2FA codes, uploading them to remote command servers. |
| `COMBO_DROPPER_PATTERN` | `REQUEST_INSTALL_PACKAGES` + `INTERNET` (Sideloaded / Non-Play) | **+20** | **Malware Dropper:** Downloads secondary APK payloads from untrusted remote endpoints and triggers device installation. |
| `COMBO_COVERT_RECORDING` | Camera / Microphone + Boot Persistence + `INTERNET` | **+15** | **Covert Surveillance:** Background spy tool capable of recording ambient audio/video upon startup and transmitting off-device. |
| `COMBO_WORM_SPREADING` | Contacts + SMS (`SEND_SMS` or `READ_SMS`) + `INTERNET` | **+10** | **SMS Worm Propagation:** Scrapes address books and broadcasts phishing SMS messages with malicious download links. |

---

## 4. Context Mismatch Heuristics

Flags applications claiming benign functionality whose requested permissions do not align with their expected profile:

| Archetype | Identifying Keywords | Expected Access Profile | Penalty for Unrelated Sensitive Categories |
|---|---|---|---|
| **Flashlight / Torch** | `torch`, `flashlight`, `flash light` | Camera (flash hardware only) | **+20** for 1st sensitive mismatch (e.g. SMS, Contacts, Location), **+10** for each additional mismatch. |
| **Calculator** | `calculator`, `calc`, `unit converter` | Basic utilities only | **+20** for sensitive permissions (SMS, Contacts, Camera, Special Access). |
| **Wallpaper** | `wallpaper`, `backgrounds`, `lockscreen theme` | Storage, Network | **+20** for SMS, Contacts, Call Logs, or Special Access. |
| **Simple Notes** | `notepad`, `simple notes`, `memo pad` | Storage only | **+20** for SMS, Location, Camera, or Phone State. |
| **QR Scanner** | `qr`, `barcode`, `scanner` | Camera | **+20** for SMS, Contacts, or Special Access. |

---

## 5. Security Modifiers

| Modifier | Condition | Points | Rationale |
|---|---|---|---|
| `MOD_SIDELOADED` | Installer is null (ADB) or `packageinstaller` (non-Google Play / Galaxy Store) | **+8** | Sideloaded apps bypass Google Play Protect automated static and dynamic vetting. |
| `MOD_OLD_TARGET_SDK` | `targetSdkVersion < 28` | **+5** | Legacy targeting allows apps to circumvent modern Android runtime permission restrictions and background limitations. |
| `MOD_DEBUGGABLE` | `FLAG_DEBUGGABLE == true` | **+4** | Exposes application runtime state, memory, and SQLite databases to local debug bridge attachments. |
| `MOD_CLEARTEXT_TRAFFIC` | `FLAG_USES_CLEARTEXT_TRAFFIC == true` and holds `INTERNET` | **+3** | Transmits unencrypted HTTP communications vulnerable to local network interception and MITM attacks. |

---

## 6. Risk Bands & The $\ge 3$ Reasons Invariant

| Score Range | Risk Band | Color Code | Remediation Guidance |
|---|---|---|---|
| **0 – 24** | **Low** | Green (`#16A34A`) | Benign or minimal capabilities. Normal operation. |
| **25 – 49** | **Medium** | Amber (`#CA8A04`) | Elevated permissions. User should review granted permissions. |
| **50 – 74** | **High** | Orange (`#EA580C`) | Dangerous capability combination. Recommend revoking permissions. |
| **75 – 100** | **Critical** | Red (`#DC2626`) | Active Trojan/Spyware pattern or abuse privileges. Immediate remediation or uninstallation urged. |

> **The $\ge 3$ Reasons Invariant:**
> Every application classified as **High** or **Critical** is mathematically guaranteed to display **at least 3 plain-language justifications** in the "Why Flagged" panel. If fewer than 3 combination rules fired, the engine dynamically pads the explanation list with the highest-weighted individual permissions.

