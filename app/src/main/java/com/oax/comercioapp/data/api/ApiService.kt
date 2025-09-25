package com.oax.comercioapp.data.api

import com.oax.comercioapp.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // User endpoints
    @GET("users")
    suspend fun getUsers(): Response<List<User>>
    
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<User>
    
    @POST("users")
    suspend fun createUser(@Body request: UserRequest): Response<UserResponse>
    
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body request: UserRequest
    ): Response<UserResponse>
    
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<UserResponse>
    
    // Product endpoints
    @GET("products")
    suspend fun getProducts(): Response<List<Product>>
    
    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<Product>
    
    @POST("products")
    suspend fun createProduct(@Body request: ProductRequest): Response<ProductResponse>
    
    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body request: ProductUpdateRequest
    ): Response<ProductResponse>
    
    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<ProductResponse>

    // Cart endpoints

    @GET("cart/user/{userId}")
    suspend fun getCartItems(@Path("userId") userId: Int): Response<List<CartItem>>

    @GET("cart/{userId}/{productId}")
    suspend fun getCartItem(
        @Path("userId") userId: Int,
        @Path("productId") productId: Int
    ): Response<Cart?>

    @POST("add-to-cart")
    suspend fun addToCart(@Body request: CartRequest): Response<CartResponse>

    @PUT("cart/{id}")
    suspend fun updateCartItem(
        @Path("id") cartId: Int,
        @Body request: CartUpdateRequest
    ): Response<CartResponse>

    @DELETE("cart/{id}")
    suspend fun removeFromCart(@Path("id") cartId: Int): Response<CartResponse>
}