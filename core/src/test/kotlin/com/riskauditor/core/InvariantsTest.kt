package com.riskauditor.core

import com.riskauditor.core.analyzer.RiskEngine
import com.riskauditor.core.categorizer.PermissionCategorizer
import com.riskauditor.core.model.*
import org.junit.Assert.*
import org.junit.Test

class InvariantsTest {

    private fun perm(name: String, granted: Boolean): PermissionFact {
        return PermissionFact(
            name = name,
            granted = granted,
            protectionLevel = ProtectionLevel.DANGEROUS,
            category = PermissionCategorizer.categorize(name)
        )
    }

    @Test
    fun testScoreAlwaysWithinZeroAndHundred() {
        // App with almost every dangerous permission and special access
        val extremeApp = AppFacts(
            packageName = "com.evil.everything",
            appName = "Extreme Malware",
            installerPackageName = null,
            targetSdkVersion = 22,
            isDebuggable = true,
            usesCleartextTraffic = true,
            specialAccess = SpecialAccessFact(
                accessibilityEnabled = true,
                deviceAdminActive = true,
                notificationListenerEnabled = true,
                overlayGranted = true,
                installPackagesGranted = true,
                usageStatsGranted = true
            ),
            permissions = listOf(
                perm("android.permission.READ_SMS", true),
                perm("android.permission.RECEIVE_SMS", true),
                perm("android.permission.SEND_SMS", true),
                perm("android.permission.READ_CONTACTS", true),
                perm("android.permission.READ_CALL_LOG", true),
                perm("android.permission.CAMERA", true),
                perm("android.permission.RECORD_AUDIO", true),
                perm("android.permission.ACCESS_FINE_LOCATION", true),
                perm("android.permission.ACCESS_BACKGROUND_LOCATION", true),
                perm("android.permission.READ_PHONE_STATE", true),
                perm("android.permission.MANAGE_EXTERNAL_STORAGE", true),
                perm("android.permission.RECEIVE_BOOT_COMPLETED", true),
                perm("android.permission.INTERNET", true)
            )
        )

        val result = RiskEngine.analyze(extremeApp)
        assertTrue("Score cannot exceed 100", result.score <= 100)
        assertTrue("Score cannot be negative", result.score >= 0)
        assertEquals(100, result.score)
        assertEquals(RiskBand.CRITICAL, result.band)
    }

    @Test
    fun testCleanControlAppScoresZero() {
        val cleanApp = AppFacts(
            packageName = "com.example.clean",
            appName = "Calculator",
            installerPackageName = "com.android.vending",
            targetSdkVersion = 34,
            permissions = emptyList(),
            specialAccess = SpecialAccessFact()
        )

        val result = RiskEngine.analyze(cleanApp)
        assertEquals(0, result.score)
        assertEquals(RiskBand.LOW, result.band)
    }

    @Test
    fun testHighAndCriticalHaveAtLeastThreeReasonsInvariant() {
        // Test high-risk app with multiple factors
        val highApp = AppFacts(
            packageName = "com.example.highrisk",
            appName = "Unknown Utility",
            installerPackageName = null, // +8
            permissions = listOf(
                perm("android.permission.READ_SMS", true), // 15
                perm("android.permission.CAMERA", true),   // 7
                perm("android.permission.ACCESS_FINE_LOCATION", true), // 6
                perm("android.permission.READ_CONTACTS", true), // 8
                perm("android.permission.RECEIVE_BOOT_COMPLETED", true) // 3
            )
        )

        val result = RiskEngine.analyze(highApp)
        assertTrue("High or Critical apps must have score >= 50", result.score >= 50)
        assertTrue(
            "Every High or Critical app must have at least 3 reasons. Found: ${result.reasons.size}",
            result.reasons.size >= 3
        )
    }

    @Test
    fun testMonotonicityAddingPermissionNeverLowersScore() {
        val baseApp = AppFacts(
            packageName = "com.example.base",
            appName = "Base App",
            installerPackageName = "com.android.vending",
            permissions = listOf(
                perm("android.permission.CAMERA", true)
            )
        )

        val baseResult = RiskEngine.analyze(baseApp)

        val expandedApp = baseApp.copy(
            permissions = baseApp.permissions + perm("android.permission.RECORD_AUDIO", true)
        )
        val expandedResult = RiskEngine.analyze(expandedApp)

        assertTrue(
            "Score must be monotonic: ${expandedResult.score} should be >= ${baseResult.score}",
            expandedResult.score >= baseResult.score
        )
    }

    @Test
    fun testDeclaredOnlyScoresLessThanGranted() {
        val declaredOnlyApp = AppFacts(
            packageName = "com.example.declared",
            appName = "Declared App",
            installerPackageName = "com.android.vending",
            permissions = listOf(
                perm("android.permission.READ_SMS", granted = false)
            )
        )
        val grantedApp = AppFacts(
            packageName = "com.example.granted",
            appName = "Granted App",
            installerPackageName = "com.android.vending",
            permissions = listOf(
                perm("android.permission.READ_SMS", granted = true)
            )
        )

        val declaredResult = RiskEngine.analyze(declaredOnlyApp)
        val grantedResult = RiskEngine.analyze(grantedApp)

        assertTrue(
            "Declared-only (${declaredResult.score}) should score less than granted (${grantedResult.score})",
            declaredResult.score < grantedResult.score
        )
    }

    @Test
    fun testBandBoundaries() {
        assertEquals(RiskBand.LOW, RiskBand.fromScore(0))
        assertEquals(RiskBand.LOW, RiskBand.fromScore(24))
        assertEquals(RiskBand.MEDIUM, RiskBand.fromScore(25))
        assertEquals(RiskBand.MEDIUM, RiskBand.fromScore(49))
        assertEquals(RiskBand.HIGH, RiskBand.fromScore(50))
        assertEquals(RiskBand.HIGH, RiskBand.fromScore(74))
        assertEquals(RiskBand.CRITICAL, RiskBand.fromScore(75))
        assertEquals(RiskBand.CRITICAL, RiskBand.fromScore(100))
    }
}

