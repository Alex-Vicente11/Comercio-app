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
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentDashboardBinding
import com.oax.comercioapp.ui.adapters.UserAdapter

class DashboardFragment : Fragment() {

  private var _binding: FragmentDashboardBinding? = null
  private val binding get() = _binding!!
  
  private lateinit var userAdapter: UserAdapter
  private lateinit var dashboardViewModel: DashboardViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

    _binding = FragmentDashboardBinding.inflate(inflater, container, false)
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
          dashboardViewModel.createUser(userName)
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
    dashboardViewModel.text.observe(viewLifecycleOwner) {
      binding.textDashboard.text = it
    }
    
    dashboardViewModel.users.observe(viewLifecycleOwner) { result ->
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
    dashboardViewModel.createUserResult.observe(viewLifecycleOwner) { result ->
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
          dashboardViewModel.clearCreateUserResult()
        }
        is NetworkResult.Error -> {
          Toast.makeText(
            requireContext(),
            "Error: ${result.message}",
            Toast.LENGTH_LONG
          ).show()
          dashboardViewModel.clearCreateUserResult()
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