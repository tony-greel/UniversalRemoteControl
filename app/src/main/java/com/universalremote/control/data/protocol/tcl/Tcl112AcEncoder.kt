package com.universalremote.control.data.protocol.tcl

import com.universalremote.control.data.ir.IrPattern

/**
 * TCL 112-bit 空调红外协议：将 14 字节状态数组编码为载波时序。
 * 参考 IRremoteESP8266 ir_Tcl.cpp。无状态，纯编码工具。
 */
object Tcl112AcEncoder {

  /** TCL112AC 状态帧固定 14 字节 */
  private const val STATE_LENGTH = 14
  /** 红外载波频率（Hz），常见 38kHz */
  private const val CARRIER_HZ = 38_000

  /** 帧头 mark 时长（μs） */
  private const val HDR_MARK = 3000
  /** 帧头 space 时长（μs） */
  private const val HDR_SPACE = 1650
  /** 每个 bit 的 mark 时长（μs） */
  private const val BIT_MARK = 500
  /** 逻辑 1 的 space 时长（μs） */
  private const val ONE_SPACE = 1050
  /** 逻辑 0 的 space 时长（μs） */
  private const val ZERO_SPACE = 325
  /** 帧尾 mark 时长（μs） */
  private const val FOOTER_MARK = 500
  /** 帧间间隔（μs） */
  private const val GAP = 40_000

  fun encode(state: ByteArray): IrPattern {
    require(state.size == STATE_LENGTH) { "TCL112AC state must be $STATE_LENGTH bytes" }
    val timings = ArrayList<Int>(STATE_LENGTH * 16 + 4)
    timings += HDR_MARK
    timings += HDR_SPACE

    for (byte in state) {
      var mask = 0x80 // 从最高位开始逐 bit 编码
      while (mask != 0) {
        timings += BIT_MARK
        timings += if (byte.toInt() and mask != 0) ONE_SPACE else ZERO_SPACE
        mask = mask shr 1
      }
    }

    timings += FOOTER_MARK
    timings += GAP

    return IrPattern(CARRIER_HZ, timings.toIntArray())
  }

  fun calcChecksum(state: ByteArray, length: Int = STATE_LENGTH): Byte {
    if (length == 0) return 0
    val sumEnd = length - 1 // 校验和不包含最后一字节自身
    val init: Int = if (length > 4 && state[3] == 0x02.toByte()) 0x0F else 0
    var sum = init
    for (i in 0 until sumEnd) {
      sum += state[i].toInt() and 0xFF
    }
    return (sum and 0xFF).toByte()
  }
}
