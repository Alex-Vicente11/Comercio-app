package com.oax.comercioapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.domain.model.User
import com.oax.comercioapp.domain.usecase.GetProfileUseCase
import com.oax.comercioapp.domain.usecase.LogoutUseCase
import com.oax.comercioapp.domain.usecase.UpdateProfileUseCase
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. Eliminadas dependencias a SessionManager y UserPreferences directos.
 *    El original llamaba UserPreferences.isGuest(), UserPreferences.getEmail(),
 *    SessionManager.getCurrentUser(), etc. directamente desde el ViewModel.
 *    Eso hacía al ViewModel dependiente de 2 singletons estáticos distintos
 *    - imposible de testear y díficil de rastrear el flujo de datos.
 *
 *    Ahora: toda la información viene del use case -> repositorio -> preferencias
 *    Un solo flujo de datos, trazable y testeable.
 *
 * 2. Eliminado loadUser(userId) que llamaba a userRepository.getUserById()
 *    Con el nuevo enfoque de app cliente, no se consultan otros usuarios.
 *    Solo el propio perfil via getProfile()
 *
 * 3. Eliminado el Log.d de debugging disperso por el ViewModel.
 *    Los logs de sesión van en UserPreferences donde tienen contexto real.
 *    Un ViewModel no debería loggear estado interno - eso es ruido en producción
 *
 * 4. updateProfile() agregado - estaba en AuthRepository original pero semánticamente
 *    pertenece al perfil del usuario (IUserRepository).
 *
 * 5. isGuest se deriva del User cargado, no de una llamada separada a UserPreferences.
 *    Si el usuario es guest, User.isGuest = true
 *    Un solo estado, no dos fuentes de verdad distintos.
 *
 * SOBRE ProfilesViewModel (dashboard):
 * El original listaba y creaba usuarios - funcionalidad admin.
 * Con el nuevo enfoque se convierte en OrderHistoryViewModel
 * mostrando los pedidos del usuario actual. Falta crear el endpoint en backend.
 */

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val profileState: StateFlow<UiState<User>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val updateState: StateFlow<UiState<User>> = _updateState.asStateFlow()

    /**
     * Estado de logout - Unit indica que el logout fue exitoso.
     * El fragment observa esto para navegar al login
     */
    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading

            getProfileUseCase()
                .onSuccess { user ->
                    _profileState.value = UiState.Success(user)
                }

                .onFailure { error ->
                    _profileState.value = UiState.Error(
                        error.message ?: "Error al cargar perfil"
                    )
                }
        }
    }

    /**
     * Cierra la sesión del usuario actual
     */
    fun logout() {
        logoutUseCase()
        _logoutState.value = UiState.Success(Unit)
    }


    /**
     * Actualiza el estado del usuario (guest vs autenticado)
     */
    fun updateProfile(newUserName: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading

            updateProfileUseCase(newUserName)
                .onSuccess { updatedUser ->
                    _updateState.value = UiState.Success(updatedUser)
                    // Refrescar el perfil para que la UI muestre el nombre actualizado
                    _profileState.value = UiState.Success(updatedUser)
                }

                .onFailure { error ->
                    _updateState.value = UiState.Error(
                        error.message ?: "Error al actualizar perfil"
                    )
                }
        }
    }

    fun onUpdateHandled() {
        _updateState.value = UiState.Idle
    }

    fun onLogoutHandled() {
        _logoutState.value = UiState.Idle
    }
}