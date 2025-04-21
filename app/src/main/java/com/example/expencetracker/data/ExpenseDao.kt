// data/ExpenseDao.kt
package com.example.expencetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}
