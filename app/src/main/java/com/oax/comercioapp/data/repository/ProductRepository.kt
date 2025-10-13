package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.api.RetrofitClient
import com.oax.comercioapp.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProductRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    
    suspend fun getProducts(): Flow<NetworkResult<List<Product>>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getProducts()
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
    
    suspend fun getProductById(id: Int): Flow<NetworkResult<Product>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getProductById(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(NetworkResult.Success(it))
                } ?: emit(NetworkResult.Error("Product not found"))
            } else {
                emit(NetworkResult.Error("Error: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun createProduct(productRequest: ProductRequest): Flow<NetworkResult<ProductResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.createProduct(productRequest)
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
    
    suspend fun updateProduct(id: Int, request: ProductUpdateRequest): Flow<NetworkResult<ProductResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.updateProduct(id, request)
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
    
    suspend fun deleteProduct(id: Int): Flow<NetworkResult<ProductResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.deleteProduct(id)
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