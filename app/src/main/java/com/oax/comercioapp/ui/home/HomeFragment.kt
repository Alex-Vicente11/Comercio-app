package com.oax.comercioapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.databinding.DialogAddProductBinding
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
    setupClickListeners()
    
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

  private fun setupClickListeners(){
    binding.fabAddProduct.setOnClickListener {
      showAddProductDialog()
    }
  }

  private fun showAddProductDialog(){
    val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)

    val dialog = AlertDialog.Builder(requireContext())
        .setView(dialogBinding.root)
        .setCancelable(false)
        .create()

    dialogBinding.buttonCancel.setOnClickListener {
      dialog.dismiss()
    }

    dialogBinding.buttonSave.setOnClickListener {
      val productName = dialogBinding.editTextProductName.text.toString().trim()
      val priceText = dialogBinding.editTextProductPrice.text.toString().trim()

      if (validateInput(productName, priceText)){
        val price = priceText.toDouble()
        val productRequest = ProductRequest(productName, price)

        // Mostrar loading en el dialogo
        dialogBinding.progressBarDialog.visibility = View.VISIBLE
        dialogBinding.buttonSave.isEnabled = false
        dialogBinding.buttonCancel.isEnabled = false

        // Crear producto
        homeViewModel.createProduct(productRequest)

        // Observar resultado de creacion
        homeViewModel.createProductResult.observe(viewLifecycleOwner) { result ->
          when (result) {
            is NetworkResult.Success -> {
              dialog.dismiss()
              Toast.makeText(
                    context,
                    "Producto agregado exitosamente",
                    Toast.LENGTH_SHORT
              ).show()
              homeViewModel.refreshProducts()
            }
            is NetworkResult.Error -> {
                dialogBinding.progressBarDialog.visibility = View.GONE
                dialogBinding.buttonSave.isEnabled = true
                dialogBinding.buttonCancel.isEnabled = true
              Toast.makeText(
                  context,
                  "Error: ${result.message}",
                  Toast.LENGTH_LONG
              ).show()
            }
            is NetworkResult.Loading -> {
              // Ya manejado arriba
            }
          }

        }
      }
    }
    dialog.show()
  }

  private fun validateInput(name: String, price: String): Boolean {
    if (name.isEmpty()){
      Toast.makeText(context, "Por favor ingresa el nombre del producto", Toast.LENGTH_SHORT).show()
      return false
    }

    if (price.isEmpty()){
      Toast.makeText(context, "Por favor ingresa el precio", Toast.LENGTH_SHORT).show()
      return false
    }

    try {
        val priceValue = price.toDouble()
      if (priceValue <= 0){
        Toast.makeText(context, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show()
        return false
      }
    }catch (e: NumberFormatException){
      Toast.makeText(context, "Por favor ingresa un precio válido", Toast.LENGTH_SHORT).show()
      return false
    }
    return true
  }



  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}