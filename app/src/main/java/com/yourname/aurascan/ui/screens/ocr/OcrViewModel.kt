package com.yourname.aurascan.ui.screens.ocr

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
import com.yourname.aurascan.data.repository.OcrRepository
import com.yourname.aurascan.domain.model.OcrLanguage
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

data class OcrUiState(
    val documentName: String? = null,
    val totalAvailablePages: Int = 0,
    val selectedSingleImagePath: String? = null,
    val selectedLanguage: OcrLanguage = OcrLanguage.LATIN,
    val isProcessing: Boolean = false,
    val currentPage: Int = 0,
    val pagesToProcess: Int = 0,
    val hasRun: Boolean = false,
    val extractedText: String = "",
    val showUnlockAllPagesCard: Boolean = false,
    val errorMessage: String? = null
)

class OcrViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val documentRepository: DocumentRepository,
    private val ocrRepository: OcrRepository
) : ViewModel() {

    private val documentId: String? = savedStateHandle.get<String>("documentId")
        ?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        OcrUiState(
            selectedSingleImagePath = savedStateHandle.get<String>(KEY_SINGLE_IMAGE_PATH),
            selectedLanguage = savedStateHandle.get<String>(KEY_LANGUAGE)
                ?.let { runCatching { OcrLanguage.valueOf(it) }.getOrNull() }
                ?: OcrLanguage.LATIN,
            extractedText = savedStateHandle.get<String>(KEY_EXTRACTED_TEXT).orEmpty(),
            hasRun = savedStateHandle.get<Boolean>(KEY_HAS_RUN) ?: false,
            showUnlockAllPagesCard = savedStateHandle.get<Boolean>(KEY_SHOW_UNLOCK) ?: false
        )
    )
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private var documentPagePaths: List<String> = emptyList()

    init {
        if (documentId != null) {
            viewModelScope.launch {
                val doc = documentRepository.getDocumentById(documentId)
                if (doc != null) {
                    documentPagePaths = doc.pagePaths
                    _uiState.update {
                        it.copy(
                            documentName = doc.name,
                            totalAvailablePages = doc.pagePaths.size
                        )
                    }
                }
            }
        }
    }

    fun selectLanguage(language: OcrLanguage) {
        savedStateHandle[KEY_LANGUAGE] = language.name
        _uiState.update { it.copy(selectedLanguage = language, errorMessage = null) }
    }

    fun onSingleImagePicked(uri: Uri) {
        viewModelScope.launch {
            val copiedFile = withContext(Dispatchers.IO) {
                val dir = File(appContext.cacheDir, "picker_ocr").apply { mkdirs() }
                val dest = File(dir, "ocr_${UUID.randomUUID()}.jpg")
                ShareAndSaveUtils.copyUriToFile(appContext, uri, dest).getOrNull()
            }
            if (copiedFile != null) {
                savedStateHandle[KEY_SINGLE_IMAGE_PATH] = copiedFile.absolutePath
                savedStateHandle[KEY_EXTRACTED_TEXT] = ""
                savedStateHandle[KEY_HAS_RUN] = false
                savedStateHandle[KEY_SHOW_UNLOCK] = false
                _uiState.update {
                    it.copy(
                        selectedSingleImagePath = copiedFile.absolutePath,
                        totalAvailablePages = 1,
                        extractedText = "",
                        hasRun = false,
                        showUnlockAllPagesCard = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun runOcr() {
        val state = _uiState.value
        if (state.isProcessing) return

        val allFiles = if (documentId != null) {
            documentPagePaths.map(::File).filter { it.exists() }
        } else {
            listOfNotNull(state.selectedSingleImagePath?.let(::File)).filter { it.exists() }
        }

        if (allFiles.isEmpty()) return

        val proUnlocked = Constants.isProUnlocked()
        val limitExceeded = !proUnlocked && allFiles.size > Constants.FREE_OCR_PAGE_LIMIT
        val filesToProcess = if (limitExceeded) {
            allFiles.take(Constants.FREE_OCR_PAGE_LIMIT)
        } else {
            allFiles
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    currentPage = 1,
                    pagesToProcess = filesToProcess.size,
                    errorMessage = null
                )
            }

            val result = ocrRepository.recognizePages(
                imageFiles = filesToProcess,
                language = state.selectedLanguage,
                onPageProgress = { current, total ->
                    _uiState.update {
                        it.copy(currentPage = current, pagesToProcess = total)
                    }
                }
            )

            result.onSuccess { text ->
                savedStateHandle[KEY_EXTRACTED_TEXT] = text
                savedStateHandle[KEY_HAS_RUN] = true
                savedStateHandle[KEY_SHOW_UNLOCK] = limitExceeded
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        hasRun = true,
                        extractedText = text,
                        showUnlockAllPagesCard = limitExceeded
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = err.localizedMessage ?: "OCR failed"
                    )
                }
            }
        }
    }

    fun suggestedTxtFileName(): String {
        val base = _uiState.value.documentName ?: "AuraScan_OCR"
        return ShareAndSaveUtils.sanitizeFileName(base, "txt")
    }

    companion object {
        private const val KEY_SINGLE_IMAGE_PATH = "single_image_path"
        private const val KEY_LANGUAGE = "ocr_language"
        private const val KEY_EXTRACTED_TEXT = "extracted_text"
        private const val KEY_HAS_RUN = "has_run"
        private const val KEY_SHOW_UNLOCK = "show_unlock"
    }

    class Factory(
        private val appContext: Context,
        private val documentRepository: DocumentRepository,
        private val ocrRepository: OcrRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return OcrViewModel(
                savedStateHandle = savedStateHandle,
                appContext = appContext,
                documentRepository = documentRepository,
                ocrRepository = ocrRepository
            ) as T
        }
    }
}
