package com.ledger.iou.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.preferences.UserPreferencesRepository
import com.ledger.iou.data.repository.BackupRepository
import com.ledger.iou.ui.auth.BiometricAuthHelper
import com.ledger.iou.ui.auth.BiometricStatus
import com.ledger.iou.ui.components.HairlineCard
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.components.MicroCapsLabel
import com.ledger.iou.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    allDebtors: List<PersonWithTransactions>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    val isBiometricEnabled by preferencesRepository.isBiometricEnabled.collectAsState(initial = false)
    val isPrivacyMasked by preferencesRepository.isPrivacyMaskEnabled.collectAsState(initial = false)

    var showLegalDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // System Save to Device document launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.bufferedWriter().use { writer ->
                                writer.write(BackupRepository.generateCsvContent(allDebtors))
                            }
                        }
                    }
                    Toast.makeText(context, "CSV file saved to your device", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & PREFERENCES",
                        style = MicroCapsStyle.copy(fontSize = 12.sp, color = ColorTextPrimary)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Security & Privacy
            item {
                SectionHeader(title = "SECURITY & PRIVACY")
                Spacer(modifier = Modifier.height(8.dp))
                HairlineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Biometric / Device App Lock",
                            subtitle = if (isBiometricEnabled) "Enabled — Authenticate on launch" else "Disabled — Unrestricted local access",
                            isChecked = isBiometricEnabled,
                            onCheckedChange = {
                                if (activity != null) {
                                    val status = BiometricAuthHelper.checkBiometricStatus(context)
                                    if (status == BiometricStatus.UNAVAILABLE) {
                                        Toast.makeText(context, "Biometrics not supported on this device", Toast.LENGTH_SHORT).show()
                                    } else if (status == BiometricStatus.NOT_ENROLLED) {
                                        Toast.makeText(context, "No fingerprint/PIN enrolled on device", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (!isBiometricEnabled) {
                                            BiometricAuthHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "Confirm Biometric Lock",
                                                subtitle = "Scan fingerprint to enable app lock",
                                                onSuccess = {
                                                    coroutineScope.launch { preferencesRepository.setBiometricEnabled(true) }
                                                    Toast.makeText(context, "App lock enabled", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Cancelled: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            coroutineScope.launch { preferencesRepository.setBiometricEnabled(false) }
                                            Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        HairlineDivider()

                        SettingsToggleRow(
                            icon = Icons.Default.VisibilityOff,
                            title = "Privacy Balance Masking",
                            subtitle = if (isPrivacyMasked) "Enabled — Values obscured with asterisks" else "Disabled — Amounts visible at all times",
                            isChecked = isPrivacyMasked,
                            onCheckedChange = {
                                coroutineScope.launch { preferencesRepository.setPrivacyMaskEnabled(!isPrivacyMasked) }
                            }
                        )
                    }
                }
            }

            // Section 2: Data & Offline Backup
            item {
                SectionHeader(title = "DATA MANAGEMENT & BACKUP")
                Spacer(modifier = Modifier.height(8.dp))
                HairlineCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Save directly to device storage / downloads
                        SettingsActionRow(
                            icon = Icons.Default.SaveAlt,
                            title = "Save CSV to Device",
                            subtitle = "Save offline spreadsheet directly to your phone's storage",
                            actionLabel = "SAVE",
                            onClick = {
                                if (allDebtors.isEmpty()) {
                                    Toast.makeText(context, "Qwarta is currently empty", Toast.LENGTH_SHORT).show()
                                } else {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    createDocumentLauncher.launch("Qwarta_Backup_$timestamp.csv")
                                }
                            }
                        )

                        HairlineDivider()

                        // 2. Share / Send via other apps
                        SettingsActionRow(
                            icon = Icons.Default.Share,
                            title = "Share CSV Spreadsheet",
                            subtitle = "Send via Drive, email, messaging, or cloud apps",
                            actionLabel = if (isExporting) "EXPORTING..." else "SHARE",
                            onClick = {
                                if (allDebtors.isEmpty()) {
                                    Toast.makeText(context, "Qwarta is currently empty", Toast.LENGTH_SHORT).show()
                                } else {
                                    isExporting = true
                                    coroutineScope.launch {
                                        try {
                                            val csvFile = BackupRepository.exportToCsv(context, allDebtors)
                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                csvFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Qwarta CSV Backup"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Legal, Compliance & Privacy (Single clean row)
            item {
                SectionHeader(title = "LEGAL, COMPLIANCE & PRIVACY")
                Spacer(modifier = Modifier.height(8.dp))
                HairlineCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                        icon = Icons.Default.Gavel,
                        title = "Legal & Compliance Agreement",
                        subtitle = "Non-lending disclaimer • Offline privacy • Terms of use",
                        onClick = { showLegalDialog = true }
                    )
                }
            }

            // Section 4: App Information
            item {
                SectionHeader(title = "APPLICATION")
                Spacer(modifier = Modifier.height(8.dp))
                HairlineCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QWARTA / IOU TRACKER",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version 1.0.0 • Offline SQLite Engine",
                                style = TextStyle(fontSize = 11.sp, color = ColorTextTertiary)
                            )
                        }

                        MicroCapsLabel(
                            text = "OFFLINE",
                            textColor = ColorAccentPositive,
                            backgroundColor = ColorAccentPositive.copy(alpha = 0.1f),
                            borderColor = ColorSurfaceBorder
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Legal & Compliance Modal
    if (showLegalDialog) {
        UnifiedLegalAgreementDialog(
            isOnboarding = false,
            onUnderstoodOrAgreed = { showLegalDialog = false },
            onDismiss = { showLegalDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MicroCapsStyle.copy(fontSize = 10.sp, color = ColorTextSecondary)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ColorTextPrimary, modifier = Modifier.size(22.dp))
            Column {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorTextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary)
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorBackground,
                checkedTrackColor = ColorAccentPositive,
                uncheckedThumbColor = ColorTextTertiary,
                uncheckedTrackColor = ColorSurfaceCardElevated
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ColorTextPrimary, modifier = Modifier.size(22.dp))
            Column {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorTextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary)
                )
            }
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ColorSurfaceCardElevated,
                contentColor = ColorTextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorSurfaceBorder),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = actionLabel, style = MicroCapsStyle.copy(fontSize = 10.sp, color = ColorTextPrimary))
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(22.dp))
            Column {
                Text(
                    text = title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorTextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(fontSize = 11.sp, color = ColorTextSecondary)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ColorTextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
