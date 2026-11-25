package com.yourname.aurascan.domain.resizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSizeOptimizerTest {

    @Test
    fun `returns max quality 95 when target fits immediately`() {
        val result = ImageSizeOptimizer.optimizeToMaxBytes(
            targetWidth = 200,
            targetHeight = 230,
            maxBytes = 50 * 1024
        ) { w, h, q ->
            // Always well below 50 KB
            ByteArray((w * h * q) / 500)
        }

        assertTrue(result.reachedTargetSize)
        assertEquals(200, result.finalWidth)
        assertEquals(230, result.finalHeight)
        assertEquals(95, result.finalQuality)
    }

    @Test
    fun `binary search finds exact highest quality under maxBytes`() {
        val maxBytes = 20_000

        // Size grows linearly: 250 bytes per quality point at 140x60
        // Max quality where q * 250 <= 20_000 is q = 80
        val result = ImageSizeOptimizer.optimizeToMaxBytes(
            targetWidth = 140,
            targetHeight = 60,
            maxBytes = maxBytes
        ) { _, _, q ->
            ByteArray(q * 250)
        }

        assertTrue(result.reachedTargetSize)
        assertEquals(140, result.finalWidth)
        assertEquals(60, result.finalHeight)
        assertEquals(80, result.finalQuality)
        assertEquals(20_000, result.bytes.size)
    }

    @Test
    fun `downscales in 5 percent steps when quality 10 still exceeds maxBytes`() {
        val maxBytes = 10_000

        // Size = (w * h * q) / 10
        // At 200x200 and q=10: 400_000 / 10 = 40_000 bytes (too big)
        // Downscale 5% steps until w*h <= 10_000 (100x100 at q=10 -> 10_000)
        val result = ImageSizeOptimizer.optimizeToMaxBytes(
            targetWidth = 200,
            targetHeight = 200,
            maxBytes = maxBytes
        ) { w, h, q ->
            ByteArray((w * h * q) / 10)
        }

        assertTrue(result.reachedTargetSize)
        assertTrue(result.finalWidth < 200)
        assertTrue(result.finalHeight < 200)
        assertTrue(result.bytes.size <= maxBytes)
        assertTrue(result.finalQuality in 10..95)
    }

    @Test
    fun `stops gracefully at minDimensionPx when target is unreachable`() {
        val maxBytes = 100

        val result = ImageSizeOptimizer.optimizeToMaxBytes(
            targetWidth = 60,
            targetHeight = 60,
            maxBytes = maxBytes,
            minDimensionPx = 16
        ) { _, _, _ ->
            ByteArray(500)
        }

        assertFalse(result.reachedTargetSize)
        assertEquals(16, result.finalWidth)
        assertEquals(16, result.finalHeight)
        assertEquals(10, result.finalQuality)
    }

    @Test
    fun `aspect ratio dimension calculation preserves proportions`() {
        val newHeight = ImageSizeOptimizer.computeAspectPreservedDimension(
            changedDimension = 200,
            sourceWidth = 1000,
            sourceHeight = 1500,
            widthChanged = true
        )
        assertEquals(300, newHeight)

        val newWidth = ImageSizeOptimizer.computeAspectPreservedDimension(
            changedDimension = 300,
            sourceWidth = 1000,
            sourceHeight = 1500,
            widthChanged = false
        )
        assertEquals(200, newWidth)
    }

    @Test
    fun `pro gating enforces custom preset and sub-20KB limit`() {
        assertFalse(
            ImageSizeOptimizer.requiresProForTarget(
                isCustomPreset = false,
                maxKb = 20,
                proUnlocked = false
            )
        )
        assertFalse(
            ImageSizeOptimizer.requiresProForTarget(
                isCustomPreset = false,
                maxKb = 50,
                proUnlocked = false
            )
        )
        assertTrue(
            ImageSizeOptimizer.requiresProForTarget(
                isCustomPreset = true,
                maxKb = 100,
                proUnlocked = false
            )
        )
        assertTrue(
            ImageSizeOptimizer.requiresProForTarget(
                isCustomPreset = false,
                maxKb = 15,
                proUnlocked = false
            )
        )
        assertFalse(
            ImageSizeOptimizer.requiresProForTarget(
                isCustomPreset = true,
                maxKb = 10,
                proUnlocked = true
            )
        )
    }
}
