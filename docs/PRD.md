# 别花乐 (BieHuaLe) — 产品需求文档 (PRD)

> **项目代号**：别花乐
> **应用名**：别花乐
> **包名**：`com.biehuale.app`
> **文档版本**：v0.2
> **最后更新**：2026-07-19
> **文档状态**：已对齐（含文档审阅修正），待开发启动；§13 开放问题开发中再定

---

## 1. 项目概述

### 1.1 一句话定义

一款**单用户、本地优先**的安卓记账 App，**3 秒记一笔**，清晰的统计与导出，**不上云、不登录、不打扰**。

### 1.2 项目目标

- **自己用得爽**：打开 → 记一笔 → 看本月花了多少；单笔记账目标 **< 3 秒**，完整「打开→记→看」路径不超过 10 秒
- **能分享给朋友**：APK 直接安装，无登录注册，本地数据自带导入导出
- **代码健全**：MVVM + Repository + Room，单元测试覆盖核心业务规则，未来好维护好扩展

### 1.3 目标用户

| 角色 | 描述 | 优先级 |
|------|------|--------|
| **主用户（自己）** | 项目发起人，单设备使用，对数据自主可控敏感 | P0 |
| **朋友（次要）** | 拿到 APK 后尝鲜试用，无需注册、无门槛 | P1 |

### 1.4 范围边界

| 类别 | 内容 |
|------|------|
| **平台** | 仅 Android（Compose 原生），暂不考虑 iOS / 桌面 / Web |
| **数据** | 本地 Room / SQLite；不上云；不同步；不收集 |
| **用户系统** | 无登录、无注册、单用户 |
| **货币** | 仅 CNY；MVP 数据模型**不**含 `currency` 字段，多币种留待日后 Migration |

---

## 2. 核心场景与用户故事

### 场景 A：日常消费后快速记一笔

> 我刚在便利店买了瓶水 5 块，掏出手机 → 点底部"记账"Tab → 选"餐饮" → 金额 5 → 保存。
> **整个过程 < 3 秒。**

### 场景 B：月底看本月花销

> 月末晚上我打开 App → 切到"账单"Tab → 顶部看到「本月已花 ¥2,341」+ 支出占收入比例条
> → 下面饼图显示「餐饮 38% / 购物 22% / 交通 15% / ...」→ 一眼看到奶茶花了 280。

### 场景 C：朋友试用

> 我把 `biehuale.apk` 发给朋友 → 他装上 → 默认空数据，自己手动记几笔。
> 如果我想给他"种子数据"演示：先导出 JSON 发给他，他在"设置"里导入。

### 场景 D：手机要重置/换新

> 我打开 App → 设置 → 备份导出 → 得到 `别花乐_2026-07-19_1812.json` → 微信发给自己。
> 新手机装上 App → 设置 → 备份导入 → 选文件 → 合并导入。

### 场景 E：账户间转账

> 我从微信转了 500 到银行卡 → 记账 Tab → 选"转账" → 选转出"微信" → 选转入"银行卡" → 金额 500 →（可选手续费，如 1）→ 保存。
> 无手续费：微信 −500，银行卡 +500；有手续费 1：微信 −500，银行卡 +499。本月总支出 **不变**（转账本金不算支出）。

---

## 3. 功能需求

### 3.1 MVP 必做（P0）

| # | 功能 | 描述 | 验收要点 |
|---|------|------|----------|
| F1 | 手动记账 | 收入 / 支出 / 转账 三类 | 必填：金额、账户、类别（转账除外）、发生时间；可选：说明 |
| F2 | 账户管理 | 多账户，用户自建 | 支持新增、改名、归档、设置初始余额 |
| F3 | 类别管理 | 内置 + 自定义 | 首次启动预置 15-20 个；用户可改名、删除、新增 |
| F4 | 账单列表 | 时间倒序 | 按月分组、按账户/类别筛选、搜索说明 |
| F5 | 搜索 | 关键词匹配说明字段 | 大小写不敏感、实时过滤 |
| F6 | 统计概览 | 本月已花 / 收入 / 结余 / **总资产** + 分类饼图 + 支出占收入比例条 | "账单" Tab 顶部；可切换月份；本月收入为 0 时不显示比例条；总资产 = 活跃账户余额合计（时点，与月份无关），可点看分户 |
| F7 | 时间筛选 | 按月 / 自定义区间 | 影响账单列表和统计；「按周」仅用于趋势图粒度，不作列表预设 |
| F8 | 备份导出 | JSON 文件 | 一键导出，文件名带时间戳 |
| F9 | 备份导入 | 从 JSON 恢复 | 合并模式，不覆盖现有数据；导入前预览 N 笔 |
| F10 | 深色模式 | 跟系统 + App 内手动切换 | 浅色 / 深色 / 跟随系统 三档 |
| F11 | 说明字段 | 每笔记账附文字描述 | 单行、≤200 字符、可为空 |
| F12 | 软删除 | 误删可恢复 | 仅流水；30 天回收站，30 天后清理 |
| F13 | 趋势折线图 | 支出按日 / 周 / 月聚合 | 账单 Tab 统计区；可切换粒度 |

