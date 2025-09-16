package com.ctos.camerahc2.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log

/**
 * 🔐 Detector de Manipulación Simplificado
 * Versión básica para desarrollo y testing
 */
class AntiTamperingDetector(private val context: Context) {
    
    data class SecurityReport(
        val isSecure: Boolean,
        val threats: List<String> = emptyList(),
        val riskLevel: RiskLevel = RiskLevel.LOW
    )
    
    enum class RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    fun performSecurityCheck(): SecurityReport {
        val threats = mutableListOf<String>()
        var riskLevel = RiskLevel.LOW
        
        // 1. Verificar si está en debugging
        if (isBeingDebugged()) {
            threats.add("DEBUGGING_DETECTED")
            riskLevel = RiskLevel.HIGH
            Log.w("AntiTampering", "⚠️ Debugging detectado")
        }
        
        // 2. Verificar integridad básica
        if (!verifyBasicIntegrity()) {
            threats.add("INTEGRITY_COMPROMISED")
            riskLevel = RiskLevel.CRITICAL
            Log.w("AntiTampering", "⚠️ Integridad comprometida")
        }
        
        // 3. Verificar fuente de instalación
        if (!isInstalledFromTrustedSource()) {
            threats.add("UNTRUSTED_SOURCE")
            if (riskLevel < RiskLevel.MEDIUM) riskLevel = RiskLevel.MEDIUM
            Log.w("AntiTampering", "⚠️ Fuente no confiable")
        }
        
        // 4. Verificar emulador (solo advertencia en desarrollo)
        if (isRunningOnEmulator()) {
            threats.add("EMULATOR_DETECTED")
            Log.i("AntiTampering", "ℹ️ Ejecutándose en emulador")
        }
        
        val isSecure = threats.isEmpty() || riskLevel <= RiskLevel.MEDIUM
        
        Log.d("AntiTampering", "🔍 Reporte de seguridad: ${threats.size} amenazas, nivel: $riskLevel")
        
        return SecurityReport(
            isSecure = isSecure,
            threats = threats,
            riskLevel = riskLevel
        )
    }
    
    private fun isBeingDebugged(): Boolean {
        return Debug.isDebuggerConnected() || 
               Debug.waitingForDebugger() ||
               (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    private fun verifyBasicIntegrity(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName, 
                PackageManager.GET_SIGNATURES
            )
            packageInfo.signatures.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isInstalledFromTrustedSource(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending" || 
               installer == "com.google.android.feedback" ||
               installer == null // null para desarrollo
    }
    
    private fun isRunningOnEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
               Build.FINGERPRINT.startsWith("unknown") ||
               Build.MODEL.contains("google_sdk") ||
               Build.MODEL.contains("Emulator") ||
               Build.MODEL.contains("Android SDK built for x86") ||
               Build.MANUFACTURER.contains("Genymotion") ||
               (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }
    
    fun isSecureEnvironment(): Boolean {
        return performSecurityCheck().isSecure
    }
}