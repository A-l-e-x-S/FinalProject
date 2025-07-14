package com.example.finalproject

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Adapter.ExpensesAdapter
import com.example.finalproject.Adapter.MembersAdapter
import com.example.finalproject.Model.ExpenseEntity
import com.example.finalproject.Model.Member
import com.example.finalproject.viewmodel.ExpenseViewModel
import com.example.finalproject.viewmodel.GroupViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.firebase.Timestamp
import com.google.gson.reflect.TypeToken

class CreatedGroupFragment : Fragment() {

    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var expensesAdapter: ExpensesAdapter
    private val expensesList = mutableListOf<ExpenseEntity>()
    private lateinit var currentUid: String
    private var isGroupCreator = true
    private lateinit var groupViewModel: GroupViewModel
    private lateinit var groupId: String
    private lateinit var membersRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        groupId = CreatedGroupFragmentArgs.fromBundle(requireArguments()).groupId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_created_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]
        currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        groupViewModel.getGroupById(groupId).observe(viewLifecycleOwner) { group ->
            if (group == null) {
                findNavController().popBackStack(R.id.homeFragment, false)
                return@observe
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.title = group.groupName
            val members: List<String> = Gson().fromJson(
                group.membersJson,
                object : TypeToken<List<String>>() {}.type
            )
            isGroupCreator = group.createdByUid == currentUid
            loadUserDetailsForMembers(members) { uidMap ->
                uidToUsernameMap.clear()
                uidToUsernameMap.putAll(uidMap)

                expensesAdapter = ExpensesAdapter(expensesList, uidToUsernameMap)
                expensesRecyclerView.adapter = expensesAdapter
            }
        }

        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        addExpenseButton = view.findViewById(R.id.addExpenseButton)

        expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        expensesAdapter = ExpensesAdapter(expensesList, uidToUsernameMap)
        expensesRecyclerView.adapter = expensesAdapter

        FirebaseFirestore.getInstance()
            .collection("expenses")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val splitBetween = doc["splitBetween"] as? List<String> ?: listOf(currentUid)
                    val expense = ExpenseEntity(
                        id = doc.id,
                        groupId = groupId,
                        title = doc["title"] as? String ?: "",
                        description = doc["description"] as? String ?: "",
                        amount = (doc["amount"] as? Number)?.toDouble() ?: 0.0,
                        payerUid = doc["payerUid"] as? String ?: currentUid,
                        timestamp = (doc["timestamp"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        splitBetweenJson = Gson().toJson(splitBetween),
                        photoUrl = doc["photoUrl"] as? String
                    )
                    expenseViewModel.insertExpense(expense)
                }
            }

        expenseViewModel.getExpensesForGroup(groupId).observe(viewLifecycleOwner) { expenses ->
            expensesList.clear()
            expensesList.addAll(expenses)
            expensesAdapter.notifyDataSetChanged()

            if (expenses.isEmpty()) {
                emptyStateContainer.visibility = View.VISIBLE
                expensesRecyclerView.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                expensesRecyclerView.visibility = View.VISIBLE
            }
        }

        addExpenseButton.setOnClickListener {
            val action = CreatedGroupFragmentDirections
                .actionCreatedGroupFragmentToAddExpenseFragment(groupId)
            findNavController().navigate(action)
        }

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView)
        membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val addMemberButton = view.findViewById<Button>(R.id.addMemberButton)

        addMemberButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Enter an email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addMemberToGroupByEmail(email)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<EditText>(R.id.emailInput)?.setText("")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_created_group, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                sendGroupCreatedFlag()
                findNavController().popBackStack(R.id.homeFragment, false)
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
                    "Leave Group" -> confirmLeaveGroup()
                    "Delete Group" -> confirmDeleteGroup()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Yes") { _, _ -> leaveGroup() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup() {
        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("groups").document(groupId)

        groupRef.get().addOnSuccessListener { doc ->
            val members = doc.get("members") as? MutableList<String> ?: return@addOnSuccessListener

            if (currentUid in members) {
                members.remove(currentUid)
                groupRef.update("members", members).addOnSuccessListener {
                    recalculateExpensesAfterLeaving(currentUid)
                    Toast.makeText(requireContext(), "You left the group", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.homeFragment, false)
                }
                groupViewModel.deleteGroup(groupId)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to leave group", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteGroup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Group")
            .setMessage("This will delete the group for all members. Are you sure?")
            .setPositiveButton("Yes") { _, _ -> deleteGroupAndExpenses() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroupAndExpenses() {
        val db = FirebaseFirestore.getInstance()

        db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }

                batch.delete(db.collection("groups").document(groupId))

                batch.commit().addOnSuccessListener {
                    groupViewModel.deleteGroup(groupId)
                    Toast.makeText(requireContext(), "Group deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.homeFragment, false)
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete group", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val uidToUsernameMap = mutableMapOf<String, String>()

    private fun loadUserDetailsForMembers(
        members: List<String>,
        onComplete: (Map<String, String>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val map = mutableMapOf<String, String>()
        val memberDetails = mutableListOf<Member>()

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        var groupCreatorUid = ""

        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { groupDoc ->
                groupCreatorUid = groupDoc.getString("createdByUid") ?: ""

                var loaded = 0
                for (uid in members) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            var name = doc.getString("username") ?: "Unknown"
                            val email = doc.getString("email") ?: "unknown@example.com"
                            val suffixes = mutableListOf<String>()
                            if (uid == currentUid) suffixes.add("You")
                            if (uid == groupCreatorUid) suffixes.add("Creator")
                            if (suffixes.isNotEmpty()) {
                                name += " (${suffixes.joinToString(", ")})"
                            }

                            map[uid] = name
                            memberDetails.add(Member(name, email, uid))
                        }
                        .addOnFailureListener {
                            map[uid] = "Unknown"
                            memberDetails.add(Member("Unknown", "unknown@example.com", uid))
                        }
                        .addOnCompleteListener {
                            loaded++
                            if (loaded == members.size) {
                                membersRecyclerView.adapter = MembersAdapter(memberDetails)
                                onComplete(map)
                            }
                        }
                }
            }
    }

    private fun addMemberToGroupByEmail(email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = snapshot.documents[0]
                val newMemberUid = userDoc.id

                val groupRef = db.collection("groups").document(groupId)

                groupRef.get().addOnSuccessListener { doc ->
                    val members = (doc.get("members") as? MutableList<String>)?.toMutableList() ?: mutableListOf()

                    if (newMemberUid in members) {
                        Toast.makeText(requireContext(), "User already in group", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    members.add(newMemberUid)
                    groupRef.update("members", members)
                        .addOnSuccessListener {
                            groupViewModel.getGroupById(groupId).observe(viewLifecycleOwner) { group ->
                                val updatedGroup = group.copy(membersJson = Gson().toJson(members))
                                groupViewModel.insertGroup(updatedGroup)

                                val emailInput = view?.findViewById<EditText>(R.id.emailInput)
                                emailInput?.setText("")
                                emailInput?.requestFocus()

                                Toast.makeText(requireContext(), "User added to group", Toast.LENGTH_SHORT).show()

                                loadUserDetailsForMembers(members) { uidMap ->
                                    uidToUsernameMap.clear()
                                    uidToUsernameMap.putAll(uidMap)

                                    expensesAdapter = ExpensesAdapter(expensesList, uidToUsernameMap)
                                    expensesRecyclerView.adapter = expensesAdapter
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to search user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recalculateExpensesAfterLeaving(leftUid: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                for (doc in snapshot.documents) {
                    val expenseRef = doc.reference
                    val splitBetween = doc.get("splitBetween") as? List<String> ?: continue

                    if (splitBetween.contains(leftUid)) {
                        val newSplit = splitBetween.filter { it != leftUid }

                        batch.update(expenseRef, "splitBetween", newSplit)
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Group", "Expenses recalculated after user left")
                    }
                    .addOnFailureListener {
                        Log.e("Group", "Failed to recalculate expenses", it)
                    }
            }
    }

}