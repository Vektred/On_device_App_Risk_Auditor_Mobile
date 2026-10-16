package com.riskauditor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.riskauditor.R
import com.riskauditor.core.analyzer.RiskEngine
import com.riskauditor.core.model.RiskBand
import com.riskauditor.data.db.AppDatabase
import com.riskauditor.data.diff.PermissionDiffEngine
import com.riskauditor.scanner.AppScanner
import com.riskauditor.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PackageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var appScanner: AppScanner
    private lateinit var diffEngine: PermissionDiffEngine

    private val packageEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val packageName = intent.data?.schemeSpecificPart ?: return

            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                serviceScope.launch {
                    handlePackageChanged(packageName, action == Intent.ACTION_PACKAGE_REPLACED)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScanner = AppScanner(this)
        diffEngine = PermissionDiffEngine(AppDatabase.getInstance(this).appSnapshotDao())

        // Start Foreground Notification
        val notification = buildPersistentNotification()
        startForeground(NOTIFICATION_ID_FOREGROUND, notification)

        // Dynamically register package receiver (required for Android 8+)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageEventReceiver, filter)
    }

    private suspend fun handlePackageChanged(packageName: String, isUpdate: Boolean) {
        val app = appScanner.scanSinglePackage(packageName) ?: return
        val result = RiskEngine.analyze(app)
        val diff = diffEngine.computeDiffAndSave(app, result)

        if (result.band == RiskBand.HIGH || result.band == RiskBand.CRITICAL || diff?.isDangerousDiff == true) {
            val title = if (isUpdate) "App Updated: ${app.appName}" else "New App Installed: ${app.appName}"
            val reason = result.reasons.firstOrNull()?.plainExplanation ?: "Significant risk capabilities detected."
            postRiskAlertNotification(
                id = packageName.hashCode(),
                title = "$title (${result.band.title} Risk: ${result.score})",
                message = reason,
                packageName = packageName
            )
        }
    }

    private fun postRiskAlertNotification(id: Int, title: String, message: String, packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_PACKAGE_NAME", packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }

    private fun buildPersistentNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("App Risk Auditor Active")
            .setContentText("Monitoring newly installed apps and dangerous permission changes in real-time.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "Audit Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status of on-device security audit monitoring"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "High Risk App Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a high or critical risk app is detected"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageEventReceiver)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val CHANNEL_MONITOR = "channel_monitor_service"
        const val CHANNEL_ALERTS = "channel_risk_alerts"
    }
}

