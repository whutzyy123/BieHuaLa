# 别花乐 (BieHuaLe)

> 一款**单用户、本地优先**的安卓记账 App —— 3 秒记一笔，清晰统计，可分享 APK。

## 项目状态

✅ **v0.6.9 — Clarity Teal**（个人安装用，非正式商店上架）

- 3 Tab：账单 / 记账 / 设置；视觉按 [`docs/UI_DESIGN.md`](./docs/UI_DESIGN.md) v2
- Room schema **v6**；备份 JSON **v2**（v1 仍可导入）
- 快速记账、转账手续费、总资产、全局 UI 壳组件等见 [CHANGELOG](./CHANGELOG.md)

## 仓库结构

```
BieHuaLe/
├── android/                 # Android Studio 项目
│   └── app/src/
│       ├── main/java/…      # 业务代码（~80 个 .kt）
│       ├── test/            # 单元测试（11 个 .kt）
│       └── androidTest/     # 集成测试（含 Migration）
├── docs/
│   ├── PRD.md / UI_DESIGN.md / STRUCTURE.md
│   └── archive/             # 历史审查、旧 DEV_PLAN、旧 RELEASE_NOTES
├── AGENTS.md                # 协作者指南
├── CHANGELOG.md
└── README.md
```

更细的目录说明见 [`docs/STRUCTURE.md`](./docs/STRUCTURE.md)。

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / UI | Kotlin · Jetpack Compose · Material 3 |
| 架构 | MVVM + Repository + Room + Hilt |
| 构建 | Gradle · AGP（见 `android/gradle/libs.versions.toml`） |

## 开发命令

```powershell
cd android
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat installDebug
```

发布构建、签名与分发说明见下文历史段落精简版：`.\gradlew.bat assembleRelease`，输出在 `app\build\outputs\apk\release\`；当前 release 暂用 debug 签名便于个人分享。

## 隐私

- 完全离线，不上云；不申请运行时权限
- `allowBackup="false"`；备份走 SAF，用户自选位置

详见 [PRD §9.2](./docs/PRD.md) / [AGENTS.md](./AGENTS.md)

## 文档

| 文档 | 说明 |
|------|------|
| [PRD.md](./docs/PRD.md) | 产品规则与边界 |
| [UI_DESIGN.md](./docs/UI_DESIGN.md) | 视觉与构图（UI 改版必读） |
| [STRUCTURE.md](./docs/STRUCTURE.md) | 目录结构 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本更新日志 |
| [AGENTS.md](./AGENTS.md) | 协作者约束 |
| [docs/archive/](./docs/archive/) | 历史审查 / 旧计划 / 旧发布说明 |

## 已知限制

- release 暂用 debug 签名
- 仅中文为主；无云同步（建议定期 SAF 备份 JSON）

## License

个人项目，暂不开放源码。
