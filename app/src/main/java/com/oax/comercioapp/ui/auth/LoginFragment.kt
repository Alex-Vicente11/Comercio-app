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