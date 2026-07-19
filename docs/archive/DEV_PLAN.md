# 别花乐 — 开发计划 (Development Plan)

> **配套文档**：[PRD.md](./PRD.md)
> **文档版本**：v0.2
> **最后更新**：2026-07-19
> **目标读者**：项目作者本人 + 协助开发的 AI Agent

---

## 0. 怎么用这份文档

- **Phase 1 是必须的**，第 1 周内出能跑的版本（哪怕只支持一个账户、几个类别，能记一笔、看列表就行）
- **Phase 2-5 按需推进**，每个 phase 独立可演示
- 每个 task 都有明确的**产出**和**验收标准**，做完一个勾一个
- 所有 task 都标了**预计耗时**（按 1 人 + AI 辅助开发估算）

---

## 1. 全局约束（不可变项）

| 项 | 值 | 备注 |
|----|----|----|
| 语言 | Kotlin 2.0+ | K2 编译器 |
| UI | Jetpack Compose | Material 3 |
| compileSdk | 35 | Android 15 |
| minSdk | 24 | Android 7.0 |
| targetSdk | 35 | Android 15 |
| Java/Kotlin Target | JVM 17 | AGP 8.7 默认 |
| 包名 | `com.biehuale.app` | 不可改 |
| 数据库 schema version | 从 1 开始 | 每次升级 +1 |
| 金额单位 | 分（Long） | 永不用 Double |
| 时间单位 | epoch millis (Long) | 永不用 Date 直存 |
| 备份格式 | JSON v1 | schema 不可破坏性升级 |
| Git 仓库 | `D:\BieHuaLe` 当前目录 | 首次 commit 立刻建 |

---

## 2. 仓库初始化（必须最先做）

### 2.1 本地仓库

```powershell
cd D:\BieHuaLe
git init
git config user.name "DELL"          # 用你的
git config user.email "you@example.com"  # 用你的
git checkout -b main
```

### 2.2 初始目录结构

```
D:\BieHuaLe\
├── docs\
│   ├── PRD.md
│   └── DEV_PLAN.md
├── .gitignore
├── README.md                          # 写一句"别花乐 — 个人记账 App"
└── (Phase 1 起会生成 android/ 子目录)
```

### 2.3 `.gitignore`（关键项）

```gitignore
# Android
*.iml
.gradle/
local.properties
.idea/
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.ap_
*.dex
*.class
bin/
gen/
out/
release/

# OS
Thumbs.db
.DS_Store

# Keys
*.jks
*.keystore
key.properties
```

### 2.4 第一次 commit

```powershell
git add .gitignore docs\ README.md
git commit -m "chore: init repo with docs"
```

---

## 3. 开发工作流

### 3.1 命名规范

| 类别 | 规则 | 示例 |
|------|------|------|
| 包名 | 全小写、点分隔 | `com.biehuale.app.data.db` |
| 类名 | UpperCamelCase | `BillViewModel` |
| 函数/属性 | lowerCamelCase | `getCurrentBalance` |
| 常量 | UPPER_SNAKE | `MAX_DESCRIPTION_LENGTH = 200` |
| Compose Composable | UpperCamelCase | `BillScreen()` |
| 数据库表名 | snake_case 复数 | `transactions` |
| 数据库字段 | snake_case | `occurred_at` |
| 资源 id | snake_case | `ic_category_food` |
| Compose 测试 tag | 描述性 snake | `Modifier.testTag("amount_input")` |

