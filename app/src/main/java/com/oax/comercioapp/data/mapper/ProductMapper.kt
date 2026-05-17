package com.oax.comercioapp.data.mapper

import com.oax.comercioapp.data.dto.ProductDto
import com.oax.comercioapp.data.dto.ProductInfoDto
import com.oax.comercioapp.domain.model.Product

/**
 * Este mapper resuelve un problema concreto en ProductAdapter:
 *  binding.textProductName.text = product.product <- confuso
 *
 * La API llama al campo 'product' (nombre del producto).
 * El dominio lo llama "name" (semántica clara).
 * El mapper hace esa traducción una sola vez aquí.
 * En el adapter futuro será:
 *  binding.textProductName.text = product.name <-claro
 *
 * También tenemos dos funciones de mapeo porque la API devuelve productos de 2 formas distintas:
 *      1. ProductoDto -> desde el endpoint GET / products
 *      2. ProductInfoDto -> desde el endpoint GET /cart (producto anidado)
 * Ambos se convierten al mismo Product del dominio. La UI no sabe de dónde vino el
 * producto - solo lo muestra.
 */

object ProductMapper {

    /**
     * Convierte ProductDto (catálogo de productos) a entidad de dominio.
     * 'idProduct' -> 'id', 'product' -> 'name': renombrado semántico.
     */
    fun ProductDto.toDomain(): Product {
        return Product(
            id = idProduct,
            name = product,  // La API dice "product", el dominio dice "name"
            price = price
        )
    }

    /**
     * Convierte ProductInfoDto (product anidado en carrito) a entidad de dominio.
     *
     * ¿Por qué esta función existe si ProductInfoDto y ProductDto son iguales?
     * Porque son tipos DISTINTOS que la API puede cambiar de forma independiente.
     * Si después el endpoint de carrito agrega un campo "discount" al producto anidado,
     * solo cambia ProductInfoDto y este mapper - sin tocar ProductDto ni el mapper del catálogo.
     * Esto es el Open/Closed Principle (OCP de SOLID):
     * abierto para extensión, cerrado para modificación.
     */
    fun ProductInfoDto.toDomain(): Product {
        return Product(
            id = idProduct,
            name = product,
            price = price
        )
    }

    /**
     * Convierte una lista de ProductDto a lista de entidades de dominio.
     * Función de conveniencia para el caso más común (GET /products)
     */
    fun List<ProductDto>.toDomain(): List<Product> = map { it.toDomain() }
}