// data/Expense.kt
package com.example.expencetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val amount: Double = 0.0,
    val note: String = "",
    val category: String? = "",
    val date: Long = 0L
) {
    constructor() : this(null, 0.0, "", "", 0L)
}

