package com.oax.comercioapp.domain.model

/**
 * User.kt original mezcla dos responsabilidades:
 *      1. Representar al usuario como lo entiende la APP (dominio)
 *      2. Mapear campos de la API con @SerializedName (infraestructura)
 *
 * Esto viola el Principio de Responsabilidad Única
 * Una clase debe tener una sola razón para cambiar. Si la API cambia
 * su campo "user_name" a "username, estarias modificando una clase
 * que la lógica de negocio usa - y eso no debería ocurrir.
 *
 * Domain Layer:
 * Ninguna clase aquí puede importar Gson, Retrofit Room ni Android.
 * Solo kotlin puro. Esto garantiza que el dominio sea testeable
 * sin un emulador ni dependencias externas.
 *
 * Diferencia respecto al User.kt original:
 *  - Sin @SerializedName -> eso es responsabilidad del DTO en data/
 *  - Sin token -> el token es un detalle de autenticación (infraestructura),
 *  - 'id' en vez de 'idUser' nombres de dominio, no de base de datos
 *  - 'isGuest' se mantiene porque sí es lógica de negocio (afecta qué
 *  puede hacer el usuario en la app)
 */

data class User(
    val id: Int,
    val userName: String,
    val email: String?,
    val isGuest: Boolean
)

