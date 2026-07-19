# 别花乐 v0.2.0 发布说明

v0.1.0 的产品方向审查修复版。**对得上 PRD 的可分享安装包**。详见 [`AUDIT_REPORT.md`](./AUDIT_REPORT.md) 和 [`AUDIT_FIX_REPORT.md`](./AUDIT_FIX_REPORT.md)。

## 安装

同 v0.1.0：

```powershell
$env:JAVA_HOME = "D:\java\jdk-17"
cd android
.\gradlew.bat :app:assembleRelease
```

APK 输出目录：`android/app/build/outputs/apk/release/`
- 推荐手机：`app-arm64-v8a-release.apk`
- 老设备：`app-armeabi-v7a-release.apk`
- 模拟器：`app-x86_64-release.apk`
- 不确定架构：`app-universal-release.apk`

> 当前 release 仍**使用 debug 签名**，便于个人直接安装。

## 从 v0.1.0 升级

- **App 升级**：Room 自动跑 `MIGRATION_1_2`（no-op 兼容升级）→ 数据完整保留
- **备份 JSON**：v1 格式继续可用；导入时 `BackupImporter` 用 `fromOrNull` 安全降级
- **图标 / 主题 / 隐私 / SDK 配置**：全部保持不变

## v0.2.0 关键改进

### 1. 底部 Tab 永远可见（PRD §4.2 对齐）

之前：进入"账户管理 / 类别管理 / 回收站 / 流水详情 / 编辑"等子页时，底部 Tab 栏消失 → 必须先按返回，再点 Tab（多 1 tap）。

现在：底部 Tab 永远显示，子页 Tap Tab 即时切换到对应顶层，**1 tap 回主流程**。

### 2. 全部流水页（PRD §4.1 + §8.2 对齐）

**之前**：账单 Tab 直接把所有流水堆在同一个 LazyColumn 里，统计卡和饼图/趋势图要滚很久才看到，大量账（1000+ 笔）时性能压力大。

**现在**：
- 账单 Tab：只显示"最近 5 笔"+ "查看全部流水（共 N 笔）→"按钮（超过 5 笔才显示）
- 全部流水页：独立 `AllTransactionsScreen`（19 KB），复用 `FilterBottomSheet` + `SearchBar` + 软删除长按

### 3. `TransactionType` / `CategoryType` Kotlin 枚举（PRD §5.3 对齐）

**之前**：95 处 `"INCOME"/"EXPENSE"/"TRANSFER"` 字符串字面量散落在 UI / VM / Repository 层，靠 `require(...)` 兜底。

**现在**：
- 新增 `domain/model/TransactionType.kt` / `CategoryType.kt` 枚举
- Room `Converters` 自动 `enum ↔ String` 互转（DB schema 兼容）
- Entity 字段：`type: TransactionType` / `type: CategoryType`
- 全工程 17 个文件 / 95 处字符串字面量替换为 enum

### 4. Room Schema v1→v2 Migration（PRD §9.4 对齐）

**之前**：`AppDatabase.MIGRATION_1_2` 是占位 TODO，无实际实现 + 无 Migration 测试。

**现在**：
- `MIGRATION_1_2` 实装（no-op migrate 函数：表结构未变，仅 Kotlin 字段类型升级）
- `DatabaseModule.addMigrations(MIGRATION_1_2)` 注册
- 新增 `src/androidTest/.../MigrationTest.kt`：2 个测试场景，验证 v1 数据升级到 v2 后完整保留 + type 字段正确

### 5. PRD 文档对齐

- §7.1 主题色文案：模糊的"青绿色"→ 精确的"松石绿 / teal" + 主色 hex `#006C5C`
- §8.3 toml 片段：删 `vico = "2.0.0"`（实际未引入）
- §8.3 图表技术栈描述：从"Vico (或 MPAndroidChart)"→ "纯 Compose Canvas（自绘）"

## 已知限制（与 v0.1.0 相同）

- 无云同步，数据仅存本机
- 备份请用设置里的导出（SAF），不要依赖系统自动云备份（`allowBackup=false`）
- 不上架 Play / 国内应用商店；无正式发布签名体系
- 图标为可分享用的 adaptive vector，非设计师终稿
- 多语言：当前只支持中文 + 英文

## 隐私

完全离线，不申请 INTERNET / 存储等运行时权限。

## 测试

- 单元测试（`src/test`）：10 个文件，约 70 个测试方法
- 集成测试（`src/androidTest`）：2 个文件，含 MigrationTest

```powershell
# 单元测试
.\gradlew.bat test

# 集成测试（含 MigrationTest）
.\gradlew.bat connectedAndroidTest
```

## 文件统计

| 项 | v0.1.0 | v0.2.0 | 增量 |
|----|--------|--------|------|
| 主代码 .kt | 52 | 56 | +4 |
| 单元测试 .kt | 8 | 10 | +2 |
| 集成测试 .kt | 0 | 2 | +2 |
| 文档 .md | 5 | 7 | +2 |
| DB schema | 1 | 2 | +1 |
