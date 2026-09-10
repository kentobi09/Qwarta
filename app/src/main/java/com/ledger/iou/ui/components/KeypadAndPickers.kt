package com.ledger.iou.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.ui.theme.ColorAccentNeutral
import com.ledger.iou.ui.theme.ColorAccentPositive
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.theme.TABULAR_NUMERALS_SETTINGS
import java.util.Calendar

@Composable
fun MonospaceKeypad(
    onDigitPress: (Int) -> Unit,
    onDecimalPoint: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: androidx.compose.ui.unit.Dp = 52.dp,
    customBottomRow: (@Composable () -> Unit)? = null
) {
    val view = androidx.compose.ui.platform.LocalView.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Keypad grid: 1-9
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorSurfaceCard)
                            .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onDigitPress(key.toInt())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                                color = ColorTextPrimary
                            )
                        )
                    }
                }
            }
        }

        if (customBottomRow != null) {
            customBottomRow()
        } else {
            // Standard bottom row: ., 0, DEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(".", "0", "DEL").forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorSurfaceCard)
                            .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                when (key) {
                                    "." -> onDecimalPoint()
                                    "DEL" -> onBackspace()
                                    else -> onDigitPress(key.toInt())
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "." -> {
                                Text(
                                    text = "•",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        color = ColorTextPrimary
                                    )
                                )
                            }
                            "DEL" -> {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = ColorTextSecondary
                                )
                            }
                            else -> {
                                Text(
                                    text = key,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                                        color = ColorTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DueDateOption {
    NONE, ONE_WEEK, ONE_MONTH, CUSTOM
}

@Composable
fun QuickDueDateSelector(
    selectedOption: DueDateOption,
    customEpochMillis: Long?,
    onOptionSelected: (DueDateOption, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "REPAYMENT DUE DATE",
            style = MicroCapsStyle
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DueDateChip(
                label = "NO DUE DATE",
                isSelected = selectedOption == DueDateOption.NONE,
                onClick = { onOptionSelected(DueDateOption.NONE, null) },
                modifier = Modifier.weight(1f)
            )

            DueDateChip(
                label = "1 WEEK",
                isSelected = selectedOption == DueDateOption.ONE_WEEK,
                onClick = {
                    val oneWeek = System.currentTimeMillis() + (7L * 24 * 3600 * 1000)
                    onOptionSelected(DueDateOption.ONE_WEEK, oneWeek)
                },
                modifier = Modifier.weight(1f)
            )

            DueDateChip(
                label = "1 MONTH",
                isSelected = selectedOption == DueDateOption.ONE_MONTH,
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, 1)
                    onOptionSelected(DueDateOption.ONE_MONTH, cal.timeInMillis)
                },
                modifier = Modifier.weight(1f)
            )

            val customLabel = if (selectedOption == DueDateOption.CUSTOM && customEpochMillis != null) {
                LedgerRepository.formatDate(customEpochMillis)
            } else {
                "CUSTOM"
            }

            DueDateChip(
                label = customLabel,
                isSelected = selectedOption == DueDateOption.CUSTOM,
                showCalendarIcon = selectedOption != DueDateOption.CUSTOM,
                onClick = {
                    val cal = Calendar.getInstance()
                    val dialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selected = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                            }
                            onOptionSelected(DueDateOption.CUSTOM, selected.timeInMillis)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    dialog.show()
                },
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun DueDateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCalendarIcon: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ColorSurfaceCardElevated else ColorSurfaceCard)
            .border(
                1.dp,
                if (isSelected) ColorAccentNeutral else ColorSurfaceBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showCalendarIcon) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (isSelected) ColorTextPrimary else ColorTextSecondary,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .width(12.dp)
                        .height(12.dp)
                )
            }
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = if (isSelected) ColorTextPrimary else ColorTextSecondary,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}
