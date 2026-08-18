package com.universalremote.control.data.preference

import android.content.Context
import com.universalremote.control.domain.model.LightBitVariant
import com.universalremote.control.domain.model.LightControlConfig
import com.universalremote.control.domain.model.LightControlMethod

/**
 * 使用 SharedPreferences 持久化 [LightControlConfig]。
 */
class LightControlPreferences(context: Context) : LightControlConfigStore {

  /** SharedPreferences 实例，文件名 [PREFS_NAME] */
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun load(): LightControlConfig {
    val method = runCatching {
      LightControlMethod.valueOf(prefs.getString(KEY_METHOD, LightControlMethod.STATE_BIT_VARIANT.name)!!)
    }.getOrDefault(LightControlMethod.STATE_BIT_VARIANT)

    val variant = runCatching {
      LightBitVariant.valueOf(prefs.getString(KEY_BIT_VARIANT, LightBitVariant.BIT_0x02.name)!!)
    }.getOrDefault(LightBitVariant.BIT_0x02)

    return LightControlConfig(
      method = method,
      bitVariant = variant,
      savedLightOffPatternId = prefs.getString(KEY_SAVED_OFF, null),
      savedLightOnPatternId = prefs.getString(KEY_SAVED_ON, null),
      discreteCandidateIndex = prefs.getInt(KEY_CANDIDATE_INDEX, 0),
    )
  }

  override fun save(config: LightControlConfig) {
    prefs.edit()
      .putString(KEY_METHOD, config.method.name)
      .putString(KEY_BIT_VARIANT, config.bitVariant.name)
      .putString(KEY_SAVED_OFF, config.savedLightOffPatternId)
      .putString(KEY_SAVED_ON, config.savedLightOnPatternId)
      .putInt(KEY_CANDIDATE_INDEX, config.discreteCandidateIndex)
      .apply()
  }

  companion object {
    /** prefs 文件名 */
    private const val PREFS_NAME = "light_control_prefs"
    /** 灯光控制方式（枚举名） */
    private const val KEY_METHOD = "method"
    /** 状态位变体（枚举名） */
    private const val KEY_BIT_VARIANT = "bit_variant"
    /** 已保存关灯候选 id */
    private const val KEY_SAVED_OFF = "saved_light_off_pattern_id"
    /** 已保存开灯候选 id */
    private const val KEY_SAVED_ON = "saved_light_on_pattern_id"
    /** 当前试码候选下标 */
    private const val KEY_CANDIDATE_INDEX = "discrete_candidate_index"
  }
}
