package com.universalremote.control.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.universalremote.control.domain.model.DeviceConnectionResult
import com.universalremote.control.domain.model.DeviceProfile
import com.universalremote.control.domain.model.TransmitResult
import com.universalremote.control.domain.repository.RemoteControlRepository

/**
 * 首页（设备列表）ViewModel。
 * 职责：IR 能力检测、设备列表、配对流程、连接失败标记；不直接操作 View。
 */
class MainViewModel(
  /** 遥控业务中枢，来自 AppContainer */
  private val repository: RemoteControlRepository,
) : ViewModel() {

  /** 内部可变：预设设备列表 */
  private val _devices = MutableLiveData(repository.getPresetDevices())
  /** 预设设备列表；观察方：MainActivity → DeviceListAdapter。 */
  val devices: LiveData<List<DeviceProfile>> = _devices

  /** 内部可变：手机是否具备红外发射器 */
  private val _hasIrEmitter = MutableLiveData(repository.hasIrEmitter())
  /** 对外只读：IR 能力状态 */
  val hasIrEmitter: LiveData<Boolean> = _hasIrEmitter

  /** 内部可变：连接/配对失败的设备 id 集合 */
  private val _connectionFailedDeviceIds = MutableLiveData<Set<String>>(emptySet())
  /** 对外只读：失败设备 id，驱动列表「重试」文案 */
  val connectionFailedDeviceIds: LiveData<Set<String>> = _connectionFailedDeviceIds

  /** 内部可变：待弹出配对对话框的设备；null 表示无待处理 */
  private val _pairingRequest = MutableLiveData<DeviceProfile?>(null)
  /** 非 null 时 MainActivity 弹出配对探测对话框。 */
  val pairingRequest: LiveData<DeviceProfile?> = _pairingRequest

  /**
   * 做什么：从 Repository 重新读取 IR 能力（如从其他 App 返回后）。
   * 调用链：MainActivity.onResume。
   */
  fun refreshIrStatus() {
    _hasIrEmitter.value = repository.hasIrEmitter()
  }

  /**
   * 做什么：检测红外并更新 [hasIrEmitter] LiveData。
   * 调用链：MainActivity.onCreate / 重试按钮。
   * 返回值：是否有 IR（用于同步逻辑）。
   */
  fun checkPhoneCapability(): Boolean {
    val ready = repository.hasIrEmitter() // 是否具备 IR 硬件
    _hasIrEmitter.value = ready
    return ready
  }

  /**
   * 做什么：用户点「设置」时检查连接；已连接则 [onConnected]，未配对则触发配对流程。
   * 调用链：DeviceListAdapter → 本方法 → Repository.checkDeviceConnection。
   * 修改指引：参数 [device]；回调 [onConnected] 里 Activity 跳转遥控页。
   */
  fun requestOpenDeviceSettings(
    device: DeviceProfile,
    onConnected: (DeviceProfile) -> Unit,
  ) {
    when (val result = repository.checkDeviceConnection(device)) {
      is DeviceConnectionResult.Connected -> {
        clearConnectionFailure(device.id)
        onConnected(device)
      }
      is DeviceConnectionResult.Failed -> {
        if (!repository.isDevicePaired(device.id) && repository.hasIrEmitter()) {
          _pairingRequest.value = device
        } else {
          markConnectionFailure(device.id)
        }
      }
    }
  }

  /**
   * 做什么：发送 IR 探测帧。
   * 调用链：MainActivity 配对对话框「发送探测」。
   * 修改指引：参数 [device]。
   */
  fun sendConnectionProbe(device: DeviceProfile): TransmitResult {
    return repository.sendConnectionProbe(device)
  }

  /**
   * 做什么：用户确认空调有响应，写入配对并进入设置页。
   * 调用链：配对确认对话框 → [onConnected]。
   * 修改指引：参数 [device]、[onConnected]。
   */
  fun confirmDevicePairing(device: DeviceProfile, onConnected: (DeviceProfile) -> Unit) {
    repository.confirmDevicePairing(device.id)
    clearConnectionFailure(device.id)
    _pairingRequest.value = null
    onConnected(device)
  }

  /**
   * 做什么：用户取消或空调无响应，清除配对并标记失败。
   * 调用链：配对对话框取消/无响应。
   * 修改指引：参数 [device]。
   */
  fun rejectDevicePairing(device: DeviceProfile) {
    repository.clearDevicePairing(device.id)
    markConnectionFailure(device.id)
    _pairingRequest.value = null
  }

  /**
   * 做什么：关闭配对对话框时清空请求（避免重复弹窗）。
   * 调用链：MainActivity.showPairingDialog 开头。
   */
  fun dismissPairingRequest() {
    _pairingRequest.value = null
  }

  /** 将设备 id 加入连接失败集合，刷新列表按钮文案。 */
  private fun markConnectionFailure(deviceId: String) {
    _connectionFailedDeviceIds.value =
      _connectionFailedDeviceIds.value.orEmpty() + deviceId
  }

  /** 从失败集合移除设备 id。 */
  private fun clearConnectionFailure(deviceId: String) {
    _connectionFailedDeviceIds.value =
      _connectionFailedDeviceIds.value.orEmpty() - deviceId
  }
}
