package com.oax.comercioapp.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "comercio_app_prefs"

    // keys para la sesion de usuario
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"

    // keys para preferencias de usuario (para la siguiente etapa)
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_LANGUAGE = "language"

    private lateinit var preferences: SharedPreferences

    fun init(context : Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    // Gestion de sesion (guardar los datos de sesión del usuario
    fun saveUserSession(userId: Int, userName: String) {
        preferences.edit().apply() {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        println("PreferencesManager: Sesión guardada - Usuario: $userName (ID: $userId)")
    }

    fun clearUserSession() {
        val userName = getUserName()
        preferences.edit().apply() {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_LOGIN_TIMESTAMP)
            apply()
        }
        println("PreferencesManager: Sesión limpiada - Usuario: $userName")
    }

    // Verificar si hay una sesion guardada
    fun  hasActiveSession(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Obtener el id del usuario guardado
    fun getUserId(): Int {
        return preferences.getInt(KEY_USER_ID, -1)
    }

    // Obtener el nombre del usuario guardado
    fun getUserName(): String? {
        return preferences.getString(KEY_USER_NAME, null)
    }

    // Obtener el timestamp de cuando se hizo login
    fun getLoginTimestamp(): Long {
        return preferences.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }

    // Informacion de debug de la sesion actual
    fun getSessionDebugInfo(): String {
        return if (hasActiveSession()) {
            val loginTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(getLoginTimestamp()))
            "Sesión activa:\n" +
                    "Usuario: ${getUserName()}\n" +
                    "ID: ${getUserId()}\n" +
                    "Login: $loginTime"
        }else {
            "No hay sesión activa"
        }
    }

    // Preferencias generales (para siguientes etapas)
    fun setThemeMode(mode: String) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(): String {
        return preferences.getString(KEY_THEME_MODE, "system") ?: "system"
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areNoticationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setLanguage(language: String) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun getLanguage(): String {
        return preferences.getString(KEY_LANGUAGE, "es") ?: "es"
    }

    // Utilidades

    // Limpiar todas la preferencias (usar con cuidado)
    fun clearAllPreferences() {
        preferences.edit().clear().apply()
        println("PreferencesManager: Todas las preferencias limpiadas")
    }

    // Información completa de todas las preferencias guardadas
    fun getAllPreferencesInfo(): String {
        val all = preferences.all
        return buildString {
            append("=== TODAS LAS PREFERENCIAS ===\n")
            if (all.isEmpty()) {
                append("No hay preferencias guardadas")
            }else {
                all.forEach { (key, value) ->
                    append("$key: $value\n")
                }
            }
        }
    }
}