# Add project specific ProGuard rules here.

# Keep line numbers for better crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Koin
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keepnames class * { @org.koin.core.annotation.* *; }

# Voyager
-keep class cafe.adriel.voyager.** { *; }
-dontwarn cafe.adriel.voyager.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# PWS domain models (keep data classes for serialization)
-keep class io.github.alelk.pws.** { *; }

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

