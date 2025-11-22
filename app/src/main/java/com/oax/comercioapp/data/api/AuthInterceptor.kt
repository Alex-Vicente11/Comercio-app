package com.oax.comercioapp.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import com.oax.comercioapp.data.local.UserPreferences

/**
 * Interceptor para inyectar automáticamente el token JWT en todas las requests
 *
 * Funcionalidad:
 * - Lee el token desde UserPreferences
 * - Agrega header "Authorization: Bearer {token}" a todas las requests
 * - Solo agrega el header si el token existe
 * - Permite que endpoints sin auth funcionen normalmente
 *
 * IMPORTANTE: Este interceptor requiere que UserPreferences esté inicializado
 */

class AuthInterceptor: Interceptor {
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val TOKEN_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtener token de UserPreferences
        val token = try {
            UserPreferences.getAuthToken()
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
        val newRequest = originalRequest.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, "$TOKEN_PREFIX$token")
            .build()

        Log.d(TAG, "Added Authorization header to ${originalRequest.url}")

        // Proceder con request modificada
        val response = chain.proceed(newRequest)

        // Log de respuesta (util para debugging)
        if (!response.isSuccessful) {
            Log.w(TAG, "Request failed with code ${response.code} for ${originalRequest.url}")
        }

        return response
    }
}