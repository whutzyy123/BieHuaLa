# 别花乐 (BieHuaLe) - 协作者指南 (AGENTS.md)

> **给 AI 协作者（Mavis / Copilot / Cursor / ...）看的项目规范**
>
> 这份文件帮你快速理解项目：做什么、用什么技术、怎么写代码。
> 读完后，请严格遵守"代码架构"和"开发工作流"。

## 1. 这是什么

一款**单用户、本地优先**的安卓记账 App。
- 自己用，APK 可分享给朋友
- 不上云、不登录、不打扰
- Kotlin + Jetpack Compose + Room + MVVM

## 2. 必读文档

| 文档 | 路径 | 内容 |
|------|------|------|
| PRD | [`docs/PRD.md`](./docs/PRD.md) | 做什么、为什么、范围边界 |
| 前端设计 | [`docs/UI_DESIGN.md`](./docs/UI_DESIGN.md) | 视觉调性、Token、分屏构图（UI 改版必读） |
| 目录结构 | [`docs/STRUCTURE.md`](./docs/STRUCTURE.md) | 每个目录/文件是干什么的 |
| 更新日志 | [`CHANGELOG.md`](./CHANGELOG.md) | 版本变更记录 |

历史材料（已完成的 phase 计划、审查报告、旧 RELEASE_NOTES）在 [`docs/archive/`](./docs/archive/)，**不作为日常开发依据**。

## 3. 当前状态

✅ **v0.6.9 — Clarity Teal**

视觉按 [`docs/UI_DESIGN.md`](./docs/UI_DESIGN.md) **v2.0** 落地；业务规则以 PRD 为准。

**近期关键变更**（细节见 CHANGELOG）：
- v0.6.x：Clarity Teal、快速记账、转账手续费、总资产、全局 UI 壳、点击/导航动效、区块面板分离
- 更早：v0.4 UX 减层；v0.5 Soft Ledger（已废止）

后续 UI 改动以 UI_DESIGN v2 为准；功能改动对照 PRD / STRUCTURE / CHANGELOG。

## 4. 关键约束（不可变）

| 项 | 值 | 备注 |
|----|----|----|
| compileSdk / targetSdk | 35 | Android 15 |
| minSdk | 24 | Android 7.0 |
| 金额单位 | 分（Long） | 永不用 Double |
| 时间单位 | epoch millis (Long) | 永不用 Date 直存 |
| 交易 type | Kotlin 枚举（v0.2 升级） | `TransactionType` / `CategoryType`，由 Room `Converters` 互转 |
| 备份格式 | JSON v2 | schemaVersion 不可破坏性升级；v1 备份仍可导入（`fee` 缺省 0） |
| Room schema | v1 → v6 | `1_2` / `2_3` / `3_4`（quick_records）/ `4_5`（余额索引）/ `5_6`（`fee`） |
| 包名 | `com.biehuale.app` | 不可改 |
| 隐私 | **不申请任何运行时权限** | 无 INTERNET、无 STORAGE、无 NOTIFICATION |
| 备份方式 | SAF（Storage Access Framework） | 不需要存储权限 |
| 云备份 | `allowBackup=false` | 数据不上系统云备份 |

## 5. 代码架构

```
UI (Compose) ──▶ ViewModel ──▶ Repository ──▶ Room DAO ──▶ SQLite
                                       └─▶ DataStore (设置)
                                       └─▶ WorkManager (30 天清理)
```

**永远不要**：
- ❌ 在 Composable 里直接调用 DAO（必须经过 ViewModel + Repository）
- ❌ 用 Double 存金额（永远 Long，分）
- ❌ 用 String 存日期（永远 Long，epoch millis）
- ❌ 在 ViewModel 里持有 Context（用 AndroidViewModel 不行就用 Hilt @ApplicationContext）
- ❌ 加 INTERNET 权限

## 6. 开发工作流

### 命令

```powershell
cd android
.\gradlew assembleDebug          # 编译
.\gradlew test                   # 单元测试
.\gradlew installDebug           # 装到设备
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Commit 规范（Conventional Commits）

```
<type>(<scope>): <subject>

feat(record): add transfer mode
fix(bill): monthly total wrong when transfer included
refactor(db): extract transaction dao
test(transfer): add balance calc tests
docs: update dev plan
chore: bump compose-bom to 2024.10.00
```

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修 bug |
| `refactor` | 重构（无功能变化） |
| `test` | 加测试 |
| `docs` | 文档 |
| `chore` | 构建/工具/杂事 |
| `style` | 格式 |
| `perf` | 性能 |

> 依赖版本以 `android/gradle/libs.versions.toml` 为准（当前 Compose BOM `2024.10.00`、Room `2.6.1`）。

### 命名规范

| 类别 | 规则 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `BillViewModel` |
| 函数/属性 | lowerCamelCase | `getCurrentBalance` |
| 常量 | UPPER_SNAKE | `MAX_DESCRIPTION_LENGTH = 200` |
| Compose Composable | UpperCamelCase | `BillScreen()` |
| 数据库表名 | snake_case 复数 | `transactions` |
| 数据库字段 | snake_case | `occurred_at` |
| 资源 id | snake_case | `ic_category_food` |

## 7. 测试规范

- **ViewModel 单测**：必须有（JUnit5 + MockK + Turbine）
- **Repository 单测**：必须有（in-memory Room）
- **转账业务规则**：**100% 覆盖**（最容易出 bug）
- **备份导入**：必须有测试（含异常路径）
- **Schema Migration**：必须有 androidTest（Room `MigrationTestHelper`）
- **AppDatabase 集成**：必须有（in-memory Room + 完整业务链路 + 外键约束）
- **Compose UI 测试**：不强求，关键路径建议

测试运行：
```powershell
.\gradlew test                   # 所有单元测试（src/test）
.\gradlew connectedAndroidTest   # 设备/模拟器测试（src/androidTest，含 MigrationTest）
```

主要测试覆盖：
- 工具类：`MoneyTest` / `DateExtTest`
- Repository / 余额：`AccountRepositoryTest` / `TransactionRepositoryTest` / `AccountBalanceTest`
- VM / 聚合：`RecordViewModelTest` / `BillAggregatorTest` / `AllTransactionsBoundRangeTest`
- 备份：`BackupExporterTest` / `BackupImporterTest`
- 集成：`AppDatabaseTest`；androidTest：`MigrationTest` / `TransactionRoomSmokeTest`

## 8. 开发节奏

Phase 1–5 与早期审查已完成（见 [`docs/archive/`](./docs/archive/)）。当前按 **版本迭代**：改 PRD/UI_DESIGN 边界内的功能或体验 → 更新 CHANGELOG →  bump `versionName` / `versionCode`。

## 9. 求助时

报 bug / 提需求时附上：
1. 当前版本（`versionName`）与相关 CHANGELOG 条目
2. 复现步骤
3. 期望 vs 实际
4. 错误日志（如果有）
5. 相关代码文件路径（`android\app\src\main\java\com\biehuale\app\...`）

---

**最后更新**：2026-07-20（v0.6.9 区块视觉分离）