### 3.2 不做（v2+ 候选）

| # | 功能 | 不做的理由 |
|---|------|-----------|
| N1 | 预算 | 用户明确不需要 |
| N2 | 自动记账（通知监听） | 维护成本高、隐私争议 |
| N3 | 定期账单 | 标准版范围外 |
| N4 | 多账本 | 单一用户的多个"本"用账户+筛选已能模拟 |
| N5 | 桌面小部件 | MVP 后视情况 |
| N6 | 云同步 | 隐私优先，不上云 |
| N7 | 多用户 / 登录 | 单用户项目 |
| N8 | 标签系统 | 类别 + 说明已能覆盖 |
| N9 | 指纹 / 应用锁 | 简单项目不引入额外权限 |
| N10 | 上架 Play Store | 个人分享用 APK 足够 |

---

## 4. 信息架构

### 4.1 页面结构（3 Tab + 详情页）

```
┌─────────────────────────────────────────────────┐
│  [账单]  [记账]  [设置]   ← 底部 Tab            │
├─────────────────────────────────────────────────┤
│                                                  │
│            当前 Tab 内容区域                     │
│                                                  │
└─────────────────────────────────────────────────┘
```

#### Tab 1：账单

- **顶部卡片**：本月已花 / 本月收入 / 本月结余（数字大）+ 月份切换器；次级「总资产」可点看分户余额
- **支出占收入比例条**：`本月支出 / 本月收入`（收入为 0 时隐藏；超过 100% 仍显示满条 + 百分比文案）
- **分类饼图**：本月支出按类别占比（可点击 → 该类别本月流水）
- **趋势折线图**：本月支出趋势（按日 / 周 / 月切换粒度）
- **最近流水**：最近 5 笔（点击 → 详情）
- **"查看全部"按钮** → 跳全部流水页（带筛选/搜索）
- **右上角筛选图标** → 弹筛选器（按账户/类别/类型）

#### Tab 2：记账

- **模式选择 Tab**：支出 / 收入 / 转账（顶部三选一）
- **金额输入**：大字号数字键盘，自动聚焦
- **类别选择**（转账不显示）：网格 / 列表
- **账户选择**：当前账户下拉，可改
- **转账模式特殊项**：转出账户 + 转入账户
- **说明输入**：单行文本框，可空，限 200 字
- **时间选择**：默认现在，可改（精确到分钟）
- **底部"保存"按钮**：主按钮

#### Tab 3：设置

- **账户管理**：账户列表 → 新增/编辑/归档
- **类别管理**：类别列表 → 新增/编辑/删除/重置默认
- **数据**：导出 / 导入 / 回收站
- **外观**：主题（浅色/深色/跟随系统）
- **关于**：版本号、GitHub、致谢

#### 详情页与子页

- **流水详情**：完整展示一笔账，可编辑 / 删除
- **编辑表单**：和记账 Tab 几乎一致
- **全部流水页**：按月分组列表 + 搜索 + 筛选（从账单 Tab「查看全部」进入）
- **回收站页**：仅流水软删除记录（从设置进入）

### 4.2 全局交互

- **底部 Tab 永远可见**——三主 Tab 内始终显示；进入详情/子页时仍保留底部 Tab（1 tap 回主流程）
- **记账入口不用 FAB**——用户明确要求把"记账"作为独立 Tab；设置内的列表页可用「+」FAB（如新建账户）
- **删除走软删除**——流水：长按/详情 → 删除 → 进回收站，30 天内可恢复；账户/类别用归档（`is_archived`），不进回收站

