package com.aj.udharbook.repository

import com.aj.udharbook.dao.CustomerDao
import com.aj.udharbook.model.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers = customerDao.getAllCustomers()

    suspend fun insert(customer: Customer): Long = customerDao.insert(customer)
    suspend fun update(customer: Customer) = customerDao.update(customer)
    suspend fun delete(customer: Customer) = customerDao.delete(customer)

    fun getCustomerById(id: Int): Flow<Customer?> = customerDao.getCustomerById(id)
    suspend fun getCustomerByIdOnce(id: Int): Customer? = customerDao.getCustomerByIdOnce(id)
    suspend fun getCustomerByMobileOnce(mobile: String): Customer? = customerDao.getCustomerByMobileOnce(mobile)
    suspend fun getCustomerBySharedLedgerOnce(ledgerId: String): Customer? = customerDao.getCustomerBySharedLedgerOnce(ledgerId)
    suspend fun getAllCustomersOnce(): List<Customer> = customerDao.getAllCustomersOnce()
}
