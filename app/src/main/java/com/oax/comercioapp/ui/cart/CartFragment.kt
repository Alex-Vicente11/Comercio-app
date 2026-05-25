package com.oax.comercioapp.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.oax.comercioapp.databinding.FragmentCartBinding
import com.oax.comercioapp.domain.model.Product
import com.oax.comercioapp.ui.UiState
import com.oax.comercioapp.ui.adapters.ProductAdapter
import com.oax.comercioapp.ui.adapters.ProductEvents
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 *
 * 1- ELIMINADO: SessionManager.currentUser.observe()
 *    El original observada SessionManager para saber quién está logueado y recargar el carrito
 *    cuando cambiaba el usuario.
 *    Ahora: ProductCatalogViewModel carga los productos en su init {}.
 *    Cuando el usuario hace login/logout, el Fragment recibe el evento via authViewModel.sessionState
 *    y reacciona limpiando o cargando. Un solo flujo de datos, no dos sistemas paralelos.
 *
 * 2. ELIMINADO: showAddProductDialog() y createProduct()
 *    Era la funcionalidad admin de crear productos desde el cliente. Con el nuevo enfoque de app
 *    de pedidos, el FAB puede reutilizarse para "Ver resumen del carrito" o eliminarse del layout.
 *
 * 3. ELIMINADO: cartQuantities como mutableMapOf local
 *    Antes: var cartQuantities = mutableMapOf<Int, Int>() en el Fragment
 *    Ahora: ProductCatalogViewModel.cartQuantities es el StateFlow que el Fragment observa.
 *    Una fuente de verdad, no una copia local.
 *
 * 4. ELIMINADO: lastLoadedUserId tracking manual
 *    El Fragment rastreaba manualmente el último userId para evitar recargas duplicadas.
 *    Con StateFlow y el ciclo de vida correcto, esto es innecesario - el ViewModel no re-emite
 *    si el estado cambia.
 *
 * 5. NetworkResult.Loading/Success/Error -> UiState con collet
 *
 * 6. COORDINACIÓN entre ProductCatalogViewModel y CartViewModel:
 *    - ProductCatalogViewModel -> catálogo + cantidades locales + add/update/remove
 *    - CartViewModel -> conteo del badge + remove confirmado
 *    El Fragment coordina ambos: cuando removeQuantities confirma en CartViewModel, notifica
 *    a ProductCatalogViewModel para limpiar la cantidad local.
 */

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var catalogViewModel: ProductCatalogViewModel
    private lateinit var cartViewModel: CartViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        catalogViewModel = ViewModelProvider(this)[ProductCatalogViewModel::class.java]
        cartViewModel = ViewModelProvider(this)[CartViewModel::class.java]

        _binding = FragmentCartBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        return binding.root
    }


    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductEvent = object : ProductEvents {

                override fun increaseQuantity(product: Product, quantity: Int) {
                    /**
                     * ProductCatalogViewModel maneja add/update internamente. Ya no necesita checkProductInCart()
                     * - usa CartQuantities local para saber si el producto ya está en el carrito.
                     */
                    catalogViewModel.increaseQuantity(product.id, quantity)
                }

                override fun decreaseQuantity(product: Product, quantity: Int) {
                    catalogViewModel.decreaseQuantity(product.id, quantity)
                }

                override fun removeQuantities(product: Product) {
                    val currentQty = catalogViewModel.getCurrentQuantity(product.id)
                    if (currentQty > 0) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar del carrito")
                            .setMessage("¿Elimininar ${product.name} del carrito?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                // CartViewModel confirma la eliminación en el servidor
                                cartViewModel.removeFromCart(product.id)
                                // ProductCatalogViewModel limpia la cantidad local
                                catalogViewModel.removeProduct(product.id)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        Toast.makeText(
                            context,
                            "Este producto no está en el carrito",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            // Lambda que el adapter llama para obtener la cantidad actual
            // La fuente de verdad es el StateFlow de ProductCatalogViewModel
            getCurrentCartQuantity = { product ->
                catalogViewModel.getCurrentQuantity(product.id)
            }

        )

        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Catálogo de productos
                launch {
                    catalogViewModel.productsState.collect { state ->
                        when (state) {
                            is UiState.Loading -> showLoading()

                            is UiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textError.visibility = View.GONE
                                binding.recyclerViewProducts.visibility = View.VISIBLE
                                productAdapter.submitList(state.data)
                            }

                            is UiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textError.visibility = View.VISIBLE
                                binding.recyclerViewProducts.visibility = View.GONE
                                binding.textError.text = state.message
                            }

                            is UiState.Idle -> Unit
                        }
                    }
                }

                /**
                 * Observa el mapa de cantidades del carrito.
                 * Cuando cambia (add/update/remove), notifica al adapter para que actualice
                 * solo los items afectados via DiffUtil.
                 *
                 * notifyDataSetChanged() fue reemplazado - es el métodoo más costoso porque redibuja
                 * toda la lista completa. submitList() con la misma lista deja que DiffUtil calcule
                 * exactamente qué items cambiaron y actualiza solo esos.
                 */
                launch {
                    catalogViewModel.cartQuantities.collect {
                        productAdapter.notifyDataSetChanged()
                        // Mejorar: usar DiffUtil payload para actualizar
                    }
                }

                // Errores de operaciones del carrito
                launch {
                    catalogViewModel.cartActionState.collect { state ->
                        if (state is UiState.Error) {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            catalogViewModel.onCartActionHandled()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textError.visibility = View.GONE
        binding.recyclerViewProducts.visibility = View.GONE
    }

    private fun setupClickListeners() {
        /**
         * ELIMINADO: showAddProductDialgog() - funcionalidad admin
         *
         * TODoo: Reutilizar el FAB para "Ver resumen del carrito"
         * que navega a una pantalla de confirmación del pedido.
         * Por ahora se oculta que exista esa pantalla.
         */
        binding.fabAddProduct.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
