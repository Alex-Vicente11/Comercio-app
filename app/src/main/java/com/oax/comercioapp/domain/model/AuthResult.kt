package com.oax.comercioapp.domain.model

/**
 * En el ApiResponses.kt original, 'AuthResponse' mezcla:
 *   - El resultado de negocio (¿se autenticó? ¿quién es?)
 *   - Detalles de infraestructura (cartMigrated, cartItemsCount, message)
 *   - El token JWT (debería ser manejado por una capa de seguridad/sesión)
 *
 *   Pregunta de dominio: "¿El login fue exitoso y quién es el usuario?"
 *   Todoo lo demás es un detalle de implementación de la API
 *
 *   ¿Por qué 'token' Si aparece aquí si antes dijimos que era infraestructura?
 *   Porque el token es necesario para que el dominio sepa que la sesión es válida
 *   y para pasárselo al SessionManager. Es un caso limite aceptable.
 *   Una arquitectura más estricta lo manejaría en un TokenRepository separado,
 *   pero para esta app ese nivel de separación sería sobre-ingeniería.
 *
 *   'cartMigrated' y 'cartItemsCount' del AuthResponse original:
 *   Esos son efectos secundarios de infraestructura (migración de carrito de invitado).
 *   No pertenecen al resultado de autenticación desde el punto de vista del dominio.
 *   El repositorio puede manejar ese efecto internamente.
 */

data class AuthResult(
    val userId: Int,
    val userName: String,
    val email: String?,
    val isGuest: Boolean,
    val token: String
)