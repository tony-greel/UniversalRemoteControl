package com.universalremote.control.core

import android.content.Context
import com.universalremote.control.data.ir.AndroidIrTransmitter
import com.universalremote.control.data.preference.AcStatePreferences
import com.universalremote.control.data.preference.DevicePairingPreferences
import com.universalremote.control.data.preference.LightControlPreferences
import com.universalremote.control.data.protocol.tcl.Tcl112AcRemote
import com.universalremote.control.domain.repository.RemoteControlRepository

/**
 * 轻量依赖容器（手动 DI）。
 * 职责：组装 data 层实现并构造唯一的 [RemoteControlRepository]；Compose 迁移时可保持不变，后续可换 Hilt/Koin。
 */
class AppContainer(context: Context) {

  /** Application 级 Context，避免 Activity 泄漏 */
  private val appContext = context.applicationContext
  /** 红外硬件发射实现 */
  private val irTransmitter = AndroidIrTransmitter(appContext)
  /** 灯光实验配置持久化 */
  private val lightControlPreferences = LightControlPreferences(appContext)
  /** 空调逻辑状态持久化 */
  private val acStatePreferences = AcStatePreferences(appContext)
  /** 设备 IR 配对状态持久化 */
  private val devicePairingPreferences = DevicePairingPreferences(appContext)

  /**
   * 遥控业务中枢实例；Activity/ViewModel 通过此对象访问全部遥控能力。
   * 调用链：UniversalRemoteApp.onCreate → AppContainer 构造 → 注入各 Preferences + IrTransmitter + Tcl112AcRemote。
   * 修改指引：新增全局服务（如网络遥控）时在此构造并传入 Repository 构造函数。
   */
  val remoteControlRepository: RemoteControlRepository = RemoteControlRepository(
    irTransmitter = irTransmitter,
    tcl112AcRemote = Tcl112AcRemote(),
    lightControlPreferences = lightControlPreferences,
    acStateStore = acStatePreferences,
    devicePairingStore = devicePairingPreferences,
  )
}
