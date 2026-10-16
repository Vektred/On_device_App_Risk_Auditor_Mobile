package com.riskauditor.core.analyzer.rules

import com.riskauditor.core.analyzer.Rule
import com.riskauditor.core.analyzer.ScoringRubric
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.Finding
import com.riskauditor.core.model.Severity

object SideloadedModifierRule : Rule {
    override val ruleId = "MOD_SIDELOADED"

    override fun evaluate(app: AppFacts): Finding? {
        if (app.isSideloaded && !app.isSystemApp) {
            val sourceText = when (app.installerPackageName) {
                null -> "Direct file installation or ADB transfer"
                else -> app.installerPackageName
            }
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.MODIFIER_SIDELOADED_OR_UNKNOWN,
                severity = Severity.LOW,
                title = "Installed Outside Verified App Store",
                plainExplanation = "App was not installed through an official app store like Google Play ($sourceText). Sideloaded apps lack Google Play Protect automated vetting."
            )
        }
        return null
    }
}

object OldTargetSdkRule : Rule {
    override val ruleId = "MOD_OLD_TARGET_SDK"

    override fun evaluate(app: AppFacts): Finding? {
        if (app.targetSdkVersion < 28) {
            return Finding(
                ruleId = ruleId,
                points = ScoringRubric.MODIFIER_TARGET_SDK_OLD,
                severity = Severity.MEDIUM,
                title = "Outdated Target SDK (API ${app.targetSdkVersion})",
                plainExplanation = "Targets an obsolete Android version (API < 28). Legacy targeting allows applications to bypass modern Android privacy restrictions and background limitations."
            )
        }
        return null
    }
}

object DebuggableBuildRule : Rule {
    override val ruleId = "MOD_DEBUGGABLE"

    override fun evaluate(app: AppFacts): Finding? {
        if (app.isDebuggable) {
            return Finding(
                ruleId = ruleId,
                points = 4,
                severity = Severity.MEDIUM,
                title = "App is Compiled as Debuggable",
                plainExplanation = "FLAG_DEBUGGABLE is set to true. Debuggable applications expose internal memory, variables, and sensitive state to debugging tools."
            )
        }
        return null
    }
}

object CleartextTrafficRule : Rule {
    override val ruleId = "MOD_CLEARTEXT_TRAFFIC"

    override fun evaluate(app: AppFacts): Finding? {
        if (app.usesCleartextTraffic && (app.hasDeclared("INTERNET") || app.hasGranted("INTERNET"))) {
            return Finding(
                ruleId = ruleId,
                points = 3,
                severity = Severity.LOW,
                title = "Allows Unencrypted Cleartext Traffic",
                plainExplanation = "Permits unencrypted HTTP network communication. Any traffic sent over open Wi-Fi can be intercepted and tampered with by third parties."
            )
        }
        return null
    }
}