---

## 5. 数据模型

### 5.1 ER 关系图

```
┌──────────────┐         ┌──────────────────┐
│   accounts   │ 1     * │   transactions   │
│              │◄────────┤                  │
│  id (PK)     │         │  id (PK)         │
│  name        │         │  amount (分)     │
│  initial_    │         │  type (枚举)     │
│   balance    │         │  category_id ──┐ │
│  icon        │         │  account_id    │ │
│  color       │         │  to_account_id─┤ │
│  is_archived │         │  description  │ │
│  created_at  │         │  occurred_at  │ │
│  updated_at  │         │  created_at   │ │
└──────────────┘         │  updated_at   │ │
                         │  deleted_at   │ │
                         └──────────────┘ │
                                ▲           │
                                │           │
                         ┌──────────────┐  │
                         │  categories  │  │
                         │              │  │
                         │  id (PK)     │  │
                         │  name        │  │
                         │  icon        │  │
                         │  color       │  │
                         │  type        │  │
                         │  is_builtin  │  │
                         │  sort_order  │  │
                         │  is_archived │  │
                         │  created_at  │  │
                         │  updated_at  │  │
                         └──────────────┘
```

### 5.2 表结构

#### `accounts`（账户表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | INTEGER | ✅ | PK，自增 |
| `name` | TEXT | ✅ | 账户名（≤20 字） |
| `icon` | TEXT | ❌ | 图标 key（如 `wallet`, `wechat`, `alipay`） |
| `color` | TEXT | ❌ | 主题色 hex（如 `#07C160`） |
| `initial_balance` | INTEGER | ✅ | 初始余额（**分**），默认 0；Kotlin Entity 用 `Long` |
| `is_archived` | INTEGER | ✅ | 0/1，软删除标记 |
| `created_at` | INTEGER | ✅ | epoch millis |
| `updated_at` | INTEGER | ✅ | epoch millis |

**索引**：`is_archived`

#### `categories`（类别表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | INTEGER | ✅ | PK |
| `name` | TEXT | ✅ | 类别名（≤10 字） |
| `icon` | TEXT | ❌ | 图标 key |
| `color` | TEXT | ❌ | 主题色 hex |
| `type` | TEXT | ✅ | `INCOME` / `EXPENSE` |
| `is_builtin` | INTEGER | ✅ | 0/1，内置不可硬删（用户可"归档"，可重置默认） |
| `sort_order` | INTEGER | ✅ | 显示顺序，默认值 |
| `is_archived` | INTEGER | ✅ | 0/1 |
| `created_at` | INTEGER | ✅ | |
| `updated_at` | INTEGER | ✅ | |

**内置类别（首次启动 seed）**：

支出（10 个）：餐饮、交通、购物、住房、娱乐、医疗、教育、通讯、护肤、其他
收入（5 个）：工资、奖金、理财、零钱、其他

> 名称以本节为准；与 `DefaultCategories` seed 必须一致。

**索引**：`(type, is_archived)`

#### `transactions`（交易表，核心）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | INTEGER | ✅ | PK |
| `amount` | INTEGER | ✅ | 金额（**分**），永远正数；正负由 `type` 决定；Kotlin Entity 用 `Long` |
| `type` | TEXT | ✅ | `INCOME` / `EXPENSE` / `TRANSFER` |
| `category_id` | INTEGER | ❌ | FK → `categories.id`，转账为 null |
| `account_id` | INTEGER | ✅ | FK → `accounts.id`（转出账户） |
| `to_account_id` | INTEGER | ❌ | FK → `accounts.id`，仅 TRANSFER 用 |
| `fee` | INTEGER | ✅ | 转账手续费（**分**），默认 0；仅 TRANSFER 有意义，且 `0 ≤ fee < amount` |
| `description` | TEXT | ❌ | 说明，≤200 字符 |
| `occurred_at` | INTEGER | ✅ | 实际发生时间（epoch millis） |
| `created_at` | INTEGER | ✅ | |
| `updated_at` | INTEGER | ✅ | |
| `deleted_at` | INTEGER | ❌ | 软删除时间，30 天后清理 |

