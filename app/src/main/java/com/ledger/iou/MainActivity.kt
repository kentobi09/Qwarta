package com.ledger.iou

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ledger.iou.data.model.TransactionType
import com.ledger.iou.ui.auth.BiometricAuthHelper
import com.ledger.iou.ui.dashboard.DashboardScreen
import com.ledger.iou.ui.dashboard.DashboardViewModel
import com.ledger.iou.ui.dashboard.DashboardViewModelFactory
import com.ledger.iou.ui.detail.PersonDetailScreen
import com.ledger.iou.ui.detail.PersonDetailViewModel
import com.ledger.iou.ui.detail.PersonDetailViewModelFactory
import com.ledger.iou.ui.theme.ColorBackground
import com.ledger.iou.ui.theme.ColorSurfaceBorder
import com.ledger.iou.ui.theme.ColorSurfaceBorderHover
import com.ledger.iou.ui.theme.ColorSurfaceCard
import com.ledger.iou.ui.theme.ColorSurfaceCardElevated
import com.ledger.iou.ui.theme.ColorTextPrimary
import com.ledger.iou.ui.theme.ColorTextSecondary
import com.ledger.iou.ui.theme.LedgerTheme
import com.ledger.iou.ui.theme.MicroCapsStyle
import com.ledger.iou.ui.transaction.AddTransactionSheet
import com.ledger.iou.worker.ReminderNotificationHelper

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LedgerApplication
        val repository = app.repository
        val preferencesRepository = app.preferencesRepository

        val deepLinkedPersonId = intent?.getStringExtra(ReminderNotificationHelper.EXTRA_PERSON_ID)

        setContent {
            LedgerTheme {
                val biometricPreference by preferencesRepository.isBiometricEnabled.collectAsState(initial = null)

                if (biometricPreference == null) {
                    // Loading preferences
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorTextPrimary, strokeWidth = 2.dp)
                    }
                } else {
                    val isBiometricConfigured = biometricPreference == true
                    var isUnlocked by remember { mutableStateOf(!isBiometricConfigured) }

                    // Trigger biometric prompt on cold start if locked
                    LaunchedEffect(isBiometricConfigured) {
                        if (isBiometricConfigured && !isUnlocked) {
                            if (BiometricAuthHelper.isBiometricAvailable(this@MainActivity)) {
                                BiometricAuthHelper.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                    onError = { /* Keep overlay visible with retry button */ }
                                )
                            } else {
                                // No hardware or enrollment, allow entry
                                isUnlocked = true
                            }
                        }
                    }

                    if (isBiometricConfigured && !isUnlocked) {
                        BiometricLockOverlay(
                            onTapUnlock = {
                                BiometricAuthHelper.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                    onError = { /* Handle error */ }
                                )
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        var showAddSheet by remember { mutableStateOf(false) }
                        var addSheetPreselectedPersonId by remember { mutableStateOf<String?>(null) }
                        var addSheetInitialType by remember { mutableStateOf(TransactionType.LENT) }

                        // Check for deep link on first launch
                        LaunchedEffect(deepLinkedPersonId) {
                            if (!deepLinkedPersonId.isNullOrBlank()) {
                                navController.navigate("detail/$deepLinkedPersonId")
                            }
                        }

                        val isTermsAccepted by preferencesRepository.isTermsAccepted.collectAsState(initial = true)
                        val scope = androidx.compose.runtime.rememberCoroutineScope()

                        if (!isTermsAccepted) {
                            com.ledger.iou.ui.settings.UnifiedLegalAgreementDialog(
                                isOnboarding = true,
                                onUnderstoodOrAgreed = {
                                    scope.launch {
                                        preferencesRepository.setTermsAccepted(true)
                                    }
                                }
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "dashboard"
                        ) {
                            composable("dashboard") {
                                val dashboardViewModel: DashboardViewModel = viewModel(
                                    factory = DashboardViewModelFactory(repository, preferencesRepository)
                                )

                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToPersonDetail = { personId ->
                                        navController.navigate("detail/$personId")
                                    },
                                    onOpenAddTransaction = { personId, transactionType ->
                                        addSheetPreselectedPersonId = personId
                                        addSheetInitialType = transactionType
                                        showAddSheet = true
                                    },
                                    onLockAppNow = {
                                        isUnlocked = false
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }

                            composable("settings") {
                                val allDebtors by repository.allPersonsWithTransactions.collectAsState(initial = emptyList())
                                com.ledger.iou.ui.settings.SettingsScreen(
                                    preferencesRepository = preferencesRepository,
                                    allDebtors = allDebtors,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "detail/{personId}",
                                arguments = listOf(navArgument("personId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val personId = backStackEntry.arguments?.getString("personId") ?: ""
                                val detailViewModel: PersonDetailViewModel = viewModel(
                                    factory = PersonDetailViewModelFactory(personId, repository, preferencesRepository)
                                )

                                PersonDetailScreen(
                                    viewModel = detailViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onOpenAddTransaction = { targetPersonId, transactionType ->
                                        addSheetPreselectedPersonId = targetPersonId
                                        addSheetInitialType = transactionType
                                        showAddSheet = true
                                    }
                                )
                            }
                        }

                        if (showAddSheet) {
                            AddTransactionSheet(
                                repository = repository,
                                preselectedPersonId = addSheetPreselectedPersonId,
                                initialTransactionType = addSheetInitialType,
                                onDismiss = {
                                    showAddSheet = false
                                    addSheetPreselectedPersonId = null
                                },
                                onTransactionSaved = {
                                    showAddSheet = false
                                    addSheetPreselectedPersonId = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun BiometricLockOverlay(
    onTapUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .clickable { onTapUnlock() }
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ColorSurfaceCard, CircleShape)
                    .border(1.dp, ColorSurfaceBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock",
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LEDGER LOCKED",
                style = MicroCapsStyle.copy(fontSize = 13.sp, color = ColorTextPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authentication required to access personal debt and loan records.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
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
                    .clickable { onTapUnlock() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "UNLOCK WITH BIOMETRICS",
                    style = MicroCapsStyle.copy(color = ColorTextPrimary)
                )
            }
        }
    }
}
