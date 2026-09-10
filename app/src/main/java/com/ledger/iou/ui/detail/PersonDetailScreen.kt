package com.ledger.iou.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.TransactionType
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.data.repository.TransactionWithRunningBalance
import com.ledger.iou.ui.components.AmountText
import com.ledger.iou.ui.components.HairlineCard
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.components.MicroCapsLabel
import com.ledger.iou.ui.components.StatusPill
import com.ledger.iou.ui.theme.ColorAccentNegative
import com.ledger.iou.ui.theme.ColorAccentNegativeMuted
import com.ledger.iou.ui.theme.ColorAccentNeutral
import com.ledger.iou.ui.theme.ColorAccentNeutralMuted
import com.ledger.iou.ui.theme.ColorAccentPositive
import com.ledger.iou.ui.theme.ColorAccentPositiveMuted
import com.ledger.iou.ui.theme.ColorBackground
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.theme.TABULAR_NUMERALS_SETTINGS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    viewModel: PersonDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenAddTransaction: (String, String) -> Unit, // (personId, transactionType)
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSettleDialog by remember { mutableStateOf(false) }
    var showDeletePersonDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<LoanTransactionEntity?>(null) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is PersonDetailUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorTextPrimary, strokeWidth = 2.dp)
            }
        }
        is PersonDetailUiState.NotFound -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Contact not found",
                    style = TextStyle(color = ColorTextSecondary, fontSize = 16.sp)
                )
            }
        }
        is PersonDetailUiState.Success -> {
            val debtor = state.debtor
            val isPrivacyMasked = state.isPrivacyMasked
            val isOverdue = debtor.isOverdue()
            val isSettled = debtor.isSettled

            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = ColorBackground,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = debtor.person.name.uppercase(),
                                style = MicroCapsStyle.copy(
                                    fontSize = 13.sp,
                                    color = ColorTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = ColorTextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.togglePrivacyMask() }) {
                                Icon(
                                    imageVector = if (isPrivacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Privacy Toggle",
                                    tint = if (isPrivacyMasked) ColorAccentNegative else ColorTextSecondary
                                )
                            }
                            IconButton(onClick = { showEditContactDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Contact",
                                    tint = ColorTextSecondary
                                )
                            }
                            IconButton(onClick = { showDeletePersonDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Contact",
                                    tint = ColorAccentNegative
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ColorBackground,
                            titleContentColor = ColorTextPrimary
                        )
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Debtor Balance Card
                    item {
                        HairlineCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = ColorSurfaceCard
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSettled) "ACCOUNT STATUS" else "CURRENT OUTSTANDING BALANCE",
                                        style = MicroCapsStyle
                                    )
                                    StatusPill(
                                        isOverdue = isOverdue,
                                        isSettled = isSettled,
                                        relativeDueText = debtor.nextUpcomingDueDate()?.let {
                                            LedgerRepository.formatRelativeDue(it)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (isSettled) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = ColorAccentPositive,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "ALL LOANS CLEARED",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ColorAccentPositive
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Zero balance remaining • 100% repaid",
                                                style = TextStyle(
                                                    fontSize = 12.sp,
                                                    color = ColorTextSecondary
                                                )
                                            )
                                        }

                                        AmountText(
                                            cents = 0L,
                                            isMasked = isPrivacyMasked,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorAccentPositive,
                                            useMonospace = true
                                        )
                                    }
                                } else {
                                    AmountText(
                                        cents = debtor.balanceCents,
                                        isMasked = isPrivacyMasked,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isOverdue -> ColorAccentNegative
                                            else -> ColorAccentNeutral
                                        },
                                        useMonospace = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HairlineDivider()
                                Spacer(modifier = Modifier.height(14.dp))

                                // Sub-stats: Total Lent vs Total Received
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "TOTAL LENT", style = MicroCapsStyle.copy(fontSize = 9.sp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        AmountText(
                                            cents = debtor.totalLentCents,
                                            isMasked = isPrivacyMasked,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ColorTextSecondary,
                                            useMonospace = true
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "TOTAL RECEIVED",
                                            style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorAccentPositive)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        AmountText(
                                            cents = debtor.totalRepaidCents,
                                            isMasked = isPrivacyMasked,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ColorAccentPositive,
                                            useMonospace = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Row: Only displayed for active debts
                    if (!isSettled) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PrimaryActionButton(
                                    icon = Icons.Default.Payments,
                                    label = "LOG PAYMENT",
                                    onClick = { onOpenAddTransaction(debtor.person.id, TransactionType.REPAYMENT) },
                                    accentColor = ColorAccentPositive,
                                    modifier = Modifier.weight(1.1f)
                                )

                                PrimaryActionButton(
                                    icon = Icons.Default.Add,
                                    label = "LEND MORE",
                                    onClick = { onOpenAddTransaction(debtor.person.id, TransactionType.LENT) },
                                    accentColor = ColorTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                PrimaryActionButton(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    label = "REMINDER",
                                    onClick = { showReminderSheet = true },
                                    modifier = Modifier.weight(0.9f)
                                )

                                PrimaryActionButton(
                                    icon = Icons.Default.CheckCircle,
                                    label = "SETTLE",
                                    onClick = { showSettleDialog = true },
                                    accentColor = ColorAccentPositive,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                        }
                    }

                    // Timeline Audit Log Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEDGER TIMELINE & AUDIT TRAIL",
                                style = MicroCapsStyle
                            )
                            Text(
                                text = "${state.timeline.size} ENTRIES",
                                style = MicroCapsStyle.copy(fontSize = 9.sp, color = ColorTextSecondary)
                            )
                        }
                    }

                    // Timeline Items
                    if (state.timeline.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions recorded yet.",
                                    style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = state.timeline,
                            key = { it.transaction.id }
                        ) { item ->
                            TimelineEntryCard(
                                item = item,
                                isPrivacyMasked = isPrivacyMasked,
                                onDeleteClick = { transactionToDelete = item.transaction }
                            )
                        }
                    }
                }
            }

            // Settle Confirmation Dialog
            if (showSettleDialog) {
                AlertDialog(
                    onDismissRequest = { showSettleDialog = false },
                    containerColor = ColorSurfaceCard,
                    title = {
                        Text(
                            text = "SETTLE FULL BALANCE",
                            style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorAccentPositive)
                        )
                    },
                    text = {
                        Text(
                            text = "Record a full settlement payment of ${LedgerRepository.formatCents(debtor.balanceCents)} from ${debtor.person.name}? This will zero out the outstanding balance.",
                            style = TextStyle(color = ColorTextSecondary, fontSize = 14.sp)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.settleAll(debtor.balanceCents)
                                showSettleDialog = false
                                Toast.makeText(context, "Balance marked as settled", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("CONFIRM SETTLEMENT", style = MicroCapsStyle.copy(color = ColorAccentPositive))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettleDialog = false }) {
                            Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                        }
                    }
                )
            }

            // Delete Transaction Dialog
            transactionToDelete?.let { tx ->
                val typeName = if (tx.type == TransactionType.LENT) "MONEY LENT" else "PAYMENT RECEIVED"
                AlertDialog(
                    onDismissRequest = { transactionToDelete = null },
                    containerColor = ColorSurfaceCard,
                    title = {
                        Text(
                            text = "DELETE TRANSACTION",
                            style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorAccentNegative)
                        )
                    },
                    text = {
                        Text(
                            text = "Remove this $typeName entry of ${LedgerRepository.formatCents(tx.amount)} from the audit log?",
                            style = TextStyle(color = ColorTextSecondary, fontSize = 14.sp)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteTransaction(tx)
                                transactionToDelete = null
                                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("DELETE", style = MicroCapsStyle.copy(color = ColorAccentNegative))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { transactionToDelete = null }) {
                            Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                        }
                    }
                )
            }

            // Delete Person Dialog
            if (showDeletePersonDialog) {
                AlertDialog(
                    onDismissRequest = { showDeletePersonDialog = false },
                    containerColor = ColorSurfaceCard,
                    title = {
                        Text(
                            text = "DELETE CONTACT & AUDIT",
                            style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorAccentNegative)
                        )
                    },
                    text = {
                        Text(
                            text = "Permanently remove ${debtor.person.name} and all associated ledger records? This cannot be undone.",
                            style = TextStyle(color = ColorTextSecondary, fontSize = 14.sp)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePerson {
                                    showDeletePersonDialog = false
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Text("DELETE PERMANENTLY", style = MicroCapsStyle.copy(color = ColorAccentNegative))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeletePersonDialog = false }) {
                            Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                        }
                    }
                )
            }

            // Edit Contact Dialog
            if (showEditContactDialog) {
                var editName by remember { mutableStateOf(debtor.person.name) }
                var editPhone by remember { mutableStateOf(debtor.person.phoneNumber ?: "") }

                AlertDialog(
                    onDismissRequest = { showEditContactDialog = false },
                    containerColor = ColorSurfaceCard,
                    title = {
                        Text(
                            text = "EDIT CONTACT",
                            style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary)
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Name", style = TextStyle(fontSize = 12.sp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorSurfaceBorderHover,
                                    unfocusedBorderColor = ColorSurfaceBorder,
                                    focusedContainerColor = ColorSurfaceCardElevated,
                                    unfocusedContainerColor = ColorSurfaceCardElevated,
                                    focusedTextColor = ColorTextPrimary,
                                    unfocusedTextColor = ColorTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone Number", style = TextStyle(fontSize = 12.sp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorSurfaceBorderHover,
                                    unfocusedBorderColor = ColorSurfaceBorder,
                                    focusedContainerColor = ColorSurfaceCardElevated,
                                    unfocusedContainerColor = ColorSurfaceCardElevated,
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
                                if (editName.isNotBlank()) {
                                    viewModel.updateContact(editName, editPhone)
                                    showEditContactDialog = false
                                }
                            }
                        ) {
                            Text("SAVE", style = MicroCapsStyle.copy(color = ColorTextPrimary))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditContactDialog = false }) {
                            Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                        }
                    }
                )
            }

            // Customizable Friendly Reminder Composer Sheet
            if (showReminderSheet) {
                com.ledger.iou.ui.reminder.ReminderComposerSheet(
                    debtor = debtor,
                    onDismiss = { showReminderSheet = false }
                )
            }
        }
    }
}

