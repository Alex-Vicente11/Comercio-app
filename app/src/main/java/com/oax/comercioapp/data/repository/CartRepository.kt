package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.api.RetrofitClient
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.AddToCartRequest
import com.oax.comercioapp.data.models.Cart
import com.oax.comercioapp.data.models.CartCheckResponse
import com.oax.comercioapp.data.models.CartCountResponse
import com.oax.comercioapp.data.models.CartDetailedResponse
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.data.models.CartRequest
import com.oax.comercioapp.data.models.CartResponse
import com.oax.comercioapp.data.models.CartUpdateRequest
import com.oax.comercioapp.data.models.MergeCartRequest
import com.oax.comercioapp.data.models.MergeCartResponse
import com.oax.comercioapp.data.models.RemoveCartRequest
import com.oax.comercioapp.data.models.UpdateCartRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException

/**
 * CartRepository - Gestión del carrito de compras con JWT
 *
 * CAMBIOS vs versión anterior:
 * - Usa endpoints protegidos con JWT (/api/cart/*)
 * - No requiere enviar user_id (viene del token)
 * - Manejo automático de 401 Unauthorized
 * - Nuevos endpoints: check, count, items detailed
 * - Mantiene endpoints legacy para compatibilidad
*/ */


class CartRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {

    // ====================================
    // NUEVOS ENDPOINTS PROTEGIDOS CON JWT
    // ====================================

    /**
     * Obtiene los items del carrito con formato detallado
     *
     * NUEVO: Usa /api/cart/items (protegido con JWT)
     * El user_id se obtiene del token automáticamente
     *
     * .@return Flow con lista de items detallados y total
     */

    fun getCartItemsDetailed(): Flow<NetworkResult<CartDetailedResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Verificar que hay sesion activa
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Error("No hay sesion activa"))
                return@flow
            }

            val response = apiService.getCartItemsDetailed()

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Obtiene el conteo rápido de items en el carrito
     *
     * NUEVO: Usa /api/cart/count (más rápido que cargar todo el carrito)
     * Ideal para mostrar badge en UI
     *
     * .@return Flow con conteo total y productos únicos
     */

    fun getCartCount(): Flow<NetworkResult<CartCountResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Success(CartCountResponse(true, 0, 0)))
                return@flow
            }

            val response = apiService.getCartCount()

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Verifica si un producto específico está en el carrito
     *
     * NUEVO: Usa /api/cart/check/{productId}
     * Evita cargar todo el carrito solo para verificar un producto
     *
     * @param productId ID del producto a verificar
     * @return Flow con resultado (exists, quantity)
     */

    fun checkProductInCart(productId: Int): Flow<NetworkResult<CartCheckResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Success(CartCheckResponse(true, false, null)))
                return@flow
            }

            val response = apiService.checkProductInCart(productId)

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)


    // Obtener items del carrito por usuario
    fun getCartItems(userId: Int): Flow<NetworkResult<List<CartItem>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getCartItems(userId)
            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 404) {
                emit(NetworkResult.Success(emptyList<CartItem>()))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error desconocido"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Agrega un producto al carrito
     *
     * ACTUALIZADO: Usa /api/cart/add (protegido con JWT)
     * Ya NO requiere enviar user_id en el body
     *
     * @param productId ID del producto
     * @param quantity Cantidad a agregar
     * @return Flow con resultado
     */


    // Agregar producto al carrito
    fun addToCart(productId: Int, quantity: Int = 1): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            // verificar sesion
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Error("Debe iniciar sesion para agregar al carrito"))
                return@flow
            }

            // Crear request (sin user_id, viene del token
            val request = AddToCartRequest(
                idProduct = productId,
                quantity = quantity
            )

            val response = apiService.addToCart(request)

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }

        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al agregar al carrito"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Actualiza la cantidad de un producto en el carrito
     *
     * ACTUALIZADO: Usa /api/cart/update (protegido con JWT)
     *
     * @param productId ID del producto
     * @param quantity Nueva cantidad
     * @return Flow con resultado
     */

    // Actualizar cantidad del carrito
    fun updateCartQuantity(productId: Int, quantity: Int): Flow<NetworkResult<CartResponse>> =
        flow {
            emit(NetworkResult.Loading())
            try {
                if (!UserPreferences.isLoggedIn()) {
                    emit(NetworkResult.Error("No hay sesion activa"))
                    return@flow
                }

                val request = UpdateCartRequest(
                    idProduct = productId,
                    quantity = quantity
                )

                val response = apiService.updateCartQuantity(request)

                if (response.isSuccessful && response.body() != null) {
                    emit(NetworkResult.Success(response.body()!!))
                } else if (response.code() == 401) {
                    emit(NetworkResult.Error("Sesion expirada", 401))
                } else {
                    emit(NetworkResult.Error(response.message()))
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    emit(NetworkResult.Error("Sesion expirada", 401))
                } else {
                    emit(NetworkResult.Error(e.message()))
                }

            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Error al actualizar carrito"))
            }
        }.flowOn(Dispatchers.IO)


    /**
     * Elimina un producto del carrito
     *
     * ACTUALIZADO: Usa /api/cart/remove (protegido con JWT)
     *
     * @param productId ID del producto a eliminar
     * @return Flow con resultado
     */


    // Eliminar item del carrito
    fun removeFromCart(productId: Int): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Error("No hay sesion activa"))
                return@flow
            }

            val request = RemoveCartRequest(idProduct = productId)

            val response = apiService.removeFromCart(request)

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }

        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }

        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al eliminar del carrito"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Vacía completamente el carrito
     *
     * NUEVO: Usa /api/cart/clear
     *
     * @return Flow con resultado
     */

    fun clearCart(): Flow<NetworkResult<CartResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!UserPreferences.isLoggedIn()) {
                emit(NetworkResult.Error("No hay sesion activa"))
                return@flow
            }

            val response = apiService.clearCart()

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al vaciar carrito"))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Fusiona manualmente el carrito de guest con usuario autenticado
     *
     * NUEVO: Backup por si falla la fusión automática en register/login
     *
     * @param guestId ID del guest para fusionar
     * @return Flow con resultado
     */

    fun mergeCart(guestId: String): Flow<NetworkResult<MergeCartResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!UserPreferences.isLoggedIn() || UserPreferences.isGuest()) {
                emit(NetworkResult.Error("Debe estar autenticado para fusionar carrito"))
                return@flow
            }

            val request = MergeCartRequest(guestId = guestId)
            val response = apiService.mergeCart(request)

            if (response.isSuccessful && response.body() != null) {
                emit(NetworkResult.Success(response.body()!!))
            } else if (response.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(response.message()))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(NetworkResult.Error("Sesion expirada", 401))
            } else {
                emit(NetworkResult.Error(e.message()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error al fusionar carrito"))
        }
    }.flowOn(Dispatchers.IO)


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
    }.flowOn(Dispatchers.IO)



    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Verifica si el usuario puede usar el carrito
     *
     * @return true si hay sesión activa (guest o autenticado)
     */
    fun canUseCart(): Boolean {
        return try {
            UserPreferences.isLoggedIn()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene el ID del usuario actual para logging/debugging
     *
     * @return ID del usuario o null
     */
    fun getCurrentUserId(): Int? {
        return try {
            val userId = UserPreferences.getUserId()
            if (userId > 0) userId else null
        } catch (e: Exception) {
            null
        }
    }
}