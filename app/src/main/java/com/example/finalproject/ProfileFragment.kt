package com.example.finalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finalproject.Userdata.SessionManager
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.room.UserDao
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog

class ProfileFragment : Fragment() {

    private lateinit var userDao: UserDao

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

        userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val userId = SessionManager.getUserSession(requireContext())

        if (userId == -1) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        lifecycleScope.launch {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userNameTextView.text = user.username
                userEmailTextView.text = user.email

                Picasso.get()
                    .load(user.profilePhotoUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView)
            }
        }

        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    SessionManager.clearUserSession(requireContext())
                    (requireActivity() as MainActivity).showAuthNavigation()
                    (requireActivity() as MainActivity).authNavController.navigate(R.id.loginFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<Button>(R.id.deleteProfileButton).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile and all related data?")
                .setPositiveButton("Yes") { _, _ ->
                    val userId = SessionManager.getUserSession(requireContext())

                    lifecycleScope.launch {
                        val database = AppDatabase.getDatabase(requireContext())
                        val userDao = database.userDao()
                        val groupDao = database.groupDao()
                        val expenseDao = database.expenseDao()

                        val userGroups = groupDao.getGroupsByUser(userId)

                        userGroups.forEach { group ->
                            expenseDao.deleteExpensesByGroup(group.id)
                        }
                        groupDao.deleteGroupsByUser(userId)
                        userDao.deleteUserById(userId)
                        SessionManager.clearUserSession(requireContext())
                        (requireActivity() as MainActivity).showAuthNavigation()
                        (requireActivity() as MainActivity).authNavController.navigate(R.id.loginFragment)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
