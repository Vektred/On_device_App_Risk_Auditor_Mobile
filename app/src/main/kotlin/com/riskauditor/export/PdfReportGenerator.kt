package com.riskauditor.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.riskauditor.core.model.AppFacts
import com.riskauditor.core.model.RiskResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    suspend fun generateReport(results: List<Pair<AppFacts, RiskResult>>): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(24, 30, 48)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val badgePaint = Paint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 45f

        // Document Title
        canvas.drawText("On-Device App Risk Auditor Report", 40f, y, titlePaint)
        y += 18f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())} | Complete Device Scan", 40f, y, subPaint)
        y += 25f

        // Summary Statistics Box
        val total = results.size
        val critical = results.count { it.second.score >= 75 }
        val high = results.count { it.second.score in 50..74 }
        val medium = results.count { it.second.score in 25..49 }
        val low = results.count { it.second.score < 25 }

        canvas.drawText("Audit Summary:", 40f, y, headerPaint)
        y += 16f
        canvas.drawText("Total Apps: $total | Critical: $critical | High: $high | Medium: $medium | Low: $low", 40f, y, textPaint)
        y += 30f

        // Riskiest Applications Section
        canvas.drawText("Top Riskiest Applications", 40f, y, headerPaint)
        y += 20f

        val sorted = results.sortedByDescending { it.second.score }.take(10)

        for ((index, pair) in sorted.withIndex()) {
            if (y > 780f) break

            val (app, risk) = pair

            // Color badge based on band
            badgePaint.color = when {
                risk.score >= 75 -> Color.rgb(220, 38, 38)
                risk.score >= 50 -> Color.rgb(234, 88, 12)
                risk.score >= 25 -> Color.rgb(202, 138, 4)
                else -> Color.rgb(22, 163, 74)
            }

            canvas.drawText("${index + 1}. ${app.appName}", 40f, y, headerPaint)
            canvas.drawText("[Score: ${risk.score}/100 - ${risk.band.title}]", 320f, y, badgePaint)
            y += 14f

            val origin = if (app.isSideloaded) "Sideloaded/ADB" else "Google Play"
            canvas.drawText("Package: ${app.packageName} | Origin: $origin | Target SDK: ${app.targetSdkVersion}", 45f, y, subPaint)
            y += 14f

            val primaryReason = risk.reasons.firstOrNull()?.plainExplanation ?: "No major risk factors detected."
            val truncatedReason = if (primaryReason.length > 95) primaryReason.take(92) + "..." else primaryReason
            canvas.drawText("Primary reason: $truncatedReason", 45f, y, textPaint)
            y += 20f
        }

        document.finishPage(page)

        // Save to cache directory
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val reportFile = File(reportsDir, "AppRiskAudit_${System.currentTimeMillis()}.pdf")

        FileOutputStream(reportFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            reportFile
        )
    }
}

