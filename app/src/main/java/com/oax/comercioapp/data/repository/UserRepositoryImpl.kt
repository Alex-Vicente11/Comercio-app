package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.dto.UpdateProfileRequestDto
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.mapper.UserMapper.toDomain
import com.oax.comercioapp.domain.model.User
import com.oax.comercioapp.domain.repository.IUserRepository

/**
 * Comparando con UserRepository.kt original:
 *
 * El UserRepository original tenía getUsers(), createUser(), deleteUser()
 * - operaciones de administración que una app de cliente no debería exponer.
 * Eso es una violación de ISP y de principios de seguridad básicos:
 * si el ViewModel puede llamar deleteUser(), puede borrar usuarios por error.
 *
 * UserRepositoryImpl implementa IUserRepository que solo expone:
 *      - getProfile() -> ver mi perfil
 *      - updateProfile() -> actualizar mi nombre
 *
 * Las operaciones admin (si algún día se necesitan) irían en un AdminRepositoryImpl separado,
 * protegido por permisos de rol.
 *
 * También se corrige el error de suspend + Flow del original:
 * Las funciones aquí son simplemente suspend - retornan Result<T>,
 * no Flow<NetworkResult<T>>. Más simple, más predecible.
 */

class UserRepositoryImpl(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : IUserRepository {
    override suspend fun getProfile(): Result<User> {
        return try {
            val response = apiService.getProfile()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.user != null) {
                    Result.success(body.user.toDomain()) // UserMapper convierte UserProfileDto -> User
                } else {
                    Result.failure(Exception(body.message ?: "Profile not found"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(userName: String): Result<User> {
        return try {
            val response = apiService.updateProfile(
                UpdateProfileRequestDto(userName = userName)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success && body.user != null) {
                    val updateUser = body.user.toDomain()

                    /**
                     * Actualizar preferencias locales con el nuevo nombre confirmado por el servidor
                     * Usamos el nombre que devuelve el servidor (no el que envió el usuario)
                     * porque el servidor puede hacerlo normalizado (trim, capitalización, etc.)
                     * userPreferences.updateUserName(updateUser.userName)
                     */
                    userPreferences.updateUserName(updateUser.userName)

                    Result.success(updateUser)
                } else {
                    Result.failure(Exception(body.message ?: "Updated failed"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}