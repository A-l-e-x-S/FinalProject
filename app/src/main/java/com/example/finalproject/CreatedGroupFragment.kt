package com.example.finalproject

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Adapter.ExpensesAdapter
import com.example.finalproject.Model.ExpenseEntity
import com.example.finalproject.viewmodel.ExpenseViewModel
import com.example.finalproject.viewmodel.GroupViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CreatedGroupFragment : Fragment() {

    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var expensesRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var expensesAdapter: ExpensesAdapter
    private val expensesList = mutableListOf<ExpenseEntity>()

    // Пока тестово — потом добавим логику кто создатель
    private val isGroupCreator = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_created_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.let { CreatedGroupFragmentArgs.fromBundle(it).groupId } ?: return
        val groupViewModel = ViewModelProvider(this).get(GroupViewModel::class.java)

        groupViewModel.getGroupById(groupId).observe(viewLifecycleOwner) { group ->
            (requireActivity() as AppCompatActivity).supportActionBar?.title = group.groupName
        }
        val expenseViewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)
        expensesRecyclerView = view.findViewById<RecyclerView>(R.id.expensesRecyclerView)

        expenseViewModel.getExpensesForGroup(groupId).observe(viewLifecycleOwner) { expenses ->
            expensesList.clear()
            expensesList.addAll(expenses)
            expensesAdapter.notifyDataSetChanged()

            if (expenses.isNotEmpty()) {
                emptyStateContainer.visibility = View.GONE
                expensesRecyclerView.visibility = View.VISIBLE
            }
        }

        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView)
        addExpenseButton = view.findViewById(R.id.addExpenseButton)
        expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        expensesAdapter = ExpensesAdapter(expensesList)
        expensesRecyclerView.adapter = expensesAdapter

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, String>>("newExpense")
            ?.observe(viewLifecycleOwner) { expense ->
                expense?.let {
                    val name = it.first
                    val amount = it.second

                    expensesList.add(ExpenseEntity(groupId = 1, expenseName = name, amount = amount.toDouble()))
                    expensesAdapter.notifyDataSetChanged()
                    emptyStateContainer.visibility = View.GONE
                    expensesRecyclerView.visibility = View.VISIBLE
                }
            }

        addExpenseButton.setOnClickListener {
            val action = CreatedGroupFragmentDirections
                .actionCreatedGroupFragmentToAddExpenseFragment(groupId) // Передаём ID выбранной группы
            findNavController().navigate(action)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                sendGroupCreatedFlag()
                findNavController().navigateUp()
                true
            }
            R.id.menu_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sendGroupCreatedFlag() {
        findNavController().previousBackStackEntry?.savedStateHandle?.set("group_created", true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_created_group, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun showSettingsDialog() {
        val options = if (isGroupCreator) {
            arrayOf("Leave Group", "Delete Group")
        } else {
            arrayOf("Leave Group")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Group Options")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Leave Group" -> {
                        // Логика выхода из группы
                        findNavController().navigateUp()
                    }
                    "Delete Group" -> {
                        // Логика удаления группы
                        findNavController().navigateUp()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
