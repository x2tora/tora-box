-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all kotlinx.serialization models, NavKey routes, serializers, and companions
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.x2tora.torabox.model.** { *; }
-keep,includedescriptorclasses class com.x2tora.torabox.ui.screens.AppRoute { *; }
-keep,includedescriptorclasses class com.x2tora.torabox.ui.screens.AppRoute$** { *; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep ViewModel constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.x2tora.torabox.viewmodel.** { *; }

-dontwarn top.yukonga.miuix.**
-dontwarn kotlinx.serialization.**

# WorkManager & Room (R8 full mode strips reflective WorkDatabase_Impl / Initializer constructors)
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.startup.Initializer { <init>(); }

# ML Kit ComponentDiscovery & Barcode Scanning
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX DefaultProvider
-keep class androidx.camera.camera2.Camera2Config$DefaultProvider { <init>(); }
-keep class * implements androidx.camera.core.CameraXConfig$Provider { <init>(); }

# Glance AppWidget & ActionCallback
-keep class com.x2tora.torabox.widget.** { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { <init>(); }

# NotificationListenerService & Security
-keep class com.x2tora.torabox.notification.ToraBoxNotificationListenerService { *; }
-keep class com.x2tora.torabox.notification.LiveUpdateActionReceiver { *; }
-keep class com.x2tora.torabox.security.** { *; }
-dontwarn androidx.biometric.**
