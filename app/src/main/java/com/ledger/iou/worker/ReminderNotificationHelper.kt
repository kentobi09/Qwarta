package com.ledger.iou.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ledger.iou.MainActivity
import com.ledger.iou.R
import com.ledger.iou.data.repository.LedgerRepository

object ReminderNotificationHelper {

    const val CHANNEL_ID = "ledger_debt_reminders"
    const val ACTION_MARK_REPAID = "com.ledger.iou.ACTION_MARK_REPAID"
    const val EXTRA_PERSON_ID = "extra_person_id"
    const val EXTRA_PERSON_NAME = "extra_person_name"
    const val EXTRA_AMOUNT_CENTS = "extra_amount_cents"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        personId: String,
        personName: String,
        amountCents: Long,
        isOverdue: Boolean,
        dueDateEpoch: Long?
    ) {
        createNotificationChannel(context)
        val notificationId = personId.hashCode()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_PERSON_ID, personId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markRepaidIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_MARK_REPAID
            putExtra(EXTRA_PERSON_ID, personId)
            putExtra(EXTRA_AMOUNT_CENTS, amountCents)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markRepaidPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            markRepaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = LedgerRepository.formatCents(amountCents)
        val title = if (isOverdue) "Overdue Debt: $personName" else "Upcoming Loan: $personName"
        val message = if (isOverdue) {
            val dueStr = dueDateEpoch?.let { LedgerRepository.formatRelativeDue(it) } ?: "overdue"
            "$personName owes $formattedAmount ($dueStr)."
        } else {
            "$personName is scheduled to repay $formattedAmount."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Mark Repaid", markRepaidPendingIntent)
            .addAction(0, "View Person", openPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted yet on Android 13+
        }
    }
}
