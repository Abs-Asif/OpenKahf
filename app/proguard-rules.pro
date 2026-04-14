# ProGuard rules for OpenKahf

# OkHttp3 rules
-keepattributes Signature, InnerClasses, AnnotationDefault
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$HandlerPost {
    <init>(...);
}

# DataStore rules
-keep class androidx.datastore.** { *; }

# Maintain integrity of the ViewModel and other lifecycle components
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Jetpack Compose rules (mostly handled by the compiler but some safety)
-keep class androidx.compose.runtime.Recomposer { *; }
-keep class androidx.compose.ui.platform.** { *; }

# App specific classes that might be accessed via reflection or need to be kept
-keep class com.open.kahf.MainViewModel { *; }
-keep class com.open.kahf.DnsStatusRepository { *; }
-keep class com.open.kahf.SettingsRepository { *; }
-keep class com.open.kahf.NotificationWorker { *; }
-keep class com.open.kahf.OpenKahfAccessibilityService { *; }
