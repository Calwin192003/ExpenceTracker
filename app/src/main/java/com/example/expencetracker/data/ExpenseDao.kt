// data/ExpenseDao.kt
package com.example.expencetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getExpensesBetween(start: Long, end: Long): LiveData<List<Expense>>

}
