package com.example.finalproject

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.squareup.picasso.Picasso
import java.io.File
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import com.example.finalproject.Model.UserEntity
import com.example.finalproject.Userdata.SessionManager
import com.example.finalproject.room.AppDatabase
import kotlinx.coroutines.launch


class UserRegistrationFragment : Fragment() {

    private var uploadedProfileImageUrl: String? = null
    private var profilePhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToCloudinary(it) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && profilePhotoUri != null) {
            uploadImageToCloudinary(profilePhotoUri!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost = requireActivity()

        (menuHost as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        val profileImageView = view.findViewById<ImageView>(R.id.profilePhotoImageView)
        profileImageView.setOnClickListener {
            showImageSourceDialog()
        }
            view.findViewById<Button>(R.id.submitRegisterButton).setOnClickListener {
                val email = view.findViewById<EditText>(R.id.emailInput).text.toString().trim()
                val password = view.findViewById<EditText>(R.id.passwordInput).text.toString().trim()
                val repeatPassword = view.findViewById<EditText>(R.id.repeatPasswordInput).text.toString().trim()
                val username = view.findViewById<EditText>(R.id.nameInput).text.toString().trim()

                if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty() || username.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (password != repeatPassword) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                    val existingUser = userDao.getUserByEmail(email)

                    if (existingUser != null) {
                        Toast.makeText(requireContext(), "User with this email already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        val newUser = UserEntity(
                            email = email,
                            password = password,
                            username = username,
                            profilePhotoUrl = uploadedProfileImageUrl
                        )
                        val userId = userDao.insertUser(newUser).toInt()
                        SessionManager.saveUserSession(requireContext(), userId)

                        (requireActivity() as MainActivity).showMainNavigation()
                        val mainNavController = (requireActivity() as MainActivity).mainNavController
                        mainNavController.navigate(R.id.homeFragment)
                    }
                }
            }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> checkCameraPermissionAndTakePhoto()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1003)
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val imageFile = File(requireContext().cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        profilePhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        takePhotoLauncher.launch(profilePhotoUri)
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        Toast.makeText(requireContext(), "Uploading photo...", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedProfileImageUrl = imageUrl

                    Toast.makeText(requireContext(), "Photo uploaded successfully", Toast.LENGTH_SHORT).show()

                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(view?.findViewById(R.id.profilePhotoImageView))
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1003 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }
}