@Composable
private fun TimelineEntryCard(
    item: TransactionWithRunningBalance,
    isPrivacyMasked: Boolean,
    onDeleteClick: () -> Unit
) {
    val tx = item.transaction
    val isLent = tx.type == TransactionType.LENT

    HairlineCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = ColorSurfaceCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Type Icon + Date/Note/Due Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isLent) ColorAccentNeutralMuted else ColorAccentPositiveMuted,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isLent) ColorSurfaceBorder else ColorAccentPositive.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isLent) ColorAccentNeutral else ColorAccentPositive,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLent) "LENT" else "RECEIVED",
                            style = MicroCapsStyle.copy(
                                fontSize = 11.sp,
                                color = if (isLent) ColorAccentNeutral else ColorAccentPositive
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${LedgerRepository.formatDate(tx.timestampEpoch)}",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = ColorTextSecondary
                            )
                        )
                    }

                    tx.note?.let { note ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = note,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = ColorTextPrimary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    tx.dueDateEpoch?.let { due ->
                        Spacer(modifier = Modifier.height(2.dp))
                        val dueText = LedgerRepository.formatRelativeDue(due)
                        val isOverdue = due < System.currentTimeMillis()
                        Text(
                            text = "Due: ${LedgerRepository.formatDate(due)} ($dueText)",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.sp,
                                color = if (isOverdue) ColorAccentNegative else ColorTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Amount + Running Balance + Delete button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val amountSign = if (isLent) "+ " else "- "
                    val formatted = LedgerRepository.formatCents(tx.amount, mask = isPrivacyMasked)
                    Text(
                        text = if (isPrivacyMasked) formatted else "$amountSign$formatted",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                            color = if (isLent) ColorTextPrimary else ColorAccentPositive
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bal: ${LedgerRepository.formatCents(item.runningBalanceCents, mask = isPrivacyMasked)}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                            color = ColorTextSecondary
                        )
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete entry",
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = ColorTextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ColorSurfaceCard)
            .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MicroCapsStyle.copy(
                    fontSize = 9.sp,
                    color = accentColor
                ),
                maxLines = 1
            )
        }
    }
}
