package com.universalremote.control.data.protocol.tcl

import com.universalremote.control.data.ir.IrPattern
import com.universalremote.control.domain.model.AcFanSpeed
import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState

/**
 * TCL112AC 状态机：维护 14 字节空调状态并生成红外波形。
 * 逻辑移植自 IRremoteESP8266 IRTcl112Ac；每个实例独立一份 state 字节数组。
 */
class Tcl112AcRemote {

  /** 当前 14 字节 TCL112AC 协议状态（与 IRremoteESP8266 布局一致） */
  private val state: ByteArray = DEFAULT_STATE.copyOf()

  /**
   * 做什么：从内部字节数组读出 [AcState] 快照（不发射）。
   * 调用链：Repository.getAcState 间接 / 测试 / snapshot 默认状态。
   * 修改指引：读逻辑改 getPower/getMode 等私有读方法。
   */
  fun snapshot(): AcState {
    return AcState(
      powerOn = getPower(),
      mode = getMode(),
      temperatureCelsius = getTemp(),
      fanSpeed = getFan(),
      lightOn = getLight(),
    )
  }

  /**
   * 做什么：用 [stateUpdate] 覆盖内部字节状态，不生成红外。
   * 有什么用：App 启动时从 SharedPreferences 恢复与协议层同步。
   * 调用链：Repository.loadPersistedAcState → restore。
   * 修改指引：参数 [stateUpdate] 各字段映射到 setPower/setMode 等。
   */
  fun restore(stateUpdate: AcState) {
    setPower(stateUpdate.powerOn)
    setMode(stateUpdate.mode)
    setTemp(stateUpdate.temperatureCelsius)
    setFan(stateUpdate.fanSpeed)
    setLight(stateUpdate.lightOn)
  }

  /**
   * 做什么：应用 [stateUpdate] 并用默认灯光 bit（0x02）生成 [IrPattern]。
   * 调用链：Repository.sendAcAction（非灯光）/ sendConnectionProbe。
   * 修改指引：灯光相关改带 bitMask 的重载 [apply]。
   */
  fun apply(stateUpdate: AcState): IrPattern {
    return apply(stateUpdate, LIGHT_BIT, lightInvertedLogic = false)
  }

  /**
   * 做什么：应用状态并指定灯光 bit 掩码与反向逻辑，返回编码后的红外波形。
   * 调用链：Repository.sendLight STATE_BIT_VARIANT / TclLightPatternLibrary。
   * 修改指引：参数 [lightBitMask]、[lightInvertedLogic] 来自 LightBitVariant。
   */
  fun apply(stateUpdate: AcState, lightBitMask: Int, lightInvertedLogic: Boolean): IrPattern {
    setPower(stateUpdate.powerOn)
    setMode(stateUpdate.mode)
    setTemp(stateUpdate.temperatureCelsius)
    setFan(stateUpdate.fanSpeed)
    setLight(stateUpdate.lightOn, lightBitMask, lightInvertedLogic)
    return buildPattern()
  }

  /**
   * 做什么：导出当前 14 字节状态副本。
   * 调用链：TclLightPatternLibrary.byte5LightFrame。
   * 修改指引：一般不改。
   */
  fun exportState(): ByteArray = state.copyOf()

  /**
   * 做什么：刷新校验和并 encode 为 [IrPattern]。
   * 调用链：apply 末尾 / buildPattern 直接调用。
   * 修改指引：校验算法在 Tcl112AcEncoder.calcChecksum。
   */
  fun buildPattern(): IrPattern {
    refreshChecksum()
    return Tcl112AcEncoder.encode(state.copyOf())
  }

  /** 设置电源 bit；调用链：restore/apply → setPower。修改：参数 [on]。 */
  fun setPower(on: Boolean) {
    setBit(BYTE_POWER, POWER_BIT, on)
  }

  /** 读取电源状态。 */
  fun getPower(): Boolean = getBit(BYTE_POWER, POWER_BIT)

  /** 设置灯光（默认 bit 0x02）；调用链：restore。 */
  fun setLight(on: Boolean) {
    setLight(on, LIGHT_BIT, invertedLogic = false)
  }

  /**
   * 设置灯光指定位；[invertedLogic] 为 true 时逻辑与默认相反。
   * 修改指引：参数 [bitMask]、[invertedLogic]、[on]。
   */
  fun setLight(on: Boolean, bitMask: Int, invertedLogic: Boolean) {
    val bitEnabled = if (invertedLogic) on else !on // IRremote 默认 Light = !on
    clearLightBits()
    setBit(BYTE_FLAGS, bitMask, bitEnabled)
  }

  /** 读取灯光状态（默认 bit 语义）。 */
  fun getLight(): Boolean = !getBit(BYTE_FLAGS, LIGHT_BIT)

  /** 清除灯光相关 bit，避免多 bit 冲突。 */
  private fun clearLightBits() {
    state[BYTE_FLAGS] = (state[BYTE_FLAGS].toInt() and (LIGHT_BIT or ALT_LIGHT_BIT).inv()).toByte()
  }

  /**
   * 设置工作模式；FAN 模式会顺带 setFan(HIGH)。
   * 修改指引：参数 [mode] 为 AcMode；协议 nibble 在 companion MODE_*。
   */
  fun setMode(mode: AcMode) {
    val value = when (mode) { // 协议模式 nibble
      AcMode.FAN -> {
        setFan(AcFanSpeed.HIGH)
        MODE_FAN
      }
      AcMode.HEAT -> MODE_HEAT
      AcMode.DRY -> MODE_DRY
      AcMode.COOL -> MODE_COOL
      AcMode.AUTO -> MODE_AUTO
    }
    val current = state[BYTE_MODE].toInt() and 0xFF
    state[BYTE_MODE] = ((current and 0xF0.inv()) or (value and 0x0F)).toByte() // 保留高 nibble
  }

