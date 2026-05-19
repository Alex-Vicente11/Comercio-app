package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.dto.AddToCartRequestDto
import com.oax.comercioapp.data.dto.MergeCartRequestDto
import com.oax.comercioapp.data.dto.RemoveCartRequestDto
import com.oax.comercioapp.data.dto.UpdateCartRequestDto
import com.oax.comercioapp.data.mapper.CartMapper.toDomain
import com.oax.comercioapp.data.mapper.ProductMapper.toDomain
import com.oax.comercioapp.domain.model.Cart
import com.oax.comercioapp.domain.model.CartItem
import com.oax.comercioapp.domain.repository.ICartRepository

/**
 * Nota sobre el manejo de cantidades (referencia: ProductAdapter.kt):
 *
 * El adapter calcula la nueva cantidad localmente y lo pasa como parámetro.
 * Este repositorio simplemente la recibe y la envía a la API - no recalcula.
 * La validación de que quantity >= 0 debería ocurrir en el UseCase, no aquí.
 * El repositorio confía en que quien lo llama ya validó.
 *
 * Flujo completo del evento "+" en el adapter (para referencia):
 *   ProductAdapter.increaseQuantity(product, qty + 1)
 *      -> ProductEvent (interfaz) en Fragment
 *          -> CartViewModel.addToCart(productId, quantity)
 *              -> AddToCartUseCase.invoke(productId, quantity)
 *                  -> ICartRepository.addToCart(productId, quantity)
 *                      -> ApiService.addToCart(AddToCartRequestDto(...)
 *
 * Cada capa tiene una responsabilidad clara en ese flujo
 */

class CartRepositoryImpl(
    private val apiService: ApiService
) : ICartRepository {
    override suspend fun getCart(): Result<Cart> {
        return try {
            val response = apiService.getCartDetailed()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body.toDomain())  // CartMapper convierte todoo el árbol
                } else {
                    Result.failure(Exception("Cart unavailable"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addToCart(
        productId: Int,
        quantity: Int
    ): Result<CartItem> {
        return try {
            val response = apiService.addToCart(
                AddToCartRequestDto(idProduct = productId, quantity = quantity)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val cartItemDto = body.cartItem

                if (body.success && cartItemDto != null) {
                    /**
                     * La API devuelve CartItemDto básico (sin detalles del producto).
                     * Para construir CartItem del dominio necesitamos el Product completo.
                     * NOTA: esto requiere un segundo call a getProductById o que la API
                     * devuelva el producto anidado. Por ahora construimos un CartItem parcial.
                     */
                    val product = apiService.getProductById(productId).body()?.toDomain()
                        ?: return Result.failure(Exception("Could not fetch product details"))

                    Result.success(
                        CartItem(
                            cartId = cartItemDto.idCart,
                            product = product,
                            quantity = cartItemDto.quantity,
                            subtotal = product.price * cartItemDto.quantity,
                            addedDate = ""
                        )
                    )
                } else {
                    Result.failure(Exception(body.message ?: "Add to cart failed"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCartItem(
        productId: Int,
        quantity: Int
    ): Result<CartItem> {
        return try {
            val response = apiService.updateCart(
                UpdateCartRequestDto(idProduct = productId, quantity = quantity)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val cartItemDto = body.cartItem

                if (body.success && cartItemDto != null) {
                    val product = apiService.getProductById(productId).body()?.toDomain()
                        ?: return Result.failure(Exception("Could not fetch product details"))

                    Result.success(
                        CartItem(
                            cartId = cartItemDto.idCart,
                            product = product,
                            quantity = cartItemDto.quantity,
                            subtotal = product.price * cartItemDto.quantity,
                            addedDate = ""
                        )
                    )
                } else {
                    Result.failure(Exception(body.message ?: "Update failed"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromCart(productId: Int): Result<Unit> {
        return try {
            val response = apiService.removeFromCart(
                RemoveCartRequestDto(idProduct = productId)
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Remove from cart failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCartCount(): Result<Int> {
        return try {
            val response = apiService.getCartCount()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.count)
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergeGuestCart(guestId: String): Result<Int> {
        return try {
            val response = apiService.mergeCart(
                MergeCartRequestDto(guestId = guestId)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body.mergedItems)
            } else {
                Result.failure(Exception("Cart merge failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}