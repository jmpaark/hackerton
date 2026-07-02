package com.nbunone.app.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val PAGE_W = 595   // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    /** 마크다운 라이트 텍스트를 간단한 레이아웃의 PDF로 저장 */
    fun export(context: Context, teamName: String, generatedAt: String, content: String): File {
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
        val headPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.rgb(49, 46, 129); isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 10.5f; color = Color.rgb(30, 41, 59); isAntiAlias = true }
        val metaPaint = Paint().apply { textSize = 9f; color = Color.GRAY; isAntiAlias = true }
        val maxWidth = PAGE_W - MARGIN * 2

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        fun newPageIfNeeded(lineHeight: Float) {
            if (y + lineHeight > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f, lineGap: Float = 4f) {
            var remaining = text
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth - indent, null)
                val line = remaining.take(count)
                newPageIfNeeded(paint.textSize + lineGap)
                canvas.drawText(line, MARGIN + indent, y + paint.textSize, paint)
                y += paint.textSize + lineGap
                remaining = remaining.drop(count)
            }
        }

        // 헤더
        canvas.drawText("N분의1 — AI 기여도 리포트", MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 8f
        canvas.drawText("팀: $teamName   ·   생성: $generatedAt", MARGIN, y + metaPaint.textSize, metaPaint)
        y += metaPaint.textSize + 18f

        content.lines().forEach { raw ->
            val line = raw.trimEnd().replace("**", "")
            when {
                line.startsWith("## ") -> { y += 8f; drawWrapped(line.removePrefix("## "), headPaint) }
                line.startsWith("### ") -> { y += 5f; drawWrapped(line.removePrefix("### "), headPaint.apply { }, 0f) }
                line.startsWith("# ") -> { y += 8f; drawWrapped(line.removePrefix("# "), headPaint) }
                line.startsWith("- ") -> drawWrapped("•  " + line.removePrefix("- "), bodyPaint, 8f)
                line.startsWith("> ") -> drawWrapped(line.removePrefix("> "), metaPaint, 8f)
                line.isBlank() -> y += 6f
                else -> drawWrapped(line, bodyPaint)
            }
        }

        doc.finishPage(page)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = teamName.replace(Regex("[\\\\/:*?\"<>|\\s]"), "_")
        val file = File(dir, "기여도리포트_$safeName.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "리포트 PDF 공유"))
    }
}
