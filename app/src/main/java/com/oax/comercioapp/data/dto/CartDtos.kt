package com.oax.comercioapp.data.dto

import com.google.gson.annotations.SerializedName

/**
 *  El Cart.kt original tenía una mezcla de 3 tipos de objetos:
 *  1. DTOs de red (Cart, CartRequest) -> van aquí
 *  2. Entidades de dominio (CartItem con Product) -> ya en domain/model/
 *  3. Comentarios de código muerto (CartResponse comentado)
 *
 *  Aquí se separa limpiamente los DTOs que representan la estructura
 *  exacta de la API, sin mezclar con lógica de presentación
 *
 *  Observación sobre CartItemDetailedDto vs CartItemDto:
 *  La API tiene dos endpoints distintos para el carrito:
 *      - Uno básico (id_cart, id_product, quantity) -> CartItemDto
 *      - Uno detallado con productos anidados -> CartItemDetailedDto
 *  El mapper sabrá cómo convertir cada uno a la entidad CartItem del dominio
 */
// --- Requests ---
data class AddToCartRequestDto(
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

data class UpdateCartRequestDto(
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

data class RemoveCartRequestDto(
    @SerializedName("id_product")
    val idProduct: Int
)

data class MergeCartRequestDto(
    @SerializedName("guest_id")
    val guestId: String
)

// --- Responses ---
/**
 * Respuesta simple de operaciones de carrito (add, update, remove)
 * Solo confirma que la operación fue exitosa y devuelve el item afectado.
 */
data class CartOperationResponseDto(
    val success: Boolean,
    val message: String?,
    @SerializedName("cart_item")
    val cartItem: CartItemDto?
)

/**
 * Item básico del carrito - solo IDs y cantidad, sin detalles del producto
 * Viene de endpoints de operación (add/update/check)
 */

data class CartItemDto(
    @SerializedName("id_cart")
    val idCart: Int,
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

/**
 * Respuesta del endpoint de carrito detallado (GET /cart)
 * Incluye productos anidados, subtotales y totales
 * Este es el DTO que usa el mapper principal para construir Cart del dominio
 */
data class CartDetailedResponseDto(
    val success: Boolean,
    val items: List<CartItemDetailedDto>,
    val total: Double,
    @SerializedName("item_count")
    val itemsCount: Int
)

data class CartItemDetailedDto(
    @SerializedName("id_cart")
    val idCart: Int,
    val product: ProductInfoDto,
    val quantity: Int,
    val subtotal: Double,
    @SerializedName("added_date")
    val addedDate: String
)

/**
 * ProductInfoDto - información del producto tal como viene DENTRO del carrito.
 * Es distinto a ProductDto porque la API devuelve una forma reducida del
 * producto cuando está anidado en el carrito (sin descripción, sin stock, etc.)
 *
 * Este es un patrón común en APIs REST: el mismo recurso tiene distintas
 * representaciones según el contexto. Por eso necesitamos DTOs separados.
 */
data class ProductInfoDto(
    @SerializedName("id_product")
    val idProduct: Int,
    val product: String,
    val price: Double
)

data class CartCountResponseDto(
    val success: Boolean,
    val count: Int,
    @SerializedName("unique_products")
    val uniqueProducts: Int
)

data class MergeCartResponseDto(
    val success: Boolean,
    @SerializedName("merged_items")
    val mergedItems: Int,
    val message: String
)
