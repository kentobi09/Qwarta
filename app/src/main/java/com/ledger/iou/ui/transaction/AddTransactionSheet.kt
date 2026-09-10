package com.ledger.iou.ui.transaction

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import java.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.TransactionType
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.ui.components.DueDateOption
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.components.MonospaceKeypad
import com.ledger.iou.ui.components.QuickDueDateSelector
import com.ledger.iou.ui.theme.ColorAccentNeutral
import com.ledger.iou.ui.theme.ColorAccentPositive
import com.ledger.iou.ui.theme.ColorBackground
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.theme.TABULAR_NUMERALS_SETTINGS
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    repository: LedgerRepository,
    preselectedPersonId: String?,
    initialTransactionType: String = TransactionType.LENT,
    onDismiss: () -> Unit,
    onTransactionSaved: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val allDebtors by repository.allPersonsWithTransactions.collectAsState(initial = emptyList())

    // If preselectedPersonId is non-null, the debtor is strictly fixed to that person
    val isDebtorFixed = !preselectedPersonId.isNullOrBlank()

    // Transaction type is fixed based on entry context (REPAYMENT for Log Payment, LENT for lend/dashboard)
    val transactionType = initialTransactionType
    val isPayment = transactionType == TransactionType.REPAYMENT

    var selectedPerson by remember { mutableStateOf<PersonWithTransactions?>(null) }
    var personNameInput by remember { mutableStateOf("") }
    var phoneNumberInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    var showPersonSelectDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showCustomInterestDialog by remember { mutableStateOf(false) }
    
    // Standard calculator decimal input (not ATM-style)
    var amountInput by remember { mutableStateOf("") }

    // Optional Agreed Interest state (only applicable when lending)
    var isInterestEnabled by remember { mutableStateOf(false) }
    var interestMode by remember { mutableStateOf(0) } // 0 = Percentage (%), 1 = Fixed Amount (₱)
    var selectedPercentPreset by remember { mutableStateOf<Double?>(null) }
    var customPercentInput by remember { mutableStateOf("") }
    var fixedInterestInput by remember { mutableStateOf("") }

    // Convert decimal input to cents
    val amountCents = remember(amountInput) {
        if (amountInput.isBlank() || amountInput == ".") {
            0L
        } else {
            try {
                val parts = amountInput.split('.')
                val whole = parts[0].toLongOrNull() ?: 0L
                val fraction = if (parts.size > 1) {
                    val decStr = parts[1].take(2).padEnd(2, '0')
                    decStr.toLongOrNull() ?: 0L
                } else 0L
                (whole * 100) + fraction
            } catch (_: Exception) {
                0L
            }
        }
    }

    // Computed interest amount in cents and rate percent
    val computedInterestRatePercent = remember(isInterestEnabled, interestMode, selectedPercentPreset, customPercentInput) {
        if (!isInterestEnabled || isPayment) null
        else if (interestMode == 0) {
            selectedPercentPreset ?: customPercentInput.toDoubleOrNull()
        } else null
    }

    val computedInterestCents = remember(isInterestEnabled, isPayment, interestMode, computedInterestRatePercent, fixedInterestInput, amountCents) {
        if (!isInterestEnabled || isPayment || amountCents <= 0) 0L
        else if (interestMode == 0) {
            val rate = computedInterestRatePercent ?: 0.0
            (amountCents * (rate / 100.0)).toLong()
        } else {
            if (fixedInterestInput.isBlank() || fixedInterestInput == ".") 0L
            else {
                try {
                    val parts = fixedInterestInput.split('.')
                    val whole = parts[0].toLongOrNull() ?: 0L
                    val fraction = if (parts.size > 1) {
                        val decStr = parts[1].take(2).padEnd(2, '0')
                        decStr.toLongOrNull() ?: 0L
                    } else 0L
                    (whole * 100) + fraction
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }

    var dueDateOption by remember { mutableStateOf(DueDateOption.NONE) }
    var customDueDateEpoch by remember { mutableStateOf<Long?>(null) }

    // Pre-select person if passed
    LaunchedEffect(preselectedPersonId, allDebtors) {
        if (isDebtorFixed) {
            val found = allDebtors.find { it.person.id == preselectedPersonId }
            if (found != null) {
                selectedPerson = found
                personNameInput = found.person.name
                phoneNumberInput = found.person.phoneNumber ?: ""
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorBackground,
        dragHandle = null,
        tonalElevation = 0.dp
    ) {
        // Single non-scrolling passive screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerTitle = when {
                    isPayment -> "LOG PAYMENT"
                    isDebtorFixed -> "LEND MORE"
                    else -> "RECORD LOAN"
                }
                Text(
                    text = headerTitle,
                    style = MicroCapsStyle.copy(
                        fontSize = 12.sp,
                        color = if (isPayment) ColorAccentPositive else ColorTextPrimary
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Primary Amount Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ColorSurfaceCard)
                    .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val amountLabel = if (isPayment) "PAYMENT RECEIVED (-)" else "AMOUNT LENT (+)"
                        Text(
                            text = amountLabel,
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (isPayment) ColorAccentPositive else ColorTextSecondary
                            )
                        )

                        if (amountInput.isNotEmpty()) {
                            Text(
                                text = "CLEAR",
                                style = MicroCapsStyle.copy(fontSize = 10.sp, color = ColorTextSecondary),
                                modifier = Modifier
                                    .clickable { amountInput = "" }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val signPrefix = if (isPayment && amountCents > 0) "- " else ""
                    val displayText = if (amountInput.isEmpty()) "₱0" else "₱$amountInput"

                    Text(
                        text = "$signPrefix$displayText",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                            color = if (isPayment) ColorAccentPositive else ColorTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )

                    // Compact live breakdown if interest is added
                    if (!isPayment && computedInterestCents > 0) {
                        val rateLabel = computedInterestRatePercent?.let { " (${it}%)" } ?: ""
                        Text(
                            text = "+ ${LedgerRepository.formatCents(computedInterestCents)} int$rateLabel  •  Total ${LedgerRepository.formatCents(amountCents + computedInterestCents)}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = ColorAccentPositive
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Debtor & Note Row (Compact pill buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Debtor Chip
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceCard)
                        .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isDebtorFixed) { showPersonSelectDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "DEBTOR",
                            style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary)
                        )
                        Text(
                            text = personNameInput.ifBlank { "Select or add debtor" },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (personNameInput.isNotBlank()) ColorTextPrimary else ColorAccentNeutral
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Note Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceCard)
                        .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { showNoteDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "NOTE",
                            style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary)
                        )
                        Text(
                            text = noteInput.ifBlank { "+ Add note" },
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = if (noteInput.isNotBlank()) ColorTextPrimary else ColorTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Repayment Due Date & Interest Chips (Lend mode only)
            if (!isPayment) {
                Spacer(modifier = Modifier.height(8.dp))

                // Due Date Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DUE",
                        style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary),
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    // NO DUE
                    val isNoDue = dueDateOption == DueDateOption.NONE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNoDue) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (isNoDue) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                dueDateOption = DueDateOption.NONE
                                customDueDateEpoch = null
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NONE",
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (isNoDue) ColorTextPrimary else ColorTextSecondary
                            )
                        )
                    }

                    // 1 WEEK
                    val is1Wk = dueDateOption == DueDateOption.ONE_WEEK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is1Wk) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (is1Wk) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                dueDateOption = DueDateOption.ONE_WEEK
                                customDueDateEpoch = System.currentTimeMillis() + (7L * 24 * 3600 * 1000)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1 WK",
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (is1Wk) ColorTextPrimary else ColorTextSecondary
                            )
                        )
                    }

                    // 1 MONTH
                    val is1Mo = dueDateOption == DueDateOption.ONE_MONTH
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is1Mo) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (is1Mo) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                dueDateOption = DueDateOption.ONE_MONTH
                                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
                                customDueDateEpoch = cal.timeInMillis
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1 MO",
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (is1Mo) ColorTextPrimary else ColorTextSecondary
                            )
                        )
                    }

                    // CUSTOM DATE PICKER
                    val isCustomDue = dueDateOption == DueDateOption.CUSTOM
                    val dueEpoch = customDueDateEpoch
                    val customDueLabel = if (isCustomDue && dueEpoch != null) {
                        LedgerRepository.formatDate(dueEpoch)
                    } else "DATE"

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCustomDue) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (isCustomDue) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, 23)
                                            set(Calendar.MINUTE, 59)
                                        }
                                        dueDateOption = DueDateOption.CUSTOM
                                        customDueDateEpoch = selected.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customDueLabel,
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (isCustomDue) ColorTextPrimary else ColorTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Agreed Interest Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INT",
                        style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // 0% (NO INTEREST)
                    val isZeroInt = !isInterestEnabled || (selectedPercentPreset == null && customPercentInput.isBlank() && fixedInterestInput.isBlank())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isZeroInt) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (isZeroInt) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                isInterestEnabled = false
                                selectedPercentPreset = null
                                customPercentInput = ""
                                fixedInterestInput = ""
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0%",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isZeroInt) FontWeight.Bold else FontWeight.Normal,
                                color = if (isZeroInt) ColorTextPrimary else ColorTextSecondary
                            )
                        )
                    }

                    // 5% PRESET
                    val is5Percent = isInterestEnabled && interestMode == 0 && selectedPercentPreset == 5.0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is5Percent) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (is5Percent) ColorAccentPositive else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                isInterestEnabled = true
                                interestMode = 0
                                selectedPercentPreset = 5.0
                                customPercentInput = ""
                                fixedInterestInput = ""
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "5%",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (is5Percent) FontWeight.Bold else FontWeight.Normal,
                                color = if (is5Percent) ColorAccentPositive else ColorTextSecondary
                            )
                        )
                    }

                    // 10% PRESET
                    val is10Percent = isInterestEnabled && interestMode == 0 && selectedPercentPreset == 10.0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is10Percent) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (is10Percent) ColorAccentPositive else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                isInterestEnabled = true
                                interestMode = 0
                                selectedPercentPreset = 10.0
                                customPercentInput = ""
                                fixedInterestInput = ""
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "10%",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (is10Percent) FontWeight.Bold else FontWeight.Normal,
                                color = if (is10Percent) ColorAccentPositive else ColorTextSecondary
                            )
                        )
                    }

                    // 20% PRESET
                    val is20Percent = isInterestEnabled && interestMode == 0 && selectedPercentPreset == 20.0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (is20Percent) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (is20Percent) ColorAccentPositive else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                isInterestEnabled = true
                                interestMode = 0
                                selectedPercentPreset = 20.0
                                customPercentInput = ""
                                fixedInterestInput = ""
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "20%",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (is20Percent) FontWeight.Bold else FontWeight.Normal,
                                color = if (is20Percent) ColorAccentPositive else ColorTextSecondary
                            )
                        )
                    }

                    // CUSTOM INTEREST
                    val isCustomInt = isInterestEnabled && (customPercentInput.isNotBlank() || fixedInterestInput.isNotBlank())
                    val customIntLabel = if (isCustomInt) {
                        if (interestMode == 0) "$customPercentInput%" else "₱$fixedInterestInput"
                    } else "MORE"

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCustomInt) ColorSurfaceCardElevated else ColorSurfaceCard)
                            .border(1.dp, if (isCustomInt) ColorAccentPositive else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                            .clickable { showCustomInterestDialog = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customIntLabel,
                            style = MicroCapsStyle.copy(
                                fontSize = 10.sp,
                                color = if (isCustomInt) ColorAccentPositive else ColorTextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Monospace Keypad with compact 44.dp height
            val isFormValid = personNameInput.isNotBlank() && amountCents > 0
            val confirmButtonLabel = when {
                isPayment -> "CONFIRM PAYMENT"
                isDebtorFixed -> "CONFIRM ADDITIONAL LOAN"
                else -> "CONFIRM LOAN"
            }

            MonospaceKeypad(
                onDigitPress = { digit ->
                    if (amountInput == "0") {
                        amountInput = digit.toString()
                    } else {
                        val decimalIndex = amountInput.indexOf('.')
                        if (decimalIndex != -1) {
                            val decimals = amountInput.length - decimalIndex - 1
                            if (decimals < 2) {
                                amountInput += digit.toString()
                            }
                        } else {
                            if (amountInput.length < 8) {
                                amountInput += digit.toString()
                            }
                        }
                    }
                },
                onDecimalPoint = {
                    if (!amountInput.contains('.')) {
                        amountInput = if (amountInput.isEmpty()) "0." else "$amountInput."
                    }
                },
                onBackspace = {
                    if (amountInput.isNotEmpty()) {
                        amountInput = amountInput.dropLast(1)
                    }
                },
                onClear = {
                    amountInput = ""
                },
                keyHeight = 44.dp,
                customBottomRow = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Decimal point
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorSurfaceCard)
                                .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!amountInput.contains('.')) {
                                        amountInput = if (amountInput.isEmpty()) "0." else "$amountInput."
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "•",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = ColorTextPrimary
                                )
                            )
                        }

                        // 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorSurfaceCard)
                                .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (amountInput != "0") {
                                        val decimalIndex = amountInput.indexOf('.')
                                        if (decimalIndex != -1) {
                                            val decimals = amountInput.length - decimalIndex - 1
                                            if (decimals < 2) amountInput += "0"
                                        } else {
                                            if (amountInput.length < 8 && amountInput.isNotEmpty()) amountInput += "0"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "0",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                                    color = ColorTextPrimary
                                )
                            )
                        }

                        // Backspace
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorSurfaceCard)
                                .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (amountInput.isNotEmpty()) {
                                        amountInput = amountInput.dropLast(1)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Confirm Button (Always visible at bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFormValid) {
                            if (isPayment) ColorAccentPositive else ColorTextPrimary
                        } else ColorSurfaceCardElevated
                    )
                    .border(
                        1.dp,
                        if (isFormValid) {
                            if (isPayment) ColorAccentPositive else ColorTextPrimary
                        } else ColorSurfaceBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = isFormValid) {
                        scope.launch {
                            val computedDueDate = if (!isPayment) customDueDateEpoch else null

                            val targetPersonId = repository.recordLoan(
                                personId = selectedPerson?.person?.id ?: preselectedPersonId,
                                personName = personNameInput,
                                phoneNumber = phoneNumberInput,
                                amountCents = amountCents,
                                type = transactionType,
                                note = noteInput,
                                dueDateEpoch = computedDueDate,
                                interestRatePercent = computedInterestRatePercent,
                                interestAmountCents = computedInterestCents
                            )
                            val feedback = if (isPayment) {
                                "Payment of ${LedgerRepository.formatCents(amountCents)} recorded"
                            } else {
                                if (computedInterestCents > 0) {
                                    "Loan of ${LedgerRepository.formatCents(amountCents)} (+${LedgerRepository.formatCents(computedInterestCents)} int) recorded"
                                } else {
                                    "Loan of ${LedgerRepository.formatCents(amountCents)} recorded"
                                }
                            }
                            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                            onTransactionSaved(targetPersonId)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmButtonLabel,
                    style = MicroCapsStyle.copy(
                        fontSize = 12.sp,
                        color = if (isFormValid) ColorBackground else ColorTextSecondary,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }

    // 7. Select Debtor Dialog
    if (showPersonSelectDialog) {
        var tempName by remember { mutableStateOf(personNameInput) }
        var tempPhone by remember { mutableStateOf(phoneNumberInput) }

        AlertDialog(
            onDismissRequest = { showPersonSelectDialog = false },
            containerColor = ColorSurfaceCard,
            title = {
                Text("SELECT OR ADD DEBTOR", style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (allDebtors.isNotEmpty()) {
                        Text("Existing contacts:", style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allDebtors) { debtor ->
                                val isSelected = tempName.equals(debtor.person.name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) ColorSurfaceCardElevated else ColorBackground)
                                        .border(1.dp, if (isSelected) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedPerson = debtor
                                            tempName = debtor.person.name
                                            tempPhone = debtor.person.phoneNumber ?: ""
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = debtor.person.name,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ColorTextPrimary else ColorTextSecondary
                                        )
                                    )
                                }
                            }
                        }
                        HairlineDivider()
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = {
                            tempName = it
                            selectedPerson = allDebtors.find { d -> d.person.name.equals(it.trim(), ignoreCase = true) }
                        },
                        label = { Text("Full Name", style = TextStyle(fontSize = 12.sp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorSurfaceBorderHover,
                            unfocusedBorderColor = ColorSurfaceBorder,
                            focusedContainerColor = ColorBackground,
                            unfocusedContainerColor = ColorBackground,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone Number (Optional)", style = TextStyle(fontSize = 12.sp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorSurfaceBorderHover,
                            unfocusedBorderColor = ColorSurfaceBorder,
                            focusedContainerColor = ColorBackground,
                            unfocusedContainerColor = ColorBackground,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            personNameInput = tempName.trim()
                            phoneNumberInput = tempPhone.trim()
                            showPersonSelectDialog = false
                        }
                    }
                ) {
                    Text("DONE", style = MicroCapsStyle.copy(color = ColorTextPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonSelectDialog = false }) {
                    Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                }
            }
        )
    }

    // 8. Note Dialog
    if (showNoteDialog) {
        var tempNote by remember { mutableStateOf(noteInput) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor = ColorSurfaceCard,
            title = {
                Text("TRANSACTION NOTE", style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary))
            },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    label = { Text("e.g. Dinner, Emergency, GCash", style = TextStyle(fontSize = 12.sp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorSurfaceBorderHover,
                        unfocusedBorderColor = ColorSurfaceBorder,
                        focusedContainerColor = ColorBackground,
                        unfocusedContainerColor = ColorBackground,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteInput = tempNote.trim()
                        showNoteDialog = false
                    }
                ) {
                    Text("SAVE", style = MicroCapsStyle.copy(color = ColorTextPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                }
            }
        )
    }

    // 9. Custom Interest Dialog
    if (showCustomInterestDialog) {
        var dialogMode by remember { mutableStateOf(interestMode) }
        var dialogPercent by remember { mutableStateOf(customPercentInput) }
        var dialogFixed by remember { mutableStateOf(fixedInterestInput) }

        AlertDialog(
            onDismissRequest = { showCustomInterestDialog = false },
            containerColor = ColorSurfaceCard,
            title = {
                Text("CUSTOM AGREED INTEREST", style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dialogMode == 0) ColorSurfaceCardElevated else ColorBackground)
                                .border(1.dp, if (dialogMode == 0) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable { dialogMode = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("% RATE", style = MicroCapsStyle.copy(fontSize = 10.sp, color = if (dialogMode == 0) ColorTextPrimary else ColorTextSecondary))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dialogMode == 1) ColorSurfaceCardElevated else ColorBackground)
                                .border(1.dp, if (dialogMode == 1) ColorTextPrimary else ColorSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable { dialogMode = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FIXED ₱", style = MicroCapsStyle.copy(fontSize = 10.sp, color = if (dialogMode == 1) ColorTextPrimary else ColorTextSecondary))
                        }
                    }

                    if (dialogMode == 0) {
                        OutlinedTextField(
                            value = dialogPercent,
                            onValueChange = { dialogPercent = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Interest Rate % (e.g. 7.5)", style = TextStyle(fontSize = 12.sp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorSurfaceBorderHover,
                                unfocusedBorderColor = ColorSurfaceBorder,
                                focusedContainerColor = ColorBackground,
                                unfocusedContainerColor = ColorBackground,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = dialogFixed,
                            onValueChange = { dialogFixed = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Fixed Amount ₱ (e.g. 250)", style = TextStyle(fontSize = 12.sp)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorSurfaceBorderHover,
                                unfocusedBorderColor = ColorSurfaceBorder,
                                focusedContainerColor = ColorBackground,
                                unfocusedContainerColor = ColorBackground,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isInterestEnabled = true
                        interestMode = dialogMode
                        if (dialogMode == 0) {
                            customPercentInput = dialogPercent
                            selectedPercentPreset = null
                            fixedInterestInput = ""
                        } else {
                            fixedInterestInput = dialogFixed
                            selectedPercentPreset = null
                            customPercentInput = ""
                        }
                        showCustomInterestDialog = false
                    }
                ) {
                    Text("APPLY", style = MicroCapsStyle.copy(color = ColorTextPrimary))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomInterestDialog = false }) {
                    Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                }
            }
        )
    }
}
