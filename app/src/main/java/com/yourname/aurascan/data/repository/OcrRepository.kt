package com.yourname.aurascan.data.repository

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yourname.aurascan.domain.model.OcrLanguage
import com.yourname.aurascan.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class OcrRepository {

    suspend fun recognizePages(
        imageFiles: List<File>,
        language: OcrLanguage,
        onPageProgress: (currentPage: Int, totalPages: Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val recognizer = when (language) {
                OcrLanguage.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                OcrLanguage.HINDI_DEVANAGARI -> TextRecognition.getClient(
                    DevanagariTextRecognizerOptions.Builder().build()
                )
            }

            try {
                val sections = mutableListOf<String>()
                val total = imageFiles.size

                imageFiles.forEachIndexed { index, file ->
                    onPageProgress(index + 1, total)
                    val bitmap = BitmapUtils.decodeSampledBitmap(file, maxDimension = 2048)
                        ?: return@forEachIndexed

                    try {
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val visionText = recognizer.process(inputImage).await()
                        val pageText = visionText.text.trim()
                        if (total > 1) {
                            val header = "--- Page ${index + 1} ---"
                            sections.add(
                                if (pageText.isNotEmpty()) "$header\n$pageText" else header
                            )
                        } else if (pageText.isNotEmpty()) {
                            sections.add(pageText)
                        }
                    } finally {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }
                }

                sections.joinToString(separator = "\n\n").trim()
            } finally {
                recognizer.close()
            }
        }
    }
}
