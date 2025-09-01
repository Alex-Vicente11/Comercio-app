package com.oax.comercioapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.data.models.ProductResponse
import com.oax.comercioapp.data.repository.ProductRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val productRepository: ProductRepository = ProductRepository()) : ViewModel() {

  //private val productRepository = ProductRepository()
  
  private val _products = MutableLiveData<NetworkResult<List<Product>>>()
  val products: LiveData<NetworkResult<List<Product>>> = _products

  private val _createProductResult = MutableLiveData<NetworkResult<ProductResponse>>()
  val createProductResult: LiveData<NetworkResult<ProductResponse>> = _createProductResult
  
  private val _text = MutableLiveData<String>().apply {
    value = "Productos"
  }
  val text: LiveData<String> = _text
  
  init {
    loadProducts()
  }
  
  fun loadProducts() {
    viewModelScope.launch {
      productRepository.getProducts().collect { result ->
        _products.postValue(result)
      }
    }
  }

  fun createProduct(productRequest: ProductRequest){
      viewModelScope.launch {
          productRepository.createProduct(productRequest).collect { result ->
              _createProductResult.postValue(result)
          }
      }
  }
  
  fun refreshProducts() {
    loadProducts()
  }
}