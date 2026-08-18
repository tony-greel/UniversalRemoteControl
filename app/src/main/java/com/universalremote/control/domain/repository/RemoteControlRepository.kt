package com.universalremote.control.domain.repository

import com.universalremote.control.data.ir.IrTransmitter
import com.universalremote.control.data.preference.AcStateStore
import com.universalremote.control.data.preference.DevicePairingStore
import com.universalremote.control.data.preference.LightControlConfigStore
import com.universalremote.control.data.protocol.tcl.Tcl112AcRemote
import com.universalremote.control.data.protocol.tcl.TclLightPatternLibrary
import com.universalremote.control.domain.model.AcFanSpeed
import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState
import com.universalremote.control.domain.model.ApplianceType
import com.universalremote.control.domain.model.DeviceConnectionResult
import com.universalremote.control.domain.model.DeviceProfile
import com.universalremote.control.domain.model.LightBitVariant
import com.universalremote.control.domain.model.LightControlConfig
import com.universalremote.control.domain.model.LightControlMethod
import com.universalremote.control.domain.model.RemoteAction
import com.universalremote.control.domain.model.TransmitResult

/**
 * 遥控指令编排层（业务中枢）。
 * 职责：维护 acState / lightConfig 内存状态、调用协议编码、IR 发射、持久化。
 * 调用方：MainViewModel、AcRemoteViewModel。
 */
