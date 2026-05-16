package com.oax.comercioapp.domain.repository

import com.oax.comercioapp.domain.model.User

/**
 * Separación respecto a IAuthRepository - ¿por qué son dos interfaces distintas?
 * Este es el principio I de SOLID: Interface Segregation Principle (ISP).
 * "Los clientes no deben depender de interfaces que no usan"
 *
 * - AuthRepository: responsable del CICLO de AUTENTICACION
 * (login, register, token, sesión de invitado)
 *
 * - IUserRepository: responsable de la GESTIÓN del PERFIL
 * (ver perfil, actualizar nombre)
 *
 * Si los uniéramos en una sola interfaz, ProfileViewModel tendría que depender
 * de métodos de login que nunca usa, y AuthViewModel tendría que depender
 * de updateProfile que tampoco usa.
 * Con ISP, cada ViewModel depende solo de lo que realmente necesita.
 *
 * Analogía: es como tener un contrato de "cocinero" separado del contrato de "cajero"
 * aunque los dos trabajen en el mismo restaurante
 */
interface IUserRepository {
    /**
     * Obtiene el perfil completo del usuario actualmente autenticado
     * La implementación sabe qué token usar (lo obtiene de SessionManager o de
     * preferencias locales)
     */
    suspend fun getProfile(): Result<User>

    /**
     * Actualiza el nombre de usuario en el servidor
     *
     * @param userName Nuevo nombre de usuario deseado
     * @return El User actualizado con los datos confirmados por el servidor
     */
    suspend fun updateProfile(userName: String): Result<User>
}