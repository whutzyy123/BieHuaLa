# 别花乐 (BieHuaLe) — 第一性原理代码审查报告

> **审查时间**：2026-07-19  
> **修订时间**：2026-07-19（对照源码复盘后修正事实、重分级、补遗漏）  
> **审查范围**：v0.2.0 全量代码（56 main + 10 test + 2 androidTest）  
> **审查方法**：正确性/边界从**代码本身**推演；产品范围用 PRD 裁剪（见 §7）  
> **审查者**：Mavis

---

## 0. 审查方法论

### 0.1 第一性原理（不预设实现先验）

代码是**人的表达**，需要回答 4 个根本问题：

| 问题 | 推论 |
|---|---|
| **它在做什么？** | 如果看不出来 → 命名/结构有问题 |
| **为什么这样做？** | 如果没合理理由 → 过度设计 或 误用 |
| **边界在哪里？** | 任何数量、长度、频率都该有清晰边界 |
| **失败时会怎样？** | 错误处理是最后防线，不是装饰 |

### 0.2 单用户本地 App 的核心命题

- **可靠性 > 性能**：用户只有自己一个，不会用高频，但数据丢了就是丢了
- **正确性 > 功能多**：少做错事 > 多做事
- **可恢复 > 不可逆**：所有破坏性操作都该有"取消路径"
- **本地 > 上云**：不上云是产品哲学，但也要承担"丢手机 = 丢数据"风险

### 0.3 审查维度

1. **正确性** — 代码做的是它声称要做的事吗？
2. **边界** — 输入/输出/容量/时区的边界都覆盖了吗？
3. **一致性** — 类似事情有类似写法吗？
4. **可演化性** — 需求变化时改起来痛不痛？
5. **简洁性** — 每一行代码在解决真问题吗？
6. **测试** — 关键路径 + 边界有覆盖吗？

### 0.4 优先级定义（本修订后）

| 级 | 含义 |
|---|---|
| **P0** | 可导致**账本余额/交易正确性**错误，且存在现实用户路径 |
| **P1** | 可靠性/一致性缺口，应修；规模或触发条件有限 |
| **P2** | 工程债、死代码、展示边界、性能优化 |
| **P3** | 注释、版本号、风格、可选清理 |

---

## 1. 全局观察

| 维度 | 数据 | 评价 |
|------|------|------|
| **代码量** | ~311 KB / 56 .kt main | 单用户 App **偏大**，但功能完整可接受 |
| **测试代码比** | 83 个 `@Test` / 56 main 文件 | **~1.48 倍**，充足 |
| **最大文件** | `ui/record/RecordScreen.kt` 579 行 | ⚠️ 单文件 **过大**（键盘已抽 `MoneyKeypad`） |
| **第二大** | `ui/settings/SettingsScreen.kt` 491 行 | ⚠️ 同上 |
| **数据层** | 4 Entity + 3 DAO + 3 Repository + Backup×2 + Converters | 结构合理 |
| **UI 层** | 主 Tab + 子页 + components | 偏多但分层清晰 |
| **测试套件** | **10** unit test + 2 androidTest | 关键路径覆盖 |

### 1.1 文件大小 Top 10（复杂度热点）

| 文件 | 行数 | 评价 |
|------|------|------|
| `ui/record/RecordScreen.kt` | 579 | **可拆** — Mode / 类别网格 / 时间 / 保存；键盘已复用 `MoneyKeypad` |
| `ui/settings/SettingsScreen.kt` | 491 | **可拆** — 多 SectionCard + Dialog |
| `ui/settings/CategoryManageScreen.kt` | 453 | **可拆** — ListItem + EditDialog + 确认框 |
| `ui/settings/AccountManageScreen.kt` | 445 | **可拆** — 同上结构 |
| `ui/bill/BillScreen.kt` | 392 | **合理** |
| `ui/bill/BillViewModel.kt` | 411 | **合理** — `BillAggregator` 纯函数可测 |
| `ui/list/AllTransactionsScreen.kt` | 363 | **合理** — 与 BillScreen 镜像 |
| `ui/record/RecordViewModel.kt` | 363 | **onSave 偏长** |
| `ui/settings/RecycleBinScreen.kt` | 307 | **合理** |
| `data/backup/BackupImporter.kt` | 307 | **合理** — 合并逻辑集中 |

