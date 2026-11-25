package com.yourname.aurascan.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

object ShareAndSaveUtils {

    fun getFileProviderUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ): Boolean {
        val uri = getFileProviderUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun writeTextToCacheAndShare(
        context: Context,
        text: String,
        fileName: String,
        chooserTitle: String
    ): Boolean {
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val txtFile = File(sharedDir, sanitizeFileName(fileName, "txt"))
        txtFile.writeText(text, Charsets.UTF_8)
        return shareFile(context, txtFile, "text/plain", chooserTitle)
    }

    fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("Unable to open destination stream")
        }
    }

    fun writeTextToUri(context: Context, text: String, destinationUri: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open destination stream")
        }
    }

    fun writeBytesToUri(context: Context, bytes: ByteArray, destinationUri: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(bytes)
            } ?: throw IllegalStateException("Unable to open destination stream")
        }
    }

    fun copyUriToFile(context: Context, sourceUri: Uri, destFile: File): Result<File> {
        return runCatching {
            destFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            } ?: throw IllegalStateException("Unable to read selected file")
            destFile
        }
    }

    fun sanitizeFileName(baseName: String, extension: String): String {
        val cleaned = baseName.trim()
            .replace(Regex("[^a-zA-Z0-9._\\- ]"), "_")
            .ifBlank { "aurascan_export" }
        return if (cleaned.endsWith(".$extension", ignoreCase = true)) {
            cleaned
        } else {
            "$cleaned.$extension"
        }
    }
}
