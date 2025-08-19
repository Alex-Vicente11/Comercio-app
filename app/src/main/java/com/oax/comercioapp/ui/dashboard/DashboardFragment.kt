package com.oax.comercioapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}