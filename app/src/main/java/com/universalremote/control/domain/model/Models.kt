package com.universalremote.control.domain.model

/**
 * 领域模型：家电类型、空调状态、遥控动作、设备档案、发射结果等。
 * 本文件无业务逻辑，仅定义类型；改协议值时改对应枚举的构造参数。
 */

/** 家电类型。新增设备（电视、风扇等）时在此扩展，并为每种类型提供协议驱动。 */
enum class ApplianceType {
  /** 空调（当前唯一实现） */
  AIR_CONDITIONER,
  // 预留：TV, FAN, SET_TOP_BOX ...
}

/**
 * 空调工作模式。
 * @param protocolValue TCL112 协议字节中的模式 nibble（改协议映射时改此值）
 */
enum class AcMode(val protocolValue: Int) {
  /** 制热 */
  HEAT(1),
  /** 除湿 */
  DRY(2),
  /** 制冷 */
  COOL(3),
  /** 送风 */
  FAN(7),
  /** 自动 */
  AUTO(8),
}

/**
 * 风速档位。
 * @param protocolValue TCL112 状态字节中的 3 bit 编码
 */
enum class AcFanSpeed(val protocolValue: Int) {
  /** 自动风速 */
  AUTO(0b000),
  /** 微风（协议支持，UI 暂未单独暴露） */
  MIN(0b001),
  /** 低风 */
  LOW(0b010),
  /** 中风 */
  MED(0b011),
  /** 高风 */
  HIGH(0b101),
}

/**
 * UI/业务层统一的「用户意图」；由 [com.universalremote.control.domain.repository.RemoteControlRepository.sendAcAction] 解释。
 */
enum class RemoteAction {
  /** 电源开/关切换 */
  POWER_TOGGLE,
  /** 设定温度 +0.5℃ */
  TEMP_UP,
  /** 设定温度 -0.5℃ */
  TEMP_DOWN,
  /** 模式：制冷 */
  MODE_COOL,
  /** 模式：制热 */
  MODE_HEAT,
  /** 模式：自动 */
  MODE_AUTO,
  /** 模式：送风 */
  MODE_FAN,
  /** 风速：自动 */
  FAN_AUTO,
  /** 风速：低 */
  FAN_LOW,
  /** 风速：中 */
  FAN_MED,
  /** 风速：高 */
  FAN_HIGH,
  /** 面板灯光：关 */
  LIGHT_OFF,
  /** 面板灯光：开 */
  LIGHT_ON,
}

/**
 * 设备档案（首页列表一项）。
 * 预设数据见 RemoteControlRepository.PRESET_DEVICES。
 */
data class DeviceProfile(
  /** 设备唯一 id，用于配对存储与 Intent 传递 */
  val id: String,
  /** 列表主标题 */
  val name: String,
  /** 列表副标题/说明 */
  val description: String,
  /** 家电类型，决定跳转哪个遥控页 */
  val applianceType: ApplianceType,
  /** 品牌名（展示/筛选预留） */
  val brand: String,
)

/**
 * 空调逻辑状态（App 内维护，与红外帧同步）。
 */
data class AcState(
  /** 电源是否开启 */
  val powerOn: Boolean = true,
  /** 当前工作模式 */
  val mode: AcMode = AcMode.COOL,
  /** 设定温度（℃），通常 16–31，步进 0.5 */
  val temperatureCelsius: Float = 24f,
  /** 当前风速档位 */
  val fanSpeed: AcFanSpeed = AcFanSpeed.AUTO,
  /** 面板灯光是否亮（逻辑状态，可能与实体灯不同步） */
  val lightOn: Boolean = true,
)

/** 红外发射结果；Repository 返回，ViewModel 据此更新 feedback。 */
sealed class TransmitResult {
  /** 发射成功 */
  data object Success : TransmitResult()
  /**
   * 发射失败。
   * @param message 用户可读错误原因
   */
  data class Failure(val message: String) : TransmitResult()
}

/** 设备连接探测结果。 */
sealed class DeviceConnectionResult {
  /** 可进入遥控页（有 IR 且已配对等条件满足） */
  data object Connected : DeviceConnectionResult()
  /**
   * 不可进入。
   * @param message 失败原因文案
   */
  data class Failed(val message: String) : DeviceConnectionResult()
}