### 3.2 Git commit 规范（Conventional Commits）

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(record): add transfer mode` |
| `fix` | 修 bug | `fix(bill): monthly total wrong when transfer included` |
| `refactor` | 重构（无功能变化） | `refactor(db): extract transaction dao` |
| `test` | 加测试 | `test(transfer): add balance calc tests` |
| `docs` | 文档 | `docs: update dev plan` |
| `chore` | 构建/工具/杂事 | `chore: bump compose-bom to 2024.10.00` |
| `style` | 格式 | `style: format ktlint` |
| `perf` | 性能 | `perf(bill): add index on occurred_at` |

**subject 规则**：
- 50 字符以内
- 中文或英文都行，但**一个仓库统一一种**
- 不用句号结尾

**branch 命名**（可选，分 phase 切也行）：
- `feat/<phase>-<short-desc>` 如 `feat/phase1-record-screen`
- 直接在 main 上推也行（个人项目，不必太严格）

### 3.3 分支策略

- **简单方案**：main 分支直接推
- **进阶方案**：`main` + `feat/xxx` + PR（不强制）
- 个人项目**推荐简单方案**，commit message 足够追溯

### 3.4 测试规范

| 项 | 标准 |
|----|------|
| ViewModel 单测 | 必须有，JUnit5 + MockK + Turbine |
| Repository 单测 | 必须有，in-memory Room |
| 转账业务规则 | **100% 覆盖**（最容易出 bug） |
| Compose UI 测试 | **不强求**，但关键路径建议有 |
| 备份导入 | **必须有测试**（含异常路径） |

### 3.5 Code style

- **ktlint**（Google 官方风格）
- **Android Studio 默认 import 排序**
- 提交前 IDE 自动 format

### 3.6 必装工具

| 工具 | 用途 | 备注 |
|------|------|------|
| Android Studio Ladybug (2024.2) 或更新 | 主 IDE | 推荐最新稳定版；最低 Ladybug |
| JDK 17 | 构建 | Android Studio 自带 |
| Android SDK 35 | 编译 | 通过 SDK Manager 装 |
| Git | 版本控制 | 已有 |
| GitHub Copilot / Cursor | AI 辅助 | 可选但强烈推荐 |

---

## 4. Phase 1 — 骨架（必须，1-3 天）

**目标**：能在自己手机上装上 App，记一笔，看列表。

**进入条件**：仓库已初始化，PRD 已对齐。

**退出条件（Demo 1）**：
- ✅ Android Studio 编译通过
- ✅ APK 能在自己手机上装上、启动
- ✅ 创建一个账户（如"现金"）
- ✅ 选类别、输金额、点保存
- ✅ 跳到账单 Tab 看到刚记的那笔

### Task 1.1 — 项目初始化（0.5h）

**做什么**：
1. Android Studio → New Project → Empty Activity
2. 配置 Package name = `com.biehuale.app`
3. Language = Kotlin / Min SDK 24 / Compile SDK 35
4. 选择"Compose"模板
5. 项目放在 `D:\BieHuaLe\android\` 子目录（避免污染根目录）

**产出**：可在 Android Studio 编译运行的空 Compose 项目。

**验收**：
- `gradle build` 成功
- `MainActivity` 显示"Hello Android"
- 在手机上能跑

**注意**：
- 不要选"Bumblebee"之前的模板（API 配置老）
- 不要选"View-based Activity"——我们要纯 Compose
- 生成的 `namespace` 改为 `com.biehuale.app`

---

### Task 1.2 — Gradle 依赖与版本对齐（0.5h）

**做什么**：按 PRD §8.3 写 `libs.versions.toml` 和 `build.gradle.kts`。

**关键依赖**（以 `android/gradle/libs.versions.toml` 为准）：
- Compose BOM 2024.10.00
- Kotlin 2.0.21
- AGP 8.7.0
- Hilt 2.52
- Room 2.6.1
- Navigation Compose 2.8.5
- DataStore 1.1.1
- kotlinx-serialization 1.7.3

> 升级依赖只改 toml；文档本节随后同步。
>
> **v0.2 实装变更**：Vico 未引入（API 不稳定），用纯 Compose Canvas 自绘饼图 + 趋势图（`CategoryPieChart.kt` / `DailyLineChart.kt`）。

**验收**：
- 全部依赖可解析
- `gradle build` 通过
- 不出现版本冲突警告

---

### Task 1.3 — 主题与 Material 3（1h）

**做什么**：
- `ui/theme/Color.kt`：定义 brand 色板（青绿色 fallback）
- `ui/theme/Theme.kt`：实现 Material 3 主题 + Dynamic Color
- `ui/theme/Type.kt`：Typography（金额用等宽字体）
- `MainActivity` 用 `BieHuaLeTheme { ... }` 包起来

**关键代码**：
```kotlin
@Composable
fun BieHuaLeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

**验收**：
- 切换系统深色模式，App 跟切
- Android 11 设备能看到 fallback 青绿色

---

### Task 1.4 — Hilt 集成（0.5h）

**做什么**：
- Application class 加 `@HiltAndroidApp`
- `MainActivity` 加 `@AndroidEntryPoint`
- 测试 `@Inject` 一个简单对象

**验收**：
- Hilt 代码生成成功
- 编译通过

---

### Task 1.5 — Room 数据库骨架（1.5h）

**做什么**：
- 定义 3 个 Entity（`AccountEntity`, `CategoryEntity`, `TransactionEntity`）—— 字段按 PRD §5.2
- 定义 3 个 DAO（接口，空方法先）
- `AppDatabase` 单例
- 第一个 Migration 占位

