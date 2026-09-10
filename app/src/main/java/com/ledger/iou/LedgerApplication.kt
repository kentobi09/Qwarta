package com.ledger.iou

import android.app.Application
import com.ledger.iou.data.db.LedgerDatabase
import com.ledger.iou.data.preferences.UserPreferencesRepository
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.worker.DebtReminderWorker
import com.ledger.iou.worker.ReminderNotificationHelper

class LedgerApplication : Application() {

    lateinit var database: LedgerDatabase
        private set

    lateinit var repository: LedgerRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = LedgerDatabase.getDatabase(this)
        repository = LedgerRepository(database.ledgerDao())
        preferencesRepository = UserPreferencesRepository(this)

        // Setup notifications & daily sync
        ReminderNotificationHelper.createNotificationChannel(this)
        DebtReminderWorker.scheduleDailySync(this)
    }
}
