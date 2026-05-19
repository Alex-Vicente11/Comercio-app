package com.oax.comercioapp.data.api

import com.google.gson.annotations.SerializedName
import com.oax.comercioapp.data.dto.AddToCartRequestDto
import com.oax.comercioapp.data.dto.AuthResponseDto
import com.oax.comercioapp.data.dto.CartCountResponseDto
import com.oax.comercioapp.data.dto.CartDetailedResponseDto
import com.oax.comercioapp.data.dto.CartOperationResponseDto
import com.oax.comercioapp.data.dto.GuestCreateRequestDto
import com.oax.comercioapp.data.dto.GuestCreateResponseDto
import com.oax.comercioapp.data.dto.LoginRequestDto
import com.oax.comercioapp.data.dto.MergeCartRequestDto
import com.oax.comercioapp.data.dto.MergeCartResponseDto
import com.oax.comercioapp.data.dto.ProductDto
import com.oax.comercioapp.data.dto.ProfileResponseDto
import com.oax.comercioapp.data.dto.RegisterRequestDto
import com.oax.comercioapp.data.dto.RemoveCartRequestDto
import com.oax.comercioapp.data.dto.UpdateCartRequestDto
import com.oax.comercioapp.data.dto.UpdateProfileRequestDto
import com.oax.comercioapp.data.dto.ValidateTokenResponseDto
import com.oax.comercioapp.data.models.*
import retrofit2.Response
import retrofit2.http.*


/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 *
 * 1. TODOS los modelos cambian de data.models.* a data.dto.*
 *    Antes: Response<AuthResponse>, Response<List<Product>>, etc.
 *    Ahora: Response<AuthResponseDto>, Response<List<ProductDto>>, etc.
 *    Razón: ApiService es infraestructura de red — solo debe conocer DTOs,
 *    nunca entidades de dominio ni modelos con lógica de negocio.
 * 2. SE ELIMINAN los endpoints de administración de usuarios:
 *    getUsers(), getUserById(), createUser(), updateUser(), deleteUser()
 *    Razón: una app de cliente no debería tener acceso a listar y borrar
 *    usuarios de la base de datos. Esos endpoints son de backend/admin.
 *    Si algún día se necesita un módulo admin, va en un AdminApiService separado.
 *
 * 3. SE ELIMINAN los endpoints duplicados del carrito:
 *    Antes había DOS versiones:
 *      - api/cart/ (nueva, autenticada con JWT via interceptor)  ← CORRECTA
 *      - cart/user/{userId} y cart/{userId}/{productId}           ← VIEJA/DUPLICADA
 *    Quedarse con dos versiones es confuso y puede causar bugs si
 *    se llama al endpoint equivocado. Solo conservamos la versión api/cart/.
 *
 * 4. SE ELIMINAN endpoints de CRUD de productos (createProduct, updateProduct,
 *    deleteProduct) por la misma razón que los de usuarios — son operaciones
 *    de administración que el cliente final no debería ejecutar.
 *
 * 5. El métodoo getCartItemsDetailed() se renombra a getCartDetailed()
 *    para consistencia con el naming de los repositorios.
 *
 * NOTA sobre healthCheck():
 * Se conserva — es útil para verificar conectividad antes de operaciones críticas.
 * Pero su DTO (HealthCheckResponse) puede quedarse en data.models por ahora
 * ya que no tiene entidad de dominio correspondiente.
 */

interface ApiService {

    // Health
    /**
     * Verifica que el servidor esté respondiendo.
     * Útil en AuthViewModel para mostrar error de conectividad
     * antes de intentar login.
     */

    @GET("api/health")
    suspend fun healthCheck(): Response<HealthCheckResponseDto>

    // Autenticación
    @POST("api/guest/create")
    suspend fun createGuest(@Body request: GuestCreateRequestDto): Response<GuestCreateResponseDto>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    /**
     * Valida el token JWT actual.
     * El token inyecta automáticamente por AuthInterceptor -
     *  no necesita parámetro aquí.
     */
    @GET("api/auth/validate")
    suspend fun validateToken(): Response<ValidateTokenResponseDto>

    // Perfil
    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponseDto>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): Response<ProfileResponseDto>

    // Productos
    /**
     * Obtiene el catálogo completo de productos.
     * Solo lectura - la app cliente no crea ni modifica productos
     * Retorna List<ProductDto> que ProductMapper convierte a List<Product>
     */
    @GET("products")
    suspend fun getProducts(): Response<List<ProductDto>>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<ProductDto>

    // Carrito
    /**
     * Obtiene el carrito completo con detalles de produtos, subtotales y total.
     * Es el endpoint principal de la pantalla de carrito.
     *
     * IMPORTANTE: solo esta versión (api/cart/items) se conserva.
     * La versión antigua cart/user/{userId} fue eliminada para evitar ambiguëdad. El userId
     * no es necesario como path param porque el backend lo extrae del token JWT via AuthInterceptor.
     */
    @GET("api/cart/items")
    suspend fun getCartDetailed(): Response<CartDetailedResponseDto>

    /**
     * Conteo rápido de ítems en el carrito.
     * Usado para el badge del toolbar - no necesita cargar todos los detalles.
     */
    @GET("api/cart/count")
    suspend fun getCartCount(): Response<CartCountResponseDto>

    @POST("api/cart/add")
    suspend fun addToCart(@Body request: AddToCartRequestDto): Response<CartOperationResponseDto>

    @PUT("api/cart/update")
    suspend fun updateCart(@Body request: UpdateCartRequestDto): Response<CartOperationResponseDto>

    /**
     * Elimina un producto del carrito por su product_id.
     * Nota: DELETE con @Body no es ideal en REST estricto (debería ser
     * DELETE /api/cart/{product_id}), pero se conserva igual que el backend original para no
     * romper la API
     */
    @DELETE("api/cart/remove")
    suspend fun removeFromCart(@Body request: RemoveCartRequestDto): Response<CartOperationResponseDto>

    @DELETE("api/cart/clear")
    suspend fun clearCart(): Response<CartOperationResponseDto>

    @POST("api/cart/merge")
    suspend fun mergeCart(@Body request: MergeCartRequestDto): Response<MergeCartResponseDto>
}
/**
 * DTO de health check — se deja aquí porque es solo para infraestructura
 * y no tiene entidad de dominio correspondiente.
 * Si el proyecto crece, podría moverse a data/dto/SystemDtos.kt.
 */
data class HealthCheckResponseDto(
    val status: String,
    val timestamp: String,
    val version: String,
    val code: Int?
)