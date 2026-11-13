# Rules proguard tạm thời (isMinifyEnabled = false nên bộ rules này chưa cần tối ưu)
# Khi bật minify cho bản release, cần giữ các model Retrofit/Gson:
-keep class com.example.smartexpense.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
