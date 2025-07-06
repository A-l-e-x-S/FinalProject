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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproject.Userdata.SessionManager
import com.example.finalproject.room.AppDatabase
import kotlinx.coroutines.launch

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
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                val user = userDao.login(email, password)

                if (user != null) {
                    SessionManager.saveUserSession(requireContext(), user.id)

                    (requireActivity() as MainActivity).showMainNavigation()
                    val mainNavController = (requireActivity() as MainActivity).mainNavController
                    mainNavController.navigate(R.id.homeFragment)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }
}