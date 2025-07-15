package com.example.finalproject.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Model.ExpenseEntity
import com.example.finalproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import com.example.finalproject.CreatedGroupFragmentDirections
import com.example.finalproject.viewmodel.ExpenseViewModel
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog

class ExpensesAdapter(
    private val uidToUsernameMap: Map<String, String>
) : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {

    private val expenses: MutableList<ExpenseEntity> = mutableListOf()

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.expenseName)
        val amountTextView: TextView = itemView.findViewById(R.id.expenseAmount)
        val balanceTextView: TextView = itemView.findViewById(R.id.balanceTextView)
        val sharedInfoTextView: TextView = itemView.findViewById(R.id.sharedInfoTextView)
        val participantsTextView: TextView = itemView.findViewById(R.id.participantsTextView)
        val expenseImageView: ImageView = itemView.findViewById(R.id.expenseImageView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.expenseDescriptionTextView)
        val editExpenseButton: ImageView = itemView.findViewById(R.id.editExpenseButton)
        val deleteExpenseButton: ImageView = itemView.findViewById(R.id.deleteExpenseButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        Log.d("ADAPTER", "Binding expense: ${expense.title}")
        holder.nameTextView.text = expense.title
        holder.amountTextView.text = "₪ %.2f".format(expense.amount)

        val splitList: List<String> = Gson().fromJson(
            expense.splitBetweenJson,
            object : TypeToken<List<String>>() {}.type
        ) ?: emptyList()

        val splitCount = splitList.size.takeIf { it > 0 } ?: 1
        val share = expense.amount / splitCount
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        val balanceText = when {
            currentUid == null -> "Shared between $splitCount people"
            expense.payerUid == currentUid -> {
                val othersCount = splitList.count { it != currentUid }
                if (othersCount > 0) {
                    "You paid. Others owe you ₪ %.2f".format(share * othersCount)
                } else {
                    "You paid for yourself only"
                }
            }
            splitList.contains(currentUid) -> {
                "You owe ₪ %.2f".format(share)
            }
            else -> {
                "Not part of this expense"
            }
        }

        holder.balanceTextView.text = balanceText
        holder.sharedInfoTextView.text = "Shared between $splitCount ${if (splitCount == 1) "person" else "people"}"

        val participantsText = buildString {
            append("Split between: ")
            append(splitList.joinToString { uid ->
                when {
                    uid == currentUid -> "You"
                    uidToUsernameMap.containsKey(uid) -> uidToUsernameMap[uid] ?: "Unknown"
                    else -> "Unknown"
                }
            })
        }
        holder.participantsTextView.text = participantsText

        if (!expense.photoUrl.isNullOrEmpty()) {
            holder.expenseImageView.visibility = View.VISIBLE
            Picasso.get()
                .load(expense.photoUrl)
                .placeholder(R.drawable.ic_group_placeholder)
                .into(holder.expenseImageView)
        } else {
            holder.expenseImageView.visibility = View.GONE
        }

        if (!expense.description.isNullOrBlank()) {
            holder.descriptionTextView.visibility = View.VISIBLE
            holder.descriptionTextView.text = expense.description
        } else {
            holder.descriptionTextView.visibility = View.GONE
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (expense.payerUid == currentUserId) {
            holder.editExpenseButton.visibility = View.VISIBLE
            holder.editExpenseButton.setOnClickListener {
                val action = CreatedGroupFragmentDirections
                    .actionCreatedGroupFragmentToEditExpenseFragment(expense.id)
                it.findNavController().navigate(action)
            }

            holder.deleteExpenseButton.visibility = View.VISIBLE
            holder.deleteExpenseButton.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setPositiveButton("Yes") { _, _ ->
                        FirebaseFirestore.getInstance()
                            .collection("expenses")
                            .document(expense.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Deleted", Toast.LENGTH_SHORT).show()
                                val context = holder.itemView.context
                                if (context is LifecycleOwner) {
                                    val viewModel = ViewModelProvider(context as ViewModelStoreOwner)[ExpenseViewModel::class.java]
                                    viewModel.deleteExpense(expense)
                                }
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            holder.editExpenseButton.visibility = View.GONE
            holder.deleteExpenseButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun updateExpenses(newExpenses: List<ExpenseEntity>) {
        expenses.clear()
        expenses.addAll(newExpenses)
        notifyDataSetChanged()
    }
}