package com.aj.udharbook.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Transaction(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val customerId: Int,

    val amount: Double,

    val type: String,

    val note: String = "",

    val timestamp: Long = System.currentTimeMillis()
)