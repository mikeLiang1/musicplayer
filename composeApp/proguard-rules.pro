# ---------------------------------------------------------------------------
# MusicPlayer release ProGuard / R8 rules
# ---------------------------------------------------------------------------

# Keep line numbers for readable crash stack traces, hide original file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations / generics / signatures needed by reflection-based libs.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisible*Annotations

# ---------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable classes and their synthesized companions/serializers.
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
}
-dontwarn kotlinx.serialization.**

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Ktor + OkHttp + Okio
# ---------------------------------------------------------------------------
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**
-keepclassmembers class io.ktor.** { volatile <fields>; }
# Conscrypt / BouncyCastle optional providers referenced by OkHttp.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# Koin (no special config needed, but silence reflection warnings)
# ---------------------------------------------------------------------------
-dontwarn org.koin.**

# ---------------------------------------------------------------------------
# NewPipe Extractor (uses Rhino JS engine, jsoup, nanojson via reflection)
# ---------------------------------------------------------------------------
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
# Mozilla Rhino — used to evaluate YouTube's player signature JS.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.classfile.**
# jsoup HTML parser.
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
# nanojson
-dontwarn com.grack.nanojson.**

# ---------------------------------------------------------------------------
# AndroidX Media3
# ---------------------------------------------------------------------------
-dontwarn androidx.media3.**

# ---------------------------------------------------------------------------
# Coil
# ---------------------------------------------------------------------------
-dontwarn coil3.**

# ---------------------------------------------------------------------------
# Kotlin coroutines internals
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
