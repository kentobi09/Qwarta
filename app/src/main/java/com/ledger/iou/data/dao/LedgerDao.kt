package com.ledger.iou.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.PersonEntity
import com.ledger.iou.data.model.PersonWithTransactions
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Transaction
    @Query("SELECT * FROM persons ORDER BY name COLLATE NOCASE ASC")
    fun getAllPersonsWithTransactions(): Flow<List<PersonWithTransactions>>

    @Transaction
    @Query("SELECT * FROM persons WHERE id = :personId LIMIT 1")
    fun getPersonWithTransactionsById(personId: String): Flow<PersonWithTransactions?>

    @Query("SELECT * FROM persons ORDER BY name COLLATE NOCASE ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :personId LIMIT 1")
    suspend fun getPersonById(personId: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :personId")
    suspend fun deletePersonById(personId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LoanTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: LoanTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: LoanTransactionEntity)

    @Query("DELETE FROM loan_transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("SELECT * FROM loan_transactions WHERE personId = :personId ORDER BY timestampEpoch ASC")
    fun getTransactionsForPerson(personId: String): Flow<List<LoanTransactionEntity>>

    @Query("SELECT * FROM loan_transactions WHERE type = 'LENT' AND dueDateEpoch IS NOT NULL AND dueDateEpoch <= :dueThresholdEpoch")
    suspend fun getDueOrOverdueLoans(dueThresholdEpoch: Long): List<LoanTransactionEntity>

    @Transaction
    @Query("SELECT * FROM persons")
    suspend fun getAllPersonsWithTransactionsSync(): List<PersonWithTransactions>
}
