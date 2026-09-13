# ProGuard rules for Photo Compressor

# Keep Room entities
-keep class com.scanflow.photocompressor.data.local.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Coil
-dontwarn coil.**

# Keep data classes for domain models
-keep class com.scanflow.photocompressor.domain.model.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
