package com.universalremote.control.data.preference

import android.content.Context
import com.universalremote.control.domain.model.AcFanSpeed
import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState

/**
 * 使用 SharedPreferences 持久化 [AcState]。
 */
class AcStatePreferences(context: Context) : AcStateStore {

  /** SharedPreferences 实例，文件名 [PREFS_NAME] */
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun load(): AcState? {
    if (!prefs.contains(KEY_POWER)) return null

    val mode = runCatching {
      AcMode.valueOf(prefs.getString(KEY_MODE, AcMode.COOL.name)!!)
    }.getOrDefault(AcMode.COOL)

    val fanSpeed = runCatching {
      AcFanSpeed.valueOf(prefs.getString(KEY_FAN, AcFanSpeed.AUTO.name)!!)
    }.getOrDefault(AcFanSpeed.AUTO)

    return AcState(
      powerOn = prefs.getBoolean(KEY_POWER, true),
      mode = mode,
      temperatureCelsius = prefs.getFloat(KEY_TEMP, 24f),
      fanSpeed = fanSpeed,
      lightOn = prefs.getBoolean(KEY_LIGHT, true),
    )
  }

  override fun save(state: AcState) {
    prefs.edit()
      .putBoolean(KEY_POWER, state.powerOn)
      .putString(KEY_MODE, state.mode.name)
      .putFloat(KEY_TEMP, state.temperatureCelsius)
      .putString(KEY_FAN, state.fanSpeed.name)
      .putBoolean(KEY_LIGHT, state.lightOn)
      .apply()
  }

  companion object {
    private const val PREFS_NAME = "ac_state_prefs"
    /** 电源开关 */
    private const val KEY_POWER = "power_on"
    /** 工作模式（AcMode 枚举名） */
    private const val KEY_MODE = "mode"
    /** 设定温度（℃） */
    private const val KEY_TEMP = "temperature_celsius"
    /** 风速（AcFanSpeed 枚举名） */
    private const val KEY_FAN = "fan_speed"
    /** 面板灯光状态 */
    private const val KEY_LIGHT = "light_on"
  }
}