### 1.2 工程化指标

| 指标 | 状态 |
|------|------|
| ProGuard / R8 启用 | ✅ release `isMinifyEnabled = true` |
| ABI splits | ✅ `arm64-v8a / armeabi-v7a / x86_64 + universal` |
| 零运行时权限 | ✅ AndroidManifest 无 INTERNET / STORAGE 等 |
| Compose BOM | ✅ 2024.10.00 |
| Room schema 导出 | ✅ `schemas/{1,2}.json`，`exportSchema=true` |
| Migration 测试 | ✅ `MigrationTest` 存在 |
| Hilt 配置 | ✅ `@HiltAndroidApp` + **10** 个 `@HiltViewModel` + 1 个 `@HiltWorker` |
| 备份 JSON schema | ✅ `schemaVersion` + 拒绝不兼容版本 |
| ProGuard 规则 | ✅ kotlinx-serialization / Room / Hilt / enum keep |
| `versionName` | ✅ 已随修复迭代至 `0.3.1`（见 CHANGELOG） |

---

## 2. 严重问题（P0 — 必修）

### 🔴 2.1 软删后重导旧备份 → 重复交易 + 余额虚高

**位置**：`data/backup/BackupImporter.kt`（`fingerprint` + `mergeBackup`）

**问题**：
```kotlin
fun fingerprint(tx: TransactionEntity): String =
    listOf(
        tx.amount.toString(),
        tx.type.name,
        // ...
        (tx.deletedAt != null).toString()  // ← 关键
    ).joinToString("|")
```

**复现路径**：
1. 导出备份（交易均为 `deletedAt = null`）
2. 在 App 内软删若干笔 → DB 中同内容指纹末尾为 `"true"`
3. 再导入**同一份**备份 → 备份行指纹末尾 `"false"`，与软删行**匹配不上**
4. `mergeBackup` **再 insert 一条 active 副本** → `getBalance` 只计 `deleted_at IS NULL` → **余额翻倍/虚高**

当前导入**仅有 merge**，无 restore/覆盖模式；现有测试覆盖「导出再导入不重复」（双方均为 active），**未覆盖软删后重导**。

**这不是「加一句 KDoc 就够」**：直接影响账本正确性。

**修复方向（需产品二选一或组合）**：
1. fingerprint **去掉** `deletedAt`；命中已存在行时：若本地软删则 **restore**，否则 skip
2. 或提供明确的「覆盖导入 / 重置后导入」模式
3. 补测试：导出 → 软删 → 再导入 → 交易行数与余额不变（或按选定语义）

---

## 3. 中等问题（P1 — 应修）

### 🟡 3.1 `emptyBin()` / `cleanupExpired()` 逐条删除、无批量事务

**位置**：
- `ui/settings/RecycleBinViewModel.kt`（`emptyBin`）
- `data/repository/TransactionRepository.kt`（`cleanupExpired`）

**问题**：`forEach { hardDelete(id) }`，每条独立 SQL/事务；中途失败时**已删部分无法回滚**。

**澄清（勿误读）**：成功文案在 `forEach` **之后**；任一条抛错会走 `catch` 发 Error，**不会**在部分失败时仍提示「已清空」。真实风险是**数据半清空**，不是成功提示撒谎。

**修复方向**：
1. DAO：`DELETE FROM transactions WHERE id IN (:ids)`（或按 `deleted_at < :threshold` 一条 SQL）
2. Repository：`database.withTransaction { ... }`
3. ViewModel：一次调用批量清空

