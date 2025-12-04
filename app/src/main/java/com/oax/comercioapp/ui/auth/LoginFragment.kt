package com.oax.comercioapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        setupTestingButtons()
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

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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