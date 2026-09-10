package com.ledger.iou.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ledger.iou.data.db.LedgerDatabase
import java.util.concurrent.TimeUnit

class DebtReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = LedgerDatabase.getDatabase(context)
        val personsWithTransactions = database.ledgerDao().getAllPersonsWithTransactionsSync()

        val now = System.currentTimeMillis()
        val oneDayAhead = now + TimeUnit.DAYS.toMillis(1)

        for (personWithTx in personsWithTransactions) {
            // Only notify if there is a positive remaining balance
            if (personWithTx.balanceCents > 0) {
                val isOverdue = personWithTx.isOverdue(now)
                val upcomingDueDate = personWithTx.nextUpcomingDueDate(now)

                val shouldNotify = isOverdue || (upcomingDueDate != null && upcomingDueDate <= oneDayAhead)
                if (shouldNotify) {
                    ReminderNotificationHelper.showReminderNotification(
                        context = context,
                        personId = personWithTx.person.id,
                        personName = personWithTx.person.name,
                        amountCents = personWithTx.balanceCents,
                        isOverdue = isOverdue,
                        dueDateEpoch = upcomingDueDate
                    )
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "debt_reminder_periodic_sync"

        fun scheduleDailySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<DebtReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
