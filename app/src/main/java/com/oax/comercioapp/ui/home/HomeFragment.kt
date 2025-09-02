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
import com.oax.comercioapp.data.models.ProductResponse
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
    setupClickListeners()
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

    // Observer para el resultado de crear producto
    homeViewModel.createProductResult.observe(viewLifecycleOwner) { result ->
      // Este observer se maneja globalmente para evitar multiples registros
      handleCreateProductResult(result)
    }
  }

  private fun setupClickListeners(){
    binding.fabAddProduct.setOnClickListener {
      showAddProductDialog()
    }
  }

  private var currentDialog: AlertDialog?= null
  private var currentDialogBinding: DialogAddProductBinding? = null

  private fun showAddProductDialog(){
    currentDialogBinding = DialogAddProductBinding.inflate(layoutInflater)

    currentDialog = AlertDialog.Builder(requireContext())
        .setView(currentDialogBinding!!.root)
        .setCancelable(false)
        .create()

    currentDialogBinding!!.buttonCancel.setOnClickListener {
      currentDialog?.dismiss()
      currentDialog = null
      currentDialogBinding = null
    }

    currentDialogBinding!!.buttonSave.setOnClickListener {
      val productName = currentDialogBinding!!.editTextProductName.text.toString().trim()
      val priceText = currentDialogBinding!!.editTextProductPrice.text.toString().trim()

      if (validateInput(productName, priceText)){
        val price = priceText.toDouble()
        val productRequest = ProductRequest(productName, price)

        // Mostrar loading en el dialogo
        currentDialogBinding!!.progressBarDialog.visibility = View.VISIBLE
        currentDialogBinding!!.buttonSave.isEnabled = false
        currentDialogBinding!!.buttonCancel.isEnabled = false

        // Crear producto
        homeViewModel.createProduct(productRequest)


      }
    }
    currentDialog?.show()
  }

  private fun handleCreateProductResult(result: NetworkResult<ProductResponse>) {
          when (result) {
              is NetworkResult.Success -> {
                  currentDialog?.dismiss()
                  currentDialog = null
                  currentDialogBinding = null
                  Toast.makeText(
                      context,
                      "Producto agregado exitosamente",
                      Toast.LENGTH_SHORT
                  ).show()
                  homeViewModel.refreshProducts()
              }

              is NetworkResult.Error -> {
                  //Re-habilitar botones en caso de error
                  currentDialogBinding?.let { binding ->
                    binding.progressBarDialog.visibility = View.GONE
                    binding.buttonSave.isEnabled = true
                    binding.buttonCancel.isEnabled = true
                  }
                  Toast.makeText(
                      context,
                      "Error: ${result.message}",
                      Toast.LENGTH_LONG
                  ).show()
              }
              is NetworkResult.Loading -> {
                  //Estado manejado en showAddProductDialog()
              }
          }
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
    currentDialog?.dismiss()
    currentDialog = null
    currentDialogBinding = null
    _binding = null
  }
}