**索引**：
- `(occurred_at DESC)` — 列表查询
- `(account_id, occurred_at)` — 账户维度
- `(category_id, occurred_at)` — 类别维度
- `(deleted_at)` — 回收站清理

**外键**：
- `category_id` → `categories.id` `ON DELETE SET NULL`（类别归档不影响历史账）
- `account_id` → `accounts.id` `ON DELETE RESTRICT`（账户不能直接删，必须先归档+迁移历史账）

### 5.3 关键枚举

```kotlin
enum class TransactionType { INCOME, EXPENSE, TRANSFER }
enum class CategoryType   { INCOME, EXPENSE }
```

---

## 6. 业务规则

### 6.1 转账逻辑（**最容易出错的地方**）

转账是**特殊操作**：

1. UI 上和"支出/收入"分开（独立 Tab 或顶部 Tab 切换）
2. 数据库只插**一条**记录，`type = TRANSFER`，`amount` 为正数；可选 `fee`（手续费，分）
3. **转出账户 ≠ 转入账户**；相同则禁止保存并提示
4. 手续费语义（例：转 500、fee=1）：转出 −500，转入 +499；要求 `0 ≤ fee < amount`
5. 转账本金**不**影响本月"支出"和"收入"统计；手续费目前亦不计入支出（仅体现为总资产减少），但会影响：
   - 转出账户余额（−amount）
   - 转入账户余额（+(amount − fee)）
6. 转账记录在列表里显示「微信 → 银行卡  ¥500」
7. 余额计算（运行时，**不落库存余额字段**）：
   ```
   账户余额 = initial_balance
            + 该账户作为 account_id 的 INCOME 总额
            - 该账户作为 account_id 的 EXPENSE 总额
            - 该账户作为 account_id 的 TRANSFER.amount 总额
            + 该账户作为 to_account_id 的 TRANSFER.(amount − fee) 总额
   ```
   上述聚合均排除 `deleted_at IS NOT NULL` 的流水。

### 6.2 软删除与回收站

- 流水删除走软删除：设置 `deleted_at = now()`
- 列表查询默认 `WHERE deleted_at IS NULL`
- 回收站页：`WHERE deleted_at IS NOT NULL` 按 `deleted_at DESC`（**仅流水**）
- **30 天后自动清理**（用 `WorkManager` 每天检查一次；App 启动再兜底一次）
- 恢复：`UPDATE ... SET deleted_at = NULL`
- 账户/类别归档用 `is_archived = 1`，不进回收站，不影响历史账展示

### 6.3 货币与精度

- **存储单位：分（INTEGER / Kotlin Long）**——绝对不用浮点做金额运算
- 显示时用 `NumberFormat.getCurrencyInstance(Locale.CHINA)`（或等价分→元格式化），避免手写 `/ 100.0` 作为唯一路径
- 多币种：MVP **不做**，schema **不预留** `currency`；未来用 Migration 加字段
- 输入金额：UI 输入"5.50" → 内部存 550；最多两位小数；金额必须 **> 0**；上限 999,999,999.99 元（分值 ≤ 99_999_999_999L）

### 6.4 类别管理

- 内置类别 `is_builtin = 1`：
  - 用户**可以**重命名、归档、改图标颜色
  - 用户**不能**硬删（界面隐藏删除按钮）
  - 设置里提供"重置默认类别"：将全部内置类别恢复为 seed 的默认 `name` / `icon` / `color` / `sort_order`，并取消归档（`is_archived = 0`）；**不删除**用户自建类别，**不改**历史账的 `category_id`
- 自建类别 `is_builtin = 0`：
  - 可以完全编辑、归档（`is_archived = 1`，不进流水回收站）
- 同 `type` 下不允许重名（收入「其他」与支出「其他」可并存）
- **类别归档不影响历史账**：账的 `category_id` 保留，显示时如果类别已归档则显示「（已归档）」

### 6.5 账户管理

- 新建账户必须填"初始余额"（可为 0）
- 账户归档：所有引用该账户的历史账仍然显示（账户名后缀「（已归档）」）
- 不允许同名账户
- 修改 `initial_balance` 会即时改变运行时余额（有意为之，不回溯改流水）

---

## 7. UI / UX 设计

### 7.1 视觉风格

