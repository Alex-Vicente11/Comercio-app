package com.oax.comercioapp.ui.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.oax.comercioapp.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.oax.comercioapp.databinding.FragmentLoginBinding
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. LiveDate.observe -> StateFlow.collect con repeatOnLifecycle
 *    El original usaba: authViewModel.loginState.observe(viewLifecycleOwner) { }
 *    El problema: LiveData.observe puede entregar valores en estados del ciclo de vida donde la UI
 *    no está visible (STARTED), lo que puede causar actualizaciones visuales inesperadas o crashes.
 *
 *    repeatOnLifecycle(Lifecycle.State.STARTED) garantiza que el collect solo corre cuando el Fragment es visible.
 *    Se cancela automáticamente en onStop y se reinicia en onStart - sin memory leaks
 *
 * 2. ELIMINADO: cartMergeInfo.observe
 *    Ya no existe en AuthViewModel. Con el nuevo enfoque el carrito simplemente se recarga al entrar
 *    a la pantalla - no necesita notificar si hubo merge. Simplifica el flujo post-login.
 *
 * 3. ELIMINADAS: validaciones duplicadas en el Fragment
 *    validateEmail() y validatePassword() existían aquí Y en LoginUseCase.
 *    El Fragment solo valida que los campos no estén vacíos antes de llamar al ViewModel. La validación
 *    de formato y reglas de negocio la hace el use case y el error regresa como UiState. Error con el mensaje correcto.
 *
 *    EXCEPTION: la validación de campo vacío SÍ queda en el Fragment porque es feedback inmediato de UI (error
 *    en el TextInputLayout) que no necesita pasar por el ViewModel. Es la única validación que pertenece aquí
 *
 * 4. ELIMINADOS: los Log.d excesivos
 *
 * 5. onLoginHadled() después de navegar
 *    Resetea el estado de Idle para evitar que al rotar la pantalla se vuelva a ejecutar la navegación.
 *    Patrón "Consume once" con StateFlow.
 */
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    companion object {
        private const val TAG = "LoginFragment"
    }

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

    }

    private fun setupObservers()  {
        viewLifecycleOwner.lifecycleScope.launch {
            /**
             * repeatOnLifecycle: el collect se suspende cuando el Fragment pasa a STOPPED (pantalla no visible)
             * y se reanuda en STARTED. Reemplaza el manejo automático de ciclo de vida que hacía LiveData
             */
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.loginState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading(true)

                        is UiState.Success -> {
                            showLoading(false)
                            showSnackbar(
                                message = getString(R.string.success_login),
                                colorRes = R.color.green
                            )
                            // Marcar como procesado ANTES de navegar
                            // para evitar doble navegación en recreación de Fragment
                            authViewModel.onLoginHandled()
                            view?.postDelayed({ navigateToHome() }, 500)
                        }

                        is UiState.Error -> {
                            showLoading(false)
                            showError(state.message)
                        }

                        is UiState.Idle -> {
                            showLoading(false)
                            hideError()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener { handleLoginClick() }

        binding.buttonGuest.setOnClickListener { navigateToHome() }

        binding.tvToRegister.setOnClickListener { navigateToRegister() }
    }

    private fun setupTextWatchers() {
        binding.textEditEmail.addTextChangedListener { clearEmailError() }

        binding.textEditPassword.addTextChangedListener { clearPasswordError() }
    }

    private fun handleLoginClick() {
        clearAllErrors()

        val email = binding.textEditEmail.text.toString().trim()
        val password = binding.textEditPassword.text.toString()

        // Única validación que pertenece al Fragment: campos vacíos
        // El formato y las reglas de negocio las valida LoginUseCase
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = getString(R.string.error_empty_email)
            binding.textEditEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = getString(R.string.error_empty_password)
            binding.textEditPassword.requestFocus()
            return
        }

        authViewModel.login(email, password)
    }

    private fun navigateToHome() {
        if (!isAdded || view == null) return
        try {
            findNavController().navigate(R.id.navigation_cart)
        } catch (e: Exception) {
            showSnackbar("Error de navegación $e")
        }
    }

    private fun navigateToRegister() {
        if (!isAdded || view == null) return
        try {
            findNavController().navigate(R.id.registerFragment)
        } catch (e: Exception) {
            showSnackbar("Error de navegación $e")
        }
    }

    // UI helpers

    private fun showSnackbar(
        message: String,
        colorRes: Int? = null
    ) {
        if (_binding == null || !isAdded || view == null) return
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).apply {
            colorRes?.let {
                setBackgroundTint(resources.getColor(it, null))
                show()
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