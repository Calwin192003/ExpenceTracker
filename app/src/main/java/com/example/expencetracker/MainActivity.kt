// MainActivity.kt
package com.example.expencetracker

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expencetracker.data.Expense
import com.example.expencetracker.data.ExpenseDao
import com.example.expencetracker.data.ExpenseDatabase
import com.example.expencetracker.databinding.ActivityMainBinding
import com.example.expencetracker.databinding.DialogAddExpenseBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.MonthDayBinder
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

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

        val adapter = ExpenseAdapter(expenses)
        binding.expenseRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.expenseRecyclerView.adapter = adapter

        binding.addExpenseButton.setOnClickListener { showAddExpenseDialog() }
        observeAllExpenses(adapter)

        binding.btnPreviousMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        binding.btnNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.scrollToMonth(it.yearMonth.plusMonths(1))
            }
        }

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
    private fun observeAllExpenses(adapter: ExpenseAdapter) {
        expenseDao.getAllExpenses().observe(this) { allExpenses ->
            expenses.clear()
            expenses.addAll(allExpenses)
            setupCalendar(expenses)
            adapter.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCalendar(expenseList: List<Expense>) {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek()

        binding.calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY)
        binding.calendarView.scrollToMonth(currentMonth)
        binding.tvMonthTitle.text = formatYearMonth(currentMonth)

        binding.calendarView.monthScrollListener = { month ->
            binding.tvMonthTitle.text = formatYearMonth(month.yearMonth)
        }

        val expensesByDate = expenseList.groupBy {
            Instant.ofEpochMilli(it.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val date = day.date
                container.textViewDate.text = date.dayOfMonth.toString()

                val total = expensesByDate[date]?.sumOf { it.amount } ?: 0.0
                container.textViewTotal.apply {
                    visibility = if (total > 0) View.VISIBLE else View.GONE
                    text = "₹%.0f".format(total)
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatYearMonth(yearMonth: YearMonth): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
        return yearMonth.format(formatter)
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textViewDate: TextView = view.findViewById(R.id.textViewDate)
    val textViewTotal: TextView = view.findViewById(R.id.textViewTotal)
}


