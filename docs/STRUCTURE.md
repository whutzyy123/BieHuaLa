# 别花乐 (BieHuaLe) - 目录结构说明

> **配套文档**：[PRD.md](./PRD.md) · [DEV_PLAN.md](./DEV_PLAN.md) · [AGENTS.md](../AGENTS.md)

## 顶层结构

```
BieHuaLe/
├── android/                    # Android Studio 项目根
├── docs/                       # 项目文档
│   ├── PRD.md
│   ├── DEV_PLAN.md
│   ├── STRUCTURE.md            # 本文件
│   ├── AUDIT_REPORT.md         # v0.1.0 产品方向审查报告
│   └── AUDIT_FIX_REPORT.md     # 审查修复完成报告（→ v0.2.0）
├── .gitignore
├── AGENTS.md                   # 协作者指南（AI agent 必读）
├── CHANGELOG.md                # 版本更新日志
├── RELEASE_NOTES_v0.1.0.md     # v0.1.0 分享说明
├── RELEASE_NOTES_v0.2.0.md     # v0.2.0 分享说明
└── README.md                   # 项目说明
```

## android/ - Android Studio 项目

```
android/
├── build.gradle.kts            # 项目级 Gradle（插件集中声明）
├── settings.gradle.kts         # 仓库配置 + 模块清单
├── gradle.properties           # JVM / AndroidX / Kotlin 配置
├── local.properties.example    # SDK 路径示例（实际 local.properties 不提交）
├── .gitignore
│
├── gradle/
│   ├── libs.versions.toml      # ★ 版本目录（所有依赖版本集中管理）
│   └── wrapper/
│       └── gradle-wrapper.properties
│
└── app/                        # 主 app 模块（单 module 起步）
    ├── build.gradle.kts        # ★ app 模块构建（SDK、依赖、构建类型）
    ├── proguard-rules.pro
    ├── .gitignore
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/biehuale/app/      # ★ Kotlin 主代码
        │   └── res/                        # ★ 资源文件
        │
        ├── test/                           # JVM 单元测试
        │   └── java/com/biehuale/app/
        │
        └── androidTest/                    # 设备集成测试
            └── java/com/biehuale/app/
```

## java/com/biehuale/app/ - Kotlin 包结构

```
com.biehuale.app/
├── BieHuaLeApp.kt              # @HiltAndroidApp Application（含 Hilt + WorkManager）
├── MainActivity.kt             # 单 Activity 容器（installSplashScreen + edge-to-edge）
│
├── data/                       # 数据层
│   ├── db/
│   │   ├── AppDatabase.kt      # Room 数据库（version=2，@TypeConverters 注册）
│   │   ├── Converters.kt       # TypeConverter（TransactionType / CategoryType ↔ String）
│   │   ├── entity/             # ★ 3 个表
│   │   │   ├── AccountEntity.kt
│   │   │   ├── CategoryEntity.kt     # type 字段为 CategoryType 枚举
│   │   │   └── TransactionEntity.kt # type 字段为 TransactionType 枚举
│   │   └── dao/                # ★ 3 个 DAO
│   │       ├── AccountDao.kt
│   │       ├── CategoryDao.kt
│   │       └── TransactionDao.kt
│   ├── repository/             # 业务封装（3 个 Repository）
│   │   ├── AccountRepository.kt
│   │   ├── CategoryRepository.kt
│   │   └── TransactionRepository.kt
│   ├── seed/
│   │   └── DefaultCategories.kt    # 首次启动 seed（15 个内置类别）
│   ├── preferences/
│   │   └── ThemePreferences.kt     # DataStore 持久化 ThemeMode
│   └── backup/
│       ├── BackupDto.kt        # JSON 备份 DTO
│       ├── BackupExporter.kt
│       ├── BackupImporter.kt   # 合并模式 + 整笔事务
│       ├── CleanupScheduler.kt # WorkManager 调度
│       └── CleanupWorker.kt    # 30 天回收站清理
│
├── domain/                     # 领域层（v0.2 启用）
│   └── model/
│       ├── TransactionType.kt  # enum: INCOME / EXPENSE / TRANSFER
│       └── CategoryType.kt     # enum: INCOME / EXPENSE
│
├── ui/                         # UI 层
│   ├── theme/
│   │   ├── Color.kt            # 品牌色（松石绿 #006C5C）+ 收入/支出/类别色
│   │   ├── Theme.kt            # Material 3 + Dynamic Color + 三种 ThemeMode
│   │   └── Type.kt             # Typography（金额用 Monospace 等宽字体）
│   ├── common/                 # 跨 Tab 复用组件
│   │   ├── States.kt           # 通用状态（EmptyState 等 Composable）
│   │   ├── MoneyInput.kt       # 金额输入框组件
│   │   └── IconColorPresets.kt # 账户/类别图标 + 颜色预设（8 色）
│   ├── nav/
│   │   ├── AppNav.kt           # 3 Tab 导航（底部 Tab 永远可见）
│   │   └── Destinations.kt     # 路由常量（11 个 destination）
│   ├── bill/                   # 账单 Tab
│   │   ├── BillScreen.kt
│   │   ├── BillViewModel.kt
│   │   └── components/         # 账单 Tab 子组件
│   │       ├── SummaryCard.kt          # 月度汇总 + 比例条
│   │       ├── CategoryPieChart.kt     # 类别饼图（纯 Compose Canvas）
│   │       ├── DailyLineChart.kt       # 趋势折线图（纯 Compose Canvas）
│   │       ├── SearchBar.kt
│   │       └── FilterBottomSheet.kt
│   ├── record/                 # 记账 Tab
│   │   ├── RecordScreen.kt
│   │   └── RecordViewModel.kt
│   ├── settings/               # 设置 Tab
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── AccountManageScreen.kt
│   │   ├── AccountManageViewModel.kt
│   │   ├── CategoryManageScreen.kt
│   │   ├── CategoryManageViewModel.kt
│   │   ├── RecycleBinScreen.kt
│   │   ├── RecycleBinViewModel.kt
│   │   ├── AppearanceViewModel.kt      # 主题切换
│   │   └── BackupViewModel.kt          # 备份导入导出
│   ├── list/                   # 全部流水页（PRD §4.1）
│   │   ├── AllTransactionsScreen.kt
│   │   └── AllTransactionsViewModel.kt
│   └── detail/                 # 流水详情
│       ├── TransactionDetailScreen.kt
│       └── TransactionDetailViewModel.kt
│
├── di/                         # Hilt 模块
│   └── DatabaseModule.kt       # Room / DAO + 注册 MIGRATION_1_2（Repository 走 @Inject constructor 隐式绑定，无需显式 Module）
│
└── util/                       # 工具类
    ├── Money.kt                # 金额格式化（分 ↔ "1.00"）+ parseToCents
    └── DateExt.kt              # 时间格式化 + monthRange
```

