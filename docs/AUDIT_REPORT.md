# 别花乐 (BieHuaLe) — 产品方向审查报告

> **审查时间**：2026-07-19
> **审查基准**：[`docs/PRD.md`](./PRD.md) v0.2（2026-07-19）
> **审查范围**：v0.1.0 已交付的全部代码
> **审查方法**：逐条对照 PRD §3 / §4 / §5 / §6 / §7 / §8 / §9 / §10 的需求项，与代码实际实现做 diff

---

## 0. 总览

| 维度 | 结论 |
|------|------|
| **核心产品方向** | ✅ **一致** — 单用户、本地优先、不上云、不打扰，定位守住了 |
| **MVP 13 项功能 F1–F13** | ✅ **12/13 完成** + 1 项部分偏离（最近 5 笔 + 查看全部 → 实际"直接展示全部"） |
| **数据模型** | ✅ **一致** — 字段、外键、索引、枚举值全部对得上 |
| **业务规则 §6** | ✅ **完全符合** — 转账 / 软删除 / 货币 / 类别 / 账户全部 OK |
| **隐私 §9.2** | ✅ **完全符合** — 零运行时权限 |
| **技术栈 §8.3** | ✅ **基本符合** — 1 项偏离（图表从 Vico 改为纯 Compose Canvas，§13 已记录） |
| **UI/UX §4 / §7** | ⚠️ **2 项严重偏离**（底部 Tab、子页结构） |
| **备份 §10** | ✅ **完全符合** |
| **不做项 N1–N10** | ✅ **全部遵守** — 没有任何范围蔓延 |
| **总体评分** | 🟢 **方向守住了**，**2 处产品体验出入需修复** + **3 处文档与实现小偏差可后续对齐** |

---

## 1. 严重出入（必须修复，否则违反产品意图）

### 🚨 1.1 底部 Tab 在子页被隐藏（违反 PRD §4.2）

**PRD 要求**（§4.2 全局交互）：
> 底部 Tab 永远可见——三主 Tab 内始终显示；进入详情/子页时仍保留底部 Tab（1 tap 回主流程）

**实际实现**（`ui/nav/AppNav.kt:39-44`）：

```kotlin
// 子页/详情页时隐藏底部 Tab
val showBottomBar = currentRoute in setOf(
    Destinations.BILL,
    Destinations.RECORD,
    Destinations.SETTINGS
)
```

**影响**：进入"账户管理 / 类别管理 / 回收站 / 流水详情 / 编辑"等子页时，底部 Tab 栏消失。用户必须先按返回，再点 Tab，多 1 tap。

**修复方向**（任选一种）：
- 方案 A（推荐）：去掉 `showBottomBar` 条件，**所有页面都显示底部 Tab**。子页 Tap Tab 即时切换到对应顶层。
- 方案 B：把"返回 + Tab 切换"做二选一，保留方案 A 的便利同时给一个左上返回箭头。

**修改文件**：`D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\nav\AppNav.kt`

---

### 🚨 1.2 全部流水页（`AllTransactionsScreen`）未实现

**PRD 要求**（§4.1 Tab 1：账单 + §8.2 模块结构）：
> 最近流水：最近 5 笔（点击 → 详情）
> "查看全部"按钮 → 跳全部流水页（带筛选/搜索）

```text
ui/
└── list/                    # PRD §8.2 明确要求
    ├── AllTransactionsScreen.kt
    └── AllTransactionsViewModel.kt
```

