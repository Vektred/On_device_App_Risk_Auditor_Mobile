package com.riskauditor.ui.dashboard

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.RiskBand
import com.riskauditor.ui.theme.*
import com.riskauditor.ui.viewmodel.AppAuditItem
import com.riskauditor.ui.viewmodel.ScanUiState
import com.riskauditor.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ScannerViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToAppsList: (RiskBand?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val includeSystem by viewModel.includeSystemApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Risk Auditor", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.triggerScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val uri = viewModel.generatePdfReport()
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Audit PDF"))
                            } else {
                                Toast.makeText(context, "Scan in progress, try again", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ScanUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Auditing permissions and special accesses...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is ScanUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Audit Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.triggerScan() }) {
                            Text("Retry Scan")
                        }
                    }
                }
            }
            is ScanUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // System Apps Toggle Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Include System Apps", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Audit pre-installed OS apps alongside user applications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = includeSystem,
                                    onCheckedChange = { viewModel.setIncludeSystemApps(it) }
                                )
                            }
                        }
                    }

                    // Risk Bands Summary Grid
                    item {
                        val criticalCount = state.allItems.count { it.risk.band == RiskBand.CRITICAL }
                        val highCount = state.allItems.count { it.risk.band == RiskBand.HIGH }
                        val mediumCount = state.allItems.count { it.risk.band == RiskBand.MEDIUM }
                        val lowCount = state.allItems.count { it.risk.band == RiskBand.LOW }

                        Column {
                            Text("Device Risk Posture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RiskBandCard(
                                    band = RiskBand.CRITICAL,
                                    count = criticalCount,
                                    color = RiskCritical,
                                    containerColor = RiskCriticalContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToAppsList(RiskBand.CRITICAL) }
                                )
                                RiskBandCard(
                                    band = RiskBand.HIGH,
                                    count = highCount,
                                    color = RiskHigh,
                                    containerColor = RiskHighContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToAppsList(RiskBand.HIGH) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RiskBandCard(
                                    band = RiskBand.MEDIUM,
                                    count = mediumCount,
                                    color = RiskMedium,
                                    containerColor = RiskMediumContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToAppsList(RiskBand.MEDIUM) }
                                )
                                RiskBandCard(
                                    band = RiskBand.LOW,
                                    count = lowCount,
                                    color = RiskLow,
                                    containerColor = RiskLowContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToAppsList(RiskBand.LOW) }
                                )
                            }
                        }
                    }

                    // Q1 Section: Top 5 Riskiest Apps
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Q1: Top 5 Riskiest Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { onNavigateToAppsList(null) }) {
                                Text("View All (${state.allItems.size})")
                            }
                        }
                    }

                    val top5 = state.allItems.take(5)
                    if (top5.isEmpty()) {
                        item {
                            Text("No apps detected.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        items(top5) { item ->
                            TopRiskyAppCard(item = item, onClick = { onAppClick(item.app.packageName) })
                        }
                    }

                    // Nine Category Device-Wide Summary
                    item {
                        Text(
                            "Device Capability Summary (9 Categories)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Number of installed apps holding capabilities in each risk category:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        CategoryDeviceGrid(categoryCounts = state.categoryDeviceCounts)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBandCard(
    band: RiskBand,
    count: Int,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "${band.title} Risk",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun TopRiskyAppCard(
    item: AppAuditItem,
    onClick: () -> Unit
) {
    val bandColor = when (item.risk.band) {
        RiskBand.CRITICAL -> RiskCritical
        RiskBand.HIGH -> RiskHigh
        RiskBand.MEDIUM -> RiskMedium
        RiskBand.LOW -> RiskLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = item.app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bandColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${item.risk.score} / 100",
                        color = Color.white,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary reason
            val primaryReason = item.risk.reasons.firstOrNull()?.plainExplanation
                ?: "Minimal permissions and risk detected."
            Text(
                text = primaryReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.app.isSideloaded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SideloadedBadgeContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sideloaded / Non-Play Origin",
                            color = SideloadedBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDeviceGrid(categoryCounts: Map<PermissionCategory, Int>) {
    val categories = listOf(
        PermissionCategory.SPECIAL_ACCESS to Icons.Default.Security,
        PermissionCategory.SMS to Icons.Default.Sms,
        PermissionCategory.CONTACTS_AND_CALLS to Icons.Default.Contacts,
        PermissionCategory.CAMERA_AND_MIC to Icons.Default.CameraAlt,
        PermissionCategory.LOCATION to Icons.Default.LocationOn,
        PermissionCategory.PHONE_AND_IDENTITY to Icons.Default.Phone,
        PermissionCategory.STORAGE_AND_MEDIA to Icons.Default.Folder,
        PermissionCategory.BACKGROUND_AND_PERSISTENCE to Icons.Default.Sync,
        PermissionCategory.NETWORK to Icons.Default.Wifi
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in categories.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((category, icon) in row) {
                    val count = categoryCounts[category] ?: 0
                    CategoryGridItem(
                        category = category,
                        icon = icon,
                        count = count,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    category: PermissionCategory,
    icon: ImageVector,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count apps",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = category.displayName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

