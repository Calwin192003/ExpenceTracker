// MainActivity.kt
package com.example.expencetracker

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expencetracker.data.Expense
import com.example.expencetracker.data.ExpenseDao
import com.example.expencetracker.data.ExpenseDatabase
import com.example.expencetracker.databinding.ActivityMainBinding
import com.example.expencetracker.databinding.DialogAddExpenseBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseDao: ExpenseDao
    private val expenses = mutableListOf<Expense>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        // Set up RecyclerView
        val adapter = ExpenseAdapter(expenses)
        binding.expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expenseRecyclerView.adapter = adapter

        // Add Expense Button
        binding.addExpenseButton.setOnClickListener { showAddExpenseDialog() }
        observeAllExpenses(adapter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val amount = dialogBinding.editTextAmount.text.toString().toDoubleOrNull()
                val note = dialogBinding.editTextNote.text.toString()
                val selectedCategoryId = dialogBinding.radioGroupCategory.checkedRadioButtonId
                val selectedCategory = dialogBinding.root.findViewById<RadioButton>(selectedCategoryId)?.text?.toString()

                val day = dialogBinding.datePicker.dayOfMonth
                val month = dialogBinding.datePicker.month
                val year = dialogBinding.datePicker.year
                val selectedDateMillis = LocalDate.of(year, month + 1, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (amount != null && selectedCategory != null) {
                    val expense = Expense(
                        amount = amount,
                        note = note,
                        category = selectedCategory,
                        date = selectedDateMillis
                    )
                    lifecycleScope.launch {
                        try {
                            expenseDao.insert(expense)
                            expenses.add(expense)
                            Toast.makeText(this@MainActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Please enter valid data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAllExpenses() {
        expenseDao.getAllExpenses().observe(this) { allExpenses ->
            expenses.clear()
            expenses.addAll(allExpenses)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAllExpenses(adapter: ExpenseAdapter) {
        expenseDao.getAllExpenses().observe(this) { allExpenses ->
            expenses.clear()
            expenses.addAll(allExpenses)
            adapter.notifyDataSetChanged()
        }
    }
}