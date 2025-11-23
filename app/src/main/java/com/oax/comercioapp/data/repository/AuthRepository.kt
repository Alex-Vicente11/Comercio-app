package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.api.RetrofitClient
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.AuthResponse
import com.oax.comercioapp.data.models.GuestCreateRequest
import com.oax.comercioapp.data.models.GuestCreateResponse
import com.oax.comercioapp.data.models.LoginRequest
import com.oax.comercioapp.data.models.ProfileResponse
import com.oax.comercioapp.data.models.RegisterRequest
import com.oax.comercioapp.data.models.UpdateProfileRequest
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.models.ValidateTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * AuthRepository - Gestión de autenticación y usuarios anónimos
 *
 * Funcionalidades:
 * - Crear usuarios guest (anónimos)
 * - Registro de usuarios con email/password
 * - Login con fusión automática de carrito
 * - Validación de tokens JWT
 * - Gestión de perfil de usuario
 */

class AuthRepository (
    private val apiService: ApiService = RetrofitClient.apiService
) {

    // ======================
    // GUEST USER MANAGEMENT
    // ======================

    /**
     * Crea un usuario guest en el backend
     *
     * El guest_id se obtiene de UserPreferences (creado localmente)
     * El backend asigna un user_id al guest
     *
     * .@return Flow con resultado de creación (userId del guest)
     */

    fun createGuestUser(): Flow<NetworkResult<GuestCreateResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Obtener o crear guest_id local
            val guestId = UserPreferences.getOrCreateGuestId()

            // Crear request con guest_id
            val request = GuestCreateRequest(guestId = guestId)

            // Llamar al backend
            val response = apiService.createGuest(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    // Guardar usuario guest en UserPreferences
                    val guestUser = User(
                        idUser = body.userId,
                        userName = "Guest User",
                        email = null,
                        isGuest = true,
                        token = null
                    )
                    UserPreferences.saveUser(guestUser)

                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message))
                }
            } else {
                emit(NetworkResult.Error("Error creating guest: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error creating guest"))
        }
    }.flowOn(Dispatchers.IO)

    // ============================================
    // USER REGISTRATION
    // ============================================

    /**
     * Registra un nuevo usuario con email y password
     *
     * Si el usuario era guest, fusiona automáticamente el carrito
     *
     * .@param email Email del usuario
     * .@param password Contraseña
     * .@param userName Nombre del usuario
     * .@return Flow con resultado de registro (incluye token JWT y info de merge)
     */

    fun register(
        email: String,
        password: String,
        userName: String
    ): Flow<NetworkResult<AuthResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Obtener guest_id si existe (para merge de carrito)
            val guestId = try {
                UserPreferences.getOrCreateGuestId()
            } catch (e: Exception) {
                null
            }

            val request = RegisterRequest(
                email = email,
                password = password,
                userName = userName,
                guestId = guestId  // puede ser null si no era guest
            )

            // Llamar al backend
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.token != null) {
                    // Crear usuario autenticado
                    val authenticatedUser = User(
                        idUser = body.userId ?: 0,
                        userName = body.userName ?: userName,
                        email = body.email,
                        isGuest = false,
                        token = body.token
                    )

                    // Guardar en UserPreferences
                    UserPreferences.saveUser(authenticatedUser)

                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                emit(NetworkResult.Error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: " Network error during registration"))
        }
    }.flowOn(Dispatchers.IO)


    // ============================================
    // USER LOGIN
    // ============================================

    /**
     * Inicia sesión con email y password
     *
     * Si el usuario era guest, fusiona automáticamente el carrito
     *
     * .@param email Email del usuario
     * .@param password Contraseña
     * .@return Flow con resultado de login (incluye token JWT y info de merge)
     */

    fun login(
        email: String,
        password: String
    ): Flow<NetworkResult<AuthResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Obtener guest_id si existe
            val guestId = try {
                UserPreferences.getOrCreateGuestId()
            } catch (e: Exception) {
                null
            }

            // Crear request de login
            val request = LoginRequest(
                email = email,
                password = password,
                guestId = guestId
            )

            // Llamar al backend
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.token != null) {
                    // Crear usuario autenticado
                    val authenticatedUser = User(
                        idUser = body.userId ?: 0,
                        userName = body.userName ?: "",
                        email = body.email,
                        isGuest = false,
                        token = body.token
                    )

                    // Guardar en usar UserPreferences
                    UserPreferences.saveUser(authenticatedUser)

                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message))
                }
            } else {
                emit(NetworkResult.Error("Invalid credentials", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error during login"))
        }
    }.flowOn(Dispatchers.IO)


    // ==================
    // TOKEN VALIDATION
    // ==================

    /**
     * Valida el token JWT actual
     *
     * Útil para verificar si la sesión sigue activa al abrir la app
     * El token se envía automáticamente por AuthInterceptor
     *
     * .@return Flow con resultado de validación
     */

    fun validateToken(): Flow<NetworkResult<ValidateTokenResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            // El token se envia automaticamente por AuthInterceptor
            val response = apiService.validateToken()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.valid) {
                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message))
                }
            } else {
                emit(NetworkResult.Error("Token validation failed", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error validating toekn"))
        }
    }.flowOn(Dispatchers.IO)


    // ========================
    // USER PROFILE MANAGEMENT
    // ========================

    /**
     * Obtiene el perfil del usuario autenticado
     *
     * .@return Flow con información del perfil
     */

    fun getProfile(): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.getProfile()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.user != null) {
                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message ?: "Profile not found"))
                }
            } else {
                emit(NetworkResult.Error("Error loading profile", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error loading profile"))
        }
    }.flowOn(Dispatchers.IO)



    /**
     * Actualiza el nombre del usuario
     *
     * .@param newUserName Nuevo nombre de usuario
     * .@return Flow con resultado de actualización
     */

    fun updateProfile(newUserName: String): Flow<NetworkResult<ProfileResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val request = UpdateProfileRequest(userName = newUserName)
            val response = apiService.updateProfile(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.user != null) {
                    // Actualizar UserPreferences con nuevo nombre
                    val currentUser = User(
                        idUser = body.user.idUser,
                        userName = body.user.userName,
                        email = body.user.email,
                        isGuest = body.user.isGuest,
                        token = UserPreferences.getAuthToken()
                    )
                    UserPreferences.saveUser(currentUser)

                    emit(NetworkResult.Success(body))
                } else {
                    emit(NetworkResult.Error(body.message ?: "Updated failed"))
                }
            } else {
                emit(NetworkResult.Error("Error updating profile", response.code()))
            }
        }catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error updating profile"))
        }
    }.flowOn(Dispatchers.IO)


    // ========
    // LOGOUT
    // ========

    /**
     * Cierra sesión del usuario actual
     *
     * Limpia UserPreferences y SessionManager
     */

    fun logout() {
        try {
            UserPreferences.clearUser()
        } catch (e: Exception) {
            // Log error but do not fail
        }
    }


    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Verifica si el usuario actual es guest
     *
     * .@return true si es guest, false si es autenticado
     */

    fun isGuestUser(): Boolean {
        return try {
            UserPreferences.isGuest()
        } catch (e: Exception) {
            true // Default a guest si hay error
        }
    }


    /**
     * Obtiene el user_id actual
     *
     *. @return ID del usuario, o null si no hay sesión
     */

    fun getCurrentUserId(): Int? {
        return try {
            val userId = UserPreferences.getUserId()
            if (userId > 0) userId else null
        } catch (e: Exception){
            null
        }
    }


    /**
     * Verifica si hay una sesión activa
     *
     * .@return true si hay usuario logueado (guest o autenticado)
     */

    fun isLoggedIn(): Boolean {
        return try {
            UserPreferences.isLoggedIn()
        } catch (e: Exception) {
            false
        }
    }
}