package com.universalremote.control.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager

/**
 * 基于 Android [ConsumerIrManager] 的红外发射实现。
 */
class AndroidIrTransmitter(context: Context) : IrTransmitter {

  /** 系统红外服务；无 IR 硬件时为 null */
  private val irManager: ConsumerIrManager? =
    context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

  override fun hasIrEmitter(): Boolean = irManager?.hasIrEmitter() == true

  override fun transmit(pattern: IrPattern): Result<Unit> {
    val manager = irManager
      ?: return Result.failure(IllegalStateException("ConsumerIrManager unavailable"))

    if (!manager.hasIrEmitter()) {
      return Result.failure(IllegalStateException("No IR emitter on this device"))
    }

    return try {
      manager.transmit(pattern.carrierFrequencyHz, pattern.timingsMicros)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
