package com.oax.comercioapp.ui.cart

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
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.CartItemDetailed
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.databinding.DialogAddProductBinding
import com.oax.comercioapp.databinding.FragmentCartBinding
import com.oax.comercioapp.ui.adapters.ProductAdapter
import com.oax.comercioapp.ui.adapters.ProductEvents
import com.oax.comercioapp.utils.SessionManager


/**
 * CartFragment - Pantalla principal con lista de productos y carrito
 *
 * CAMBIOS vs versión anterior:
 * - Ya NO pasa userId manualmente a CartViewModel
 * - Usa CartItemDetailed en lugar de CartItem
 * - Observa cambios de sesión (guest y autenticado)
 * - Limpia carrito automáticamente al cerrar sesión
 */
class CartFragment : Fragment() {

  private var _binding: FragmentCartBinding? = null
  private val binding get() = _binding!!

  private lateinit var productAdapter: ProductAdapter
  private lateinit var cartProductsViewModel: CartProductsViewModel
  private lateinit var cartViewModel: CartViewModel

  private var cartQuantities = mutableMapOf<Int, Int>()

  // Variable para trackear el ultimo usuario cargado
  private var lastLoadedUserId: Int? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    cartProductsViewModel = ViewModelProvider(this).get(CartProductsViewModel::class.java)
    cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)

    _binding = FragmentCartBinding.inflate(inflater, container, false)
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
      Log.d("CartFragment", "Session changed: ${user?.userName} (Guest: ${user?.isGuest}")
      updateCartTitle(user?.userName, user?.isGuest ?: false)

      if (user != null && user.idUser > 0) {
        // solo recargar si cambio el usuario
        if (lastLoadedUserId != user.idUser){
          Log.d("CartFragment", "Usuario cambió de $lastLoadedUserId a ${user.idUser}, recargando carrito...")

          // Limpiar cantidades anteriores
          cartQuantities.clear()
          productAdapter.notifyDataSetChanged()

          lastLoadedUserId = user.idUser

          //cargar cantidades del nuevo usuario
          loadCartQuantities()
        }else {
          Log.d("CartFragment", "Mismo usuario, no se recarga carrito")
        }
      } else {
        //Usuario cerro sesion
        lastLoadedUserId = null
        clearCartQuantities()
      }
    }
  }

  /**
   * Carga las cantidades del carrito
   *
   * ACTUALIZADO: Ya NO requiere userId (se obtiene del token)
   */
  private fun loadCartQuantities() {
    if (UserPreferences.isLoggedIn()) {
      cartViewModel.loadCartItems()
    } else {
      clearCartQuantities()
    }
  }

  private fun clearCartQuantities() {
    Log.d("CartFragment", "Limpiando cantidades del carrito")
    cartQuantities.clear()
    productAdapter.notifyDataSetChanged() // refrescar vista
  }

  private fun updateCartTitle(userName: String?, isGuest: Boolean) {
    val baseTitle = "Cart"
    when {
      userName != null && isGuest -> {
        binding.textCart.text = "$baseTitle\n Modo Invitado"
      }
      userName != null -> {
        binding.textCart.text = "$baseTitle\n Sesión: $userName"
      }
      else -> {
        binding.textCart.text = "$baseTitle\n Inicia sesión para agregar al carrito"
      }
    }
  }

  private fun setupRecyclerView() {
    productAdapter = ProductAdapter(
      onProductEvent = object : ProductEvents {
        override fun increaseQuantity(product: Product, quantity: Int) {
          if (UserPreferences.isLoggedIn()) {
            cartViewModel.handleIncreaseQuantity(product, quantity)
          } else {
            Toast.makeText(context, "Inicia sesión para agregar productos al carrito", Toast.LENGTH_SHORT).show()
          }
        }

        override fun decreaseQuantity(product: Product, quantity: Int) {
          if (UserPreferences.isLoggedIn()) {
            cartViewModel.handleDecreaseQuantity(
              UserPreferences.getUserId(),  // Para compatibilidad con la firma del metodo
              product,
              quantity
            )
          } else {
            Toast.makeText(context, "Inicia sesión para modificar el carrito", Toast.LENGTH_SHORT).show()
          }
        }

        override fun removeQuantities(product: Product) {
          if (UserPreferences.isLoggedIn()) {
            val currentQty = cartQuantities[product.idProduct] ?: 0
            if (currentQty > 0) {
              // Mostrar dialogo de confirmacion
              AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cantidad total del carrito")
                .setMessage("¿Desea elimnar las cantidades de ${product.product} del carrito?")
                .setPositiveButton("Eliminar") { _, _ ->
                  cartViewModel.clearQuantitiesFromCart(product)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            } else {
              Toast.makeText(context, "Este producto no está en el carrito", Toast.LENGTH_SHORT).show()
            }
          }else {
            Toast.makeText(context, "Inicia sesión para eliminar del carrito", Toast.LENGTH_SHORT).show()
          }

        }
      },
      // Callback para obtener cantidades
      getCurrentCartQuantity = { product ->
        cartQuantities[product.idProduct] ?: 0
      }
    )

    binding.recyclerViewProducts.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = productAdapter
    }
  }

  private fun observeViewModel() {
    cartProductsViewModel.text.observe(viewLifecycleOwner) { vmText ->
      if (!SessionManager.isLoggedIn()) {
        binding.textCart.text = "$vmText\n Inicia sesión para usar el carrito"
      }
    }

    cartProductsViewModel.products.observe(viewLifecycleOwner) { result ->
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


  /**
   * Observa los cambios del CartViewModel
   *
   * ACTUALIZADO: Usa CartItemDetailed en lugar de CartItem
   */
  private fun observeCartViewModel() {
    // Observer para items del carrito
    cartViewModel.cartItems.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Success -> {
          updateCartQuantities(result.data)
        }
        is NetworkResult.Error -> {
          // Limpiar cantidades en caso de error
          cartQuantities.clear()
          productAdapter.notifyDataSetChanged()

          // Si es 401, la sesion ya fue cerrada por SessionManager
          if (result.code != 401) {
            Log.e("CartFragment", "Error al cargar carrito: ${result.message}")
          }
        }
        is NetworkResult.Loading -> {
          // No hacer nada durante la carga
        }
      }
    }

    cartViewModel.addToCartResult.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Success -> {
          Toast.makeText(context, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()

          // Recargar cantidades después de agregar
          loadCartQuantities()
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
          // Recargar carrito despues de actualizar
          loadCartQuantities()
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
          loadCartQuantities()
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

  /**
   * Actualiza las cantidades del carrito en la UI
   *
   * ACTUALIZADO: Recibe CartItemDetailed en lugar de CartItem
   */
  private fun updateCartQuantities(cartItems: List<CartItemDetailed>) {
    Log.d("CartFragment", "Actualizando cantidades del carrito...")
    // Limpiar cantidades anteriores
    cartQuantities.clear()

    // Actualizar con cantidades del carrito
    cartItems.forEach { cartItem ->
      cartQuantities[cartItem.product.idProduct] = cartItem.quantity
    }

    // Notificar al adapter para actualizar la vista
    productAdapter.notifyDataSetChanged()

    Log.d("CartFragment", "Vista actualizada con ${cartQuantities.size} productos en carrito")
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
        cartProductsViewModel.createProduct(productRequest)

        // Observar resultado de creacion
        cartProductsViewModel.createProductResult.observe(viewLifecycleOwner) { result ->
          when (result) {
            is NetworkResult.Success -> {
              dialog.dismiss()
              Toast.makeText(
                    context,
                    "Producto agregado exitosamente",
                    Toast.LENGTH_SHORT
              ).show()
              cartProductsViewModel.refreshProducts()
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
    updateCartTitle(currentUser?.userName, currentUser?.isGuest ?: false)
    Log.d("CartFragment", "onResume - Current user: ${currentUser?.userName} (Guest: ${currentUser?.isGuest}")

    if (currentUser == null || currentUser.idUser <= 0) {
      Log.d("CartFragment", "No hay usuario valido en onResume, limpiando carrito")
      lastLoadedUserId = null
      clearCartQuantities()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
