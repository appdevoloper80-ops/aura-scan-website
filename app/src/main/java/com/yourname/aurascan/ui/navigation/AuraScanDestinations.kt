package com.yourname.aurascan.ui.navigation

object AuraScanRoutes {
    const val HOME = "home"
    const val DOCUMENT = "document/{documentId}"
    const val OCR = "ocr?documentId={documentId}"
    const val IMAGES_TO_PDF = "images_to_pdf"
    const val RESIZER = "resizer"
    const val SETTINGS = "settings"

    fun document(documentId: String): String = "document/$documentId"
    fun ocrForDocument(documentId: String): String = "ocr?documentId=$documentId"
    fun ocrStandalone(): String = "ocr"
}
