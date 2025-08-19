package com.oax.comercioapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.databinding.FragmentHomeBinding
import com.oax.comercioapp.ui.adapters.ProductAdapter

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!
  
  private lateinit var productAdapter: ProductAdapter
  private lateinit var homeViewModel: HomeViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val root: View = binding.root

    setupRecyclerView()
    observeViewModel()
    
    return root
  }
  
  private fun setupRecyclerView() {
    productAdapter = ProductAdapter { product ->
      Toast.makeText(
        context, 
        "Producto seleccionado: ${product.product}", 
        Toast.LENGTH_SHORT
      ).show()
    }
    
    binding.recyclerViewProducts.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = productAdapter
    }
  }
  
  private fun observeViewModel() {
    homeViewModel.text.observe(viewLifecycleOwner) {
      binding.textHome.text = it
    }
    
    homeViewModel.products.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          binding.progressBar.visibility = View.VISIBLE
          binding.textError.visibility = View.GONE
          binding.recyclerViewProducts.visibility = View.GONE
        }
        is NetworkResult.Success -> {
          binding.progressBar.visibility = View.GONE
          binding.textError.visibility = View.GONE
          binding.recyclerViewProducts.visibility = View.VISIBLE
          productAdapter.submitList(result.data)
        }
        is NetworkResult.Error -> {
          binding.progressBar.visibility = View.GONE
          binding.textError.visibility = View.VISIBLE
          binding.recyclerViewProducts.visibility = View.GONE
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