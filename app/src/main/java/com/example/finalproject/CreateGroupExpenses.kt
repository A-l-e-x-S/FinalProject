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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.finalproject.Model.GroupEntity
import com.example.finalproject.viewmodel.GroupViewModel
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import com.squareup.picasso.Picasso
import androidx.core.content.ContextCompat
import com.example.finalproject.Userdata.SessionManager


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
        val viewModel = ViewModelProvider(this).get(GroupViewModel::class.java)

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
            var valid = true
            val userId = SessionManager.getUserSession(requireContext())

            if (groupName.isEmpty()) {
                groupNameEditText.error = "Group name is required"
                valid = false
            }

            if (valid) {
                lifecycleScope.launch {
                    val group = GroupEntity(groupName = groupName, groupType = selectedGroupType, groupPhotoUrl = uploadedImageUrl, userId = userId)
                    val groupId = viewModel.insertGroup(group)
                    val action = CreateGroupExpensesFragmentDirections
                        .actionCreateGroupExpensesFragmentToCreatedGroupFragment(groupId.toInt())
                    findNavController().navigate(action)

                    Toast.makeText(requireContext(), "Group created successfully", Toast.LENGTH_SHORT).show()
                }
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
        Toast.makeText(requireContext(), "Uploading started", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedImageUrl = imageUrl
                    isImageUploaded = true

                    Toast.makeText(requireContext(), "Upload Success", Toast.LENGTH_SHORT).show()

                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_group_placeholder)
                        .into(view?.findViewById(R.id.groupPhotoImageView))
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(requireContext(), "Upload Failed", Toast.LENGTH_SHORT).show()
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