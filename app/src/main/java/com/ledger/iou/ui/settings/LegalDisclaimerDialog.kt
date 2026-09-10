package com.ledger.iou.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.iou.ui.components.HairlineDivider
import com.ledger.iou.ui.theme.*

@Composable
fun UnifiedLegalAgreementDialog(
    isOnboarding: Boolean = false,
    onUnderstoodOrAgreed: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val fullLegalAgreementText = """
        1. NOT A LENDING INSTITUTION OR BANK
        Ledger is strictly an offline personal notebook and digital calculator utility designed exclusively for informal record-keeping of personal loans and borrowings between consenting individuals.
        
        Ledger is NOT a bank, quasi-bank, financing company, credit agency, pawnshop, or licensed lending company under Philippine Republic Act No. 9474 (Lending Company Regulation Act of 2007) or Bangko Sentral ng Pilipinas (BSP) / Securities and Exchange Commission (SEC) regulations.
        
        2. NO FINANCIAL TRANSACTIONS OR MONEY HANDLING
        Ledger does not originate, disburse, collect, transfer, or process money. It does not solicit deposits, issue credit, or perform credit scoring. All records are purely reflective of manual entries typed by the device owner.
        
        3. NO LIABILITY FOR DISPUTES & "AS-IS" WARRANTY
        The developer and publisher of Ledger assume NO responsibility or legal liability for disputes, unpaid debts, misunderstandings, inaccuracies in user entry, lost records, or broken agreements between borrowers and lenders. Ledger is provided "as-is" without warranties of any kind.
        
        4. 100% OFFLINE PRIVACY GUARANTEE
        Ledger operates completely offline. No personal data, contacts, debt amounts, timestamps, or usage analytics are ever sent to remote servers or third parties. All records reside exclusively within an isolated SQLite database on your device.
        
        5. USER RESPONSIBILITY & DATA BACKUP
        You are solely responsible for the accuracy of names and amounts you record and for regularly creating backups using the CSV export feature.
    """.trimIndent()

    AlertDialog(
        onDismissRequest = {
            if (!isOnboarding && onDismiss != null) {
                onDismiss()
            }
        },
        containerColor = ColorSurfaceCard,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = ColorAccentNeutral,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isOnboarding) "TERMS & DISCLAIMER" else "LEGAL & COMPLIANCE AGREEMENT",
                        style = MicroCapsStyle.copy(fontSize = 11.sp, color = ColorAccentNeutral)
                    )
                }
                if (!isOnboarding && onDismiss != null) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ColorTextSecondary)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isOnboarding) {
                    Text(
                        text = "Before using Ledger, please confirm that you understand this is an offline personal tracking tool only.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                HairlineDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = fullLegalAgreementText,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = ColorTextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUnderstoodOrAgreed,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAccentPositive,
                    contentColor = ColorBackground
                )
            ) {
                Text(
                    text = if (isOnboarding) "I UNDERSTAND & AGREE" else "UNDERSTOOD",
                    style = MicroCapsStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorBackground
                    )
                )
            }
        }
    )
}
