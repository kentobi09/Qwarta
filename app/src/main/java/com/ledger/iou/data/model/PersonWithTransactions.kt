package com.ledger.iou.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class PersonWithTransactions(
    @Embedded
    val person: PersonEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "personId"
    )
    val transactions: List<LoanTransactionEntity> = emptyList()
) {
    val totalPrincipalLentCents: Long
        get() = transactions
            .filter { it.type == TransactionType.LENT }
            .sumOf { it.amount }

    val totalInterestCents: Long
        get() = transactions
            .filter { it.type == TransactionType.LENT }
            .sumOf { it.interestAmountCents }

    val totalLentCents: Long
        get() = totalPrincipalLentCents + totalInterestCents

    val totalRepaidCents: Long
        get() = transactions
            .filter { it.type == TransactionType.REPAYMENT }
            .sumOf { it.amount }

    val balanceCents: Long
        get() = totalLentCents - totalRepaidCents

    val isSettled: Boolean
        get() = balanceCents <= 0

    val latestActivityEpoch: Long
        get() = transactions.maxOfOrNull { it.timestampEpoch } ?: person.createdAt

    fun isOverdue(currentTimeEpoch: Long = System.currentTimeMillis()): Boolean {
        if (isSettled) return false
        return transactions.any {
            it.type == TransactionType.LENT &&
            it.dueDateEpoch != null &&
            it.dueDateEpoch < currentTimeEpoch
        }
    }

    fun nextUpcomingDueDate(currentTimeEpoch: Long = System.currentTimeMillis()): Long? {
        if (isSettled) return null
        return transactions
            .filter { it.type == TransactionType.LENT && it.dueDateEpoch != null && it.dueDateEpoch >= currentTimeEpoch }
            .minOfOrNull { it.dueDateEpoch!! }
            ?: transactions
                .filter { it.type == TransactionType.LENT && it.dueDateEpoch != null }
                .minOfOrNull { it.dueDateEpoch!! }
    }
}
