package com.oax.comercioapp.ui.auth


import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.oax.comercioapp.R
import com.oax.comercioapp.databinding.FragmentRegisterBinding
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. ELIMINADOS imports que causaban error de compilación
 *
 * 2. ELIMINADO: authViewModel.isValidPassword(password) desde el Fragment
 *    El original llamaba un métodoo del ViewModel para validar la contraseña:
 *      val isValid = authViewModel.isValidPassword(password)
 *    Problemas:
 *      a) isValidPassword no existía como public en el ViewModel refactorizado
 *      (estaba como private en LoginUseCase donde corresponde)
 *      b) Lógica de negocio llamada desde la UI - viola la separación de capas
 *    Ahora: el Fragment valida solo que el campo no esté vacío.
 *    RegisterUseCase valida las reglas (8 chars, mayúscula, número) y retorna Result.failure con el
 *    mensaje que UiState.Error muestra.
 *
 * 3. ELIMINADO: cartMergeInfo.observe
 *    El original intentaba acceder a mergeInfo.cartMigrated y mergeInfo.cartItemsCount
 *    como propiedades de objeto, pero cartMergeInfo era Pair<Boolean, Int> - incompatibilidad
 *    de tipos que causaba error de compilación. Con el nuevo enfoque esto desaparece.
 *
 * 4. CoroutinesScope(Dispatcher.Main) -> lifecycleScope
 *    El delay de 500ms para mostrar el Snackbar antes de navegar se maneja con view?.postDelayed
 *    igual que LoginFragment. lifecycleScope respeta el ciclo de vida - CoroutineScope manual no.
 *
 * 5. Validaciones de UI conservadas (nombre, email, confirmPassword, términos)
 *    Estas SÍ pertenecen al Fragment porque son feedback inmediato en los TextInputLayout. La diferencia
 *    con las reglas de negocio de la contraseña es que estas son reglas de formulario (campos vacíos, coincidencia
 *    de passwords) - no lógica de dominio.
 */
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

        setupObservers()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.registerState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading(true)

                        is UiState.Success -> {
                            showLoading(false)
                            hideErrorBanner()
                            showSnackbar(
                                message = getString(R.string.success_register),
                                colorRes = R.color.green
                            )
                            authViewModel.onRegisterHandled()
                            view?.postDelayed({ navigateToHome()}, 500)
                        }

                        is UiState.Error -> {
                            showLoading(false)
                            // state.message ya viene traducido desde RegisterUseCase
                            // Puede ser error de validación (contraseña débil) o de red
                            showErrorBanner(state.message)
                        }

                        is UiState.Idle -> {
                            showLoading(false)
                            hideErrorBanner()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.buttonRegisterCreateAccount.setOnClickListener { performRegister() }

        binding.tvToLogInCreateAccount.setOnClickListener { navigateToLogin() }
    }

    private fun setupTextWatchers() {
        binding.editTextCompleteNameCreateAccount.addTextChangedListener {
            if (binding.inputLayoutCompleteNameCreateAccount.error != null) {
                binding.inputLayoutCompleteNameCreateAccount.error = null
            }
        }

        binding.editTextEmailCreateAccount.addTextChangedListener {
            if (binding.inputLayoutEmailCreateAccount.error != null) {
                binding.inputLayoutEmailCreateAccount.error = null
            }
        }

        binding.editTextPasswordCreateAccount.addTextChangedListener { text ->
            if (binding.inputLayoutPasswordCreateAccount.error != null) {
                binding.inputLayoutPasswordCreateAccount.error = null
            }
            updatePasswordRequirementsIndicator(text.toString())
        }

        binding.editTextConfirmPasswordCreateAccount.addTextChangedListener {
            if (binding.inputLayoutConfirmPasswordCreateAccount.error != null) {
                binding.inputLayoutConfirmPasswordCreateAccount.error = null
            }
        }
    }

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

            else -> true
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

            else -> true

        }
    }

    /**
     * Solo valida que la contraseña no esté vacía.
     * Las reglas (longitud, mayúscula, número) las valida RegisterUsaCase
     * y el error llega como UiState.Error al observer.
     */
    private fun validatePasswordNotEmpty(password: String): Boolean {
        return if (password.isEmpty()) {
            binding.inputLayoutPasswordCreateAccount.error =
                getString(R.string.error_empty_password)
            binding.editTextPasswordCreateAccount.requestFocus()
            false
        } else true
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
                true
            }
        }
    }

    private fun validateTerms(): Boolean {
        return if (!binding.checkBoxTerms.isChecked) {
            showSnackbar(
                message = getString(R.string.error_terms_not_accepted),
                colorRes = R.color.orange
            )
            false

        } else true
    }

    private fun performRegister() {
        val fullName = binding.editTextCompleteNameCreateAccount.text.toString().trim()
        val email = binding.editTextEmailCreateAccount.text.toString().trim()
        val password = binding.editTextPasswordCreateAccount.text.toString()
        val confirmPassword = binding.editTextConfirmPasswordCreateAccount.text.toString()

        // Validaciones de formulario en el Fragment (campos vacíos, coincidencia)
        // Las reglas de negocio de la contraseña las valida RegisterUseCase
        if (!validateFullName(fullName)) return
        if (!validateEmail(email)) return
        if (!validatePasswordNotEmpty(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return
        if (!validateTerms()) return

        authViewModel.register(
            email = email,
            password = password,
            userName = fullName
        )
    }

    // Navegación
    private fun navigateToHome() {
        if (!isAdded || view == null) return
        try {
            findNavController().navigate(R.id.navigation_cart)
        } catch (e: Exception) {
            showSnackbar("Error de navegación $e")
        }
    }

    private fun navigateToLogin() {
        if (!isAdded || view == null) return
        try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            showSnackbar("Error de navegación $e")
        }
    }

    // UI helpers
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegisterCreateAccount.isEnabled = !isLoading
    }

    private fun showErrorBanner(message: String) {
        binding.tvErrorMessage.text = message
        binding.tvErrorMessage.visibility = View.VISIBLE
    }

    private fun hideErrorBanner() {
        binding.tvErrorMessage.visibility = View.GONE
    }

    private fun showSnackbar(message: String, colorRes: Int? = null) {
        if (_binding == null || !isAdded || view == null) return
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).apply {
            colorRes?.let { setBackgroundTint(resources.getColor(it, null)) }
            show()
        }
    }

    /**
     * Actualiza tvPasswordRequirements mientras el usuario escribe.
     *
     * Usa el TextView existente en el layout (tvPasswordRequirements)
     * que inicialmente muestra @string/password_requirements_short.
     * Lo reemplazamos dinámicamente con el estado de cada requisito.
     *
     * Diseño de la función:
     * Cada requisito tiene dos estados: pendiente (✗) y cumplido (✓).
     * Cuando todos se cumplen, el texto completo cambia a verde
     * para dar confirmación visual clara antes de continuar.
     *
     * ¿Por qué íconos de texto (✓/✗) y no ImageView?
     * El layout ya tiene tvPasswordRequirements como TextView.
     * Agregar ImageViews requeriría modificar el XML y el ConstraintLayout.
     * Con Spannable podemos colorear cada línea individualmente
     * sin tocar el layout — cambio de comportamiento sin cambio de diseño.
     *
     * ¿Por qué esta lógica vive en el Fragment y no en el ViewModel?
     * Porque es feedback visual puro de formulario — no es una regla
     * de negocio. La regla de negocio (contraseña válida = 8+ chars,
     * mayúscula, número) vive en RegisterUseCase. Este métodoo solo
     * muestra visualmente el progreso del usuario mientras tipea.
     */
    private fun updatePasswordRequirementsIndicator(password: String) {
        if (_binding == null) return

        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val allMet = hasMinLength && hasUppercase && hasDigit

        // Construir texto con estado de cada requisito
        val req1 = if (hasMinLength) "✓ 8 caracteres mínimo" else "✗ 8 caracteres mínimo"
        val req2 = if (hasUppercase) "✓ Una letra mayúscula" else "✗ Una letra mayúscula"
        val req3 = if (hasDigit)    "✓ Un número"           else "✗ Un número"

        val fullText = "$req1\n$req2\n$req3"

        // Construir Spannable para colorear cada línea individualmente
        val spannable = android.text.SpannableStringBuilder(fullText)

        // Colores
        val colorMet   = android.graphics.Color.parseColor("#4CAF50")  // verde Material
        val colorPending = android.graphics.Color.parseColor("#9E9E9E") // gris
        val colorAllMet  = android.graphics.Color.parseColor("#2E7D32") // verde oscuro

        if (allMet) {
            // Todos cumplidos — colorear todoo de verde oscuro
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(colorAllMet),
                0, fullText.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            // Colorear cada requisito individualmente
            applyColorToLine(spannable, req1, if (hasMinLength) colorMet else colorPending)
            applyColorToLine(spannable, req2, if (hasUppercase) colorMet else colorPending)
            applyColorToLine(spannable, req3, if (hasDigit)    colorMet else colorPending)
        }

        binding.tvPasswordRequirements.text = spannable

        // Ocultar el indicador si la contraseña está vacía (estado inicial)
        binding.tvPasswordRequirements.visibility =
            if (password.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Aplica un color a una línea específica dentro del SpannableStringBuilder.
     * Busca la línea por su texto exacto y aplica el color solo a ese segmento.
     */
    private fun applyColorToLine(
        spannable: android.text.SpannableStringBuilder,
        line: String,
        color: Int
    ) {
        val start = spannable.indexOf(line)
        if (start < 0) return
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(color),
            start,
            start + line.length,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}