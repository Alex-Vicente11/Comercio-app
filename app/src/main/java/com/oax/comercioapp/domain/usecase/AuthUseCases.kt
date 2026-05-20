package com.oax.comercioapp.domain.usecase

import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.domain.model.AuthResult
import com.oax.comercioapp.domain.repository.IAuthRepository

/**
 * ¿Qué es un Use Case?
 * Es una clase que encapsula UNA operación de negocio específica.
 * Su única responsabilidad es ejecutar esa operación y retornar el resultado.
 *
 * ¿Por qué sacar esta lógica del ViewModel?
 * El AuthViewModel original tenía mezclado:
 *   1. Validación de formulario (isValidEmail, isValidPassword)
 *   2. Lógica de negocio (obtener guestId, construir User, decidir qué hacer)
 *   3. Coordinación de estado de UI (_loginState, _registerState)
 *
 * Con Use Cases:
 *   - Use Case  -> lógica de negocio (reutilizable, testeable sin Android)
 *   - ViewModel -> solo coordina el estado de UI y llama al use case
 *
 * Ventaja concreta: si se agrega una pantalla de login biométrico, reutiliza LoginUseCase sin
 * copiar lógica. Si el ViewModel la tuviera, tendría que duplicarla o acoplar los ViewModel
 * entre sí.
 *
 * Patrón de invoación con operador fun invoke():
 * Permite llamar el use case como si fuera una función:
 *   loginUseCase.execute(email, password, guestId)
 * Es idiomático en kotlin y Clean Architecture para Android.
 */

// Login
class LoginUseCase(
    private val authRepository: IAuthRepository,
    private val userPreferences: UserPreferences
) {
    /**
     * Ejecuta el login con email y contraseña.
     * Responsabilidades de este use case:
     *   1. Validar formato de email y contraseña (reglas de negocio)
     *   2. Obtener el guestId si existe (para migrar el carrito)
     *   3. Delegar al repositorio y retornar el resultado
     *
     * Lo que No hace:
     *   - Actualizar UI (eso es del ViewModel)
     *   - Guardar en preferencias (eso ya lo hace el repositorio)
     *   - Conocer Retrofit o la API
     * @return Result.failure con mensaje legible si la validación falla,
     *         Result del repositorio si pasa la validación.
     *
     * @return Result.failure con mensaje legible si la validación falla,
     *         Result del repositorio si pasa la validación
     */
    suspend operator fun invoke(email: String, password: String): Result<AuthResult> {
        // Validaciones de negocio - no de UI
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("El formato del email no es válido"))
        }

        if (!isValidPassword(password)) {
            return Result.failure(
                IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número"
                )
            )
        }

        // Obtener guestId para migrar el carrito si había sesión de invitado
        val guestId = if (userPreferences.isGuest()) {
            userPreferences.getOrCreateGuestId()
        } else null

        return authRepository.login(email, password, guestId)
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isDigit() }
}

// Register
class RegisterUseCase(
    private val authRepository: IAuthRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        userName: String
    ): Result<AuthResult> {
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("El formato del email no es válido"))
        }

        if (!isValidPassword(password)) {
            return Result.failure(
                IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número"
                )
            )
        }
        if (userName.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("El nombre de usuario es requerido"))
        }

        val guestId = if (userPreferences.isGuest()) {
            userPreferences.getOrCreateGuestId()
        } else null

        return authRepository.register(email, password, userName.trim(), guestId)
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isDigit() }
}

// Validate Token
/**
 * Verifica si la sesión actual sigue siendo válida.
 *
 * Lógica de negocio importante aquí:
 * Antes de hacer el call de red, verifica localmente si hay token.
 * Esto evita un round-trip innecesario a la API cuando ni siquiera hay sesión activa -
 * y eso es lógica de negocio, no de UI.
 */
class ValidateSessionUseCase(
    private val authRepository: IAuthRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(): Result<Boolean> {
        val token = userPreferences.getAuthToken()

        // Sin token no hay sesión que validar - retornar false sin llamar a red
        if (token.isNullOrEmpty()) {
            return Result.success(false)
        }

        // Con token, verificar con el servidor si sigue siendo válido
        return authRepository.validateToken(token)
    }
}

// Create Guest Session
class CreateGuestSessionUseCase(
    private val authRepository: IAuthRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(): Result<AuthResult> {
        val guestId = userPreferences.getOrCreateGuestId()
        return authRepository.createGuestSession(guestId)
    }
}

// Logout
/**
 *  Logout no es suspend - no hace llamadas de red.
 *  Solo limpia el estado local. Si la API tiene un endpoint de logout, se puede agregar opcionalmente,
 *  pero no debe bloquear el flujo si falla (el usuario debe poder salir igual).
 */
class LogoutUseCase(
    private val userPreference: UserPreferences
) {
    operator fun invoke() {
        userPreference.clearUser()
    }
}
