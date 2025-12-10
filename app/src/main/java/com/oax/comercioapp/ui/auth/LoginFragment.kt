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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentLoginBinding

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
        Log.d(TAG, "================================================")
        Log.d(TAG, "onCreateView() - Iniciando LoginFragment")

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        Log.d(TAG, "ViewBinding creado exitosamente")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG,"onViewCreated() - Configurando fragment")

        setupObservers()
        setupClickListeners()
        setupTextWatchers()

        //setupTestingButtons()
        Log.d(TAG, "Fragment configurado completamente")
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
        Log.d(TAG, "setupObservers() - Configurando observadores")

        authViewModel.loginState.observe(viewLifecycleOwner) { result ->
            Log.d(TAG, "-----------------------------------------")
            Log.d(TAG, "Observer loginState disparado")
            Log.d(TAG, "Tipo de resultado: ${result.javaClass.simpleName}")
            when (result) {
                is NetworkResult.Loading -> {
                    Log.i(TAG, "Estado: LOADING")
                    showLoading(true)
                }

                is NetworkResult.Success -> {
                    Log.i(TAG, "Estado: SUCCESS")
                    Log.d(TAG, "Datos recibidos: ${result.data}")

                    showLoading(false)
                    hideError()

                    showSafeSnackbar(
                        message = getString(R.string.success_login),
                        duration = Snackbar.LENGTH_SHORT,
                        backgroundColor = R.color.green
                    )
                    Log.d(TAG, "Snackbar de éxito mostrado")

                    // Navegar con pequeño delay para que se vea el Snackbar
                    view?.postDelayed({
                        navigateToHome()
                    }, 500)

                }

                is NetworkResult.Error -> {
                    Log.e(TAG, "Estado: ERROR")
                    Log.e(TAG, "Mensaje de error: ${result.message}")
                    Log.e(TAG, "Código de error: ${result.code}")

                    showLoading(false)
                    showError(result.message)
                }
            }
            Log.d(TAG, "----------------------------------")
        }

        authViewModel.cartMergeInfo.observe(viewLifecycleOwner) { (merged, count) ->
            Log.d(TAG, "Observer cartMergeInfo disparado")
            Log.d(TAG, "Carrito fusionado: $merged, Items: $count")

            if (merged && count > 0) {
                val message = "Carrito fusionado: $count productos agregados"
                showSafeSnackbar(
                    message = message,
                    duration = Snackbar.LENGTH_SHORT,
                    backgroundColor = R.color.green
                )

                Log.i(TAG, "Snackbar de carrito fusionado mostrado")
            }
        }
        Log.d(TAG, "Observadores configurados")
    }

    private fun setupClickListeners() {
        Log.d(TAG, "setupClickListeners() - Configurado listeners")
        binding.buttonLogin.setOnClickListener {
            Log.d(TAG, "----------------")
            Log.d(TAG, "CLICK EN BOTÓN LOGIN")
            Log.d(TAG, "-------------------")
            handleLoginClick()
        }

        binding.buttonGuest.setOnClickListener {
            Log.d(TAG, "-------------------------")
            Log.d(TAG, "CLICK EN BOTÓN GUEST")
            Log.d(TAG, "-------------------------")
            navigateToHome()
        }

        binding.tvToRegister.setOnClickListener {
            Log.d(TAG, "-------------------------")
            Log.d(TAG, "CLICK EN LINK REGISTRARSE")
            Log.d(TAG, "---------------------------")
            navigateToRegister()
        }

        Log.d(TAG, "Click listeners configurados")
    }

    private fun setupTextWatchers() {
        Log.d(TAG, "setupTextWatchers() - Configurando watchers")

        binding.textEditEmail.addTextChangedListener {
            Log.v(TAG, "Email cambió: ${it?.toString()?.take(3)}")
            clearEmailError()
        }

        binding.textEditPassword.addTextChangedListener {
            Log.v(TAG, "Password cambió (${it?.length} caracteres)")
            clearPasswordError()
        }
    }

    private fun handleLoginClick() {
        Log.i(TAG, "handleLoginClick() - Iniciando proceso de login")

        clearAllErrors()

        val email = binding.textEditEmail.text.toString().trim()
        val password = binding.textEditPassword.text.toString()

        Log.d(TAG, "Email extraido '$email'")
        Log.d(TAG, "Password extraído: ${password.length} caracteres")

        if (!validateEmail(email)){
            Log.w(TAG, "Validación de email FALLÓ - deteniendo proceso")
            return
        }
        if (!validatePassword(password)) {
            Log.w(TAG, "Validación de password FALLÓ - deteniendo proceso")
            return
        }

        Log.i(TAG, "Validaciones locales EXITOSAS")
        Log.i(TAG, "Llamando authViewModel.login()")

        authViewModel.login(email, password)
    }

    private fun validateEmail(email: String): Boolean {
        Log.d(TAG, "validateEmail() - Validando: '$email'")
        return when {
            email.isEmpty() -> {
                Log.w(TAG, "Email está VACÍO")
                binding.textInputLayoutEmail.error = getString(R.string.error_empty_email)
                binding.textEditEmail.requestFocus()
                Log.d(TAG, "Focus movido a campo email")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Log.w(TAG, "Email con FORMATO INVÁLIDO")
                binding.textInputLayoutEmail.error = getString(R.string.error_invalid_email)
                binding.textEditEmail.requestFocus()
                Log.d(TAG, "Focus movido a campo Email")
                false
            }
            else -> {
                Log.i(TAG, "Email VÁLIDO")
                binding.textInputLayoutEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        Log.d(TAG, "validatePassword() - Validando password de ${password.length} caracteres")
        return when {
            password.isEmpty() -> {
                Log.w(TAG, "Password está VACÍO")
                binding.textInputLayoutPassword.error = getString(R.string.error_empty_password)
                binding.textEditPassword.requestFocus()
                Log.d(TAG, "Focus movido a campo Password")
                false
            }
            else -> {
                Log.i(TAG, "Password NO vacío (validación local OK)")
                binding.textInputLayoutPassword.error = null
                true
            }
        }
    }

    private fun navigateToHome() {
        Log.i(TAG, "navigateToHome() - Navegando a Home")

        // verificación de seguridad
        if (!isAdded) {
            Log.w(TAG, "Fragment No está attached - abortando navegación")
            return
        }

        if (view == null) {
            Log.w(TAG, "View es NULL - abortando navegación")
            return
        }

        try {
            findNavController().navigate(R.id.navigation_home)
            Log.d(TAG, "Navegación a Home exitosa")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a Home: ${e.message}", e)
            Snackbar.make(binding.root, "Error de navegación", Snackbar.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Destino no encontrado: ${e.message}", e)
            showSafeSnackbar("Error: Destino no encontrado")
        } catch (e: IllegalStateException) {
            Log.d(TAG, "NavController no disponible: ${e.message}", e)
            showSafeSnackbar("Error de navegación")
        }
    }

    private fun navigateToRegister() {
        Log.i(TAG, "navigateToRegister() - Navegando a Register")

        if (!isAdded || view == null) {
            Log.w(TAG, "Fragment no esta listo - abortando navegación ")
            return
        }

        try {
            findNavController().navigate(R.id.registerFragment)
            Log.d(TAG, "Navegación a Register exitosa")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a Register: ${e.message}", e)
            Snackbar.make(binding.root, "Error de navegación", Snackbar.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Destino no encontrado: ${e.message}", e)
            showSafeSnackbar("Error: Destino no encontrado")
        } catch (e: java.lang.IllegalStateException) {
            Log.e(TAG, "NavController no disponible: ${e.message}", e)
            showSafeSnackbar("Error de navegación")
        }
    }

    // MÉTODOS AUXILIARES

    private fun showSafeSnackbar(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        backgroundColor: Int? = null
    ) {
        try {
            // Verificar que el Fragment está en estado válido
            if (_binding != null && isAdded && view != null) {
                val snackbar = Snackbar.make(binding.root, message, duration)

                // Aplicar color personalizado si se proporciona
                backgroundColor?.let { colorRes ->
                    snackbar.setBackgroundTint(resources.getColor(colorRes, null))
                }

                snackbar.show()
                Log.d(TAG, "Snackbar mostrado: '$message' (duration: $duration)")
            } else {
                Log.w(TAG, "No se pudo mostrar Snackbar - Fragment no está listo")
                Log.w(TAG, "- _binding null: ${_binding == null}")
                Log.w(TAG, "- isAdded: $isAdded")
                Log.w(TAG, "- view null: ${view == null}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando Snackbar: ${e.message}", e)
        }
    }
    private fun clearAllErrors() {
        Log.v(TAG, "clearAllErrors() - Limpiando todos los errores")
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null
        hideError()
    }

    private fun clearEmailError() {
        Log.v(TAG, "clearEmailError() - Limpiando error de Email")
        binding.textInputLayoutEmail.error = null
        hideError()
    }

    private fun clearPasswordError() {
        Log.v(TAG, "clearPasswordError() - Limpiando error de password")
        binding.textInputLayoutPassword.error = null
        hideError()
    }

    private fun showLoading(isLoading: Boolean) {
        Log.d(TAG, "----------------------------------")
        Log.d(TAG, "showLoading($isLoading)")

        Log.v(TAG, "ProgressBar: ${if (isLoading) "VISIBLE" else "GONE"}")
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        Log.v(TAG, "Botones: ${if (isLoading) "DESHABILITADOS" else "HABILITADOS"}")
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGuest.isEnabled = !isLoading

        Log.v(TAG, "Campos: ${if (isLoading) "DESHABILITADOS" else "HABILITADOS"}")
        binding.textEditEmail.isEnabled = !isLoading
        binding.textEditPassword.isEnabled = !isLoading

        Log.d(TAG, "Estado de loading actualizado")
        Log.d(TAG, "----------------------------------")
    }

    private fun showError(message: String) {
        Log.e(TAG, "showError() - Mostrando error: '$message'")

        binding.tvErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }

        Log.d(TAG, "Banner de error mostrado")
    }

    private fun hideError() {
        Log.v(TAG, "hideError() - Ocultando banner de error")
        binding.tvErrorMessage.visibility = View.GONE
    }


    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() - Limpiando ViewBinding")
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "ViewBinding liberando (prevención de memory leak)")
    }
}