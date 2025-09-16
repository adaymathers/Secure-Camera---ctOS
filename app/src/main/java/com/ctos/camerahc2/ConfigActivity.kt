package com.ctos.camerahc2

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ctos.camerahc2.databinding.ActivityConfigBinding

class ConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityConfigBinding
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("ctOS_config", MODE_PRIVATE)
        
        setupUI()
        loadSavedConfig()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnSaveConfig.setOnClickListener {
            saveConfig()
        }
        
        binding.btnTestEmail.setOnClickListener {
            testEmail()
        }
    }
    
    private fun loadSavedConfig() {
        binding.etEmailAddress.setText(prefs.getString("email_address", ""))
        binding.switchAutoSend.isChecked = prefs.getBoolean("auto_send_enabled", false)
        binding.etServerUrl.setText(prefs.getString("server_url", ""))
        binding.switchUseServer.isChecked = prefs.getBoolean("use_server", false)
    }
    
    private fun saveConfig() {
        val emailAddress = binding.etEmailAddress.text.toString().trim()
        val serverUrl = binding.etServerUrl.text.toString().trim()
        
        if (emailAddress.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(this, "❌ EMAIL INVÁLIDO", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (serverUrl.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(serverUrl).matches()) {
            Toast.makeText(this, "❌ URL DE SERVIDOR INVÁLIDA", Toast.LENGTH_SHORT).show()
            return
        }
        
        val editor = prefs.edit()
        editor.putString("email_address", emailAddress)
        editor.putBoolean("auto_send_enabled", binding.switchAutoSend.isChecked)
        editor.putString("server_url", serverUrl)
        editor.putBoolean("use_server", binding.switchUseServer.isChecked)
        editor.apply()
        
        Toast.makeText(this, "✅ CONFIGURACIÓN GUARDADA", Toast.LENGTH_SHORT).show()
    }
    
    private fun testEmail() {
        val emailAddress = binding.etEmailAddress.text.toString().trim()
        
        if (emailAddress.isEmpty()) {
            Toast.makeText(this, "❌ INGRESA UN EMAIL PRIMERO", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            Toast.makeText(this, "❌ EMAIL INVÁLIDO", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crear email de prueba
        try {
            val testEmailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, "📧 ctOS Camera - Prueba de Configuración")
                putExtra(Intent.EXTRA_TEXT, """
🎯 ctOS CAMERA - TEST EMAIL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ La configuración de email está funcionando correctamente.

📱 Cuando captures una foto con overlay ctOS, 
   se enviará automáticamente a este email.

🔧 Configuración actual:
   📧 Email: $emailAddress
   🤖 Envío automático: ${if (binding.switchAutoSend.isChecked) "Activado" else "Desactivado"}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 Mensaje de prueba enviado automáticamente
                """.trimIndent())
            }
            
            if (testEmailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(testEmailIntent, "📧 Enviar email de prueba con:"))
                Toast.makeText(this, "📧 ABRIENDO EMAIL DE PRUEBA", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ NO HAY APPS DE EMAIL INSTALADAS", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "❌ ERROR AL CREAR EMAIL DE PRUEBA", Toast.LENGTH_SHORT).show()
        }
    }
}