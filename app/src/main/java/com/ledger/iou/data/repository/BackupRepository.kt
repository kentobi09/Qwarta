package com.ledger.iou.data.repository

import android.content.Context
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupRepository {

    fun generateCsvContent(data: List<PersonWithTransactions>): String {
        val sb = StringBuilder()
        sb.append("Person Name,Phone Number,Transaction Date,Type,Principal (PHP),Interest (PHP),Total Due (PHP),Due Date,Notes\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dueDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (personWithTx in data) {
            val person = personWithTx.person
            val nameEscaped = escapeCsv(person.name)
            val phoneEscaped = escapeCsv(person.phoneNumber ?: "")

            for (tx in personWithTx.transactions) {
                val dateStr = dateFormat.format(Date(tx.timestampEpoch))
                val typeStr = if (tx.type == TransactionType.LENT) "LENT" else "REPAYMENT"
                val principalStr = String.format(Locale.US, "%.2f", tx.amount / 100.0)
                val interestStr = if (tx.type == TransactionType.LENT) {
                    String.format(Locale.US, "%.2f", tx.interestAmountCents / 100.0)
                } else "0.00"
                val totalDueStr = if (tx.type == TransactionType.LENT) {
                    String.format(Locale.US, "%.2f", (tx.amount + tx.interestAmountCents) / 100.0)
                } else {
                    principalStr
                }
                val dueDateStr = tx.dueDateEpoch?.let { dueDateFormat.format(Date(it)) } ?: ""
                val noteEscaped = escapeCsv(tx.note ?: "")

                sb.append("$nameEscaped,$phoneEscaped,$dateStr,$typeStr,$principalStr,$interestStr,$totalDueStr,$dueDateStr,$noteEscaped\n")
            }
        }
        return sb.toString()
    }

    suspend fun exportToCsv(
        context: Context,
        data: List<PersonWithTransactions>
    ): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Qwarta_Backup_$timestamp.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        file.bufferedWriter().use { writer ->
            writer.write(generateCsvContent(data))
        }

        file
    }

    suspend fun saveCsvToDownloads(
        context: Context,
        data: List<PersonWithTransactions>
    ): String = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Qwarta_Backup_$timestamp.csv"
        val csvContent = generateCsvContent(data)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Could not create file in Downloads")
            
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(csvContent.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: throw IllegalStateException("Could not write to file in Downloads")
            "Downloads/$fileName"
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            file.absolutePath
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
