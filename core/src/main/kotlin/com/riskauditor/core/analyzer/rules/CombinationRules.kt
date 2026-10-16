package com.riskauditor.core.analyzer.rules

import com.riskauditor.core.analyzer.Rule
import com.riskauditor.core.analyzer.ScoringRubric
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding
import com.riskauditor.core.model.Severity

/**
 * Combination rules that elevate risk when complementary capabilities form known attack patterns.
 */
object AccessibilityPlusOverlayRule : Rule {
    override val ruleId = "COMBO_ACCESSIBILITY_OVERLAY"

    override fun evaluate(app: AppFacts): Finding? {
        val hasAccessibility = app.specialAccess.accessibilityEnabled || app.specialAccess.accessibilityDeclared
        val hasOverlay = app.specialAccess.overlayGranted || app.specialAccess.overlayDeclared ||
                app.hasDeclared("SYSTEM_ALERT_WINDOW")

        if (hasAccessibility && hasOverlay) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_ACCESSIBILITY_OVERLAY,
                severity = Severity.CRITICAL,
                title = "Banking Trojan Pattern: Screen Reading & Overlay",
                plainExplanation = "App combines Accessibility service with Screen Overlay permissions. This is the hallmark technique used by Android banking trojans to detect when financial apps open and project fake login phishing screens over them."
            )
        }
        return null
    }
}

object SmsExfiltrationRule : Rule {
    override val ruleId = "COMBO_SMS_EXFILTRATION"

    override fun evaluate(app: AppFacts): Finding? {
        val hasSms = app.hasGranted("READ_SMS") || app.hasGranted("RECEIVE_SMS") ||
                app.hasDeclared("READ_SMS") || app.hasDeclared("RECEIVE_SMS")
        val hasInternet = app.hasGranted("INTERNET") || app.hasDeclared("INTERNET")
        val hasBoot = app.hasGranted("RECEIVE_BOOT_COMPLETED") || app.hasDeclared("RECEIVE_BOOT_COMPLETED")
        val isNotPlay = app.isSideloaded

        if (hasSms && hasInternet && hasBoot && isNotPlay) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_SMS_INTERNET_BOOT_SIDELOAD,
                severity = Severity.CRITICAL,
                title = "Sideloaded OTP Interceptor Pattern",
                plainExplanation = "Sideloaded app can read incoming SMS messages, launch at system boot, and exfiltrate data over the internet. This pattern is commonly used by spyware to intercept One-Time Passwords (OTPs) without user awareness."
            )
        }
        return null
    }
}

object DeviceAdminNonMdmRule : Rule {
    override val ruleId = "COMBO_DEVICE_ADMIN_NON_MDM"

    override fun evaluate(app: AppFacts): Finding? {
        val hasAdmin = app.specialAccess.deviceAdminActive || app.specialAccess.deviceAdminDeclared
        val isKnownSecurity = ScoringRubric.KNOWN_SECURITY_PACKAGES.contains(app.packageName.lowercase())

        if (hasAdmin && !isKnownSecurity) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_DEVICE_ADMIN_NON_MDM,
                severity = Severity.HIGH,
                title = "Suspicious Device Administrator Privilege",
                plainExplanation = "Holds or requests device admin privileges on an application that is not a recognized enterprise mobility management (MDM) or security suite. Often abused to prevent uninstallation."
            )
        }
        return null
    }
}

object NotificationExfiltrationRule : Rule {
    override val ruleId = "COMBO_NOTIFICATION_EXFILTRATION"

    override fun evaluate(app: AppFacts): Finding? {
        val hasNotification = app.specialAccess.notificationListenerEnabled || app.specialAccess.notificationListenerDeclared
        val hasInternet = app.hasGranted("INTERNET") || app.hasDeclared("INTERNET")

        if (hasNotification && hasInternet) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_NOTIFICATION_INTERNET,
                severity = Severity.HIGH,
                title = "Notification Exfiltration Capability",
                plainExplanation = "Combines notification listener access with internet connectivity. Can read incoming messages from any application (including two-factor auth and banking alerts) and transmit them off-device."
            )
        }
        return null
    }
}

object DropperPatternRule : Rule {
    override val ruleId = "COMBO_DROPPER_PATTERN"

    override fun evaluate(app: AppFacts): Finding? {
        val hasInstall = app.specialAccess.installPackagesGranted || app.specialAccess.installPackagesDeclared ||
                app.hasDeclared("REQUEST_INSTALL_PACKAGES")
        val hasInternet = app.hasGranted("INTERNET") || app.hasDeclared("INTERNET")
        val isSideloaded = app.isSideloaded

        if (hasInstall && hasInternet && isSideloaded) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_INSTALL_PACKAGES_SIDELOAD_INTERNET,
                severity = Severity.HIGH,
                title = "Sideloaded Dropper Pattern",
                plainExplanation = "App was installed outside Google Play, has full internet access, and can prompt the installation of arbitrary package files. This structure matches known dropper malware designed to deliver secondary payloads."
            )
        }
        return null
    }
}

object CovertRecordingRule : Rule {
    override val ruleId = "COMBO_COVERT_RECORDING"

    override fun evaluate(app: AppFacts): Finding? {
        val hasCameraOrMic = app.hasGranted("CAMERA") || app.hasGranted("RECORD_AUDIO") ||
                app.hasDeclared("CAMERA") || app.hasDeclared("RECORD_AUDIO")
        val hasBoot = app.hasGranted("RECEIVE_BOOT_COMPLETED") || app.hasDeclared("RECEIVE_BOOT_COMPLETED")
        val hasInternet = app.hasGranted("INTERNET") || app.hasDeclared("INTERNET")

        if (hasCameraOrMic && hasBoot && hasInternet) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_CAMERA_MIC_BOOT_INTERNET,
                severity = Severity.HIGH,
                title = "Persistent Covert Surveillance Capability",
                plainExplanation = "Can access hardware camera/microphone, launch automatically on device boot, and upload data over the network. Characteristic of background spyware and stalkerware."
            )
        }
        return null
    }
}

object WormSpreadingRule : Rule {
    override val ruleId = "COMBO_WORM_SPREADING"

    override fun evaluate(app: AppFacts): Finding? {
        val hasContacts = app.hasGranted("READ_CONTACTS") || app.hasDeclared("READ_CONTACTS")
        val hasSms = app.hasGranted("SEND_SMS") || app.hasDeclared("SEND_SMS") ||
                app.hasGranted("READ_SMS") || app.hasDeclared("READ_SMS")
        val hasInternet = app.hasGranted("INTERNET") || app.hasDeclared("INTERNET")

        if (hasContacts && hasSms && hasInternet) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.COMBO_CONTACTS_SMS_INTERNET,
                severity = Severity.MEDIUM,
                title = "Contact Scraping & SMS Propagation Pattern",
                plainExplanation = "App can harvest your contact address book and transmit SMS messages or upload contacts to remote servers, typical of SMS worms and scam propagation campaigns."
            )
        }
        return null
    }
}

