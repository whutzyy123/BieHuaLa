# Changelog

别花乐 (BieHuaLe) 项目的所有重要变更都会记录在此。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [0.6.9] - 2026-07-20

### Changed
- 新增 `SectionPanel`：elevated 浅底圆角岛，账单 Hero/饼图/趋势/最近、记账表单、设置分组与管理/详情/全部流水轻量对齐
- 区块间距统一 `space.md`；禁止行级 Card 墙与多阴影叠层（见 UI_DESIGN §3.3 / §6 / §7）
- `versionName` → `0.6.9`（versionCode 17）

## [0.6.8] - 2026-07-20

### Changed
- 导航转场对齐 AppMotion：主 Tab 120ms fade、二级页 200ms slide+fade（去掉 Navigation 默认 700ms）
- 底栏切 Tab 改为单次 `navigate`+`popUpTo`；记账目录聚合 `flowOn(Default)`；账单 `contentSignature` 去抖
- 键盘按下 70ms；模式字重即时；列表/设置行显式 bounded ripple
- `versionName` → `0.6.8`（versionCode 16）

## [0.6.7] - 2026-07-20

### Removed
- 未使用的 DAO/Repository API（账单已改内存聚合后的区间/饼图/趋势 SQL 透传；账户/类别 `restore` 与无调用方计数查询）
- 空 Preview stub、`EmptyState.showBrand`、未用的 `RecordMode` 扩展
- 根目录旧 `RELEASE_NOTES_*` 与已完成的审查/计划文档移入 [`docs/archive/`](./docs/archive/)

### Changed
- README / AGENTS / STRUCTURE 与现行 v0.6.x 对齐；`versionName` → `0.6.7`（versionCode 15）

## [0.6.6] - 2026-07-20

### Changed
- 全局 UI 组件化：抽出 `SubScreenScaffold` / `CategoryIconCircle` / `LoadingState` / `ListSectionHeader` / `PrimaryButton` / `ManageListRow`；管理三屏、回收站、详情、全部流水改用统一壳
- `SearchBar` / `FilterBottomSheet` 迁入 `ui/common/`；快速记账编辑字段收敛为 `LedgerField`
- `versionName` → `0.6.6`（versionCode 14）

## [0.6.5] - 2026-07-20

### Added
- 账单 Tab 次级行展示**总资产**（活跃账户余额合计）；点击打开分户明细 Sheet

### Changed
- `versionName` → `0.6.5`（versionCode 13）

## [0.6.4] - 2026-07-20

### Fixed
- 账单空月不再误显示「还没有记账」；区分全库空 vs 区间空
- 「本月流水」/ 饼图跳转带当前日期区间；「查看全部」仍为全历史
- 支出/收入比例条改用 `支出÷收入`，并显示百分比
- 饼图未选中时不再半透明；点击限制在环带

### Changed
- 自定义区间标题含年、标签「区间已花」；禁止切到未来月；单月隐藏趋势「月」粒度
- 账单/全部流水账户含归档展示；`versionName` → `0.6.4`（versionCode 12）

## [0.6.3] - 2026-07-19

### Added
- 转账可选**手续费**：转出扣全额，转入实收 `amount − fee`（例：转 500、费 1 → −500 / +499）
- Room `transactions.fee`（分）+ Migration 5→6；备份 JSON `schemaVersion=2`

### Changed
- 转账表单预览随手续费更新到账金额；详情展示手续费与到账
- `versionName` → `0.6.3`（versionCode 11）

## [0.6.2] - 2026-07-19

**性能**：点击跟手——记账键盘 / Flow 下沉 / 账户余额聚合。

### Changed

- 记账：去掉逐键金额 pulse；拆分 amount/canSave/form 订阅；键盘 `graphicsLayer` 反馈
- 账单/流水：聚合与月分组 `flowOn(Default)` + `distinctUntilChanged`；搜索 debounce 150ms；列表仅软删 fadeOut
- 账户管理：一次 SQL 聚合余额，去掉 N+1 `getBalance`
- 备份导出/导入走 `Dispatchers.IO`
- Room v5：`(account_id, deleted_at, type)` 索引
- Hero 金额入场只播一次；设置外观区独立订阅
- `versionName` → `0.6.2`（versionCode 10）

---

## [0.6.1] - 2026-07-19

**快速记账**：设置预设模板，支出页下拉一点即记。

### Added

- Room v4 表 `quick_records`（类别 / 账户 / 金额分 / 说明）
- 设置 →「管理快速记账」增删改
- 记账支出页「快速记账」下拉，选中即落一笔 EXPENSE
- `versionName` → `0.6.1`（versionCode 9）

### Fixed

- 归档类别/账户时同步清理依赖的快速记账模板
- 记账保存：活跃账户/类别校验、`isSaving` 竞态与保存中锁定表单
- `TransactionRepository.save`：金额上限 + 实体存在/未归档校验
- 快速记账管理防连点；编辑失效 id 强制重选
- 备份 JSON 仍不含 `quick_records`（刻意；重装/导入不恢复模板）

