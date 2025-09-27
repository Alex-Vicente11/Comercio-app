package com.oax.comercioapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.databinding.DialogAddProductBinding
import com.oax.comercioapp.databinding.FragmentHomeBinding
import com.oax.comercioapp.ui.adapters.ProductAdapter
import com.oax.comercioapp.ui.adapters.ProductEvents
import com.oax.comercioapp.utils.SessionManager

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!
  
  private lateinit var productAdapter: ProductAdapter
  private lateinit var homeViewModel: HomeViewModel
  private lateinit var cartViewModel: CartViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
    cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)

    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val root: View = binding.root

    setupRecyclerView()
    setupSessionObserver()
    observeViewModel()
    observeCartViewModel()
    setupClickListeners()
    
    return root
  }


  private fun setupSessionObserver() {
    SessionManager.currentUser.observe(viewLifecycleOwner) { user ->
      Log.d("HomeFragment", "Session changed: ${user?.userName}")
      updateHomeTitle(user?.userName)

      /*user?.let {
        cartViewModel.loadCartItems(it.idUser)
      }*/
    }
  }

  private fun updateHomeTitle(userName: String?) {
    val baseTitle = "Productos"
    if (userName != null) {
      binding.textHome.text = "$baseTitle\n Sesión: $userName"
    }else {
      binding.textHome.text = "$baseTitle\n Inicia sesión seleccionando un usuario"
    }
  }

  private fun setupRecyclerView() {
    productAdapter = ProductAdapter(object : ProductEvents {
      override fun increaseQuantity(product: Product, quantity: Int) {
        Log.i("DEBUG", "${product}  -> quantity ${quantity}")
        val currentUser = SessionManager.getCurrentUser()
        if (currentUser != null) {
          cartViewModel.handleIncreaseQuantity(currentUser.idUser, product, quantity)
        } else {
          Toast.makeText(context, "Inicia sesión para agregar productos al carrito", Toast.LENGTH_SHORT).show()
        }
      }

      override fun decreaseQuantity(product: Product, quantity: Int) {
        Log.i("DEBUG", "${product} -> decreasing to quantity ${quantity}")
        val currentUser = SessionManager.getCurrentUser()
        if (currentUser != null) {
          cartViewModel.handleDecreaseQuantity(currentUser.idUser, product, quantity)
        } else {
          Toast.makeText(context, "Inicia sesión para modificar el carrito", Toast.LENGTH_SHORT).show()
        }
      }

    })
    
    binding.recyclerViewProducts.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = productAdapter
    }
  }
  
  private fun observeViewModel() {
    homeViewModel.text.observe(viewLifecycleOwner) { vmText ->
      if (!SessionManager.isLoggedIn()) {
        binding.textHome.text = "$vmText\n Inicia sesión seleccionando un usuario"
      }
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


  private fun observeCartViewModel() {
    cartViewModel.addToCartResult.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Success -> {
          Toast.makeText(context, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Error -> {
          Toast.makeText(context, "Error al agregar al carrito: ${result.message}", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Loading -> {
          // Opcional: motrar loading
        }
      }
    }

    cartViewModel.updateCartResult.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Success -> {
          Toast.makeText(context, "Carrito actualizado", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Error -> {
          Toast.makeText(context, "Error al actualizar carrito: ${result.message}", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Loading -> {
          // Loading state
        }
      }
    }

    cartViewModel.removeFromCartResult.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Success -> {
          Toast.makeText(context, "Producto eliminado del carrito", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Error -> {
          Toast.makeText(context, "Error al eliminar del carrito: ${result.message}", Toast.LENGTH_SHORT).show()
        }
        is NetworkResult.Loading -> {
          // Loading state
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

  override fun onResume() {
    super.onResume()

    //
    val currentUser = SessionManager.getCurrentUser()
    updateHomeTitle(currentUser?.userName)
    Log.d("HomeFragment", "onResume - Current user: ${currentUser?.userName}")
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}