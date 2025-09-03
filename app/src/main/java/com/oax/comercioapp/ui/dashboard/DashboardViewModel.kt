package com.oax.comercioapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.models.UserRequest
import com.oax.comercioapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

  private val userRepository = UserRepository()
  
  private val _users = MutableLiveData<NetworkResult<List<User>>>()
  val users: LiveData<NetworkResult<List<User>>> = _users

  private val _createUserResult = MutableLiveData<NetworkResult<String>>()
  val createUserResult: LiveData<NetworkResult<String>> = _createUserResult
  
  private val _text = MutableLiveData<String>().apply {
    value = "Usuarios"
  }
  val text: LiveData<String> = _text
  
  init {
    loadUsers()
  }
  
  fun loadUsers() {
    viewModelScope.launch {
      userRepository.getUsers().collect { result ->
        _users.postValue(result)
      }
    }
  }
  
  fun refreshUsers() {
    loadUsers()
  }

  fun createUser(userName: String) {
    if (userName.trim().isEmpty()) {
      _createUserResult.postValue(NetworkResult.Error("El nombre de usuario no puede estar vacío"))
      return
    }

    viewModelScope.launch {
      val userRequest = UserRequest(userName.trim())
      userRepository.createUser(userRequest).collect { result ->
        when (result) {
          is NetworkResult.Loading -> {
            _createUserResult.postValue(NetworkResult.Loading())
          }
          is NetworkResult.Success -> {
            _createUserResult.postValue(NetworkResult.Success("Usuario creado exitosamente"))
            //Recargar la lista de usuarios despues de crear uno nuevo
            loadUsers()
          }
          is NetworkResult.Error -> {
            _createUserResult.postValue(NetworkResult.Error(result.message ?: "Error al crear usuario"))
          }
        }
      }
    }
  }

  fun clearCreateUserResult() {
    _createUserResult.value = null
  }
}