**关键约定**：
- Entity 全部用 `Long` 主键自增
- 字段名 snake_case（用 `@ColumnInfo`）
- 软删除字段 `deleted_at` / `is_archived` 写进 Entity
- Room 的 `Date` 转换器**不写**——用 Long 直存

**验收**：
- `AppDatabase` 编译通过
- 能通过 `Room.databaseBuilder` 构造实例

---

### Task 1.6 — 内置类别 seed（1h）

**做什么**：
- `data/seed/DefaultCategories.kt` 写死 15 个内置类别（10 支出 + 5 收入）
- AppDatabase 首次创建时插入
- 用 `RoomDatabase.Callback.onCreate`

**内置类别清单**：

| name | type | icon | color |
|------|------|------|-------|
| 餐饮 | EXPENSE | restaurant | #FF5722 |
| 交通 | EXPENSE | directions_bus | #2196F3 |
| 购物 | EXPENSE | shopping_cart | #9C27B0 |
| 住房 | EXPENSE | home | #795548 |
| 娱乐 | EXPENSE | sports_esports | #E91E63 |
| 医疗 | EXPENSE | medical_services | #F44336 |
| 教育 | EXPENSE | school | #3F51B5 |
| 通讯 | EXPENSE | phone | #00BCD4 |
| 护肤 | EXPENSE | spa | #FF4081 |
| 其他 | EXPENSE | more_horiz | #9E9E9E |
| 工资 | INCOME | payments | #4CAF50 |
| 奖金 | INCOME | redeem | #8BC34A |
| 理财 | INCOME | trending_up | #009688 |
| 零钱 | INCOME | savings | #66BB6A |
| 其他 | INCOME | more_horiz | #9E9E9E |

**注意**：
- 支出"其他"和收入"其他"是两个独立记录（id 不同）
- `is_builtin = 1`
- `sort_order` 按表填写

**验收**：
- 删除 App 重装，首启后 `categories` 表有 15 条
- 类别 id 稳定（每次重装都从 1 开始）

---

### Task 1.7 — Navigation 3 Tab 框架（1.5h）

**做什么**：
- `MainActivity` 用 `Scaffold` + `NavigationBar` + `NavHost`
- 3 个 Tab：账单 / 记账 / 设置
- 占位 Composable：每个 Tab 一个空 `Text("账单")` 等
- 底部 Tab 永远可见
- 当前 Tab 高亮

**验收**：
- 切 Tab 不重建 Activity（状态保留）
- 切 Tab 无明显卡顿

---

### Task 1.8 — 记账 Tab 基础 UI（3h）

**做什么**：
- 顶部 Tab：支出 / 收入（转账 Phase 2 再做）
- 金额大字号显示（默认 `¥0.00`）
- 自定义数字键盘（0-9 + . + ⌫ + 完成）
- 类别网格（2-3 列）
- 账户下拉（暂时只显示"现金"占位）
- 时间选择（暂时默认当前时间）
- 说明输入框（单行，可空）
- 保存按钮

**自定义数字键盘**：
- `Modifier.testTag("keypad_<digit>")`
- 输入"5" → 显示 "5" → 再输入 "0" → 显示 "50" → 输入 "." → "50." → 输入 "5" → "50.5"
- 自动补零：输 "5.5" → 显示 "¥5.50"
- 长按 ⌫ 清空

**金额输入组件**：
- 抽出来 `MoneyInput.kt`，无状态、纯 UI
- 内部维护 String 状态，对外暴露 `Long` 分

**类别网格**：
- 2 列 Grid，每个类别一个 Card
- 当前选中高亮
- 类别不够时滚动

**验收**：
- 点数字键盘金额正确显示
- 点类别能切换
- 点保存能调用 ViewModel

---

### Task 1.9 — TransactionRepository + RecordViewModel（2h）

**做什么**：
- `TransactionRepository`（CRUD 基本方法）
- `RecordViewModel`（StateFlow<RecordUiState>）
- RecordUiState：amount, type, selectedCategoryId, selectedAccountId, description, occurredAt
- 暴露 `save()` 方法

**State 模式**：
```kotlin
data class RecordUiState(
    val mode: RecordMode = RecordMode.EXPENSE,
    val amount: Long = 0L,             // 分
    val amountDisplay: String = "0.00",// 用于显示
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
```

**验收**：
- ViewModel 有单测
- save 后账真的进数据库

---

