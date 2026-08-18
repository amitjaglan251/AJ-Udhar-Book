package com.aj.udharbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aj.udharbook.model.Customer
import com.aj.udharbook.repository.CustomerRepository
import com.aj.udharbook.sync.FirestoreSyncManager
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val repository: CustomerRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModel() {

    // ==================================================
    // ALL CUSTOMERS
    // ==================================================

    val allCustomers = repository.allCustomers


    // ==================================================
    // INSERT CUSTOMER + FIRESTORE SYNC
    // ==================================================

    fun insert(customer: Customer) =
        viewModelScope.launch {

            val generatedId =
                repository.insert(customer)

            val savedCustomer =
                customer.copy(
                    id = generatedId.toInt()
                )

            try {
                firestoreSyncManager.syncCustomer(
                    savedCustomer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    // ==================================================
    // UPDATE CUSTOMER + FIRESTORE SYNC
    // ==================================================

    fun update(customer: Customer) =
        viewModelScope.launch {

            repository.update(customer)

            try {
                firestoreSyncManager.syncCustomer(customer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    // ==================================================
    // DELETE CUSTOMER + FIRESTORE SYNC
    // ==================================================

    fun delete(customer: Customer) =
        viewModelScope.launch {

            repository.delete(customer)

            try {
                firestoreSyncManager.deleteCustomer(customer.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    // ==================================================
    // GET CUSTOMER BY ID
    // ==================================================

    fun getCustomerById(
        id: Int
    ) =
        repository.getCustomerById(id)
}


// ======================================================
// VIEWMODEL FACTORY
// ======================================================

class CustomerViewModelFactory(
    private val repository: CustomerRepository,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                CustomerViewModel::class.java
            )
        ) {

            return CustomerViewModel(
                repository,
                firestoreSyncManager
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}