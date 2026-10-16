package com.riskauditor.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riskauditor.core.model.*
import com.riskauditor.ui.theme.*
import com.riskauditor.ui.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val item = viewModel.getAppByPackage(packageName)
    var showDeviceAdminDialog by remember { mutableStateOf(false) }

    if (showDeviceAdminDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceAdminDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = "Security Alert", tint = RiskCritical) },
            title = { Text("Device Administrator Active", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Android will reject uninstallation while Device Admin privileges are active. " +
                            "Please open Security Settings, deactivate this app as a Device Administrator, and then proceed with uninstallation."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeviceAdminDialog = false
                    safeLaunchIntent(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
                }) {
                    Text("Open Security Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceAdminDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.app?.appName ?: "App Risk Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("App facts not found for $packageName")
            }
            return@Scaffold
        }

        val app = item.app
        val risk = item.risk

        val bandColor = when (risk.band) {
            RiskBand.CRITICAL -> RiskCritical
            RiskBand.HIGH -> RiskHigh
            RiskBand.MEDIUM -> RiskMedium
            RiskBand.LOW -> RiskLow
        }

        val bandContainerColor = when (risk.band) {
            RiskBand.CRITICAL -> RiskCriticalContainer
            RiskBand.HIGH -> RiskHighContainer
            RiskBand.MEDIUM -> RiskMediumContainer
            RiskBand.LOW -> RiskLowContainer
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = app.appName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val installedStr = if (app.firstInstallTime > 0) dateFormat.format(Date(app.firstInstallTime)) else "Unknown"
                        val updatedStr = if (app.lastUpdateTime > 0) dateFormat.format(Date(app.lastUpdateTime)) else "Unknown"

                        Text(text = "Version: ${app.versionName ?: "v${app.versionCode}"} (Code: ${app.versionCode})", fontSize = 12.sp)
                        Text(text = "Target SDK: API ${app.targetSdkVersion} ${if (app.targetSdkVersion < 28) "⚠️ (Legacy bypass risk)" else ""}", fontSize = 12.sp)
                        Text(text = "Installed: $installedStr | Updated: $updatedStr", fontSize = 12.sp)

                        val origin = when {
                            app.installerPackageName == "com.android.vending" -> "Google Play Store"
                            app.installerPackageName == "com.sec.android.app.samsungapps" -> "Galaxy Store"
                            app.installerPackageName == null -> "Sideloaded / ADB direct installation"
                            else -> "Installer: ${app.installerPackageName}"
                        }
                        Text(text = "Source: $origin", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Score Gauge Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bandContainerColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${risk.band.title} Risk",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = bandColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = risk.band.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bandColor)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${risk.score} / 100",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Q2: Why Flagged? Panel
            item {
                Text(
                    text = "Q2: Why is this app flagged?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Plain-language explainable risk reasons produced directly by our audit rules:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(risk.reasons) { finding ->
                FindingReasonCard(finding = finding)
            }

            // Q3: What Can I Do Right Now? Action Buttons Panel
            item {
                Text(
                    text = "Q3: What can I do right now?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Direct deep-links into Android Settings to revoke capabilities or remove the app:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                RemediationActionCard(
                    app = app,
                    onOpenAdminWarning = { showDeviceAdminDialog = true }
                )
            }

            // Declared vs Granted Permissions Grouped by Category
            item {
                Text(
                    text = "Permissions Breakdown by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val categoriesWithPerms = PermissionCategory.values().filter { cat ->
                app.getPermissionsByCategory(cat).isNotEmpty() || (risk.categoryPoints[cat] ?: 0) > 0
            }

            if (categoriesWithPerms.isEmpty()) {
                item {
                    Text(
                        text = "This application requests zero runtime permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(categoriesWithPerms) { cat ->
                    val perms = app.getPermissionsByCategory(cat)
                    val points = risk.categoryPoints[cat] ?: 0
                    CategoryPermissionCard(category = cat, points = points, perms = perms)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FindingReasonCard(finding: Finding) {
    val pillColor = when (finding.severity) {
        Severity.CRITICAL -> RiskCritical
        Severity.HIGH -> RiskHigh
        Severity.MEDIUM -> RiskMedium
        Severity.LOW, Severity.INFO -> RiskLow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = finding.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${finding.points} pts",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = finding.plainExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RemediationActionCard(
    app: AppFacts,
    onOpenAdminWarning: () -> Unit
) {
    val context = LocalContext.current
    val sa = app.specialAccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Special Access remediation deep links
            if (sa.deviceAdminActive) {
                Button(
                    onClick = {
                        safeLaunchIntent(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCritical),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deactivate Device Admin (Required)")
                }
            }

            if (sa.accessibilityEnabled) {
                Button(
                    onClick = {
                        safeLaunchIntent(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Accessibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Accessibility Settings")
                }
            }

            if (sa.overlayGranted || sa.overlayDeclared || app.hasDeclared("SYSTEM_ALERT_WINDOW")) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${app.packageName}")
                        )
                        safeLaunchIntent(context, intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Overlay Permission")
                }
            }

            if (sa.installPackagesGranted || sa.installPackagesDeclared || app.hasDeclared("REQUEST_INSTALL_PACKAGES")) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${app.packageName}")
                        )
                        safeLaunchIntent(context, intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Install Unknown Apps")
                }
            }

            if (sa.notificationListenerEnabled) {
                Button(
                    onClick = {
                        safeLaunchIntent(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Notification Access")
                }
            }

            // General App Settings Page
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${app.packageName}")
                    )
                    safeLaunchIntent(context, intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open System App Info & Permissions")
            }

            // Uninstall Button
            Button(
                onClick = {
                    if (sa.deviceAdminActive) {
                        onOpenAdminWarning()
                    } else {
                        val uninstallIntent = Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse("package:${app.packageName}")
                        )
                        safeLaunchIntent(context, uninstallIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uninstall Application")
            }
        }
    }
}

@Composable
fun CategoryPermissionCard(
    category: PermissionCategory,
    points: Int,
    perms: List<PermissionFact>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Contribution: $points / ${category.maxCategoryCap} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            perms.forEach { perm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.simpleName,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (perm.granted) RiskCriticalContainer else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (perm.granted) "GRANTED (Active)" else "Declared (Inactive)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (perm.granted) RiskCritical else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun safeLaunchIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Settings screen not available on this device", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch settings: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

