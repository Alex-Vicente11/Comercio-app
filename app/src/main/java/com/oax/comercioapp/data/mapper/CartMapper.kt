package com.oax.comercioapp.data.mapper

import com.oax.comercioapp.data.dto.CartDetailedResponseDto
import com.oax.comercioapp.data.dto.CartItemDetailedDto
import com.oax.comercioapp.data.mapper.ProductMapper.toDomain
import com.oax.comercioapp.domain.model.Cart
import com.oax.comercioapp.domain.model.CartItem

/**
 * El carrito es el mapper más interesante porque construye un objeto compuesto
 * (Cart con lista de CartItems, cada uno con su Product).
 *
 * Flujo de conversión:
 * CartDetailedResponseDto
 * --> items: List<CartItemDetailedDto>
 *     --> product: ProductInfoDto
 * Se convierte a:
 *  Cart (dominio)
 *  --> items: List<CartItem> (dominio)
 *          --> product: Product (dominio)
 *
 * Cada nivel tiene su propia función de mapeo, siguiendo el principio de composición.
 * CartMapper delega en ProductMapper para los productos, en ves duplicar esta lógica aquí.
 *
 */

object CartMapper {

    /**
     * Convierte la respuesta completa del carrito a la entidad Cart del dominio.
     *
     * 'itemsCount' del DTO viene del servidor - lo usamos directamente en vez de recalcular
     * con items.size, porque el servidor puede tener lógica de conteo distinta
     * (ej: contar unidades totales vs productos únicos)
     */
    fun CartDetailedResponseDto.toDomain(): Cart {
        return Cart(
            items = items.map { it.toDomain() },
            total = total,
            itemCount = itemsCount
        )
    }

    /**
     * Convierte un ítem detallado del carrito a CartItem del dominio.
     *
     * Observa que delegemos en ProductMapper.toDomain() para el producto anidado.
     * Esto evita duplicar la lógica de conversión de ProductInfoDto -> Product
     * y garantiza que si el mapper de producto cambia, este se actualiza solo.
     * Es el principio DRY (Don't Repeat Yourself) aplicado a los mappers.
     */
    private fun CartItemDetailedDto.toDomain(): CartItem {
        return CartItem(
            cartId = idCart,
            product = product.toDomain(), // Delega en ProductMapper
            quantity = quantity,
            subtotal = subtotal,
            addedDate = addedDate
        )
    }
}