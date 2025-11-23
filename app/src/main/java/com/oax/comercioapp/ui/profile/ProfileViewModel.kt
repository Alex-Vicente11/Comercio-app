package com.oax.comercioapp.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.AuthRepository
import com.oax.comercioapp.data.repository.UserRepository
import com.oax.comercioapp.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * ProfileViewModel - ViewModel para gestión del perfil de usuario
 *
 * CAMBIOS vs versión anterior:
 * - Diferencia entre usuarios guest y autenticados
 * - Integración con AuthRepository para perfil
 * - Nuevos LiveData para estado de guest
 * - Métodos actualizados para JWT
 */

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Usuario cargado desde API
    private val _user = MutableLiveData<NetworkResult<User>>()
    val user: LiveData<NetworkResult<User>> = _user

    // Usuario actual desde SessionManager
    val currentUser: LiveData<User?> = SessionManager.currentUser

    // Estado de sesion
    val sessionStatus: LiveData<String> = SessionManager.sessionStatus

    // Indica si el usuario es guest
    private val _isGuest = MutableLiveData<Boolean>()
    val isGuest: LiveData<Boolean> = _isGuest

    // Email del usuario (null si es guest)
    private val _userEmail = MutableLiveData<String?>()
    val userEmail: LiveData<String?> = _userEmail

    init {
        // Cargar estado inicial
        updateUserState()
    }


    // ============================================
    // CARGAR INFORMACIÓN DE USUARIO
    // ============================================

    /**
     * Carga información del usuario desde la API
     *
     * @param userId ID del usuario a cargar
     */

    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _user.postValue(NetworkResult.Loading())
            userRepository.getUserById(userId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _user.postValue(result)
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }
                        _user.postValue(result)
                    }

                    is NetworkResult.Loading -> {
                        _user.postValue(result)
                    }
                }
            }
        }
    }

    /**
     * Carga el perfil del usuario autenticado actual
     *
     * NUEVO: Usa AuthRepository con JWT
     */
    fun loadAuthenticatedProfile() {
        viewModelScope.launch {
            // Solo para usuarios autenticados
            if (UserPreferences.isGuest()) {
                _user.postValue(NetworkResult.Error("Usuario guest no tiene perfil"))
                return@launch
            }

            authRepository.getProfile().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val userProfile = result.data.user
                        if (userProfile != null) {
                            val user = User(
                                idUser = userProfile.idUser,
                                userName = userProfile.userName,
                                email = userProfile.email,
                                isGuest = userProfile.isGuest,
                                token = UserPreferences.getAuthToken()
                            )
                            _user.postValue(NetworkResult.Success(user))
                        }
                    }

                    is NetworkResult.Error -> {
                        if (result.code == 401) {
                            SessionManager.logout()
                        }

                        _user.postValue(NetworkResult.Error(result.message))
                    }

                    is NetworkResult.Loading -> {
                        _user.postValue(NetworkResult.Loading())
                    }
                }
            }
        }
    }



    // ============================================
    // GESTIÓN DE SESIÓN
    // ============================================

    /**
     * Establece el usuario actual en SessionManager
     *
     * @param user Usuario a establecer como actual
     */
    fun setCurrentUser(user: User) {

        SessionManager.login(user)
        Log.d("ProfileViewModel", "SessionManager.login() ejecutado")

        updateUserState()

        //Para guardar en SharedPreferences para persistir la sesion
        //E iniciliciar datos especificos del usuario (carrito, preferencias, etc)
    }


    /**
     * Obtiene el usuario actual
     *
     * @return Usuario actual o null si no hay sesión
     */
    fun getCurrentUser(): User? {
        return SessionManager.getCurrentUser()
    }


    /**
     * Verifica si hay usuario logueado
     *
     * @return true si hay sesión activa (guest o autenticado)
     */
    fun isUserLoggedIn(): Boolean = SessionManager.isLoggedIn()


    /**
     * Cierra la sesión del usuario actual
     */
    fun logout() {
        Log.d("ProfileViewModel", "Ejecutando logout desde ViewModel")
        SessionManager.logout()
        authRepository.logout()
        updateUserState()

        // Para limpiar SharedPreferences
        // Limpiar cache de datos del usuario
        // Resetear otros estados relacionados
    }

    /**
     * Refresca la información del usuario actual
     */
    fun refreshCurrentUser() {
        SessionManager.getCurrentUser()?.let { user ->
            if (user.isGuest) {
                // Para guests, solo actualizar desde SessionManager
                updateUserState()
            } else {
                // Para autenticados, cargar desde API
                loadUser(user.idUser)
            }
        }
    }


    /**
     * Obtiene información básica del usuario actual
     *
     * @return Pair con userId y userName, o null si no hay sesión
     */
    fun getCurrentUserInfo(): Pair<Int, String>? {
        return SessionManager.getCurrentUser()?.let { user ->
            Pair(user.idUser, user.userName)
        }
    }


    // ============================================
    // ACTUALIZAR ESTADO
    // ============================================

    /**
     * Actualiza el estado del usuario (guest vs autenticado)
     */
    private fun updateUserState() {
        val isGuestUser = try {
            UserPreferences.isGuest()
        } catch (e: Exception) {
            true
        }

        val email = try {
            UserPreferences.getEmail()
        } catch (e: Exception) {
            null
        }

        _isGuest.value = isGuestUser
        _userEmail.value = email

        Log.d("ProfileViewModel", "Estado actualizado: isGuest=$isGuestUser, email=$email")
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Obtiene información completa de la sesión (para debugging)
     *
     * @return String con información de sesión y preferencias
     */
    fun getSessionDebugInfo(): String {
        return try {
            buildString {
                append("=== PROFILE VIEW MODEL DEBUG ===\n")
                append("SessionManager Info:\n")
                append(SessionManager.debugCurrentSession())
                append("\n\n")
                append("UserPreferences Info:\n")
                append(UserPreferences.getSessionDebugInfo())
            }
        } catch (e: Exception) {
            "Error obteniendo debug info: ${e.message}"
        }
    }

    /**
     * Verifica si el token JWT está expirado
     *
     * @return true si está expirado o no existe
     */
    fun isTokenExpired(): Boolean {
        return try {
            UserPreferences.isTokenExpired()
        } catch (e: Exception) {
            true
        }
    }
}