### Task 1.10 — 记账 Tab 端到端（1h）

**做什么**：
- 联调：选类别 + 输金额 + 点保存 → Repository.insert() → 跳账单 Tab
- "保存成功"提示用 Snackbar

**注意**：
- 第一次保存时如果账户是空的，要先跳"账户管理"建一个——简单粗暴做"如果账户列表空，提示去设置 Tab 建"
- 类别也类似处理

**验收**：
- 完整流程跑通
- 出错时不崩

---

### Task 1.11 — 账单 Tab 列表（2h）

**做什么**：
- 顶部占位"本月已花"（Phase 3 实装）
- 流水列表：按月分组、occurredAt 倒序
- 每行：类别图标 + 类别名 + 说明 + 金额 + 时间
- 用 `LazyColumn` + `items`
- 收入显示绿色、支出显示红色
- 暂时不支持点击

**验收**：
- 记完一笔回到账单 Tab 能看到
- 滑动流畅

---

### Task 1.12 — 第一次跑通（0.5h）

**做什么**：
- 连接真机
- 编译、装包、运行
- 走一遍：建账户 → 记一笔 → 看列表

**验收**：
- **Demo 1 通过**：能装、能记、能看
- Commit：`feat(phase1): basic record and bill list`

**这是 Phase 1 出口。** 🎉

---

## 5. Phase 2 — 完善记账与多账户（1-3 天）

**目标**：多账户完整闭环 + 转账 + 流水编辑。

**进入条件**：Phase 1 出口达成。

**退出条件（Demo 2）**：
- ✅ 能建多个账户，每个账户有余额
- ✅ 转账：微信→银行卡 ¥500，微信 -500、银行卡 +500、本月总支出不变
- ✅ 流水详情能编辑、删除（软删除）
- ✅ 类别能改名字、改图标颜色、删除

### Task 2.1 — 账户管理（2h）

**做什么**：
- 设置 Tab → 账户管理 → 列表
- 新建账户：名字 + 初始余额 + 图标 + 颜色
- 编辑账户：同表单
- 归档账户：软删除，列表中隐藏

**UI**：
- 列表项：图标 + 名字 + 当前余额
- 右下角「+」FAB 新建（仅账户/类别管理页；记账入口仍用独立 Tab，见 PRD §4.2）
- 长按 → 归档/编辑

**验收**：
- 至少 2 个账户能并存
- 当前余额 = 初始余额 + 收入 - 支出 **- 转出转账 + 转入转账**（与 Task 2.3 / PRD §6.1 一致）

---

### Task 2.2 — 类别管理（2h）

**做什么**：
- 设置 Tab → 类别管理 → 列表（分支出/收入两组）
- 编辑类别：改名、改图标、改颜色
- 归档自定义类别（`is_archived = 1`，不进流水回收站）
- 内置类别不能硬删（隐藏删除按钮）
- "重置默认类别"：恢复全部内置类别的默认 name/icon/color/sort_order，并取消归档；不删自建类别、不改历史 `category_id`（见 PRD §6.4）

**验收**：
- 内置类别改名字后 UI 立即反映
- 归档后历史账还能看，显示「（已归档）」
- 重置后内置类别恢复 seed 默认值

---

### Task 2.3 — 余额计算（2h）

**做什么**：
- `AccountRepository.getBalance(accountId): Long`
- SQL 聚合查询：
  ```sql
  SELECT
    initial_balance
    + COALESCE((SELECT SUM(amount) FROM transactions
        WHERE account_id = :id AND type = 'INCOME' AND deleted_at IS NULL), 0)
    - COALESCE((SELECT SUM(amount) FROM transactions
        WHERE account_id = :id AND type = 'EXPENSE' AND deleted_at IS NULL), 0)
    - COALESCE((SELECT SUM(amount) FROM transactions
        WHERE account_id = :id AND type = 'TRANSFER' AND deleted_at IS NULL), 0)
    + COALESCE((SELECT SUM(amount) FROM transactions
        WHERE to_account_id = :id AND type = 'TRANSFER' AND deleted_at IS NULL), 0)
  FROM accounts WHERE id = :id
  ```
- 单元测试覆盖所有场景

**验收**：
- 100% 测试覆盖
- 复杂场景：转账后再转账、删除后回退、撤销恢复

---

### Task 2.4 — 转账 UI（2h）

