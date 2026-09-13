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

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