- **Material 3**（Compose Material 3）
- **Dynamic Color**（Android 12+）：从系统壁纸自动取色
- **回退主题色**：松石绿 / teal（暗合"乐"），作为 Android 11- 的 fallback
  - 主色 hex `#006C5C`（深松石绿）+ 薄荷色 `#6FF7DD`（强调）
- **Light / Dark 自动** + App 内手动切换

### 7.2 深色模式

| 模式 | 说明 |
|------|------|
| 跟随系统 | 默认，跟 Android 系统设置 |
| 浅色 | 强制浅色主题 |
| 深色 | 强制深色主题 |

**实现**：
- `MaterialTheme` 中根据 `ThemeMode` 选择 `lightColorScheme()` / `darkColorScheme()` / 动态
- 设置存 `DataStore`（不是 SharedPreferences，类型安全）
- 配置变更（系统切换）通过 Compose `isSystemInDarkTheme()` 自动响应

### 7.3 关键页面草图（文字版）

#### 账单 Tab

```
┌─────────────────────────────────────────┐
│  2026 年 7 月              [<] [>]     │ ← 月份切换
├─────────────────────────────────────────┤
│  ┌───────────────────────────────────┐  │
│  │         ¥ 2,341                   │  │ ← 本月已花（大数字）
│  │  收入 ¥8,000  结余 ¥5,659         │  │
│  │  ████████████░░░░░░  29%          │  │ ← 支出占收入（2341/8000）
│  └───────────────────────────────────┘  │
│                                          │
│  分类饼图                                │
│  ╭─────────╮                             │
│  │ 餐饮 38%│                             │
│  │ 购物 22%│                             │
│  │ 交通 15%│                             │
│  │ 其他 25%│                             │
│  ╰─────────╯                             │
│                                          │
│  趋势（按日）                            │
│  ╱╲    ╱╲                               │
│ ╱  ╲__╱  ╲___                            │
│                                          │
│  最近 5 笔                               │
│  🍔 餐饮  星巴克       -35.00  08-12    │
│  🚇 交通  地铁          -6.00  08-12    │
│  💰 工资  7月薪资    +8000.00  08-10    │
│  🍔 餐饮  麦当劳       -32.50  08-10    │
│  🛒 购物  京东        -128.00  08-09    │
│                                          │
│        [查看全部流水 →]                  │
└─────────────────────────────────────────┘
```

#### 记账 Tab

```
┌─────────────────────────────────────────┐
│   [支出]   [收入]   [转账]              │ ← 模式 Tab
├─────────────────────────────────────────┤
│                                          │
│        ¥ 0.00                            │ ← 金额（大）
│        ┌──────────────────────────┐     │
│        │  0   1   2   3           │     │ ← 自定义数字键盘
│        │  4   5   6   7           │     │
│        │  8   9   .   ⌫           │     │
│        └──────────────────────────┘     │
│                                          │
│  类别：🍔 餐饮  ▼                       │
│  账户：💵 现金  ▼                       │
│  说明：[买早餐____________]              │
│  时间：08-12 08:30  ▼                    │
│                                          │
│           [   保  存   ]                 │ ← 主按钮
└─────────────────────────────────────────┘
```

#### 设置 Tab

```
┌─────────────────────────────────────────┐
│                                          │
│  账户管理                          →    │
│  类别管理                          →    │
│                                          │
│  数据                                    │
│    导出备份                        →    │
│    导入备份                        →    │
│    回收站（2 项）                  →    │
│                                          │
│  外观                                    │
│    主题：跟随系统              →        │
│                                          │
│  关于                                    │
│    版本号 v0.1.0                         │
│                                          │
└─────────────────────────────────────────┘
```

---

## 8. 技术架构

### 8.1 技术栈

| 类别 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin 2.0+ | 现代 Android 官方语言 |
| UI | Jetpack Compose | Google 主推，2026 主流 |
| UI 设计 | Material 3 | 官方设计系统 |
| 架构 | MVVM + Repository | Google 官方标准 |
| 数据库 | Room (SQLite) | 类型安全、官方 |
| 异步 | Coroutines + Flow | 标配 |
| DI | Hilt | Google 官方 DI |
| 导航 | Navigation Compose | 官方导航方案 |
| 持久化（设置） | DataStore | 替代 SharedPreferences |
| 序列化 | kotlinx.serialization | Kotlin 官方、JSON 导出 |
| 图表 | **纯 Compose Canvas**（自绘） | 零外部依赖，灵活度最高；v0.1 实装（饼图 + 趋势折线） |
| 时间 | kotlinx-datetime | 替代 java.time，更 Kotlin 友好 |
| 测试 | JUnit5 + MockK + Turbine | ViewModel/Repository 单测 |
| 后台任务 | WorkManager | 30 天清理任务 |

