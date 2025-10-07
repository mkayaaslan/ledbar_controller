
# JNI köprüsü Sınıfı ve native metotları KORU
-keep class com.example.elcapi.jnielc { *; }
-keepclasseswithmembernames class * { native <methods>; }

# (opsiyonel) plugin kanal sınıflarını da koru
-keep class nl.procc.plugin.ledbar_controller.** { *; }