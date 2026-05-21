package com.oax.comercioapp.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import com.oax.comercioapp.data.local.UserPreferences

/**
 * CAMBIO PRINCIPAL: UserPreferences ya no es 'object' estático.
 * El interceptor original hacía:
 *   UserPreference.getAuthToken()  <- llamada estática al singleton
 *
 * Ahora UserPreferences es una clase que se inyecta.
 * El interceptor recibe la instancia por constructor - el mismo patrón que los repositorios.
 * Esto mantiene consistencia en toda la capa de datos.
 *
 * ¿Cómo llega UserPreferences aquí si OkHttpClient se crea en RetrofitClient?
 * Con Hilt, el módulo DI construye AuthInterceotor con la instancia de UserPreferences ya creada,
 * y la pasa a OkHttpClient.Builder(). Así toda la cadena de dependencias queda resuelta por Hilt,
 * sin singletons estáticos no llamadas init()
 *
 * Por ahora (sin Hilt), RetrofitClient necesita recibir el interceptor externamente
 * o inicializarse después de que UserPreferences esté listo.
 */

class AuthInterceptor(
    private val userPreferences: UserPreferences
): Interceptor {
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener token de UserPreferences
        val token = try {
            userPreferences.getAuthToken()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth token: ${e.message}")
            null
        }

        // Si no hay token, proceder con request original
        if (token.isNullOrEmpty()) {
            Log.d(TAG, "No auth token found, proceeding without Authorization header")
            return chain.proceed(originalRequest)
        }

        // Agregar token al header
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, "$TOKEN_PREFIX$token")
            .build()

        Log.d(TAG, "Added Authorization header to ${originalRequest.url}")

        // Proceder con request modificada
        val response = chain.proceed(authenticatedRequest)

        // Log de respuesta (util para debugging)
        if (!response.isSuccessful) {
            Log.w(TAG, "Request failed with code ${response.code} for ${originalRequest.url}")
        }

        return response
    }
}