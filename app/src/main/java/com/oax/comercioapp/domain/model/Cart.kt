package com.oax.comercioapp.domain.model

/**
 * El Cart.kt original tenía una mezcla problemática:
 *  - 'Cart': modelo con @SerializedName para la DB/API (DTO)
 *  - 'CartItem': modelo que ya usaba Product del dominio (mezclado)
 *  - 'CartRequest', 'CartUpdateResquest': objetos de request de red
 *
 * Todoo eso en un solo archivo viola tanto SRP como la separación de capas.
 * Los requests de red no tienen nada que hacer junto con las entidades.
 *
 * Decisión de diseño - ¿Cart o CartItem como entidad principal?
 * En el dominio de e-commerce, lo que le importa a la UI es el CARRITO
 * como agregado completo: sus ítems, el total, cuántos productos hay.
 * Por eso modelamos:
 *  - CartItem: un producto + cantidad + fecha (lo que el usuario agregó)
 *  - Cart: el agregado completo que ve la pantalla del carrito
 *
 * Nota sobre 'addedDate' como String:
 * Idealmente sería kotlinx.datetime.LocalDateTime o java.time.LocalDateTime,
 * pero mantenemos String para simplificar la migración inicial. Es una mejora
 * que se puede hacer cuando esten los mappers listos.
 */


/**
 * Representa un item dentro del carrito.
 * Combina el producto (entidad de dominio) con la cantidad elegida.
 *
 * ¿Por qué 'product': Product' y no 'productId: Int'?
 * En el dominio, cuando mostramos el carrito necesitamos el nombre y precio
 * del producto - no solo su ID. Guardar solo el ID obligaría a la UI a hacer
 * una segunda consulta, lo que es una fuga de lógica de infraestructura.
 */
data class CartItem(
    val cartId: Int,
    val product: Product,
    val quantity: Int,
    val subtotal: Double,
    val addedDate: String
)

/**
 * Representa el carrito completo del usuario.
 * Es un "Aggregate Root" en téminos de DDD (Domain-Driven-Design):
 * el objeto raíz que agrupa y da coherencia a sus CartItems.
 *
 * La UI solo necesita este objeto para renderizar la pantalla completa
 * del carrito - no necesita hacer múltiples llamadas por separado.
 */
data class Cart(
    val items: List<CartItem>,
    val total: Double,
    val itemCount: Int
)