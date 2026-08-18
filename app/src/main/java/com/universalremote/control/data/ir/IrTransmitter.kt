package com.universalremote.control.data.ir

/**
 * 红外发射抽象接口。
 * 职责：隔离硬件；测试时可 mock，未来可接外接 IR 模块实现。
 */
interface IrTransmitter {

  fun hasIrEmitter(): Boolean

  fun transmit(pattern: IrPattern): Result<Unit>
}

/**
 * 红外波形：载波频率 + 高低电平时序（微秒）。
 */
data class IrPattern(
  /** 载波频率（Hz），TCL 空调通常为 38000 */
  val carrierFrequencyHz: Int,
  /** 交替的高低电平持续时间序列（微秒），由编码器生成 */
  val timingsMicros: IntArray,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as IrPattern
    if (carrierFrequencyHz != other.carrierFrequencyHz) return false
    return timingsMicros.contentEquals(other.timingsMicros)
  }

  override fun hashCode(): Int {
    var result = carrierFrequencyHz
    result = 31 * result + timingsMicros.contentHashCode()
    return result
  }
}
