package com.example.finalproject

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Adapter.GroupsAdapter
import com.example.finalproject.Model.GroupEntity
import com.example.finalproject.viewmodel.GroupViewModel
import androidx.appcompat.app.AppCompatActivity

class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var groupsAdapter: GroupsAdapter
    private val groupsList = mutableListOf<GroupEntity>()
    private lateinit var emptyStateContainer: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Home"
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        groupsRecyclerView = view.findViewById(R.id.groupsRecyclerView)
        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        groupsAdapter = GroupsAdapter(groupsList) { group ->
            val action = HomeFragmentDirections.actionHomeFragmentToCreatedGroupFragment(group.id)
            findNavController().navigate(action)
        }
        groupsRecyclerView.adapter = groupsAdapter

        val viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)
        viewModel.allGroups.observe(viewLifecycleOwner) { groups ->
            if (groups.isNotEmpty()) {
                emptyStateContainer.visibility = View.GONE
                groupsRecyclerView.visibility = View.VISIBLE

                groupsList.clear()
                groupsList.addAll(groups)
                groupsAdapter.notifyDataSetChanged()
            } else {
                emptyStateContainer.visibility = View.VISIBLE
                groupsRecyclerView.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_transaction -> {
                showSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSelectionDialog() {
        val options = arrayOf("Track Personal Expenses", "Create Group")

        AlertDialog.Builder(requireContext())
            .setTitle("Select an option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val action = HomeFragmentDirections.actionHomeFragmentToPersonalExpensesFragment()
                        findNavController().navigate(action)
                    }
                    1 -> {
                        val action = HomeFragmentDirections.actionHomeFragmentToCreateGroupExpensesFragment()
                        findNavController().navigate(action)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