单用户回收站规模通常不大 → **P1**（非 P0）。

---

### 🟡 3.2 `softDelete` 静默吞异常（Bill + AllTransactions）

**位置**：
- `ui/bill/BillViewModel.kt`
- `ui/list/AllTransactionsViewModel.kt`

```kotlin
try {
    transactionRepository.softDelete(id)
} catch (_: Exception) {
    // 列表会通过 Flow 自动 refresh  ← 失败时 Flow 也不会变
}
```

**对比**：`TransactionDetailViewModel` 有 `_events` 报错。两处列表页无反馈渠道。

**修复**：为 Bill / AllTransactions 增加 `_events`，失败时 Snackbar；成功可静默（列表会刷新）。

---

### 🟡 3.3 `RecycleBinViewModel.restore()` 不校验更新行数

**位置**：`ui/settings/RecycleBinViewModel.kt` + DAO `restore`

`UPDATE` 对无效 id 通常**不抛错**；VM 仍 emit「已恢复」。应检查 row count / 或恢复后读库确认。

---

### 🟡 3.4 `SettingsViewModel.createAccount` 与 `AccountManageViewModel.create` 重复

两处都做：名称 1–20、金额解析、异常捕获、事件。AccountManage 多 icon/color。

**修复建议**：Settings 的「快速新建」改为导航到账户管理页，或共用同一创建入口（勿再复制一份校验）。

---

### 🟡 3.5 `BackupExporter.withTransaction`（读快照）

**位置**：`data/backup/BackupExporter.kt`

块内：`observeAll().first()` 拼装；块外：序列化 + 写 URI。

**修正后的判断**：多表导出用读事务拿**一致快照**是合理的；IO 已在块外，**不要**为「少一次锁」而删掉事务。可保留，并在 KDoc 写明「仅为多表一致快照」。

（原「移除 withTransaction」建议已撤回。）

---

### 🟡 3.6 `RecordViewModel.onSave` 校验链过长

提取 `validate(state): Error?`，校验与保存分离，便于单测。

---

### 🟡 3.7 Seed 注释不一致

**位置**：`di/DatabaseModule.kt` **模块级** KDoc（约 L28–31）仍写「CoroutineScope 异步」；`SeedCallback` 本体注释与实现已是**同步** `execSQL`。

**修复**：改模块级 KDoc，与 SeedCallback 对齐。

---

### 🟡 3.8 `CleanupScheduler.runOnce` 无去重

**位置**：`BieHuaLeApp.onCreate` 每次启动 `runOnce()`；`CleanupScheduler.runOnce` 使用普通 `enqueue`，非 `enqueueUniqueWork` + KEEP。

冷启动多次会排队多个 OneTime。应用 `ExistingWorkPolicy.KEEP` 的 unique work，或仅依赖 Periodic。

---

### 🟡 3.9 `RecordScreen` 仍偏大（非键盘）

`MoneyKeypad` **已复用**（`ui/common/MoneyInput.kt`）。可继续拆类别网格 / 时间选择 / 账户选择等。维护痛点，非功能 bug。

---

## 4. 小问题（P2 / P3）

### 🟢 4.1 `BillAggregator.startOfWeek`（原误标 P0）

**位置**：`BillViewModel.kt` 内 `BillAggregator`

使用 `Calendar` + `firstDayOfWeek = MONDAY`。跨年边界未专项测试；对折线图分桶多为展示问题。

**建议**：补 12/31→1/1 测试；可选迁到 `java.time`。→ **P2**

---

### 🟢 4.2 `searchByDescription` LIKE 未转义（原误标 P0）

**位置**：`TransactionDao.searchByDescription`

DAO 层 `%` / `_` 未转义属实，但 **UI 未调用该 DAO**：`BillViewModel` / `AllTransactionsViewModel` 对内存列表做 `contains()`。属死代码路径。

**建议**：接入 UI 前先转义 + `ESCAPE`；或删 DAO 方法，统一内存/FTS 策略。→ **P2**

