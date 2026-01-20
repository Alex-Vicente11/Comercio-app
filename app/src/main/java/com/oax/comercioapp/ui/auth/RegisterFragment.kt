package com.oax.comercioapp.ui.auth


import android.os.Bundle
import android.util.Log
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

    // setup + observadores
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


}