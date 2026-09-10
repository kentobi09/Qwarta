package com.ledger.iou

import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.PersonEntity
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.TransactionType
import com.ledger.iou.data.repository.LedgerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LedgerCalculationTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("₱0.00", LedgerRepository.formatCents(0L))
        assertEquals("₱10.50", LedgerRepository.formatCents(1050L))
        assertEquals("₱1,450.00", LedgerRepository.formatCents(145000L))
        assertEquals("-₱25.00", LedgerRepository.formatCents(-2500L))
        assertEquals("••••••", LedgerRepository.formatCents(145000L, mask = true))
    }

    @Test
    fun testPersonBalanceCalculations() {
        val person = PersonEntity(id = "person-1", name = "Alice", phoneNumber = "+1234567890")
        val transactions = listOf(
            LoanTransactionEntity(
                id = "tx-1",
                personId = "person-1",
                type = TransactionType.LENT,
                amount = 10000L, // $100.00
                timestampEpoch = 1000L
            ),
            LoanTransactionEntity(
                id = "tx-2",
                personId = "person-1",
                type = TransactionType.LENT,
                amount = 4500L, // $45.00
                timestampEpoch = 2000L
            ),
            LoanTransactionEntity(
                id = "tx-3",
                personId = "person-1",
                type = TransactionType.REPAYMENT,
                amount = 2500L, // $25.00
                timestampEpoch = 3000L
            )
        )

        val personWithTx = PersonWithTransactions(person = person, transactions = transactions)

        assertEquals(14500L, personWithTx.totalLentCents) // $145.00
        assertEquals(2500L, personWithTx.totalRepaidCents) // $25.00
        assertEquals(12000L, personWithTx.balanceCents) // $120.00
        assertFalse(personWithTx.isSettled)
    }

    @Test
    fun testAgreedInterestCalculations() {
        val person = PersonEntity(id = "person-interest", name = "Juan")
        val transactions = listOf(
            LoanTransactionEntity(
                id = "tx-int-1",
                personId = "person-interest",
                type = TransactionType.LENT,
                amount = 100000L, // ₱1,000.00 principal
                interestRatePercent = 10.0,
                interestAmountCents = 10000L, // ₱100.00 agreed interest (10%)
                timestampEpoch = 1000L
            ),
            LoanTransactionEntity(
                id = "tx-int-2",
                personId = "person-interest",
                type = TransactionType.REPAYMENT,
                amount = 50000L, // ₱500.00 repayment
                timestampEpoch = 2000L
            )
        )

        val personWithTx = PersonWithTransactions(person = person, transactions = transactions)

        assertEquals(100000L, personWithTx.totalPrincipalLentCents)
        assertEquals(10000L, personWithTx.totalInterestCents)
        assertEquals(110000L, personWithTx.totalLentCents) // Principal + Interest = ₱1,100.00
        assertEquals(50000L, personWithTx.totalRepaidCents) // ₱500.00 repaid
        assertEquals(60000L, personWithTx.balanceCents) // Remaining balance = ₱600.00
        assertFalse(personWithTx.isSettled)
    }

    @Test
    fun testSettledCalculation() {
        val person = PersonEntity(id = "person-2", name = "Bob")
        val transactions = listOf(
            LoanTransactionEntity(
                id = "tx-1",
                personId = "person-2",
                type = TransactionType.LENT,
                amount = 5000L,
                timestampEpoch = 1000L
            ),
            LoanTransactionEntity(
                id = "tx-2",
                personId = "person-2",
                type = TransactionType.REPAYMENT,
                amount = 5000L,
                timestampEpoch = 2000L
            )
        )

        val personWithTx = PersonWithTransactions(person = person, transactions = transactions)
        assertEquals(0L, personWithTx.balanceCents)
        assertTrue(personWithTx.isSettled)
    }

    @Test
    fun testOverdueDetection() {
        val now = 1000000000000L
        val person = PersonEntity(id = "person-3", name = "Charlie")
        val overdueTx = LoanTransactionEntity(
            id = "tx-1",
            personId = "person-3",
            type = TransactionType.LENT,
            amount = 3000L,
            dueDateEpoch = now - (2 * 24 * 3600 * 1000L), // 2 days ago
            timestampEpoch = now - (5 * 24 * 3600 * 1000L)
        )

        val personWithTx = PersonWithTransactions(person = person, transactions = listOf(overdueTx))
        assertTrue(personWithTx.isOverdue(now))

        val settledTx = overdueTx.copy(type = TransactionType.REPAYMENT)
        val settledPerson = PersonWithTransactions(person = person, transactions = listOf(overdueTx, settledTx))
        assertFalse(settledPerson.isOverdue(now))
    }

    @Test
    fun testReminderMessageGeneration() {
        val msg = LedgerRepository.generateReminderMessage(
            personName = "David",
            balanceCents = 15000L,
            dueDateEpoch = null
        )
        assertTrue(msg.contains("David"))
        assertTrue(msg.contains("₱150.00"))
        assertTrue(msg.contains("polite reminder"))
    }
}
