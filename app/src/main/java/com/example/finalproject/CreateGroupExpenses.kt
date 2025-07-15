package com.example.finalproject

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.finalproject.viewmodel.GroupViewModel
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import com.squareup.picasso.Picasso
import androidx.core.content.ContextCompat
import com.example.finalproject.Model.GroupEntity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class CreateGroupExpensesFragment : Fragment() {

    private var uploadedImageUrl: String? = null
    private var photoUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToCloudinary(it) }
    }
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            uploadImageToCloudinary(photoUri!!)
        }
    }
    private val CAMERA_PERMISSION_REQUEST_CODE = 1003
    private var isImageUploaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_group_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupNameEditText = view.findViewById<EditText>(R.id.groupNameEditText)
        val groupTypeSpinner = view.findViewById<Spinner>(R.id.groupTypeSpinner)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val groupPhotoImageView = view.findViewById<ImageView>(R.id.groupPhotoImageView)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.group_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            groupTypeSpinner.adapter = adapter
        }

        groupPhotoImageView.setOnClickListener {
            showImageSourceDialog()
        }

        submitButton.setOnClickListener {
            val groupName = groupNameEditText.text.toString().trim()
            val selectedGroupType = groupTypeSpinner.selectedItem.toString()

            if (groupName.isEmpty()) {
                groupNameEditText.error = "Group name is required"
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Snackbar.make(requireView(), "User session not found. Please login again.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val groupData = hashMapOf(
                "groupName" to groupName,
                "groupType" to selectedGroupType,
                "groupPhotoUrl" to uploadedImageUrl,
                "createdByUid" to uid,
                "members" to listOf(uid)
            )

            FirebaseFirestore.getInstance()
                .collection("groups")
                .add(groupData)
                .addOnSuccessListener { documentRef ->
                    Snackbar.make(requireView(), "Group created!", Snackbar.LENGTH_SHORT).show()

                    val groupId = documentRef.id
                    val groupEntity = GroupEntity(
                        firestoreId = groupId,
                        groupName = groupData["groupName"] as String,
                        groupType = groupData["groupType"] as String,
                        groupPhotoUrl = uploadedImageUrl,
                        createdByUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        membersJson = Gson().toJson(groupData["members"])
                    )
                    val viewModel = ViewModelProvider(this)[GroupViewModel::class.java]
                    viewModel.insertGroup(groupEntity)
                    val action = CreateGroupExpensesFragmentDirections
                        .actionCreateGroupExpensesFragmentToCreatedGroupFragment(groupId)
                    findNavController().navigate(action)
                }
                .addOnFailureListener { e ->
                    Snackbar.make(requireView(), "Failed to create group: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
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
        val progressBar = view?.findViewById<ProgressBar>(R.id.photoUploadProgressBar)
        val imageView = view?.findViewById<ImageView>(R.id.groupPhotoImageView)

        progressBar?.visibility = View.VISIBLE

        Snackbar.make(requireView(), "Uploading started", Snackbar.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedImageUrl = imageUrl
                    isImageUploaded = true

                    Snackbar.make(requireView(), "Upload Success", Snackbar.LENGTH_SHORT).show()

                    if (isAdded && imageView != null) {
                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_group_placeholder)
                            .into(imageView)
                    }

                    progressBar?.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Snackbar.make(requireView(), "Upload Failed", Snackbar.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    progressBar?.visibility = View.GONE
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