**做什么**：
- 记账 Tab 顶部 Tab 增加"转账"
- 转账模式隐藏类别选择
- 显示两个账户选择器：转出 + 转入
- 转出 = 转入时禁用保存并提示
- 显示"金额 → 转出账户 -X → 转入账户 +X"提示
- 保存逻辑：插一条 TRANSFER 记录；金额必须 > 0

**验收**：
- 转账保存成功
- 同账户互转被拒绝
- 列表显示「微信 → 银行卡 ¥500」
- 两侧余额按 PRD §6.1 变化，本月总支出不变

---

### Task 2.5 — 流水详情 + 编辑（2h）

**做什么**：
- 账单 Tab 行点击 → 详情页
- 详情页：完整字段展示
- "编辑"按钮 → 复用 RecordScreen（传 transactionId）
- "删除"按钮 → 软删除（弹确认）

**编辑逻辑**：
- 复用 RecordViewModel
- 加 `loadTransaction(id)` 方法
- 区分新建/编辑模式

**验收**：
- 编辑后列表立即反映
- 删除后流水从列表消失（实际软删除）

---

### Task 2.6 — Phase 2 收尾（0.5h）

**验收**：
- **Demo 2 通过**
- 关键 bug 列表清空
- Commit：`feat(phase2): multi-account, transfer, edit`

---

## 6. Phase 3 — 统计与筛选（2-4 天）

**目标**：能看到"我这个月都花哪了"。

**进入条件**：Phase 2 出口达成。

**退出条件（Demo 3）**：
- ✅ 账单 Tab 顶部有「本月已花」卡片 + 支出占收入比例条
- ✅ 分类饼图
- ✅ 趋势折线图（按日/周/月）
- ✅ 搜索能按说明找
- ✅ 筛选能按账户/类别/类型

### Task 3.1 — 本月已花卡片（1h）

**做什么**：
- 账单 Tab 顶部
- 大数字（本月已花）+ 收入 + 结余
- **支出占收入比例条**：`expense / income`；收入为 0 时隐藏；超过 100% 显示满条 + 百分比文案
- 月份切换器（左右箭头）

**数据**：
- ViewModel 暴露 `monthlySummary: StateFlow<MonthlySummary>`
- SQL 聚合按月查（转账不计入支出/收入）

**验收**：
- 切月份数字变
- 有收入时比例条正确；无收入时不显示比例条

---

### Task 3.2 — 分类饼图（2h）

**做什么**：
- 用 Compose Canvas 自绘饼图（`CategoryPieChart.kt`，**v0.1 实装**：放弃 Vico）
- 数据：本月支出按类别聚合
- 点击饼图扇区 → 跳到该类别本月流水
- 图例：类别名 + 金额 + 占比

**验收**：
- 饼图渲染正常
- 数据对得上流水

---

### Task 3.3 — 趋势折线图（3h）

**做什么**：
- 按日聚合（默认）显示本月每日支出
- 切换：按周 / 按月
- 用 Compose Canvas 自绘（`DailyLineChart.kt`，**v0.1 实装**：放弃 Vico LineChart）

**验收**：
- 折线能看出"今天花得特别多"

---

### Task 3.4 — 时间筛选（2h）

**做什么**：
- 月份切换器（已有）+ 自定义区间（起始日 + 结束日 DatePicker）
- 「按周」**不是**列表时间预设，仅作为趋势图粒度（Task 3.3）
- 影响账单列表和统计（饼图、比例条、趋势）

**验收**：
- 选 6/1 - 6/15 只显示这段时间的列表与统计

---

### Task 3.5 — 搜索（1.5h）

**做什么**：
- 账单 Tab 顶部搜索图标
- 点开搜索框（顶替 Tab 的位置）
- 实时过滤说明字段
- 大小写不敏感
- 搜索时同时影响列表

**验收**：
- 输"星巴克"立即过滤
- 清空搜索恢复全部

---

### Task 3.6 — 筛选器（2h）

**做什么**：
- 账单 Tab 右上角筛选图标
- 弹 BottomSheet：账户多选 / 类别多选 / 类型多选（支出/收入/转账）
- "应用"按钮
- 筛选状态保留在 ViewModel

**验收**：
- 多选组合正确

---

### Task 3.7 — Phase 3 收尾（0.5h）

**验收**：
- **Demo 3 通过**
- Commit：`feat(phase3): stats, search, filter`

---

## 7. Phase 4 — 备份与打磨（1-3 天）

**目标**：完整可用，可分享 APK。

**进入条件**：Phase 3 出口达成。

