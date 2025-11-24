package com.oax.comercioapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.AuthResponse
import com.oax.comercioapp.data.models.GuestCreateResponse
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.AuthRepository
import com.oax.comercioapp.utils.SessionManager
import com.oax.comercioapp.utils.SessionManager.logout
import kotlinx.coroutines.launch

/**
 * AuthViewModel - ViewModel para autenticación de usuarios
 *
 * Funcionalidades:
 * - Crear usuarios guest
 * - Registro con email/password
 * - Login con fusión automática de carrito
 * - Validación de tokens JWT
 * - Logout
 */



class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
): ViewModel() {

    // Estado de creacion de guest
    private val _guestCreateState = MutableLiveData<NetworkResult<GuestCreateResponse>>()
    val guestCreateRequest: LiveData<NetworkResult<GuestCreateResponse>> = _guestCreateState

    // Estado de registro
    private val _registerState = MutableLiveData<NetworkResult<AuthResponse>>()
    val registerState: LiveData<NetworkResult<AuthResponse>> = _registerState

    // Estado de login
    private val _loginState = MutableLiveData<NetworkResult<AuthResponse>>()
    val loginState: LiveData<NetworkResult<AuthResponse>> = _loginState

    // Estado de validacion de token
    private val _tokenValidationState = MutableLiveData<NetworkResult<Boolean>>()
    val tokenValidationState: LiveData<NetworkResult<Boolean>> = _tokenValidationState

    // Informacion de fusion de carrito
    private val _cartMergeInfo = MutableLiveData<Pair<Boolean, Int>>()
    val cartMergeInfo: LiveData<Pair<Boolean, Int>> = _cartMergeInfo


    // ============================================
    // GUEST USER CREATION
    // ============================================

    /**
     * Crea un usuario guest en el backend
     *
     * Se debe llamar al iniciar la app si no hay sesión
     */
    fun createGuestUser() {
        viewModelScope.launch {
            authRepository.createGuestUser().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Guest creado, guardar en SessionManager
                        val guestUser = User(
                            idUser = result.data.userId,
                            userName = "Guest User",
                            email = null,
                            isGuest = true,
                            token = null
                        )
                        SessionManager.login(guestUser)
                        _guestCreateState.postValue(result)
                    }

                    is NetworkResult.Error -> {
                        _guestCreateState.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _guestCreateState.postValue(result)
                    }
                }
            }
        }
    }

    // ============================================
    // REGISTRO DE USUARIOS
    // ============================================

    /**
     * Registra un nuevo usuario con email y password
     *
     * @param email Email del usuario
     * @param password Contraseña
     * @param userName Nombre del usuario
     */

    fun register(email: String, password: String, userName: String) {
        // validacion basica
        if (!isValidEmail(email)) {
            _registerState.postValue(NetworkResult.Error("Email invalido"))
            return
        }

        if (!isValidPassword(password)) {
            _registerState.postValue(NetworkResult.Error("Contraseña debe tener al menos 8 caracteres"))
            return
        }

        if (userName.trim().isEmpty()) {
            _registerState.postValue(NetworkResult.Error("Nombre de usuario requerido"))
            return
        }

        viewModelScope.launch {
            authRepository.register(email, password, userName).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val response = result.data

                        // Crear usuario autenticado
                        val authenticatedUser = User (
                            idUser = response.userId ?: 0,
                            userName = response.userName ?: userName,
                            email = response.email,
                            isGuest = false,
                            token = response.token
                        )

                        // Actualizar SessionManager
                        SessionManager.login(authenticatedUser)

                        // Informacion de merge de carrito
                        if (response.cartMigrated) {
                            _cartMergeInfo.postValue(Pair(true, response.cartItemsCount))
                        }

                        _registerState.postValue(result)
                    }

                    is NetworkResult.Error -> {
                        _registerState.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _registerState.postValue(result)
                    }
                }
            }
        }
    }

    // ============================================
    // LOGIN DE USUARIOS
    // ============================================

    /**
     * Inicia sesión con email y password
     *
     * @param email Email del usuario
     * @param password Contraseña
     */

    fun login(email: String, password: String) {
        // Validacion basica
        if (!isValidEmail(email)) {
            _loginState.postValue(NetworkResult.Error("Email inválido"))
            return
        }

        if (password.isEmpty()) {
            _loginState.postValue(NetworkResult.Error("Contraseña requerida"))
            return
        }

        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val response = result.data

                        // Crear usuario autenticado

                        val authenticatedUser = User(
                            idUser = response.userId ?: 0,
                            userName = response.userName ?: "",
                            email = response.email,
                            isGuest = false,
                            token = response.token
                        )

                        // Actualizar SessionManager
                        SessionManager.login(authenticatedUser)

                        // Informacion de merge de carrito
                        if (response.cartMigrated) {
                            _cartMergeInfo.postValue(Pair(true, response.cartItemsCount))
                        }

                        _loginState.postValue(result)
                    }

                    is NetworkResult.Error -> {
                        _loginState.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _loginState.postValue(result)
                    }
                }
            }
        }
    }

    // ============================================
    // VALIDACIÓN DE TOKEN
    // ============================================

    /**
     * Valida el token JWT actual
     *
     * Se debe llamar al iniciar la app para verificar si la sesión sigue activa
     */
    fun validateToken() {
        viewModelScope.launch {
            // Verificar que hay token
            if (UserPreferences.getAuthToken() == null) {
                _tokenValidationState.postValue(NetworkResult.Success(false))
                return@launch
            }

            authRepository.validateToken().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _tokenValidationState.postValue(NetworkResult.Success(result.data.valid))

                        // Si el token es invalido, cerrar sesion
                        if (!result.data.valid) {
                            logout()
                        }
                    }

                    is NetworkResult.Error -> {
                        // Si hay error 401, cerrar sesion
                        if (result.code == 401) {
                            logout()
                        }

                        _tokenValidationState.postValue(NetworkResult.Error(result.message))
                    }

                    is NetworkResult.Loading -> {
                        _tokenValidationState.postValue(NetworkResult.Loading())
                    }
                }
            }
        }
    }

    // ============================================
    // LOGOUT
    // ============================================

    /**
     * Cierra la sesión del usuario actual
     *
     * Limpia UserPreferences y SessionManager
     */
    fun logout() {
        authRepository.logout()
        SessionManager.logout()
    }


    // ============================================
    // VALIDACIONES
    // ============================================

    /**
     * Valida formato de email
     *
     * @param email Email a validar
     * @return true si es válido
     */

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    /**
     * Valida que la contraseña cumpla requisitos mínimos
     *
     * @param password Contraseña a validar
     * @return true si es válida (mínimo 8 caracteres)
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Verifica si el usuario actual es guest
     *
     * @return true si es guest
     */
    fun isGuestUser(): Boolean {
        return authRepository.isGuestUser()
    }

    /**
     * Verifica si hay una sesión activa
     *
     * @return true si hay usuario logueado (guest o autenticado)
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    /**
     * Obtiene el ID del usuario actual
     *
     * @return ID del usuario o null
     */
    fun getCurrentUserId(): Int? {
        return authRepository.getCurrentUserId()
    }


    /**
     * Limpia los resultados de operaciones
     *
     * Útil para limpiar mensajes después de mostrarlos
     */
    /*
    fun clearOperationResults() {
        _guestCreateState.value = null
        _registerState.value = null
        _loginState.value = null
        _tokenValidationState.value = null
        _cartMergeInfo.value = null
    }*/
}