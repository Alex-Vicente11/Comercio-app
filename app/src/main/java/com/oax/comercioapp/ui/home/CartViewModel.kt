package com.oax.comercioapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.data.models.CartRequest
import com.oax.comercioapp.data.models.CartResponse
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.repository.CartRepository
import kotlinx.coroutines.launch

class CartViewModel ( private val cartRepository: CartRepository = CartRepository()
) : ViewModel() {

    private val _cartItems = MutableLiveData<NetworkResult<List<CartItem>>>()
    val cartItems: LiveData<NetworkResult<List<CartItem>>> = _cartItems

    private val _addToCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val addToCartResult: LiveData<NetworkResult<CartResponse>> = _addToCartResult

    private val _updateCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val updateCartResult: LiveData<NetworkResult<CartResponse>> = _updateCartResult

    private val _removeFromCartResult = MutableLiveData<NetworkResult<CartResponse>>()
    val removeFromCartResult: LiveData<NetworkResult<CartResponse>> = _removeFromCartResult

    // Cargar items del carrito

    fun loadCartItems(userId: Int) {
        viewModelScope.launch {
            cartRepository.getCartItems(userId).collect { result ->
                _cartItems.postValue(result)
            }
        }
    }

    // Agregar producto al carrito
    fun addToCart(userId: Int, productId: Int, quantity: Int) {
        viewModelScope.launch {
            // Primero verificar si el producto ya esta en el carrito
            cartRepository.getCartItem(userId, productId).collect { existingItemResult ->
                when (existingItemResult) {
                    is NetworkResult.Success -> {
                        val existingItem = existingItemResult.data
                        if (existingItem != null) {
                            // si existe, actualizar la cantidad
                            val newQuantity = existingItem.quantity + quantity
                            updateCartQuantity(existingItem.idCart, newQuantity)
                        } else {
                            // si no existe, agregar nuevo item
                            val cartRequest = CartRequest(userId, productId, quantity)
                            cartRepository.addToCart(cartRequest).collect { result ->
                                _addToCartResult.postValue(result)
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        // Si hay error verificando, intentar agregar de todos modos
                        val cartRequest = CartRequest(userId, productId, quantity)
                        cartRepository.addToCart(cartRequest).collect { result ->
                            _addToCartResult.postValue(result)
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Waiting...
                    }
                }
            }
        }
    }

    // Actualizar cantidad del carrito
    fun updateCartQuantity(cartId: Int, quantity: Int) {
        viewModelScope.launch {
            cartRepository.updateCartQuantity(cartId, quantity).collect { result ->
                _updateCartResult.postValue(result)
            }
        }
    }

    // Eliminar del carrito
    fun removeFromCart(cartId: Int) {
        viewModelScope.launch {
            cartRepository.removeFromCart(cartId).collect { result ->
                _removeFromCartResult.postValue(result)
            }
        }
    }

    // Manejar incremento de cantidad desde el adapter
    fun handleIncreaseQuantity(userId: Int, product: Product, newQuantity: Int) {
        if (newQuantity == 1) {
            // Primera vez agregando al carrito
            addToCart(userId, product.idProduct, 1)
        } else {
            // Ya existe, buscar el cart item y actualizar
            viewModelScope.launch {
                cartRepository.getCartItem(userId, product.idProduct).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            result.data?.let { cartItem ->
                                updateCartQuantity(cartItem.idCart, newQuantity)
                            }
                        }
                        else -> {
                            // Fallback: agregar como nuevo item
                            addToCart(userId, product.idProduct, newQuantity)
                        }
                    }
                }
            }
        }
    }

    // Manejar decremento de cantidad desde el adapter
    fun handleDecreaseQuantity(userId: Int, product: Product, newQuantity: Int) {
        viewModelScope.launch {
            cartRepository.getCartItem(userId, product.idProduct).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        result.data?.let { cartItem ->
                            if (newQuantity == 0) {
                                removeFromCart(cartItem.idCart)
                            } else {
                                updateCartQuantity(cartItem.idCart, newQuantity)
                            }
                        }
                    }
                    else -> {
                        // Error handling
                    }
                }
            }
        }
    }
}