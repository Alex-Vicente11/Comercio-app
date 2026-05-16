package com.oax.comercioapp.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs de productos
 *
 * Comparando con el Product.kt original:
 * El campo se llamaba 'product' (mismo nombre que la clase) y era a la vez DTO de red y entidad
 * de dominio. Eso causaba que en el adapter se escriba 'product.product' - confuso y frágil
 *
 * Aquí el DTO conserva el nombre exacto que usa la API ('product')
 * porque eso es lo que viene en el JSON. El mapper lo convierte a 'name'
 * al crear la entidad de dominio Product.
 *
 * ProductRequestDto y ProductUpdateRequestDto:
 * Antes vivían en Product.kt mezclados con el modelo. Ahora tienen su lugar correcto - son objetos
 * de red, va en data/dto/
 * Si la app de cliente nunca crea ni edita productos (solo los consulta),
 * estos DTOs se pueden eliminar en el futuro sin tocar el dominio
 */

data class ProductDto(
    @SerializedName("id_product")
    val idProduct: Int,
    @SerializedName("product")
    val product: String,   // La API lo llama "product", el dominio lo llamará "name"
    val price: Double
)

data class ProductRequestDto(
    @SerializedName("product")
    val product: String,
    val price: Double
)

data class ProductUpdateRequestDto(
    @SerializedName("product")
    val product: String? = null,
    val price: Double? = null
)

data class ProductResponseDto(
    val message: String,
    val id: Int? = null
)
