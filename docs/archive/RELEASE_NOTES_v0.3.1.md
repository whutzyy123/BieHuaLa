# 别花乐 v0.3.1 发布说明

**日期**：2026-07-19  
**主题**：v0.3.0 后残余缺陷修复（备份语义 / 连点保存 / Room 索引）

## 用户可见

- 导入含回收站数据的备份时，不会把仍应软删的账误恢复
- 备份里已删除的交易会同步软删本地同内容账
- 同名账户若本地期初已非 0，重导不会被旧备份盖掉
- 记账页快速连点保存不会记两笔
- 导入预览阶段显示「读取中…」，确认后才显示「导入中…」

## 构建

```powershell
$env:JAVA_HOME = "D:\java\jdk-17"
cd D:\BieHuaLe\android
.\gradlew.bat :app:assembleRelease --no-daemon
```

`versionName`：`0.3.1` · `versionCode`：`3` · Room schema：`3`
