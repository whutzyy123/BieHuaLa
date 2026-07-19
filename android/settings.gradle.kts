// =====================================================
//  别花乐 (BieHuaLe) - settings.gradle.kts
// =====================================================
//  - 启用版本目录 (Version Catalog)
//  - 单模块起步，预留 multi-module 扩展位
//  - dependencyResolutionManagement 用 FAIL_ON_PROJECT_REPOS 强制集中
// =====================================================

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 如需 jitpack 等三方仓库，加在这里
    }
}

rootProject.name = "BieHuaLe"

// ----- 模块清单 -----
// 起步只有 :app。后续如需拆 :core / :feature-* / :data 等，在这里 include。
include(":app")
