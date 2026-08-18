# 万能遥控 (Universal Remote Control)

Android 红外万能遥控器应用，支持 TCL 空调（TCL112AC 协议）遥控与面板灯光控制。面向带红外发射器的 Android 手机，可扩展更多家电类型。

## 功能特性

- **设备列表**：预设 TCL 空调等设备，支持 IR 能力检测与配对流程
- **空调遥控**：电源、温度、模式（制冷/制热/自动/送风）、风速调节
- **状态持久化**：退出 App 后恢复上次空调设置
- **面板灯光**：支持关灯/开灯，提供多种试码策略（状态位变体、独立红外码、自动轮询）
- **连接探测**：进入设置前发送探测帧，确认手机已对准目标空调
- **发送失败回滚**：红外发送失败时不更新 App 内状态，避免界面与实际不一致

## 环境要求

| 项 | 要求 |
|----|------|
| Android Studio | Ladybug 或更高（推荐） |
| JDK | 17（推荐；JDK 25 与当前 Gradle/Kotlin 插件不兼容） |
| minSdk | 24 |
| targetSdk | 35 |
| 硬件 | 带 `ConsumerIrManager` 红外发射器的手机（非所有机型支持） |

## 快速开始

```bash
# 克隆仓库
git clone git@github.com:tony-greel/UniversalRemoteControl.git
cd UniversalRemoteControl

# Debug 构建
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug

# 运行单元测试
./gradlew test
```

构建产物：`app/build/outputs/apk/debug/app-debug.apk`

## 使用说明

1. 在支持红外的手机上安装并打开 App
2. 若提示「当前设备不支持红外发射」，请换用带 IR 功能的机型
3. 在设备列表中点击 TCL 空调的「设置」
4. 按引导将手机红外口对准空调，发送探测并确认空调有响应
5. 进入遥控页操作；若 App 显示状态与空调实际不符，先按一次电源键对齐

## 项目结构

```
app/src/main/java/com/universalremote/control/
├── ui/              # Activity、ViewModel、列表适配器
├── domain/          # 领域模型与 RemoteControlRepository 编排层
├── data/
│   ├── protocol/tcl # TCL112AC 编解码与灯光候选库
│   ├── ir/          # 红外发射抽象与 Android 实现
│   └── preference/  # SharedPreferences 持久化
└── core/            # AppContainer 依赖组装

docs/
├── architecture.md  # 架构与调用链说明
└── knowledge-base/  # Bug 与功能变更记录
```

详细架构见 [docs/architecture.md](docs/architecture.md)。

## 技术栈

- Kotlin
- AndroidX（AppCompat、Material、Lifecycle、ViewBinding）
- Gradle 9.1 + Android Gradle Plugin 9.0.1
- 单元测试：JUnit 4

## 贡献指南

欢迎提交 Issue 与 Pull Request。请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解分支、构建验证与代码规范。

## 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 作者

[tony-greel](https://github.com/tony-greel)
