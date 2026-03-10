# ProGuard rules for Clash Widget - Production Ready

# Keep all public classes in the package
-keep public class com.clashwidget.** { *; }

# Keep widget components
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application

# Keep sealed classes and their subclasses for proper serialization
-keep class com.clashwidget.ShellResult { *; }
-keep class com.clashwidget.ShellResult$* { *; }
-keep class com.clashwidget.ToggleResult { *; }
-keep class com.clashwidget.ToggleResult$* { *; }
-keep class com.clashwidget.ClashState { *; }
-keep class com.clashwidget.ClashState$* { *; }

# Keep interfaces for dependency injection
-keep interface com.clashwidget.ShellExecutor { *; }

# Keep data classes
-keep class com.clashwidget.ShellOutput { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }

# Keep coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep BuildConfig for debug flag
-keep class com.clashwidget.BuildConfig { *; }

# Optimize aggressively
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimizations
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep all constructors for sealed classes
-keepclassmembers class * extends com.clashwidget.ShellResult {
    <init>(...);
}
-keepclassmembers class * extends com.clashwidget.ToggleResult {
    <init>(...);
}
-keepclassmembers class * extends com.clashwidget.ClashState {
    <init>(...);
}
