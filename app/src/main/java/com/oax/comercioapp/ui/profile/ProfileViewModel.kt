package com.oax.comercioapp.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _user = MutableLiveData<NetworkResult<User>>()
    val user: LiveData<NetworkResult<User>> = _user

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser


    fun loadUser(userId: Int) {
        viewModelScope.launch {
            userRepository.getUserById(userId).collect { result ->
                _user.postValue(result)
            }
        }
    }

    fun setCurrentUser(user: User) {
        _currentUser.postValue(user)

        com.oax.comercioapp.utils.SessionManager.login(user)

        //Para guardar en SharedPreferences para persistir la sesion
        //E iniciliciar datos especificos del usuario (carrito, preferencias, etc)
    }

    fun getCurrentUser(): User? {
        return _currentUser.value
    }

    fun isUserLoggedIn(): Boolean {
        return _currentUser.value != null
    }

    fun logout() {
        _currentUser.postValue(null)

        // Para limpiar SharedPreferences
        // Limpiar cache de datos del usuario
        // Resetear otros estados relacionados
    }

    fun refreshCurrentUser() {
        _currentUser.value?.let { user ->
            loadUser(user.idUser)
        }
    }

    fun getCurrentUserInfo(): Pair<Int, String>? {
        return _currentUser.value?.let { user ->
            Pair(user.idUser, user.userName)
        }
    }
}