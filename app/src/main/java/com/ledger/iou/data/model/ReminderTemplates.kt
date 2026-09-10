package com.ledger.iou.data.model

import com.ledger.iou.data.repository.LedgerRepository

enum class ReminderLanguage {
    TAGALOG,
    ENGLISH
}

enum class ReminderTone {
    FLEXIBLE,       // Partial / installment ("kahit magkano muna")
    BUDGET_NEED,    // Need for bills ("may babayarang bills")
    ACCOUNTING,     // Month-end audit ("inaayos ko ang listahan")
    COMMITMENT      // Date lock-in ("anong date kaya")
}

data class ReminderTemplateItem(
    val tone: ReminderTone,
    val labelTagalog: String,
    val labelEnglish: String,
    val shortLabel: String
)

object ReminderTemplates {

    val availableTemplates = listOf(
        ReminderTemplateItem(
            tone = ReminderTone.FLEXIBLE,
            labelTagalog = "Hulugan / Kahit Magkano",
            labelEnglish = "Installment / Partial",
            shortLabel = "INSTALLMENT"
        ),
        ReminderTemplateItem(
            tone = ReminderTone.BUDGET_NEED,
            labelTagalog = "May Babayarang Bills",
            labelEnglish = "Need for Bills",
            shortLabel = "BILLS / NEED"
        ),
        ReminderTemplateItem(
            tone = ReminderTone.ACCOUNTING,
            labelTagalog = "Month-End Audit",
            labelEnglish = "Month-End Audit",
            shortLabel = "AUDIT / TRACKER"
        ),
        ReminderTemplateItem(
            tone = ReminderTone.COMMITMENT,
            labelTagalog = "Target Date Follow-up",
            labelEnglish = "Target Date Follow-up",
            shortLabel = "DATE LOCK-IN"
        )
    )

    fun buildMessage(
        language: ReminderLanguage,
        tone: ReminderTone,
        personName: String,
        balanceCents: Long,
        dueDateEpoch: Long?
    ): String {
        val formattedAmount = LedgerRepository.formatCents(balanceCents)
        val firstName = personName.trim().split(" ").firstOrNull() ?: personName.trim()

        return when (language) {
            ReminderLanguage.TAGALOG -> buildTagalogMessage(tone, firstName, formattedAmount, dueDateEpoch)
            ReminderLanguage.ENGLISH -> buildEnglishMessage(tone, firstName, formattedAmount, dueDateEpoch)
        }
    }

    private fun buildTagalogMessage(
        tone: ReminderTone,
        name: String,
        amount: String,
        dueDateEpoch: Long?
    ): String {
        val dueInfo = if (dueDateEpoch != null) {
            " (scheduled nung ${LedgerRepository.formatDate(dueDateEpoch)})"
        } else ""

        return when (tone) {
            ReminderTone.FLEXIBLE ->
                "Hi $name! Kumusta? Paalala lang sana sa balance natin na $amount$dueInfo. Ayos lang kahit installment or kahit magkano muna para mabawasan at hindi mabigat sa'yo. Salamat! 😊"

            ReminderTone.BUDGET_NEED ->
                "Hi $name, pasensya na sa abala. May parating kasi akong kailangang bayaran ngayong linggo. Baka sakaling pwede mahiram pabalik kahit parte o buong $amount$dueInfo? Kahit magkano malaking tulong na ngayon. Salamat talaga!"

            ReminderTone.ACCOUNTING ->
                "Hi $name! Inaayos ko lang personal budget at listahan ko for month-end. Ask ko lang kailan natin maisisingit i-settle yung balance na $amount$dueInfo, kahit hulugan muna para mai-update ko na sa records. Salamat!"

            ReminderTone.COMMITMENT ->
                "Hi $name! Follow-up ko lang sana yung sa $amount natin$dueInfo. Pwede humingi ng specific date kung kailan mo kaya mag-send, kahit partial muna? Para maipasok ko sa budget planner ko. Thank you!"
        }
    }

    private fun buildEnglishMessage(
        tone: ReminderTone,
        name: String,
        amount: String,
        dueDateEpoch: Long?
    ): String {
        val dueInfo = if (dueDateEpoch != null) {
            " (scheduled for ${LedgerRepository.formatDate(dueDateEpoch)})"
        } else ""

        return when (tone) {
            ReminderTone.FLEXIBLE ->
                "Hi $name! Hope all is well. Just a quick check-in about the $amount balance$dueInfo. No pressure to pay in full right now—even a partial amount or installment is totally fine whenever convenient. Thanks! 😊"

            ReminderTone.BUDGET_NEED ->
                "Hi $name, sorry to bother you. I have some unexpected bills to settle this week, so I was wondering if it's possible to pay back part or all of the $amount$dueInfo? Even a small amount helps a lot right now. Thanks!"

            ReminderTone.ACCOUNTING ->
                "Hey $name! Balancing my personal budget and tracker today. Just wanted to check when we can update our $amount balance$dueInfo—even small installments are welcome so I can balance my ledger. Appreciate it!"

            ReminderTone.COMMITMENT ->
                "Hi $name! Following up on our $amount balance$dueInfo. Could you give me an estimated date for when you can send a payment, even partially? That way I can align my planner. Thanks so much!"
        }
    }
}
