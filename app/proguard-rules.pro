# ProGuard / R8 rules for ScanFlow Photo
# Optimized for release stability, ML Kit on-device inference, Hilt, Room, and WorkManager

# Application & Core Activities
-keep class com.scanflow.photocompressor.PhotoCompressorApplication { *; }
-keep class com.scanflow.photocompressor.MainActivity { *; }
-keep class com.scanflow.photocompressor.Hilt_* { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class * extends android.app.Application { *; }

# WorkManager Workers (Instantiated via reflection by Android OS)
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.scanflow.photocompressor.work.** { *; }

# Hilt Dependency Injection
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep interface * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.ComponentManager { *; }
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep class com.scanflow.photocompressor.di.** { *; }
-keep class **.*_Factory { *; }
-keep class **.*_MembersInjector { *; }
-keep class **.*_HiltModules* { *; }

# Data, Domain, and Engine Repositories (Prevent R8 stripping implementations)
-keep class com.scanflow.photocompressor.data.** { *; }
-keep class com.scanflow.photocompressor.domain.** { *; }
-keep class com.scanflow.photocompressor.engine.** { *; }

# Room Database & DAO
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter { *; }
-keep class * extends androidx.room.EntityInsertionAdapter { *; }
-keep class * extends androidx.room.SharedSQLiteStatement { *; }
-keep class com.scanflow.photocompressor.data.local.** { *; }
-keep class **_Impl { *; }

# Google ML Kit Selfie Segmentation (On-device offline background removal)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-keepclassmembers class * {
    native <methods>;
}

# Coil Image Loader
-dontwarn coil.**
-keep class coil.** { *; }

# DataStore Preferences
-keep class androidx.datastore.** { *; }

# Domain Models & Data Classes
-keep class com.scanflow.photocompressor.domain.model.** { *; }

# ViewModel Constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# General Android & Reflection Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin Metadata
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
