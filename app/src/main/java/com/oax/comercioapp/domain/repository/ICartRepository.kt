package com.oax.comercioapp.domain.repository

import com.oax.comercioapp.domain.model.Cart
import com.oax.comercioapp.domain.model.CartItem

/**
 * Observación sobre el Cart.kt original:
 * Se tenía 'CartRequest' y 'CartUpdateRequest' mezclados con los modelos.
 * Los objetos de request son DTOs de red - pertenecen a data/dto/, no al dominio.
 * Aquí la interfaz habla en términos de negocio:
 * "agregar producto X con cantidad Y", no "enviar CartRequest a /api/cart"
 *
 * Esto es el principio de Abstracción: el dominio no sabe que existe
 * una API REST, solo sabe que puede agregar/quitar/consultar el carrito
 *
 * ¿Por qué 'getCart()' retorna Cart y no List<CartItem>?
 * Porque Cart es el Aggregate Root - incluye el total calculado y el conteo
 * de ítems que la pantalla necesita. Si retornará solo la lista, el ViewModel
 * tendría que calcular el total, y eso es lógica de negocio
 * que no debería vivir en la capa de presentación
 */
interface ICartRepository {

    /**
     * Obtiene el carrito completo del usuario actual con todos sus ítems,
     * el total calculado y el conteo de productos únicos.
     */
    suspend fun getCart(): Result<Cart>

    /**
     * Agrega un producto al carrito o incrementa su cantidad si ya existe.
     *
     * @param productId ID del producto a agregar
     * @param quantity Cantidad a agregar (mínimo 1)
     * @return El CartItem actualizado/creado
     */
    suspend fun addToCart(productId: Int, quantity: Int): Result<CartItem>

    /**
     * Actualiza la cantidad de un producto ya existente en el carrito.
     * Si quantity llega a 0, el comportamiento esperado es eliminarlo
     * (aunque la implementación puede optar por requerir removeFromCart).
     *
     * @param productId ID del producto a actualizar
     * @param quantity Nueva cantidad deseada
     */
    suspend fun updateCartItem(productId: Int, quantity: Int): Result<CartItem>

    /**
     * Elimina un producto del carrito completamente, sin importar la cantidad
     *
     * @param productId ID del producto a eliminar
     */
    suspend fun removeFromCart(productId: Int): Result<Unit>

    /**
     * Obtiene solo el conteo total de ítems en el carrito.
     * Útil para el badge del ícono del carrito en el toolbar,
     * sin necesidad de cargar todos los detalles.
     */
    suspend fun getCartCount(): Result<Int>

    /**
     * Migra el carrito de un usuario invitado al usuario autenticado.
     * Se llama automáticamente después del login/registro cuando había
     * sesión de invitado activa.
     *
     * @param guestId UUID del invitado cuyo carrito se va a migrar
     */
    suspend fun mergeGuestCart(guestId: String): Result<Int>
}