package com.universalremote.control.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.universalremote.control.R
import com.universalremote.control.databinding.ItemDeviceBinding
import com.universalremote.control.domain.model.DeviceProfile

/**
 * 首页设备列表 RecyclerView 适配器。
 * 职责：展示 DeviceProfile；点击「设置」回调到 MainActivity。
 */
class DeviceListAdapter(
  /** 用户点击「设置/重试」时的回调 */
  private val onSettingsClick: (DeviceProfile) -> Unit,
) : ListAdapter<DeviceProfile, DeviceListAdapter.DeviceViewHolder>(DiffCallback) {

  /** 当前标记为连接失败的设备 id 集合 */
  private var failedDeviceIds: Set<String> = emptySet()

  /**
   * 做什么：更新连接失败的设备 id 集合并刷新列表。
   * 调用链：MainActivity 观察 connectionFailedDeviceIds。
   * 修改指引：参数 [failedIds] 为 device.id 集合。
   */
  fun submitConnectionFailures(failedIds: Set<String>) {
    if (failedDeviceIds == failedIds) return
    failedDeviceIds = failedIds
    notifyDataSetChanged()
  }

  /**
   * 做什么：创建列表项 ViewHolder。
   * 调用链：RecyclerView 内部。
   */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
    val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return DeviceViewHolder(binding, onSettingsClick)
  }

  /**
   * 做什么：绑定第 [position] 项数据到 Holder。
   * 调用链：RecyclerView 内部。
   */
  override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
    val device = getItem(position)
    holder.bind(device, failedDeviceIds.contains(device.id))
  }

  /**
   * 单条设备列表项。
   */
  class DeviceViewHolder(
    private val binding: ItemDeviceBinding,
    private val onSettingsClick: (DeviceProfile) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    /**
     * 做什么：填充名称、描述，设置按钮文案（设置/重试）与点击。
     * 调用链：onBindViewHolder。
     * 修改指引：参数 [device]、[connectionFailed] 决定按钮文字。
     */
    fun bind(device: DeviceProfile, connectionFailed: Boolean) {
      binding.tvDeviceName.text = device.name
      binding.tvDeviceDesc.text = device.description
      binding.btnSettings.setText(
        if (connectionFailed) { // true：显示「连接失败，需重试」
          R.string.device_settings_retry
        } else {
          R.string.device_settings
        },
      )
      binding.btnSettings.setOnClickListener { onSettingsClick(device) }
    }
  }

  /** ListAdapter 差量比较，避免无效刷新。 */
  private object DiffCallback : DiffUtil.ItemCallback<DeviceProfile>() {
    override fun areItemsTheSame(oldItem: DeviceProfile, newItem: DeviceProfile): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DeviceProfile, newItem: DeviceProfile): Boolean {
      return oldItem == newItem
    }
  }
}
