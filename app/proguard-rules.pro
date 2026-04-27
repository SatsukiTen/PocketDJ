# ===========================================================================
# DJ App — ProGuard / R8 ルール
# ===========================================================================

# ---------------------------------------------------------------------------
# JNI — ネイティブメソッドはリネームするとリンクが切れる
# ---------------------------------------------------------------------------
-keep class com.djapp.data.source.AudioEngineDataSource {
    public *;
    native <methods>;
}

# ---------------------------------------------------------------------------
# TarsosDSP — ローカル AAR のため自前ルール不明。全クラスを保持
# ---------------------------------------------------------------------------
-keep class be.tarsos.** { *; }
-dontwarn be.tarsos.**

# ---------------------------------------------------------------------------
# ドメインモデル Enum — DataStore に名前で保存するためリネーム禁止
# ---------------------------------------------------------------------------
-keepclassmembers enum com.djapp.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public <fields>;
}

# ---------------------------------------------------------------------------
# Hilt / Dagger — 生成コードはリフレクション経由でアクセスされる
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ---------------------------------------------------------------------------
# Room — エンティティ・DAO はリフレクションでアクセスされる
# ---------------------------------------------------------------------------
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep interface * extends androidx.room.RoomDatabase$Callback { *; }

# ---------------------------------------------------------------------------
# DataStore — PreferencesSerializer
# ---------------------------------------------------------------------------
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    *;
}

# ---------------------------------------------------------------------------
# Kotlin — コルーチン・リフレクション
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, SourceFile, LineNumberTable
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Compose — ランタイムで使われるリフレクション対象を保護
# ---------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# デバッグ情報 — スタックトレースをリリースでも読める形で保持
# ---------------------------------------------------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