  /** 从字节解析 [AcMode]。 */
  fun getMode(): AcMode = when (state[BYTE_MODE].toInt() and 0x0F) {
    MODE_HEAT -> AcMode.HEAT
    MODE_DRY -> AcMode.DRY
    MODE_COOL -> AcMode.COOL
    MODE_FAN -> AcMode.FAN
    else -> AcMode.AUTO
  }

  /**
   * 设置温度（16–31℃，支持 0.5℃）；内部写 BYTE_TEMP 与半度 bit。
   * 修改指引：参数 [celsius]；范围 TEMP_MIN/TEMP_MAX。
   */
  fun setTemp(celsius: Float) {
    val safe = celsius.coerceIn(TEMP_MIN, TEMP_MAX)
    val halfDegrees = (safe * 2).toInt() // 以 0.5℃ 为单位的整数
    val halfBit = halfDegrees and 1 // 是否有 0.5℃ 小数部分
    val tempValue = (TEMP_MAX - halfDegrees / 2).toInt() // 协议温度编码

    val current = state[BYTE_TEMP].toInt() and 0xFF
    state[BYTE_TEMP] = ((current and 0xF0) or (tempValue and 0x0F)).toByte()

    setBit(BYTE_SWING, HALF_DEGREE_BIT, halfBit == 1)
    setBit(BYTE_SWING, IS_TCL_BIT, true)
  }

  /** 读取当前温度（℃）。 */
  fun getTemp(): Float {
    val base = TEMP_MAX - (state[BYTE_TEMP].toInt() and 0x0F)
    return if (getBit(BYTE_SWING, HALF_DEGREE_BIT)) base + 0.5f else base.toFloat()
  }

  /**
   * 设置风速；参数 [speed] 的 protocolValue 写入字节低 3 bit。
   * 修改指引：改 AcFanSpeed.protocolValue。
   */
  fun setFan(speed: AcFanSpeed) {
    val current = state[BYTE_FAN_SWING].toInt() and 0xFF
    state[BYTE_FAN_SWING] = ((current and 0x1F.inv()) or (speed.protocolValue and 0x07)).toByte()
  }

  /** 读取风速档位。 */
  fun getFan(): AcFanSpeed = when (state[BYTE_FAN_SWING].toInt() and 0x07) {
    AcFanSpeed.MIN.protocolValue -> AcFanSpeed.MIN
    AcFanSpeed.LOW.protocolValue -> AcFanSpeed.LOW
    AcFanSpeed.MED.protocolValue -> AcFanSpeed.MED
    AcFanSpeed.HIGH.protocolValue -> AcFanSpeed.HIGH
    else -> AcFanSpeed.AUTO
  }

  /** 根据当前 state 重算 BYTE_CHECKSUM。 */
  private fun refreshChecksum() {
    state[BYTE_CHECKSUM] = Tcl112AcEncoder.calcChecksum(state)
  }

  /** 写某字节的单个 bit；参数 [index] 字节下标、[bit] 掩码、[enabled] 是否置 1。 */
  private fun setBit(index: Int, bit: Int, enabled: Boolean) {
    val current = state[index].toInt() and 0xFF
    state[index] = if (enabled) {
      (current or bit).toByte()
    } else {
      (current and bit.inv()).toByte()
    }
  }

  /** 读某字节的单个 bit。 */
  private fun getBit(index: Int, bit: Int): Boolean {
    return state[index].toInt() and bit != 0
  }

  companion object {
    private const val STATE_LENGTH = 14
    /** 最低设定温度（℃） */
    private const val TEMP_MIN = 16f
    /** 最高设定温度（℃） */
    private const val TEMP_MAX = 31f

    /** state[5]：标志位（电源/灯光等） */
    private const val BYTE_FLAGS = 5
    /** state[6]：模式 nibble */
    private const val BYTE_MODE = 6
    /** state[7]：温度 nibble */
    private const val BYTE_TEMP = 7
    /** state[8]：风速低 3 bit */
    private const val BYTE_FAN_SWING = 8
    /** state[12]：摆风/半度/TCL 标志 */
    private const val BYTE_SWING = 12
    /** 与 BYTE_FLAGS 相同，电源 bit 所在字节 */
    private const val BYTE_POWER = 5
    /** state[13]：校验和 */
    private const val BYTE_CHECKSUM = 13

    /** 默认灯光控制 bit */
    private const val LIGHT_BIT = 0x02
    /** 备选灯光 bit（部分机型） */
    private const val ALT_LIGHT_BIT = 0x40
    /** 电源 bit */
    private const val POWER_BIT = 0x20
    /** 0.5℃ 半度 bit */
    private const val HALF_DEGREE_BIT = 0x10
    /** TCL 机型标识 bit */
    private const val IS_TCL_BIT = 0x01

    private const val MODE_HEAT = 1
    private const val MODE_DRY = 2
    private const val MODE_COOL = 3
    private const val MODE_FAN = 7
    private const val MODE_AUTO = 8

    /** 冷启动默认 14 字节状态（开机、制冷 24℃ 等） */
    private val DEFAULT_STATE = byteArrayOf(
      0x23, 0xCB.toByte(), 0x26, 0x01, 0x00, 0x24, 0x03, 0x07, 0x40,
      0x00, 0x00, 0x00, 0x00, 0x03,
    )
  }
}
