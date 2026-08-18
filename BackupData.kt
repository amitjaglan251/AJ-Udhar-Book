package com.aj.udharbook.backup

import com.aj.udharbook.model.Customer
import com.aj.udharbook.model.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val customers: List<Customer>,
    val transactions: List<Transaction>
)