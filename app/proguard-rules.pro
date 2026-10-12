# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.duifenyi.app.data.model.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jspecify.annotations.NullMarked
