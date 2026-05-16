package com.oax.comercioapp.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs de usuario y perfil
 *
 * Observación sobre UserRepository.kt original:
 * Exponía getUsers(), createUser(), deleteUser() - operaciones CRUD
 * de administración que una app de cliente no debería usar.
 * Eso sugiere que fue creado como wrapper directo de la API sin filtrar contexto de negocio
 * (lo que el usuario de la app realmente puede hacer vs lo que la API técnicamente permite).
 *
 * En la refactorización, IUserRepository solo expone getProfile() y updateProfile - lo que un
 * usuario normal de la app necesita. Las operaciones de admin (si se necesitan) irían en un
 * IAdminRepository separado, siguiendo ISP.
 *
 * UserDto aquí representa al usuario tal como viene de la API, con @SerializedName.
 * El User en domain/model/ no tiene ninguna anotación - esa es la separación que se hace
 */

data class UserDto(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("user_name")
    val userName: String,
    val email: String? = null,
    @SerializedName("is_guest")
    val isGuest: Boolean = false,
    val token: String? = null
)

data class UserProfileDto(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("user_name")
    val userName: String,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    @SerializedName("created_at")
    val createdAt: String?
)

data class ProfileResponseDto(
    val success: Boolean,
    val user: UserProfileDto?,
    val message: String?
)

data class UpdateProfileRequestDto(
    @SerializedName("user_name")
    val userName: String
)

// --- Requests CRUD (solo para referencia, no usar en cliente normal)
/**
 * Estos DTOs corresponden a operaciones de administración que el UserRepository original exponía
 * pero que la app cliente no debería usar. Se conservan aquí documentados por si se necesita
 * un módulo admin futuro, pero NO tienen interfaz de dominio correspondiente por ahora.
 */
data class UserRequestDto(
    @SerializedName("user_name")
    val userName: String
)

data class UserResponseDto(
    val message: String,
    val id: Int? = null,
    @SerializedName("user_name")
    val userName: String? = null
)
