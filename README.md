# 别花乐 (BieHuaLe)

> 一款**单用户、本地优先**的安卓记账 App —— 3 秒记一笔，清晰统计，可分享 APK。

## 项目状态

✅ **v0.2.0 — 产品方向审查后修复完成**（个人安装用，非正式商店上架）

- **Phase 1–5 全部完成**（M1–M5 全部交付）
- **v0.2.0 审查修复**（依据 [`docs/AUDIT_REPORT.md`](./docs/AUDIT_REPORT.md)）：
  - **P0**：底部 Tab 永远可见（PRD §4.2 对齐）+ 全部流水页 `AllTransactionsScreen` + 账单 Tab"最近 5 笔 + 查看全部"（PRD §4.1 对齐）
  - **P1**：`CircularProgressIndicator` Modifier bug、PRD 主题色文案对齐、toml 片段对齐
  - **P2**：`TransactionType` / `CategoryType` Kotlin 枚举改造（17 个文件，95 处字符串字面量替换；Converters 自动互转；DB schema 升级 v1→v2）+ Room Migration 测试
- `assembleRelease` 产出可直接安装的 APK（暂用 debug 签名）

详见 [开发计划](./docs/DEV_PLAN.md) / [产品需求文档](./docs/PRD.md) / [更新日志](./CHANGELOG.md) / [v0.2.0 发布说明](./RELEASE_NOTES_v0.2.0.md) / [审查报告](./docs/AUDIT_REPORT.md) / [修复报告](./docs/AUDIT_FIX_REPORT.md)。

## 仓库结构

```
BieHuaLe/
├── android/                       # Android Studio 项目根
│   ├── app/                       # 主 app 模块
│   │   ├── build.gradle.kts       # app 级 Gradle（minSdk 24, R8, ABI splits）
│   │   ├── proguard-rules.pro     # R8 keep 规则（含 enum keep）
│   │   └── src/
│   │       ├── main/              # 业务代码（56 个 .kt 文件）
│   │       │   ├── java/com/biehuale/app/
│   │       │   │   ├── data/          # Room + Repository + Backup + Preferences
│   │       │   │   │   ├── db/        # AppDatabase / Converters / entity / dao
│   │       │   │   │   ├── repository/
│   │       │   │   │   ├── seed/      # DefaultCategories
│   │       │   │   │   ├── preferences/ # ThemePreferences
│   │       │   │   │   └── backup/    # Backup DTO/Exporter/Importer + Cleanup
│   │       │   │   ├── domain/        # enum TransactionType / CategoryType
│   │       │   │   ├── di/            # Hilt modules
│   │       │   │   ├── ui/            # Compose 屏幕 + ViewModel
│   │       │   │   │   ├── theme/     # Color / Theme / Type
│   │       │   │   │   ├── common/    # States（含 EmptyState） / MoneyInput / IconColorPresets
│   │       │   │   │   ├── nav/       # AppNav / Destinations
│   │       │   │   │   ├── bill/      # 账单 Tab + components/
│   │       │   │   │   ├── record/    # 记账 Tab
│   │       │   │   │   ├── settings/  # 设置 Tab + 7 个 ViewModel
│   │       │   │   │   ├── list/      # 全部流水页（v0.2 新增）
│   │       │   │   │   └── detail/    # 流水详情
│   │       │   │   ├── util/          # Money / DateExt
│   │       │   │   ├── BieHuaLeApp.kt # Application（Hilt + WorkManager）
│   │       │   │   └── MainActivity.kt
│   │       │   └── res/           # 资源（图标 / 主题 / 字符串）
│   │       ├── test/              # 单元测试（10 个 .kt 文件）
│   │       └── androidTest/       # 集成测试（2 个 .kt 文件，含 Migration）
│   ├── gradle/                    # Gradle 配置（libs.versions.toml）
│   ├── build.gradle.kts           # 项目级 Gradle
│   └── settings.gradle.kts
├── docs/                          # 项目文档
│   ├── PRD.md                     # 产品需求文档
│   ├── UI_DESIGN.md               # 前端视觉与构图规范
│   ├── DEV_PLAN.md                # 开发计划（5 phase / 36 task）
│   ├── STRUCTURE.md               # 目录结构说明
│   ├── AUDIT_REPORT.md            # v0.1.0 产品方向审查报告
│   └── AUDIT_FIX_REPORT.md        # 审查修复完成报告
├── .gitignore
├── AGENTS.md                      # 协作者指南（给 AI agent 看）
├── CHANGELOG.md                   # 版本更新日志
├── RELEASE_NOTES_v0.1.0.md        # v0.1.0 分享说明
├── RELEASE_NOTES_v0.2.0.md        # v0.2.0 分享说明（含 enum 改造 + Migration）
└── README.md
```

## 技术栈

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.10.00 |
| 架构 | MVVM + Repository + Room | — |
| 构建 | Gradle Wrapper + AGP | 8.10.2 / 8.7.0 |
| DI | Hilt | 2.52 |
| 数据库 | Room | 2.6.1 |
| 序列化 | kotlinx-serialization | 1.7.3 |
| 异步 | kotlinx-coroutines | 1.9.0 |
| 后台 | WorkManager | 2.10.0 |
| 图表 | 纯 Compose Canvas | 自绘（饼图 + 趋势折线） |
| 时间 | kotlinx-datetime | 0.6.1 |
| 测试 | JUnit5 + MockK + Robolectric + Truth + Turbine | — |

