# ============================================================
# Atveyn ProGuard/R8 Rules
# ============================================================

# --- General ---
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# Keep the app entry points
-keep class com.atveyn.app.AtveynApp { *; }
-keep class com.atveyn.app.MainActivity { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
# Keep Composable functions metadata
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- AndroidX Lifecycle ---
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }

# --- ML Kit ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- Navigation ---
-keep class * extends androidx.navigation.Navigator { *; }

# --- DataStore ---
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# --- Kotlin Serialization (if used) ---
-keepattributes RuntimeVisibleAnnotations

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- JSON parsing (org.json is part of Android SDK) ---
-keep class org.json.** { *; }

# --- BroadcastReceivers ---
-keep class com.atveyn.app.core.update.UpdateInstallReceiver { *; }
-keep class com.atveyn.app.core.notification.AttendanceActionReceiver { *; }
-keep class com.atveyn.app.core.notification.BootReceiver { *; }
-keep class com.atveyn.app.core.scheduler.AlarmReceiver { *; }

# --- FileProvider ---
-keep class androidx.core.content.FileProvider { *; }

# --- Data classes used in state/serialization ---
-keep class com.atveyn.app.core.update.UpdateInfo { *; }
-keep class com.atveyn.app.core.update.UpdateState { *; }
-keep class com.atveyn.app.core.update.UpdateState$* { *; }
-keep class com.atveyn.app.core.model.** { *; }
-keep class com.atveyn.app.core.database.entity.** { *; }

# --- Enums ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- R8 full mode compatibility ---
-dontwarn java.lang.invoke.StringConcatFactory
