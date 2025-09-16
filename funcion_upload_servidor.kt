    private fun uploadToWebServer(imageFile: java.io.File) {
        val serverUrl = prefs.getString("server_url", "") ?: ""
        val notifyEmail = prefs.getString("email_address", "") ?: ""
        
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "🌐 CONFIGURA SERVIDOR EN CONFIGURACIÓN", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "⬆️ SUBIENDO AUTOMÁTICAMENTE...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image", 
                        imageFile.name,
                        imageFile.asRequestBody("image/jpeg".toMediaType())
                    )
                    .addFormDataPart("email", notifyEmail)
                    .addFormDataPart("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    .addFormDataPart("location", currentLocation?.let { 
                        "${"%.6f".format(it.latitude)},${"%.6f".format(it.longitude)}"
                    } ?: "unknown")
                    .build()
                
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "✅ IMAGEN ENVIADA Y EMAIL NOTIFICADO", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "❌ ERROR DEL SERVIDOR", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ ERROR DE CONEXIÓN", Toast.LENGTH_SHORT).show()
                    Log.e("CameraHC2", "Upload error: ${e.message}", e)
                }
            }
        }
    }