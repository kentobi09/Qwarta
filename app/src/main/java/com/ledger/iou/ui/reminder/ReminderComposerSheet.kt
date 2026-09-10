package com.ledger.iou.ui.reminder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.ReminderLanguage
import com.ledger.iou.data.model.ReminderTemplates
import com.ledger.iou.data.model.ReminderTone
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderComposerSheet(
    debtor: PersonWithTransactions,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedLanguage by remember { mutableStateOf(ReminderLanguage.TAGALOG) }
    var selectedTone by remember { mutableStateOf(ReminderTone.FLEXIBLE) }
    var isEditing by remember { mutableStateOf(false) }

    var messageText by remember {
        mutableStateOf(
            ReminderTemplates.buildMessage(
                language = selectedLanguage,
                tone = selectedTone,
                personName = debtor.person.name,
                balanceCents = debtor.balanceCents,
                dueDateEpoch = debtor.nextUpcomingDueDate()
            )
        )
    }

    fun updateTemplate(lang: ReminderLanguage, tone: ReminderTone) {
        selectedLanguage = lang
        selectedTone = tone
        messageText = ReminderTemplates.buildMessage(
            language = lang,
            tone = tone,
            personName = debtor.person.name,
            balanceCents = debtor.balanceCents,
            dueDateEpoch = debtor.nextUpcomingDueDate()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = ColorSurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(32.dp)
                    .height(3.dp)
                    .background(ColorSurfaceBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with compact Language Pill [ PH | EN ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPOSE REMINDER",
                        style = MicroCapsStyle.copy(fontSize = 10.sp, color = ColorAccentNeutral)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = debtor.person.name,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                    )
                }

                // Sleek Language Switcher Pill in Top Right
                Row(
                    modifier = Modifier
                        .background(ColorBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selectedLanguage == ReminderLanguage.TAGALOG) ColorSurfaceCardElevated else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { updateTemplate(ReminderLanguage.TAGALOG, selectedTone) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PH",
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selectedLanguage == ReminderLanguage.TAGALOG) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedLanguage == ReminderLanguage.TAGALOG) ColorAccentPositive else ColorTextSecondary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selectedLanguage == ReminderLanguage.ENGLISH) ColorSurfaceCardElevated else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { updateTemplate(ReminderLanguage.ENGLISH, selectedTone) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EN",
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selectedLanguage == ReminderLanguage.ENGLISH) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedLanguage == ReminderLanguage.ENGLISH) ColorAccentPositive else ColorTextSecondary
                            )
                        )
                    }
                }
            }

            HairlineDivider()

            // 2x2 Clean Balanced Tone Grid (No horizontal overflow/cut-off)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT REMINDER INTENT",
                    style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary)
                )

                val templates = ReminderTemplates.availableTemplates
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToneGridButton(
                        item = templates[0],
                        isTagalog = selectedLanguage == ReminderLanguage.TAGALOG,
                        isSelected = selectedTone == templates[0].tone,
                        onClick = { updateTemplate(selectedLanguage, templates[0].tone) },
                        modifier = Modifier.weight(1f)
                    )
                    ToneGridButton(
                        item = templates[1],
                        isTagalog = selectedLanguage == ReminderLanguage.TAGALOG,
                        isSelected = selectedTone == templates[1].tone,
                        onClick = { updateTemplate(selectedLanguage, templates[1].tone) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToneGridButton(
                        item = templates[2],
                        isTagalog = selectedLanguage == ReminderLanguage.TAGALOG,
                        isSelected = selectedTone == templates[2].tone,
                        onClick = { updateTemplate(selectedLanguage, templates[2].tone) },
                        modifier = Modifier.weight(1f)
                    )
                    ToneGridButton(
                        item = templates[3],
                        isTagalog = selectedLanguage == ReminderLanguage.TAGALOG,
                        isSelected = selectedTone == templates[3].tone,
                        onClick = { updateTemplate(selectedLanguage, templates[3].tone) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Message Preview / Editor Box
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MESSAGE PREVIEW",
                        style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary)
                    )

                    Row(
                        modifier = Modifier
                            .clickable { isEditing = !isEditing }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEditing) ColorAccentPositive else ColorTextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isEditing) "Done editing" else "Customize",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = if (isEditing) ColorAccentPositive else ColorTextTertiary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 7,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = ColorTextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorBackground,
                        unfocusedContainerColor = ColorBackground,
                        focusedBorderColor = ColorAccentPositive,
                        unfocusedBorderColor = ColorSurfaceBorder,
                        cursorColor = ColorAccentPositive
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action Buttons: Copy Message + Share via Apps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Ledger Reminder", messageText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ColorSurfaceCardElevated,
                        contentColor = ColorTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COPY",
                        style = MicroCapsStyle.copy(fontSize = 11.sp, color = ColorTextPrimary)
                    )
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, messageText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Send Reminder via..."))
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorAccentPositive,
                        contentColor = ColorBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share",
                        tint = ColorBackground,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHARE VIA APPS",
                        style = MicroCapsStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorBackground
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneGridButton(
    item: com.ledger.iou.data.model.ReminderTemplateItem,
    isTagalog: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) ColorAccentPositive.copy(alpha = 0.12f) else ColorBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) ColorAccentPositive else ColorSurfaceBorder
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.shortLabel,
                style = MicroCapsStyle.copy(
                    fontSize = 9.sp,
                    color = if (isSelected) ColorAccentPositive else ColorTextTertiary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isTagalog) item.labelTagalog else item.labelEnglish,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) ColorAccentPositive else ColorTextPrimary
                ),
                maxLines = 1
            )
        }
    }
}
