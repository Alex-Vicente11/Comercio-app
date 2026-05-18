package com.oax.comercioapp.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.oax.comercioapp.domain.model.AuthResult
import java.util.UUID

/**
 * CAMBIOS RESPECTO AL ORIGINAL y sus razones:
 *  1. YA NO ES 'object' - ahora es una clase normal inyectable
 *     El original era 'object UserPreferences' (singleton estático)
 *     Problema: los repositorios lo llamaban con UserPreferences.saveUser(...)
 *     directamente, lo que hace imposible escribir tests unitarios
 *     (no se puede ser reemplazado con un fake/mock).
 *     Ahora se instancia una vez en el módulo de DI (Hilt) y se inyecta donde se necesite
 *     - igual que ApiService.
 *  2. saveUser(User) -> saveAuthResult(AuthResult)
 *     El original recibía 'data.models.User' que tiene @SerializedName (acoplamiento a Gson/red).
 *     Los repositorios ahora trabajan con AuthResult del dominio. Este cambio rompe ese acoplamiento.
 *  3. Se agrega updateUserName() - métodoo específico que UserRepositoryImpl necesita después de
 *     actualizar el perfil (solo cambia el nombre, no toda la sesión).
 *  4. Se conserva la lógica de EncryptedSharedPreferences y la migración porque eso si esta bien
 *     hecho y no tiene razon de cambio.
 *  5. Los métodoss @Deprecated de compatibilidad se eliminan - ya no hay código que los llame una
 *     vez que los Impl estén activos.
 *  NOTA sobre 'init(context)':
 *  El patrón cambia - ya no hay que llamar init() manualmente.
 *  El Context se pasa al constructor y se inicializa en el momento de instanciación (en el módulo de DI).
 *  Menos propenso a olvidar la llamada.
 *
 */

class UserPreferences(context: Context) {

    private val appContext = context.applicationContext // Evitar memory leak con Activity context

    companion object {
        private const val TAG = "UserPreferences"

        // Nombre del archivo cifrado
        private const val PREFS_NAME = "user_secure_prefs"

        // Keys para datos de usuario
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"

        // Key para controlar migracion
        private const val KEY_MIGRATED = "migrated_from_old_prefs"
    }