---

## [0.6.0] - 2026-07-19

**Clarity Teal**：推翻雾青软萌，对齐主流记账「想用」体验。

### Changed

- 近白实色底；主色提亮 `#0B7A6A`；圆角 12 / 16 / 20
- 类别矩阵：色圆 + 图标（`CategoryIconMap`）；管理页同步
- 记账金额舞台去灰笼；可保存时主按钮实心青绿；空态主 CTA
- 底栏选中青绿 container；LedgerField 用 muted 实底
- `versionName` → `0.6.0`（versionCode 8）

---

## [0.5.0] - 2026-07-19

**Mist Teal Soft Ledger**：软萌圆润雾青账本全量换皮。

### Changed

- 圆角上调（14 / 20 / 28）；自研 `BhlIcons` 替换 Material Filled 脸
- `LedgerField` / `LedgerSheet` / `LedgerConfirm` 去 M3 露馅
- 冷启动默认进入**记账** Tab；底栏选中软椭圆底
- 空态简笔账本 + 品牌弱露出；纸感噪点微调
- `versionName` → `0.5.0`（versionCode 7）

---

## [0.4.2] - 2026-07-19

**UX 交互减层**：账单只「看」、查历史进全部流水、长按编辑、保存后连记。

### Changed

- 账单首页去掉搜索/筛选；Hero 下「本月流水」弱链；饼图点击跳转全部流水并带 `categoryId`
- 全部流水：筛选即时生效 +「完成」关 Sheet；已筛选 badge；列表长按编辑/删除
- 新建记账保存后留在记账 Tab 并清空；编辑态锁模式 + 标题「编辑账单」
- 账户下拉整块可点；无账户跳转管理；时间字段接 Date/TimePicker；类别行点击编辑；详情/回收站 CTA 权重
- `versionName` → `0.4.2`（versionCode 6）

---

## [0.4.1] - 2026-07-19

**UI 视觉平衡调优**（对照 [`docs/UI_DESIGN.md`](./docs/UI_DESIGN.md)）。

### Changed

- 解绑 `titleMedium` 金额 Mono 污染；金额仅走 `MoneyRowStyle` / `AmountText`
- 记账 ModeSelector 三等分 + 指示条居中；AmountStage / 键盘比例收敛
- 账单 Hero 呼吸与月份居中；饼图/折线降权；区块标题改为 caption 级
- 统一 `AppSpacing` 与列表行高；底栏顶部分割线；管理页透明底 + Serif 顶栏
- `versionName` → `0.4.1`（versionCode 5）

---

## [0.4.0] - 2026-07-19

**Mist Teal Ledger UI 全量重构**（[`docs/UI_DESIGN.md`](./docs/UI_DESIGN.md) / [archive/RELEASE_NOTES_v0.4.0.md](./docs/archive/RELEASE_NOTES_v0.4.0.md)）。

### Changed

- **主题**：品牌色板优先；Dynamic Color 默认关，设置可开「跟随壁纸取色」
- **字体**：Noto Serif SC / Noto Sans SC / IBM Plex Mono；金额 tabular
- **氛围底**：浅色雾青渐变 / 深色墨绿炭；柔化 `primaryContainer`
- **账单**：首屏 Hero「本月已花」；图表与最近流水下移；流水行去 Card
- **记账**：模式指示条 + 金额舞台 + 类别选中描边 + 键盘降噪
- **设置**：分组列表替代 SectionCard；关于区品牌 Serif
- **动效**：金额入场、模式色切换、软删收起
- `versionName` → `0.4.0`（versionCode 4）

---

## [0.3.1] - 2026-07-19

**残余缺陷修复**（全量复盘后）。详见 [archive/RELEASE_NOTES_v0.3.1.md](./docs/archive/RELEASE_NOTES_v0.3.1.md)。

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

**第一性原理审查修复**（[`docs/archive/REVIEW_REPORT.md`](./docs/archive/REVIEW_REPORT.md) / [archive/RELEASE_NOTES_v0.3.0.md](./docs/archive/RELEASE_NOTES_v0.3.0.md)）。

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

**v0.1.0 产品方向审查修复**。详见 [`docs/archive/AUDIT_REPORT.md`](./docs/archive/AUDIT_REPORT.md) / [`docs/archive/AUDIT_FIX_REPORT.md`](./docs/archive/AUDIT_FIX_REPORT.md) / [archive/RELEASE_NOTES_v0.2.0.md](./docs/archive/RELEASE_NOTES_v0.2.0.md)。

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

首个可分享安装包。**构建通过后可发给朋友试用**（个人分享；release 暂用 debug 签名）。详见 [archive/RELEASE_NOTES_v0.1.0.md](./docs/archive/RELEASE_NOTES_v0.1.0.md)。

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
