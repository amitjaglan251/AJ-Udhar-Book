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

    fun insert(
        transaction: Transaction,
        onCompleted: (Double) -> Unit = {}
    ) = viewModelScope.launch {
        val customer = firestoreSyncManager.runCatchingCustomer(transaction.customerId)
        val syncKey = if (customer?.sharedLedgerId?.matches(Regex("\\d{6}")) == true) {
            UUID.randomUUID().toString()
        } else ""
        val prepared = transaction.copy(syncKey = if (transaction.syncKey.isBlank()) syncKey else transaction.syncKey)
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

    fun getTransactionsByCustomer(customerId: Int) =
        repository.getTransactionsByCustomer(customerId)
}

private suspend fun FirestoreSyncManager.runCatchingCustomer(customerId: Int) =
    try {
        val field = FirestoreSyncManager::class.java.getDeclaredField("customerDao")
        field.isAccessible = true
        val dao = field.get(this) as com.aj.udharbook.dao.CustomerDao
        dao.getCustomerByIdOnce(customerId)
    } catch (_: Exception) {
        null
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
