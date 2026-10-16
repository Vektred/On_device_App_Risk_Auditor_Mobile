package com.riskauditor.ui.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riskauditor.data.diff.DiffResult
import com.riskauditor.ui.theme.RiskCritical
import com.riskauditor.ui.theme.RiskCriticalContainer
import com.riskauditor.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDiffScreen(
    viewModel: ScannerViewModel,
    onAppClick: (String) -> Unit
) {
    val diffs by viewModel.recentDiffs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Diff & History", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (diffs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Baseline Snapshot Established",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No permission escalations or new package changes detected since initial scan. Future updates will automatically appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Detected Package Updates & Escalations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(diffs) { diff ->
                    DiffItemCard(diff = diff, onClick = { onAppClick(diff.packageName) })
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DiffItemCard(diff: DiffResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (diff.isDangerousDiff) RiskCriticalContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = diff.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = diff.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (diff.isDangerousDiff) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dangerous Escalation",
                        tint = RiskCritical
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val scoreDelta = diff.newScore - diff.oldScore
            val deltaPrefix = if (scoreDelta > 0) "+$scoreDelta" else "$scoreDelta"
            Text(
                text = "Score: ${diff.oldScore} → ${diff.newScore} ($deltaPrefix pts) | Version: ${diff.oldVersionCode} → ${diff.newVersionCode}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            if (diff.newlyGrantedPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Newly Granted Permissions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskCritical
                )
                diff.newlyGrantedPermissions.forEach {
                    Text(text = " • $it", fontSize = 11.sp)
                }
            }

            if (diff.newlyEnabledSpecialAccess.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Newly Enabled Special Access:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RiskCritical
                )
                diff.newlyEnabledSpecialAccess.forEach {
                    Text(text = " • $it", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inspect App Details")
            }
        }
    }
}

