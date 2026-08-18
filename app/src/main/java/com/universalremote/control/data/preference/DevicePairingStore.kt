package com.universalremote.control.data.preference

/**
 * 设备 IR 配对状态接口（用户确认空调有响应后标记已配对）。
 * 实现类：[DevicePairingPreferences]。
 */
interface DevicePairingStore {

  /** 是否已配对；调用链：Repository.checkDeviceConnection / isDevicePaired。 */
  fun isPaired(deviceId: String): Boolean

  /** 标记已配对；调用链：Repository.confirmDevicePairing。 */
  fun markPaired(deviceId: String)

  /** 清除配对；调用链：Repository.clearDevicePairing / rejectDevicePairing。 */
  fun clearPairing(deviceId: String)
}
