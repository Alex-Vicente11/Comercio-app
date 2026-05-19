package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.dto.GuestCreateRequestDto
import com.oax.comercioapp.data.dto.LoginRequestDto
import com.oax.comercioapp.data.dto.RegisterRequestDto
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.mapper.AuthMapper.toDomain
import com.oax.comercioapp.domain.model.AuthResult
import com.oax.comercioapp.domain.repository.IAuthRepository

/**
 * ¿Por qué se llama "Impl"?
 * Porque implementa la interfaz IAuthRepository del dominio
 * Es la convencion estándar en Android/Clean Architecture.
 * El ViewModel solo conoce IAuthRepository - nunca esta clase directamente
 * Después Hilt inyectará AuthRepositoryImpl donde se pida IAuthRepository
 *
 * Comparando con AuthRepository.kt original:
 *
 * ANTES (problemas):
 *  - Retornaba Flow<NetworkResult<AuthResponse>> - la UI procesaba DTOs de red
 *  - Guardaba User (con @SerializedName) en UserPreference desde aquí
 *  - getProfile() y updateProfile() mezclados con login/register
 *  - isGuestUser(), getCurrentUserId(), isLoggedIn() como métodos del repositorio
 *    (esas son responsabilidades de SessionManager, no del repositorio de auth)
 *  - RetrofitClient.apiService como default en constructor (imposible testear)
 *
 * AHORA (mejoras):
 *  - Retorna Result<AuthResult> - la UI recibe entidades de dominio limpias
 *  - El mapper convierte el DTO antes de devolver el resultado
 *  - Responsabilidad única: solo autenticación
 *  - ApiService inyectado por constructor (testeable con mocks)
 *
 * Sobre la persistencia en UserPreferences:
 * El repositorio sigue guardando en UserPreferences porque es el lugar correcto
 * - es infraestructura local que el repositorio coordina.
 * Lo que cambia es que ya no guarda User con @SerializedName sino que usa los datos
 * de AuthResult (entidad de dominio ya mapeada)
 */

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences // Inyectado, no estático
): IAuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        guestId: String?
    ): Result<AuthResult> {
        return try {
            val response = apiService.login(
                LoginRequestDto(
                    email = email,
                    password = password,
                    guestId = guestId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Validar antes de mapear - si el token falta, la API falló su contrato
                if (body.success && body.token != null) {
                    val authResult = body.toDomain()     // <- Mapper hace la conversión
                    userPreferences.saveAuthResult(authResult)
                    Result.success(authResult)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGuestSession(guestId: String): Result<AuthResult> {
        return try {
            val response = apiService.createGuest(GuestCreateRequestDto(guestId = guestId))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    val authResult = body.toDomain()
                    userPreferences.saveAuthResult(authResult)
                    Result.success(authResult)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                Result.failure(Exception("Guest session failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        userName: String,
        guestId: String?
    ): Result<AuthResult> {
        return try {
            val response = apiService.register(
                RegisterRequestDto(
                    email = email,
                    password = password,
                    userName = userName,
                    guestId = guestId
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.token != null) {
                    val authResult = body.toDomain()
                    userPreferences.saveAuthResult(authResult)
                    Result.success(authResult)
                } else {
                    Result.failure(Exception(body.message))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateToken(token: String): Result<Boolean> {
        return try {
            // El token se envía automáticamente por AuthInterceptor
            val response = apiService.validateToken()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body.valid)
            } else {
                Result.failure(Exception("Token validation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}