package com.yourname.aurascan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.yourname.aurascan.domain.model.CropRect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtils {

    fun readImageBounds(file: File): Pair<Int, Int>? {
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val rawW = options.outWidth
        val rawH = options.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        val orientation = readExifOrientation(file)
        val isRotated90 = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

        return if (isRotated90) rawH to rawW else rawW to rawH
    }

    fun decodeSampledBitmap(file: File, maxDimension: Int = 2048): Bitmap? {
        if (!file.exists()) return null
        return decodeSampled(
            openStream = { FileInputStream(file) },
            maxDimension = maxDimension
        )
    }

    fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return decodeSampled(
            openStream = { context.contentResolver.openInputStream(uri) },
            maxDimension = maxDimension
        )
    }

    private fun decodeSampled(
        openStream: () -> InputStream?,
        maxDimension: Int
    ): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val outW = boundsOptions.outWidth
        val outH = boundsOptions.outHeight
        if (outW <= 0 || outH <= 0) return null

        val sampleSize = calculateInSampleSize(outW, outH, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = openStream()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val orientation = openStream()?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return applyExifOrientation(decoded, orientation)
    }

    private fun readExifOrientation(file: File): Int {
        return try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        var halfW = width
        var halfH = height
        while (max(halfW, halfH) > maxDimension) {
            inSampleSize *= 2
            halfW = width / inSampleSize
            halfH = height / inSampleSize
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (transformed !== bitmap) {
                bitmap.recycle()
            }
            transformed
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    fun cropBitmap(source: Bitmap, cropRect: CropRect): Bitmap {
        val norm = cropRect.normalized()
        if (norm.isFullImage) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        }
        val leftPx = (norm.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val topPx = (norm.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val rightPx = (norm.right * source.width).roundToInt().coerceIn(leftPx + 1, source.width)
        val bottomPx = (norm.bottom * source.height).roundToInt().coerceIn(topPx + 1, source.height)
        val widthPx = (rightPx - leftPx).coerceAtLeast(1)
        val heightPx = (bottomPx - topPx).coerceAtLeast(1)

        return Bitmap.createBitmap(source, leftPx, topPx, widthPx, heightPx)
    }

    fun scaleAndCompressJpeg(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        quality: Int
    ): ByteArray {
        val scaled = if (source.width == targetWidth && source.height == targetHeight) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }
        return try {
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
                out.toByteArray()
            }
        } finally {
            if (scaled !== source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }
}
