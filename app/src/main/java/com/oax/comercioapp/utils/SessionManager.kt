package com.oax.comercioapp.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SessionManager {
    private val TAG = "SessionManager"

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private val _sessionStatus = MutableLiveData<String>("No hay sesión activa")
    val sessionStatus: LiveData<String> = _sessionStatus

    private val _isRestoring = MutableLiveData<Boolean>(false)
    val isRestoring: LiveData<Boolean> = _isRestoring

    // Scope para operaciones asíncronas del SessionManager
    private val sessionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Repository lazy-initialized para evitar problemas de inicialización
    private val userRepository by lazy { UserRepository() }

    // Flag para evitar múltiples inicializaciones
    private var isInitialized = false

    /**
     * Inicializa el SessionManager de forma segura
     */
    fun initialize() {
        if (isInitialized) {
            println("$TAG: Ya está inicializado, saltando...")
            return
        }

        try {
            println("$TAG: Inicializando SessionManager...")

            // Verificar si PreferencesManager está inicializado
            try {
                PreferencesManager.hasActiveSession()
            } catch (e: Exception) {
                println("$TAG: Error - PreferencesManager no está inicializado: ${e.message}")
                updateSessionStatus(null)
                isInitialized = true
                return
            }

            if (PreferencesManager.hasActiveSession()) {
                println("$TAG: Sesión encontrada, iniciando restauración...")
                restoreSessionFromPreferences()
            } else {
                println("$TAG: No hay sesión previa")
                updateSessionStatus(null)
            }

            isInitialized = true

        } catch (e: Exception) {
            println("$TAG: Error en inicialización: ${e.message}")
            e.printStackTrace()
            updateSessionStatus(null)
            isInitialized = true
        }
    }

    /**
     * Inicia sesión de forma segura
     */
    fun login(user: User?) {
        try {
            if (user == null) {
                println("$TAG: Error - Intento de login con usuario null")
                return
            }

            if (user.idUser <= 0 || user.userName.isNullOrBlank()) {
                println("$TAG: Error - Usuario con datos inválidos: ID=${user.idUser}, Name='${user.userName}'")
                return
            }

            _currentUser.postValue(user)
            updateSessionStatus(user)

            println("$TAG: LiveData updated - Current observers: ${_currentUser.hasObservers()}") //log para seguimiento de session

            // Persistir solo si PreferencesManager está disponible
            try {
                PreferencesManager.saveUserSession(user.idUser, user.userName)
                println("$TAG: Sesión iniciada y persistida: ${user.userName} (ID: ${user.idUser})")
            } catch (e: Exception) {
                println("$TAG: Error al persistir sesión: ${e.message}")
                // Continuar aunque falle la persistencia
            }

        } catch (e: Exception) {
            println("$TAG: Error en login: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Restaura sesión con manejo robusto de errores
     */
    private fun restoreSessionFromPreferences() {
        try {
            _isRestoring.postValue(true)

            val userId = PreferencesManager.getUserId()
            val userName = PreferencesManager.getUserName()

            println("$TAG: Datos de SharedPreferences - UserID: $userId, UserName: '$userName'")

            if (userId <= 0 || userName.isNullOrBlank()) {
                println("$TAG: Datos inválidos en SharedPreferences, limpiando...")
                PreferencesManager.clearUserSession()
                updateSessionStatus(null)
                _isRestoring.postValue(false)
                return
            }

            // Crear usuario básico como fallback
            val basicUser = User(userId, userName)

            // Intentar cargar desde API
            sessionScope.launch {
                try {
                    userRepository.getUserById(userId).collect { result ->
                        when (result) {
                            is NetworkResult.Loading -> {
                                println("$TAG: Cargando usuario desde API...")
                            }
                            is NetworkResult.Success -> {
                                val apiUser = result.data
                                if (apiUser != null && apiUser.idUser > 0 && !apiUser.userName.isNullOrBlank()) {
                                    _currentUser.postValue(apiUser)
                                    updateSessionStatus(apiUser)
                                    println("$TAG: Sesión restaurada desde API: ${apiUser.userName}")
                                } else {
                                    println("$TAG: Usuario de API inválido, usando datos básicos")
                                    _currentUser.postValue(basicUser)
                                    updateSessionStatus(basicUser)
                                }
                                _isRestoring.postValue(false)
                            }
                            is NetworkResult.Error -> {
                                println("$TAG: Error API (${result.message}), usando datos básicos")
                                _currentUser.postValue(basicUser)
                                updateSessionStatus(basicUser)
                                _isRestoring.postValue(false)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("$TAG: Excepción en restauración API: ${e.message}")
                    _currentUser.postValue(basicUser)
                    updateSessionStatus(basicUser)
                    _isRestoring.postValue(false)
                }
            }

        } catch (e: Exception) {
            println("$TAG: Error crítico en restauración: ${e.message}")
            e.printStackTrace()
            updateSessionStatus(null)
            _isRestoring.postValue(false)
        }
    }

    /**
     * Logout seguro
     */
    fun logout() {
        try {
            val userName = _currentUser.value?.userName ?: "Usuario desconocido"

            // Limpiar estado en memoria
            _currentUser.postValue(null)
            updateSessionStatus(null)

            // Limpiar SharedPreferences
            try {
                PreferencesManager.clearUserSession()
            } catch (e: Exception) {
                println("$TAG: Error al limpiar SharedPreferences: ${e.message}")
            }

            println("$TAG: Logout completo para: $userName")

        } catch (e: Exception) {
            println("$TAG: Error en logout: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Refresh del usuario actual con manejo de errores
     */
    fun refreshCurrentUser() {
        try {
            val currentUser = _currentUser.value
            if (currentUser == null) {
                println("$TAG: No hay usuario actual para refrescar")
                return
            }

            if (currentUser.idUser <= 0) {
                println("$TAG: Usuario actual tiene ID inválido: ${currentUser.idUser}")
                return
            }

            sessionScope.launch {
                try {
                    userRepository.getUserById(currentUser.idUser).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                val updatedUser = result.data
                                if (updatedUser != null && updatedUser.idUser > 0) {
                                    _currentUser.postValue(updatedUser)
                                    updateSessionStatus(updatedUser)
                                    PreferencesManager.saveUserSession(updatedUser.idUser, updatedUser.userName)
                                    println("$TAG: Usuario actualizado: ${updatedUser.userName}")
                                } else {
                                    println("$TAG: Usuario actualizado es inválido")
                                }
                            }
                            is NetworkResult.Error -> {
                                println("$TAG: Error al actualizar usuario: ${result.message}")
                            }
                            is NetworkResult.Loading -> {
                                // No hacer nada
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("$TAG: Excepción al refrescar usuario: ${e.message}")
                }
            }

        } catch (e: Exception) {
            println("$TAG: Error en refreshCurrentUser: ${e.message}")
            e.printStackTrace()
        }
    }

    // === MÉTODOS SEGUROS DE ACCESO ===

    fun getCurrentUser(): User? {
        return try {
            _currentUser.value
        } catch (e: Exception) {
            println("$TAG: Error al obtener usuario actual: ${e.message}")
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return try {
            val user = _currentUser.value
            user != null && user.idUser > 0 && !user.userName.isNullOrBlank()
        } catch (e: Exception) {
            println("$TAG: Error al verificar login: ${e.message}")
            false
        }
    }

    fun getCurrentUserId(): Int? {
        return try {
            val user = _currentUser.value
            if (user != null && user.idUser > 0) user.idUser else null
        } catch (e: Exception) {
            println("$TAG: Error al obtener ID: ${e.message}")
            null
        }
    }

    fun getCurrentUserName(): String? {
        return try {
            val user = _currentUser.value
            if (user != null && !user.userName.isNullOrBlank()) user.userName else null
        } catch (e: Exception) {
            println("$TAG: Error al obtener nombre: ${e.message}")
            null
        }
    }

    fun getCurrentSessionText(): String {
        return try {
            _sessionStatus.value ?: "Estado desconocido"
        } catch (e: Exception) {
            println("$TAG: Error al obtener texto de sesión: ${e.message}")
            "Error en sesión"
        }
    }

    // === MÉTODOS DE DEBUG SEGUROS ===

    fun debugCurrentSession(): String {
        return try {
            val memoryUser = _currentUser.value
            val memoryInfo = if (memoryUser != null) {
                "Usuario en memoria: ${memoryUser.userName} (ID: ${memoryUser.idUser})"
            } else {
                "No hay usuario en memoria"
            }

            val prefsInfo = try {
                PreferencesManager.getSessionDebugInfo()
            } catch (e: Exception) {
                "Error al obtener info de SharedPreferences: ${e.message}"
            }

            buildString {
                append("=== DEBUG DE SESIÓN ===\n")
                append("$memoryInfo\n")
                append("---\n")
                append(prefsInfo)
            }
        } catch (e: Exception) {
            "Error en debug: ${e.message}"
        }
    }

    fun getCompleteSessionInfo(): String {
        return try {
            buildString {
                append(debugCurrentSession())
                append("\n---\n")
                append("¿Inicializado?: $isInitialized\n")
                append("¿Sesión expirada?: ${isSessionExpired()}\n")
                append("¿Restaurando?: ${_isRestoring.value}\n")
                append("Estado: ${_sessionStatus.value}\n")
            }
        } catch (e: Exception) {
            "Error al obtener información completa: ${e.message}"
        }
    }

    fun isSessionExpired(): Boolean {
        return try {
            if (!PreferencesManager.hasActiveSession()) return true

            val loginTime = PreferencesManager.getLoginTimestamp()
            if (loginTime <= 0) return true

            val currentTime = System.currentTimeMillis()
            val sessionDuration = currentTime - loginTime

            // Sesión expira después de 30 días
            val maxSessionDuration = 30L * 24L * 60L * 60L * 1000L

            sessionDuration > maxSessionDuration
        } catch (e: Exception) {
            println("$TAG: Error al verificar expiración: ${e.message}")
            true
        }
    }

    // === MÉTODOS PRIVADOS ===

    private fun updateSessionStatus(user: User?) {
        try {
            val status = user?.let {
                if (!it.userName.isNullOrBlank()) "Sesión: ${it.userName}" else "Sesión: Usuario sin nombre"
            } ?: "No hay sesión activa"

            _sessionStatus.postValue(status)
        } catch (e: Exception) {
            println("$TAG: Error al actualizar estado: ${e.message}")
            _sessionStatus.postValue("Error en sesión")
        }
    }
}