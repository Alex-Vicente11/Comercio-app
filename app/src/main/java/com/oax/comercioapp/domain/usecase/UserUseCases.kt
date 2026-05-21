package com.oax.comercioapp.domain.usecase

import com.oax.comercioapp.domain.model.User
import com.oax.comercioapp.domain.repository.IUserRepository

/**
 * Contexto: con el nuevo enfoque de app de pedidos, el usuario gestiona su cuenta (ver y editar perfil)
 * pero no administra otros usuarios.
 *
 * ProfilesViewModel original llamaba a userRepository.getUsers() y createUser() - con el nuevo
 * enfoque ese ViewModel se reemplaza por funcinalidad de historial de pedidos.
 */

class GetProfileUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Result<User> {
        return userRepository.getProfile()
    }
}

class UpdateProfileUseCase(
    private val userRepository: IUserRepository
) {
    /**
     * Actualiza el nombre de usuario con validación
     *
     * La validación de logitud máxima (50 chars) es una regla de negocio que el backend también
     * debería tener, pero validarla aquí da feedback inmediato al usuario sin necesidad de un
     * round-trip de red.
     */
    suspend operator fun invoke(newUserName: String): Result<User> {
        val trimmed = newUserName.trim()

        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
        }

        if (trimmed.length > 50) {
            return Result.failure(IllegalArgumentException("El nombre no puede exceder 50 caracteres"))
        }

        return userRepository.updateProfile(trimmed)
    }
}