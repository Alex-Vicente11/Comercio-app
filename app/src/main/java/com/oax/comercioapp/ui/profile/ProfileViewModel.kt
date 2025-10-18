package com.oax.comercioapp.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.UserRepository
import com.oax.comercioapp.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _user = MutableLiveData<NetworkResult<User>>()
    val user: LiveData<NetworkResult<User>> = _user

    val currentUser: LiveData<User?> = SessionManager.currentUser
    val sessionStatus: LiveData<String> = SessionManager.sessionStatus


    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _user.postValue(NetworkResult.Loading())
            userRepository.getUserById(userId).collect { result ->
                _user.postValue(result)
            }
        }
    }

    fun setCurrentUser(user: User) {

        SessionManager.login(user)
        Log.d("ProfileViewModel", "SessionManager.login() ejecutado")

        //Para guardar en SharedPreferences para persistir la sesion
        //E iniciliciar datos especificos del usuario (carrito, preferencias, etc)
    }

    fun getCurrentUser(): User? {
        return SessionManager.getCurrentUser()
    }

    fun isUserLoggedIn(): Boolean = SessionManager.isLoggedIn()

    fun logout() {
        Log.d("ProfileViewModel", "Ejecutando logout desde ViewModel")
        SessionManager.logout()

        // Para limpiar SharedPreferences
        // Limpiar cache de datos del usuario
        // Resetear otros estados relacionados
    }

    fun refreshCurrentUser() {
        SessionManager.getCurrentUser()?.let { user ->
            loadUser(user.idUser)
        }
    }

    fun getCurrentUserInfo(): Pair<Int, String>? {
        return SessionManager.getCurrentUser()?.let { user ->
            Pair(user.idUser, user.userName)
        }
    }
}