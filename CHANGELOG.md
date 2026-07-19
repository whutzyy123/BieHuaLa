# Changelog

别花乐 (BieHuaLe) 项目的所有重要变更都会记录在此。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [0.3.1] - 2026-07-19

**残余缺陷修复**（全量复盘后）。详见 [RELEASE_NOTES_v0.3.1.md](./RELEASE_NOTES_v0.3.1.md)。

### Fixed

- **备份 merge deletedAt 矩阵**：本地软删+备份软删保持回收站；本地活跃+备份软删 → 同步 softDelete；本地软删+备份 active → restore
- **同名账户期初**：仅本地 `initialBalance==0` 时采用备份期初，避免同机重导盖掉已改期初
- **记账连点**：`onSave` 在 `launch` 前同步 `isSaving`，防止双 insert
- **softDelete**：仅更新尚未软删的行；**hardDelete** 校验行数
- **Room v3**：`to_account_id` 索引 + `MIGRATION_2_3`

### Changed

- 备份预览与导入状态分离（读取中 / 导入中）
- `ImportResult` 拆分 `categoriesSkipped` / `transactionsSoftDeleted`
- `versionName` → `0.3.1`（versionCode 3）

---

## [0.3.0] - 2026-07-19

**第一性原理审查修复**（[`docs/REVIEW_REPORT.md`](./docs/REVIEW_REPORT.md) / [RELEASE_NOTES_v0.3.0.md](./RELEASE_NOTES_v0.3.0.md)）。

### Fixed

- **备份 merge**：fingerprint 不含 `deletedAt`；软删后重导同内容交易会 **restore**，不再插入重复活跃行导致余额虚高
- **回收站清空 / 过期清理**：改为单条 SQL 批量硬删（原子）
- **restore**：DAO 返回行数；无效 id 不再提示「已恢复」
- **列表软删**：Bill / AllTransactions 失败时 Snackbar 反馈
- **CleanupScheduler.runOnce**：`enqueueUniqueWork` + KEEP，避免冷启动堆任务

### Changed

- Settings 去掉「快速新建账户」，统一走账户管理页
- `RecordViewModel.onSave` 抽 `validate()`；Record 表单组件拆到 `ui/record/components`
- 账户/类别编辑复用 `IconColorPickerSection`
- 底部 Tab 在子路由（详情 / 全部流水 / 设置子页）仍高亮所属主 Tab
- 删除未使用的 `searchByDescription` DAO 路径；导出读事务 KDoc 标明快照意图
- `versionName` → `0.3.0`（versionCode 2）

### Tests

- 软删后重导备份用例；跨年周分桶用例

---

## [0.2.0] - 2026-07-19

**v0.1.0 产品方向审查修复**。详见 [`docs/AUDIT_REPORT.md`](./docs/AUDIT_REPORT.md) / [`docs/AUDIT_FIX_REPORT.md`](./docs/AUDIT_FIX_REPORT.md) / [RELEASE_NOTES_v0.2.0.md](./RELEASE_NOTES_v0.2.0.md)。

### Changed（修复严重出入）

#### P0 — 必须修

- **底部 Tab 永远可见**（`AppNav.kt`）：删除 `showBottomBar` 条件判断，子页 / 详情页 / 编辑页都保留 Tab。PRD §4.2 对齐（"1 tap 回主流程"）。
- **账单 Tab"最近 5 笔 + 查看全部"**（`BillScreen.kt` / `BillViewModel.kt`）：新加 `recentTransactions: List<TransactionEntity>`（`take(5)`）+ "查看全部流水（共 N 笔）"按钮。超过 5 笔才显示"查看全部"按钮。PRD §4.1 对齐。
- **新增 `AllTransactionsScreen`**（`ui/list/AllTransactionsScreen.kt` + `AllTransactionsViewModel.kt`，共 19 KB）：独立"全部流水"页，复用 `FilterBottomSheet` + `SearchBar` + 软删除长按。PRD §4.1 + §8.2 对齐（"全部流水页"模块原本缺失）。

#### P1 — 应修

- **`CircularProgressIndicator` Modifier bug**（`SettingsScreen.kt`）：`Modifier.height(16.dp)` 改 `Modifier.size(20.dp) + strokeWidth = 2.dp`，正确控制大小。
- **PRD §7.1 主题色文案对齐**：把模糊的"青绿色"改成精确的"松石绿 / teal" + 主色 hex `#006C5C`，与 `Color.kt` 的 `Brand40 = 0xFF006C5C` 对齐。
- **PRD §8.3 文档对齐**：
  - 删 toml 片段里残留的 `vico = "2.0.0"`（实际未引入）
  - 图表技术栈描述从"Vico (或 MPAndroidChart)"改为"**纯 Compose Canvas**（自绘）"，与 `CategoryPieChart.kt` / `DailyLineChart.kt` 一致

#### P2 — 工程债

- **`TransactionType` / `CategoryType` Kotlin 枚举**（PRD §5.3）：从 String 散落 95 处改造为枚举 + Room TypeConverter 互转
  - 新增 `domain/model/TransactionType.kt` / `CategoryType.kt`（带 `fromOrNull` 安全降级）
  - 新增 `data/db/Converters.kt`（`@TypeConverter` enum ↔ String）
  - `TransactionEntity.type: String` → `TransactionType`
  - `CategoryEntity.type: String` → `CategoryType`
  - `AppDatabase`：`@TypeConverters(Converters::class)` + `version = 1 → 2`
  - 17 个文件批量替换（main + test + 备份层）
  - `RecordMode.toType(): String` → `TransactionType`，新增 `RecordMode.toCategoryType(): CategoryType`
  - `BillFilter.types: Set<String>` → `Set<TransactionType>`
  - `FilterBottomSheet` 同步改 enum
  - ProGuard 加 enum keep 规则
  - DB schema 兼容（"INCOME"/"EXPENSE"/"TRANSFER" 字符串继续可用）
