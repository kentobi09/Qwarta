package com.ledger.iou.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.model.TransactionType
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.ui.auth.BiometricAuthHelper
import com.ledger.iou.ui.auth.BiometricStatus
import com.ledger.iou.ui.components.AmountText
import com.ledger.iou.ui.components.DebtorCard
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.components.MicroCapsLabel
import com.ledger.iou.ui.components.SegmentedFilterBar
import com.ledger.iou.ui.theme.ColorAccentNegative
import com.ledger.iou.ui.theme.ColorAccentNeutral
import com.ledger.iou.ui.theme.ColorAccentPositive
import com.ledger.iou.ui.theme.ColorBackground
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.ColorTextTertiary
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.theme.TABULAR_NUMERALS_SETTINGS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPersonDetail: (String) -> Unit,
    onOpenAddTransaction: (String?, String) -> Unit, // (personId, transactionType)
    onLockAppNow: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var selectedDebtorForSheet by remember { mutableStateOf<PersonWithTransactions?>(null) }
    var debtorToDelete by remember { mutableStateOf<PersonWithTransactions?>(null) }
    var debtorToSettle by remember { mutableStateOf<PersonWithTransactions?>(null) }
    var debtorForReminder by remember { mutableStateOf<PersonWithTransactions?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackground,
        topBar = {
            DashboardTopBar(
                isPrivacyMasked = when (val s = uiState) {
                    is DashboardUiState.Success -> s.isPrivacyMasked
                    is DashboardUiState.Empty -> s.isPrivacyMasked
                    else -> false
                },
                onTogglePrivacy = { viewModel.togglePrivacyMask() },
                onOpenSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenAddTransaction(null, TransactionType.LENT) },
                containerColor = ColorSurfaceCardElevated,
                contentColor = ColorTextPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .border(1.dp, ColorSurfaceBorderHover, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Record Transaction",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECORD",
                        style = MicroCapsStyle.copy(color = ColorTextPrimary)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorTextPrimary, strokeWidth = 2.dp)
                    }
                }
                is DashboardUiState.Empty -> {
                    EmptyDashboardView(onAddTransaction = { onOpenAddTransaction(null, TransactionType.LENT) })
                }
                is DashboardUiState.Success -> {
                    SuccessDashboardContent(
                        state = state,
                        onFilterChanged = { viewModel.setFilter(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onDebtorClick = { onNavigateToPersonDetail(it.person.id) },
                        onDebtorLongClick = { selectedDebtorForSheet = it }
                    )
                }
            }
        }
    }

    // Fast Action Bottom Sheet
    selectedDebtorForSheet?.let { debtor ->
        FastActionBottomSheet(
            debtor = debtor,
            isPrivacyMasked = when (val s = uiState) {
                is DashboardUiState.Success -> s.isPrivacyMasked
                else -> false
            },
            onDismiss = { selectedDebtorForSheet = null },
            onQuickPaymentReceived = {
                selectedDebtorForSheet = null
                onOpenAddTransaction(debtor.person.id, TransactionType.REPAYMENT)
            },
            onQuickLendMore = {
                selectedDebtorForSheet = null
                onOpenAddTransaction(debtor.person.id, TransactionType.LENT)
            },
            onSettleAll = {
                debtorToSettle = debtor
                selectedDebtorForSheet = null
            },
            onSendReminder = {
                debtorForReminder = debtor
                selectedDebtorForSheet = null
            },
            onDelete = {
                debtorToDelete = debtor
                selectedDebtorForSheet = null
            }
        )
    }

    // Reminder Composer Sheet
    debtorForReminder?.let { debtor ->
        com.ledger.iou.ui.reminder.ReminderComposerSheet(
            debtor = debtor,
            onDismiss = { debtorForReminder = null }
        )
    }

    // Settle All Confirmation Dialog
    debtorToSettle?.let { debtor ->
        AlertDialog(
            onDismissRequest = { debtorToSettle = null },
            containerColor = ColorSurfaceCard,
            title = {
                Text(
                    text = "SETTLE FULL BALANCE",
                    style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorAccentPositive)
                )
            },
            text = {
                Text(
                    text = "Record a full settlement payment of ${LedgerRepository.formatCents(debtor.balanceCents)} from ${debtor.person.name}? This will zero out the balance.",
                    style = TextStyle(color = ColorTextSecondary, fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.quickSettle(debtor.person.id, debtor.balanceCents)
                        debtorToSettle = null
                        Toast.makeText(context, "Balance marked as settled", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("SETTLE", style = MicroCapsStyle.copy(color = ColorAccentPositive))
                }
            },
            dismissButton = {
                TextButton(onClick = { debtorToSettle = null }) {
                    Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                }
            }
        )
    }

    // Delete Debtor Confirmation Dialog
    debtorToDelete?.let { debtor ->
        AlertDialog(
            onDismissRequest = { debtorToDelete = null },
            containerColor = ColorSurfaceCard,
            title = {
                Text(
                    text = "DELETE CONTACT & AUDIT TRAIL",
                    style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorAccentNegative)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${debtor.person.name} and all associated records? This cannot be undone.",
                    style = TextStyle(color = ColorTextSecondary, fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePerson(debtor.person.id)
                        debtorToDelete = null
                        Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("DELETE", style = MicroCapsStyle.copy(color = ColorAccentNegative))
                }
            },
            dismissButton = {
                TextButton(onClick = { debtorToDelete = null }) {
                    Text("CANCEL", style = MicroCapsStyle.copy(color = ColorTextSecondary))
                }
            }
        )
    }
}

