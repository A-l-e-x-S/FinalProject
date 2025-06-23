package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.backArrow).setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<Button>(R.id.submitRegisterButton).setOnClickListener {
            // Login logic will go here (e.g. Firebase auth)
            // After successful login:
            val action = RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
            findNavController().navigate(action)
        }
    }
}