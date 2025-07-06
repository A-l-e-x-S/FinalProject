package com.example.finalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.finalproject.Model.ExpenseEntity
import com.example.finalproject.viewmodel.ExpenseViewModel

class AddExpenseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expenseNameEditText = view.findViewById<EditText>(R.id.expenseNameEditText)
        val expenseAmountEditText = view.findViewById<EditText>(R.id.expenseAmountEditText)
        val saveButton = view.findViewById<Button>(R.id.saveExpenseButton)
        val expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        val args = AddExpenseFragmentArgs.fromBundle(requireArguments())
        val groupId = args.groupId

        saveButton.setOnClickListener {
            val expenseName = expenseNameEditText.text.toString().trim()
            val expenseAmount = expenseAmountEditText.text.toString().trim()

            if (expenseName.isEmpty() || expenseAmount.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expense = ExpenseEntity(
                groupId = groupId,
                expenseName = expenseName,
                amount = expenseAmount.toDouble()
            )

            expenseViewModel.insertExpense(expense)
            findNavController().navigateUp()
        }
    }
}
