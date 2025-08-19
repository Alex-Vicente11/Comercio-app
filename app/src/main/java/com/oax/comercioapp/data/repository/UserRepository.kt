package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.api.RetrofitClient
import com.oax.comercioapp.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UserRepository {
    private val apiService: ApiService = RetrofitClient.apiService
    
    suspend fun getUsers(): Flow<NetworkResult<List<User>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("Empty response body"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun getUserById(id: Int): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getUserById(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("User not found"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun createUser(userRequest: UserRequest): Flow<NetworkResult<UserResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.createUser(userRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("Empty response"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun updateUser(id: Int, request: UserRequest): Flow<NetworkResult<UserResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.updateUser(id, request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("Empty response"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun deleteUser(id: Int): Flow<NetworkResult<UserResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.deleteUser(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("Empty response"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
}