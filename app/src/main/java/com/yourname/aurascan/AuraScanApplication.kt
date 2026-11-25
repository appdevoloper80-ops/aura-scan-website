package com.yourname.aurascan

import android.app.Application
import com.yourname.aurascan.data.local.AgePreferenceStore
import com.yourname.aurascan.data.local.AuraScanDatabase
import com.yourname.aurascan.data.repository.DocumentRepository
import com.yourname.aurascan.data.repository.OcrRepository

class AppContainer(application: Application) {
    private val database: AuraScanDatabase by lazy {
        AuraScanDatabase.getInstance(application)
    }

    val agePreferenceStore: AgePreferenceStore by lazy {
        AgePreferenceStore(application)
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(
            context = application,
            documentDao = database.documentDao()
        )
    }

    val ocrRepository: OcrRepository by lazy {
        OcrRepository()
    }
}

class AuraScanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
