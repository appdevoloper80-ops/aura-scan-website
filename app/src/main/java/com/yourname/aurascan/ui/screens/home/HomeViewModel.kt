package com.yourname.aurascan.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.aurascan.data.repository.DocumentRepository
import com.yourname.aurascan.domain.model.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val documents: StateFlow<List<Document>> = documentRepository.observeAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isSavingScan = MutableStateFlow(false)
    val isSavingScan: StateFlow<Boolean> = _isSavingScan.asStateFlow()

    fun saveScannedDocument(
        pdfUri: Uri?,
        pageUris: List<Uri>,
        onSuccess: (Document) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (_isSavingScan.value) return
        viewModelScope.launch {
            _isSavingScan.value = true
            try {
                val result = documentRepository.saveScannedDocument(
                    pdfUri = pdfUri,
                    pageImageUris = pageUris
                )
                result.onSuccess(onSuccess).onFailure(onError)
            } finally {
                _isSavingScan.value = false
            }
        }
    }

    fun renameDocument(id: String, newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            documentRepository.renameDocument(id, newName)
            onDone()
        }
    }

    fun deleteDocument(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
            onDone()
        }
    }

    class Factory(
        private val documentRepository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(documentRepository) as T
        }
    }
}
