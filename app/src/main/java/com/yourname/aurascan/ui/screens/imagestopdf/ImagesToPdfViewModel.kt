package com.yourname.aurascan.ui.screens.imagestopdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yourname.aurascan.Constants
import com.yourname.aurascan.data.repository.DocumentRepository
import com.yourname.aurascan.domain.model.Document
import com.yourname.aurascan.domain.model.PdfMargin
import com.yourname.aurascan.domain.model.PdfPageSize
import com.yourname.aurascan.util.ShareAndSaveUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ImagesToPdfUiState(
    val imagePaths: List<String> = emptyList(),
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val margin: PdfMargin = PdfMargin.SMALL,
    val isBuilding: Boolean = false
)

class ImagesToPdfViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ImagesToPdfUiState(
            imagePaths = savedStateHandle.get<ArrayList<String>>(KEY_IMAGE_PATHS)?.toList().orEmpty()
                .filter { File(it).exists() },
            pageSize = savedStateHandle.get<String>(KEY_PAGE_SIZE)
                ?.let { runCatching { PdfPageSize.valueOf(it) }.getOrNull() }
                ?: PdfPageSize.A4,
            margin = savedStateHandle.get<String>(KEY_MARGIN)
                ?.let { runCatching { PdfMargin.valueOf(it) }.getOrNull() }
                ?: PdfMargin.SMALL
        )
    )
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val copiedPaths = withContext(Dispatchers.IO) {
                val scratchDir = File(appContext.cacheDir, "picker_pdf").apply { mkdirs() }
                uris.mapNotNull { uri ->
                    val dest = File(scratchDir, "img_${UUID.randomUUID()}.jpg")
                    ShareAndSaveUtils.copyUriToFile(appContext, uri, dest)
                        .getOrNull()?.absolutePath
                }
            }
            val updated = (_uiState.value.imagePaths + copiedPaths)
                .take(Constants.MAX_SCAN_PAGES)
            persistPaths(updated)
        }
    }

    fun moveUp(index: Int) {
        val list = _uiState.value.imagePaths.toMutableList()
        if (index <= 0 || index >= list.size) return
        val item = list.removeAt(index)
        list.add(index - 1, item)
        persistPaths(list)
    }

    fun moveDown(index: Int) {
        val list = _uiState.value.imagePaths.toMutableList()
        if (index < 0 || index >= list.lastIndex) return
        val item = list.removeAt(index)
        list.add(index + 1, item)
        persistPaths(list)
    }

    fun removeAt(index: Int) {
        val list = _uiState.value.imagePaths.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        persistPaths(list)
    }

    fun setPageSize(pageSize: PdfPageSize) {
        savedStateHandle[KEY_PAGE_SIZE] = pageSize.name
        _uiState.update { it.copy(pageSize = pageSize) }
    }

    fun setMargin(margin: PdfMargin) {
        savedStateHandle[KEY_MARGIN] = margin.name
        _uiState.update { it.copy(margin = margin) }
    }

    fun createPdf(
        onSuccess: (Document) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val current = _uiState.value
        if (current.isBuilding || current.imagePaths.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBuilding = true) }
            try {
                val result = documentRepository.createPdfFromImages(
                    imageFiles = current.imagePaths.map(::File),
                    pageSize = current.pageSize,
                    margin = current.margin
                )
                result.onSuccess { doc ->
                    persistPaths(emptyList())
                    onSuccess(doc)
                }.onFailure(onError)
            } finally {
                _uiState.update { it.copy(isBuilding = false) }
            }
        }
    }

    private fun persistPaths(paths: List<String>) {
        savedStateHandle[KEY_IMAGE_PATHS] = ArrayList(paths)
        _uiState.update { it.copy(imagePaths = paths) }
    }

    companion object {
        private const val KEY_IMAGE_PATHS = "image_paths"
        private const val KEY_PAGE_SIZE = "page_size"
        private const val KEY_MARGIN = "pdf_margin"
    }

    class Factory(
        private val appContext: Context,
        private val documentRepository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ImagesToPdfViewModel(savedStateHandle, appContext, documentRepository) as T
        }
    }
}
