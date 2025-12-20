// file: com/logesh/pftnavigator/ui/LoginFragment.kt
package com.logesh.pftnavigator.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.logesh.pftnavigator.R
import com.logesh.pftnavigator.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // --- Firebase Login Logic ---
        binding.loginButton.setOnClickListener {
            val email = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // If login is successful, navigate to the main screen
                        findNavController().navigate(R.id.action_loginFragment_to_languageSelectionFragment)
                    } else {
                        // The 'else' for a failed login goes INSIDE the listener
                        Toast.makeText(context, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // The 'else' for empty fields is correctly placed here
                Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        } // The loginButton listener ends here

        // --- Navigation to Sign Up Screen ---
        binding.textSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    } // The onViewCreated function ends here

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}