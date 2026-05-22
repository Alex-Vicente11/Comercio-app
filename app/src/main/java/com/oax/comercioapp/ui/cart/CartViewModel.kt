package com.oax.comercioapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.domain.model.Cart
import com.oax.comercioapp.domain.usecase.GetCartCountUseCase
import com.oax.comercioapp.domain.usecase.GetCartUseCase
import com.oax.comercioapp.domain.usecase.RemoveFromCartUseCase
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. ELIMINADO: checkProductInCart() antes de cada operación
 *    El original hacía un call de red extra POR CADA toque en el adapter:
 *      handledIncreaseQuantity -> checkProductInCart -> addToCart -> updateCart
 *    Eso son 2 requests de red por cada "+" . Con el mapa cartQuantities en
 *    ProductCatalogViewModel como fuente de verdad local, ya sabemos si el producto está en el
 *    carrito SIN llamar a la red.
 *
 *    Este era el problema de rendimiento más visible para el usuario: latencia perceptible en cada
 *    toque del botón "+"
 *
 * 2. ELIMINADO: handleDecreaseQuantity(userId: Int, product, quantity)
 *    El userId nunca se usaba - era un parámetro fantasma que el Fragment pasaba como
 *    UserPreferences.getUserId() innecesariamente. Las operaciones del adapter ahora van directo
 *    a ProductCatalogViewModel que tiene los use cases correspondientes.
 *
 * 3. RESPONSABILIDAD ACLARADA:
 *    CartViewModel original manejaba TANTO el catálogo de productos como el carrito. Ahora hay una
 *    separación clara:
 *      - ProductCatalogViewModel -> catálogo + acciones add/update/remove
 *      - CartViewModel -> vista del carrito (resumen, total, count para badge)
 *    ProductCatalogViewModel ya tiene AddToCartUseCase, UpdateCartItemUseCase y RemoveFromCartUseCase.
 *    CartViewModel se enfoca en la vista del carrito.
 *
 * 4. clearCart() se conserva - es una operación válida de negocio ("vaciar carrito") que el usuario
 *    puede ejecutar conscientement.
 */

class CartViewModel (
    private val getCartUseCase: GetCartUseCase,
    private val getCartCountUseCase: GetCartCountUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase
) : ViewModel() {

    /**
     * Estado del carrito completo (items + total + count).
     * Usado por la pantalla de resumen del carrito.
     */
    private val _cartState = MutableStateFlow<UiState<Cart>>(UiState.Idle)
    val cartState: StateFlow<UiState<Cart>> = _cartState.asStateFlow()

    /**
     * Contador de items únicos - para el badge del toolbar.
     * Se actualiza independientemente del cartState para no recargar todoo el carrito solo
     * para actualizar el número.
     */
    private val _cartCount = MutableStateFlow(0)
    val cartCount: StateFlow<Int> = _cartCount.asStateFlow()

    /**
     * Estado de operaciones de eliminación (remove, clear).
     * Separado de cartState para que el Fragment pueda mostrar feedback de la acción sin
     * interferir con la vista del carrito.
     */
    private val _cartActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val cartActionState: StateFlow<UiState<Unit>> = _cartActionState.asStateFlow()


    /**
     * Carga el carrito completo con detalles de productos, subtotales y total.
     * CUÁNDO LLAMAR ESTO:
     * - Al entrar a la pantalla de resumen del carrito
     * - Después de confirmar un pedido (para mostrar carrito vacío)
     * NO llamar después de cada add/update - el adapter actualiza cartQuantities localmente para evitar
     * recargas innecesarias.
     */
    fun loadCart() {
        viewModelScope.launch {
            _cartState.value = UiState.Loading

            getCartUseCase()
                .onSuccess { cart ->
                    _cartState.value = UiState.Success(cart)
                    _cartCount.value = cart.itemCount
                }

                .onFailure { error ->
                    val message = when {
                        error.message?.contains("401") == true -> "Sesión expirada, inicia sesión nuevamente"
                        else -> error.message ?: "Error al cargar el carrito"
                    }
                    _cartState.value = UiState.Error(message)
                }
        }
    }

    /**
     * Actualiza solo el contador sin recargar el carrito completo.
     * Más eficiente para mantener el badge del toolbar actualizado.
     */

    fun refreshCartCount() {
        viewModelScope.launch {
            getCartCountUseCase()
                .onSuccess { count -> _cartCount.value = count }
                .onFailure { _cartCount.value = 0 }
        }
    }

    /**
     * Elimina completamente un producto del carrito.
     *
     * NOTA SOBRE EL FLUJO CON ProductAdapter:
     * El adapter llama removeQuantities(product) -> CartFragment llama esto.
     * Después eliminar, CartFragment debe notificar a ProductCatalogViewModel
     * para que actualice su cartQuantities local. Eso se hace via:
     *   productCatalogViewModel.removeProduct(productId)
     *
     * Ver CartFragment para el flujo completo coordinado.
     */
    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            _cartActionState.value = UiState.Loading

            removeFromCartUseCase(productId)
                .onSuccess {
                    _cartActionState.value = UiState.Success(Unit)
                    refreshCartCount()
                }

                .onFailure { error ->
                    _cartActionState.value = UiState.Error(
                        error.message ?: "Error al eliminar del carrito"
                    )
                }
        }
    }

    fun onCartActionHandled() {
        _cartActionState.value = UiState.Idle
    }
}
