package com.oax.comercioapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.domain.model.CartItem
import com.oax.comercioapp.domain.model.Product
import com.oax.comercioapp.domain.usecase.AddToCartUseCase
import com.oax.comercioapp.domain.usecase.GetProductsUseCase
import com.oax.comercioapp.domain.usecase.RemoveFromCartUseCase
import com.oax.comercioapp.domain.usecase.UpdateCartItemUseCase
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CartProductsViewModel -> ProductCatalogViewModel
 *
 * CAMBIO DE NOMBRE Y PROPÓSITO:
 * Con el nuevo enfoque de app de pedidos:
 *   - ProductCatalogViewModel -> muestra el catálogo, permite agregar al carrito
 *   - CartViewModel -> muestra el contenido del carrito actual
 *
 * ELIMINADO: createProduct()
 * El original tenía createProduct(productRequest) - operación de administración de inventario.
 * Con el nuevo enfoque el cliente solo consulta productos.
 *
 * AGREGADO: manejo de carrito desde el catálogo
 * La pantalla de catálogo tiene botones +/- por producto (ProductAdapter).
 * Este ViewModel coordina esas acciones con los use cases del carrito.
 *
 * SOBRE EL ESTADO DE CANTIDADES:
 * cartQuantities es un Map<productId, quantity> que el ProductAdapter usa via el callback getCurrentCartQuantity(product)
 */

class ProductCatalogViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val addToCartUseCase: AddToCartUseCase,
    private val updateCartItemUseCase: UpdateCartItemUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase
) : ViewModel() {

    // Estado del catálogo
    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    // Estado de operaciones del carrito
    private val _cartActionState = MutableStateFlow<UiState<CartItem>>(UiState.Idle)
    val cartActionState: StateFlow<UiState<CartItem>> = _cartActionState.asStateFlow()

    /**
     * Mapa de cantidades actuales en el carrito por productId.
     * Es la fuente de verdad el ProductAdapter consulta con getCurrentCartQuantity(product) para
     * mostrar la cantidad correcta.
     *
     * NOTA PARA REVISIÓN FUTURA DE ProductAdapter:
     * Actualmente el adapter recibe este mapa via lambda. Cuando se refactorice el adapter, debería
     * observar este StateFlow directamente y actualizarse solo cuando cambia - sin que el Fragment intervenga
     */
    private val _cartQuantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cartQuantities: StateFlow<Map<Int, Int>> = _cartQuantities.asStateFlow()

    init {
        loadProducts()
    }

    // Catálogo
    fun loadProducts() {
        viewModelScope.launch {
            _productsState.value = UiState.Loading

            getProductsUseCase()
                .onSuccess { products ->
                    _productsState.value = UiState.Success(products)
                }

                .onFailure { error ->
                    _productsState.value = UiState.Error(
                        error.message ?: "Error al cargar productos"
                    )
                }
        }
    }

    fun refreshProducts() = loadProducts()

    // Acciones del carrito desde el catálogo

    /**
     * Agrega o actualiza cantidad de un producto en el carrito.
     * Llamado desde ProductAdapter cuando el usuario toca "+"
     *
     * @param productId ID del producto
     * @param newQuantity Nueva cantidad total (ya calculada en el Adapter)
     */
    fun increaseQuantity(productId: Int, newQuantity: Int) {
        viewModelScope.launch {
            val currentQty = _cartQuantities.value[productId] ?: 0

            val result = if (currentQty == 0) {
                // Primera vez que agrega este producto
                addToCartUseCase(productId, newQuantity)
            } else {
                // Ya está en el carrito, actualizar cantidad
                updateCartItemUseCase(productId, newQuantity)
            }

            result
                .onSuccess { cartItem ->
                    // Actualizar el mapa de cantidades localmente
                    _cartQuantities.value = _cartQuantities.value + (productId to cartItem.quantity)
                    _cartActionState.value = UiState.Success(cartItem)
                }

                .onFailure { error ->
                    _cartActionState.value = UiState.Error(
                        error.message ?: "Error al agregar al carrito"
                    )
                }
        }
    }

    /**
     * Reduce la cantidad de un producto. Si llega a 0, lo elimina.
     * Llamado desde ProductAdapter cuando el usuario toca "-"
     */
    fun decreaseQuantity(productId: Int, newQuantity: Int) {
        viewModelScope.launch {
            if (newQuantity == 0) {
                removeProduct(productId)
                return@launch
            }

            updateCartItemUseCase(productId, newQuantity)
                .onSuccess { cartItem ->
                    _cartQuantities.value = _cartQuantities.value + (productId to cartItem.quantity)
                    _cartActionState.value = UiState.Success(cartItem)
                }

                .onFailure { error ->
                    // "ITEM_REMOVED" es el código del UpdateCartItemUseCase cuando qty = 0
                    if (error.message == "ITEM_REMOVED") {
                        _cartQuantities.value = _cartQuantities.value - productId
                    } else{
                        _cartActionState.value = UiState.Error(
                            error.message ?: "Error al eliminar del carrito"
                        )
                    }
                }
        }
    }

    /**
     * Elimina completamente un producto del carrito.Llamado desde ProductAdapter en el botón eliminar.
     */
    fun removeProduct(productId: Int) {
        viewModelScope.launch {
            removeFromCartUseCase(productId)
                .onSuccess {
                    _cartQuantities.value = _cartQuantities.value - productId
                    _cartActionState.value = UiState.Idle
                }

                .onFailure { error ->
                    _cartActionState.value = UiState.Error(
                        error.message ?: "Error al eliminar del carrito"
                    )
                }
        }
    }

    /**
     * Cantidad actual de un producto en el carrito. Usado por el adapter
     */
    fun getCurrentQuantity(productId: Int): Int = _cartQuantities.value[productId] ?: 0

    fun onCartActionHandled() {
        _cartActionState.value = UiState.Idle
    }

}
