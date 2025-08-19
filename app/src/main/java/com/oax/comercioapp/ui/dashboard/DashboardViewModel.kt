package com.oax.comercioapp.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.data.repository.UserRepository
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

  private val userRepository = UserRepository()
  
  private val _users = MutableLiveData<NetworkResult<List<User>>>()
  val users: LiveData<NetworkResult<List<User>>> = _users
  
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
}