---

### 🟢 4.3 `BillViewModel.uiState` 性能

`combine` 多 Flow，全量 filter/groupBy。1K 笔以下可接受。→ **P2**

---

### 🟢 4.4 `CategoryEntity.isBuiltin` 默认值

Entity 默认 `isBuiltin = false`，创建路径多显式传参。→ **P3**

---

### 🟢 4.5 `libs.versions.toml` / 依赖卫生

- `ktlint` 版本未挂插件
- `androidx-test-espresso-core` **已在** `build.gradle.kts` 声明，但 androidTest **源码未用**
- `androidx-test-runner` alias 使用情况需再对一下 dependencies

→ **P3**

---

### 🟢 4.6 ProGuard 注释残留

`app/build.gradle.kts` 中 `// ---------- Charts ----------`（Vico 已删）。→ **P3**

---

### 🟢 4.7 fingerprint 用字符串拼接

定长字段 `joinToString("|")` 对个人账本碰撞可忽略；**勿**为「加密感」优先上 SHA-256。更优先修 §2.1 语义。→ **P3**（可选）

---

### 🟢 4.8 Account / Category 编辑 Dialog UI 重复

可抽 `IconColorPickerSection`。→ **P2**

---

### 🟢 4.9 `MainActivity` 注入 ThemePreferences

单 Activity 可接受。→ **P3**

---

### 🟢 4.10 子路由下 Bottom Tab 无选中态

`AppNav` 用 `currentRoute == route` 精确匹配；进入详情 / 全部流水 / 设置子页时三 Tab 可能全不亮。→ **P2**

---

### 🟢 4.11 `versionName` 与文档不一致

`versionName = "0.1.0"` vs 文档 v0.2.0。→ **P3**

---

### 🟢 4.12 DB 缺 CHECK 约束

Repository `require` 是主防线；Room 对 CHECK 支持有限。单用户绕过 Repository 概率低。→ **P2**（有 Migration 成本时再做）

---

## 5. 已澄清的误报（勿再按旧结论改）

| 旧结论 | 正确理解 |
|--------|----------|
| `AccountManageViewModel` 的 `transactionRepository` 冗余可删 | **错误**。`combine(..., observeAllActive())` 用于交易变化后刷新余额；删掉会坏刷新 |
| RecordScreen 未复用 MoneyInput | **错误**。已 `import` 并使用 `MoneyKeypad` |
| emptyBin 部分失败仍提示「已清空」 | **错误**。见 §3.1 |
| BackupExporter 应去掉 `withTransaction` | **撤回**。见 §3.5 |
| Hilt「3 个 ViewModel」 | 实际 **10** 个 |
| 「8 个 unit test 文件」 | 实际 **10** 个 |

---

## 6. 维度评分（修订后）

| 维度 | 评分 | 关键问题 |
|------|------|----------|
| **正确性** | 🟡 6.5/10 | §2.1 备份软删重导；§3.1 半清空 |
| **边界** | 🟢 7/10 | 周边界测试、搜索双路径 |
| **一致性** | 🟢 7/10 | softDelete 错误处理不一致；建账户重复 |
| **可演化性** | 🟢 8/10 | v0.2 enum + MigrationTest |
| **简洁性** | 🟢 7/10 | 大 Screen、onSave 链 |
| **测试** | 🟢 8/10 | 83 `@Test`；缺软删重导备份用例、Compose UI |
| **可恢复性** | 🟢 8/10 | 30 天回收站 + JSON 备份（merge 语义需写清） |
| **隐私** | 🟢 10/10 | 零运行时权限、SAF、无网络 |
| **性能** | 🟢 7/10 | 1K 笔 OK |

**总分：约 7.2/10** — 主路径健康；**1 个 P0 数据正确性** + 若干 P1 应在下一小版本处理。

---

## 7. 修复计划（修订后）

### 阶段 1（P0 — 必修）

