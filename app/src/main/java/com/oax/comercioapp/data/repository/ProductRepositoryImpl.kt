package com.oax.comercioapp.data.repository

import com.oax.comercioapp.data.api.ApiService
import com.oax.comercioapp.data.mapper.ProductMapper.toDomain
import com.oax.comercioapp.domain.model.Product
import com.oax.comercioapp.domain.repository.IProductRepository

/**
 * Se corrige el error más importante del ProductRepository.kt original:
 * PROBLEMA - suspend fun que retorna flow:
 *
 * suspend fun getProducts(): Flow<NetworkResult<List<Product>>>  <- Incorrecto
 *
 * Esto es una contradicción de tipos:
 *  - 'suspend' significa "espera aquí hasta tener el resultado"
 *  - 'Flow' significa "dame un stream de valores a lo largo del tiempo"
 * Son dos contratos incompatibles. El compilador lo permite porque técnicamente puede suspender
 * para construir el Flow, pero simánticamente es incorrecto y confunde a quien lee el código.
 *
 * Para una consulta de red puntual (pides una vez, recibes una vez):
 *   suspend fun getProducts(): Result<List>Product>> <- CORRECTO
 *
 * Para datos que cambian en tiempo real (WebSocket, Room con Flow):
 *   fun observeProducts(): Flow<List<Product>>       <- CORRECTO (sin suspend)
 *
 * Los productos de un e-commerce no cambian en tiempo real desde la perspectiva de la app cliente -
 * se cargan una vez por pantalla.
 * Por eso se usa suspend + Result, no Flow.
 */

class ProductRepositoryImpl(
    private val apiService: ApiService
) : IProductRepository {
    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = apiService.getProducts()

            if (response.isSuccessful) {
                val products = response.body()
                if (products != null) {
                    // toDomain() convierte List<ProductDto> -> List<Product>
                    // 'product.product' pasa a ser 'product.name' en el dominio
                    Result.success(products.toDomain())
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductById(productId: Int): Result<Product> {
        return try {
            val response = apiService.getProductById(productId)

            if (response.isSuccessful) {
                val productDto = response.body()
                if (productDto != null) {
                    Result.success(productDto.toDomain())
                } else {
                    // NoSuchElementException es semánticamente correcto para "no encontrado"
                    Result.failure(NoSuchElementException("Product $productId not found"))
                }
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}