package com.ledger.iou.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ledger.iou.data.dao.LedgerDao
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.PersonEntity

@Database(
    entities = [
        PersonEntity::class,
        LoanTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
