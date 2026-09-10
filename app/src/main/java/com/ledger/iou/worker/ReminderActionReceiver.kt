package com.ledger.iou.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ledger.iou.data.db.LedgerDatabase
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ReminderNotificationHelper.ACTION_MARK_REPAID) {
            val personId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_PERSON_ID) ?: return
            val amountCents = intent.getLongExtra(ReminderNotificationHelper.EXTRA_AMOUNT_CENTS, 0L)
            val notificationId = intent.getIntExtra(ReminderNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

            if (notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }

            if (amountCents > 0) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = LedgerDatabase.getDatabase(context)
                        val repayment = LoanTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            personId = personId,
                            type = TransactionType.REPAYMENT,
                            amount = amountCents,
                            note = "Marked repaid via notification",
                            dueDateEpoch = null,
                            timestampEpoch = System.currentTimeMillis()
                        )
                        db.ledgerDao().insertTransaction(repayment)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
