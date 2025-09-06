package com.oax.comercioapp.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.oax.comercioapp.data.models.User

object SessionManager {
    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private val _sessionStatus = MutableLiveData<String>("No hay sesión activa")
    val sessionStatus: LiveData<String> = _sessionStatus

    fun login(user: User) {
        _currentUser.postValue(user)
        updateSessionStatus(user)
        println("Sesión iniciada para: ${user.userName} (ID: ${user.idUser}")
    }

    fun getCurrentUser(): User? {
        return _currentUser.value
    }

    fun isLoggedIn(): Boolean {
        return _currentUser.value != null
    }

    fun getCurrentUserId(): Int? {
        return _currentUser.value?.idUser
    }

    fun getCurrentUserName(): String? {
        return _currentUser.value?.userName
    }

    fun logout() {
        val userName = _currentUser.value?.userName
        _currentUser.postValue(null)
        _sessionStatus.postValue("No hay sesión activa")
        println("Sesión cerrada para: $userName")
    }


    private fun updateSessionStatus(user: User) {
        _sessionStatus.postValue("Sesión: ${user.userName}")
    }

    fun getCurrentSessionText(): String {
        return _sessionStatus.value ?: "No hay sesión activa"
    }

    fun debugCurrentSession(): String {
        return if (isLoggedIn()) {
            "Usuario logueado: ${getCurrentUserName()} (ID: ${getCurrentUserId()}"
        }else {
            "No hay usuario logueado"
        }
    }
}