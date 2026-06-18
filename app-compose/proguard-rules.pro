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

# SQLCipher
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Google Tink (transitive dep of security-crypto) references javax.annotation which is not bundled
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# PWS — keep domain/features/portable but NOT database.BuildConfig (let R8 inline DB_DECRYPT_KEY)
-keep class io.github.alelk.pws.domain.** { *; }
-keep class io.github.alelk.pws.features.** { *; }
-keep class io.github.alelk.pws.portable.** { *; }

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
