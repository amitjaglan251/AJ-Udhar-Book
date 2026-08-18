package com.aj.udharbook.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aj.udharbook.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // ==================================================
    // ADD CUSTOMER
    // ==================================================

    @Insert
    suspend fun insert(customer: Customer): Long
    @Insert
    suspend fun insertAll(
        customers: List<Customer>
    )


    // ==================================================
    // UPDATE CUSTOMER
    // ==================================================

    @Update
    suspend fun update(customer: Customer)


    // ==================================================
    // DELETE CUSTOMER
    // ==================================================

    @Delete
    suspend fun delete(customer: Customer)


    // ==================================================
    // GET ALL CUSTOMERS
    // ==================================================

    @Query("SELECT * FROM customers ORDER BY id DESC")
    fun getAllCustomers(): Flow<List<Customer>>


    // ==================================================
    // GET CUSTOMER BY ID
    // ==================================================

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    fun getCustomerById(
        customerId: Int
    ): Flow<Customer?>


    // ==================================================
    // ADDITIONAL METHODS FOR BACKUP
    // ==================================================

    @Query("SELECT * FROM customers ORDER BY id DESC")
    suspend fun getAllCustomersOnce(): List<Customer>

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}