@Composable
private fun DashboardTopBar(
    isPrivacyMasked: Boolean,
    onTogglePrivacy: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LEDGER",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = ColorTextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                MicroCapsLabel(
                    text = "OFFLINE • ENCRYPTED",
                    textColor = ColorTextSecondary,
                    backgroundColor = ColorSurfaceCardElevated,
                    borderColor = ColorSurfaceBorder
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Privacy balance mask toggle
                IconButton(
                    onClick = onTogglePrivacy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPrivacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPrivacyMasked) "Show Balances" else "Hide Balances",
                        tint = if (isPrivacyMasked) ColorAccentNegative else ColorTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Settings gear button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HairlineDivider()
    }
}

@Composable
private fun SuccessDashboardContent(
    state: DashboardUiState.Success,
    onFilterChanged: (DashboardFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onDebtorClick: (PersonWithTransactions) -> Unit,
    onDebtorLongClick: (PersonWithTransactions) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Section: Outstanding Header & Metrics
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "TOTAL OUTSTANDING",
                    style = MicroCapsStyle
                )
                Spacer(modifier = Modifier.height(6.dp))

                AmountText(
                    cents = state.stats.totalOutstandingCents,
                    isMasked = state.isPrivacyMasked,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    useMonospace = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown metric pill
                val formattedOwed = LedgerRepository.formatCents(state.stats.totalOutstandingCents, mask = state.isPrivacyMasked)
                val overduePart = if (state.stats.overdueDebtorsCount > 0) " • ${state.stats.overdueDebtorsCount} OVERDUE" else ""
                val breakdownText = "$formattedOwed OWED • ${state.stats.activeDebtorsCount} ACTIVE DEBTORS$overduePart"

                Box(
                    modifier = Modifier
                        .background(ColorSurfaceCard, RoundedCornerShape(6.dp))
                        .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = breakdownText.uppercase(),
                        style = MicroCapsStyle.copy(
                            fontSize = 9.sp,
                            color = if (state.stats.overdueDebtorsCount > 0) ColorAccentNegative else ColorTextSecondary
                        )
                    )
                }
            }
        }

        // Search Input
        item {
            TextField(
                value = state.searchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(
                        "Search debtor or phone...",
                        style = TextStyle(color = ColorTextSecondary, fontSize = 13.sp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ColorSurfaceCard,
                    unfocusedContainerColor = ColorSurfaceCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = ColorTextPrimary,
                    unfocusedTextColor = ColorTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorSurfaceBorder, RoundedCornerShape(8.dp))
            )
        }

        // Segmented Filter Tabs
        item {
            val tabs = listOf(
                "Active" to state.stats.activeDebtorsCount,
                "Overdue" to state.stats.overdueDebtorsCount,
                "Settled" to state.stats.settledCount
            )
            val selectedIndex = when (state.selectedFilter) {
                DashboardFilter.ACTIVE -> 0
                DashboardFilter.OVERDUE -> 1
                DashboardFilter.SETTLED -> 2
            }
            SegmentedFilterBar(
                selectedTab = selectedIndex,
                tabs = tabs,
                onTabSelected = { idx ->
                    val filter = when (idx) {
                        0 -> DashboardFilter.ACTIVE
                        1 -> DashboardFilter.OVERDUE
                        else -> DashboardFilter.SETTLED
                    }
                    onFilterChanged(filter)
                }
            )
        }

        // Debtor List Items
        if (state.filteredDebtors.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records matching this filter.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            color = ColorTextSecondary,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        } else {
            items(
                items = state.filteredDebtors,
                key = { it.person.id }
            ) { debtor ->
                DebtorCard(
                    personWithTx = debtor,
                    isPrivacyMasked = state.isPrivacyMasked,
                    onClick = { onDebtorClick(debtor) },
                    onLongClick = { onDebtorLongClick(debtor) }
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboardView(
    onAddTransaction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ColorSurfaceCard, CircleShape)
                .border(1.dp, ColorSurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = ColorTextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "NO TRANSACTIONS YET",
            style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your offline personal debt and loan ledger is completely empty. Record a loan to begin tracking balances and repayment schedules.",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ColorSurfaceCardElevated)
                .border(1.dp, ColorSurfaceBorderHover, RoundedCornerShape(8.dp))
                .clickable { onAddTransaction() }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "RECORD FIRST LOAN",
                style = MicroCapsStyle.copy(color = ColorTextPrimary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FastActionBottomSheet(
    debtor: PersonWithTransactions,
    isPrivacyMasked: Boolean,
    onDismiss: () -> Unit,
    onQuickPaymentReceived: () -> Unit,
    onQuickLendMore: () -> Unit,
    onSettleAll: () -> Unit,
    onSendReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorSurfaceCard,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = debtor.person.name,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ColorTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Current Balance: ${LedgerRepository.formatCents(debtor.balanceCents, mask = isPrivacyMasked)}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontFeatureSettings = TABULAR_NUMERALS_SETTINGS,
                            color = ColorTextSecondary
                        )
                    )
                }

                MicroCapsLabel(
                    text = if (debtor.isSettled) "SETTLED" else "ACTIVE",
                    textColor = if (debtor.isSettled) ColorAccentPositive else ColorAccentNeutral,
                    backgroundColor = if (debtor.isSettled) ColorAccentPositive.copy(alpha = 0.1f) else ColorSurfaceCardElevated,
                    borderColor = ColorSurfaceBorder
                )
            }

            HairlineDivider()

            // Distinct actions for Log Payment vs Lend More
            if (!debtor.isSettled) {
                FastActionRow(
                    icon = Icons.Default.ArrowDownward,
                    title = "LOG PAYMENT",
                    subtitle = "Record payment received (reduces balance)",
                    onClick = onQuickPaymentReceived,
                    iconTint = ColorAccentPositive
                )
            }

            FastActionRow(
                icon = Icons.Default.ArrowUpward,
                title = if (debtor.isSettled) "LEND AGAIN" else "LEND MORE",
                subtitle = if (debtor.isSettled) "Record a new loan for this person" else "Record additional loan amount given (increases balance)",
                onClick = onQuickLendMore,
                iconTint = ColorTextPrimary
            )

            if (!debtor.isSettled) {
                FastActionRow(
                    icon = Icons.Default.Share,
                    title = "SEND POLITE REMINDER",
                    subtitle = "Draft polite message for SMS or WhatsApp",
                    onClick = onSendReminder
                )

                FastActionRow(
                    icon = Icons.Default.CheckCircle,
                    title = "SETTLE ALL (ZERO BALANCE)",
                    subtitle = "Instantly record full settlement transaction",
                    onClick = onSettleAll,
                    iconTint = ColorAccentPositive
                )
            }

            FastActionRow(
                icon = Icons.Default.DeleteOutline,
                title = "DELETE CONTACT & AUDIT",
                subtitle = "Erase person and all transactions permanently",
                onClick = onDelete,
                iconTint = ColorAccentNegative
            )

        }
    }
}

@Composable
private fun FastActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = ColorTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ColorSurfaceCardElevated, CircleShape)
                .border(1.dp, ColorSurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MicroCapsStyle.copy(fontSize = 11.sp, color = ColorTextPrimary)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = ColorTextSecondary
                )
            )
        }
    }
}

private fun sendReminder(context: Context, debtor: PersonWithTransactions) {
    val reminderText = LedgerRepository.generateReminderMessage(
        personName = debtor.person.name,
        balanceCents = debtor.balanceCents,
        dueDateEpoch = debtor.nextUpcomingDueDate()
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, reminderText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Send Reminder via")
    try {
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ledger Reminder", reminderText))
        Toast.makeText(context, "Reminder copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
