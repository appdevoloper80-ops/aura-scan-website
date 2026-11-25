package com.yourname.aurascan.data.repository

import android.content.Context
import android.net.Uri
import com.yourname.aurascan.data.local.DocumentDao
import com.yourname.aurascan.data.local.DocumentEntity
import com.yourname.aurascan.domain.model.Document
import com.yourname.aurascan.domain.model.PdfMargin
import com.yourname.aurascan.domain.model.PdfPageSize
import com.yourname.aurascan.util.DateFormatter
import com.yourname.aurascan.util.PdfGenerator
import com.yourname.aurascan.util.ShareAndSaveUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao
) {
    private val documentsRootDir: File
        get() = File(context.filesDir, "documents").apply { mkdirs() }

    fun observeAllDocuments(): Flow<List<Document>> {
        return documentDao.observeAllDocuments().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeDocumentById(id: String): Flow<Document?> {
        return documentDao.observeDocumentById(id).map { it?.toDomain() }
    }

    suspend fun getDocumentById(id: String): Document? {
        return documentDao.getDocumentById(id)?.toDomain()
    }

    suspend fun saveScannedDocument(
        pdfUri: Uri?,
        pageImageUris: List<Uri>
    ): Result<Document> = withContext(Dispatchers.IO) {
        runCatching {
            val id = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            val name = DateFormatter.defaultScanName(createdAt)
            val docDir = File(documentsRootDir, id).apply { mkdirs() }

            val copiedPageFiles = mutableListOf<File>()
            pageImageUris.forEachIndexed { index, uri ->
                val destPage = File(docDir, "page_%03d.jpg".format(index + 1))
                ShareAndSaveUtils.copyUriToFile(context, uri, destPage).getOrThrow()
                copiedPageFiles.add(destPage)
            }

            val pdfFile = File(docDir, "document.pdf")
            if (pdfUri != null) {
                ShareAndSaveUtils.copyUriToFile(context, pdfUri, pdfFile).getOrThrow()
            } else {
                require(copiedPageFiles.isNotEmpty()) { "Scanner returned no pages or PDF" }
                PdfGenerator.createPdfFromImageFiles(
                    sourceFiles = copiedPageFiles,
                    outputDir = docDir,
                    pageSize = PdfPageSize.A4,
                    margin = PdfMargin.SMALL
                )
            }

            val pageCount = copiedPageFiles.size.coerceAtLeast(1)
            val document = Document(
                id = id,
                name = name,
                createdAt = createdAt,
                pageCount = pageCount,
                pdfPath = pdfFile.absolutePath,
                pagePaths = copiedPageFiles.map { it.absolutePath }
            )
            documentDao.insertDocument(DocumentEntity.fromDomain(document))
            document
        }
    }

    suspend fun createPdfFromImages(
        imageFiles: List<File>,
        pageSize: PdfPageSize,
        margin: PdfMargin
    ): Result<Document> = withContext(Dispatchers.IO) {
        runCatching {
            val id = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            val name = DateFormatter.defaultPdfName(createdAt)
            val docDir = File(documentsRootDir, id).apply { mkdirs() }

            val generated = PdfGenerator.createPdfFromImageFiles(
                sourceFiles = imageFiles,
                outputDir = docDir,
                pageSize = pageSize,
                margin = margin
            )

            val document = Document(
                id = id,
                name = name,
                createdAt = createdAt,
                pageCount = generated.pageImageFiles.size,
                pdfPath = generated.pdfFile.absolutePath,
                pagePaths = generated.pageImageFiles.map { it.absolutePath }
            )
            documentDao.insertDocument(DocumentEntity.fromDomain(document))
            document
        }
    }

    suspend fun renameDocument(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            documentDao.renameDocument(id, trimmed)
        }
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(id)
        val docDir = File(documentsRootDir, id)
        if (docDir.exists()) {
            docDir.deleteRecursively()
        }
    }
}
