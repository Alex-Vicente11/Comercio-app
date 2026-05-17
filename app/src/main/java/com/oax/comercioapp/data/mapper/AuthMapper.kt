package com.oax.comercioapp.data.mapper

import com.oax.comercioapp.data.dto.AuthResponseDto
import com.oax.comercioapp.data.dto.GuestCreateResponseDto
import com.oax.comercioapp.domain.model.AuthResult

/**
 * ¿Qué es un Mapper y por qué existe?
 * Es el traductor entre el lenguaje de la API (DTOs) y el lenguaje del dominio (entidades).
 * Su única responsabilidad es convertir de una representación a otra - SRP
 *
 * ¿Por qué no hacemos esta conversión dentro del repositorio?
 * Porque el repositorio ya tiene responsabilidad suficiente:
 * hacer la llamada de red y manejar errores. Si además convierte los datos,
 * tiene dos razones para cambiar (SRP violado).
 * El mapper tiene UNA razón para cambiar: si cambia la estructura del DTO o de la
 * entidad de dominio
 *
 * ¿Por qué funciones de extensión (fun AuthResponseDto.toDomain())?
 * Es una convención idiomática de Kotlin. Permite escribir:
 *  val authResult = responseDto.toDomain()
 * En vez de:
 *  val authResult = AuthMapper.map(responseDto)
 * Es más legible y no requiere instanciar el mapper.
 *
 * Alternativa: object AuthMapper con fun map(...) - también válido,
 * pero las extension functions son mas "kotlinicas"
 */

object AuthMapper {
    /**
     * Convierte AuthResponseDto (respuesta de login/register de la API)
     * a AuthResult (entidad del dominio).
     *
     * Observar se que SE PIERDE en la conversión intencional:
     * - CartMigrated -> detalle de infraestructura, el ViewModel no lo necesita
     * - cartItemsCount -> idem
     * - message -> es un mensaje de la API, no del dominio
     *
     * Observar lo que FALLA explícitamente:
     * Si token es null o userId es null, el repositorio debe haber validado antes de llamar al mapper.
     * Si llega aquí con null, es un error de programación - por eso usamos !! con intención.
     * En producción podría lanzar un exception custom aquí.
     */
    fun AuthResponseDto.toDomain(): AuthResult {
        return AuthResult(
            userId = userId ?: error("AuthResponseDto llegó sin userId - revisar validación en repositorio"),
            userName = userName ?: error("AuthResponseDto llegó sin userName"),
            email = email,
            isGuest = isGuest,
            token = token ?: error("AuthResponseDto llegó sin token - revisar validación en repositorio")
        )
    }

    /**
     * Convierte GuestCreateResponseDto a AuthResult
     *
     * Un usuario guest también produce un AuthResult porque desde el punto de vista del dominio,
     * "hay alguien usando la app" - sea invitado o autenticado. El ViewModel no necesita saber
     * la diferencia en términos de estructura, solo en términos de isGuest = true
     *
     * El token del guest es null porque los guests no tienen JWT.
     * En el dominio modelamos esto con un token vacio ("") para no romper el tipo no-nullable
     * de AuthResult.token
     * Nota futura: si los guests llegan a tener token, solo cambiar esta línea - el dominio no se entera
     */
    fun GuestCreateResponseDto.toDomain(guestUserName: String = "Guest User"): AuthResult {
        return AuthResult(
            userId = userId,
            userName = guestUserName,
            email = null,
            isGuest = true,
            token = "" // Guest no tienen JWT - verificar y ver nota arriba
        )
    }
}