| 任务 | 文件 | 验收 |
|------|------|------|
| 1.1 fingerprint / merge 语义：软删后重导不重复、不虚高余额 | `BackupImporter.kt` + 测试 | 导出→软删→再导入，余额与活跃交易数符合选定语义 |

### 阶段 2（P1 — 应修）

| 任务 | 文件 | 验收 |
|------|------|------|
| 2.1 `emptyBin` / `cleanupExpired` 批量 + `withTransaction` | Dao / Repository / RecycleBinVM | 中途失败可回滚或明确失败且不半成功 |
| 2.2 Bill + AllTransactions `softDelete` 错误事件 | 两 ViewModel + Screen | 失败 Snackbar |
| 2.3 `restore` 校验 row count | RecycleBinVM / Repository | 无效 id 不提示成功 |
| 2.4 Settings 快速建账户收敛 | Settings* / AccountManage* | 单一入口 |
| 2.5 Seed 模块级 KDoc | `DatabaseModule.kt` | 与同步实现一致 |
| 2.6 `runOnce` unique + KEEP | `CleanupScheduler.kt` | 反复启动不堆 job |
| 2.7 `onSave` 提取 `validate()` | `RecordViewModel.kt` | 可单测 |
| 2.8 Exporter 读事务 KDoc（保留事务） | `BackupExporter.kt` | 说明快照意图 |

### 阶段 3（P2 — 优化）

| 任务 | 验收 |
|------|------|
| 3.1 `startOfWeek` 跨年测试 / 可选 `java.time` | 12/31→1/1 |
| 3.2 搜索：删死 DAO 或转义后接入 | 单一策略 |
| 3.3 Tab 选中态支持子路由 | 详情页时父 Tab 仍亮 |
| 3.4 RecordScreen 继续拆组件 | < 400 行目标可选 |
| 3.5 IconColorPicker 复用 | Account/Category 共用 |
| 3.6 DB CHECK（可选 Migration） | 有成本再做 |

### 阶段 4（P3 — 清理）

| 任务 |
|------|
| `versionName` → `0.2.0`（或与 CHANGELOG 对齐） |
| 删 Charts 注释；清理未用 ktlint / 未用 espresso 源码或依赖 |
| AGENTS / CHANGELOG 登记本轮审查结论 |

---

## 8. 不在范围内

- **iOS / 云同步 / 多用户 / 预算 / 自动记账 / 标签**：PRD §3.2 明确不做
- **1 万笔压测**：当前单人规模不痛；有增长再做

---

## 9. 总结

### 优点
- 转账、余额、软删除主路径有测试，架构 data → domain → ui 清晰
- 隐私彻底：零运行时权限 + SAF + 无网络
- v0.2 枚举改造 + Migration 显示可演化性

### 风险点（修订后）
- **1 个 P0**：备份 merge + fingerprint 含 `deletedAt` → 软删后重导重复记账
- **若干 P1**：批量删除原子性、列表软删无反馈、restore 假成功、清理 Worker 去重等
- **勿再执行**的旧误修：删 `AccountManageViewModel` 的 `transactionRepository`、去掉 Exporter 读事务

### 建议
- 阶段 1（P0）优先，配测试  
- 阶段 2（P1）随 v0.3.0  
- 阶段 3–4 按需  

### 落地状态（2026-07-19）

**已按本报告阶段 1–4（不含 DB CHECK / 账单 DAO 下推）落地于 v0.3.0。**  
**v0.3.1 残余修复**：备份 deletedAt 矩阵、期初余额策略、onSave 竞态、`to_account_id` 索引（Room v3）等。详见 `CHANGELOG.md` / `RELEASE_NOTES_v0.3.1.md`。

---

**初版审查**：2026-07-19 15:51 (CST)  
**源码复盘修订**：2026-07-19  
**修复落地**：2026-07-19（v0.3.0 / v0.3.1）  
**下次审查建议**：下一功能版本或发现新数据路径时
