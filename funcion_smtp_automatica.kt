    private fun sendImageBySMTP(imageFile: java.io.File) {
        val emailAddress = prefs.getString("email_address", "") ?: ""
        val smtpUser = prefs.getString("smtp_user", "") ?: ""
        val smtpPassword = prefs.getString("smtp_password", "") ?: ""
        
        if (emailAddress.isEmpty() || smtpUser.isEmpty() || smtpPassword.isEmpty()) {
            Toast.makeText(this, "📧 CONFIGURA SMTP EN CONFIGURACIÓN", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "📧 ENVIANDO EMAIL AUTOMÁTICO...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Configuración SMTP para Gmail
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }
                
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(smtpUser, smtpPassword)
                    }
                })
                
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(smtpUser))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress))
                    subject = "📸 ctOS AUTO - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
                    
                    // Crear mensaje multipart
                    val multipart = MimeMultipart()
                    
                    // Texto del email
                    val textPart = MimeBodyPart().apply {
                        val locationText = currentLocation?.let { 
                            "GPS: ${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}"
                        } ?: "GPS: No disponible"
                        
                        setText("""
📸 ctOS SURVEILLANCE AUTO-CAPTURE
${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
$locationText
                        """.trimIndent())
                    }
                    multipart.addBodyPart(textPart)
                    
                    // Adjuntar imagen
                    val imagePart = MimeBodyPart().apply {
                        val dataSource = FileDataSource(imageFile)
                        dataHandler = DataHandler(dataSource)
                        fileName = imageFile.name
                    }
                    multipart.addBodyPart(imagePart)
                    
                    setContent(multipart)
                }
                
                // Enviar email
                Transport.send(message)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✅ EMAIL ENVIADO AUTOMÁTICAMENTE", Toast.LENGTH_LONG).show()
                    Log.d("CameraHC2", "Email SMTP enviado: ${imageFile.name}")
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ ERROR SMTP: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("CameraHC2", "Error SMTP: ${e.message}", e)
                    
                    // Fallback al método tradicional
                    sendImageByEmail(imageFile)
                }
            }
        }
    }