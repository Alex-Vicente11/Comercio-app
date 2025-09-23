package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.api.RetrofitClient
import com.oax.comercioapp.data.models.Cart
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.data.models.CartRequest
import com.oax.comercioapp.data.models.CartResponse
import com.oax.comercioapp.data.models.CartUpdateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CartRepository {
    private val apiService: ApiService = RetrofitClient.apiService

    // Obtener items del carrito por usuario
    fun getCartItems(userId: Int): Flow<NetworkResult<List<CartItem>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getCartItems(userId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }

    // Agregar producto al carrito
    fun addToCart(cartRequest: CartRequest): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.addToCart(cartRequest)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al agregar al carrito"))
        }
    }

    // Actualizar cantidad del carrito
    fun updateCartQuantity(cartId: Int, quantity: Int): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val updateRequest = CartUpdateRequest(quantity = quantity)
            val response = apiService.updateCartItem(cartId, updateRequest)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception){
            emit(NetworkResult.Error(e.message ?: "Error al actualizar carrito"))
        }
    }

    // Eliminar item del carrito
    fun removeFromCart(cartId: Int): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.removeFromCart(cartId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al eliminar del carrito"))
        }
    }

    // Verificar si un producto ya está en el carrito
    fun getCartItem(userId: Int, productId: Int): Flow<NetworkResult<Cart?>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getCartItem(userId, productId)
            if (response.isSuccessful) {
                emit(NetworkResult.Success(response.body()))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al verificar carrito"))
        }
    }
}