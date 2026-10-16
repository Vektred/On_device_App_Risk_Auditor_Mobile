package com.riskauditor.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riskauditor.core.analyzer.RiskEngine
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.PermissionCategory
import com.riskauditor.core.model.RiskBand
import com.riskauditor.core.model.RiskResult
import com.riskauditor.data.db.AppDatabase
import com.riskauditor.data.diff.DiffResult
import com.riskauditor.data.diff.PermissionDiffEngine
import com.riskauditor.export.PdfReportGenerator
import com.riskauditor.scanner.AppScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppAuditItem(
    val app: AppFacts,
    val risk: RiskResult
)

sealed interface ScanUiState {
    object Loading : ScanUiState
    data class Success(
        val allItems: List<AppAuditItem>,
        val filteredItems: List<AppAuditItem>,
        val categoryDeviceCounts: Map<PermissionCategory, Int>,
        val scanTimestamp: Long
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val appScanner = AppScanner(application)
    private val diffEngine = PermissionDiffEngine(AppDatabase.getInstance(application).appSnapshotDao())
    private val pdfGenerator = PdfReportGenerator(application)

    private val _rawState = MutableStateFlow<ScanUiState>(ScanUiState.Loading)
    val rawState: StateFlow<ScanUiState> = _rawState.asStateFlow()

    val includeSystemApps = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val selectedBand = MutableStateFlow<RiskBand?>(null)
    val selectedCategory = MutableStateFlow<PermissionCategory?>(null)

    private val _recentDiffs = MutableStateFlow<List<DiffResult>>(emptyList())
    val recentDiffs: StateFlow<List<DiffResult>> = _recentDiffs.asStateFlow()

    val uiState: StateFlow<ScanUiState> = combine(
        _rawState,
        searchQuery,
        selectedBand,
        selectedCategory
    ) { state, query, band, category ->
        if (state !is ScanUiState.Success) return@combine state

        val filtered = state.allItems.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.app.appName.contains(query, ignoreCase = true) ||
                    item.app.packageName.contains(query, ignoreCase = true)

            val matchesBand = band == null || item.risk.band == band

            val matchesCategory = category == null ||
                    (item.risk.categoryPoints[category] ?: 0) > 0 ||
                    item.app.permissions.any { it.category == category }

            matchesQuery && matchesBand && matchesCategory
        }

        state.copy(filteredItems = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanUiState.Loading)

    init {
        triggerScan()
    }

    fun triggerScan() {
        viewModelScope.launch {
            _rawState.value = ScanUiState.Loading
            try {
                val scannedApps = appScanner.scanAllApps(includeSystemApps.value)
                val diffs = mutableListOf<DiffResult>()

                val auditItems = scannedApps.map { facts ->
                    val riskResult = RiskEngine.analyze(facts)
                    val diff = diffEngine.computeDiffAndSave(facts, riskResult)
                    if (diff != null) diffs.add(diff)
                    AppAuditItem(facts, riskResult)
                }.sortedByDescending { it.risk.score }

                _recentDiffs.value = diffs

                // Compute 9-category device-wide counts
                val counts = mutableMapOf<PermissionCategory, Int>()
                for (cat in PermissionCategory.values()) {
                    counts[cat] = auditItems.count { item ->
                        (item.risk.categoryPoints[cat] ?: 0) > 0 ||
                                item.app.permissions.any { it.category == cat }
                    }
                }

                _rawState.value = ScanUiState.Success(
                    allItems = auditItems,
                    filteredItems = auditItems,
                    categoryDeviceCounts = counts,
                    scanTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _rawState.value = ScanUiState.Error(e.localizedMessage ?: "Unknown scanning error occurred")
            }
        }
    }

    fun setIncludeSystemApps(include: Boolean) {
        if (includeSystemApps.value != include) {
            includeSystemApps.value = include
            triggerScan()
        }
    }

    fun getAppByPackage(packageName: String): AppAuditItem? {
        val current = _rawState.value
        return if (current is ScanUiState.Success) {
            current.allItems.find { it.app.packageName == packageName }
        } else null
    }

    suspend fun generatePdfReport(): Uri? {
        val current = _rawState.value
        if (current !is ScanUiState.Success) return null
        val pairs = current.allItems.map { it.app to it.risk }
        return pdfGenerator.generateReport(pairs)
    }
}

