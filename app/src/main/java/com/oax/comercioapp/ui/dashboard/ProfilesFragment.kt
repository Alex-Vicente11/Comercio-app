package com.oax.comercioapp.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.R
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentDashboardBinding
import com.oax.comercioapp.ui.adapters.UserAdapter

class ProfilesFragment : Fragment() {

  private var _binding: FragmentDashboardBinding? = null
  private val binding get() = _binding!!
  
  private lateinit var userAdapter: UserAdapter
  private lateinit var profilesViewModel: ProfilesViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    profilesViewModel = ViewModelProvider(this).get(ProfilesViewModel::class.java)

    _binding = FragmentDashboardBinding.inflate(inflater)
    val root: View = binding.root

    setupRecyclerView()
    setupClickListeners()
    observeViewModel()
    
    return root
  }
  
  private fun setupRecyclerView() {
    userAdapter = UserAdapter { user ->
      Toast.makeText(
        context, 
        "Usuario seleccionado: ${user.userName}", 
        Toast.LENGTH_SHORT
      ).show()

      val navController = findNavController()
      navController.navigate(
        R.id.action_navigation_profile_to_profileFragment,
        args = Bundle().apply {
          putString("profile_id", user.idUser.toString())
        })
    }
    
    binding.recyclerViewUsers.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = userAdapter
    }
  }

  private fun setupClickListeners() {
    binding.fabAddUser.setOnClickListener {
      showAddUserDialog()
    }
  }

  private fun showAddUserDialog() {
    val editText = EditText(requireContext()).apply {
      hint = "Ingresa el nombre del usuario"
      setPadding(50, 40, 50, 40)
    }

    AlertDialog.Builder(requireContext())
      .setTitle("Nuevo usuario")
      .setMessage("Ingresa los datos del nuevo usuario")
      .setView(editText)
      .setPositiveButton("Crear") { _, _ ->
        val userName = editText.text.toString()
        if (userName.isNotBlank()) {
          profilesViewModel.createUser(userName)
        }else {
          Toast.makeText(
            requireContext(),
            "El nombre no puede estar vacío",
            Toast.LENGTH_SHORT
          ).show()
        }
      }
      .setNegativeButton("Cancelar", null)
      .show()
  }
  
  private fun observeViewModel() {
    profilesViewModel.text.observe(viewLifecycleOwner) {
      binding.textDashboard.text = it
    }

    profilesViewModel.users.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          binding.progressBar.visibility = View.VISIBLE
          binding.textError.visibility = View.GONE
          binding.recyclerViewUsers.visibility = View.GONE
        }
        is NetworkResult.Success -> {
          binding.progressBar.visibility = View.GONE
          binding.textError.visibility = View.GONE
          binding.recyclerViewUsers.visibility = View.VISIBLE
          userAdapter.submitList(result.data)
        }
        is NetworkResult.Error -> {
          binding.progressBar.visibility = View.GONE
          binding.textError.visibility = View.VISIBLE
          binding.recyclerViewUsers.visibility = View.GONE
          binding.textError.text = "Error: ${result.message}"
        }
      }
    }

    // Observer para el resultado de crear usuario
    profilesViewModel.createUserResult.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          // Se puede mostrar un ProgressDialog aqui
        }
        is NetworkResult.Success -> {
          Toast.makeText(
            requireContext(),
            "Usuario creado exitosamente",
            Toast.LENGTH_SHORT
          ).show()
          profilesViewModel.clearCreateUserResult()
        }
        is NetworkResult.Error -> {
          Toast.makeText(
            requireContext(),
            "Error: ${result.message}",
            Toast.LENGTH_LONG
          ).show()
          profilesViewModel.clearCreateUserResult()
        }
        null -> {
          // No hacer nada cuando es null
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}