package com.ctos.camerahc2

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.location.Location
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.ctos.camerahc2.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: SharedPreferences
    
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var currentRecording: Recording? = null
    private var isRecording = false
    private var isVideoRecording = false
    private var btnVideo: Button? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var currentLocation: Location? = null
    private val updateHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    
    // Variables para orientación
    private var currentOrientation = 0
    private var isLandscape = false
    private lateinit var orientationEventListener: OrientationEventListener
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        // 📍 Aceptar cualquier tipo de permiso de ubicación
        val locationGranted = fineLocationGranted || coarseLocationGranted
        
        android.util.Log.d("ctOS_Permisos", "📷 Cámara: $cameraGranted")
        android.util.Log.d("ctOS_Permisos", "🎤 Audio: $audioGranted")
        android.util.Log.d("ctOS_Permisos", "📍 Ubicación precisa: $fineLocationGranted")
        android.util.Log.d("ctOS_Permisos", "📍 Ubicación aproximada: $coarseLocationGranted")
        
        if (cameraGranted && audioGranted && locationGranted) {
            startCamera()
            startLocationUpdates() // 🚀 Iniciar GPS después de permisos
        } else {
            val missingPerms = mutableListOf<String>()
            if (!cameraGranted) missingPerms.add("Cámara")
            if (!audioGranted) missingPerms.add("Audio")
            if (!locationGranted) missingPerms.add("Ubicación")
            
            Toast.makeText(this, "Permisos necesarios: ${missingPerms.joinToString(", ")}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔐 VERIFICACIONES DE SEGURIDAD ANTI-PIRATERÍA
        performSecurityChecks()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        prefs = getSharedPreferences("ctOS_config", MODE_PRIVATE)
        
        setupOrientationListener()
        checkCurrentOrientation()
        checkPermissions()
        setupUI()
        startLocationUpdates()
        startOverlayUpdates()
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, // 🎤 Para grabación de video con audio
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION // 📍 Agregar ubicación aproximada
        )
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isEmpty()) {
            startCamera()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            
            // 📹 Configurar VideoCapture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch (exc: Exception) {
                Log.e("CameraHC2", "Error al iniciar cámara", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        // Animación GoldenEye flash
        showGoldenEyeAnimation()
        
        // Capturar foto real con CameraX
        val imageCapture = imageCapture ?: return
        
        // Crear directorio
        val picturesDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val ctosDir = java.io.File(picturesDir, "ctOS_Images")
        if (!ctosDir.exists()) {
            ctosDir.mkdirs()
        }
        
        // Crear archivo para la foto
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "CTOS_TRACK_$name.jpg"
        val photoFile = java.io.File(ctosDir, fileName)
        
        // Configurar opciones de salida
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        // Capturar imagen
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraHC2", "Error al capturar imagen: ${exception.message}", exception)
                    Toast.makeText(this@MainActivity, "❌ ERROR AL CAPTURAR IMAGEN", Toast.LENGTH_SHORT).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Ahora superponer el overlay sobre la imagen capturada
                    addOverlayToImage(photoFile)
                }
            }
        )
    }
    
    private fun addOverlayToImage(photoFile: java.io.File) {
        try {
            // Cargar la imagen capturada
            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            
            // Crear bitmap mutable para dibujar el overlay
            val overlayBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)
            
            // Escalar y dibujar elementos del overlay
            addCtOSOverlayToCanvas(canvas, overlayBitmap.width, overlayBitmap.height)
            
            // Guardar imagen con overlay
            FileOutputStream(photoFile).use { outputStream ->
                overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            Toast.makeText(this, "✅ IMAGEN GUARDADA: ${photoFile.name}", Toast.LENGTH_LONG).show()
            Log.d("CameraHC2", "Imagen con overlay guardada: ${photoFile.absolutePath}")
            
            // También intentar guardar en galería
            try {
                MediaStore.Images.Media.insertImage(
                    contentResolver, overlayBitmap, photoFile.name, "Secure Camera - ctOS Capture"
                )
            } catch (e: Exception) {
                Log.w("CameraHC2", "No se pudo agregar a galería: ${e.message}")
            }
            
            // Envío automático si está habilitado
            if (prefs.getBoolean("auto_send_enabled", false)) {
                // Prioridad: Servidor web > Email
                if (prefs.getBoolean("use_server", false)) {
                    uploadToWebServer(photoFile)
                } else {
                    sendImageByEmail(photoFile)
                }
            }
            
        } catch (e: Exception) {
            Log.e("CameraHC2", "Error al agregar overlay: ${e.message}", e)
            Toast.makeText(this, "❌ ERROR AL PROCESAR IMAGEN", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun addCtOSOverlayToCanvas(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            color = Color.parseColor("#00FF00")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        
        // Detectar orientación de la imagen
        val imageIsLandscape = width > height
        val baseSize = if (imageIsLandscape) height else width
        
        val textPaint = Paint().apply {
            color = Color.parseColor("#00FF00")
            textSize = baseSize * 0.025f // Texto más pequeño y adaptativo
            typeface = Typeface.MONOSPACE
        }
        
        val smallTextPaint = Paint().apply {
            color = Color.parseColor("#AAFFAA")
            textSize = baseSize * 0.02f
            typeface = Typeface.MONOSPACE
        }
        
        // Líneas de esquina (adaptativas)
        val cornerSize = baseSize * 0.08f
        
        // Esquinas ctOS
        // Superior izquierda
        canvas.drawLine(0f, 0f, cornerSize, 0f, paint)
        canvas.drawLine(0f, 0f, 0f, cornerSize, paint)
        
        // Superior derecha
        canvas.drawLine(width - cornerSize, 0f, width.toFloat(), 0f, paint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), cornerSize, paint)
        
        // Inferior izquierda
        canvas.drawLine(0f, height.toFloat(), cornerSize, height.toFloat(), paint)
        canvas.drawLine(0f, height - cornerSize, 0f, height.toFloat(), paint)
        
        // Inferior derecha
        canvas.drawLine(width - cornerSize, height.toFloat(), width.toFloat(), height.toFloat(), paint)
        canvas.drawLine(width.toFloat(), height - cornerSize, width.toFloat(), height.toFloat(), paint)
        
        // Líneas de escaneo (adaptadas a orientación)
        val scanLines = if (imageIsLandscape) 6 else 8
        for (i in 1..scanLines) {
            val y = height * i / (scanLines + 1).toFloat()
            canvas.drawLine(width * 0.15f, y, width * 0.85f, y, paint)
        }
        
        // Cruz central de targeting
        val centerX = width / 2f
        val centerY = height / 2f
        val crossSize = baseSize * 0.05f
        
        canvas.drawLine(centerX - crossSize, centerY, centerX + crossSize, centerY, paint)
        canvas.drawLine(centerX, centerY - crossSize, centerX, centerY + crossSize, paint)
        
        // Información de texto adaptada a orientación
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(System.currentTimeMillis())
        
        // 📍 GPS Status con indicador visual
        val locationText = currentLocation?.let { location ->
            val accuracy = location.accuracy
            val ageMs = System.currentTimeMillis() - location.time
            val ageSeconds = ageMs / 1000
            
            when {
                accuracy <= 10 -> "GPS: EXCELENTE (${"%.1f".format(accuracy)}m)"
                accuracy <= 50 -> "GPS: BUENO (${"%.1f".format(accuracy)}m)"
                accuracy <= 100 -> "GPS: REGULAR (${"%.1f".format(accuracy)}m)"
                else -> "GPS: POBRE (${"%.1f".format(accuracy)}m)"
            } + if (ageSeconds > 60) " (${ageSeconds}s)" else ""
        } ?: "GPS: BUSCANDO SEÑAL..."
        
        val coordinatesText = currentLocation?.let { 
            "LAT: ${"%.6f".format(it.latitude)} LON: ${"%.6f".format(it.longitude)}"
        } ?: "COORDENADAS: NO DISPONIBLES"
        
        val orientationText = if (imageIsLandscape) "LANDSCAPE" else "PORTRAIT"
        val deviceRotation = when (currentOrientation) {
            90 -> "90°"
            180 -> "180°"
            270 -> "270°"
            else -> "0°"
        }
        
        // Fondo semi-transparente para texto
        val textBackgroundPaint = Paint().apply {
            color = Color.parseColor("#88000000")
            style = Paint.Style.FILL
        }
        
        if (imageIsLandscape) {
            // Layout landscape: texto en laterales
            val leftMargin = width * 0.02f
            val rightMargin = width * 0.98f
            
            // Texto superior izquierdo
            canvas.drawRect(0f, 0f, width * 0.4f, textPaint.textSize * 3, textBackgroundPaint)
            canvas.drawText("ctOS SURVEILLANCE", leftMargin, textPaint.textSize * 1.2f, textPaint)
            canvas.drawText("REC ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}", leftMargin, textPaint.textSize * 2.4f, smallTextPaint)
            
            // Texto superior derecho
            canvas.drawRect(width * 0.6f, 0f, width.toFloat(), textPaint.textSize * 2.5f, textBackgroundPaint)
            canvas.drawText("$orientationText $deviceRotation", rightMargin - textPaint.measureText("$orientationText $deviceRotation"), textPaint.textSize * 1.2f, smallTextPaint)
            canvas.drawText("CAM: HC-001", rightMargin - textPaint.measureText("CAM: HC-001"), textPaint.textSize * 2.2f, smallTextPaint)
            
            // Texto inferior - GPS en dos líneas
            val bottomY = height * 0.95f
            canvas.drawRect(0f, bottomY - textPaint.textSize * 2, width.toFloat(), height.toFloat(), textBackgroundPaint)
            canvas.drawText(timestamp, leftMargin, bottomY - textPaint.textSize * 0.5f, smallTextPaint)
            canvas.drawText(locationText, leftMargin, bottomY, smallTextPaint)
            canvas.drawText(coordinatesText, rightMargin - textPaint.measureText(coordinatesText), bottomY, smallTextPaint)
            
        } else {
            // Layout portrait: texto arriba y abajo
            val margin = width * 0.02f
            
            // Texto superior
            canvas.drawRect(0f, 0f, width.toFloat(), textPaint.textSize * 3, textBackgroundPaint)
            canvas.drawText("ctOS SURVEILLANCE", margin, textPaint.textSize * 1.2f, textPaint)
            canvas.drawText("REC ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}", margin, textPaint.textSize * 2.4f, smallTextPaint)
            canvas.drawText("$orientationText $deviceRotation", width - margin - textPaint.measureText("$orientationText $deviceRotation"), textPaint.textSize * 2.4f, smallTextPaint)
            
            // Texto inferior - GPS en múltiples líneas
            val bottomY = height * 0.95f
            canvas.drawRect(0f, bottomY - textPaint.textSize * 3, width.toFloat(), height.toFloat(), textBackgroundPaint)
            canvas.drawText(timestamp, margin, bottomY - textPaint.textSize * 1.5f, smallTextPaint)
            canvas.drawText(locationText, margin, bottomY - textPaint.textSize * 0.5f, smallTextPaint)
            canvas.drawText(coordinatesText, margin, bottomY, smallTextPaint)
            canvas.drawText("CAM: HC-001", width - margin - textPaint.measureText("CAM: HC-001"), bottomY, smallTextPaint)
        }
    }
    
    private fun showGoldenEyeAnimation() {
        // Crear overlay blanco para efecto flash
        val flashOverlay = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            alpha = 0f
        }
        
        // Añadir overlay a la vista principal
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )
        binding.root.addView(flashOverlay, params)
        
        // Animación flash estilo GoldenEye
        flashOverlay.animate()
            .alpha(0.8f)
            .setDuration(150)
            .withEndAction {
                flashOverlay.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        binding.root.removeView(flashOverlay)
                    }
            }
    }
    
    private fun sendImageByEmail(imageFile: java.io.File) {
        val emailAddress = prefs.getString("email_address", "") ?: ""
        
        if (emailAddress.isEmpty()) {
            Toast.makeText(this, "📧 CONFIGURA EMAIL EN CONFIGURACIÓN", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Intent más directo que abre Gmail directamente si está disponible
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                setPackage("com.google.android.gm") // Forzar Gmail si está disponible
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, "📸 Secure Camera - ctOS AUTO - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                
                // Texto más corto para envío rápido
                val emailBody = """
📸 SECURE CAMERA - ctOS SURVEILLANCE AUTO-CAPTURE
${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
${currentLocation?.let { "GPS: ${"%.6f".format(it.latitude)}, ${"%.6f".format(it.longitude)}" } ?: "GPS: No disponible"}
                """.trimIndent()
                
                putExtra(Intent.EXTRA_TEXT, emailBody)
                
                // Adjuntar la imagen
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "${applicationContext.packageName}.fileprovider",
                    imageFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Intentar Gmail primero, luego fallback a otras apps
            try {
                startActivity(emailIntent)
                Toast.makeText(this, "📧 GMAIL ABIERTO → PULSA ENVIAR", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Si Gmail no está disponible, usar selector
                emailIntent.setPackage(null) // Remover restricción de Gmail
                if (emailIntent.resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(emailIntent, "📧 Envío rápido ctOS:"))
                    Toast.makeText(this, "📧 EMAIL LISTO → PULSA ENVIAR", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ NO HAY APPS DE EMAIL", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e("CameraHC2", "Error enviando email: ${e.message}", e)
            Toast.makeText(this, "❌ ERROR AL PREPARAR EMAIL", Toast.LENGTH_SHORT).show()
        }
    }
    
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
                        // Fallback a email si el servidor falla
                        sendImageByEmail(imageFile)
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ ERROR DE CONEXIÓN", Toast.LENGTH_SHORT).show()
                    Log.e("CameraHC2", "Upload error: ${e.message}", e)
                    // Fallback a email si el servidor falla
                    sendImageByEmail(imageFile)
                }
            }
        }
    }
    
    private fun setupUI() {
        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnConfig.setOnClickListener { 
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        
        // 📹 Configurar botón de video
        btnVideo = findViewById(R.id.btnVideo)
        btnVideo?.setOnClickListener { startVideoRecording() }
        
        startTerminalAnimation()
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.w("ctOS_GPS", "❌ Permisos de ubicación no concedidos")
            return
        }

        // 🔧 Configurar LocationRequest para mayor precisión
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Cada 5 segundos
            fastestInterval = 2000 // Mínimo cada 2 segundos
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // 📍 Callback para recibir actualizaciones de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    android.util.Log.d("ctOS_GPS", "✅ Ubicación actualizada: ${location.latitude}, ${location.longitude}")
                    android.util.Log.d("ctOS_GPS", "📡 Precisión: ${location.accuracy}m, Proveedor: ${location.provider}")
                } ?: android.util.Log.w("ctOS_GPS", "⚠️ LocationResult sin ubicación")
            }
        }

        // 🚀 Estrategia múltiple para obtener ubicación
        android.util.Log.d("ctOS_GPS", "🔍 Iniciando búsqueda de ubicación...")

        // 1. Intentar obtener última ubicación conocida
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
                android.util.Log.d("ctOS_GPS", "✅ Última ubicación conocida: ${location.latitude}, ${location.longitude}")
            } else {
                android.util.Log.w("ctOS_GPS", "⚠️ No hay última ubicación conocida")
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("ctOS_GPS", "❌ Error obteniendo última ubicación: ${e.message}")
        }

        // 2. Solicitar actualizaciones de ubicación en tiempo real
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                android.util.Log.d("ctOS_GPS", "✅ Actualizaciones de ubicación iniciadas")
            }.addOnFailureListener { e ->
                android.util.Log.e("ctOS_GPS", "❌ Error iniciando actualizaciones: ${e.message}")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("ctOS_GPS", "❌ SecurityException: ${e.message}")
        }

        // 3. Verificar si el GPS está habilitado
        checkGPSSettings()
    }

    // 🛰️ Verificar configuración de GPS
    private fun checkGPSSettings() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        
        android.util.Log.d("ctOS_GPS", "📡 GPS habilitado: $isGPSEnabled")
        android.util.Log.d("ctOS_GPS", "🌐 Red habilitada: $isNetworkEnabled")
        
        if (!isGPSEnabled && !isNetworkEnabled) {
            android.util.Log.w("ctOS_GPS", "⚠️ Ni GPS ni ubicación por red están habilitados")
            // Mostrar mensaje al usuario (opcional)
            android.widget.Toast.makeText(this, "💡 Habilita el GPS para mejor precisión", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // 🛑 Detener actualizaciones de ubicación
    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            android.util.Log.d("ctOS_GPS", "🛑 Actualizaciones de ubicación detenidas")
        }
    }
    
    private fun startOverlayUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateOverlayInfo()
                updateHandler.postDelayed(this, 1000) // Actualizar cada segundo
            }
        }
        updateHandler.post(updateRunnable)
    }
    
    private fun updateOverlayInfo() {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        
        binding.tvTimestamp.text = "TIMESTAMP: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}"
        binding.tvOverlayTimestamp.text = "REC $currentTime"
        
        currentLocation?.let {
            val locationText = "LAT: ${String.format("%.6f", it.latitude)} | LON: ${String.format("%.6f", it.longitude)}"
            binding.tvLocation.text = locationText
            binding.tvOverlayLocation.text = "GPS: ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}"
        } ?: run {
            binding.tvLocation.text = "LAT: -- | LON: --"
            binding.tvOverlayLocation.text = "GPS: ACQUIRING..."
        }
        
        binding.tvCameraId.text = "CAM ID: HC-001"
        binding.tvStatus.text = "STATUS: RECORDING"
    }
    
    private fun setupOrientationListener() {
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                val newOrientation = when {
                    orientation >= 315 || orientation < 45 -> 0      // Portrait
                    orientation >= 45 && orientation < 135 -> 90     // Landscape izquierda
                    orientation >= 135 && orientation < 225 -> 180   // Portrait invertido
                    orientation >= 225 && orientation < 315 -> 270   // Landscape derecha
                    else -> currentOrientation
                }
                
                if (newOrientation != currentOrientation) {
                    currentOrientation = newOrientation
                    updateOrientationUI()
                    updateCameraOrientation()
                }
            }
        }
        
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }
    
    private fun checkCurrentOrientation() {
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateOrientationDisplay()
    }
    
    private fun updateOrientationUI() {
        val wasLandscape = isLandscape
        isLandscape = (currentOrientation == 90 || currentOrientation == 270)
        
        if (wasLandscape != isLandscape) {
            updateOrientationDisplay()
        }
    }
    
    private fun updateOrientationDisplay() {
        // Agregar indicador de orientación al overlay
        val orientationText = if (isLandscape) "LANDSCAPE" else "PORTRAIT"
        val rotationText = when (currentOrientation) {
            0 -> "↑ 0°"
            90 -> "← 90°"
            180 -> "↓ 180°"
            270 -> "→ 270°"
            else -> "- ${currentOrientation}°"
        }
        
        // Actualizar el texto de sistema para incluir orientación
        binding.tvTerminal.text = "${binding.tvTerminal.text}\n>> ORIENTACIÓN: $orientationText $rotationText"
        
        // Toast informativo ocasional
        if (Math.random() < 0.3) { // 30% probabilidad para no ser molesto
            Toast.makeText(this, "📱 $orientationText $rotationText", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateCameraOrientation() {
        // Actualizar la orientación de la captura de imagen
        imageCapture?.targetRotation = when (currentOrientation) {
            90 -> android.view.Surface.ROTATION_90
            180 -> android.view.Surface.ROTATION_180
            270 -> android.view.Surface.ROTATION_270
            else -> android.view.Surface.ROTATION_0
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        checkCurrentOrientation()
        
        // Reiniciar cámara para ajustar preview
        CoroutineScope(Dispatchers.Main).launch {
            delay(100) // Pequeña pausa para que se complete el cambio
            startCamera()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::orientationEventListener.isInitialized) {
            orientationEventListener.disable()
        }
        updateHandler.removeCallbacks(updateRunnable)
        stopLocationUpdates() // 🛑 Detener actualizaciones de GPS
        cameraExecutor.shutdown()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates() // 🛑 Pausar GPS para ahorrar batería
    }

    override fun onResume() {
        super.onResume()
        if (::fusedLocationClient.isInitialized) {
            startLocationUpdates() // 🚀 Reanudar GPS
        }
    }
    
    private fun startTerminalAnimation() {
        val messages = listOf(
            ">> SISTEMA CTOS INICIADO",
            ">> CÁMARA HABILITADA",
            ">> PREVIEW ACTIVO",
            ">> OVERLAY SUPERPUESTO",
            ">> READY FOR SURVEILLANCE"
        )
        
        var messageIndex = 0
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val runnable = object : Runnable {
            override fun run() {
                if (messageIndex < messages.size) {
                    // binding.tvTerminal.text = messages[messageIndex] // Comentado temporalmente
                    messageIndex++
                    handler.postDelayed(this, 1500)
                } else {
                    // binding.tvTerminal.text = ">> RECORDING ACTIVE" // Comentado temporalmente
                }
            }
        }
        
        handler.post(runnable)
    }
    
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Verificación de seguridad antes de capturar
                if (!performRuntimeSecurityCheck()) {
                    handleSecurityThreat("RUNTIME_SECURITY_FAILED")
                    return true
                }
                // Tomar foto con botones de volumen
                takePhoto()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    // ================================================================
    // 🔐 SISTEMA DE PROTECCIÓN ANTI-PIRATERÍA
    // ================================================================
    
    private fun performSecurityChecks() {
        try {
            // Verificación básica de debugging
            if (isBeingDebugged()) {
                handleSecurityThreat("DEBUGGING_DETECTED")
                return
            }
            
            // Verificación básica de integridad
            if (!verifyBasicIntegrity()) {
                handleSecurityThreat("INTEGRITY_COMPROMISED")
                return
            }
            
            // Verificación de instalación
            if (!isInstalledFromTrustedSource()) {
                handleSecurityThreat("UNTRUSTED_SOURCE")
                return
            }
            
        } catch (e: Exception) {
            // Si hay error en verificaciones, asumir compromiso
            handleSecurityThreat("SECURITY_CHECK_FAILED")
        }
    }
    
    private fun isBeingDebugged(): Boolean {
        return android.os.Debug.isDebuggerConnected() ||
               android.os.Debug.waitingForDebugger() ||
               (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    private fun verifyBasicIntegrity(): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            packageInfo.signatures.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isInstalledFromTrustedSource(): Boolean {
        val installer = packageManager.getInstallerPackageName(packageName)
        return installer == "com.android.vending" || installer == "com.google.android.feedback" || installer == null // null para debug
    }
    
    private fun handleSecurityThreat(threat: String) {
        // En versión de producción, cerrar la app o mostrar mensaje
        android.util.Log.w("ctOS_Security", "Security threat detected: $threat")
        
        // Para producción, descomentar estas líneas:
        /*
        Toast.makeText(this, "❌ ACCESO DENEGADO - APLICACIÓN NO AUTORIZADA", Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({
            finishAffinity()
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 3000)
        */
    }
    
    // Verificación adicional durante el uso de la app
    private fun performRuntimeSecurityCheck(): Boolean {
        return try {
            !android.os.Debug.isDebuggerConnected() && 
            verifyBasicIntegrity() &&
            isInstalledFromTrustedSource()
        } catch (e: Exception) {
            false
        }
    }
    
    // 📹 Funciones de grabación de video
    private fun startVideoRecording() {
        if (isVideoRecording) {
            stopVideoRecording()
            return
        }
        
        val videoFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), 
            "ctOS_Video_${System.currentTimeMillis()}.mp4")
        
        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        
        currentRecording = videoCapture?.output?.prepareRecording(this, outputOptions)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isVideoRecording = true
                        updateVideoButtonUI()
                        Toast.makeText(this, "🔴 GRABANDO VIDEO", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isVideoRecording = false
                        updateVideoButtonUI()
                        if (!recordEvent.hasError()) {
                            processVideoWithOverlay(videoFile)
                            Toast.makeText(this, "✅ VIDEO GUARDADO: ${videoFile.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "❌ ERROR AL GRABAR VIDEO", Toast.LENGTH_SHORT).show()
                            Log.e("CameraHC2", "Error en grabación: ${recordEvent.error}")
                        }
                    }
                }
            }
    }
    
    private fun stopVideoRecording() {
        currentRecording?.stop()
        currentRecording = null
    }
    
    private fun updateVideoButtonUI() {
        btnVideo?.apply {
            if (isVideoRecording) {
                text = "⏹ DETENER"
                setBackgroundColor(Color.RED)
            } else {
                text = "📹 VIDEO"
                setBackgroundColor(Color.parseColor("#2196F3"))
            }
        }
    }
    
    private fun processVideoWithOverlay(videoFile: File) {
        try {
            // Para videos, solo guardamos el archivo original
            // El overlay en video requiere procesamiento más complejo
            Log.d("CameraHC2", "Video guardado: ${videoFile.absolutePath}")
            
            // Agregar a galería
            MediaScannerConnection.scanFile(
                this,
                arrayOf(videoFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
            
            // Envío automático si está habilitado
            if (prefs.getBoolean("auto_send_enabled", false)) {
                if (prefs.getBoolean("use_server", false)) {
                    uploadVideoToWebServer(videoFile)
                } else {
                    sendVideoByEmail(videoFile)
                }
            }
            
        } catch (e: Exception) {
            Log.e("CameraHC2", "Error al procesar video: ${e.message}", e)
            Toast.makeText(this, "❌ ERROR AL PROCESAR VIDEO", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadVideoToWebServer(videoFile: File) {
        // Implementación similar a uploadToWebServer pero para videos
        // Por ahora solo logging
        Log.d("CameraHC2", "Video upload no implementado aún: ${videoFile.name}")
    }
    
    private fun sendVideoByEmail(videoFile: File) {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(prefs.getString("email_address", "")))
                putExtra(Intent.EXTRA_SUBJECT, "Secure Camera - ctOS Video - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                
                val gpsText = currentLocation?.let { 
                    "LAT: ${"%.6f".format(it.latitude)} LON: ${"%.6f".format(it.longitude)}"
                } ?: "GPS: NO DISPONIBLE"
                
                putExtra(Intent.EXTRA_TEXT, "Video capturado con Secure Camera - ctOS\n\nGPS: $gpsText\nFecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                
                val videoUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "com.ctos.camerahc2.fileprovider",
                    videoFile
                )
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(emailIntent, "Enviar video por email"))
            
        } catch (e: Exception) {
            Log.e("CameraHC2", "Error al enviar video por email: ${e.message}", e)
            Toast.makeText(this, "❌ ERROR AL ENVIAR VIDEO", Toast.LENGTH_SHORT).show()
        }
    }
}