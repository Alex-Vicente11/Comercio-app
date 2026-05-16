package com.oax.comercioapp.domain.repository

import com.oax.comercioapp.domain.model.AuthResult

/**
 * ¿Por qué hacerlo interfaz y no directamente la implementación?
 * Se aplica el principio: Dependency Inversion Principle (DIP)
 *
 * Sin DIP (lo que se tiene ahora):
 *  AuthViewModel -> AuthRepository (clase concreta en data/)
 *  El ViewModel conoce Retrofit, sabe como se llama la API. Si se cambia
 *  de Retrofit a Ktor, se tiene que modificar el ViewModel también.
 *
 *  Con DIP:
 *      AutViewModel -> IAuthRepository (interfaz en domain/)
 *      AuthRepositoryImpl -> IAuthRepository (implemetación en data/)
 *      El ViewModel solo sabe QUÉ puede hacer, nunca CÓMO se hace.
 *      Posibilidad de cambiar Retrofit por Ktor sin tocar un solo ViewModel
 *
 * ¿Por qué Result<T> como tipo de retorno?
 * El AuthRepository original probablemente lanza excepciones o retorna null cuando algo falla.
 * Eso obliga al ViewModel a usar try/catch, lo que es lógica de manejo de errores dispersa por toda la app.
 *
 *  Result<T> de Kotlin hace el error Explícito en el tipo:
 *      - Result.success(authResult) -> todoo bien
 *      - Result.failure(exception) algo falló, con el motivo
 *  El ViewModel siempre sabe que puede llegar un fallo y lo maneja
 *  en un solo lugar con .onSuccess { } / .onFailure { }.
 *
 *  IMPORTANTE: Esta interfaz no sabe nada de Retrofit, Gson ni HTTP.
 *  Solo habla el lenguaje de negocio: "hacer login", "registrar usuario"
 */
interface IAuthRepository {
    /**
     * Intenta autenticar al usuario con email y contraseña
     *
     * @param email Correo del usuario
     * @param password Contraseña en texto plano (el cifrado es responsabilidad de la implemetnación
     *      en data/, no del dominio)
     * @param guestId ID del invitado actual para migrar su carrito, si existe
     * @return Result.success con AuthResult si el login fue exitoso,
     *         Result.failure con la excepción si falló
     */

    suspend fun login(
        email: String,
        password: String,
        guestId: String?
    ): Result<AuthResult>

    /**
     * Registra un nuevo usuario.
     *
     * @param email Correo del nuevo usuario
     * @param password Contraseña elegida
     * @param userName Nombre de usuario
     * @param guestId ID del invitado para migrar carrito, si aplica
     */
    suspend fun register(
        email: String,
        password: String,
        userName: String,
        guestId: String?
    ): Result<AuthResult>

    /**
     * Valida si el token de sesión actual sigue siendo válido.
     * Útil para verificar al abrir la app si la sesión expiró.
     *
     * @param token El JWT almacenado localmente
     * @return Result.success(true) si el token es válido,
     *         Result.success(false) si expiró,
     *         Result.failure si hubo error de red
     */
    suspend fun validateToken(token: String): Result<Boolean>

    /**
     * Crea una sesión de invitado.
     * @param guestId UUID generado localmente para identificar al invitado
     */
    suspend fun createGuestSession(guestId: String): Result<AuthResult>
}