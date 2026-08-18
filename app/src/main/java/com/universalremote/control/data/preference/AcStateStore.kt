package com.universalremote.control.data.preference

import com.universalremote.control.domain.model.AcState

/**
 * 空调逻辑状态持久化接口。
 * 实现类：[AcStatePreferences]。
 */
interface AcStateStore {

  /** 读取持久化的 [AcState]；无数据时返回 null。调用链：Repository.loadPersistedAcState。 */
  fun load(): AcState?

  /** 保存 [AcState]；调用链：Repository.persistAcState。 */
  fun save(state: AcState)
}
