package com.ctos.camerahc2.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * 🔐 Sistema de Verificación de Licencia Simplificado
 * Versión independiente para desarrollo y testing
 */
class LicenseChecker(private val context: Context) {
    
    // Clave pública de Google Play (para producción)
    private val playStorePublicKey = "YOUR_GOOGLE_PLAY_PUBLIC_KEY_HERE"
    
    interface LicenseCheckerCallback {
        fun allow(reason: Int)
        fun dontAllow(reason: Int)
        fun applicationError(errorCode: Int)
    }
    
    fun checkAccess(callback: LicenseCheckerCallback) {
        try {
            // Verificación básica de instalación
            if (isInstalledFromPlayStore()) {
                Log.d("LicenseChecker", "✅ App instalada desde Play Store")
                callback.allow(LICENSED)
            } else {
                Log.w("LicenseChecker", "⚠️ App no instalada desde Play Store")
                // En desarrollo permitir, en producción bloquear
                callback.allow(LICENSED) // Cambiar a dontAllow(NOT_LICENSED) en producción
            }
        } catch (e: Exception) {
            Log.e("LicenseChecker", "❌ Error verificando licencia: ${e.message}")
            callback.applicationError(ERROR_CHECK_IN_PROGRESS)
        }
    }
    
    private fun isInstalledFromPlayStore(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending" || installer == "com.google.android.feedback"
    }
    
    fun onDestroy() {
        // Limpiar recursos si es necesario
        Log.d("LicenseChecker", "LicenseChecker destruido")
    }
    
    companion object {
        const val LICENSED = 0x0100
        const val NOT_LICENSED = 0x0231
        const val ERROR_CHECK_IN_PROGRESS = 0x05
    }
}