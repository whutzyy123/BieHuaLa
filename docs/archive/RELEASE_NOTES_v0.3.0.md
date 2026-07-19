# 别花乐 v0.3.0 发布说明

**日期**：2026-07-19  
**主题**：审查报告全量修复（数据正确性 + 可靠性）

详见 [`REVIEW_REPORT.md`](./REVIEW_REPORT.md) 与 [`../../CHANGELOG.md`](../../CHANGELOG.md)。

## 用户可见

- 导入备份：同内容交易会去重；若本地已软删，会**恢复**该笔，而不会再记一笔导致余额异常
- 清空回收站更可靠（整批删除）
- 账单 / 全部流水删除失败时有提示
- 设置里新建账户请进入「管理账户」
- 进入详情或设置子页时，底部对应 Tab 仍保持选中

## 构建

```powershell
$env:JAVA_HOME = "D:\java\jdk-17"
cd D:\BieHuaLe\android
.\gradlew.bat :app:assembleRelease --no-daemon
```

APK：`android/app/build/outputs/apk/release/`（含 ABI 分包与 universal）

`versionName`：`0.3.0` · `versionCode`：`2`
