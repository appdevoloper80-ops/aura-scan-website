package com.yourname.aurascan.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.yourname.aurascan.domain.model.PdfMargin
import com.yourname.aurascan.domain.model.PdfPageSize
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PdfGenerator {

    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842
    private const val MAX_FIT_PAGE_DIMENSION_PT = 1600
    private const val MAX_DECODE_DIMENSION_PX = 1800

    data class GeneratedPdfResult(
        val pdfFile: File,
        val pageImageFiles: List<File>
    )

    fun createPdfFromImageFiles(
        sourceFiles: List<File>,
        outputDir: File,
        pageSize: PdfPageSize,
        margin: PdfMargin
    ): GeneratedPdfResult {
        require(sourceFiles.isNotEmpty()) { "At least one image is required to create a PDF." }
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val pdfFile = File(outputDir, "document.pdf")
        val savedPageFiles = mutableListOf<File>()
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            sourceFiles.forEachIndexed { index, sourceFile ->
                val bitmap = BitmapUtils.decodeSampledBitmap(sourceFile, MAX_DECODE_DIMENSION_PX)
                    ?: throw IllegalStateException("Unable to decode image ${index + 1}")

                try {
                    val pageImageFile = File(
                        outputDir,
                        "page_%03d.jpg".format(index + 1)
                    )
                    FileOutputStream(pageImageFile).use { imgOut ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, imgOut)
                    }
                    savedPageFiles.add(pageImageFile)

                    val marginPt = margin.points
                    val (pageWidth, pageHeight) = computePageDimensions(
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height,
                        pageSize = pageSize,
                        marginPt = marginPt
                    )

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        index + 1
                    ).create()

                    val page = pdfDocument.startPage(pageInfo)
                    try {
                        val canvas = page.canvas
                        canvas.drawColor(Color.WHITE)

                        val availableW = (pageWidth - marginPt * 2).coerceAtLeast(1).toFloat()
                        val availableH = (pageHeight - marginPt * 2).coerceAtLeast(1).toFloat()

                        val scale = min(
                            availableW / bitmap.width.toFloat(),
                            availableH / bitmap.height.toFloat()
                        )
                        val drawW = bitmap.width * scale
                        val drawH = bitmap.height * scale
                        val left = marginPt + (availableW - drawW) / 2f
                        val top = marginPt + (availableH - drawH) / 2f

                        val destRect = RectF(left, top, left + drawW, top + drawH)
                        canvas.drawBitmap(bitmap, null, destRect, paint)
                    } finally {
                        pdfDocument.finishPage(page)
                    }
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        return GeneratedPdfResult(
            pdfFile = pdfFile,
            pageImageFiles = savedPageFiles
        )
    }

    private fun computePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        pageSize: PdfPageSize,
        marginPt: Int
    ): Pair<Int, Int> {
        return when (pageSize) {
            PdfPageSize.A4 -> A4_WIDTH_PT to A4_HEIGHT_PT
            PdfPageSize.FIT_TO_IMAGE -> {
                val maxContentDim = MAX_FIT_PAGE_DIMENSION_PT - marginPt * 2
                val largest = max(bitmapWidth, bitmapHeight)
                val scale = if (largest > maxContentDim && maxContentDim > 0) {
                    maxContentDim.toFloat() / largest.toFloat()
                } else {
                    1f
                }
                val contentW = (bitmapWidth * scale).roundToInt().coerceAtLeast(1)
                val contentH = (bitmapHeight * scale).roundToInt().coerceAtLeast(1)
                (contentW + marginPt * 2) to (contentH + marginPt * 2)
            }
        }
    }
}
