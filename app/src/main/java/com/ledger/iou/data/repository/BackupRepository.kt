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

    suspend fun exportToCsv(
        context: Context,
        data: List<PersonWithTransactions>
    ): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Ledger_Backup_$timestamp.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        file.bufferedWriter().use { writer ->
            // CSV Header
            writer.write("Person Name,Phone Number,Transaction Date,Type,Amount (PHP),Due Date,Notes\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dueDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (personWithTx in data) {
                val person = personWithTx.person
                val nameEscaped = escapeCsv(person.name)
                val phoneEscaped = escapeCsv(person.phoneNumber ?: "")

                for (tx in personWithTx.transactions) {
                    val dateStr = dateFormat.format(Date(tx.timestampEpoch))
                    val typeStr = if (tx.type == TransactionType.LENT) "LENT" else "REPAYMENT"
                    val amountStr = String.format(Locale.US, "%.2f", tx.amount / 100.0)
                    val dueDateStr = tx.dueDateEpoch?.let { dueDateFormat.format(Date(it)) } ?: ""
                    val noteEscaped = escapeCsv(tx.note ?: "")

                    writer.write("$nameEscaped,$phoneEscaped,$dateStr,$typeStr,$amountStr,$dueDateStr,$noteEscaped\n")
                }
            }
        }

        file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
