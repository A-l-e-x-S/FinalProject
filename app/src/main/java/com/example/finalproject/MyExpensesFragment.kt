package com.example.finalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Adapter.ExpensesAdapter
import com.example.finalproject.Model.ExpenseEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class MyExpensesFragment : Fragment(R.layout.fragment_my_expenses) {

    private lateinit var expensesAdapter: ExpensesAdapter
    private lateinit var recyclerView: RecyclerView
    private val uidToUsernameMap = mutableMapOf<String, String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "My Expenses"


        recyclerView = view.findViewById(R.id.myExpensesRecyclerView)
        expensesAdapter = ExpensesAdapter(uidToUsernameMap, showEditDelete = false)
        recyclerView.adapter = expensesAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val groupId = arguments?.getString("groupId")
            ?: MyExpensesFragmentArgs.fromBundle(requireArguments()).groupId
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        loadUsernamesForGroup(groupId) { usernamesMap ->
            uidToUsernameMap.clear()
            uidToUsernameMap.putAll(usernamesMap)
            expensesAdapter.notifyDataSetChanged()
        }

        FirebaseFirestore.getInstance()
            .collection("expenses")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("payerUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val expenses = snapshot?.documents?.map { doc ->
                    ExpenseEntity(
                        id = doc.id,
                        groupId = doc.getString("groupId") ?: groupId,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description"),
                        amount = doc.getDouble("amount") ?: 0.0,
                        payerUid = doc.getString("payerUid") ?: "",
                        timestamp = (doc.getTimestamp("timestamp")?.toDate()?.time) ?: System.currentTimeMillis(),
                        splitBetweenJson = Gson().toJson(doc.get("splitBetween") as? List<String> ?: listOf(currentUid)),
                        photoUrl = doc.getString("photoUrl")
                    )
                } ?: emptyList()
                expensesAdapter.updateExpenses(expenses)
                val emptyStateContainer = view.findViewById<LinearLayout>(R.id.emptyStateContainer)

                expensesAdapter.updateExpenses(expenses)
                if (expenses.isEmpty()) {
                    emptyStateContainer.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateContainer.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
    }

    private fun loadUsernamesForGroup(groupId: String, onLoaded: (Map<String, String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("groups").document(groupId).get()
            .addOnSuccessListener { doc ->
                val members = doc["members"] as? List<String> ?: emptyList()
                val map = mutableMapOf<String, String>()
                if (members.isEmpty()) {
                    onLoaded(map)
                    return@addOnSuccessListener
                }
                var loaded = 0
                for (uid in members) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            map[uid] = userDoc.getString("username") ?: "Unknown"
                        }
                        .addOnCompleteListener {
                            loaded++
                            if (loaded == members.size) {
                                onLoaded(map)
                            }
                        }
                }
            }
    }
}