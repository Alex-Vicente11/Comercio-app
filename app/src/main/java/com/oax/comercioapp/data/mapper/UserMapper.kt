package com.oax.comercioapp.data.mapper

import com.oax.comercioapp.data.dto.UserDto
import com.oax.comercioapp.data.dto.UserProfileDto
import com.oax.comercioapp.domain.model.User

/**
 * Dos funciones de mapeo porque la API devuelve al usuario de dos formas distintas según el contexto:
 *  - UserDto: viene del flujo de auth (login/register/guest)
 *  - UserProfileDto: viene del endpoint GET /profile
 *
 * Ambos se convierten al mismo User del dominio.
 *
 * Nota sobre 'token':
 * UserDto tiene token (viene del auth flow), pero User en dominio no lo tiene - el token es un
 * detalle de infraestrutura que maneja SessionManager/UserPreferences directamente.
 * UserProfileDto tampoco lo tiene porque GET /profile no devuelve token.
 * En ambos casos el mapper simplemente no lo pasa al dominio.
 */

object UserMapper {

    /**
     *  Desde el DTO de auth (login/register).
     *  'idUser' -> 'id': mismo patrón de renombrado semántico que en Product.
     */
    fun UserDto.toDomain(): User {
        return User(
            id = idUser,
            userName = userName,
            email = email,
            isGuest = isGuest
            // token se omite intencionalmente - va a SessionManager, no al dominio
        )
    }

    /**
     *  Desde el DTO de perfil (GET /profile).
     *  'createdAt' no existe en la entidad User del dominio porque la pantalla de perfil
     *  actual no lo muestra. Si en el futuro se requiere, se agrega a User en dominio y se
     *  actualiza aquí. Ese es el flujo correcto: UI necesita dato -> se agrega al dominio
     *  -> se actualiza el mapper. Nunca al revés
     */
    fun UserProfileDto.toDomain(): User {
        return User(
            id = idUser,
            userName = userName,
            email = email,
            isGuest = isGuest
            // createdAt omitido - no está en la entidad de dominio actual
        )
    }
}