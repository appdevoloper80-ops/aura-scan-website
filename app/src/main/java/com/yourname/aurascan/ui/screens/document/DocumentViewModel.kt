package com.yourname.aurascan.ui.screens.document

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yourname.aurascan.data.repository.DocumentRepository
import com.yourname.aurascan.domain.model.Document
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])

    val document: StateFlow<Document?> = documentRepository.observeDocumentById(documentId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun renameDocument(newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, newName)
            onDone()
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId)
            onDeleted()
        }
    }

    class Factory(
        private val documentRepository: DocumentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return DocumentViewModel(savedStateHandle, documentRepository) as T
        }
    }
}
