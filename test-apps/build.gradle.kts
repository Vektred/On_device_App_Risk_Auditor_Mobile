plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.riskauditor.testapps"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "archetype"
    productFlavors {
        create("cleanControl") {
            dimension = "archetype"
            applicationId = "com.test.cleancontrol"
            manifestPlaceholders["appLabel"] = "Clean Calculator"
        }
        create("superTorch") {
            dimension = "archetype"
            applicationId = "com.test.supertorch"
            manifestPlaceholders["appLabel"] = "Super Bright Torch"
        }
        create("bankingTrojanSample") {
            dimension = "archetype"
            applicationId = "com.test.bankingtrojan"
            manifestPlaceholders["appLabel"] = "Flash Update Service"
        }
        create("deviceAdminSample") {
            dimension = "archetype"
            applicationId = "com.test.deviceadmin"
            manifestPlaceholders["appLabel"] = "Device Locker"
        }
        create("dropperSample") {
            dimension = "archetype"
            applicationId = "com.test.dropper"
            manifestPlaceholders["appLabel"] = "Plugin Installer"
        }
    }
}