### 8.2 模块结构（单 module，简化）

```
com.biehuale.app/
├── BieHuaLeApp.kt              # @HiltAndroidApp
├── MainActivity.kt             # 单 Activity 容器
├── data/                       # 数据层
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── entity/
│   │   │   ├── AccountEntity.kt
│   │   │   ├── CategoryEntity.kt
│   │   │   └── TransactionEntity.kt
│   │   └── dao/
│   │       ├── AccountDao.kt
│   │       ├── CategoryDao.kt
│   │       └── TransactionDao.kt
│   ├── repository/
│   │   ├── AccountRepository.kt
│   │   ├── CategoryRepository.kt
│   │   └── TransactionRepository.kt
│   ├── seed/
│   │   └── DefaultCategories.kt
│   └── backup/
│       ├── BackupExporter.kt
│       └── BackupImporter.kt
├── domain/                     # 领域层（可选，初期可省）
│   ├── model/
│   └── usecase/
├── ui/                         # UI 层
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── common/                 # 通用 Composable
│   ├── nav/
│   │   └── AppNav.kt           # Navigation 图
│   ├── bill/                   # 账单 Tab
│   │   ├── BillScreen.kt
│   │   └── BillViewModel.kt
│   ├── record/                 # 记账 Tab
│   │   ├── RecordScreen.kt
│   │   └── RecordViewModel.kt
│   ├── settings/               # 设置 Tab
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   ├── AccountManageScreen.kt
│   │   ├── CategoryManageScreen.kt
│   │   └── RecycleBinScreen.kt
│   ├── list/                   # 全部流水
│   │   ├── AllTransactionsScreen.kt
│   │   └── AllTransactionsViewModel.kt
│   └── detail/                 # 流水详情
│       ├── TransactionDetailScreen.kt
│       └── TransactionDetailViewModel.kt
├── di/                         # Hilt Modules
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── util/
    ├── Money.kt                # 金额格式化
    └── DateExt.kt
```

### 8.3 关键依赖版本（以 `android/gradle/libs.versions.toml` 为准）

> 下列片段为文档快照；**实装与升级只改 toml**，再回写本节。

```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
compose-bom = "2024.10.00"
hilt = "2.52"
room = "2.6.1"
ksp = "2.0.21-1.0.27"
coroutines = "1.9.0"
lifecycle = "2.8.7"
navigation = "2.8.5"
datastore = "1.1.1"
serialization = "1.7.3"
hilt-navigation-compose = "1.2.0"
workmanager = "2.10.0"
```

完整 catalog 见仓库内 toml（含 libraries / plugins）。骨架阶段选用 **稳定** Compose BOM 与 Room，不追最新 alpha。

**SDK 配置**：

```kotlin
android {
    compileSdk = 35  // Android 15
    defaultConfig {
        minSdk = 24    // Android 7.0，覆盖 95%+ 设备
        targetSdk = 35 // Android 15
    }
}
```

---

## 9. 非功能性需求

### 9.1 性能

- App 冷启动 ≤ 1.5s（中端机：骁龙 778G 级别）
- 列表滚动 60fps（1000 笔账以内无卡顿）
- 打开"记账"Tab 到金额可输入 ≤ 300ms
- Room 查询（按月聚合）≤ 100ms（1 万笔账以内）

### 9.2 隐私

- **不申请任何运行时权限**——纯本地，不需要 INTERNET、不需要通知监听、不需要存储权限（用 SAF 选文件）
- 网络：默认 **no INTERNET permission**——hardcode 防止意外加进去
- 数据全部存 App 私有目录（`/data/data/com.biehuale.app/databases/`）
- 备份 JSON 走 Storage Access Framework（SAF），用户自选位置

### 9.3 兼容性

