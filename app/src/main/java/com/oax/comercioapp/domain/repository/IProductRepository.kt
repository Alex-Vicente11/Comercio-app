package com.oax.comercioapp.domain.repository

import com.oax.comercioapp.domain.model.Product

/**
 * ¿Por qué las operaciones son suspend?
 * Todas las operaciones que van a la red o la base de datos deben ser suspend functions.
 * Esto es parte del contrato, quien implemente esta interfaz DEBE hacer la operación
 * de forma asíncrona y no bloquear el hilo principal (Main thread). El ViewModel las llama
 * desde un coroutine scope (viewModelScope) automáticamente.
 *
 * Nota sobre el naming:
 * 'getProducts' en vez de 'fetchProducts' o 'loadProducts' - en las interfaces de dominio
 * se usan verbos simples orientados al negocio, no a la implementación técnica
 * (fetch/load implican red, y la interfaz no debería saber si los datos vienen de red o caché).
 */

interface IProductRepository {

    /**
     * Obtiene el catálogo completo de productos disponibles.
     * La implementación decidirá si va a la red, a caché, o a ambos.
     */
    suspend fun getProducts(): Result<List<Product>>

    /**
     * Obtiene el detalle de un producto especifico por su ID.
     *
     * @param product ID del producto a consultar
     * @return Result.failure con NoSuchElementException si el producto
     *      no existe, o con IOException si hay error de red
     */
    suspend fun getProductById(productId: Int): Result<Product>
}