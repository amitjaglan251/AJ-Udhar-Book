package com.aj.udharbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aj.udharbook.model.Transaction
import com.aj.udharbook.repository.TransactionRepository
import com.aj.udharbook.sync.FirestoreSyncManager
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModel() {

    // ==================================================
    // ALL TRANSACTIONS
    // ==================================================

    val allTransactions =
        repository.allTransactions


    // ==================================================
    // INSERT TRANSACTION + FIRESTORE SYNC
    // ==================================================

    fun insert(
        transaction: Transaction,
        onCompleted: (Double) -> Unit = {}
    ) = viewModelScope.launch {

        val generatedId =
            repository.insert(transaction)

        val savedTransaction =
            transaction.copy(
                id = generatedId.toInt()
            )

        // ==================================================
        // FIRESTORE SYNC
        // ==================================================

        try {

            firestoreSyncManager.syncTransaction(
                savedTransaction
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }

        // ==================================================
        // GET NEW BALANCE
        // ==================================================

        val newBalance =
            try {

                repository.getCustomerBalance(
                    transaction.customerId
                )

            } catch (e: Exception) {

                e.printStackTrace()

                0.0
            }

        // ==================================================
        // CALLBACK
        // ==================================================

        onCompleted(newBalance)
    }


    // ==================================================
    // UPDATE TRANSACTION + FIRESTORE SYNC
    // ==================================================

    fun update(
        transaction: Transaction
    ) =
        viewModelScope.launch {

            repository.update(transaction)

            try {

                firestoreSyncManager.syncTransaction(
                    transaction
                )

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }


    // ==================================================
    // DELETE TRANSACTION + FIRESTORE SYNC
    // ==================================================

    fun delete(
        transaction: Transaction
    ) =
        viewModelScope.launch {

            repository.delete(transaction)

            try {

                firestoreSyncManager.deleteTransaction(
                    transaction.id
                )

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }


    // ==================================================
    // LIVE TRANSACTIONS OF ONE CUSTOMER
    // ==================================================

    fun getTransactionsByCustomer(
        customerId: Int
    ) =
        repository.getTransactionsByCustomer(
            customerId
        )
}


// ======================================================
// VIEWMODEL FACTORY
// ======================================================

class TransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                TransactionViewModel::class.java
            )
        ) {

            return TransactionViewModel(
                repository,
                firestoreSyncManager
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}