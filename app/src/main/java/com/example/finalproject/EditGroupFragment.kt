package com.example.finalproject

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.finalproject.viewmodel.GroupViewModel
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import java.io.File

class EditGroupFragment : Fragment() {

    private lateinit var groupViewModel: GroupViewModel
    private lateinit var groupId: String
    private var uploadedImageUrl: String? = null
    private var photoUri: Uri? = null
    private var isImageUploaded = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToCloudinary(it) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            uploadImageToCloudinary(photoUri!!)
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE = 2003

    private lateinit var groupNameEditText: EditText
    private lateinit var groupTypeSpinner: Spinner
    private lateinit var groupPhotoImageView: ImageView
    private lateinit var photoProgressBar: ProgressBar
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = EditGroupFragmentArgs.fromBundle(requireArguments()).groupId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_group_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Edit Group"

        groupViewModel = ViewModelProvider(this)[GroupViewModel::class.java]

        groupNameEditText = view.findViewById(R.id.groupNameEditText)
        groupTypeSpinner = view.findViewById(R.id.groupTypeSpinner)
        groupPhotoImageView = view.findViewById(R.id.groupPhotoImageView)
        photoProgressBar = view.findViewById(R.id.photoUploadProgressBar)
        submitButton = view.findViewById(R.id.submitButton)

        val cameraIcon = view.findViewById<ImageView>(R.id.cameraIconImageView)
        cameraIcon.setOnClickListener { showImageSourceDialog() }

        setupSpinner()
        loadGroupData()

        submitButton.setOnClickListener {
            updateGroup()
        }
    }

    private fun setupSpinner() {
        val types = listOf("Travel", "House", "Family", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupTypeSpinner.adapter = adapter
    }

    private fun loadGroupData() {
        groupViewModel.getGroupById(groupId).observe(viewLifecycleOwner) { group ->
            groupNameEditText.setText(group.groupName)
            uploadedImageUrl = group.groupPhotoUrl
            val typeIndex = (groupTypeSpinner.adapter as ArrayAdapter<String>).getPosition(group.groupType)
            groupTypeSpinner.setSelection(typeIndex)

            if (!group.groupPhotoUrl.isNullOrEmpty()) {
                Picasso.get().load(group.groupPhotoUrl).into(groupPhotoImageView)
            }
        }
    }

    private fun updateGroup() {
        val name = groupNameEditText.text.toString().trim()
        val type = groupTypeSpinner.selectedItem.toString()

        if (name.isEmpty()) {
            Snackbar.make(requireView(), "Please enter a group name", Snackbar.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("groups").document(groupId)

        groupRef.update(
            mapOf(
                "groupName" to name,
                "groupType" to type,
                "groupPhotoUrl" to uploadedImageUrl
            )
        ).addOnSuccessListener {
            groupViewModel.getGroupById(groupId).observe(viewLifecycleOwner) { oldGroup ->
                val updatedGroup = oldGroup.copy(
                    groupName = name,
                    groupType = type,
                    groupPhotoUrl = uploadedImageUrl
                )
                groupViewModel.insertGroup(updatedGroup)
                Snackbar.make(requireView(), "Group updated", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }.addOnFailureListener {
            Snackbar.make(requireView(), "Failed to update group", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> checkCameraPermissionAndTakePhoto()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun takePhoto() {
        val imageFile = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        takePhotoLauncher.launch(photoUri)
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        photoProgressBar.visibility = View.VISIBLE
        Snackbar.make(requireView(), "Uploading image...", Snackbar.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedImageUrl = imageUrl
                    isImageUploaded = true

                    if (isAdded) {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_group_placeholder)
                            .into(groupPhotoImageView)
                    }

                    Snackbar.make(requireView(), "Upload Success", Snackbar.LENGTH_SHORT).show()
                    photoProgressBar.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Snackbar.make(requireView(), "Upload Failed", Snackbar.LENGTH_SHORT).show()
                    photoProgressBar.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    photoProgressBar.visibility = View.GONE
                }
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Snackbar.make(requireView(), "Camera permission is required", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}