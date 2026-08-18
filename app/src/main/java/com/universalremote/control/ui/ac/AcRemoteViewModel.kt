package com.universalremote.control.ui.ac

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.universalremote.control.domain.model.AcState
import com.universalremote.control.domain.model.LightBitVariant
import com.universalremote.control.domain.model.LightControlConfig
import com.universalremote.control.domain.model.LightControlMethod
import com.universalremote.control.domain.model.RemoteAction
import com.universalremote.control.domain.model.TransmitResult
import com.universalremote.control.domain.repository.RemoteControlRepository

/**
 * 空调遥控页 ViewModel。
 * 职责：转发用户操作到 Repository，用 LiveData 驱动 UI 状态与反馈。
 */
class AcRemoteViewModel(
  /** 遥控业务中枢 */
  private val repository: RemoteControlRepository,
) : ViewModel() {

  /** 内部可变：当前空调状态 */
  private val _acState = MutableLiveData(repository.getAcState())
  /** 空调状态（模式/温度/灯光等）；观察方：AcRemoteActivity.renderState。 */
  val acState: LiveData<AcState> = _acState

  private val _lightConfig = MutableLiveData(repository.getLightControlConfig())
  /** 灯光实验配置；观察方：renderLightConfig。 */
  val lightConfig: LiveData<LightControlConfig> = _lightConfig

  /** 内部可变：操作反馈码或错误文案 */
  private val _feedback = MutableLiveData<String?>(null)
  /** 操作反馈：sent / candidate_changed / saved_* / 错误文案。 */
  val feedback: LiveData<String?> = _feedback

  /**
   * 做什么：发送标准遥控动作（电源/温度/模式/风速/灯光面板按钮）。
   * 调用链：AcRemoteActivity.bindActions → Repository.sendAcAction。
   * 修改指引：参数 [action] 为 RemoteAction。
   */
  fun sendAction(action: RemoteAction) {
    dispatch { repository.sendAcAction(action) }
  }

  /**
   * 做什么：切换灯光控制方式（状态位/独立码/混合）。
   * 调用链：灯光方式 Switch → Repository.setLightControlMethod。
   * 修改指引：参数 [method]。
   */
  fun setLightControlMethod(method: LightControlMethod) {
    repository.setLightControlMethod(method)
    refreshLightConfig()
  }

  /**
   * 做什么：切换状态位变体（0x02/0x40 等）。
   * 调用链：RadioGroup → Repository.setLightBitVariant。
   * 修改指引：参数 [variant]。
   */
  fun setLightBitVariant(variant: LightBitVariant) {
    repository.setLightBitVariant(variant)
    refreshLightConfig()
  }

  /**
   * 做什么：发送当前离散候选试码。
   * 调用链：btnCandidateOff/On → Repository.sendDiscreteCandidate。
   * 修改指引：参数 [lightOn]。
   */
  fun sendDiscreteCandidate(lightOn: Boolean) {
    dispatch { repository.sendDiscreteCandidate(lightOn) }
  }

  /**
   * 做什么：发送已保存的有效独立码。
   * 调用链：btnUseSavedOff/On → Repository.sendSavedDiscretePattern。
   * 修改指引：参数 [lightOn]。
   */
  fun sendSavedDiscretePattern(lightOn: Boolean) {
    dispatch { repository.sendSavedDiscretePattern(lightOn) }
  }

  /**
   * 做什么：切换到下一个候选并提示 candidate_changed。
   * 调用链：btnNextCandidate。
   */
  fun nextDiscreteCandidate() {
    repository.nextDiscreteCandidate()
    refreshLightConfig()
    _feedback.value = "candidate_changed"
  }

  /**
   * 做什么：标记当前候选为有效关灯码。
   * 调用链：btnMarkSavedOff。
   */
  fun saveCurrentCandidateAsLightOff() {
    repository.saveCurrentCandidateAsLightOff()
    refreshLightConfig()
    _feedback.value = "saved_off"
  }

  /**
   * 做什么：标记当前候选为有效开灯码。
   * 调用链：btnMarkSavedOn。
   */
  fun saveCurrentCandidateAsLightOn() {
    repository.saveCurrentCandidateAsLightOn()
    refreshLightConfig()
    _feedback.value = "saved_on"
  }

  /** 当前候选显示名；调用链：AcRemoteActivity tvCandidateInfo。 */
  fun currentCandidateLabel(): String = repository.currentCandidateLabel()

  /** 混合模式轮询摘要；调用链：tvHybridSummary。 */
  fun hybridAttemptSummary(): String = repository.hybridAttemptSummary()

  /** 已保存关灯码标签。 */
  fun savedLightOffLabel(): String {
    val id = repository.getLightControlConfig().savedLightOffPatternId // 已保存关灯候选 id
    return com.universalremote.control.data.protocol.tcl.TclLightPatternLibrary.labelFor(id)
  }

  /** 已保存开灯码标签。 */
  fun savedLightOnLabel(): String {
    val id = repository.getLightControlConfig().savedLightOnPatternId // 已保存开灯候选 id
    return com.universalremote.control.data.protocol.tcl.TclLightPatternLibrary.labelFor(id)
  }

  /** 清除反馈文案。 */
  fun clearFeedback() {
    _feedback.value = null
  }

  /**
   * 做什么：执行 Repository 发射操作，根据结果刷新 acState/lightConfig/feedback。
   * 调用链：sendAction / sendDiscreteCandidate 等内部。
   * 修改指引：[block] 应返回 TransmitResult。
   */
  private fun dispatch(block: () -> TransmitResult) {
    when (val result = block()) {
      is TransmitResult.Success -> {
        _acState.value = repository.getAcState()
        refreshLightConfig()
        _feedback.value = "sent"
      }
      is TransmitResult.Failure -> {
        _acState.value = repository.getAcState()
        _feedback.value = result.message
      }
    }
  }

  /** 从 Repository 拉取最新 lightConfig 到 LiveData。 */
  private fun refreshLightConfig() {
    _lightConfig.value = repository.getLightControlConfig()
  }
}