## 测试文件

```
src/test/                       # JVM 单元测试（JUnit5 + Robolectric + MockK）
└── java/com/biehuale/app/
    ├── util/
    │   ├── MoneyTest.kt                  # parseToCents / toMoneyString 边界
    │   └── DateExtTest.kt                # monthRange 闰年 / 跨年
    ├── data/
    │   ├── AccountBalanceTest.kt         # 转账余额守恒
    │   ├── backup/
    │   │   ├── BackupExporterTest.kt
    │   │   └── BackupImporterTest.kt     # schemaVersion 校验 + 合并
    │   ├── db/
    │   │   └── AppDatabaseTest.kt         # 完整业务链路 + 外键
    │   └── repository/
    │       ├── AccountRepositoryTest.kt
    │       └── TransactionRepositoryTest.kt # 业务校验 100% 覆盖
    └── ui/
        ├── bill/
        │   └── BillAggregatorTest.kt      # 聚合 + 趋势 + 筛选
        └── record/
            └── RecordViewModelTest.kt

src/androidTest/                # 设备集成测试
└── java/com/biehuale/app/
    └── data/db/
        ├── MigrationTest.kt              # v1→v2 数据保留
        └── TransactionRoomSmokeTest.kt   # Room 插入/观察 烟雾
```

## res/ - 资源文件

```
res/
├── values/
│   ├── strings.xml             # 字符串资源
│   ├── colors.xml              # 仅原生资源（splash/icon bg），Compose ColorScheme 运行时取
│   └── themes.xml              # Activity 启动瞬间的 native 主题
├── values-night/
│   └── themes.xml              # 深色模式 native 主题
├── values-zh/
│   └── strings.xml             # 中文 locale
├── drawable/
│   ├── ic_launcher_foreground.xml  # vector 前景
│   └── ic_launcher_background.xml  # 颜色背景
├── mipmap-anydpi-v26/          # Android 8+ adaptive icon
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
├── mipmap-{m,h,xh,xxh,xxxh}dpi/  # API 24–25 回退图标（layer-list）
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
└── xml/
    ├── backup_rules.xml        # Android 6-11 备份规则（禁用云备份）
    └── data_extraction_rules.xml  # Android 12+ 备份规则
```

## 文件命名约定

| 类别 | 规则 | 示例 |
|------|------|------|
| Composable Screen | `<Feature>Screen.kt` | `BillScreen.kt` |
| ViewModel | `<Feature>ViewModel.kt` | `BillViewModel.kt` |
| Entity | `<Table>Entity.kt` | `AccountEntity.kt` |
| DAO | `<Table>Dao.kt` | `AccountDao.kt` |
| Repository | `<Table>Repository.kt` | `AccountRepository.kt` |
| 资源 drawable | `ic_<purpose>_<variant>.xml` | `ic_launcher_foreground.xml` |

## 包依赖方向（必须遵守）

现阶段（单 module、domain 几乎为空）：

```
ui ──▶ data (Repository / DAO)
 │         │
 └──▶ util ◀┘
```

- **ui** 可以依赖 data、util（经 ViewModel；Composable 不直接调 DAO）
- **data** 不可以依赖 ui
- **util** 不可以依赖 ui、data
- **domain**（可选）：若引入 usecase，接口放 domain、实现放 data；在引入前不要虚构依赖边

未来若启用完整 domain 层，再改为：`ui → domain → data`。

## 隐私 / 备份

- Manifest：`android:allowBackup="false"`（不上云）
- `backup_rules.xml` / `data_extraction_rules.xml` 仍保留作防御性配置

## 扩展方向

未来如果项目变大（>30 个 .kt 文件），可以拆 multi-module：

```
android/
├── app/                  # 主入口 + 3 Tab 容器
├── core/
│   ├── data/             # db + repository + backup
│   ├── domain/           # model + usecase
│   ├── ui/               # theme + common components
│   └── util/
└── feature/
    ├── bill/
    ├── record/
    ├── settings/
    └── detail/
```

当前**单 module 起步**，等真正变重再拆。

> 依赖版本以 `android/gradle/libs.versions.toml` 为准。
