package com.corey.notepad3.app

object DocumentImport {
    val openMimeTypes: Array<String> = arrayOf("*/*")

    fun canImportFromIncomingIntent(
        action: String?,
        hasDataUri: Boolean = false,
        hasStreamUri: Boolean = false,
        hasPlainText: Boolean = false,
    ): Boolean =
        when (action) {
            "android.intent.action.VIEW",
            "android.intent.action.EDIT",
            -> hasDataUri
            "android.intent.action.SEND" -> hasStreamUri || hasPlainText
            else -> false
        }
}
