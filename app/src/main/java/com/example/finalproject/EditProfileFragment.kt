package com.example.finalproject

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class EditProfileFragment : Fragment() {

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 2025

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            uploadImageToCloudinary(it)
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            uploadImageToCloudinary(photoUri!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Edit Profile"

        val profileImageView = view.findViewById<ImageView>(R.id.editIconImageView)
        val userNameEditText = view.findViewById<EditText>(R.id.userNameEditText)
        val userEmailEditText = view.findViewById<EditText>(R.id.userEmailEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("username") ?: ""
                    val email = doc.getString("email") ?: ""
                    val photoUrl = doc.getString("profilePhotoUrl")

                    userNameEditText.setText(name)
                    userEmailEditText.setText(email)

                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .into(profileImageView)
                        uploadedImageUrl = photoUrl
                    }
                }
            }

        val openGallery = View.OnClickListener {
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
        profileImageView.setOnClickListener(openGallery)
        profileImageView.setOnClickListener(openGallery)

        saveButton.setOnClickListener {
            val newName = userNameEditText.text.toString().trim()
            val newEmail = userEmailEditText.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedMap = mapOf(
                "username" to newName,
                "email" to newEmail,
                "profilePhotoUrl" to uploadedImageUrl
            )

            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(updatedMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.profileFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.photoUploadProgressBar)
        progressBar?.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "Uploading photo...", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    uploadedImageUrl = resultData["secure_url"] as? String

                    view?.findViewById<ImageView>(R.id.editIconImageView)?.let {
                        Picasso.get().load(uploadedImageUrl).into(it)
                    }

                    Toast.makeText(requireContext(), "Photo uploaded", Toast.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val imageFile = File(requireContext().cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        takePhotoLauncher.launch(photoUri)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

