# 别花乐 v0.1.0 发布说明

首个可分享安装包。构建通过后可发给朋友试用（个人分享，非正式商店上架）。

## 安装

1. 在电脑上构建（或使用已打好的 APK）：

```powershell
$env:JAVA_HOME = "D:\java\jdk-17"
cd android
.\gradlew.bat :app:assembleRelease
```

2. APK 输出目录：`android/app/build/outputs/apk/release/`
   - 推荐手机：`app-arm64-v8a-release.apk`
   - 老设备：`app-armeabi-v7a-release.apk`
   - 模拟器：`app-x86_64-release.apk`
   - 不确定架构：`app-universal-release.apk`

3. 传到手机后允许「未知来源」安装。

> 当前 release **使用 debug 签名**，便于个人直接安装；正式上架前请换成独立 keystore。

## 功能

- 支出 / 收入 / 转账记账（金额以「分」存储）
- 多账户、多类别管理
- 账单统计：月汇总、比例条、类别饼图、日/周/月趋势
- 搜索与筛选（类型 / 账户 / 类别）
- 流水详情与编辑
- 回收站（软删除，约 30 天后自动清理）
- JSON 备份导入导出（系统文件选择器 SAF）
- 浅色 / 深色 / 跟随系统

## 已知限制

- **无云同步**，数据仅存本机
- 备份请用设置里的导出（SAF），不要依赖系统自动云备份（`allowBackup=false`）
- 不上架 Play / 国内应用商店；无正式发布签名体系
- 图标为可分享用的 adaptive vector，非设计师终稿

## 隐私

完全离线，不申请 INTERNET / 存储等运行时权限。
