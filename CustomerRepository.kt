package com.aj.udharbook.repository

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.model.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao
) {

    // ==================================================
    // LIVE CUSTOMER LIST
    // ==================================================

    val allCustomers = customerDao.getAllCustomers()


    // ==================================================
    // INSERT CUSTOMER
    // ==================================================

    suspend fun insert(
        customer: Customer
    ): Long {
        return customerDao.insert(customer)
    }


    // ==================================================
    // UPDATE CUSTOMER
    // ==================================================

    suspend fun update(
        customer: Customer
    ) {
        customerDao.update(customer)
    }


    // ==================================================
    // DELETE CUSTOMER
    // ==================================================

    suspend fun delete(
        customer: Customer
    ) {
        customerDao.delete(customer)
    }


    // ==================================================
    // GET CUSTOMER BY ID
    // ==================================================

    fun getCustomerById(
        id: Int
    ): Flow<Customer?> {
        return customerDao.getCustomerById(id)
    }


    // ==================================================
    // GET ALL CUSTOMERS ONCE
    // FOR FIRESTORE INITIAL SYNC
    // ==================================================

    suspend fun getAllCustomersOnce(): List<Customer> {
        return customerDao.getAllCustomersOnce()
    }
}