> 依赖版本以 [`android/gradle/libs.versions.toml`](./android/gradle/libs.versions.toml) 为准。

## 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更新
- **JDK 17**
- **Android SDK 35**（compileSdk）
- 目标设备：**Android 7.0+**（minSdk 24）

### 第一次构建

```powershell
# 1. 用 Android Studio 打开 android/ 目录
# 2. 复制 local.properties.example 为 local.properties，填入 SDK 路径：
#      sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
# 3. 等待 Gradle 同步完成
# 4. 连接 Android 设备或启动模拟器
# 5. 点击 Run（Shift+F10）

# 或命令行（Windows + PowerShell）：
cd android
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> macOS / Linux 把 `.\gradlew.bat` 换成 `./gradlew` 即可。

### 运行测试

```powershell
cd android

# 所有单元测试（src/test）
.\gradlew.bat test

# 单模块测试
.\gradlew.bat :app:testDebugUnitTest

# 带覆盖率
.\gradlew.bat :app:createDebugUnitTestReport

# 设备/模拟器集成测试（src/androidTest，含 MigrationTest）
.\gradlew.bat connectedAndroidTest
```

测试套件覆盖（约 70+ 测试方法）：

| 套件 | 文件 | 范围 |
|------|------|------|
| 工具类 | `MoneyTest`, `DateExtTest` | 金额解析 / 时间区间 |
| Repository | `AccountRepositoryTest`, `TransactionRepositoryTest`, `AccountBalanceTest` | 余额 / 转账 / 业务校验 |
| 业务逻辑 | `RecordViewModelTest`, `BillAggregatorTest` | VM Intent / 聚合 |
| 备份 | `BackupExporterTest`, `BackupImporterTest` | JSON 导出 / 合并导入 / schemaVersion |
| 集成（JVM） | `AppDatabaseTest` | 完整业务链路 + 外键约束 |
| 集成（设备） | `TransactionRoomSmokeTest` | Room 插入/观察烟雾 |
| Migration | `MigrationTest` | v1→v2 schema 升级 + 数据保留 |

## 发布构建

```powershell
cd android

# 1. 编译 release APK（自动启用 R8 + 资源压缩 + ABI splits）
.\gradlew.bat assembleRelease

# 2. 输出在：
#    app\build\outputs\apk\release\
#      ├── app-arm64-v8a-release.apk     （大部分现代手机）
#      ├── app-armeabi-v7a-release.apk   （老手机）
#      ├── app-x86_64-release.apk        （模拟器 / Chromebook）
#      └── app-universal-release.apk     （兜底，文件大）
```

### 签名

当前 release **使用 debug `signingConfig`**，方便个人分享安装。正式上架前请换成独立 keystore（密钥勿提交仓库）。

### 分发

- **直接分享 APK**：把对应 ABI 的 APK 发给朋友（推荐 `app-arm64-v8a-release.apk`），见 [RELEASE_NOTES_v0.2.0.md](./RELEASE_NOTES_v0.2.0.md)
- **应用商店 / Play**：本版本不做上架

## v0.2.0 升级提示

从 v0.1.0 升级到 v0.2.0：

- **App 升级**时 Room 会自动跑 `MIGRATION_1_2`（no-op，schema 兼容）→ 数据完整保留
- **备份 JSON**：v1 格式继续可用；导入时 `BackupImporter` 用 `fromOrNull` 安全降级
- **图标 / 主题 / 隐私 / SDK 配置**：全部保持不变

## 隐私

- ✅ 完全离线，**不上云**
- ✅ 不申请任何运行时权限（无 INTERNET / STORAGE / NOTIFICATION）
- ✅ 不收集任何数据
- ✅ `allowBackup="false"`：数据不上系统云备份
- ✅ 备份 / 恢复走 **Storage Access Framework (SAF)**，用户自选位置，App 不读用户其他文件

详细：[PRD §9.2](./docs/PRD.md) / [AGENTS.md 关键约束](./AGENTS.md)

## 文档

| 文档 | 说明 |
|------|------|
| [PRD.md](./docs/PRD.md) | 产品需求：做什么、为什么、范围 |
| [UI_DESIGN.md](./docs/UI_DESIGN.md) | 前端设计：调性、Token、分屏构图（UI 改版必读） |
| [DEV_PLAN.md](./docs/DEV_PLAN.md) | 开发计划：5 phase / 36 task / 命名规范 / git 规范 |
| [STRUCTURE.md](./docs/STRUCTURE.md) | 目录结构：每个文件夹是干什么的 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本更新日志 |
| [AUDIT_REPORT.md](./docs/AUDIT_REPORT.md) | v0.1.0 → v0.2.0 产品方向审查报告 |
| [AUDIT_FIX_REPORT.md](./docs/AUDIT_FIX_REPORT.md) | 审查修复完成报告 |
| [AGENTS.md](./AGENTS.md) | 协作者指南（给 AI agent 看） |

## 已知限制

- **release 暂用 debug 签名**：便于个人安装；正式上架前请换成独立 keystore（见上「发布构建 → 签名」）
- **多语言**：当前只支持中文 + 英文（`res/values/` + `res/values-zh/`）
- **图标**：用系统矢量图标 + 自适应启动图，未做完整品牌设计
- **无桌面 Widget / 快捷方式**：记账入口在 App 内
- **无云同步**：纯本地，丢手机 = 丢数据（建议定期用 SAF 备份 JSON）

## License

个人项目，暂不开放源码。
