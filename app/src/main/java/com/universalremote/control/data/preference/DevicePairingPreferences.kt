package com.universalremote.control.data.preference

import android.content.Context

/**
 * 记录用户已确认 IR 探测成功的设备 id 集合。
 */
class DevicePairingPreferences(context: Context) : DevicePairingStore {

  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun isPaired(deviceId: String): Boolean {
    return prefs.getStringSet(KEY_PAIRED_IDS, emptySet()).orEmpty().contains(deviceId)
  }

  override fun markPaired(deviceId: String) {
    val updated = prefs.getStringSet(KEY_PAIRED_IDS, emptySet()).orEmpty().toMutableSet()
    updated.add(deviceId)
    prefs.edit().putStringSet(KEY_PAIRED_IDS, updated).apply()
  }

  override fun clearPairing(deviceId: String) {
    val updated = prefs.getStringSet(KEY_PAIRED_IDS, emptySet()).orEmpty().toMutableSet()
    updated.remove(deviceId)
    prefs.edit().putStringSet(KEY_PAIRED_IDS, updated).apply()
  }

  companion object {
    private const val PREFS_NAME = "device_pairing_prefs"
    /** 已配对设备 id 集合（StringSet） */
    private const val KEY_PAIRED_IDS = "paired_device_ids"
  }
}