| 项 | 目标 |
|----|------|
| 最低 Android 版本 | 7.0 (API 24) |
| 目标 Android 版本 | 15 (API 35) |
| 屏幕尺寸 | 4.7" ~ 7"（手机 + 折叠屏） |
| 架构 | arm64-v8a, armeabi-v7a, x86_64 |
| 字体 | 默认 Roboto（系统字体） |

### 9.4 健壮性

- 关键业务逻辑（转账、余额计算、统计聚合）必须有单元测试
- 数据库迁移有 `Migration` 测试
- 备份导入必须有 schema version 校验
- 所有金额用 Long（分），不用 Double
- 所有时间用 epoch millis (Long)，不用 Date/LocalDateTime 直存

---

## 10. 数据导入导出格式

### 10.1 JSON Schema (v1)

```json
{
  "schemaVersion": 1,
  "appVersion": "0.1.0",
  "exportedAt": "2026-07-19T18:12:34Z",
  "accounts": [
    {
      "id": 1,
      "name": "现金",
      "icon": "cash",
      "color": "#FF9800",
      "initialBalance": 50000,
      "isArchived": false,
      "createdAt": 1721300000000,
      "updatedAt": 1721300000000
    }
  ],
  "categories": [
    {
      "id": 1,
      "name": "餐饮",
      "icon": "restaurant",
      "color": "#FF5722",
      "type": "EXPENSE",
      "isBuiltin": true,
      "sortOrder": 1,
      "isArchived": false,
      "createdAt": 1721300000000,
      "updatedAt": 1721300000000
    }
  ],
  "transactions": [
    {
      "id": 1,
      "amount": 500,
      "type": "EXPENSE",
      "categoryId": 1,
      "accountId": 1,
      "toAccountId": null,
      "description": "买早餐",
      "occurredAt": 1721300000000,
      "createdAt": 1721300000000,
      "updatedAt": 1721300000000,
      "deletedAt": null
    }
  ]
}
```

### 10.2 导入规则

- 校验 `schemaVersion` 匹配，**不匹配则拒绝并提示**（含过高/过低版本）
- 合并模式（默认）：保留现有 `id`，导入的记录 `id` 重新映射避免冲突
- 导入前弹预览："将导入 N 个账户、M 个类别、K 笔账" → 用户确认
- 导入失败：整笔回滚，不留半成品
- 余额为运行时聚合，导入成功后**无需**单独「重算余额」步骤；下次查询即反映新数据

### 10.3 导出文件名

`别花乐_yyyy-MM-dd_HHmm.json`

例：`别花乐_2026-07-19_1812.json`

---

## 11. 里程碑 / 阶段计划

> 用户明确**不设时间 deadline**，以下为推荐阶段，**每阶段可独立交付**。

### Phase 1：骨架（最最核心，能跑起来）

- 项目初始化（Android Studio + Compose 模板）
- 主题、Hilt、Room、Navigation 基础配置
- 三 Tab 框架
- 记账 Tab：金额输入 + 类别选择 + 账户选择 + 保存
- 账单 Tab：纯流水列表
- 内置类别 seed
- **里程碑**：能在自己手机上装上、记一笔、看一眼列表

### Phase 2：完善记账与多账户

- 转账功能（含余额联动）
- 账户管理（新增/编辑/归档）
- 类别管理（编辑/删除/重置默认）
- 流水详情 + 编辑
- **里程碑**：完整的多账户记账闭环

### Phase 3：统计与筛选

- 账单 Tab 顶部「本月已花」卡片 + 支出占收入比例条
- 分类饼图
- 趋势折线图（按日/周/月）— 对应 F13
- 时间筛选（月份切换 + 自定义区间）
- 搜索（按说明）
- 筛选（账户/类别/类型）
- **里程碑**：能看到"我这个月都花哪了"

### Phase 4：备份与打磨

- JSON 导出（SAF）
- JSON 导入（合并 + 校验）
- 回收站（30 天软删除，仅流水）
- 深色模式（系统 + 手动）
- 空状态、错误状态、加载状态
- **里程碑**：完整可用，可分享 APK

### Phase 5：发布准备（可选）

- 应用图标设计
- 关于页
- 单元测试补全
- 性能优化
- 多 ABI APK 打包

---

