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
    version = 2,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loan_transactions ADD COLUMN interestRatePercent REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE loan_transactions ADD COLUMN interestAmountCents INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "ledger_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
