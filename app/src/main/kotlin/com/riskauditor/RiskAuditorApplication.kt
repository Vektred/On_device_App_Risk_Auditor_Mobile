package com.riskauditor

import android.app.Application
import android.content.Intent
import android.os.Build
import com.riskauditor.data.db.AppDatabase
import com.riskauditor.service.PackageMonitorService

class RiskAuditorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize database
        AppDatabase.getInstance(this)

        // Automatically start real-time monitoring service
        try {
            val serviceIntent = Intent(this, PackageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Foreground service launch might be restricted until permission granted on Android 14+
        }
    }
}

