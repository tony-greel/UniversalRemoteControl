package com.universalremote.control.data.protocol.tcl

import com.universalremote.control.data.ir.IrPattern
import com.universalremote.control.domain.model.AcState

/**
 * 灯光独立红外码候选库（方式二/混合模式）。
 */
object TclLightPatternLibrary {

  /**
   * 一条灯光试码候选。
   * @param id 持久化与 patternFor 分支用的唯一键
   * @param label UI 展示用中文名
   */
  data class Candidate(
    val id: String,
    val label: String,
  )

  /** 全部试码候选（顺序影响 nextDiscreteCandidate 轮询） */
  val candidates: List<Candidate> = listOf(
    Candidate("special_type2_0x40", "特殊帧 Type2 + 0x40"),
    Candidate("byte5_0x24_0x64", "Byte5: 0x24开 / 0x64关"),
    Candidate("bit02_in_frame", "状态帧 bit 0x02"),
    Candidate("bit40_in_frame", "状态帧 bit 0x40"),
    Candidate("bit02_inverted_frame", "状态帧 bit 0x02 反向"),
    Candidate("bit40_inverted_frame", "状态帧 bit 0x40 反向"),
  )

  fun allCandidateIds(): List<String> = candidates.map { it.id }

  fun patternFor(candidateId: String, lightOn: Boolean, acState: AcState): IrPattern? {
    return when (candidateId) {
      "special_type2_0x40" -> encodeRaw(specialType2LightFrame(lightOn))
      "byte5_0x24_0x64" -> encodeRaw(byte5LightFrame(lightOn, acState))
      "bit02_in_frame" -> encodeFromRemote(acState, lightOn, bit = 0x02, inverted = false)
      "bit40_in_frame" -> encodeFromRemote(acState, lightOn, bit = 0x40, inverted = false)
      "bit02_inverted_frame" -> encodeFromRemote(acState, lightOn, bit = 0x02, inverted = true)
      "bit40_inverted_frame" -> encodeFromRemote(acState, lightOn, bit = 0x40, inverted = true)
      else -> null
    }
  }

  fun labelFor(id: String?): String {
    if (id == null) return "未保存"
    return candidates.firstOrNull { it.id == id }?.label ?: id
  }

  private fun encodeFromRemote(
    acState: AcState,
    lightOn: Boolean,
    bit: Int,
    inverted: Boolean,
  ): IrPattern {
    val remote = Tcl112AcRemote()
    remote.apply(acState.copy(lightOn = lightOn), bit, inverted)
    return remote.buildPattern()
  }

  private fun encodeRaw(state: ByteArray): IrPattern {
    val copy = state.copyOf()
    copy[copy.lastIndex] = Tcl112AcEncoder.calcChecksum(copy)
    return Tcl112AcEncoder.encode(copy)
  }

  private fun specialType2LightFrame(lightOn: Boolean): ByteArray {
    val byte5 = if (lightOn) 0x24 else 0x40 // Type2 帧 byte5 开/关取值
    return byteArrayOf(
      0x23, 0xCB.toByte(), 0x26, 0x02, 0x00, byte5.toByte(), 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    )
  }

  private fun byte5LightFrame(lightOn: Boolean, acState: AcState): ByteArray {
    val remote = Tcl112AcRemote()
    remote.apply(acState.copy(lightOn = lightOn))
    val state = remote.exportState()
    state[5] = if (lightOn) 0x24 else 0x64.toByte() // 0x24 亮 / 0x64 灭
    return state
  }
}
