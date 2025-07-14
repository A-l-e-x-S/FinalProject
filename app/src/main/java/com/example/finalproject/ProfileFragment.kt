package com.example.finalproject

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.finalproject.Userdata.SessionManager
import com.squareup.picasso.Picasso
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.fragment.findNavController
import com.example.finalproject.room.AppDatabase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.reflect.TypeToken
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.WriteBatch

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        val userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        val userEmailTextView = view.findViewById<TextView>(R.id.userEmailTextView)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        val uid = SessionManager.getUserSession(requireContext())

        if (uid == null || FirebaseAuth.getInstance().currentUser == null) {
            (requireActivity() as MainActivity).showAuthNavigation()
            (requireActivity() as MainActivity).authNavController.navigate(R.id.loginFragment)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: ""
                    val email = document.getString("email") ?: ""
                    val photoUrl = document.getString("profilePhotoUrl")

                    userNameTextView.text = username
                    userEmailTextView.text = email

                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(profileImageView)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        val editButton = view.findViewById<Button>(R.id.editButton)
        editButton.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
            findNavController().navigate(action)
        }

        logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    SessionManager.clearUserSession(requireContext())
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                    (requireActivity() as MainActivity).showAuthNavigation()
                    (requireActivity() as MainActivity).authNavController.navigate(R.id.loginFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val deleteAccountButton = view.findViewById<Button>(R.id.deleteProfileButton)

        deleteAccountButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account permanently? This will remove all your data.")
                .setPositiveButton("Yes") { _, _ ->
                    val user = FirebaseAuth.getInstance().currentUser
                    val uid = user?.uid

                    if (uid == null) {
                        Toast.makeText(requireContext(), "No user session found", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val input = EditText(requireContext()).apply {
                        inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                        hint = "Enter your password"
                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Password")
                        .setView(input)
                        .setPositiveButton("Confirm") { _, _ ->
                            val password = input.text.toString().trim()
                            val email = user.email

                            if (email.isNullOrBlank() || password.isEmpty()) {
                                Toast.makeText(requireContext(), "Email or password is empty", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }

                            val credential = EmailAuthProvider.getCredential(email, password)

                            user.reauthenticate(credential)
                                .addOnSuccessListener {
                                    deleteAllUserData(uid, user)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Reauthentication failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteAllUserData(uid: String, user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).delete()
            .addOnSuccessListener {
                Log.d("ProfileFragment", "User profile deleted successfully")

                deleteUserExpenses(uid, db) {
                    updateUserGroups(uid, db) {
                        user.delete().addOnSuccessListener {
                            clearLocalUserData()

                            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                            (requireActivity() as MainActivity).showAuthNavigation()
                            (requireActivity() as MainActivity).authNavController.navigate(R.id.loginFragment)
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to delete account from authentication", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete user profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserExpenses(uid: String, db: FirebaseFirestore, onComplete: () -> Unit) {
        db.collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()

                for (doc in result.documents) {
                    val payerUid = doc.getString("payerUid")
                    val splitBetween = doc["splitBetween"] as? List<String> ?: continue
                    val groupId = doc.getString("groupId") ?: continue

                    if (uid in splitBetween) {
                        db.collection("groups").document(groupId)
                            .get()
                            .addOnSuccessListener { groupDoc ->
                                val groupMembers = groupDoc["members"] as? List<String> ?: listOf()

                                if (groupMembers.size <= 1) {
                                    batch.delete(doc.reference)
                                } else {
                                    val updatedSplit = splitBetween.filter { it != uid }

                                    val updates = mutableMapOf<String, Any>()
                                    updates["splitBetween"] = updatedSplit

                                    if (payerUid == uid && updatedSplit.isNotEmpty()) {
                                        updates["payerUid"] = updatedSplit[0]
                                    }

                                    if (updatedSplit.isEmpty()) {
                                        batch.delete(doc.reference)
                                    } else {
                                        batch.update(doc.reference, updates)
                                    }
                                }

                                if (doc == result.documents.last()) {
                                    batch.commit()
                                        .addOnSuccessListener { onComplete() }
                                        .addOnFailureListener { onComplete() }
                                }
                            }
                            .addOnFailureListener {
                                if (doc == result.documents.last()) onComplete()
                            }
                    } else if (doc == result.documents.last()) {
                        onComplete()
                    }
                }

                if (result.documents.isEmpty()) {
                    onComplete()
                }
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    private fun updateUserGroups(uid: String, db: FirebaseFirestore, onComplete: () -> Unit) {
        db.collection("groups")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { result ->
                Log.d("ProfileFragment", "Found ${result.documents.size} groups to update")

                if (result.documents.isEmpty()) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in result.documents) {
                    val groupId = doc.id
                    val members = doc.get("members") as? MutableList<String> ?: continue
                    members.remove(uid)

                    if (members.isEmpty()) {
                        batch.delete(db.collection("groups").document(groupId))
                    } else {
                        batch.update(db.collection("groups").document(groupId), "members", members)
                    }
                }

                batch.commit().addOnSuccessListener {
                    Log.d("ProfileFragment", "Successfully updated groups")
                    onComplete()
                }.addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Failed to update groups", e)
                    onComplete()
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to query groups", e)
                onComplete()
            }
    }

    private fun clearLocalUserData() {
        SessionManager.clearUserSession(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(requireContext()).clearAllTables()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error clearing local database", e)
            }
        }
    }
}