**退出条件（Demo 4）**：
- ✅ 能导出 JSON 到本地
- ✅ 能从 JSON 导入（合并）
- ✅ 软删除 30 天清理
- ✅ 深色模式手动切换
- ✅ 主要空状态/错误状态有提示

### Task 4.1 — JSON Schema 定义（1h）

**做什么**：
- 用 `kotlinx.serialization` 定义 DTO
- `BackupDto`, `AccountDto`, `CategoryDto`, `TransactionDto`
- 写在 `data/backup/BackupDto.kt`

**关键字段**：
```kotlin
@Serializable
data class BackupDto(
    val schemaVersion: Int = 1,
    val appVersion: String,
    val exportedAt: String,  // ISO 8601
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>
)
```

**验收**：
- 能序列化、能反序列化

---

### Task 4.2 — SAF 导出（2h）

**做什么**：
- 设置 Tab → 数据 → 导出
- 用 `ActivityResultContracts.CreateDocument("application/json")`
- 文件名：`别花乐_yyyy-MM-dd_HHmm.json`
- 写入 SAF Uri

**注意**：
- 不需要任何存储权限
- Android 14+ 部分写入权限（系统会处理）

**验收**：
- 文件真在那个目录创建
- 用文本编辑器打开 JSON 合法

---

### Task 4.3 — SAF 导入（3h）

**做什么**：
- 设置 Tab → 数据 → 导入
- 用 `ActivityResultContracts.OpenDocument()`
- 读 JSON → 解析 → 弹预览 Dialog（"将导入 N 个账户、M 个类别、K 笔账"）
- 用户确认 → 合并模式插入

**关键校验**：
- `schemaVersion` 匹配（不匹配弹错，含过高/过低）
- 解析失败弹错
- 整笔事务，失败回滚
- 余额为运行时聚合，导入后**无需**单独重算步骤

**验收**：
- 导入成功后数据全在，账户余额查询正确
- 错误情况不破坏现有数据
- 单元测试覆盖解析失败、版本不匹配、空文件

---

### Task 4.4 — 回收站（2h）

**做什么**：
- 设置 Tab → 回收站
- 列表：软删除的**流水**（`deleted_at IS NOT NULL`）；账户/类别归档不在此
- "恢复"按钮 → `UPDATE ... SET deleted_at = NULL`
- "永久删除"按钮 → 真删
- "清空回收站"按钮

**验收**：
- 删了能恢复
- 30 天前的自动清理（看 T4.5）

---

### Task 4.5 — WorkManager 30 天清理（2h）

**做什么**：
- `CleanupWorker`：`DELETE FROM transactions WHERE deleted_at < ?`（30 天前）
- 每天跑一次
- App 启动时也跑一次兜底
- `HiltWorker` 注入 Repository

**验收**：
- 注入软删除的账超过 30 天的，下次启动后清掉
- WorkManager 约束（不在充电时不跑等）暂时不做

---

### Task 4.6 — 深色模式手动切换（1.5h）

**做什么**：
- 设置 Tab → 外观 → 主题
- 三选一：浅色 / 深色 / 跟随系统
- 存 `DataStore<Preferences>`
- `MainActivity` 读出来后传给 `BieHuaLeTheme`

**验收**：
- 切浅色 App 立即变白
- 切深色立即变黑
- 选"跟随系统"跟系统走

---

### Task 4.7 — 空状态 / 错误状态 / 加载状态（2h）

**做什么**：
- 空状态组件：`EmptyState(icon, title, subtitle, action?)`
- 账单 Tab 空状态："还没有记账，点 [去记账] 开始吧"
- 设置 Tab 账户空："还没有账户，点 [新建账户]"
- 加载状态：`CircularProgressIndicator` 居中
- 错误状态：Snackbar（数据库/解析/导入失败等；本 App **无网络**，不要写「网络错误」文案）

**验收**：
- 删除所有账后，账单 Tab 显示空状态
- 导入失败等错误有 Snackbar

---

### Task 4.8 — Phase 4 收尾（0.5h）

**验收**：
- **Demo 4 通过**
- 完整 demo 走一遍：建账户 → 记几笔 → 看统计 → 导出 → 卸载重装 → 导入 → 数据回来
- Commit：`feat(phase4): backup, restore, recycle bin, dark mode`

---

## 8. Phase 5 — 发布准备（可选，1-2 天）

**目标**：可分享、APK 优化、测试补全。

**进入条件**：Demo 4 通过。

### Task 5.1 — 应用图标（1h）

