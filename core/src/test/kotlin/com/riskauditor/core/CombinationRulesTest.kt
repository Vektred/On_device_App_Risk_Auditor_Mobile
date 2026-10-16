package com.riskauditor.core

import com.riskauditor.core.analyzer.RiskEngine
import com.riskauditor.core.categorizer.PermissionCategorizer
import com.riskauditor.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CombinationRulesTest {

    private fun makePerm(name: String, granted: Boolean = true): PermissionFact {
        return PermissionFact(
            name = name,
            granted = granted,
            protectionLevel = ProtectionLevel.DANGEROUS,
            category = PermissionCategorizer.categorize(name)
        )
    }

    @Test
    fun testAccessibilityPlusOverlayCombination() {
        val app = AppFacts(
            packageName = "com.fake.banktrojan",
            appName = "FlashPlayer",
            specialAccess = SpecialAccessFact(
                accessibilityEnabled = true,
                overlayGranted = true
            )
        )

        val result = RiskEngine.analyze(app)
        assertEquals(RiskBand.CRITICAL, result.band)
        assertTrue(result.score >= 75)
        assertTrue(result.reasons.any { it.ruleId == "COMBO_ACCESSIBILITY_OVERLAY" })
    }

    @Test
    fun testSmsExfiltrationOtpStealer() {
        val app = AppFacts(
            packageName = "com.untrusted.sms",
            appName = "Free Recharge",
            installerPackageName = null, // Sideloaded / adb install
            permissions = listOf(
                makePerm("android.permission.RECEIVE_SMS", granted = true),
                makePerm("android.permission.INTERNET", granted = true),
                makePerm("android.permission.RECEIVE_BOOT_COMPLETED", granted = true)
            )
        )

        val result = RiskEngine.analyze(app)
        assertTrue(result.score >= 50)
        assertTrue(result.reasons.any { it.ruleId == "COMBO_SMS_EXFILTRATION" })
    }

    @Test
    fun testDropperPatternSideloaded() {
        val app = AppFacts(
            packageName = "com.fake.appmarket",
            appName = "Super Market",
            installerPackageName = "com.google.android.packageinstaller", // Sideloaded
            specialAccess = SpecialAccessFact(
                installPackagesGranted = true
            ),
            permissions = listOf(
                makePerm("android.permission.INTERNET", granted = true)
            )
        )

        val result = RiskEngine.analyze(app)
        assertTrue(result.reasons.any { it.ruleId == "COMBO_DROPPER_PATTERN" })
    }

    @Test
    fun testNotificationListenerWithInternet() {
        val app = AppFacts(
            packageName = "com.notify.stealer",
            appName = "Status Saver",
            specialAccess = SpecialAccessFact(
                notificationListenerEnabled = true
            ),
            permissions = listOf(
                makePerm("android.permission.INTERNET", granted = true)
            )
        )

        val result = RiskEngine.analyze(app)
        assertTrue(result.reasons.any { it.ruleId == "COMBO_NOTIFICATION_EXFILTRATION" })
    }

    @Test
    fun testCovertRecordingSpyware() {
        val app = AppFacts(
            packageName = "com.spy.record",
            appName = "Secret Monitor",
            permissions = listOf(
                makePerm("android.permission.RECORD_AUDIO", granted = true),
                makePerm("android.permission.INTERNET", granted = true),
                makePerm("android.permission.RECEIVE_BOOT_COMPLETED", granted = true)
            )
        )

        val result = RiskEngine.analyze(app)
        assertTrue(result.reasons.any { it.ruleId == "COMBO_COVERT_RECORDING" })
    }

    @Test
    fun testContextMismatchFlashlightWithSms() {
        val app = AppFacts(
            packageName = "com.simple.torch",
            appName = "Super Bright Flashlight",
            permissions = listOf(
                makePerm("android.permission.CAMERA", granted = true),
                makePerm("android.permission.READ_SMS", granted = true),
                makePerm("android.permission.READ_CONTACTS", granted = true)
            )
        )

        val result = RiskEngine.analyze(app)
        assertTrue(result.reasons.any { it.ruleId == "CONTEXT_MISMATCH" })
    }
}

