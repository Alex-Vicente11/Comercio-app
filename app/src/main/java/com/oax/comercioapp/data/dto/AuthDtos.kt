package com.oax.comercioapp.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs de autenticación
 *
 * ¿Qué es un DTO? (Data Transfer Object)
 * Es un objeto cuya Única responsabilidad es transportar datos entre la app y la API.
 * No tiene lógica de negocio, solo estructura de red.
 *
 * Antes: User.kt AuthResponse, etc. vivían en data/models/ mezclados con objetos de dominio.
 * Un cambio en la API (ej: renombrar "user_name") a "username" obligaba a modificar clases
 * que usa toda la app.
 *
 * Ahora: los DTOs absorben ese cambio. Solo el mapper se actualiza.
 *        El dominio (y toda la UI) no se entera de nada.
 *
 * Nota sobre los nombres:
 * Sufijo "Dto" para dejar claro que son objetos de transporte.
 * Sufijo "Request" para los que se ENVÍAN a la API
 * Sufijo "Response" para los que se RECIBEN de la API
 */

// ----- Request (lo que enviamos) -----
data class GuestCreateRequestDto(
    @SerializedName("guest_id")
    val guestId: String
)

data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerializedName("guest_id")
    val guestId: String?
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("guest_id")
    val guestId: String?
)

// ---- Response (lo que recibimos) ----
data class GuestCreateResponseDto(
    val success: Boolean,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val message: String
)

/**
 * AuthResponseDto - respuesta de login/register
 *
 * Observación importante como apredizaje:
 * 'cartMigrated' y 'cartItemsCount' son efectos secundarios de infraestructura
 * (migración de carrito invitado). En el dominio no nos importan - el AuthRepositoryImpl los puede
 * usar internamente para decidir si llamar a mergeGuestCart, pero NUNCA suben al ViewModel
 * Eso es exactamente la barrera que pone el mapper
 */

data class AuthResponseDto(
    val success: Boolean,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("user_name")
    val userName: String?,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val token: String?,
    @SerializedName("cart_migrated")
    val cartMigrated: Boolean,
    @SerializedName("cart_items_count")
    val cartItemsCount: Int,
    val message: String
)

data class ValidateTokenResponseDto(
    val success: Boolean,
    val valid: Boolean,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("user_name")
    val userName: String?,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val message: String
)
