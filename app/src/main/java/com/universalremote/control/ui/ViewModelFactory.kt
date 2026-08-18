package com.universalremote.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 创建带构造参数的 ViewModel 的工厂辅助函数。
 * 职责：避免为每个 ViewModel 手写 Factory 类。
 */

/**
 * 做什么：返回一个 [ViewModelProvider.Factory]，用 [creator] lambda 创建 ViewModel。
 * 有什么用：MainActivity / AcRemoteActivity 通过 `by viewModels { viewModelFactory { ... } }` 注入 Repository。
 * 调用链：Activity.viewModels → Factory.create → creator()。
 * 修改指引：改 [creator] 闭包内的构造参数即可（通常传入 container.remoteControlRepository）。
 */
inline fun <VM : ViewModel> viewModelFactory(crossinline creator: () -> VM): ViewModelProvider.Factory {
  return object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
  }
}
