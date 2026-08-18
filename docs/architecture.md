# UniversalRemoteControl 架构说明

## 分层总览

```
┌─────────────────────────────────────────────────────────────┐
│  UI 层 (ui/)                                                │
│  MainActivity / AcRemoteActivity → ViewModel → LiveData     │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  领域编排层 (domain/repository/)                              │
│  RemoteControlRepository — 状态维护 + 指令编排 + 持久化       │
└───────────────────────────┬─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌───────────────┐  ┌────────────────┐  ┌──────────────────┐
│ data/protocol │  │ data/ir        │  │ data/preference  │
│ TCL112 编码   │  │ 红外硬件发射   │  │ SharedPreferences│
└───────────────┘  └────────────────┘  └──────────────────┘
```

## 核心调用链（空调遥控）

```
用户点击按钮
  → AcRemoteActivity.bindActions()
  → AcRemoteViewModel.sendAction(RemoteAction)
  → RemoteControlRepository.sendAcAction()
       ├─ 更新 AcState（温度/模式/风速等）
       ├─ Tcl112AcRemote.apply() 生成 IrPattern
       └─ AndroidIrTransmitter.transmit() → ConsumerIrManager
  → ViewModel 刷新 LiveData
  → Activity renderState() 更新界面
```

## 灯光实验调用链

```
用户点「关灯」
  → RemoteControlRepository.sendAcAction(LIGHT_OFF)
  → sendLight(false)
       ├─ STATE_BIT_VARIANT: Tcl112AcRemote.apply + bitMask
       ├─ DISCRETE_PATTERN: TclLightPatternLibrary.patternFor()
       └─ HYBRID_ALL: sendHybridLight() 轮询多种策略
```

## 设备配对调用链

```
MainActivity 点「设置」
  → MainViewModel.requestOpenDeviceSettings()
  → RemoteControlRepository.checkDeviceConnection()
  → 未配对 → pairingRequest → 探测对话框
  → sendConnectionProbe() → transmit 当前状态帧
  → 用户确认 → confirmDevicePairing() → DevicePairingStore
```

## 类职责速查

| 类 | 层 | 职责 |
|----|-----|------|
| `UniversalRemoteApp` | 应用入口 | 创建 `AppContainer` |
| `AppContainer` | 核心 | 手动依赖注入，组装 Repository |
| `Models.kt` | 领域模型 | 枚举、状态、结果类型 |
| `RemoteControlRepository` | 编排 | **业务中枢**：状态 + 发码 + 持久化 |
| `Tcl112AcRemote` | 协议 | 14 字节状态机，生成 TCL 空调帧 |
| `Tcl112AcEncoder` | 协议 | 字节数组 → 红外时序波形 |
| `TclLightPatternLibrary` | 协议 | 灯光独立码候选库 |
| `IrTransmitter` / `AndroidIrTransmitter` | 硬件 | 抽象 / 实现红外发射 |
| `*Preferences` | 持久化 | SharedPreferences 读写 |
| `MainViewModel` / `AcRemoteViewModel` | UI 逻辑 | 暴露 LiveData，转发 Repository |
| `MainActivity` / `AcRemoteActivity` | UI | 绑定 View，观察 LiveData |

## 常见修改入口

| 想改什么 | 改哪里 |
|----------|--------|
| 预设设备列表 | `RemoteControlRepository.PRESET_DEVICES` |
| 温度范围 16–31℃ | `Tcl112AcRemote` companion `TEMP_MIN/TEMP_MAX` + `Repository.sendAcAction` coerce |
| 红外载波 38kHz | `Tcl112AcEncoder.CARRIER_HZ` |
| 灯光候选策略 | `TclLightPatternLibrary.candidates` |
| 默认灯光控制方式 | `LightControlConfig` 默认值 / `LightControlPreferences` |
| 新增遥控按钮 | `RemoteAction` → Repository → Activity 绑定 |
