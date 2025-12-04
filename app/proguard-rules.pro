# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
########################################
# General Android & Kotlin
########################################
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

########################################
# Jetpack Compose
########################################
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

########################################
# Room Database
########################################
-keep class androidx.room.** { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.**

########################################
# Jetpack Navigation
########################################
-keep class androidx.navigation.** { *; }
-keepclassmembers class * {
    @androidx.navigation.* <fields>;
}
-dontwarn androidx.navigation.**

########################################
# Kotlin Coroutines
########################################
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

########################################
# Retrofit
########################################
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**

########################################
# OkHttp
########################################
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

########################################
# Gson
########################################
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

########################################
# Coil (Image Loading)
########################################
-keep class coil.** { *; }
-dontwarn coil.**

########################################
# Moshi (if used instead of Gson)
########################################
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

########################################
# Prevent stripping of enums used in reflection
########################################
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

########################################
# Keep Application class
########################################
-keep class ** extends android.app.Application { *; }

########################################
# Keep all Activities, Fragments, Services, BroadcastReceivers
########################################
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