**做什么**：
- 用 Android Studio Image Asset Studio
- 选个简单风格（"钱包 + 文字'别'"）
- 自适应图标 + 旧版图标都生成
- 不满意再找设计师

**验收**：
- 桌面图标清晰
- 各 launcher 显示正常

---

### Task 5.2 — 启动屏（0.5h）

**做什么**：
- 用 `androidx.core:core-splashscreen`
- 显示 App 图标 + 应用名
- 200ms 后切主界面

**验收**：
- 启动不闪白屏

---

### Task 5.3 — ProGuard / R8 配置（1h）

**做什么**：
- release build 开启 R8
- 写 `proguard-rules.pro`：
  - kotlinx-serialization keep rules
  - Room keep rules
  - Hilt keep rules
- 测一遍 release APK 功能正常

**验收**：
- release APK 体积小
- 不崩

---

### Task 5.4 — 多 ABI APK 拆分（0.5h）

**做什么**：
- `splits.abi` 配置 arm64 / armeabi-v7a / x86_64
- 朋友手机一般是 arm64，APK 能更小

**验收**：
- 生成多个 ABI 的 APK

---

### Task 5.5 — 单元测试补全（2h）

**做什么**：
- 已有：转账、导入
- 补充：
  - 余额计算边界
  - 软删除/恢复
  - 统计聚合（按月、按周、按日）
  - 搜索过滤
  - 筛选组合

**目标覆盖率**：
- Repository 层 ≥ 80%
- ViewModel 层 ≥ 70%
- 工具类 ≥ 90%

---

### Task 5.6 — 集成测试（1h）

**做什么**：
- Room Migration 测试（占位，因为现在 v1）
- 端到端"建账户 → 记一笔 → 看列表" 自动化

---

### Task 5.7 — 性能优化（1h）

**做什么**：
- LazyColumn key 稳定
- 避免在 Composable 里做 IO
- 启动时只 seed 不查
- 大量账（1000+）下滑动测试

---

### Task 5.8 — 文档与发布说明（1h）

**做什么**：
- 写 `README.md`：项目介绍、构建命令、功能截图
- 写 `CHANGELOG.md`
- 写 `RELEASE_NOTES_v0.1.0.md`

---

## 9. 关键里程碑 / Demo 检查点

| 里程碑 | 阶段 | 验证方法 | 验收标志 |
|--------|------|----------|----------|
| **M1: 骨架能跑** | Phase 1 末 | 装到手机 | 能记一笔、看一眼 |
| **M2: 多账户闭环** | Phase 2 末 | demo | 完整转账 + 余额对 |
| **M3: 统计可用** | Phase 3 末 | demo | 月饼图 + 趋势 + 搜索 |
| **M4: 完整可用** | Phase 4 末 | 卸载重装 | 导出 → 卸载 → 装 → 导入 → 数据在 |
| **M5: 可发布** | Phase 5 末 | 朋友试用 | 朋友能装、能用、给反馈 |
| **M6: 对齐 PRD** | v0.2 修复末（2026-07-19） | 审查报告 | 底部 Tab + AllTransactionsScreen + enum 改造 + Migration v1→v2（详见 [`AUDIT_REPORT.md`](./AUDIT_REPORT.md) / [`AUDIT_FIX_REPORT.md`](./AUDIT_FIX_REPORT.md)） |

---

## 10. 风险与回退

| 风险 | 触发条件 | 缓解 / 回退 |
|------|----------|------------|
| ~~Vico 图表 API 变~~ | ~~升级到 Vico 3.x~~ | ~~锁版本到 2.0.0；或换 MPAndroidChart~~ | **v0.1 实装已规避**：未引入 Vico，用纯 Compose Canvas 自绘 |
| Room 追 alpha 不稳定 | 编译/运行错 | 保持 toml 中的 2.6.1 稳定版 |
| 朋友手机 Android < 7 | 装不上 | minSdk 已经是 24，正常不会发生 |
| 软删除 30 天清理漏跑 | WorkManager 没启动 | App 启动时也做兜底查询 |
| 备份 JSON 升级 | 加新字段 | schemaVersion +1；旧 App 拒绝新版本；新 App 可择机做向后兼容解析 |
| Room schema 升级 v1→v2 | v0.1 → v0.2 升级时数据丢失风险 | **v0.2 实装已缓解**：`MIGRATION_1_2` 注册 + `MigrationTest` 验证数据保留 |
| Compose BOM 与文档不一致 | 误导升级 | **以 `libs.versions.toml` 为准**（当前 `2024.10.00`） |
| 用户中途要改设计 | Phase 1 后 | 在 Phase 1 出口停下确认，再决定 |

