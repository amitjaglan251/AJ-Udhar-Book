package com.aj.udharbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aj.udharbook.model.Transaction
import com.aj.udharbook.repository.TransactionRepository
import com.aj.udharbook.sync.FirestoreSyncManager
import kotlinx.coroutines.launch
import java.util.UUID

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModel() {

    val allTransactions = repository.allTransactions

    fun insert(transaction: Transaction, onCompleted: (Double) -> Unit = {}) = viewModelScope.launch {
        // Every new transaction gets a stable cloud identity. This is required so
        // the same transaction can be mirrored on both phones without relying on
        // Room's device-local integer primary key.
        val prepared = if (transaction.syncKey.isBlank()) {
            transaction.copy(syncKey = UUID.randomUUID().toString())
        } else transaction

        val generatedId = repository.insert(prepared)
        val savedTransaction = prepared.copy(id = generatedId.toInt())

        try {
            firestoreSyncManager.syncTransaction(savedTransaction)
            firestoreSyncManager.syncSharedLedgerTransaction(savedTransaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val newBalance = try {
            repository.getCustomerBalance(transaction.customerId)
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
        onCompleted(newBalance)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
        try {
            firestoreSyncManager.syncTransaction(transaction)
            firestoreSyncManager.syncSharedLedgerTransaction(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
        try {
            firestoreSyncManager.deleteTransaction(transaction.id)
            firestoreSyncManager.deleteSharedLedgerTransaction(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTransactionsByCustomer(customerId: Int) = repository.getTransactionsByCustomer(customerId)
}

class TransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(repository, firestoreSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
