package com.universalremote.control.data.preference

import com.universalremote.control.domain.model.LightControlConfig

/**
 * 灯光实验配置的持久化接口。
 * 实现类：[LightControlPreferences]。
 */
interface LightControlConfigStore {

  /** 从本地存储读取配置；调用链：Repository 初始化 / getLightControlConfig。 */
  fun load(): LightControlConfig

  /** 写入配置；调用链：Repository.persistLightConfig。 */
  fun save(config: LightControlConfig)
}
