package com.universalremote.control.domain.model

/**
 * 灯光控制实验相关类型：方式（状态位/独立码/混合）与 bit 变体。
 * 持久化见 [com.universalremote.control.data.preference.LightControlPreferences]。
 */

/** 灯光控制策略（三选一互斥）。 */
enum class LightControlMethod {
  /** 方式一：在 TCL112AC 完整状态帧内切换灯光 bit */
  STATE_BIT_VARIANT,
  /** 方式二：发送预置/已保存的独立红外波形 */
  DISCRETE_PATTERN,
  /** 方式三：一次按键轮询状态位 + 全部独立候选 */
  HYBRID_ALL,
}

/**
 * 状态帧内灯光 bit 变体。
 * @param bitMask 要操作的 bit 掩码（0x02 或 0x40）
 * @param invertedLogic 是否反转 bit 语义（部分老机型需要）
 * @param label UI 显示用中文标签
 */
enum class LightBitVariant(val bitMask: Int, val invertedLogic: Boolean, val label: String) {
  BIT_0x02(0x02, false, "bit 0x02（默认）"),
  BIT_0x02_INVERTED(0x02, true, "bit 0x02（反向）"),
  BIT_0x40(0x40, false, "bit 0x40（备选）"),
  BIT_0x40_INVERTED(0x40, true, "bit 0x40（反向）"),
}

/**
 * 灯光实验运行时配置（内存 + SharedPreferences）。
 */
data class LightControlConfig(
  /** 当前生效的灯光控制方式 */
  val method: LightControlMethod = LightControlMethod.STATE_BIT_VARIANT,
  /** 方式一下使用的 bit 变体 */
  val bitVariant: LightBitVariant = LightBitVariant.BIT_0x02,
  /** 用户标记的「有效关灯」候选 id（方式二） */
  val savedLightOffPatternId: String? = null,
  /** 用户标记的「有效开灯」候选 id（方式二） */
  val savedLightOnPatternId: String? = null,
  /** 方式二当前试码候选在 candidates 列表中的下标 */
  val discreteCandidateIndex: Int = 0,
)