---

## 11. 工具清单

### IDE 与构建

| 工具 | 版本 | 用途 |
|------|------|------|
| Android Studio | Ladybug (2024.2) 或更新 | 主 IDE |
| Gradle | 8.10+ | 构建 |
| JDK | 17 | 编译 |

### 命令速查

```powershell
# 第一次跑
cd D:\BieHuaLe\android
.\gradlew assembleDebug
# APK 在 app\build\outputs\apk\debug\app-debug.apk

# 装到手机
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 单测
.\gradlew test

# Lint
.\gradlew lint

# 清理
.\gradlew clean
```

### 依赖检查

```powershell
# 看依赖树
.\gradlew app:dependencies

# 看可更新版本
.\gradlew dependencyUpdates
```

---

## 12. 给开发者的速查表

### 12.1 关键文件位置

```
D:\BieHuaLe\
├── android\                         # Android Studio 项目根
│   ├── app\
│   │   ├── build.gradle.kts
│   │   └── src\main\java\com\biehuale\app\
│   │       ├── BieHuaLeApp.kt
│   │       ├── MainActivity.kt
│   │       ├── data\
│   │       ├── ui\
│   │       └── di\
│   ├── build.gradle.kts             # 项目级
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── gradle\libs.versions.toml    # 版本目录
├── docs\
│   ├── PRD.md
│   └── DEV_PLAN.md
├── .gitignore
└── README.md
```

### 12.2 关键 SQL 速记

```sql
-- 本月已花
SELECT SUM(amount) FROM transactions
WHERE type = 'EXPENSE'
  AND occurred_at >= :monthStart AND occurred_at < :nextMonthStart
  AND deleted_at IS NULL;

-- 账户余额
-- 见 Task 2.3

-- 分类聚合
SELECT category_id, SUM(amount) FROM transactions
WHERE type = 'EXPENSE'
  AND occurred_at >= :start AND occurred_at < :end
  AND deleted_at IS NULL
GROUP BY category_id;
```

### 12.3 关键 Composable 速记

```kotlin
// 金额格式化（分 → 展示；运算仍用 Long，勿用 Double 做账）
fun Long.toMoneyString(): String =
    NumberFormat.getCurrencyInstance(Locale.CHINA).format(this / 100.0)

// 颜色 hex 解析
fun parseColor(hex: String): Int = android.graphics.Color.parseColor(hex)

// 时间格式化（存储仍用 epoch millis；展示可用 java.time 或 kotlinx-datetime）
fun Long.toDateString(): String {
    val instant = java.time.Instant.ofEpochMilli(this)
    val zdt = instant.atZone(java.time.ZoneId.systemDefault())
    return zdt.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))
}
```

### 12.4 调试技巧

- **数据库查看**：Android Studio App Inspection → Database Inspector
- **Compose 预览**：写 `Preview` 函数 + 多种状态
- **慢 UI**：Layout Inspector + Composition Tracing
- **崩溃日志**：`adb logcat | Select-String "AndroidRuntime"`

---

## 13. 决策日志（开发计划相关）

| # | 决策 | 理由 |
|---|------|------|
| D1 | 项目放在 `D:\BieHuaLe\android\` 子目录 | 避免污染根目录，docs 留在根 |
| D2 | Phase 1 必须 1-3 天出 demo | 早用上才能提真实需求 |
| D3 | commit 用 Conventional Commits | 统一格式，AI 友好 |
| D4 | 不用 commitlint | 个人项目杀鸡用牛刀 |
| D5 | 关键业务 100% 单测覆盖 | 转账最易错；导入/导出需校验 |
| D6 | 软删除 30 天 | 仅流水；WorkManager 兜底 + 启动兜底 |
| D7 | ProGuard 在 Phase 5 才开 | 早期 R8 报错难调 |
| D8 | 不强制 PR | 个人项目 main 直接推 |
| D9 | 内置支出第 9 类名「护肤」 | 与 PRD §5.2 对齐（不作「美容」） |
| D10 | Compose BOM `2024.10.00` + Room `2.6.1` | 骨架用稳定版；以 toml 为准 |
| D11 | 比例条 = 支出/收入 | 无预算；收入为 0 时隐藏 |
| D12 | 账户管理可用 FAB | 记账入口仍用 Tab，不与 PRD 冲突 |

---

**文档结束。** Phase 1 是核心，骨架搭起来后面就顺了。
