package com.oax.comercioapp.domain.model

/**
 * El Product.kt original tiene @SerializedName en cada campo, lo que
 * lo acopla directamente a Gson y a la estructura Json de la API.
 *
 * Problema concreto: si ocurre un cambio de Gson a Kotlinx Serialization,
 * o si la API evoluciona, tendría que modificar la clase que usan los ViewModels,
 * Adapters y toda la UI - un cambio de infraestructura que no debería impactar
 * al dominio. Esto es exactamente el problema que resuelve la Dependency Inversion (DIP de SOLID)
 *
 * También: el campo se llamaba 'product' (mismo nombre que la clase),
 * lo que es confuso. Se renombra a 'name' porque en el dominio nos importa
 * el concepto, no cómo lo llama la base de datos.
 *
 * Sobre el tipo de 'price':
 * El original usaba Double. Para precios en una app de e-commerce real se recomienda BigDecimal
 * para evitar errores de punto flotante (ej: 10.1 + 0.2 = 10.299999... con Double).
 * Lo dejamos como Double por ahora para no compilar la migración,
 * pero es una mejora que se debe considerar.
 */

data class Product(
    val id: Int,
    val name: String,
    val price: Double
)