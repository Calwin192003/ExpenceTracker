package com.example.expencetracker

import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import com.kizitonwose.calendar.core.DayPosition
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
    private var allExpenses = mutableListOf<Expense>()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var totalAmountTextView: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        totalAmountTextView = binding.tvTotalAmount

        adapter = ExpenseAdapter(expenses) { expenseToDelete ->
            lifecycleScope.launch {
                expenseDao.delete(expenseToDelete)
                expenses.remove(expenseToDelete)
                Toast.makeText(this@MainActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                updateDisplayedExpenses(adapter, binding.calendarView.findFirstVisibleMonth()?.yearMonth ?: YearMonth.now())
            }
        }
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
    private fun showAddExpenseDialog(preSelectedDate: LocalDate? = null) {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)

        dialogBinding.editTextCustomCategory.visibility = View.GONE

        dialogBinding.radioGroupCategory.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = dialogBinding.radioGroupCategory.findViewById<RadioButton>(checkedId)
            if (selectedRadioButton?.text.toString().equals("Add Category", ignoreCase = true)) {
                dialogBinding.editTextCustomCategory.visibility = View.VISIBLE
            } else {
                dialogBinding.editTextCustomCategory.visibility = View.GONE
            }
        }
        val selectedDate = preSelectedDate ?: LocalDate.now()
        val formattedDate = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        dialogBinding.textViewSelectedDate.text = "Selected Date: $formattedDate"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val amount = dialogBinding.editTextAmount.text.toString().toDoubleOrNull()
                val note = dialogBinding.editTextNote.text.toString()

                val selectedCategoryId = dialogBinding.radioGroupCategory.checkedRadioButtonId
                val selectedRadioButton = dialogBinding.radioGroupCategory.findViewById<RadioButton>(selectedCategoryId)
                var selectedCategory = selectedRadioButton?.text?.toString()

                if (selectedCategory.equals("Add Category", ignoreCase = true)) {
                    selectedCategory = dialogBinding.editTextCustomCategory.text.toString().ifBlank { null }
                }

                val selectedDate = preSelectedDate ?: LocalDate.now()
                val selectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                if (amount != null) {
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
                            updateDisplayedExpenses(adapter, binding.calendarView.findFirstVisibleMonth()?.yearMonth ?: YearMonth.now())
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeAllExpenses(adapter: ExpenseAdapter) {
        expenseDao.getAllExpenses().observe(this) { allExpenses ->
            this.allExpenses = allExpenses.toMutableList() // optional cache
            setupCalendar(allExpenses)

            binding.calendarView.post {
                val visibleMonth = binding.calendarView.findFirstVisibleMonth()?.yearMonth ?: YearMonth.now()
                updateDisplayedExpenses(adapter, visibleMonth)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDisplayedExpenses(adapter: ExpenseAdapter, yearMonth: YearMonth) {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val filtered = allExpenses.filter {
            it.date in startOfMonth..endOfMonth
        }.sortedByDescending { it.date }

        expenses.clear()
        expenses.addAll(filtered)
        adapter.notifyDataSetChanged()

        val totalAmount = filtered.sumOf { it.amount }
        totalAmountTextView.text = "₹${totalAmount.format(2)}"
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

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
            updateDisplayedExpenses(adapter, month.yearMonth)
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
                val title = dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault())
                textView.text = title
            }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            private val today = LocalDate.now()

            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textViewDate.text = day.date.dayOfMonth.toString()

                if (day.date == today) {
                    container.view.setBackgroundResource(R.drawable.bg_circle_accent) // custom circle drawable with accent color
                } else if (day.position == DayPosition.MonthDate) {
                    container.textViewDate.setTextColor(Color.BLACK)
                    container.view.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    container.textViewDate.setTextColor(Color.GRAY)
                    container.view.setBackgroundColor(Color.TRANSPARENT)
                }
                val date = day.date

                val total = expensesByDate[date]?.sumOf { it.amount } ?: 0.0
                container.textViewTotal.apply {
                    visibility = if (total > 0) View.VISIBLE else View.GONE
                    text = "₹%.0f".format(total)
                }

                container.view.setOnClickListener {
                    showAddExpenseDialog(preSelectedDate = date)
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


