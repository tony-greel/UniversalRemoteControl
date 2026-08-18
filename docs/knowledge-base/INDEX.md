# 知识库索引 — UniversalRemoteControl

本目录记录项目的 **Bug 修复** 与 **新需求迭代**，按年归档，供后续排查与 onboarding。

## 目录

| 类型 | 路径 | 说明 |
|------|------|------|
| Bug | [bugs/2026.md](./bugs/2026.md) | 2026 年 bug 修复记录 |
| Feature | [features/2026.md](./features/2026.md) | 2026 年新需求/功能记录 |
| 模板 | [templates/](./templates/) | 复制用模板 |

## 最近条目

| 日期 | 类型 | 标题 | 模块 | 详情 |
|------|------|------|------|------|
| 2026-08-17 | Bug | 红外发送失败仍更新 App 状态（回家 P0） | `domain/repository`, `ui/ac`, `res/layout`, `res/values`, `app/src/test` | [bugs/2026.md#2026-08-17-红外发送失败仍更新-app-状态回家场景-p0](./bugs/2026.md#2026-08-17-红外发送失败仍更新-app-状态回家场景-p0) |
| 2026-08-17 | Feature | 全量变量中文注释补强 | 全层 `app/src/main` | [features/2026.md#2026-08-17-全量变量中文注释补强](./features/2026.md#2026-08-17-全量变量中文注释补强) |
| 2026-08-17 | Feature | 项目架构说明与全量方法中文注释 | `docs`, 全层 `app/src/main` | [features/2026.md#2026-08-17-项目架构说明与全量方法中文注释](./features/2026.md#2026-08-17-项目架构说明与全量方法中文注释) |
| 2026-08-17 | Bug | 无空调时点击设置仍可进入遥控页 | `ui/main`, `domain/repository`, `data/preference` | [bugs/2026.md#2026-08-17-无空调时点击设置仍可进入遥控页](./bugs/2026.md#2026-08-17-无空调时点击设置仍可进入遥控页) |
| 2026-08-17 | Feature | 设备连接探测与设置入口 | `ui/main`, `domain/repository`, `domain/model` | [features/2026.md#2026-08-17-设备连接探测与设置入口](./features/2026.md#2026-08-17-设备连接探测与设置入口) |
| 2026-08-17 | Feature | 空调状态持久化：进入 App 恢复上次键位设置 | `data/preference`, `domain/repository`, `data/protocol/tcl` | [features/2026.md#2026-08-17-空调状态持久化进入-app-恢复上次键位设置](./features/2026.md#2026-08-17-空调状态持久化进入-app-恢复上次键位设置) |
| 2026-08-17 | Bug | 空调模式/风速选中「自动」后无法切换 | `ui/ac`, `res/color`, `res/layout` | [bugs/2026.md#2026-08-17-空调模式风速选中自动后无法切换](./bugs/2026.md#2026-08-17-空调模式风速选中自动后无法切换) |
| 2026-08-17 | Feature | 空调模式与风速单选高亮 | `ui/ac`, `res/color`, `res/layout` | [features/2026.md#2026-08-17-空调模式与风速单选高亮](./features/2026.md#2026-08-17-空调模式与风速单选高亮) |
| 2026-08-17 | Feature | 灯光控制可靠性验证与单测补强 | `data/preference`, `domain/repository`（测试）, `data/protocol/tcl`（测试） | [features/2026.md#2026-08-17-灯光控制可靠性验证与单测补强](./features/2026.md#2026-08-17-灯光控制可靠性验证与单测补强) |
| 2026-08-17 | Bug | 方式三 HYBRID_ALL 混合轮询仅发送首条策略即返回 | `domain/repository`, `data/preference` | [bugs/2026.md#2026-08-17-方式三-hybrid_all-混合轮询仅发送首条策略即返回](./bugs/2026.md#2026-08-17-方式三-hybrid_all-混合轮询仅发送首条策略即返回) |
| 2026-08-17 | Feature | 灯光控制方式三：HYBRID_ALL 自动轮询落地 | `domain/repository`, `ui/ac` | [features/2026.md#2026-08-17-灯光控制方式三hybrid_all-自动轮询落地](./features/2026.md#2026-08-17-灯光控制方式三hybrid_all-自动轮询落地) |
| 2026-08-17 | Feature | 灯光控制实验扩展：预留混合轮询模式 | `domain/model` | [features/2026.md#2026-08-17-灯光控制实验扩展预留混合轮询模式](./features/2026.md#2026-08-17-灯光控制实验扩展预留混合轮询模式) |
| 2026-08-17 | Feature | 灯光控制实验：状态位变体 + 独立红外码试码 | `ui/ac`, `data/protocol/tcl`, `data/preference` | [features/2026.md#2026-08-17-灯光控制实验状态位变体--独立红外码试码](./features/2026.md#2026-08-17-灯光控制实验状态位变体--独立红外码试码) |
| 2026-08-17 | Bug | 单元测试 Tcl112AcRemoteTest checksum 断言错误 | `data/protocol/tcl`（测试） | [bugs/2026.md#2026-08-17-单元测试-tcl112acremotetest-checksum-断言错误](./bugs/2026.md#2026-08-17-单元测试-tcl112acremotetest-checksum-断言错误) |
| 2026-08-17 | Bug | 构建失败：JDK 25 与 Gradle 8.9 / Kotlin 插件不兼容 | 构建脚本 | [bugs/2026.md#2026-08-17-构建失败jdk-25-与-gradle-89--kotlin-插件不兼容](./bugs/2026.md#2026-08-17-构建失败jdk-25-与-gradle-89--kotlin-插件不兼容) |
| 2026-08-17 | Feature | 初始化 Android 红外遥控 MVP（TCL 空调 + 关灯） | `ui/ac`, `data/protocol/tcl`, `domain/repository` | [features/2026.md#2026-08-17-初始化-android-红外遥控-mvptcl-空调--关灯](./features/2026.md#2026-08-17-初始化-android-红外遥控-mvptcl-空调--关灯) |

## 按模块速查（手工维护摘要）

| 模块 | Bug 次数 | Feature 次数 | 备注 |
|------|----------|--------------|------|
| `docs` | 0 | 2 | `architecture.md` + 方法/变量注释文档化 |
| 构建脚本 | 1 | 0 | Gradle 9.1 + AGP 9.0.1 |
| `domain/model` | 0 | 4 | 空调状态模型 + 灯光策略枚举 + 连接探测结果 |
| `data/preference` | 0 | 3 | 灯光/`AcState` 持久化 + 设备配对状态 |
| `data/protocol/tcl` | 1 | 2 | TCL112AC 编解码 + 灯光候选库；单测 4 项 |
| `data/ir` | 0 | 1 | ConsumerIrManager 发射层 |
| `domain/repository` | 3 | 6 | 遥控编排 + 连接探测 + 配对校验 + 发射失败回滚；单测 12 项 |
| `ui/ac` | 1 | 4 | 遥控面板 + 状态同步提示 + 灯光实验区 + 模式/风速 ToggleGroup 选中态 |
| `ui/main` | 1 | 2 | 设备列表 + IR 门禁 + 配对对话框 |

## 使用方式

- 手动追加：复制 `templates/bug-record.md` 或 `feature-record.md` 到对应年份文件
- 提问示例：「今年遥控指令模块改过哪些 bug？」→ 查 `bugs/2026.md` 与上表
