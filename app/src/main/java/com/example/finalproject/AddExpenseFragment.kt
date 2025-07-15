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
import android.widget.Toast
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

class AddExpenseFragment : Fragment() {

    private lateinit var expenseViewModel: ExpenseViewModel
    private var uploadedImageUrl: String? = null
    private var photoUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 2003
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

        val expenseNameEditText = view.findViewById<EditText>(R.id.expenseNameEditText)
        val expenseAmountEditText = view.findViewById<EditText>(R.id.expenseAmountEditText)
        val notesEditText = view.findViewById<EditText>(R.id.notesExpensesEditText)
        val saveButton = view.findViewById<Button>(R.id.saveExpenseButton)

        val groupId = AddExpenseFragmentArgs.fromBundle(requireArguments()).groupId
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        val addImageButton = view.findViewById<Button>(R.id.addImageButton)

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
            val expenseName = expenseNameEditText.text.toString().trim()
            val expenseAmountStr = expenseAmountEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()

            if (expenseName.isEmpty() || expenseAmountStr.isEmpty() || uid == null) {
                Snackbar.make(requireView(), "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = expenseAmountStr.toDoubleOrNull()
            if (amount == null) {
                Snackbar.make(requireView(), "Invalid amount", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance()
                .collection("groups")
                .document(groupId)
                .get()
                .addOnSuccessListener { groupDoc ->
                    val members = groupDoc["members"] as? List<String> ?: listOf(uid)

                    val expenseData = hashMapOf(
                        "groupId" to groupId,
                        "amount" to amount,
                        "description" to notes,
                        "title" to expenseName,
                        "payerUid" to uid,
                        "splitBetween" to members,
                        "timestamp" to Timestamp.now(),
                        "photoUrl" to uploadedImageUrl
                    )

                    FirebaseFirestore.getInstance()
                        .collection("expenses")
                        .add(expenseData)
                        .addOnSuccessListener { expenseDocRef ->
                            val firestoreId = expenseDocRef.id
                            Snackbar.make(requireView(), "Expense saved successfully", Snackbar.LENGTH_SHORT).show()

                            val splitJson = Gson().toJson(members)

                            val cachedExpense = ExpenseEntity(
                                id = firestoreId,
                                groupId = groupId,
                                title = expenseName,
                                description = notes,
                                amount = amount,
                                payerUid = uid,
                                timestamp = System.currentTimeMillis(),
                                splitBetweenJson = splitJson,
                                photoUrl = uploadedImageUrl
                            )
                            expenseViewModel.insertExpense(cachedExpense)
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener {
                            Snackbar.make(requireView(), "Failed to save expense", Snackbar.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Snackbar.make(requireView(), "Failed to load group", Snackbar.LENGTH_SHORT).show()
                }
        }
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
        val imageFile = File(requireContext().cacheDir, "expense_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
        takePhotoLauncher.launch(photoUri)
    }

    private fun uploadImageToCloudinary(imageUri: Uri) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.photoUploadProgressBar)
        progressBar?.visibility = View.VISIBLE
        Snackbar.make(requireView(), "Uploading image...", Snackbar.LENGTH_SHORT).show()
        val expenseImageView = view?.findViewById<ImageView>(R.id.expenseImageView)

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    uploadedImageUrl = imageUrl

                    expenseImageView?.visibility = View.VISIBLE
                    Picasso.get()
                        .load(imageUrl)
                        .into(expenseImageView)

                    Snackbar.make(requireView(), "Image uploaded successfully", Snackbar.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Snackbar.make(requireView(), "Upload failed", Snackbar.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    progressBar?.visibility = View.GONE
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