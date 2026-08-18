package com.universalremote.control.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.universalremote.control.R
import com.universalremote.control.UniversalRemoteApp
import com.universalremote.control.databinding.ActivityMainBinding
import com.universalremote.control.domain.model.ApplianceType
import com.universalremote.control.domain.model.DeviceProfile
import com.universalremote.control.domain.model.TransmitResult
import com.universalremote.control.ui.ac.AcRemoteActivity
import com.universalremote.control.ui.viewModelFactory

/**
 * 应用首页：设备列表、IR 能力门禁、设备 IR 配对流程。
 * 调用链入口：用户从 Launcher 进入 → onCreate。
 */
class MainActivity : AppCompatActivity() {

  /** ViewBinding：主页布局 activity_main */
  private lateinit var binding: ActivityMainBinding

  private val viewModel: MainViewModel by viewModels {
    viewModelFactory {
      MainViewModel((application as UniversalRemoteApp).container.remoteControlRepository)
    }
  }

  /** 设备列表适配器；点击设置回调 viewModel.requestOpenDeviceSettings */
  private val deviceAdapter = DeviceListAdapter { device ->
    viewModel.requestOpenDeviceSettings(device) { connectedDevice ->
      openDeviceSettings(connectedDevice)
    }
  }

  /**
   * 做什么：初始化布局、列表、观察 LiveData、检测 IR。
   * 调用链：系统 → onCreate → checkPhoneCapability。
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.toolbar.title = getString(R.string.home_title)
    binding.rvDevices.layoutManager = LinearLayoutManager(this)
    binding.rvDevices.adapter = deviceAdapter

    binding.btnRetryIr.setOnClickListener {
      if (viewModel.checkPhoneCapability()) {
        showDeviceList()
      }
    }

    viewModel.devices.observe(this) { devices ->
      deviceAdapter.submitList(devices)
    }

    viewModel.connectionFailedDeviceIds.observe(this) { failedIds ->
      deviceAdapter.submitConnectionFailures(failedIds)
    }

    viewModel.pairingRequest.observe(this) { device ->
      if (device != null) {
        showPairingDialog(device)
      }
    }

    viewModel.hasIrEmitter.observe(this) { hasIr ->
      if (hasIr) {
        showDeviceList()
      } else {
        showIrGate()
      }
    }

    viewModel.checkPhoneCapability()
  }

  /**
   * 做什么：从后台返回时刷新 IR 状态。
   * 调用链：系统 onResume → refreshIrStatus。
   */
  override fun onResume() {
    super.onResume()
    viewModel.refreshIrStatus()
  }

  /** 显示设备列表区域，隐藏无 IR 提示。 */
  private fun showDeviceList() {
    binding.panelIrGate.visibility = View.GONE
    binding.panelDeviceList.visibility = View.VISIBLE
  }

  /** 显示无红外硬件提示页，隐藏列表。 */
  private fun showIrGate() {
    binding.panelIrGate.visibility = View.VISIBLE
    binding.panelDeviceList.visibility = View.GONE
    binding.tvIrGateMessage.text = getString(R.string.no_ir_emitter)
  }

  /**
   * 做什么：首次配对第一步对话框——发送探测 IR。
   * 调用链：pairingRequest LiveData → 本方法 → sendConnectionProbe。
   * 修改指引：参数 [device] 为待配对设备。
   */
  private fun showPairingDialog(device: DeviceProfile) {
    viewModel.dismissPairingRequest()

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.device_pairing_title)
      .setMessage(R.string.device_pairing_message)
      .setPositiveButton(R.string.device_pairing_probe) { _, _ ->
        when (viewModel.sendConnectionProbe(device)) {
          is TransmitResult.Success -> showPairingConfirmDialog(device)
          is TransmitResult.Failure -> viewModel.rejectDevicePairing(device)
        }
      }
      .setNegativeButton(android.R.string.cancel) { _, _ ->
        viewModel.rejectDevicePairing(device)
      }
      .setOnCancelListener {
        viewModel.rejectDevicePairing(device)
      }
      .show()
  }

  /**
   * 做什么：配对第二步——用户确认空调是否有响应。
   * 调用链：探测发送成功 → confirmDevicePairing / rejectDevicePairing。
   * 修改指引：参数 [device]。
   */
  private fun showPairingConfirmDialog(device: DeviceProfile) {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.device_pairing_confirm_title)
      .setMessage(R.string.device_pairing_confirm_message)
      .setPositiveButton(R.string.device_pairing_responded) { _, _ ->
        viewModel.confirmDevicePairing(device) { connectedDevice ->
          openDeviceSettings(connectedDevice)
        }
      }
      .setNegativeButton(R.string.device_pairing_no_response) { _, _ ->
        viewModel.rejectDevicePairing(device)
      }
      .setOnCancelListener {
        viewModel.rejectDevicePairing(device)
      }
      .show()
  }

  /**
   * 做什么：按家电类型跳转对应遥控 Activity。
   * 调用链：配对成功 / 已连接设备点设置 → 本方法。
   * 修改指引：参数 [device]；新增类型时在 when 加分支与新 Activity。
   */
  private fun openDeviceSettings(device: DeviceProfile) {
    when (device.applianceType) {
      ApplianceType.AIR_CONDITIONER -> {
        startActivity(
          Intent(this, AcRemoteActivity::class.java).apply {
            putExtra(AcRemoteActivity.EXTRA_DEVICE_ID, device.id)
            putExtra(AcRemoteActivity.EXTRA_DEVICE_NAME, device.name)
          },
        )
      }
    }
  }
}
