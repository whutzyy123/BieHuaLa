// =====================================================
//  别花乐 (BieHuaLe) - 项目级 build.gradle.kts
// =====================================================
//  - 集中声明所有插件，子模块 apply false
//  - 实际插件在 app/build.gradle.kts 里 apply
// =====================================================

plugins {
    // Android
    alias(libs.plugins.android.application) apply false

    // Kotlin
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // Annotation processing
    alias(libs.plugins.ksp) apply false

    // DI
    alias(libs.plugins.hilt) apply false
}
