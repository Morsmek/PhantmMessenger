# Phantm ProGuard rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep libsodium / JNA
-keep class com.goterl.lazysodium.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# Keep Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep SQLDelight generated code
-keep class com.stagic.phantm.db.** { *; }
