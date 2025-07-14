package com.example.finalproject

import android.os.Bundle
import android.view.*
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
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment() {

    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var groupsAdapter: GroupsAdapter
    private val groupsList = mutableListOf<GroupEntity>()
    private lateinit var emptyStateContainer: View
    private lateinit var currentUid: String
    private lateinit var viewModel: GroupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Home"

        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_transaction -> {
                        navigateToCreateGroup()
                        true
                    }
                    else -> false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        groupsRecyclerView = view.findViewById(R.id.groupsRecyclerView)
        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        groupsAdapter = GroupsAdapter(
            groupsList,
            onGroupClick = { group ->
            val action = HomeFragmentDirections.actionHomeFragmentToCreatedGroupFragment(group.firestoreId)
            findNavController().navigate(action)
        },
        onEditClick = { group ->
            val action = HomeFragmentDirections.actionHomeFragmentToEditGroupFragment(group.firestoreId)
            findNavController().navigate(action)
        },
        currentUid = currentUid
        )
        groupsRecyclerView.adapter = groupsAdapter
        viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        viewModel.syncGroups(currentUid, requireContext())

        viewModel.getGroupsForUser(currentUid).observe(viewLifecycleOwner) { groups ->
            val filteredGroups = groups.filter { group ->
                val members = Gson().fromJson<List<String>>(group.membersJson, object : TypeToken<List<String>>() {}.type)
                members.contains(currentUid)
            }

            if (filteredGroups.isNotEmpty()) {
                emptyStateContainer.visibility = View.GONE
                groupsRecyclerView.visibility = View.VISIBLE

                groupsList.clear()
                groupsList.addAll(filteredGroups)
                groupsAdapter.notifyDataSetChanged()
            } else {
                emptyStateContainer.visibility = View.VISIBLE
                groupsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun navigateToCreateGroup() {
        val action = HomeFragmentDirections.actionHomeFragmentToCreateGroupExpensesFragment()
        findNavController().navigate(action)
    }
}