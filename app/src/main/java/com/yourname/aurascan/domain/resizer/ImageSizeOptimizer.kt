package com.yourname.aurascan.domain.resizer

import kotlin.math.roundToInt

data class ResizeOptimizationResult(
    val bytes: ByteArray,
    val finalWidth: Int,
    val finalHeight: Int,
    val finalQuality: Int,
    val reachedTargetSize: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResizeOptimizationResult) return false
        return bytes.contentEquals(other.bytes) &&
            finalWidth == other.finalWidth &&
            finalHeight == other.finalHeight &&
            finalQuality == other.finalQuality &&
            reachedTargetSize == other.reachedTargetSize
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + finalWidth
        result = 31 * result + finalHeight
        result = 31 * result + finalQuality
        result = 31 * result + reachedTargetSize.hashCode()
        return result
    }
}

/**
 * Pure Kotlin image size-search engine.
 *
 * Scales to [targetWidth] x [targetHeight], then binary-searches JPEG quality
 * from [maxQuality] (95) down to [minQuality] (10) to find the highest quality
 * whose output is <= [maxBytes]. If even [minQuality] exceeds [maxBytes],
 * downscales width and height by 5% steps and repeats until the size fits
 * or [minDimensionPx] is reached.
 */
object ImageSizeOptimizer {

    const val DEFAULT_MAX_QUALITY = 95
    const val DEFAULT_MIN_QUALITY = 10
    const val DEFAULT_MIN_DIMENSION_PX = 16
    const val DOWNSCALE_FACTOR = 0.95

    fun optimizeToMaxBytes(
        targetWidth: Int,
        targetHeight: Int,
        maxBytes: Int,
        minDimensionPx: Int = DEFAULT_MIN_DIMENSION_PX,
        minQuality: Int = DEFAULT_MIN_QUALITY,
        maxQuality: Int = DEFAULT_MAX_QUALITY,
        compress: (width: Int, height: Int, quality: Int) -> ByteArray
    ): ResizeOptimizationResult {
        require(targetWidth > 0) { "targetWidth must be positive" }
        require(targetHeight > 0) { "targetHeight must be positive" }
        require(maxBytes > 0) { "maxBytes must be positive" }
        require(minQuality in 1..maxQuality && maxQuality <= 100) {
            "Invalid quality bounds: $minQuality..$maxQuality"
        }

        val effectiveMinDim = minDimensionPx.coerceAtLeast(1)
        var currentWidth = targetWidth
        var currentHeight = targetHeight

        while (true) {
            var low = minQuality
            var high = maxQuality
            var bestBytes: ByteArray? = null
            var bestQuality = minQuality

            while (low <= high) {
                val mid = (low + high) ushr 1
                val candidate = compress(currentWidth, currentHeight, mid)
                if (candidate.size <= maxBytes) {
                    bestBytes = candidate
                    bestQuality = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            if (bestBytes != null) {
                return ResizeOptimizationResult(
                    bytes = bestBytes,
                    finalWidth = currentWidth,
                    finalHeight = currentHeight,
                    finalQuality = bestQuality,
                    reachedTargetSize = true
                )
            }

            val nextWidth = stepDown5Percent(currentWidth, effectiveMinDim)
            val nextHeight = stepDown5Percent(currentHeight, effectiveMinDim)

            if (nextWidth == currentWidth && nextHeight == currentHeight) {
                val fallbackBytes = compress(currentWidth, currentHeight, minQuality)
                return ResizeOptimizationResult(
                    bytes = fallbackBytes,
                    finalWidth = currentWidth,
                    finalHeight = currentHeight,
                    finalQuality = minQuality,
                    reachedTargetSize = fallbackBytes.size <= maxBytes
                )
            }

            currentWidth = nextWidth
            currentHeight = nextHeight
        }
    }

    private fun stepDown5Percent(value: Int, minDim: Int): Int {
        if (value <= minDim) return value
        val scaled = (value * DOWNSCALE_FACTOR).roundToInt()
        val guaranteedStep = if (scaled >= value) value - 1 else scaled
        return guaranteedStep.coerceAtLeast(minDim)
    }

    fun computeAspectPreservedDimension(
        changedDimension: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        widthChanged: Boolean
    ): Int {
        if (changedDimension <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return changedDimension.coerceAtLeast(1)
        }
        return if (widthChanged) {
            ((changedDimension.toDouble() * sourceHeight) / sourceWidth).roundToInt().coerceAtLeast(1)
        } else {
            ((changedDimension.toDouble() * sourceWidth) / sourceHeight).roundToInt().coerceAtLeast(1)
        }
    }

    fun requiresProForTarget(
        isCustomPreset: Boolean,
        maxKb: Int,
        proUnlocked: Boolean,
        freeMinKb: Int = 20
    ): Boolean {
        if (proUnlocked) return false
        return isCustomPreset || maxKb < freeMinKb
    }
}
