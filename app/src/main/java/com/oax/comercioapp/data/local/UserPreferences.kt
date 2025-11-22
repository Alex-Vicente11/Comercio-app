package com.oax.comercioapp.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.oax.comercioapp.data.models.User
import java.util.UUID

/**
 * UserPreferences - Sistema de almacenamiento seguro con cifrado
 *
 * Características:
 * - EncryptedSharedPreferences para datos sensibles (tokens, emails)
 * - Soporte para usuarios anónimos (guest users)
 * - Migración automática desde PreferencesManager
 * - Compatible con código existente
 * - Gestión de tokens JWT
 *
 * IMPORTANTE: Llamar a init(context) antes de usar cualquier método
 */

object UserPreferences {

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

    // SharedPreferences cifrado
    private lateinit var prefs: SharedPreferences

    // Flag de inicializacion
    private var isInitialized = false


    // INICIALIZACIÓN
    // ============================================
    /**
     * Inicializa UserPreferences con EncryptedSharedPreferences
     *
     * DEBE llamarse en onCreate() de MainActivity o Application class
     *
     * a@param context Contexto de la aplicación
     */

    fun init (context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Ya está inicializado")
        }

        try {
            Log.d(TAG, "Inicializando UserPreferences con cifrado..")

            // Crear MasterKey para cifrado AES256
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            isInitialized = true
            Log.d(TAG, "Inicializacion exitosa")

            // Migrar datos antiguos si es primera vez
            migrateFromOldPreferences(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar ${e.message}", e)

            // Fallback a SharedPreferences normal si falla el cifrado
            Log.w(TAG, "Usando SharedPreferences sin cifrado como fallback")
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isInitialized = true
        }
    }

    // GESTIÓN DE GUEST ID
    // ============================================

    /**
     * Obtiene el guest_id actual o crea uno nuevo
     *
     * Formato: guest_{UUID}_{timestamp}
     * Ejemplo: guest_a1b2c3d4-e5f6-7890-abcd-ef1234567890_1699123456789
     *
     * a@return guest_id único
     */

    fun getOrCreateGuestId(): String {
        checkInitialized()

        var guestId = prefs.getString(KEY_GUEST_ID, null)

        if (guestId == null) {
            guestId = "guest_${UUID.randomUUID()}_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_GUEST_ID, guestId).apply()
            Log.d(TAG, "Guest ID creado: $guestId")
        } else {
            Log.d(TAG, "Guest ID existente: $guestId")
        }

        return guestId
    }

    /**
     * Limpia el guest_id (se usa después de registro/login)
     */

    fun clearGuestId() {
        checkInitialized()
        prefs.edit().remove(KEY_GUEST_ID).apply()
        Log.d(TAG, "Guest ID eliminado")
    }

    // GESTIÓN DE USUARIO
    // ============================================
    /**
     * Guarda los datos del usuario actual
     *
     * a@param user Usuario a guardar (puede ser guest o autenticado)
     */

    fun saveUser (user: User) {
        checkInitialized()

        try {
            prefs.edit().apply {
                putInt(KEY_USER_ID, user.idUser)
                putString(KEY_USER_NAME, user.userName)
                putString(KEY_EMAIL, user.email)
                putString(KEY_AUTH_TOKEN, user.token)
                putBoolean(KEY_IS_GUEST, user.isGuest)
                putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
                apply()
            }

            // Limpiar guest_id si ya no es guest
            if (!user.isGuest) {
                clearGuestId()
            }

            val userType = if (user.isGuest) "GUEST" else "AUTHENTICATED"
            Log.d(TAG, " Usuario guardado: ${user.userName} (ID: ${user.idUser}) - Tipo: $userType")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar usuario: ${e.message}", e)
        }
    }

    /**
     * Obtiene el ID del usuario actual
     *
     *a @return ID del usuario, o 0 si no hay usuario
     */

    fun getUserId(): Int {
        checkInitialized()
        return prefs.getInt(KEY_USER_ID, 0)
    }

