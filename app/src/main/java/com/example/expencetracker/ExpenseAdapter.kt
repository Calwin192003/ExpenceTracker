package com.example.expencetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expencetracker.data.Expense
import com.example.expencetracker.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        with(holder.binding) {
            textViewAmount.text = "₹%.2f".format(expense.amount)
            textViewDate.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))

            if (expense.category.isNullOrBlank()) {
                textViewCategory.visibility = View.GONE
            } else {
                textViewCategory.text = expense.category
                textViewCategory.visibility = View.VISIBLE
            }
            if (expense.note.isNullOrBlank()) {
                textViewNote.visibility = View.GONE
            } else {
                textViewNote.text = expense.note
                textViewNote.visibility = View.VISIBLE
            }
            imageViewDelete.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    override fun getItemCount(): Int = expenses.size
}
