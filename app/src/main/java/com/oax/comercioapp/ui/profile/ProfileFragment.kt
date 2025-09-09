package com.oax.comercioapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.cast.framework.SessionManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.databinding.FragmentProfileBinding
import java.lang.NumberFormatException

class ProfileFragment : Fragment() {

  companion object {
    fun newInstance() = ProfileFragment()
  }

  private var profileId: String? = null
  private lateinit var binding: FragmentProfileBinding

  private val viewModel: ProfileViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    profileId = arguments?.getString("profile_id") ?: "NO HAY PROFILE ID"

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentProfileBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupObservers()
    loadUserProfile()
  }

  private fun setupObservers() {
    viewModel.user.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          binding.profileId.text = "Cargando usuario..."
        }
        is NetworkResult.Success -> {

          viewModel.setCurrentUser(result.data)
        }
        is NetworkResult.Error -> {
          binding.profileId.text = "Error: ${result.message}\nID recibido: $profileId"
        }
      }
    }

    viewModel.currentUser.observe(viewLifecycleOwner) { user ->
      updateUserUI(user)

    }
  }

  private fun updateUserUI(user: User?) {
    user?.let {
      binding.profileId.text = "Perfil de:${it.userName}\nID: ${it.idUser}"
      Toast.makeText(
        requireContext(),
        "¡Sesión iniciada para: ${it.userName}",
        Toast.LENGTH_LONG
      ).show()
    }?: run {
      binding.profileId.text = "No hay sesión activa"
    }
  }

  private fun loadUserProfile() {
    profileId?.let { id ->
      if (id != "No hay profile id") {
        try {
          val userId = id.toInt()
          viewModel.loadUser(userId)
        }catch (e: NumberFormatException) {
          binding.profileId.text = "Error: ID de usuario inválido ($id)"
        }
      }else {
        binding.profileId.text = "Error: No se recibió ID de usuario válido"
      }
    }
  }

  fun getCurrentUser() = viewModel.getCurrentUser()

  fun isUserLoggedIn() = viewModel.isUserLoggedIn()
}