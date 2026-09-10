package com.ledger.iou.data.repository

import com.ledger.iou.data.dao.LedgerDao
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.PersonEntity
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TransactionWithRunningBalance(
    val transaction: LoanTransactionEntity,
    val runningBalanceCents: Long
)

data class LedgerOverviewStats(
    val totalOutstandingCents: Long,
    val activeDebtorsCount: Int,
    val overdueDebtorsCount: Int,
    val settledCount: Int
)

class LedgerRepository(
    private val dao: LedgerDao
) {
    val allPersonsWithTransactions: Flow<List<PersonWithTransactions>> =
        dao.getAllPersonsWithTransactions()

    fun getPersonWithTransactions(personId: String): Flow<PersonWithTransactions?> =
        dao.getPersonWithTransactionsById(personId)

    fun getTransactionsWithRunningBalance(personId: String): Flow<List<TransactionWithRunningBalance>> {
        return dao.getTransactionsForPerson(personId).map { transactions ->
            var currentBalance = 0L
            val result = mutableListOf<TransactionWithRunningBalance>()
            // Sorted chronologically ASC
            for (tx in transactions) {
                if (tx.type == TransactionType.LENT) {
                    currentBalance += tx.amount
                } else {
                    currentBalance -= tx.amount
                }
                result.add(TransactionWithRunningBalance(tx, currentBalance))
            }
            // Reverse so latest is on top in UI timeline
            result.reversed()
        }
    }

    suspend fun recordLoan(
        personId: String?,
        personName: String,
        phoneNumber: String?,
        amountCents: Long,
        type: String,
        note: String?,
        dueDateEpoch: Long?
    ): String {
        val targetPersonId = if (!personId.isNullOrBlank()) {
            personId
        } else {
            // Find existing person with exact case-insensitive name or create new
            val existing = dao.getAllPersonsWithTransactionsSync()
                .find { it.person.name.trim().equals(personName.trim(), ignoreCase = true) }
            if (existing != null) {
                existing.person.id
            } else {
                val newPersonId = UUID.randomUUID().toString()
                dao.insertPerson(
                    PersonEntity(
                        id = newPersonId,
                        name = personName.trim(),
                        phoneNumber = phoneNumber?.trim()?.ifBlank { null }
                    )
                )
                newPersonId
            }
        }

        val transaction = LoanTransactionEntity(
            id = UUID.randomUUID().toString(),
            personId = targetPersonId,
            type = type,
            amount = amountCents,
            note = note?.trim()?.ifBlank { null },
            dueDateEpoch = dueDateEpoch,
            timestampEpoch = System.currentTimeMillis()
        )
        dao.insertTransaction(transaction)
        return targetPersonId
    }

    suspend fun settleAll(personId: String, currentBalanceCents: Long) {
        if (currentBalanceCents <= 0) return
        val settlementTx = LoanTransactionEntity(
            id = UUID.randomUUID().toString(),
            personId = personId,
            type = TransactionType.REPAYMENT,
            amount = currentBalanceCents,
            note = "Full Balance Settlement",
            dueDateEpoch = null,
            timestampEpoch = System.currentTimeMillis()
        )
        dao.insertTransaction(settlementTx)
    }

    suspend fun deleteTransaction(transaction: LoanTransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deletePerson(personId: String) {
        dao.deletePersonById(personId)
    }

    suspend fun updatePerson(person: PersonEntity) {
        dao.updatePerson(person)
    }

    companion object {
        fun formatCents(cents: Long, mask: Boolean = false, currencySymbol: String = "₱"): String {
            if (mask) {
                return "••••••"
            }
            val absCents = Math.abs(cents)
            val dollars = absCents / 100
            val remainderCents = absCents % 100
            val formattedWhole = NumberFormat.getNumberInstance(Locale.US).format(dollars)
            val formattedDecimal = String.format(Locale.US, "%02d", remainderCents)
            val prefix = if (cents < 0) "-$currencySymbol" else currencySymbol
            return "$prefix$formattedWhole.$formattedDecimal"
        }

        fun formatDate(epochMillis: Long): String {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
            return sdf.format(Date(epochMillis))
        }

        fun formatRelativeDue(dueDateEpoch: Long, now: Long = System.currentTimeMillis()): String {
            val diffMillis = dueDateEpoch - now
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
            return when {
                diffMillis < 0 -> {
                    val overdueDays = Math.abs(diffDays)
                    if (overdueDays == 0) "OVERDUE TODAY" else "OVERDUE (${overdueDays}d ago)"
                }
                diffDays == 0 -> "DUE TODAY"
                diffDays == 1 -> "DUE TOMORROW"
                else -> "DUE IN ${diffDays}d"
            }
        }

        fun generateReminderMessage(
            personName: String,
            balanceCents: Long,
            dueDateEpoch: Long?,
            currencySymbol: String = "₱"
        ): String {
            val formattedAmount = formatCents(balanceCents, mask = false, currencySymbol = currencySymbol)
            val dueInfo = if (dueDateEpoch != null) {
                " which was scheduled for ${formatDate(dueDateEpoch)}"
            } else ""
            return "Hi $personName, hope you're doing well. Just sharing a polite reminder regarding our ledger balance of $formattedAmount$dueInfo. Let me know when you'd like to settle up. Thank you!"
        }
    }
}