class RemoteControlRepository(
  /** 红外发射器（硬件或测试替身） */
  private val irTransmitter: IrTransmitter,
  /** TCL112 协议状态机（维护 14 字节并编码） */
  private val tcl112AcRemote: Tcl112AcRemote,
  /** 灯光实验配置读写 */
  private val lightControlPreferences: LightControlConfigStore,
  /** 空调逻辑状态读写 */
  private val acStateStore: AcStateStore,
  /** 设备 IR 配对状态读写 */
  private val devicePairingStore: DevicePairingStore,
) {

  /** 当前空调逻辑状态（内存；每次变更会 persist） */
  private var acState: AcState = loadPersistedAcState()
  /** 当前灯光实验配置（内存；变更会 persist） */
  private var lightConfig: LightControlConfig = lightControlPreferences.load()

  /**
   * 做什么：委托 IrTransmitter 检测红外硬件。
   * 调用链：MainViewModel.checkPhoneCapability → 本方法。
   */
  fun hasIrEmitter(): Boolean = irTransmitter.hasIrEmitter()

  /**
   * 做什么：判断能否「打开」该设备遥控页（有 IR + 已配对 + 类型支持）。
   * 调用链：MainViewModel.requestOpenDeviceSettings。
   * 修改指引：参数 [device]；新增家电类型时在 when(applianceType) 扩展。
   */
  fun checkDeviceConnection(device: DeviceProfile): DeviceConnectionResult {
    if (!hasIrEmitter()) {
      return DeviceConnectionResult.Failed(NO_IR_EMITTER_MESSAGE)
    }
    if (!devicePairingStore.isPaired(device.id)) {
      return DeviceConnectionResult.Failed(DEVICE_NOT_PAIRED_MESSAGE)
    }
    return when (device.applianceType) {
      ApplianceType.AIR_CONDITIONER -> DeviceConnectionResult.Connected
    }
  }

  /**
   * 做什么：查询设备是否已 IR 配对。
   * 调用链：MainViewModel.requestOpenDeviceSettings。
   * 修改指引：参数 [deviceId]。
   */
  fun isDevicePaired(deviceId: String): Boolean = devicePairingStore.isPaired(deviceId)

  /**
   * 做什么：发送当前 acState 对应的红外帧，供用户观察空调是否响应。
   * 调用链：MainActivity 配对对话框 → MainViewModel.sendConnectionProbe。
   * 修改指引：参数 [device]；不改 acState，只发射当前状态帧。
   */
  fun sendConnectionProbe(device: DeviceProfile): TransmitResult {
    if (!hasIrEmitter()) {
      return TransmitResult.Failure(NO_IR_EMITTER_MESSAGE)
    }
    return when (device.applianceType) {
      ApplianceType.AIR_CONDITIONER -> {
        val pattern = tcl112AcRemote.apply(acState)
        transmit(pattern)
      }
    }
  }

  /**
   * 做什么：标记设备已配对。
   * 调用链：MainViewModel.confirmDevicePairing。
   * 修改指引：参数 [deviceId]。
   */
  fun confirmDevicePairing(deviceId: String) {
    devicePairingStore.markPaired(deviceId)
  }

  /**
   * 做什么：清除设备配对记录。
   * 调用链：MainViewModel.rejectDevicePairing。
   * 修改指引：参数 [deviceId]。
   */
  fun clearDevicePairing(deviceId: String) {
    devicePairingStore.clearPairing(deviceId)
  }

  /**
   * 做什么：返回首页预设设备列表。
   * 修改指引：改 companion PRESET_DEVICES，不要改方法体。
   */
  fun getPresetDevices(): List<DeviceProfile> = PRESET_DEVICES

  /** 返回当前空调逻辑状态副本。调用链：ViewModel 初始化 / dispatch 后刷新 UI。 */
  fun getAcState(): AcState = acState

  /** 返回当前灯光实验配置。调用链：AcRemoteViewModel / renderLightConfig。 */
  fun getLightControlConfig(): LightControlConfig = lightConfig

  /**
   * 做什么：切换灯光控制方式并持久化。
   * 调用链：AcRemoteViewModel.setLightControlMethod。
   * 修改指引：参数 [method] 为 LightControlMethod 枚举。
   */
  fun setLightControlMethod(method: LightControlMethod) {
    lightConfig = lightConfig.copy(method = method)
    persistLightConfig()
  }

  /**
   * 做什么：切换状态位灯光变体并持久化。
   * 调用链：AcRemoteViewModel.setLightBitVariant。
   * 修改指引：参数 [variant] 的 bitMask/invertedLogic。
   */
  fun setLightBitVariant(variant: LightBitVariant) {
    lightConfig = lightConfig.copy(bitVariant = variant)
    persistLightConfig()
  }

  /**
   * 做什么：离散候选索引 +1 循环，并持久化。
   * 调用链：AcRemoteViewModel.nextDiscreteCandidate。
   * 修改指引：候选总数由 TclLightPatternLibrary.candidates.size 决定。
   */
  fun nextDiscreteCandidate(): LightControlConfig {
    val next = (lightConfig.discreteCandidateIndex + 1) % TclLightPatternLibrary.candidates.size
    lightConfig = lightConfig.copy(discreteCandidateIndex = next)
    persistLightConfig()
    return lightConfig
  }

  /**
   * 做什么：把当前候选 id 记为「有效关灯码」。
   * 调用链：AcRemoteViewModel.saveCurrentCandidateAsLightOff。
   */
  fun saveCurrentCandidateAsLightOff() {
    val id = currentCandidateId() ?: return
    lightConfig = lightConfig.copy(savedLightOffPatternId = id)
    persistLightConfig()
  }

  /**
   * 做什么：把当前候选 id 记为「有效开灯码」。
   * 调用链：AcRemoteViewModel.saveCurrentCandidateAsLightOn。
   */
  fun saveCurrentCandidateAsLightOn() {
    val id = currentCandidateId() ?: return
    lightConfig = lightConfig.copy(savedLightOnPatternId = id)
    persistLightConfig()
  }

  /**
   * 做什么：处理用户遥控动作（电源/温度/模式/风速/灯光），更新状态并发射红外。
   * 有什么用：空调遥控页绝大部分按钮的入口。
   * 调用链：AcRemoteViewModel.sendAction → 本方法 → Tcl112AcRemote / sendLight → transmit。
   * 修改指引：参数 [action]；新增动作改 RemoteAction 与本 when 分支；温度步进改 TEMP coerce。
   */
  fun sendAcAction(action: RemoteAction): TransmitResult {
    val isLightAction = action == RemoteAction.LIGHT_OFF || action == RemoteAction.LIGHT_ON
    if (isLightAction) {
      val newState = when (action) {
        RemoteAction.LIGHT_OFF -> acState.copy(lightOn = false)
        RemoteAction.LIGHT_ON -> acState.copy(lightOn = true)
        else -> acState
      }
      return commitAcStateAfterTransmit(newState) { sendLight(newState) }
    }

    val newState = when (action) {
      RemoteAction.POWER_TOGGLE -> acState.copy(powerOn = !acState.powerOn)
      RemoteAction.TEMP_UP -> acState.copy(
        temperatureCelsius = (acState.temperatureCelsius + 0.5f).coerceAtMost(31f),
      )
      RemoteAction.TEMP_DOWN -> acState.copy(
        temperatureCelsius = (acState.temperatureCelsius - 0.5f).coerceAtLeast(16f),
      )
      RemoteAction.MODE_COOL -> acState.copy(mode = AcMode.COOL)
      RemoteAction.MODE_HEAT -> acState.copy(mode = AcMode.HEAT)
      RemoteAction.MODE_AUTO -> acState.copy(mode = AcMode.AUTO)
      RemoteAction.MODE_FAN -> acState.copy(mode = AcMode.FAN)
      RemoteAction.FAN_AUTO -> acState.copy(fanSpeed = AcFanSpeed.AUTO)
      RemoteAction.FAN_LOW -> acState.copy(fanSpeed = AcFanSpeed.LOW)
      RemoteAction.FAN_MED -> acState.copy(fanSpeed = AcFanSpeed.MED)
      RemoteAction.FAN_HIGH -> acState.copy(fanSpeed = AcFanSpeed.HIGH)
      RemoteAction.LIGHT_OFF, RemoteAction.LIGHT_ON -> acState
    }

    val pattern = tcl112AcRemote.apply(newState)
    return commitAcStateAfterTransmit(newState) { transmit(pattern) }
  }

  /**
   * 做什么：发送当前离散候选独立码（方式二试码）。
   * 调用链：AcRemoteViewModel.sendDiscreteCandidate。
   * 修改指引：参数 [lightOn] 决定开/关与 acState.lightOn 同步。
   */
  fun sendDiscreteCandidate(lightOn: Boolean): TransmitResult {
    val candidateId = currentCandidateId()
      ?: return TransmitResult.Failure("无可用候选码")
    val pattern = TclLightPatternLibrary.patternFor(candidateId, lightOn, acState)
      ?: return TransmitResult.Failure("候选码不存在")
    val newState = acState.copy(lightOn = lightOn)
    return commitAcStateAfterTransmit(newState) { transmit(pattern) }
  }

  /**
   * 做什么：发送用户已保存的有效独立码。
   * 调用链：AcRemoteViewModel.sendSavedDiscretePattern。
   * 修改指引：参数 [lightOn]；saved id 在 lightConfig.savedLightOff/OnPatternId。
   */
  fun sendSavedDiscretePattern(lightOn: Boolean): TransmitResult {
    val patternId = if (lightOn) {
      lightConfig.savedLightOnPatternId
    } else {
      lightConfig.savedLightOffPatternId
    } ?: return TransmitResult.Failure("尚未保存有效码，请先试码并标记")
    val pattern = TclLightPatternLibrary.patternFor(patternId, lightOn, acState)
      ?: return TransmitResult.Failure("已保存的码无效")
    val newState = acState.copy(lightOn = lightOn)
    return commitAcStateAfterTransmit(newState) { transmit(pattern) }
  }

  /** UI 显示当前候选标签。调用链：AcRemoteActivity renderLightConfig。 */
  fun currentCandidateLabel(): String {
    return currentCandidateId()?.let { TclLightPatternLibrary.labelFor(it) } ?: "-"
  }

  /** 混合模式轮询顺序摘要（状态位 + 全部候选）。调用链：tvHybridSummary。 */
  fun hybridAttemptSummary(): String {
    return buildList {
      add(lightConfig.bitVariant.label)
      addAll(TclLightPatternLibrary.allCandidateIds().map { TclLightPatternLibrary.labelFor(it) })
    }.joinToString(" -> ")
  }

  /**
   * 做什么：按 lightConfig.method 生成灯光红外并发射（不直接改 acState，由调用方 commit）。
   * 调用链：sendAcAction(LIGHT_*) → commitAcStateAfterTransmit → 本方法。
   * 修改指引：参数 [newState] 含目标 lightOn；分支逻辑改 LightControlMethod。
   */
  private fun sendLight(newState: AcState): TransmitResult {
    val lightOn = newState.lightOn
    return when (lightConfig.method) {
      LightControlMethod.STATE_BIT_VARIANT -> {
        val variant = lightConfig.bitVariant
        val pattern = tcl112AcRemote.apply(
          newState,
          variant.bitMask,
          variant.invertedLogic,
        )
        transmit(pattern)
      }
      LightControlMethod.DISCRETE_PATTERN -> {
        val savedId = if (lightOn) {
          lightConfig.savedLightOnPatternId
        } else {
          lightConfig.savedLightOffPatternId
        }
        val patternId = savedId ?: currentCandidateId()
          ?: return TransmitResult.Failure("请先选择候选码或保存有效码")
        val pattern = TclLightPatternLibrary.patternFor(patternId, lightOn, acState)
          ?: return TransmitResult.Failure("红外码生成失败")
        transmit(pattern)
      }
      LightControlMethod.HYBRID_ALL -> sendHybridLight(newState)
    }
  }

  /**
   * 做什么：混合模式下一次发送多种灯光策略（状态位 + 各独立候选）。
   * 调用链：sendLight HYBRID_ALL。
   * 修改指引：参数 [newState] 含目标 lightOn；attempts 列表决定轮询内容。
   */
  private fun sendHybridLight(newState: AcState): TransmitResult {
    val lightOn = newState.lightOn
    val attempts = buildList {
      val variant = lightConfig.bitVariant
      add(
        "状态位:${variant.label}" to tcl112AcRemote.apply(
          newState,
          variant.bitMask,
          variant.invertedLogic,
        ),
      )
      TclLightPatternLibrary.allCandidateIds().forEach { id ->
        TclLightPatternLibrary.patternFor(id, lightOn, acState)?.let { pattern ->
          add("独立码:${TclLightPatternLibrary.labelFor(id)}" to pattern)
        }
      }
    }

    if (attempts.isEmpty()) {
      return TransmitResult.Failure("没有可用的混合轮询策略")
    }

    var lastError: String? = null // 记录最后一次发射失败原因
    var anySuccess = false // 是否至少有一种策略发射成功
    for ((_, pattern) in attempts) {
      when (val result = transmit(pattern)) {
        is TransmitResult.Success -> {
          anySuccess = true
        }
        is TransmitResult.Failure -> {
          lastError = result.message
        }
      }
    }
    return if (anySuccess) {
      TransmitResult.Success
    } else {
      TransmitResult.Failure(lastError ?: "混合轮询发送失败")
    }
  }

  /** 根据 discreteCandidateIndex 取当前候选 id。 */
  private fun currentCandidateId(): String? {
    return TclLightPatternLibrary.candidates.getOrNull(lightConfig.discreteCandidateIndex)?.id
  }

  /**
   * 做什么：调用 IrTransmitter 并包装为 TransmitResult。
   * 调用链：所有发射路径最终到本方法。
   * 修改指引：参数 [pattern] 为已编码波形。
   */
  private fun transmit(pattern: com.universalremote.control.data.ir.IrPattern): TransmitResult {
    return irTransmitter.transmit(pattern).fold(
      onSuccess = { TransmitResult.Success },
      onFailure = { TransmitResult.Failure(it.message ?: "Unknown error") },
    )
  }

  /**
   * 做什么：从 AcStateStore 加载状态并同步 Tcl112AcRemote。
   * 调用链：Repository 构造时。
   */
  private fun loadPersistedAcState(): AcState {
    val saved = acStateStore.load()
    return if (saved != null) {
      tcl112AcRemote.restore(saved)
      saved
    } else {
      tcl112AcRemote.snapshot()
    }
  }

  /**
   * 做什么：执行红外发射，仅成功时更新并持久化 [newState]；失败时恢复协议层字节状态。
   * 有什么用：避免 IR 发送失败时 App 状态与真机/持久化不一致（回家场景 P0）。
   * 调用链：sendAcAction / sendDiscreteCandidate / sendSavedDiscretePattern → 本方法。
   * 修改指引：参数 [newState] 为待发目标状态；[transmitBlock] 应返回 TransmitResult。
   */
  private fun commitAcStateAfterTransmit(
    newState: AcState,
    transmitBlock: () -> TransmitResult,
  ): TransmitResult {
    return when (val result = transmitBlock()) {
      is TransmitResult.Success -> {
        updateAcState(newState)
        TransmitResult.Success
      }
      is TransmitResult.Failure -> {
        tcl112AcRemote.restore(acState) // 编码过程可能改动协议层，失败时回滚
        result
      }
    }
  }

  /** 持久化 acState 到 SharedPreferences。 */
  private fun persistAcState() {
    acStateStore.save(acState)
  }

  /** 持久化 lightConfig。 */
  private fun persistLightConfig() {
    lightControlPreferences.save(lightConfig)
  }

  /**
   * 做什么：更新内存 acState 并立即持久化。
   * 调用链：sendAcAction / sendDiscreteCandidate 等。
   * 修改指引：参数 [newState] 为完整 AcState。
   */
  private fun updateAcState(newState: AcState) {
    acState = newState
    persistAcState()
  }

  companion object {
    /** 无红外硬件时的用户提示 */
    private const val NO_IR_EMITTER_MESSAGE = "当前设备不支持红外发射，请使用带 IR 功能的手机"
    /** 未完成 IR 配对时的用户提示 */
    private const val DEVICE_NOT_PAIRED_MESSAGE = "未检测到可用空调，请先完成连接探测"

    /** 首页设备列表；新增设备改此列表。 */
    private val PRESET_DEVICES = listOf(
      DeviceProfile(
        id = "tcl_ac_default",
        name = "TCL 空调",
        description = "TCL112AC 协议 · 支持关闭面板灯光",
        applianceType = ApplianceType.AIR_CONDITIONER,
        brand = "TCL",
      ),
    )
  }
}
