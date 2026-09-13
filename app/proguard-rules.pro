# Hermes Android Client - ProGuard Rules
# This file is part of the Hermes Android project

# AndroidX and Compose keep rules
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Compose annotations
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }

# Keep Retrofit and OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface retrofit2.** { *; }
-keep interface okhttp3.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.internal.** { *; }

# Keep AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep all classes in the hermes package
-keep class com.hermes.android.** { *; }

# Keep all data classes (for JSON serialization)
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep R classes
-keep class **.R$* { *; }

# Remove logging from release builds
-assumenosideeffects class android.util.Log {
    * d(...);
    * e(...);
    * w(...);
    * i(...);
    * v(...);
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
