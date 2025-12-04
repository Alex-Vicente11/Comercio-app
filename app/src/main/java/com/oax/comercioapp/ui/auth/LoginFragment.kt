package com.oax.comercioapp.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.oax.comercioapp.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        setupTextWatchers()

        //setupTestingButtons()
    }

    private fun setupTestingButtons() {
        println("MODO TESTING")

        // Simular login exitoso
        binding.buttonLogin.setOnClickListener {
            println("\n Test 1: Login con credenciales de prueba")
            authViewModel.login("test@test.com", "Test1234")
        }

        // Simular guest
        binding.buttonGuest.setOnClickListener {
            println("\n Test 2: Botón guest presionado")
            Snackbar.make(binding.root, "Modo guest (sin implementar)", Snackbar.LENGTH_SHORT).show()
        }

        // Simular error manual
        binding.tvForgotPassword.setOnClickListener {
            println("\n Test 3: Simulacion error manual")
            showLoading(false)
            showError("Este es un error de prueba")
        }
    }


    private fun setupObservers()  {
        authViewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    showLoading(true)
                }

                is NetworkResult.Success -> {
                    showLoading(false)
                    hideError()

                    val message = getString(R.string.success_login)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(resources.getColor(R.color.green, null))
                        .show()
                }

                is NetworkResult.Error -> {
                    showLoading(false)
                    showError(result.message)
                }
            }
        }

        authViewModel.cartMergeInfo.observe(viewLifecycleOwner) { (merged, count) ->
            if (merged && count > 0) {
                val message = "Carrito fusionado: $count productos agregados"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(resources.getColor(R.color.green, null))
                    .show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            handleLoginClick()
        }
    }

    private fun setupTextWatchers() {
        binding.textEditEmail.addTextChangedListener {
            clearEmailError()
        }

        binding.textEditPassword.addTextChangedListener {
            clearPasswordError()
        }
    }

    private fun handleLoginClick() {
        clearAllErrors()
        val email = binding.textEditEmail.text.toString().trim()
        val password = binding.textEditPassword.text.toString()

        if (!validateEmail(email)) return
        if (!validatePassword(password)) return

        authViewModel.login(email, password)
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.textInputLayoutEmail.error = getString(R.string.error_empty_email)
                binding.textEditEmail.requestFocus()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.textInputLayoutEmail.error = getString(R.string.error_invalid_email)
                binding.textEditEmail.requestFocus()
                false
            }
            else -> {
                binding.textInputLayoutEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                binding.textInputLayoutPassword.error = getString(R.string.error_empty_password)
                binding.textEditPassword.requestFocus()
                false
            }
            else -> {
                binding.textInputLayoutPassword.error = null
                true
            }
        }
    }

    private fun clearAllErrors() {
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null
        hideError()
    }

    private fun clearEmailError() {
        binding.textInputLayoutEmail.error = null
        hideError()
    }

    private fun clearPasswordError() {
        binding.textInputLayoutPassword.error = null
        hideError()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGuest.isEnabled = !isLoading
        binding.textEditEmail.isEnabled = !isLoading
        binding.textEditPassword.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.tvErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun hideError() {
        binding.tvErrorMessage.visibility = View.GONE
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}