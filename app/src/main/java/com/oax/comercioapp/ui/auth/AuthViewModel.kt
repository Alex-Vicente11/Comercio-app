package com.oax.comercioapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.domain.model.AuthResult
import com.oax.comercioapp.domain.usecase.CreateGuestSessionUseCase
import com.oax.comercioapp.domain.usecase.LoginUseCase
import com.oax.comercioapp.domain.usecase.LogoutUseCase
import com.oax.comercioapp.domain.usecase.RegisterUseCase
import com.oax.comercioapp.domain.usecase.ValidateSessionUseCase
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. LiveData -> StateFlow
 *    LiveData requiere un Observer con ciclo de vida de Android - no es testeable sin un emulador.
 *    StateFlow es kotlin puro: el Fragment lo colecta con lifecycleScope. el test lo colecta directamente.
 *    Mismo comporatamientp, mejor testeabilidad.
 *
 * 2. NetworkResult -> UiState
 *    El ViewModel ya no sabe que los datos vienen de una red. Solo sabe que una operación puede estar en
 *    Loading, Success o Error. Si después login se hace con biometría o con caché local, el fragment no cambia nada.
 *
 * 3. AuthRepository -> Use Cases inyectados
 *    Antes: authRepository.login(email, password).collect { ... }
 *    Ahora: loginUseCase(email, password)
 *    El ViewModel ya no construye objetos User, no llama a SessionManager, no obtiene guestId -
 *    todoo eso lo hace el use case.
 *
 * 4. Se elimina SessionManager del ViewModel
 *    SessionManager era un singleton estático que el ViewModel llamaba directamente. Con los use cases,
 *    el repositorio ya guarda la sesión en UserPreferences.
 *
 * 5. Estados separados por operación
 *    loginState, registerState, guestState separados - así el Fragment puede reaccionar a cada uno sin
 *    ambigüedad. Un solo _authState compartido causaría confusión si login y register compiten.
 *
 * 6. cartMergeInfo eliminado del ViewModel
 *    Era un Pair<Boolean, Int> que comunicaba si el carrito migró.
 *    Con el nuevo enfoque de app de pedidos, después del login la app simplemente recarga el
 *    carrito actual - no necesita saber su hubo merge. Esa información es un detalle de infraestructura
 *    que no sube a la UI.
 */

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val validateSessionUseCase: ValidateSessionUseCase,
    private val createGuestSessionUseCase: CreateGuestSessionUseCase,
    private val logoutUseCase: LogoutUseCase
): ViewModel() {

    // Estado de creacion de guest
    private val _guestState = MutableStateFlow<UiState<AuthResult>>(UiState.Idle)
    val guestState: StateFlow<UiState<AuthResult>> = _guestState.asStateFlow()

    // Estado de registro
    private val _registerState = MutableStateFlow<UiState<AuthResult>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthResult>> = _registerState.asStateFlow()

    // Estado de login
    private val _loginState = MutableStateFlow<UiState<AuthResult>>(UiState.Idle)
    val loginState: StateFlow<UiState<AuthResult>> = _loginState.asStateFlow()

    /**
     * Estado de validación de sesión - Boolean indica si la sesión es válida.
     * El Fragment observa esto al iniciar para decidir si navegar al login o al home.
     */
    private val _sessionState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val sessionState: StateFlow<UiState<Boolean>> = _sessionState.asStateFlow()

    // OPERACIONES

    /**
     * Crea un usuario guest en el backend
     *
     * Se debe llamar al iniciar la app si no hay sesión
     */
    fun createGuestSession() {
        viewModelScope.launch {
            _guestState.value = UiState.Loading

            createGuestSessionUseCase()
                .onSuccess { authResult ->
                    _guestState.value = UiState.Success(authResult)
                }

                .onFailure { error ->
                    _guestState.value = UiState.Error(
                        error.message ?: "Error al crear sesión de invitado"
                    )
                }
        }
    }

    /**
     * Registra un nuevo usuario con email y password
     *
     * @param email Email del usuario
     * @param password Contraseña
     * @param userName Nombre del usuario
     */

    fun register(email: String, password: String, userName: String) {
        viewModelScope.launch {
            _registerState.value = UiState.Loading

            registerUseCase(email, password, userName)
                .onSuccess { authResult ->
                    _registerState.value = UiState.Success(authResult)
                }

                .onFailure { error ->
                    _registerState.value = UiState.Error(
                    error.message ?: "Error al registrar usuario"
                    )
                }
        }
    }

    /**
     * Inicia sesión con email y password
     *
     * @param email Email del usuario
     * @param password Contraseña
     */

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading

            loginUseCase(email, password)
                .onSuccess { authResult ->
                    _loginState.value = UiState.Success(authResult)
                }

                .onFailure { error ->
                    // El use case ya traduce los errores técnicos a mensajes legibles
                    _loginState.value = UiState.Error(
                        error.message ?: "Error al iniciar sesión"
                    )
                }
        }
    }

    /**
     * Valida el token JWT actual
     *
     * Se debe llamar al iniciar la app para verificar si la sesión sigue activa
     */
    fun validateSession() {
        viewModelScope.launch {
            _sessionState.value = UiState.Loading

            validateSessionUseCase()
                .onSuccess { isValid ->
                    _sessionState.value = UiState.Success(isValid)
                    // Si el token no es válido, limpiar sesión actual
                    if (!isValid) logoutUseCase()
                }

                .onFailure {
                    // Error de red al validar - tratamos como sesión inválida
                    _sessionState.value = UiState.Success(false)
                    logoutUseCase()
                }
        }
    }

    /**
     * Cierra la sesión del usuario actual
     *
     * Limpia UserPreferences y SessionManager
     */
    fun logout() {
        logoutUseCase()
        // Resetear todos los estados al cerrar sesión
        resetStates()
    }

    /**
     * Limpia el estado de login después de que el Fragment lo procesó.
     * Evita que al rotar la pantalla se vuelva a navegar al home.
     * Patrón "consume once" para evetos de navegación.
     */
    fun onLoginHandled() {
        _loginState.value = UiState.Idle
    }

    fun onRegisterHandled() {
        _registerState.value = UiState.Idle
    }

    private fun resetStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
        _guestState.value = UiState.Idle
        _sessionState.value = UiState.Idle
    }
}