# Room
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.app.mitvplayer.data.models.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
