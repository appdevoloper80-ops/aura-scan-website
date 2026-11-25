package com.yourname.aurascan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.aurascan.domain.model.Document

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val pageCount: Int,
    val pdfPath: String,
    val pagePaths: List<String>
) {
    fun toDomain(): Document = Document(
        id = id,
        name = name,
        createdAt = createdAt,
        pageCount = pageCount,
        pdfPath = pdfPath,
        pagePaths = pagePaths
    )

    companion object {
        fun fromDomain(document: Document): DocumentEntity = DocumentEntity(
            id = document.id,
            name = document.name,
            createdAt = document.createdAt,
            pageCount = document.pageCount,
            pdfPath = document.pdfPath,
            pagePaths = document.pagePaths
        )
    }
}
