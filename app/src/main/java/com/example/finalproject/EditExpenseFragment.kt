package com.example.finalproject

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.finalproject.Model.ExpenseEntity
import com.example.finalproject.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.gson.Gson
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import java.io.File

class EditExpenseFragment : Fragment() {

    private lateinit var expenseViewModel: ExpenseViewModel
    private var uploadedImageUrl: String? = null
    private var photoUri: Uri? = null
    private var currentGroupId: String? = null
    private var currentSplitBetweenJson: String? = null

    private val CAMERA_PERMISSION_REQUEST_CODE = 2004

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImageToCloudinary(it) }
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
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expenseId = EditExpenseFragmentArgs.fromBundle(requireArguments()).expenseId
        val expenseNameEditText = view.findViewById<EditText>(R.id.expenseNameEditText)
        val expenseAmountEditText = view.findViewById<EditText>(R.id.expenseAmountEditText)
        val notesEditText = view.findViewById<EditText>(R.id.notesExpensesEditText)
        val expenseImageView = view.findViewById<ImageView>(R.id.expenseImageView)
        val addImageButton = view.findViewById<Button>(R.id.addImageButton)
        val saveButton = view.findViewById<Button>(R.id.saveExpenseButton)
        var existingPhotoUrl: String? = null
        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        FirebaseFirestore.getInstance().collection("expenses").document(expenseId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                    val payerUid = doc.getString("payerUid")
                    if (payerUid != currentUid) {
                        Snackbar.make(requireView(), "You can only edit your own expenses", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                        return@addOnSuccessListener
                    }

                    val title = doc.getString("title") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val description = doc.getString("description") ?: ""
                    val photoUrl = doc.getString("photoUrl")
                    val groupId = doc.getString("groupId") ?: ""
                    val splitBetween = doc["splitBetween"] as? List<String> ?: listOf(currentUid)
                    existingPhotoUrl = photoUrl

                    expenseNameEditText.setText(title)
                    expenseAmountEditText.setText(amount.toString())
                    notesEditText.setText(description)

                    if (!photoUrl.isNullOrEmpty()) {
                        expenseImageView.visibility = View.VISIBLE
                        Picasso.get()
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .into(expenseImageView)
                    }

                    uploadedImageUrl = photoUrl
                    currentGroupId = groupId
                    currentSplitBetweenJson = Gson().toJson(splitBetween)
                } else {
                    Snackbar.make(requireView(), "Expense not found", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener {
                Snackbar.make(requireView(), "Failed to load expense", Snackbar.LENGTH_SHORT).show()
            }

        addImageButton.setOnClickListener {
            val options = arrayOf("Choose from Gallery", "Take a Photo")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Image")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> pickImageLauncher.launch("image/*")
                        1 -> checkCameraPermissionAndTakePhoto()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        saveButton.setOnClickListener {
            val title = expenseNameEditText.text.toString().trim()
            val amountStr = expenseAmountEditText.text.toString().trim()
            val description = notesEditText.text.toString().trim()
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (title.isEmpty() || amountStr.isEmpty() || uid == null) {
                Snackbar.make(requireView(), "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null) {
                Snackbar.make(requireView(), "Invalid amount", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalPhotoUrl = uploadedImageUrl ?: existingPhotoUrl

            val updatedExpense = mapOf(
                "title" to title,
                "amount" to amount,
                "description" to description,
                "photoUrl" to finalPhotoUrl,
                "timestamp" to Timestamp.now()
            )

            FirebaseFirestore.getInstance()
                .collection("expenses")
                .document(expenseId)
                .update(updatedExpense)
                .addOnSuccessListener {
                    Snackbar.make(requireView(), "Expense updated successfully", Snackbar.LENGTH_SHORT).show()

                    val updatedEntity = ExpenseEntity(
                        id = expenseId,
                        groupId = currentGroupId ?: "",
                        title = title,
                        description = description,
                        amount = amount,
                        payerUid = uid,
                        timestamp = System.currentTimeMillis(),
                        splitBetweenJson = currentSplitBetweenJson ?: "[]",
                        photoUrl = finalPhotoUrl
                    )

                    expenseViewModel.insertExpense(updatedEntity)
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Snackbar.make(requireView(), "Failed to update expense", Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val imageFile = File(requireContext().cacheDir, "expense_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        takePhotoLauncher.launch(photoUri)
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        if (!isAdded) return
        val rootView = view ?: return
        val expenseImageView = rootView.findViewById<ImageView>(R.id.expenseImageView)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.photoUploadProgressBar)
        progressBar.visibility = View.VISIBLE
        Snackbar.make(requireView(), "Uploading image...", Snackbar.LENGTH_SHORT).show()

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedImageUrl = imageUrl

                    expenseImageView.visibility = View.VISIBLE
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(expenseImageView)

                    Snackbar.make(requireView(), "Image uploaded successfully", Snackbar.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Snackbar.make(requireView(), "Failed to upload image", Snackbar.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    progressBar.visibility = View.GONE
                }
            })
            .dispatch()
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

