// data/Expense.kt
package com.example.expencetracker.data

data class Expense(
    var documentId: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val category: String? = null,
    val date: Long = 0L
) {
    constructor() : this("", 0.0, "", "", 0L)
}