**实际实现**：
- `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\list\` 目录**不存在**
- `BillScreen.kt` 内**没有**"查看全部"按钮
- 账单 Tab 直接在同一个 `LazyColumn` 列出所有流水（按月分组），没有 5 笔限制

**影响**：
1. PRD §4.1 的"最近 5 笔 + 查看全部"两段式 UI 没了，账单 Tab 第一屏就把所有交易挤下去，统计卡和饼图要滚很久才看到
2. 大量账（1000+ 笔）时账单 Tab 性能压力（每次重组完整 list + 滚动成本），而独立"全部流水"页可以做更激进的优化（分页/虚拟化）

**修复方向**（推荐）：
1. 在 `BillScreen.kt` 的汇总卡/饼图/趋势之后**只显示最近 5 笔**（改 `transactions.take(5)` 即可）
2. 加一个"查看全部流水 →"按钮，点击 `navController.navigate("all-transactions")`
3. 新建 `ui/list/AllTransactionsScreen.kt` + `AllTransactionsViewModel.kt`
4. 在 `Destinations.kt` 加路由 `ALL_TRANSACTIONS = "all-transactions"`
5. `AppNav.kt` 加 `composable(ALL_TRANSACTIONS) { AllTransactionsScreen(...) }`

**修改文件**：
- 新增 `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\list\AllTransactionsScreen.kt`
- 新增 `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\list\AllTransactionsViewModel.kt`
- 修改 `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\bill\BillScreen.kt`
- 修改 `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\nav\Destinations.kt`
- 修改 `D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\nav\AppNav.kt`

---

## 2. 中等偏差（设计妥协，已偏离 PRD 但有合理理由）

### ⚠️ 2.1 TransactionType / CategoryType 用 String 而非 enum class

**PRD 要求**（§5.3 关键枚举）：
```kotlin
enum class TransactionType { INCOME, EXPENSE, TRANSFER }
enum class CategoryType   { INCOME, EXPENSE }
```

**实际实现**：所有业务代码用 `String`（"INCOME" / "EXPENSE" / "TRANSFER"）

**影响**：
- 字符串字面量散落，IDE 重构差
- 数据库 `type` 字段可写入任意字符串，需要 Repository 层 `require(...)` 兜底
- kotlinx-serialization 序列化没问题（String 自由）

**为什么是 String**：entity 是 Room Entity，Kotlin enum 存数据库需要 `TypeConverter`，项目早期为减少样板选了 String。

**修复方向**（v0.2 候选）：
- 加 `Converters.kt`：`@TypeConverter fun TransactionType.toString()` / `fromString(...)`
- 升级 `TransactionEntity.type: TransactionType` / `CategoryEntity.type: CategoryType`
- 全工程 `type` 字符串替换为枚举值
- DB schema 升级，version 2 + Migration

**为什么不是 v0.1 必须修**：功能正常、测试覆盖、`require` 校验防住了非法值，发布给朋友用没问题。只是工程债。

---

### ⚠️ 2.2 BillScreen 展示"全部流水"而非"最近 5 笔"

**PRD 要求**（§4.1 Tab 1：账单）：
> 最近流水：最近 5 笔（点击 → 详情）
> "查看全部"按钮 → 跳全部流水页（带筛选/搜索）

**实际**：直接展示所有流水（按月分组）。

**与 1.2 的关系**：这是 1.2 缺失的直接后果。**修了 1.2 即修复此项**。

---

### ⚠️ 2.3 AppDatabase.MIGRATION_1_2 是占位 TODO

**PRD 要求**（§9.4 健壮性）：
> 数据库迁移有 `Migration` 测试

**实际**（`data/db/AppDatabase.kt:55-62`）：
```kotlin
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // TODO(schema v2): 在此写入真实 ALTER / CREATE
    }
}
```

**影响**：目前 v0.1 schemaVersion = 1，未用到这个 MIGRATION，所以暂时无 bug。但**如果升 v2 时忘记填实际 SQL**，会回退到"毁 schema"（Room 会抛 `IllegalStateException: Migration didn't properly handle ...`），且没有 Migration 测试兜底。

**修复方向**（v0.2 候选）：
- 等真要做 schema 升级时（加 currency 字段 / 加标签系统等）才动
- 同时增加 `MigrationTest.kt`：用 `androidx.room.testing.MigrationTestHelper`，验证 v1 → v2 数据保留正确
- 在测试套件（§13 已记）中加入"每次 schema 变更必须跑 Migration 测试"作为流程

**为什么不是 v0.1 必须修**：v0.1 还没用上。

---

## 3. 小偏差（不影响功能，但与文档对不上）

### 📝 3.1 主题色"青绿色" — 实际是偏深的青绿松石色

**PRD 描述**（§7.1）："回退主题色：青绿色（暗合"乐"），作为 Android 11- 的 fallback"

