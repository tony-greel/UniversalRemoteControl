package com.universalremote.control

import android.app.Application
import com.universalremote.control.core.AppContainer

/**
 * 应用全局入口。
 * 职责：在进程启动时创建依赖容器，供各 Activity 通过 `(application as UniversalRemoteApp).container` 获取 Repository。
 */
class UniversalRemoteApp : Application() {

  /** 全局依赖容器；由 [onCreate] 初始化，外部只读。 */
  lateinit var container: AppContainer
    private set

  /**
   * 做什么：Application 生命周期回调，创建 [AppContainer]。
   * 有什么用：整个 App 依赖树的根；所有 ViewModel 通过此处拿到 [com.universalremote.control.domain.repository.RemoteControlRepository]。
   * 调用链：系统启动 App → Application.onCreate() → AppContainer(context)。
   * 修改指引：一般无需改；若换 Hilt/Koin，可在此注册模块后删除手动 container。
   */
  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
