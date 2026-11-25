package com.yourname.aurascan.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    fun defaultScanName(timestampMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return "Scan ${formatter.format(Date(timestampMillis))}"
    }

    fun defaultPdfName(timestampMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return "PDF ${formatter.format(Date(timestampMillis))}"
    }

    fun formatDisplayDate(timestampMillis: Long): String {
        val formatter = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault()
        )
        return formatter.format(Date(timestampMillis))
    }
}
