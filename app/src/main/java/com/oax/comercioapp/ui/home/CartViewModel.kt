package com.oax.comercioapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.data.models.CartItemDetailed
import com.oax.comercioapp.data.models.CartRequest
import com.oax.comercioapp.data.models.CartResponse
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.CartRepository
import com.oax.comercioapp.utils.SessionManager
import kotlinx.coroutines.launch


/**
 * CartViewModel - ViewModel para gestión del carrito de compras
 *
 * CAMBIOS vs versión anterior:
 * - Ya NO requiere userId en los métodos (se obtiene del token)
 * - Manejo automático de 401 Unauthorized
 * - Nuevos LiveData: cartCount, cartTotal
 * - Métodos simplificados sin userId manual
 * - Verificación de sesión antes de operaciones
 */

class CartViewModel ( private val cartRepository: CartRepository = CartRepository()
) : ViewModel() {

    // Items del carrito (formato detallado)
    private val _cartItems = MutableLiveData<NetworkResult<List<CartItemDetailed>>>()
    val cartItems: LiveData<NetworkResult<List<CartItemDetailed>>> = _cartItems

    // Total del carrito
    private val _cartTotal = MutableLiveData<Double>()
    val cartTotal: LiveData<Double> = _cartTotal

    // Contador de items (para badge)
    private val _cartCount = MutableLiveData<Int>()
    val cartCount: LiveData<Int> = _cartCount

    // Resultado de agregar al carrito
    private val _addToCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val addToCartResult: LiveData<NetworkResult<CartResponse>> = _addToCartResult

    // Resultado de actualizar cantidad
    private val _updateCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val updateCartResult: LiveData<NetworkResult<CartResponse>> = _updateCartResult

    // Resultado de eliminar del carrito
    private val _removeFromCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val removeFromCartResult: LiveData<NetworkResult<CartResponse>> = _removeFromCartResult

    // Cargar items del carrito


    // ============================================
    // CARGAR ITEMS DEL CARRITO
    // ============================================

    /**
     * Carga los items del carrito del usuario actual
     *
     * ACTUALIZADO: Ya NO requiere userId (viene del token JWT)
     */
    fun loadCartItems() {
        viewModelScope.launch {
            // Verificar sesion
            if (!UserPreferences.isLoggedIn()) {
                _cartItems.postValue(NetworkResult.Error("Debe inicar sesión"))
                return@launch
            }

            _cartItems.postValue(NetworkResult.Loading())

            cartRepository.getCartItemsDetailed().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Extraer los items de la respuesta detallada
                        _cartItems.postValue(NetworkResult.Success(result.data.items))
                        _cartTotal.postValue(result.data.total)
                        _cartCount.postValue(result.data.itemsCount)
                    }

                    is NetworkResult.Error -> {
                        // Manejo especial de 401
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _cartItems.postValue(NetworkResult.Error(result.message, result.code))
                    }

                    is NetworkResult.Loading -> {
                        _cartItems.postValue(NetworkResult.Loading())
                    }
                }
            }
        }
    }

    /**
     * Refresca solo el contador (más rápido que cargar todo)
     *
     * NUEVO: Usa endpoint optimizado /api/cart/count
     */

    fun refreshCartCount() {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _cartCount.postValue(0)
                return@launch
            }

            cartRepository.getCartCount().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _cartCount.postValue(result.data.count)
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _cartCount.postValue(0)
                    }
                    is NetworkResult.Loading -> {
                        // No cambiar valor mientras carga
                    }
                }
            }
        }
    }


    // ============================================
    // AGREGAR AL CARRITO
    // ============================================

    /**
     * Agrega un producto al carrito
     *
     * ACTUALIZADO: Ya NO requiere userId
     *
     * @param productId ID del producto
     * @param quantity Cantidad a agregar (default: 1)
     */
    fun addToCart(productId: Int, quantity: Int = 1) {
        viewModelScope.launch {
            // Verificar sesion
            if (!UserPreferences.isLoggedIn()) {
                _addToCartResult.postValue(NetworkResult.Error("Debe inicar sesion para agregar al carrito "))
                return@launch
            }

            _addToCartResult.postValue(NetworkResult.Loading())

            // Primero verificar si el producto ya esta en el carrito
            cartRepository.addToCart(productId, quantity).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _addToCartResult.postValue(result)
                        // Refrescar contador automáticamente
                        refreshCartCount()
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _addToCartResult.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _addToCartResult.postValue(result)
                    }
                }
            }
        }
    }

    /**
     * Agrega o actualiza un producto en el carrito de forma inteligente
     *
     * NUEVO: Verifica si existe antes de agregar
     * - Si existe: incrementa cantidad
     * - Si no existe: agrega nuevo
     *
     * @param productId ID del producto
     * @param quantity Cantidad a agregar
     */

    fun addOrUpdateProduct(productId: Int, quantity: Int = 1) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _addToCartResult.postValue(NetworkResult.Error("Debe iniciar sesion"))
                return@launch
            }

            _addToCartResult.postValue(NetworkResult.Loading())

            // Primero verificar si existe
            cartRepository.checkProductInCart(productId).collect { checkResult ->
                when (checkResult) {
                    is NetworkResult.Success -> {
                        if (checkResult.data.exists) {
                            // Ya existe, actualizar cantidad
                            val currentQuantity = checkResult.data.cartItem?.quantity ?: 0
                            val newQuantity = currentQuantity + quantity
                            updateCartQuantity(productId, newQuantity)
                        } else {
                            // No existe, agregar nuevo
                            addToCart(productId, quantity)
                        }
                    }
                    is NetworkResult.Error -> {
                        if (checkResult.code == 401) {
                            SessionManager.logout()
                        }
                        // Si falla el check, intentar agregar de todos modos
                        addToCart(productId, quantity)
                    }
                    is NetworkResult.Loading -> {
                        _addToCartResult.postValue(NetworkResult.Loading())
                    }
                }
            }
        }
    }

    // ============================================
    // ACTUALIZAR CANTIDAD
    // ============================================

    /**
     * Actualiza la cantidad de un producto en el carrito
     *
     * ACTUALIZADO: Ya NO requiere cartId, usa productId
     *
     * @param productId ID del producto
     * @param quantity Nueva cantidad
     */

    // Actualizar cantidad del carrito
    fun updateCartQuantity(productId: Int, quantity: Int) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _updateCartResult.postValue(NetworkResult.Error("No hay sesion activa"))
                return@launch
            }

            _updateCartResult.postValue(NetworkResult.Loading())

            cartRepository.updateCartQuantity(productId, quantity).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _updateCartResult.postValue(result)
                        // Refrescar contador
                        refreshCartCount()
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _updateCartResult.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _updateCartResult.postValue(result)
                    }
                }

            }
        }
    }

    // ============================================
    // ELIMINAR DEL CARRITO
    // ============================================

    /**
     * Elimina un producto del carrito
     *
     * ACTUALIZADO: Ya NO requiere cartId, usa productId
     *
     * @param productId ID del producto a eliminar
     */

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _removeFromCartResult.postValue(NetworkResult.Error("No hay sesion activa"))
                return@launch
            }

            _removeFromCartResult.postValue(NetworkResult.Loading())

            cartRepository.removeFromCart(productId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _removeFromCartResult.postValue(result)
                        // Refrescar contador
                        refreshCartCount()
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _removeFromCartResult.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _removeFromCartResult.postValue(result)
                    }
                }
            }
        }
    }


    /**
     * Vacía completamente el carrito
     *
     * NUEVO: Elimina todos los items de una vez
     */

    fun clearCart() {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _removeFromCartResult.postValue(NetworkResult.Error("No hay sesion activa"))
                return@launch
            }

            _removeFromCartResult.postValue(NetworkResult.Loading())

            cartRepository.clearCart().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _removeFromCartResult.postValue(result)
                        _cartCount.postValue(0)
                        _cartTotal.postValue(0.0)
                        loadCartItems() // Recargar lista vacia
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _removeFromCartResult.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _removeFromCartResult.postValue(result)
                    }
                }
            }
        }
    }


    // ============================================
    // MÉTODOS PARA ADAPTER (ProductAdapter)
    // ============================================

    /**
     * Maneja incremento de cantidad desde el adapter
     *
     * ACTUALIZADO: Ya NO requiere userId
     *
     * @param product Producto a incrementar
     * @param newQuantity Nueva cantidad
     */

    // Manejar incremento de cantidad desde el adapter
    fun handleIncreaseQuantity(product: Product, newQuantity: Int) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _updateCartResult.postValue(NetworkResult.Error("Debe iniciar sesion"))
                return@launch
            }

            // Verificar si existe en el carrito
            cartRepository.checkProductInCart(product.idProduct).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data.exists) {
                            // Ya existe en el carrito, actualizar cantidad
                            updateCartQuantity(product.idProduct, newQuantity)
                        } else {
                            // No existe, agregar nuevo
                            addToCart(product.idProduct, newQuantity)
                        }
                    }

                    is NetworkResult.Error -> {
                        // Si hay error (404 o cualquier otro), agregar como nuevo item
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        // Intentar agregar de todos modos
                        addToCart(product.idProduct, newQuantity)
                    }

                    is NetworkResult.Loading -> {
                        // No hacer nada, el loading lo manejará addToCart o updateCartQuantity
                    }
                }
            }
        }
    }



    /**
     * Maneja decremento de cantidad desde el adapter
     *
     * ACTUALIZADO: Ya NO requiere userId
     *
     * @param product Producto a decrementar
     * @param newQuantity Nueva cantidad (si es 0, elimina del carrito)
     */
    fun handleDecreaseQuantity(userId: Int, product: Product, newQuantity: Int) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _updateCartResult.postValue(NetworkResult.Error("No hay sesion activa"))
                return@launch
            }

            _updateCartResult.postValue(NetworkResult.Loading())

            cartRepository.checkProductInCart(product.idProduct).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data.exists) {
                            if (newQuantity <= 0) {
                                // Eliminar el carrito
                                removeFromCart(product.idProduct)
                            } else {
                                // Actualizar cantidad
                                updateCartQuantity(product.idProduct, newQuantity)
                            }
                        } else {
                            // No esta en el carrito, no hacer nada
                            _updateCartResult.postValue(NetworkResult.Error("Producto no esta en el carrito"))
                        }
                    }
                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                            _updateCartResult.postValue(NetworkResult.Error("Sesión expirada", 401))
                        } else {
                            _updateCartResult.postValue(NetworkResult.Error(result.message ?: "Error al verificar producto", result.code))
                        }
                    }
                    is NetworkResult.Loading -> {
                        _updateCartResult.postValue(NetworkResult.Loading())
                    }
                }
            }
        }
    }


    /**
     * Limpia completamente un producto del carrito
     *
     * ACTUALIZADO: Ya NO requiere userId, usa productId directamente
     *
     * @param product Producto a eliminar
     */
    fun clearQuantitiesFromCart(product: Product) {
        viewModelScope.launch {
            if (!UserPreferences.isLoggedIn()) {
                _removeFromCartResult.postValue(NetworkResult.Error("No hay sesion activa"))
                return@launch
            }
            println("DEBUG: clearQuantitiesFromCart - productId: ${product.idProduct}")

            // Directamente eliminar por productId
            removeFromCart(product.idProduct)

        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Verifica si el usuario puede usar el carrito
     *
     * @return true si hay sesión activa
     */
    fun canUseCart(): Boolean {
        return UserPreferences.isLoggedIn()
    }

    /**
     * Obtiene información del usuario actual para debugging
     *
     * @return Pair con userId y isGuest, o null si no hay sesión
     */
    fun getCurrentUserInfo(): Pair<Int, Boolean>? {
        return try {
            val userId = UserPreferences.getUserId()
            val isGuest = UserPreferences.isGuest()
            if (userId > 0) Pair(userId, isGuest) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resetea los resultados de operaciones
     *
     * Útil para limpiar mensajes de error/success después de mostrarlos
     */
    /*
    fun clearOperationResults() {
        _addToCartResult.value = null
        _updateCartResult.value = null
        _removeFromCartResult.value = null
    }*/
}