**实际**（`ui/theme/Color.kt:13`）：
```kotlin
val Brand40 = Color(0xFF006C5C)  // 深松石绿
val Brand80 = Color(0xFF6FF7DD)  // 薄荷色
```

**判断**：`#006C5C` 是深松石绿 / 深青，介于"青"和"绿"之间。严格说偏"墨绿/松石绿"，"青绿色"略偏绿。**属于可接受范围**，无需修改，但品牌主色可以明确为"松石绿"更精确。

**修复方向**：把 PRD §7.1 的"青绿色"改写为"松石绿"，与 Color.kt 的 `0xFF006C5C` 对齐。

**修改文件**：`D:\BieHuaLe\docs\PRD.md` §7.1

---

### 📝 3.2 类别"护肤" / "美妆" — 实际是"护肤"

**PRD §5.2 内置类别**：支出第 9 类"护肤"

**实际**（`data/seed/DefaultCategories.kt:32`）：
```kotlin
category(name = "护肤", icon = "spa", colorHex = "#FF4081", type = "EXPENSE", sortOrder = 9)
```

**判断**：**完全一致 ✓**。这里 3.2 是反向案例 — PRD 与实现**完全对齐**，但 DevPlan 的 D9 决策有冗余说明。不需修改。

---

### 📝 3.3 SettingsScreen 的 CircularProgressIndicator 用了奇怪的 Modifier

**实际**（`ui/settings/SettingsScreen.kt:255`）：
```kotlin
CircularProgressIndicator(modifier = Modifier.height(16.dp))
```

`Modifier.height(16.dp)` 用在 `CircularProgressIndicator` 上**不会改其本身大小**（默认 40dp），而是改其所在 row 高度，可能造成 40dp 组件 + 16dp row 的视觉错位。

**修复方向**：去掉 `modifier = Modifier.height(16.dp)` 或换成 `Modifier.size(16.dp)`。

**修改文件**：`D:\BieHuaLe\android\app\src\main\java\com\biehuale\app\ui\settings\SettingsScreen.kt:255`

---

### 📝 3.4 Domain 层目录存在但为空

**PRD §8.2**：
```text
domain/                     # 领域层（可选，初期可省）
├── model/
└── usecase/
```

**实际**：
- `domain/model/.gitkeep` 存在
- `domain/usecase/.gitkeep` 存在

**判断**：PRD 写"可选，初期可省"，所以保留空目录 + `.gitkeep` 是合规的。**不需修改**，但可以考虑干脆删掉空目录，等真正需要时再加。

---

### 📝 3.5 关于页无 GitHub 链接

**PRD §13 开放问题 #6**：
> 关于页 GitHub 链接：是否开源、仓库 URL——发布前再定；MVP 可先隐藏或放占位。

**实际**（`SettingsScreen.kt` 关于区块）：
```kotlin
SettingsItem(text = "别花乐", subtitle = "v${...} · 个人记账 App")
HorizontalDivider()
SettingsItem(text = "隐私", subtitle = "完全本地 · 不联网 · 无追踪")
```

**判断**：**符合 PRD 倾向**（MVP 隐藏）。等决定开源策略后再加。**不需修改**。

---

## 4. 与 PRD 完全一致的部分（确认无偏差）

下面这些是**核对了，没问题**的，可以作为后续 v0.2 重构/扩展时的安全基石：

### 4.1 数据模型（§5）
| 项目 | 状态 | 文件 |
|------|------|------|
| `accounts` 表结构 | ✅ 完全一致 | `data/db/entity/AccountEntity.kt` |
| `categories` 表结构 | ✅ 完全一致 | `data/db/entity/CategoryEntity.kt` |
| `transactions` 表结构 | ✅ 完全一致 | `data/db/entity/TransactionEntity.kt` |
| 外键策略（SET NULL / RESTRICT） | ✅ 完全一致 | `TransactionEntity.kt:25-50` |
| 索引（4 个） | ✅ 完全一致 | `TransactionEntity.kt:51-58` |
| 内置类别（10 支出 + 5 收入） | ✅ 完全一致（"护肤"等都对） | `data/seed/DefaultCategories.kt` |