## 12. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| 转账余额计算错 | 高 | 写单元测试覆盖所有转账场景；phase 2 重点 |
| JSON 导入版本不兼容 | 中 | schemaVersion 字段 + 拒绝不匹配版本 + Migration 测试 |
| 软删除 30 天清理任务漏跑 | 中 | WorkManager 兜底，App 启动时也做一次兜底 |
| Dynamic Color 在 Android 11- 不支持 | 低 | fallback 到青绿色，逻辑要写正确 |
| 单测覆盖不足 | 中 | 关键业务（转账、聚合、导入）必须测；UI 单测不强求 |
| Compose BOM / Room 版本漂移 | 中 | **以 `libs.versions.toml` 为准**；文档跟 toml，不追未验证的最新 BOM |
| 用户中断开发周期 | 低 | 13 问确认过不赶工，按 phase 推进，每个 phase 独立可用 |

---

## 13. 开放问题

> 标记为"待定"的问题，开发过程中再决定或确认。

1. **首次启动引导**：要不要做？目前倾向"不做"，第一次打开直接进账单 Tab，账户/类别空着（类别有 seed），提示"去记账" → 记账 Tab → 引导先建账户。
2. **导出进度提示**：大账本（1 万+ 笔）导出可能慢，要不要做进度条？倾向"做"，简单 LinearProgressIndicator。
3. **多币种**：不做；schema 不预留字段。若未来需要，再 Migration 加 `currency`。
4. **账户颜色选择**：要不要做颜色拾色器？倾向"提供 6-8 个预设颜色让用户选"，不做拾色器。
5. **应用图标**：先用 Material 钱包图标，做完 phase 5 再设计。
6. **关于页 GitHub 链接**：是否开源、仓库 URL——发布前再定；MVP 可先隐藏或放占位。

---

## 14. 附录：决策日志

> 13 问收尾 + 3 项补充 + 文档审阅修正，按顺序记录。

| # | 决策点 | 结论 | 理由 |
|---|--------|------|------|
| 1 | 目标用户 | 自己用 + 可发朋友 | 项目命名"BieHuaLe"暗示小范围；MVP 不做云同步 |
| 2 | 技术栈 | Kotlin + Jetpack Compose | 官方主推；AI 训练数据多；APK 小 |
| 3 | MVP 范围 | 标准版（= §3.1 F1–F13） | 极简版太薄，进阶版太重；以功能表为准 |
| 4 | 账户模型 | 多账户 + 自建 | 现代人多支付方式，转账逻辑多 1-2 天 |
| 5 | 类别系统 | 内置 + 可改 | 零摩擦启动；预设覆盖 90% 场景 |
| 6 | 预算 | ❌ 不做 | 用户明确不需要；比例条 = 支出/收入，非预算 |
| 7 | 自动记账 | ❌ 不做 | 维护成本高，3 秒手动记账够用 |
| 8 | 数据备份 | JSON 导出 + 导入 | 关系型数据用 JSON；人类可读 |
| 9 | UI 风格 | Material 3 + Dynamic Color | 零额外成本；自动适配系统 |
| 10 | 页面结构 | 3 Tab：账单 / 记账 / 设置 | 记账放独立 Tab 一打开就记 |
| 11 | 代码架构 | MVVM + Repository + Room | Google 官方；AI 最会写 |
| 12 | 应用名 + 包名 | 别花乐 / `com.biehuale.app` | 文件夹已用；记忆点强；3 字 |
| 13 | 节奏 | 不设时间，质量优先 | 13 问确认；按 phase 推进 |
| 14 | 目标 SDK | API 35 (Android 15) | 用户确认；Play Store 2026 强制 |
| 15 | 深色模式 | 跟系统 + App 内手动切换 | 用户确认；Material 3 默认有 |
| 16 | 说明字段 | 单行 ≤200 字符 | 用户确认；纯文本 |
| 17 | currency 字段 | MVP 不预留 | 避免假预留；需要时 Migration |
| 18 | 比例条语义 | 支出/收入 | 无预算功能时的唯一合理含义 |
| 19 | Compose BOM / Room | BOM `2024.10.00` + Room `2.6.1` | 骨架用稳定版；以 toml 为准 |

---

**文档结束。** 下一步：把 Phase 1 拆成可执行 task，启动开发。
