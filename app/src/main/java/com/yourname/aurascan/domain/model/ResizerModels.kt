package com.yourname.aurascan.domain.model

enum class ResizePreset(
    val defaultWidthPx: Int,
    val defaultHeightPx: Int,
    val defaultMaxKb: Int,
    val isCustom: Boolean
) {
    EXAM_PHOTO(
        defaultWidthPx = 200,
        defaultHeightPx = 230,
        defaultMaxKb = 50,
        isCustom = false
    ),
    EXAM_SIGNATURE(
        defaultWidthPx = 140,
        defaultHeightPx = 60,
        defaultMaxKb = 20,
        isCustom = false
    ),
    PASSPORT_PHOTO(
        defaultWidthPx = 413,
        defaultHeightPx = 531,
        defaultMaxKb = 200,
        isCustom = false
    ),
    CUSTOM(
        defaultWidthPx = 600,
        defaultHeightPx = 600,
        defaultMaxKb = 100,
        isCustom = true
    )
}

/**
 * Normalized [0f..1f] crop rectangle relative to the source bitmap dimensions.
 */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun normalized(): CropRect {
        val l = left.coerceIn(0f, 0.95f)
        val t = top.coerceIn(0f, 0.95f)
        val r = right.coerceIn(l + 0.05f, 1f)
        val b = bottom.coerceIn(t + 0.05f, 1f)
        return CropRect(l, t, r, b)
    }

    val isFullImage: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f
}
