package com.ledger.iou.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.preferences.UserPreferencesRepository
import com.ledger.iou.data.repository.LedgerOverviewStats
import com.ledger.iou.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardFilter {
    ACTIVE, OVERDUE, SETTLED
}

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Empty(
        val isPrivacyMasked: Boolean,
        val isBiometricEnabled: Boolean
    ) : DashboardUiState
    data class Success(
        val stats: LedgerOverviewStats,
        val allDebtors: List<PersonWithTransactions>,
        val filteredDebtors: List<PersonWithTransactions>,
        val selectedFilter: DashboardFilter,
        val isPrivacyMasked: Boolean,
        val searchQuery: String,
        val isBiometricEnabled: Boolean
    ) : DashboardUiState
}

class DashboardViewModel(
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DashboardFilter.ACTIVE)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allPersonsWithTransactions,
        preferencesRepository.isPrivacyMaskEnabled,
        preferencesRepository.isBiometricEnabled,
        _selectedFilter,
        _searchQuery
    ) { debtors, isMasked, isBiometricEnabled, filter, query ->
        if (debtors.isEmpty()) {
            DashboardUiState.Empty(
                isPrivacyMasked = isMasked,
                isBiometricEnabled = isBiometricEnabled
            )
        } else {
            val now = System.currentTimeMillis()
            val totalOutstanding = debtors.sumOf { it.balanceCents }
            val activeDebtors = debtors.filter { !it.isSettled }
            val overdueDebtors = debtors.filter { it.isOverdue(now) }
            val settledDebtors = debtors.filter { it.isSettled }

            val stats = LedgerOverviewStats(
                totalOutstandingCents = totalOutstanding,
                activeDebtorsCount = activeDebtors.size,
                overdueDebtorsCount = overdueDebtors.size,
                settledCount = settledDebtors.size
            )

            val baseFiltered = when (filter) {
                DashboardFilter.ACTIVE -> activeDebtors
                DashboardFilter.OVERDUE -> overdueDebtors
                DashboardFilter.SETTLED -> settledDebtors
            }

            val searchFiltered = if (query.isBlank()) {
                baseFiltered
            } else {
                baseFiltered.filter {
                    it.person.name.contains(query, ignoreCase = true) ||
                    (it.person.phoneNumber?.contains(query) == true)
                }
            }

            DashboardUiState.Success(
                stats = stats,
                allDebtors = debtors,
                filteredDebtors = searchFiltered,
                selectedFilter = filter,
                isPrivacyMasked = isMasked,
                searchQuery = query,
                isBiometricEnabled = isBiometricEnabled
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun setFilter(filter: DashboardFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePrivacyMask() {
        viewModelScope.launch {
            val currentState = uiState.value
            val isCurrentMasked = when (currentState) {
                is DashboardUiState.Success -> currentState.isPrivacyMasked
                is DashboardUiState.Empty -> currentState.isPrivacyMasked
                else -> false
            }
            preferencesRepository.setPrivacyMaskEnabled(!isCurrentMasked)
        }
    }

    fun toggleBiometricSetting() {
        viewModelScope.launch {
            val currentState = uiState.value
            val isCurrentEnabled = when (currentState) {
                is DashboardUiState.Success -> currentState.isBiometricEnabled
                is DashboardUiState.Empty -> currentState.isBiometricEnabled
                else -> false
            }
            preferencesRepository.setBiometricEnabled(!isCurrentEnabled)
        }
    }

    fun quickSettle(personId: String, currentBalanceCents: Long) {
        viewModelScope.launch {
            repository.settleAll(personId, currentBalanceCents)
        }
    }

    fun deletePerson(personId: String) {
        viewModelScope.launch {
            repository.deletePerson(personId)
        }
    }
}

class DashboardViewModelFactory(
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