- **Schema v1→v2 Migration**：新增 `MIGRATION_1_2`（no-op migrate 函数）+ `DatabaseModule.addMigrations(MIGRATION_1_2)`
- **Migration Test**（`src/androidTest/.../MigrationTest.kt`，2 个测试场景）：验证 v1 数据升级到 v2 后完整保留 + type 字段正确

#### P3 — 清理

- `domain/` 目录从空升级为"活起来"（含 TransactionType / CategoryType 枚举）

### Privacy

无变化（仍零运行时权限）

### Technical

- Room schema version：1 → 2（兼容升级）
- 主代码文件数：52 → 56（+ AllTransactionsViewModel/Screen + Converters + TransactionType/CategoryType）
- 单元测试文件数：8 → 10（含新加的测试）
- 集成测试文件数：0 → 2（含 MigrationTest）
- 文档数：5 → 7（+ AUDIT_REPORT.md / AUDIT_FIX_REPORT.md）

### Fixed

- **账单 Tab 性能/体验**：之前把所有流水挤在一个 LazyColumn；现在只显示最近 5 笔，统计卡和饼图/趋势图能直接看到
- **子页 1-tap 回主页**：之前需要先按返回，再点 Tab；现在子页直接显示 Tab
- **enum 重构**：消除 String 散落 95 处；编译期类型安全

### Security

无变化

---

## [0.1.0] - 2026-07-19

首个可分享安装包。**构建通过后可发给朋友试用**（个人分享；release 暂用 debug 签名）。详见 [RELEASE_NOTES_v0.1.0.md](./RELEASE_NOTES_v0.1.0.md)。

### Added

#### Phase 1 — 项目骨架与基础记账
- 项目骨架：Kotlin 2.0.21 + Jetpack Compose (BOM 2024.10.00) + Material 3 + Hilt 2.52 + Room 2.6.1
- 单 Activity 架构 + AndroidX Navigation Compose 三 Tab（账单 / 记账 / 设置）
- 主题：浅色 / 深色 / 跟随系统（DataStore 持久化）
- 内置默认类别（餐饮 / 工资等），首启自动 seed
- 基础记账：支出 / 收入 + 类别 + 账户 + 说明 + 时间
- 金额工具 `Money`（分 ↔ "1.00"） + 时间工具 `DateExt`（monthRange / format）

#### Phase 2 — 多账户与转账
- 多账户管理（创建 / 编辑 / 归档）
- 多类别管理（创建 / 编辑 / 归档；支出 / 收入）
- 转账业务：转出/转入两端账户、金额守恒校验
- 流水详情页（点击账单进入）+ 编辑模式
- 转账不计入支出/收入（独立 `transferCents`）

#### Phase 3 — 统计与搜索
- 月度汇总卡（支出 / 收入 / 转账 / 净流）
- 支出/收入比例（收入为 0 时隐藏比例条）
- 类别饼图（纯 Compose Canvas）
- 日/周/月趋势折线图（Compose Canvas）
- 搜索（按 description 模糊匹配）
- 筛选 Sheet（按账户 / 类别 / 类型 + 自定义时间区间）

#### Phase 4 — 备份 / 回收站 / 主题
- JSON 备份导出（v1 schema）：SAF（不需要存储权限）
- JSON 备份导入（合并模式：同名账户/类别复用 id，id 重新映射）
- 回收站：软删除 + 30 天后由 WorkManager 自动清理
- 手动深色模式切换（系统 / 浅色 / 深色）

#### Phase 5 — 发布准备
- R8 / ProGuard 开启（`isMinifyEnabled = true` + `isShrinkResources = true`）
- keep 规则覆盖 kotlinx-serialization / Room / Hilt / WorkManager（Compose 不全量 keep）
- ABI splits：`arm64-v8a` / `armeabi-v7a` / `x86_64` + universal APK 兜底
- SplashScreen：`Theme.SplashScreen` + `postSplashScreenTheme` + `installSplashScreen()`
- release 个人分享签名：debug keystore（可直接安装）
- 单元测试（含筛选组合）+ androidTest：Migration + Room 插入/观察烟雾
- 发布说明：`RELEASE_NOTES_v0.1.0.md`

### Privacy

- **不申请任何运行时权限**（无 INTERNET / STORAGE / NOTIFICATION）
- `allowBackup="false"`：数据不上系统云备份
- 备份 / 恢复走 SAF（Storage Access Framework），用户选目录，App 不申请存储权限

### Technical

- minSdk = 24（Android 7.0）/ targetSdk = 35（Android 15）/ JVM 17
- 包名：`com.biehuale.app`
- 金额单位：分（Long，永不用 Double）
- 时间单位：epoch millis（Long）
- 备份格式：JSON v1，schemaVersion 字段做不可破坏性升级

### Fixed

（首个版本，无历史 bug 记录）

### Security

- 数据库：Room + 物理 SQLite 存储于 App 私有目录（其他 App 不可访问）
- 备份 JSON：明文存储于用户选定的 SAF 目录（用户自管）
- 不上传任何数据到外部服务器

---

## 版本号规划

- **0.1.0**：M1–M5 全部 phase 完成，可分享给朋友用
- **0.2.0**（当前）：v0.1.0 产品方向审查修复
- 0.3.x：周预算、月度预算提醒
- 1.0.0：累计运行稳定后再发
