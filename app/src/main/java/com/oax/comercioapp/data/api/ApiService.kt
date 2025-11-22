package com.oax.comercioapp.data.api

import com.google.gson.annotations.SerializedName
import com.oax.comercioapp.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {


    @GET("api/health")
    suspend fun healthCheck(): Response<HealthCheckResponse>

    @GET("api/auth/validate")
    suspend fun validateToken(): Response<ValidateTokenResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>

    @GET("api/cart/items")
    suspend fun getCartItemsDetailed(): Response<CartDetailedResponse>

    @GET("api/cart/count")
    suspend fun getCartCount(): Response<CartCountResponse>

    @GET("api/cart/check/{product_id}")
    suspend fun checkProductInCart(@Path("product_id") productId: Int): Response<CartCheckResponse>



    // Guest endpoitns
    @POST("api/guest/create")
    suspend fun createGuest(@Body request: GuestCreateRequest): Response <GuestCreateResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/cart/add")
    suspend fun addToCart(@Body request: AddToCartRequest): Response<CartResponse>

    @PUT("api/cart/update")
    suspend fun updateCartQuantity(@Body request: UpdateCartRequest): Response<CartResponse>

    @DELETE("api/cart/remove")
    suspend fun removeFromCart(@Body request: RemoveCartRequest): Response<CartResponse>

    @DELETE("api/cart/clear")
    suspend fun clearCart(): Response<CartResponse>

    @POST("api/cart/merge")
    suspend fun mergeCart(@Body request: MergeCartRequest): Response<MergeCartResponse>


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

    @PUT("cart/{id}")
    suspend fun updateCartItem(
        @Path("id") cartId: Int,
        @Body request: CartUpdateRequest
    ): Response<CartResponse>

    @DELETE("cart/{id}")
    suspend fun removeFromCartId(@Path("id") cartId: Int): Response<CartResponse>
}