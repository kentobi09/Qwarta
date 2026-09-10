package com.ledger.iou.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledger.iou.data.model.LoanTransactionEntity
import com.ledger.iou.data.model.PersonWithTransactions
import com.ledger.iou.data.preferences.UserPreferencesRepository
import com.ledger.iou.data.repository.LedgerRepository
import com.ledger.iou.data.repository.TransactionWithRunningBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PersonDetailUiState {
    object Loading : PersonDetailUiState
    object NotFound : PersonDetailUiState
    data class Success(
        val debtor: PersonWithTransactions,
        val timeline: List<TransactionWithRunningBalance>,
        val isPrivacyMasked: Boolean,
        val isBiometricEnabled: Boolean
    ) : PersonDetailUiState
}

class PersonDetailViewModel(
    private val personId: String,
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<PersonDetailUiState> = combine(
        repository.getPersonWithTransactions(personId),
        repository.getTransactionsWithRunningBalance(personId),
        preferencesRepository.isPrivacyMaskEnabled,
        preferencesRepository.isBiometricEnabled
    ) { personWithTx, timeline, isMasked, isBiometricEnabled ->
        if (personWithTx == null) {
            PersonDetailUiState.NotFound
        } else {
            PersonDetailUiState.Success(
                debtor = personWithTx,
                timeline = timeline,
                isPrivacyMasked = isMasked,
                isBiometricEnabled = isBiometricEnabled
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonDetailUiState.Loading
    )

    fun settleAll(currentBalanceCents: Long) {
        viewModelScope.launch {
            repository.settleAll(personId, currentBalanceCents)
        }
    }

    fun deleteTransaction(transaction: LoanTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deletePerson(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePerson(personId)
            onDeleted()
        }
    }

    fun updateContact(name: String, phoneNumber: String?) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is PersonDetailUiState.Success) {
                val updatedPerson = currentState.debtor.person.copy(
                    name = name.trim(),
                    phoneNumber = phoneNumber?.trim()?.ifBlank { null }
                )
                repository.updatePerson(updatedPerson)
            }
        }
    }

    fun togglePrivacyMask() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is PersonDetailUiState.Success) {
                preferencesRepository.setPrivacyMaskEnabled(!currentState.isPrivacyMasked)
            }
        }
    }
}

class PersonDetailViewModelFactory(
    private val personId: String,
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonDetailViewModel(personId, repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
