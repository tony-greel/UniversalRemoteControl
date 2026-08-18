# 贡献指南

感谢你对 Universal Remote Control 的关注。以下为参与开发与提交代码的基本规则。

## 开发流程

1. Fork 本仓库并在本地创建功能分支（如 `feature/xxx`、`fix/xxx`）
2. 在 Android Studio 或命令行完成改动
3. 本地验证通过后再提交 PR

## 必须通过的验证

```bash
./gradlew assembleDebug   # Debug 构建
./gradlew test            # 单元测试
./gradlew lint            # Lint（建议）
```

## 代码规范

- 使用 Kotlin，遵循项目现有包结构与命名风格
- 新增或修改的公开方法、类属性需补充中文 KDoc（说明职责、调用链、修改入口）
- 优先**最小改动**：新功能做加法，Bug 修复在边界上小范围修改，避免大范围重构
- 涉及以下内容的改动需额外说明并在 PR 中标注，便于人工审查：
  - `AndroidManifest.xml` 与权限声明
  - 签名与发布配置
  - TCL112AC 等核心协议编解码逻辑

## 提交信息

建议使用清晰的中文或英文摘要，例如：

```
fix: 红外发送失败时回滚 AcState
feat: 设备列表增加连接探测提示
```

## Pull Request

- 一个 PR 聚焦一个用户可见或一个 Bug 修复
- 描述中说明：改了什么、如何验证、是否有破坏性变更
- 关联相关 Issue（如有）

## 知识库归档

Bug 修复或新功能合并后，可在 `docs/knowledge-base/` 对应年份文件中追加记录，便于后续排查。可参考 `docs/knowledge-base/templates/` 下的模板。

## 禁止提交的内容

以下内容不应进入仓库（已在 `.gitignore` 中排除）：

- 本地 IDE 配置（`.idea/`、`local.properties`）
- 构建产物（`build/`、`*.apk`）
- 密钥与签名文件（`*.jks`、`*.keystore`）
- 个人本地工具配置目录