    /**
     * SharedPreferences inicializando de forma lazy.
     * Se crea al primer acceso, no en el constructor.
     * Si falla el cifrado, cae al fallback sin cifrar (mismo comportamiento que antes).
     */
    // SharedPreferences cifrado
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                Log.d(TAG, "EncryptedSharedPreferences inicializando correctamente")
                migrateFromOldPreferences()
            }
        }catch (e: Exception) {
            Log.e(TAG, "Error al inicializar cifrado, usando fallback: ${e.message}")
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // --- Gestión de sesión ---
    /**
     * Guarda el resultado de autenticación (login, register, guest).
     *
     * CAMBIO CLAVE: recibe AuthResult (dominio) en vez de User (data.models).
     * Esto rompe el acoplamiento entre la capa local y los modelos de red.
     * Si la estructura de User en data.models cambia, este métodoo no se entera.
     *
     * El token puede ser vacío ("") para usuarios guest - se guarda null
     * en ese caso para mantener consistencia con getAuthToken() retornando null
     */
    fun saveAuthResult(authResult: AuthResult) {
        try {
            prefs.edit().apply {
                putInt(KEY_USER_ID, authResult.userId)
                putString(KEY_USER_NAME, authResult.userName)
                putString(KEY_EMAIL, authResult.email)
                putString(KEY_AUTH_TOKEN, authResult.token.ifEmpty { null })
                putBoolean(KEY_IS_GUEST, authResult.isGuest)
                putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
                apply()
            }

            // Si se autenticó, ya no necesitamos el guest_id
            if (!authResult.isGuest) clearGuestId()

            val type = if (authResult.isGuest) "GUEST" else "AUTENTICADO"
            Log.d(TAG, "Sesión guardada: ${authResult.userName} - $type")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar sesión: ${e.message}", e)
        }
    }

    /**
     * Actualiza solo el nombre de usuario, sin tocar el resto de la sesión.
     * Usado por UserRepositoryImpl.updateProfile() - el servidor confirma el nuevo nombre y
     * solo actualizamos ese campo localmente.
     */
    fun updateUserName(newUserName: String) {
        try {
            prefs.edit().putString(KEY_USER_NAME, newUserName).apply()
            Log.d(TAG, "Nombre actualizado: $newUserName")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar nombre: ${e.message}", e)
        }
    }

    /**
     * Limpia los datos de la sesión del usuario actual.
     * NO limpia el guest_id - se conserva para que el siguiente
     * usuario guest de este dispositivo tenga continuidad de carrito
     */
    fun clearUser() {
        val userName = getUserName()
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_EMAIL)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_IS_GUEST)
            remove(KEY_LOGIN_TIMESTAMP)
            apply()
        }
        Log.d(TAG, "Sesión limpiada: $userName")
    }

    /**
     * Limpia absolutamente todoo, incluyendo guest_id- Usar en desinstalación/reset.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Todas las preferencias limpiadas")
    }

    // -- Getters de sesión ---

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun isGuest(): Boolean = prefs.getBoolean(KEY_IS_GUEST, true)
    fun getLoginTimestamp(): Long = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)

    fun getAuthToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        Log.d(TAG, if (token != null) "Token encontrado (${token.take(20)}...)" else "Sin token")
        return token
    }

    /**
     * Hay sesión activa si hay un userId válido guardado.
     * Aplica tanto a usuarios autenticados como guests con userId asignado
     */
    fun isLoggedIn(): Boolean {
        val userId = getUserId()
        return (userId > 0).also {
            Log.d(TAG, "¿Sesión activa? $it (userId = $userId")
        }
    }

    // --- Gestión de Guest ID ---
    /**
     * Obtiene el guest_id existente o genera uno nuevo
     * Formato: guest_{UUID}_{timestamp}
     */
    fun getOrCreateGuestId(): String {
        return prefs.getString(KEY_GUEST_ID, null) ?: run {
            val newId = "guest_${UUID.randomUUID()}_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_GUEST_ID, newId).apply()
            Log.d(TAG, "Guest ID creado: $newId")
            newId
        }
    }

    fun clearGuestId() {
        prefs.edit().remove(KEY_GUEST_ID).apply()
        Log.d(TAG, "Guest ID eliminado")
    }

    // --- Expiración de token ---
    /**
     * Verificación local de expiración basada en timestamp de login.
     * No reemplaza la validación real del servidor (validateToken()),
     * sirve como check rápido antes de hacer llamdas de red.
     */
    fun isTokenExpiredLocally(maxAgeHours: Int = 24): Boolean {
        val token = getAuthToken() ?: return true
        val loginTime = getLoginTimestamp()
        if (loginTime == 0L) return true
        val elapsedHours = (System.currentTimeMillis() - loginTime) / (1000 * 60 * 60)
        return elapsedHours > maxAgeHours
    }

    // --- Migración ---
    /**
     * Migra datos desde PreferencesManager (sistema antiguo).
     * Se llama automáticamente una solo vez al crear la instancia.
     * Conservada del original - la lógica de migración estaba bien.
     */
    private fun migrateFromOldPreferences() {
        try {
            if (prefs.getBoolean(KEY_MIGRATED, false)) return

            Log.d(TAG, "Verificando migración desde PreferencesManager...")
            val oldPrefs = appContext.getSharedPreferences("comercio_app_prefs", Context.MODE_PRIVATE)

            if (oldPrefs.getBoolean("is_logged_in", false)) {
                val oldUserId = oldPrefs.getInt("user_id", 0)
                val oldUserName = oldPrefs.getString("user_name", null)
                val oldTimestamp = oldPrefs.getLong("login_timestamp", 0L)

                if (oldUserId > 0 && oldUserName != null) {
                    prefs.edit().apply {
                        putInt(KEY_USER_ID, oldUserId)
                        putString(KEY_USER_NAME, oldUserName)
                        putBoolean(KEY_IS_GUEST, false)
                        putLong(KEY_LOGIN_TIMESTAMP, oldTimestamp)
                        putBoolean(KEY_MIGRATED, true)
                        apply()
                    }
                    Log.d(TAG, "Migración exitosa: $oldUserName")
                } else {
                    prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
                }
            } else {
                prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en migración: ${e.message}", e)
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    // Debug
    /**
     * Solo para debugging - no expone valores sensibles.
     * Llamar desde un Fragment/Activity en builds de debug.
     */
    fun getSessionDebugInfo(): String = buildString {
        append("=== SESIÓN ACTUAL ===\n")
        append("User ID: ${getUserId()}\n")
        append("Nombre: ${getUserName() ?: "N/A"}\n")
        append("Email: ${getEmail()?.replace(Regex(".(?=.*@)"), "*") ?: "N/A"}\n")
        append("Tipo: ${if (isGuest()) "GUEST" else "AUTENTICADO"}\n")
        append("Token: ${if (getAuthToken() != null) "SÍ" else "NO"}\n")
        append("Expirado (local): ${isTokenExpiredLocally()}\n")
        if (isGuest()) append("Guest ID: ${prefs.getString(KEY_GUEST_ID, "No generado")}\n")
    }
}
