# ================================================================
# ctOS CAMERA - REGLAS DE PROTECCIÓN ANTI-PIRATERÍA
# ================================================================

# OFUSCACIÓN AGRESIVA
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively

# OPTIMIZACIONES DE CÓDIGO
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# OFUSCAR NOMBRES DE CLASES Y MÉTODOS
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# ================================================================
# PROTECCIÓN ESPECÍFICA DE ctOS
# ================================================================

# Proteger clases de seguridad
-keep class com.ctos.camerahc2.security.** {
    public <methods>;
}

# Ofuscar completamente lógica de negocio pero mantener funcionalidad
-keep class com.ctos.camerahc2.MainActivity {
    public void onCreate(android.os.Bundle);
    public void onDestroy();
}

-keep class com.ctos.camerahc2.ConfigActivity {
    public void onCreate(android.os.Bundle);
}

# ================================================================
# ANTI-DEBUGGING Y ANTI-TAMPERING
# ================================================================

# Eliminar logs en release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Proteger strings sensibles
-adaptclassstrings
-adaptresourcefilenames **.xml
-adaptresourcefilecontents **.xml

# ================================================================
# MANTENER CLASES NECESARIAS PARA FUNCIONAMIENTO
# ================================================================

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Play Licensing
-keep class com.google.android.vending.licensing.** { *; }
-dontwarn com.google.android.vending.licensing.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Data Binding y View Binding
-keep class androidx.databinding.** { *; }
-keep class com.ctos.camerahc2.databinding.** { *; }

# ================================================================
# CONFIGURACIONES ESTÁNDAR DE ANDROID
# ================================================================

# Preserve some attributes that may be required for reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters in Views so that animations can still work.
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ================================================================
# CONFIGURACIÓN ESPECÍFICA PARA RELEASE
# ================================================================

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove debug info
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}