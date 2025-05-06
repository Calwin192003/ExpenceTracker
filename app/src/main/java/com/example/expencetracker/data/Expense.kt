// data/Expense.kt
package com.example.expencetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val note: String,
    val category: String?,
    val date: Long
)
