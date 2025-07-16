package com.example.finalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.finalproject.Userdata.SessionManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToUserRegistrationFragment()
            findNavController().navigate(action)
        }

        view.findViewById<Button>(R.id.loginButton).setOnClickListener {
            val email = view.findViewById<EditText>(R.id.emailEditText).text.toString().trim()
            val password = view.findViewById<EditText>(R.id.passwordEditText).text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(requireView(), "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val username = document.getString("username") ?: ""

                                    SessionManager.saveUserSession(requireContext(), uid)

                                    Snackbar.make(requireView(), "Welcome, $username!", Snackbar.LENGTH_SHORT).show()

                                    (requireActivity() as MainActivity).showMainNavigation()
                                    val mainNavController = (requireActivity() as MainActivity).mainNavController
                                    mainNavController.navigate(R.id.homeFragment)
                                } else {
                                    Snackbar.make(requireView(), "User registered but profile is missing. Please contact support.", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Snackbar.make(requireView(), "Failed to load profile", Snackbar.LENGTH_SHORT).show()
                            }

                    }  else {
                val errorMessage = when (val exception = task.exception) {
                is FirebaseAuthInvalidUserException -> "User not found. Please register first."
                is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                else -> "Login failed: ${exception?.message}"
            }
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        }
        }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<EditText>(R.id.emailEditText)?.setText("")
        view?.findViewById<EditText>(R.id.passwordEditText)?.setText("")
    }
}