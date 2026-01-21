package com.oax.comercioapp.ui.auth


import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.oax.comercioapp.R
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import okhttp3.Dispatcher

class RegisterFragment: Fragment(){
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    companion object {
        private const val TAG = "RegisterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: RegisterFragment initialized")

        setupObservers()
        setupListeners()
        setupTextWatchers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroy: Cleaning up binding")
        _binding = null
    }



    // SETUP + OBSERVADORES
    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Configuring LiveData observers")

        // Observer para el estado de registro
        authViewModel.registerState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    Log.d(TAG, "resgisterState: Loading - Showing progress")
                    showLoading(true)
                }

                is NetworkResult.Success -> {
                    Log.d(TAG, "registerState: Success - ${result.data}")
                    showLoading(false)
                    hideErrorBanner()

                    showSafeSnackbar(
                        message = getString(R.string.register_success),
                        duration = Snackbar.LENGTH_SHORT,
                        backgroundColor = R.color.green
                    )

                    // Auto-navegacion a Home después de registro exitoso
                    CoroutineScope(Dispatcher.Main).launch {
                        delay(500) // Delay para que el usuario vea el Snackbar
                        navigateToHome()
                    }
                }

                is NetworkResult.Error -> {
                    Log.e(TAG, "registerState: Error - ${result.message}")
                    showLoading(false)
                    showErrorBanner(result.message ?: getString(R.string.error_generic))
                }
            }
        }

        // Obsever para información de fusion de carrito
        authViewModel.cartMergeInfo.observe(viewLifecycleOwner) { mergeInfo ->
            mergeInfo?.let {
                if (it.cartMigrated) {
                    Log.d(TAG, "cartMergeInfo: Cart Merged - ${it.cartItemsCount} items")
                    val messege = getString(
                        R.string.cart_merged_message,
                        it.cartItemsCount
                    )

                    showSafeSnackbar(
                        messege = messege,
                        duration = Snackbar.LENGTH_LONG,
                        backgroundColor = R.color.blue
                    )
                }
            }
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners: Configuring button listeners")

        // Boton registrarse
        binding.buttonRegisterCreateAccount.setOnClickListener {
            Log.d(TAG, "Register button clicked")
            performRegister()
        }

        // Link Iniciar Sesion
        binding.tvToLogInCreateAccount.setOnClickListener {
            Log.d(TAG, "Login link clicked")
            navigateToLogin()
        }
    }

    private fun setupTextWatchers() {
        Log.d(TAG, "setupTextWatchers: Configuring text change listeners")

        // Limpiar error de nombre al escribir
        binding.editTextCompleteNameCreateAccount.addTextChangedListener {
            if (binding.inputLayoutCompleteNameCreateAccount.error != null) {
                binding.inputLayoutCompleteNameCreateAccount.error = null
            }
        }

        // Limpiar error de email al escribir
        binding.editTextEmailCreateAccount.addTextChangedListener {
            if (binding.inputLayoutEmailCreateAccount.error != null) {
                binding.inputLayoutEmailCreateAccount.error = null
            }
        }

        // Limpiar error de password al escribir + actualizar indicador de requisitos
        binding.editTextPasswordCreateAccount.addTextChangedListener { text ->
            if (binding.inputLayoutPasswordCreateAccount.error != null) {
                binding.inputLayoutPasswordCreateAccount.error = null
            }
            // Actualizar indicador visual de requisitos
            updatePasswordRequirementsIndicator(text.toString())
        }

        // Limpiar error de confimar password al escribir
        binding.editTextConfirmPasswordCreateAccount.addTextChangedListener {
            if (binding.inputLayoutConfirmPasswordCreateAccount.error != null) {
                binding.inputLayoutPasswordCreateAccount.error = null
            }
        }
    }


    // VALIDACIONES + REGISTRO
    private fun validateFullName(name: String): Boolean {
        return when {
            name.isEmpty() -> {
                binding.inputLayoutCompleteNameCreateAccount.error =
                    getString(R.string.error_empty_email)
                binding.editTextCompleteNameCreateAccount.requestFocus()
                false
            }

            name.length < 3 -> {
                binding.inputLayoutCompleteNameCreateAccount.error =
                    getString(R.string.error_name_too_short)
                binding.editTextCompleteNameCreateAccount.requestFocus()
                false
            }

            else -> {
                binding.inputLayoutCompleteNameCreateAccount.error = null
                true
            }
        }
    }


    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.inputLayoutEmailCreateAccount.error =
                    getString(R.string.error_empty_email)
                binding.editTextEmailCreateAccount.requestFocus()
                false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.inputLayoutEmailCreateAccount.error =
                    getString(R.string.error_invalid_email)
                binding.editTextEmailCreateAccount.requestFocus()
                false
            }

            else -> {
                binding.inputLayoutEmailCreateAccount.error = null
                true
            }
        }
    }


    private fun validatePassword(password: String): Boolean {
        // Usar la validacion del ViewModel (corregida)
        val isValid = authViewModel.isValidPassword(password)

        return when {
            password.isEmpty() -> {
                binding.inputLayoutPasswordCreateAccount.error =
                    getString(R.string.error_empty_password)
                binding.editTextPasswordCreateAccount.requestFocus()
                false
            }

            !isValid -> {
                binding.inputLayoutPasswordCreateAccount.error =
                    getString(R.string.error_invalid_email)
                binding.editTextConfirmPasswordCreateAccount.requestFocus()
                false
            }
            else -> {
                binding.inputLayoutPasswordCreateAccount.error = null
                true
            }
        }
    }


    private fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                binding.inputLayoutConfirmPasswordCreateAccount.error =
                    getString(R.string.error_empty_confirm_password)
                binding.editTextConfirmPasswordCreateAccount.requestFocus()
                false
            }
            confirmPassword != password -> {
                binding.inputLayoutConfirmPasswordCreateAccount.error =
                    getString(R.string.error_passwords_dont_match)
                binding.editTextConfirmPasswordCreateAccount.requestFocus()
                false
            }

            else -> {
                binding.inputLayoutConfirmPasswordCreateAccount.error = null
                true
            }
        }
    }

    private fun validateTerms(): Boolean {
        return if (!binding.checkBoxTerms.isChecked) {
            showSafeSnackbar(
                message = getString(R.string.error_terms_not_accepted),
                duration = Snackbar.LENGTH_LONG,
                backgroundColor = R.color.orange
            )
            false
        } else {
            true
        }
    }

    private fun performRegister() {
        Log.d(TAG, "performRegister: Starting registration validation")

        // Obtener valores de los campos
        val fullName = binding.editTextCompleteNameCreateAccount.text.toString().trim()
        val email = binding.editTextEmailCreateAccount.text.toString().trim()
        val password = binding.editTextPasswordCreateAccount.text.toString()
        val confirmPassword = binding.editTextConfirmPasswordCreateAccount.text.toString()

        // Validaciones locales (guard clauses)
        if (!validateFullName(fullName)) {
            Log.d(TAG, "performRegister: Full name validation failed")
            return
        }

        if (!validateEmail(email)) {
            Log.d(TAG, "performRegister: Email validation failed")
            return
        }

        if (!validatePassword(password)) {
            Log.d(TAG, "performRegister: Password validation failed")
            return
        }

        if (!validateConfirmPassword(password, confirmPassword)) {
            Log.d(TAG, "performRegister: Confirm password validation failed")
            return
        }

        if (!validateTerms()) {
            Log.d(TAG, "performRegister: Terms validation failed")
            return
        }

        // Todas las validaciones pasaron - llamar al ViewModel
        Log.d(TAG, "performRegister: All validations passed - Calling ViewModel")
        authViewModel.register(
            email = email,
            password = password,
            userName = fullName
        )
    }

}