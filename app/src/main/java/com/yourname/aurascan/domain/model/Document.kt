package com.yourname.aurascan.domain.model

data class Document(
    val id: String,
    val name: String,
    val createdAt: Long,
    val pageCount: Int,
    val pdfPath: String,
    val pagePaths: List<String>
)
