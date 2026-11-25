package com.yourname.aurascan.domain.model

enum class PdfPageSize {
    A4,
    FIT_TO_IMAGE
}

enum class PdfMargin(val points: Int) {
    NONE(0),
    SMALL(24),
    MEDIUM(48)
}
