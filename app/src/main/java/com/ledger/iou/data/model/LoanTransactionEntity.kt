package com.ledger.iou.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

object TransactionType {
    const val LENT = "LENT"
    const val REPAYMENT = "REPAYMENT"
}

@Entity(
    tableName = "loan_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["dueDateEpoch"])
    ]
)
data class LoanTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val personId: String,
    val type: String, // "LENT" or "REPAYMENT"
    val amount: Long, // principal amount in cents (e.g. ₱10.50 -> 1050)
    val note: String? = null,
    val dueDateEpoch: Long? = null,
    val interestRatePercent: Double? = null, // e.g. 5.0 for 5%
    val interestAmountCents: Long = 0L, // agreed interest in cents
    val timestampEpoch: Long = System.currentTimeMillis()
)
