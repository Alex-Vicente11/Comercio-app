package com.oax.comercioapp.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/*
Singleton que gestiona la configuracion de Retrofit para comunicacion con el API
Caracteristicas:
- Logging HTTP para debugging
- AuthInterceptor para inyeccion automatica de tokens JWT
- Timeouts configurados para red lenta
- Gson con modo lenient para mejor compatibilidad
 */

object RetrofitClient {
    /*
    Base URL for the Flask API running on localhost
    For Android Emulator, use 10.0.2.2 instead of localhost
    For physical device, use your computer's IP address
     */
    private const val BASE_URL = "http://172.23.81.161:5002/"

    // Flag para habilitar/deshabilitar logs en produccion
    private const val ENABLE_LOOGING = true

    /*
    - Logging Interceptor para debugging
    - Muestra requests y responses en Logcat
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (ENABLE_LOOGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }


    /*
    OkHttpClient con interceptors configurados:
    - loggingInterceptor: Para debugging
    - AuthInterceptor: Para inyectar tokens JWT automaticamente
    - Timeouts: 30 segundos para conectar/leer/escribir
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(AuthInterceptor())  // Para agregar token automaticamente
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()


    /*
     GSON CONFIGURATION
     - Configuracion de Gson para parseo JSON
     setLenient(): Permite JSON con formato mas flexible
     util cuando el backend no envia JSON estricto
     */
    private val gson = GsonBuilder()
        .setLenient()
        .create()


    /*
    RETROFIT INSTANCE
    - Instancia de Retrofit (lazy initialized)
    - Se crea solo cuando se accede por primera vez
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }


    /*
    API Service interface (lazy initialized)
    Contiene todos los endpoints del API
     */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }


    /*
    UTILITY FUNCTIONS
    - Obtener la URL base actual
    - Util para debugging o mostrar configuracion
     */
    fun getBaseUrl(): String = BASE_URL


    /* Verificar si logging esta habilitado */
    fun isLoggingEnabled(): Boolean = ENABLE_LOOGING

    /**
     * Helper para actualizar BASE_URL dinámicamente
     *
     * NOTA: Esta función es para referencia, pero NO se recomienda
     * cambiar la URL en runtime. En su lugar, usar:
     * 1. Build variants (debug/release)
     * 2. BuildConfig con diferentes URLs
     * 3. SharedPreferences para URL de desarrollo
     *
     * @param ipAddress Nueva IP del servidor
     */
    @Deprecated(
        message = "No recomendado cambiar URL en runtime. Usar BuildConfig en su lugar",
        level = DeprecationLevel.WARNING
    )
    fun updateBaseUrl(ipAddress: String) {
        // Esta función requeriría hacer apiService mutable
        // Lo cual NO es thread-safe y puede causar problemas

        // Mejor enfoque: Reiniciar la app con nueva configuración
        throw UnsupportedOperationException(
            "Para cambiar URL, modificar BASE_URL y recompilar la app"
        )
    }
}