    /**
     * Obtiene el nombre del usuario actual
     *
     * a@return Nombre del usuario, o null si no hay usuario
     */
    fun getUserName(): String? {
        checkInitialized()
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Obtiene el email del usuario actual
     *
     *a @return Email del usuario, o null si es guest o no hay usuario
     */
    fun getEmail(): String? {
        checkInitialized()
        return prefs.getString(KEY_EMAIL, null)
    }

    /**
     * Obtiene el token JWT del usuario autenticado
     *
     * a@return Token JWT, o null si es guest o no hay token
     */

    fun getAuthToken(): String? {
        checkInitialized()
        val token = prefs.getString(KEY_AUTH_TOKEN, null)

        if (token != null) {
            Log.d(TAG,"Token encontrado (${token.take(20)}...")
        } else {
            Log.d(TAG,"No hay token")
        }

        return token
    }

    /**
     * Verifica si el usuario actual es guest
     *
     * @return true si es guest, false si es autenticado o no hay usuario
     */
    fun isGuest(): Boolean {
        checkInitialized()
        return prefs.getBoolean(KEY_IS_GUEST, true)
    }

    /**
     * Verifica si hay una sesión activa (guest o autenticado)
     *
     * @return true si hay usuario logueado
     */
    fun isLoggedIn(): Boolean {
        checkInitialized()
        val userId = getUserId()
        val isLoggedIn = userId > 0

        Log.d(TAG, "¿Sesión activa? $isLoggedIn (UserID: $userId)")
        return isLoggedIn
    }

    /**
     * Obtiene el timestamp del último login
     *
     * @return Timestamp en milisegundos, o 0 si no hay login
     */
    fun getLoginTimestamp(): Long {
        checkInitialized()
        return prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }

    /**
     * Limpia todos los datos del usuario actual
     *
     * NOTA: NO limpia el guest_id, solo los datos de sesión
     */
    fun clearUser() {
        checkInitialized()

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

        Log.d(TAG, "Usuario limpiado: $userName")
    }

    /**
     * Limpia TODOS los datos incluyendo guest_id
     *
     * USAR CON CUIDADO: Limpia toda la información de sesión
     */
    fun clearAll() {
        checkInitialized()
        prefs.edit().clear().apply()
        Log.d(TAG, "Todas las preferencias limpiadas")
    }

    // MIGRACIÓN DE DATOS
    // ============================================
    /**
     * Migra datos desde PreferencesManager (sistema antiguo)
     *
     * Solo se ejecuta una vez, en la primera inicialización
     */

    private fun migrateFromOldPreferences(context: Context) {
        try {
            // Verificar si ya se migró
            if (prefs.getBoolean(KEY_MIGRATED, false)) {
                Log.d(TAG, "Migración ya ejecutada anteriormente")
                return
            }

            Log.d(TAG, "Iniciando migración desde PreferencesManager...")

            // Leer datos antiguos
            val oldPrefs = context.getSharedPreferences("comercio_app_prefs", Context.MODE_PRIVATE)
            val hasOldSession = oldPrefs.getBoolean("is_logged_in", false)

            if (hasOldSession) {
                val oldUserId = oldPrefs.getInt("user_id", 0)
                val oldUserName = oldPrefs.getString("user_name", null)
                val oldLoginTimestamp = oldPrefs.getLong("login_timestamp", 0L)

                if (oldUserId > 0 && oldUserName != null) {
                    Log.d(TAG, "Datos antiguos encontrados: $oldUserName (ID: $oldUserId)")

                    // Migrar a nuevo sistema
                    prefs.edit().apply {
                        putInt(KEY_USER_ID, oldUserId)
                        putString(KEY_USER_NAME, oldUserName)
                        putBoolean(KEY_IS_GUEST, false)
                        putLong(KEY_LOGIN_TIMESTAMP, oldLoginTimestamp)
                        putBoolean(KEY_MIGRATED, true)
                        apply()
                    }

                    Log.d(TAG, "Migración exitosa: $oldUserName")
                } else {
                    Log.d(TAG, "Datos antiguos inválidos, saltando migración")
                    prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
                }
            } else {
                Log.d(TAG, "No hay sesión antigua para migrar")
                prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en migración: ${e.message}", e)
            // Marcar como migrado para no reintentar
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }


    // ====================
    // UTILIDADES DE DEBUG
    // ====================
    /**
     * Obtiene información completa de la sesión actual
     *
     * .@return String con todos los datos de sesión (útil para debugging)
     */

    fun getSessionDebugInfo(): String {
        checkInitialized()

        val userId = getUserId()
        val userName = getUserName()
        val email = getEmail()
        val isGuest = isGuest()
        val hasToken = getAuthToken() != null
        val loginTime = getLoginTimestamp()

        val loginTimeFormatted = if (loginTime > 0) {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(loginTime))
        } else {
            "N/A"
        }

        return buildString {
            append("=== SESIÓN ACTUAL === \n")
            append("=== SESIÓN ACTUAL ===\n")
            append("User ID: $userId\n")
            append("Nombre: ${userName ?: "N/A"}\n")
            append("Email: ${email ?: "N/A"}\n")
            append("Tipo: ${if (isGuest) "GUEST" else "AUTENTICADO"}\n")
            append("Token: ${if (hasToken) "SÍ" else "NO"}\n")
            append("Login: $loginTimeFormatted\n")

            if (isGuest) {
                val guestId = prefs.getString(KEY_GUEST_ID, null)
                append("Guest ID: ${guestId ?: "No generado"}\n")

            }
        }
    }


    /**
     * Obtiene todas las preferencias guardadas
     *
     *  Solo para debugging, no expone valores sensibles
     */

    fun getAllPreferencesDebug(): String {
        checkInitialized()

        return buildString {
            append("=== PREFERENCIAS GUARDADAS ===\n")
            append("User ID: ${getUserId()}\n")
            append("User Name: ${getUserName() ?: "N/A"}\n")
            append("Email: ${if (getEmail() != null) "***@***.***" else "N/A"}\n")
            append("Is Guest: ${isGuest()}\n")
            append("Has Token: ${getAuthToken() != null}\n")
            append("Logged In: ${isLoggedIn()}\n")
            append("Migrated: ${prefs.getBoolean(KEY_MIGRATED, false)}\n")
        }
    }

    /**
     * Verifica si el token ha expirado (opcional)
     *
     * .@param maxAgeHours Edad máxima del token en horas (default: 24)
     * .@return true si el token ha expirado o no existe
     */

    fun isTokenExpired(maxAgeHours: Int = 24): Boolean {
        checkInitialized()

        val token = getAuthToken()
        if (token == null) return true

        val loginTime = getLoginTimestamp()
        if (loginTime == 0L) return true

        val currentTime = System.currentTimeMillis()
        val elapsedHours = (currentTime - loginTime) / (1000 * 60 * 60)

        return elapsedHours > maxAgeHours
    }


    // ===================
    // VALIDACIÓN INTERNA
    // ===================

    /**
     * Verifica que UserPreferences esté inicializado
     *
     * .@throws IllegalStateException si no está inicializado
     */

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "UserPreferences no está inicializado. " +
                "Llamar UserPreferences.init(context) antes de usar."
            )
        }
    }

    // ============================================
    // COMPATIBILIDAD CON CÓDIGO EXISTENTE
    // ============================================

    /**
     * Guarda sesión de usuario (compatibilidad con PreferencesManager)
     *
     * .@deprecated Usar saveUser(User) en su lugar
     */
    @Deprecated(
        message = "Usar saveUser(User) para tener todos los datos",
        replaceWith = ReplaceWith("saveUser(User(userId, userName))"),
        level = DeprecationLevel.WARNING
    )
    fun saveUserSession(userId: Int, userName: String) {
        checkInitialized()
        saveUser(User(
            idUser = userId,
            userName = userName,
            email = null,
            isGuest = false,
            token = null
        ))
    }

    /**
     * Limpia sesión de usuario (compatibilidad con PreferencesManager)
     *
     * @deprecated Usar clearUser() en su lugar
     */
    @Deprecated(
        message = "Usar clearUser() en su lugar",
        replaceWith = ReplaceWith("clearUser()"),
        level = DeprecationLevel.WARNING
    )
    fun clearUserSession() {
        clearUser()
    }

    /**
     * Verifica sesión activa (compatibilidad con PreferencesManager)
     *
     * @deprecated Usar isLoggedIn() en su lugar
     */
    @Deprecated(
        message = "Usar isLoggedIn() en su lugar",
        replaceWith = ReplaceWith("isLoggedIn()"),
        level = DeprecationLevel.WARNING
    )
    fun hasActiveSession(): Boolean {
        return isLoggedIn()
    }
}
