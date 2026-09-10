package com.ledger.iou.ui.transaction

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val headerTitle = when {
                        isPayment -> "LOG PAYMENT"
                        isDebtorFixed -> "LEND MORE"
                        else -> "RECORD NEW LOAN"
                    }
                    val headerSubtitle = when {
                        isPayment -> "Reduces ${personNameInput.ifBlank { "debtor" }}'s loan balance"
                        isDebtorFixed -> "Adds to ${personNameInput.ifBlank { "debtor" }}'s loan balance"
                        else -> "Record money lent to an individual"
                    }

                    Text(
                        text = headerTitle,
                        style = MicroCapsStyle.copy(
                            fontSize = 13.sp,
                            color = if (isPayment) ColorAccentPositive else ColorTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = headerSubtitle,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        )
                    )
                }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Display (Standard decimal number with clear action)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurfaceCard, RoundedCornerShape(10.dp))
                    .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val amountLabel = if (isPayment) "PAYMENT AMOUNT RECEIVED (-)" else "AMOUNT LENT (+)"
                    Text(
                        text = amountLabel,
                        style = MicroCapsStyle.copy(
                            color = if (isPayment) ColorAccentPositive else ColorTextSecondary
                        )
                    )

                    if (amountInput.isNotEmpty()) {
                        Text(
                            text = "CLEAR",
                            style = MicroCapsStyle.copy(color = ColorTextSecondary),
                            modifier = Modifier
                                .clickable { amountInput = "" }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val signPrefix = if (isPayment && amountCents > 0) "- " else ""
                val displayText = if (amountInput.isEmpty()) "₱0" else "₱$amountInput"

                Text(
                    text = "$signPrefix$displayText",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                        color = if (isPayment) ColorAccentPositive else ColorTextPrimary,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debtor Section: If fixed from debt tab, just display the name uneditable (no lock word, no lock symbol)
            if (isDebtorFixed) {
                Text(
                    text = "DEBTOR",
                    style = MicroCapsStyle
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceCardElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(
                            text = personNameInput,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = ColorTextPrimary
                            )
                        )
                        selectedPerson?.let { d ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Current Balance: ${LedgerRepository.formatCents(d.balanceCents)}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                                    color = ColorTextSecondary
                                )
                            )
                        }
                    }
                }
            } else {
                // Not fixed: User opened from dashboard, allow selecting/typing debtor
                Text(
                    text = "DEBTOR / PERSON",
                    style = MicroCapsStyle
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (allDebtors.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allDebtors) { debtor ->
                            val isSelected = selectedPerson?.person?.id == debtor.person.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ColorSurfaceCardElevated else ColorSurfaceCard)
                                    .border(
                                        1.dp,
                                        if (isSelected) ColorTextPrimary else ColorSurfaceBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedPerson = null
                                            personNameInput = ""
                                            phoneNumberInput = ""
                                        } else {
                                            selectedPerson = debtor
                                            personNameInput = debtor.person.name
                                            phoneNumberInput = debtor.person.phoneNumber ?: ""
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = debtor.person.name,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ColorTextPrimary else ColorTextSecondary
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = personNameInput,
                    onValueChange = {
                        personNameInput = it
                        selectedPerson = allDebtors.find { d -> d.person.name.equals(it.trim(), ignoreCase = true) }
                    },
                    label = { Text("Full Name", style = TextStyle(fontSize = 12.sp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorSurfaceBorderHover,
                        unfocusedBorderColor = ColorSurfaceBorder,
                        focusedContainerColor = ColorSurfaceCard,
                        unfocusedContainerColor = ColorSurfaceCard,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedPerson == null || selectedPerson?.person?.phoneNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneNumberInput,
                        onValueChange = { phoneNumberInput = it },
                        label = { Text("Phone Number (Optional)", style = TextStyle(fontSize = 12.sp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorSurfaceBorderHover,
                            unfocusedBorderColor = ColorSurfaceBorder,
                            focusedContainerColor = ColorSurfaceCard,
                            unfocusedContainerColor = ColorSurfaceCard,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optional Note
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = {
                    Text(
                        if (isPayment) "Note (e.g. Cash, GCash, Bank transfer)" else "Note / Reason (e.g. Dinner, Emergency, Groceries)",
                        style = TextStyle(fontSize = 12.sp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorSurfaceBorderHover,
                    unfocusedBorderColor = ColorSurfaceBorder,
                    focusedContainerColor = ColorSurfaceCard,
                    unfocusedContainerColor = ColorSurfaceCard,
                    focusedTextColor = ColorTextPrimary,
                    unfocusedTextColor = ColorTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Agreed Interest (only shown when lending money, never for payments)
            if (!isPayment) {
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorSurfaceCard)
                        .border(1.dp, if (isInterestEnabled) ColorSurfaceBorderHover else ColorSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInterestEnabled = !isInterestEnabled },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AGREED INTEREST (OPTIONAL)",
                                style = MicroCapsStyle.copy(
                                    color = if (isInterestEnabled) ColorTextPrimary else ColorTextSecondary
                                )
                            )
                            Text(
                                text = if (isInterestEnabled) "Interest added to total repayable" else "No interest (0% standard personal loan)",
                                style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary)
                            )
                        }

                        Switch(
                            checked = isInterestEnabled,
                            onCheckedChange = { isInterestEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColorBackground,
                                checkedTrackColor = ColorTextPrimary,
                                uncheckedThumbColor = ColorTextSecondary,
                                uncheckedTrackColor = ColorSurfaceCardElevated,
                                uncheckedBorderColor = ColorSurfaceBorder
                            )
                        )
                    }

                    if (isInterestEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HairlineDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle between Percentage (%) and Fixed Amount (₱)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (interestMode == 0) ColorSurfaceCardElevated else ColorSurfaceCard)
                                    .border(
                                        1.dp,
                                        if (interestMode == 0) ColorTextPrimary else ColorSurfaceBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        interestMode = 0
                                        fixedInterestInput = ""
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "% RATE",
                                    style = MicroCapsStyle.copy(
                                        color = if (interestMode == 0) ColorTextPrimary else ColorTextSecondary,
                                        fontWeight = if (interestMode == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (interestMode == 1) ColorSurfaceCardElevated else ColorSurfaceCard)
                                    .border(
                                        1.dp,
                                        if (interestMode == 1) ColorTextPrimary else ColorSurfaceBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        interestMode = 1
                                        selectedPercentPreset = null
                                        customPercentInput = ""
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "FIXED ₱ AMOUNT",
                                    style = MicroCapsStyle.copy(
                                        color = if (interestMode == 1) ColorTextPrimary else ColorTextSecondary,
                                        fontWeight = if (interestMode == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (interestMode == 0) {
                            // Preset percentage chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5.0, 10.0, 15.0, 20.0).forEach { preset ->
                                    val isSelected = selectedPercentPreset == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) ColorSurfaceCardElevated else ColorSurfaceCard)
                                            .border(
                                                1.dp,
                                                if (isSelected) ColorAccentPositive else ColorSurfaceBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                selectedPercentPreset = if (isSelected) null else preset
                                                customPercentInput = ""
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${preset.toInt()}%",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ColorAccentPositive else ColorTextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customPercentInput,
                                onValueChange = {
                                    customPercentInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    if (it.isNotEmpty()) selectedPercentPreset = null
                                },
                                label = { Text("Custom Rate % (e.g. 7.5)", style = TextStyle(fontSize = 12.sp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorSurfaceBorderHover,
                                    unfocusedBorderColor = ColorSurfaceBorder,
                                    focusedContainerColor = ColorSurfaceCard,
                                    unfocusedContainerColor = ColorSurfaceCard,
                                    focusedTextColor = ColorTextPrimary,
                                    unfocusedTextColor = ColorTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = fixedInterestInput,
                                onValueChange = {
                                    fixedInterestInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                                },
                                label = { Text("Fixed Interest Amount (₱)", style = TextStyle(fontSize = 12.sp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorSurfaceBorderHover,
                                    unfocusedBorderColor = ColorSurfaceBorder,
                                    focusedContainerColor = ColorSurfaceCard,
                                    unfocusedContainerColor = ColorSurfaceCard,
                                    focusedTextColor = ColorTextPrimary,
                                    unfocusedTextColor = ColorTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Live summary breakdown when interest > 0
                        if (amountCents > 0 && computedInterestCents > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorSurfaceCardElevated, RoundedCornerShape(6.dp))
                                    .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Principal:", style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary))
                                        Text(
                                            text = LedgerRepository.formatCents(amountCents),
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ColorTextPrimary)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val rateLabel = computedInterestRatePercent?.let { " (${it}%)" } ?: ""
                                        Text(text = "Agreed Interest$rateLabel:", style = TextStyle(fontSize = 11.sp, color = ColorAccentPositive))
                                        Text(
                                            text = "+ ${LedgerRepository.formatCents(computedInterestCents)}",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ColorAccentPositive)
                                        )
                                    }
                                    HairlineDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Repayable:", style = MicroCapsStyle.copy(fontSize = 10.sp, color = ColorTextPrimary))
                                        Text(
                                            text = LedgerRepository.formatCents(amountCents + computedInterestCents),
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
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

            // Due Date selector (only shown when lending money, never for payments)
            if (!isPayment) {
                Spacer(modifier = Modifier.height(14.dp))
                QuickDueDateSelector(
                    selectedOption = dueDateOption,
                    customEpochMillis = customDueDateEpoch,
                    onOptionSelected = { option, epoch ->
                        dueDateOption = option
                        customDueDateEpoch = epoch
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad with decimal point and standard calculator behavior
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
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Submit Button
            val isFormValid = personNameInput.isNotBlank() && amountCents > 0
            val confirmButtonLabel = when {
                isPayment -> "CONFIRM PAYMENT"
                isDebtorFixed -> "CONFIRM ADDITIONAL LOAN"
                else -> "CONFIRM LOAN"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmButtonLabel,
                    style = MicroCapsStyle.copy(
                        color = if (isFormValid) ColorBackground else ColorTextSecondary,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
