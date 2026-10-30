# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# OkHttp
-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Coil
-keep class coil.** { *; }

# Kotlin Coroutines
-keepclassmembers class * {
    @kotlinx.coroutines.InternalCoroutinesApi *;
}