### 4.2 业务规则（§6）
| 项目 | 状态 | 验证 |
|------|------|------|
| 转账：单条记录、type=TRANSFER、amount 正数 | ✅ | `AccountRepositoryTest`, `AccountBalanceTest` |
| 转出 ≠ 转入 | ✅ | `TransactionRepositoryTest: TRANSFER toAccountId 不能等于 accountId` |
| 转账不计入支出/收入 | ✅ | `BillAggregatorTest: transfer_notCountedInExpenseOrIncome` |
| 余额公式（含转账两端） | ✅ | `AccountDao.getBalance` SQL 完全匹配 §6.1 公式 |
| 软删除 + 30 天清理 | ✅ | `TransactionRepositoryTest: cleanupExpired` |
| 分 / Long 货币 | ✅ | `Money.MAX_CENTS = 99_999_999_999L` |
| 类别同 type 不重名 | ✅ | `CategoryRepository.create: require(...)` |
| 账户不重名 | ✅ | `AccountRepository.create: require(...)` |

### 4.3 隐私 / 权限（§9.2）
- ✅ AndroidManifest **无任何 uses-permission**
- ✅ `allowBackup="false"`
- ✅ 无 INTERNET / STORAGE / NOTIFICATION 申请
- ✅ 备份走 SAF（`ActivityResultContracts.CreateDocument` / `OpenDocument`）

### 4.4 备份（§10）
- ✅ JSON schema v1 完整（schemaVersion / appVersion / exportedAt）
- ✅ 文件名格式 `别花乐_yyyy-MM-dd_HHmm.json`
- ✅ 合并模式（同名账户/类别复用 id）
- ✅ schemaVersion 校验（过低/过高都拒绝）— `BackupImporter.validateSchema` + 测试覆盖
- ✅ 整笔事务（`withTransaction { mergeBackup(...) }`）— 失败回滚

### 4.5 UI/UX 关键交互
- ✅ 比例条（支出/收入，收入=0 时隐藏）— `SummaryCard` + `MonthlySummary.expenseIncomeRatioOrNull`
- ✅ 转账显示 "微信 → 银行卡" — `BillScreen.kt:341`
- ✅ 类别归档不影响历史账 — `TransactionEntity` 外键 `ON DELETE SET NULL`
- ✅ 深色模式 3 档（SYSTEM/LIGHT/DARK）— `ThemePreferences` + `BieHuaLeTheme`
- ✅ 主题色 Dynamic Color (Android 12+) + 青绿 fallback — `Theme.kt`
- ✅ 账户/类别颜色预设 8 个 — `IconColorPresets.COLORS`
- ✅ 重置默认类别 — `CategoryRepository.resetBuiltinDefaults` + `CategoryManageScreen` 入口
- ✅ 导出进度提示 — `BackupViewModel.isExporting` + `CircularProgressIndicator`（但有 3.3 的小 bug）
- ✅ 3 Tab 顺序（账单/记账/设置）— `AppNav.BottomNav`

### 4.6 不做项（§3.2 N1–N10）
- ✅ 无预算
- ✅ 无自动记账
- ✅ 无定期账单
- ✅ 无多账本
- ✅ 无桌面小部件
- ✅ 无云同步
- ✅ 无多用户/登录
- ✅ 无标签系统
- ✅ 无指纹/应用锁
- ✅ 无 Play Store 上架

**没有任何范围蔓延**，方向守得很好。

---

## 5. 技术栈偏离（§8.3）

| PRD 列出 | 实际 | 原因 | 影响 |
|---------|------|------|------|
| Compose BOM 2024.10.00 | ✅ 同 | — | 无 |
| Room 2.6.1 | ✅ 同 | — | 无 |
| Hilt 2.52 | ✅ 同 | — | 无 |
| Vico 2.0.0（图表） | ❌ **未引入** | Vico 2.0 API 仍不稳定（§13 决策） | 用纯 Compose Canvas 自绘饼图/折线图（`CategoryPieChart.kt` / `DailyLineChart.kt`）。功能 OK，但 PRD §8.3 的 toml 片段里还有 `vico = "2.0.0"`，与实际 `libs.versions.toml` 不一致 |
| kotlinx-datetime 0.6.1 | ✅ 同 | — | 无 |

