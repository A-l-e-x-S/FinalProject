package com.example.finalproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finalproject.Userdata.SessionManager
import com.example.finalproject.room.AppDatabase
import com.example.finalproject.room.UserEntity
import com.example.finalproject.room.UserDao
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private lateinit var userDao: UserDao
    private var currentUser: UserEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Make sure your XML is named fragment_edit_profile.xml
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View references
        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        val editIconImageView = view.findViewById<ImageView>(R.id.editIconImageView)
        val userNameEditText = view.findViewById<EditText>(R.id.userNameEditText)
        val userEmailEditText = view.findViewById<EditText>(R.id.userEmailEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        // DAO & Session
        userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val userId = SessionManager.getUserSession(requireContext())
        if (userId == -1) {
            // Not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Load existing user data
        lifecycleScope.launch {
            currentUser = userDao.getUserById(userId)
            currentUser?.let { user ->
                userNameEditText.setText(user.username)
                userEmailEditText.setText(user.email)
                Picasso.get()
                    .load(user.profilePhotoUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView)
            }
        }

        // Open gallery to pick new image
        val openGallery = View.OnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        profileImageView.setOnClickListener(openGallery)
        editIconImageView.setOnClickListener(openGallery)

        // Save changes
        saveButton.setOnClickListener {
            val newUsername = userNameEditText.text.toString().trim()
            val newEmail = userEmailEditText.text.toString().trim()

            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                currentUser?.let { user ->
                    val updatedUser = user.copy(
                        username = newUsername,
                        email = newEmail,
                        profilePhotoUrl = selectedImageUri?.toString() ?: user.profilePhotoUrl
                    )
                    userDao.updateUser(updatedUser)
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    // Navigate back to ProfileFragment
                    findNavController().navigate(R.id.profileFragment)
                }
            }
        }

        // Cancel edits
        cancelButton.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    // Handle gallery result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                view?.findViewById<ImageView>(R.id.profileImageView)?.setImageURI(uri)
            }
        }
    }
}
