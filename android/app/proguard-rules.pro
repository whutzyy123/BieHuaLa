# =====================================================
#  别花乐 (BieHuaLe) - ProGuard / R8 规则
#  Phase 5 开启 release minify=true + shrinkResources=true
# =====================================================

# ---------- 通用 ----------
# 保留行号（崩溃日志有用）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留异常信息
-keepattributes Signature, InnerClasses, EnclosingMethod

# 保留注解（kotlinx-serialization / Room / Hilt 需要）
-keepattributes *Annotation*

# ---------- Kotlin / Coroutines ----------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# ---------- kotlinx-serialization ----------
# 序列化类的 Companion / Serializer 保留
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.biehuale.app.data.backup.**$$serializer { *; }
-keepclassmembers class com.biehuale.app.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.biehuale.app.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# 所有 @Serializable 类的 Companion
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class **
-keep,allowobfuscation,allowshrinking class **$$serializer

# ---------- Room ----------
# 保留 Room 实体
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ---------- Hilt ----------
# Hilt + Dagger 生成代码通常已自动 keep，但保守起见加规则
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Hilt Worker（HiltWorker + WorkManager 集成）
-keep class androidx.hilt.work.** { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }
-keepclassmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}

# ---------- Compose ----------
# 不必全量 keep runtime；保留 tooling 与常见反射入口即可
-keep class androidx.compose.ui.tooling.** { *; }
-dontwarn androidx.compose.**

# ---------- WorkManager ----------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# ---------- DataStore ----------
-dontwarn androidx.datastore.**

# ---------- kotlinx-datetime ----------
-dontwarn kotlinx.datetime.**

# ---------- 我们的 enum 类 ----------
# enum.name / valueOf 会被备份导入/导出时反射调用（R8 不能优化掉）
-keepclassmembers enum com.biehuale.app.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    *;
}
-keep enum com.biehuale.app.domain.model.** { *; }

# ---------- ViewModel ----------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---------- 我们的应用代码（关键业务类） ----------
# 防止 R8 误删 — 这些类通过反射或 Hilt 注入
-keep class com.biehuale.app.BieHuaLeApp { *; }
-keep class com.biehuale.app.MainActivity { *; }

# Worker 不能混淆（WorkManager 通过 class name 反射）
-keep class com.biehuale.app.data.backup.CleanupWorker { *; }

# 备份 DTO（kotlinx-serialization 已用 keep 规则，这里冗余保证）
-keep class com.biehuale.app.data.backup.BackupDto { *; }
-keep class com.biehuale.app.data.backup.AccountDto { *; }
-keep class com.biehuale.app.data.backup.CategoryDto { *; }
-keep class com.biehuale.app.data.backup.TransactionDto { *; }
-keep class com.biehuale.app.data.backup.ImportResult { *; }

# Entity（Room 用反射读字段名 — 但 @ColumnInfo 实际不反射，所以这条更保险）
-keep class com.biehuale.app.data.db.entity.** { *; }