**修复方向**：把 PRD §8.3 的 Vico 描述改为"纯 Compose Canvas（自绘）"，把 toml 片段里的 `vico` 行删掉（libs.versions.toml 已经清掉了）。

**修改文件**：`D:\BieHuaLe\docs\PRD.md` §8.3

---

## 6. 性能 / 健壮性（§9.1 / §9.4）

| 项目 | 状态 |
|------|------|
| App 冷启动 ≤ 1.5s | ⚠️ 未测（无性能基线测试） |
| 列表滚动 60fps（1000 笔） | ⚠️ 未在真机验证 |
| Room 查询 ≤ 100ms | ⚠️ 未做 benchmark 测试 |
| 关键业务 100% 单测覆盖（转账） | ✅ 14 个测试覆盖转账场景 |
| Room Migration 测试 | ❌ 无（MIGRATION_1_2 还是占位） — 见 2.3 |
| 备份导入 schema 校验测试 | ✅ `BackupImporterTest` 覆盖 |
| 金额用 Long（不用 Double） | ✅ 全工程 `Long` |
| 时间用 epoch millis（不用 Date） | ✅ Room Entity + 业务代码 |

**建议**（v0.2 候选）：
- 用 `androidx.benchmark` 跑冷启动 / 查询延迟基线
- 1000 笔压测在 Compose `LazyColumn` 滚动手感（人工 + 自动截图）

---

## 7. 修复优先级建议

| 优先级 | 项 | 估时 | 理由 |
|--------|------|------|------|
| **P0** | 1.1 底部 Tab 永远可见 | 0.5h | 违反产品交互意图，影响体验 |
| **P0** | 1.2 AllTransactionsScreen + 最近 5 笔 + 查看全部 | 3-4h | PRD §4.1 明确要求，账单 Tab 首屏体验 |
| **P1** | 3.3 CircularProgressIndicator 的 Modifier.height bug | 5min | 视觉小瑕疵 |
| **P1** | 3.1 PRD 主题色文案 | 5min | 文档对齐 |
| **P1** | 5 PRD §8.3 toml 片段更新 | 5min | 文档对齐 |
| **P2** | 2.1 引入 TransactionType / CategoryType 枚举 | 4-6h | 工程债，影响后续维护 |
| **P2** | 2.3 Migration 测试 + 真实 MIGRATION_1_2 | 2-3h | 升 schema v2 前必须 |
| **P3** | 6 性能基线测试 | 4-6h | 1000+ 笔用户体感相关 |
| **P3** | 3.4 删 domain 空目录 | 1min | 清理 |

**P0 建议先做**：1.1 + 1.2 = 一个完整的功能修补包，1 个工作日内能完成。修完后 v0.1.0 才是真正"对得上 PRD"的发布版。

---

## 8. 总结

**方向是对的**。`BieHuaLe` 这个名字、3 Tab + 详情/子页结构、本地优先 + 不打扰、产品定义的全 13 项 MVP 功能、严格遵守 10 个"不做"项 — **产品哲学守住了**。

**2 处严重出入**（底部 Tab + AllTransactionsScreen）都是**导航/页面结构**的细节，不是产品哲学问题。**修起来不复杂**，但会显著改善用户体验和 PRD 对齐度。

**3 处文档/实现小偏差**可以下次版本顺手对齐。

**没发现范围蔓延**（没偷偷加预算、没偷偷加同步、没偷偷加多用户）— 这对一个多 phase 项目来说，是 PRD 文档化和执行纪律做得好的表现。

**下一步建议**：先把 P0 两项（1.1 + 1.2）修完，再发 v0.1.0 给朋友。其余的进 v0.2 backlog。

---

**审查完成时间**：2026-07-19 13:49 (CST)
**审查工具**：人工对照 + 静态扫描
**附注**：本报告是 v0.1.0 首次正式审查，下一次大审查建议在 v0.3 之后（涉及 schema 升级、新功能加 PRD 时）。
