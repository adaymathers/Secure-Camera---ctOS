package com.ctos.camerahc2.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 🔐 Sistema de Autenticación Online Simplificado
 * Versión básica para desarrollo y testing
 */
class OnlineAuthenticator(private val context: Context) {
    
    // Configuración del servidor (para producción)
    private val AUTH_SERVER_URL = "https://tu-servidor.com/api/validate-license"
    private val API_KEY = "TU_API_KEY_SECRETA"
    
    private val prefs: SharedPreferences = context.getSharedPreferences("ctOS_auth", Context.MODE_PRIVATE)
    
    data class AuthResult(
        val isValid: Boolean,
        val message: String,
        val remainingDays: Int = -1
    )
    
    interface AuthCallback {
        fun onAuthSuccess(result: AuthResult)
        fun onAuthFailure(error: String)
    }
    
    fun authenticateUser(callback: AuthCallback) {
        try {
            // Simulación de autenticación para desarrollo
            Log.d("OnlineAuth", "🔍 Verificando autenticación...")
            
            // En desarrollo siempre permitir
            val result = AuthResult(
                isValid = true,
                message = "Licencia válida para desarrollo",
                remainingDays = 365
            )
            
            // Guardar estado de autenticación
            prefs.edit()
                .putBoolean("auth_valid", result.isValid)
                .putLong("last_check", System.currentTimeMillis())
                .apply()
            
            callback.onAuthSuccess(result)
            
        } catch (e: Exception) {
            Log.e("OnlineAuth", "❌ Error en autenticación: ${e.message}")
            callback.onAuthFailure("Error de conexión")
        }
    }
    
    fun isLastAuthValid(): Boolean {
        val isValid = prefs.getBoolean("auth_valid", false)
        val lastCheck = prefs.getLong("last_check", 0)
        val hoursSinceCheck = (System.currentTimeMillis() - lastCheck) / (1000 * 60 * 60)
        
        // Válido por 24 horas
        return isValid && hoursSinceCheck < 24
    }
    
    fun clearAuthData() {
        prefs.edit().clear().apply()
        Log.d("OnlineAuth", "🗑️ Datos de autenticación limpiados")
    }
}