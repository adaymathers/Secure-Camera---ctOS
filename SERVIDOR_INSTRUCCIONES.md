# 🌐 ctOS - Configuración Servidor Web Propio

## **OPCIÓN 3: MÁXIMA AUTONOMÍA**

### **📋 INSTRUCCIONES DE INSTALACIÓN**

#### **1. PREPARAR TU SERVIDOR WEB**

Puedes usar cualquier servidor web con soporte PHP:
- **Hosting compartido** (más fácil)
- **VPS propio** (más control)
- **Servidor local** (para desarrollo/testing)

#### **2. SUBIR EL ARCHIVO PHP**

1. Sube el archivo `servidor_recibir_imagen.php` a tu servidor web
2. Créalo en la raíz o en una carpeta específica
3. Asegúrate de que tenga permisos de escritura

#### **3. CONFIGURAR LA URL EN LA APP**

En la configuración de ctOS, activa "SERVIDOR WEB" y configura:

```
https://tudominio.com/servidor_recibir_imagen.php
```

O si está en una subcarpeta:

```
https://tudominio.com/ctos/servidor_recibir_imagen.php
```

#### **4. PERSONALIZAR EL SCRIPT PHP**

Edita el archivo `servidor_recibir_imagen.php` y ajusta:

```php
// Cambiar configuración de email
$headers = "From: noreply@TUDOMINIO.com\r\n";

// Personalizar mensaje
$subject = "🔍 ctOS - Nueva captura desde tu móvil";
```

#### **5. ESTRUCTURA DE ARCHIVOS EN EL SERVIDOR**

```
tu-servidor.com/
├── servidor_recibir_imagen.php    ← El script principal
├── ctos_images/                   ← Se crea automáticamente
│   ├── ctOS_20231215_143052_abc123.jpg
│   ├── ctOS_20231215_143103_def456.jpg
│   └── ...
└── ctos_log.txt                   ← Log de todas las capturas
```

### **📊 VENTAJAS DEL SERVIDOR PROPIO**

✅ **Completamente automático** - Cero interacción del usuario
✅ **Sin límites de tamaño** - Configurables por ti
✅ **Historial completo** - Log de todas las capturas
✅ **Notificación por email** - Opcional pero recomendado
✅ **URLs directas** - Acceso inmediato a las imágenes
✅ **Control total** - Tu infraestructura, tus reglas

### **🔧 CONFIGURACIONES AVANZADAS**

#### **Para servidor con autenticación:**

```php
// Agregar validación básica
if ($_POST['api_key'] !== 'tu_clave_secreta') {
    http_response_code(401);
    exit('Unauthorized');
}
```

#### **Para redimensionar imágenes automáticamente:**

```php
// Reducir tamaño de imagen si es muy grande
if (filesize($filepath) > 2 * 1024 * 1024) { // > 2MB
    $image = imagecreatefromjpeg($filepath);
    $resized = imagescale($image, 1920, 1080);
    imagejpeg($resized, $filepath, 85);
}
```

### **🌍 EJEMPLOS DE USO**

#### **Hosting compartido (ej: Hostinger, SiteGround):**
```
https://tucuenta.hostinger.com/ctos/upload.php
```

#### **VPS propio (ej: DigitalOcean, Vultr):**
```
https://tu-ip-o-dominio.com/ctos/upload.php
```

#### **Servidor local (para testing):**
```
http://192.168.1.100/ctos/upload.php
```

### **📱 TESTING**

1. Configura la URL en ctOS
2. Captura una foto de prueba
3. Verifica que aparezca en `ctos_images/`
4. Revisa `ctos_log.txt` para confirmar el registro
5. Si configuraste email, verifica que llegue la notificación

### **🔐 CONSIDERACIONES DE SEGURIDAD**

- Usa HTTPS siempre que sea posible
- Considera agregar autenticación API
- Limita el tamaño máximo de archivos
- Revisa regularmente el espacio en disco
- Configura backups periódicos

---

**¡Con esta configuración tendrás un sistema completamente autónomo y profesional para recibir las capturas de ctOS!**