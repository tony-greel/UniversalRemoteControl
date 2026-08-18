package com.universalremote.control.ui.ac

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.universalremote.control.R
import com.universalremote.control.UniversalRemoteApp
import com.universalremote.control.data.protocol.tcl.TclLightPatternLibrary
import com.universalremote.control.databinding.ActivityAcRemoteBinding
import com.universalremote.control.domain.model.AcFanSpeed
import com.universalremote.control.domain.model.AcMode
import com.universalremote.control.domain.model.AcState
import com.universalremote.control.domain.model.LightBitVariant
import com.universalremote.control.domain.model.LightControlConfig
import com.universalremote.control.domain.model.LightControlMethod
import com.universalremote.control.domain.model.RemoteAction
import com.universalremote.control.ui.viewModelFactory

/**
 * TCL 空调红外遥控页：电源/温度/模式/风速 + 灯光实验面板。
 * 职责：绑定 View 点击 → ViewModel；观察 LiveData 刷新 UI（含 suppress 防循环回调）。
 */
class AcRemoteActivity : AppCompatActivity() {

  /** ViewBinding：空调遥控页 activity_ac_remote */
  private lateinit var binding: ActivityAcRemoteBinding
  /** 刷新灯光 UI 时屏蔽 Switch/Radio 回调，避免触发二次 setMethod。 */
  private var suppressLightUiCallback = false
  /** 刷新模式 Toggle 时屏蔽，避免 check 触发 sendAction。 */
  private var suppressModeCallback = false
  /** 刷新风速 Toggle 时屏蔽。 */
  private var suppressFanCallback = false

  private val viewModel: AcRemoteViewModel by viewModels {
    viewModelFactory {
      AcRemoteViewModel((application as UniversalRemoteApp).container.remoteControlRepository)
    }
  }

  /**
   * 做什么：inflate 布局、设置标题、绑定点击、订阅 ViewModel。
   * 调用链：MainActivity.startActivity → 本方法。
   * Intent 参数：EXTRA_DEVICE_NAME 用于 toolbar 标题。
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAcRemoteBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME).orEmpty() // 首页传入的设备显示名
    binding.toolbar.title = deviceName.ifBlank { getString(R.string.ac_remote_title) }
    binding.toolbar.setNavigationOnClickListener { finish() }

    bindActions()
    observeViewModel()
  }

  /**
   * 做什么：为所有按钮、Toggle、Switch 注册监听器，映射到 RemoteAction 或灯光 API。
   * 调用链：onCreate；用户点击 → viewModel.sendAction / setLightControlMethod 等。
   * 修改指引：新增按钮在此加 setOnClickListener 与对应 RemoteAction。
   */
  private fun bindActions() {
    binding.btnPower.setOnClickListener { viewModel.sendAction(RemoteAction.POWER_TOGGLE) }
    binding.btnTempUp.setOnClickListener { viewModel.sendAction(RemoteAction.TEMP_UP) }
    binding.btnTempDown.setOnClickListener { viewModel.sendAction(RemoteAction.TEMP_DOWN) }

    binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (suppressModeCallback || !isChecked) return@addOnButtonCheckedListener
      val action = when (checkedId) { // 模式按钮 id → RemoteAction
        R.id.btnModeCool -> RemoteAction.MODE_COOL
        R.id.btnModeHeat -> RemoteAction.MODE_HEAT
        R.id.btnModeAuto -> RemoteAction.MODE_AUTO
        R.id.btnModeFan -> RemoteAction.MODE_FAN
        else -> return@addOnButtonCheckedListener
      }
      viewModel.sendAction(action)
    }

    binding.toggleFan.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (suppressFanCallback || !isChecked) return@addOnButtonCheckedListener
      val action = when (checkedId) { // 风速按钮 id → RemoteAction
        R.id.btnFanAuto -> RemoteAction.FAN_AUTO
        R.id.btnFanLow -> RemoteAction.FAN_LOW
        R.id.btnFanMed -> RemoteAction.FAN_MED
        R.id.btnFanHigh -> RemoteAction.FAN_HIGH
        else -> return@addOnButtonCheckedListener
      }
      viewModel.sendAction(action)
    }

    binding.btnLightOff.setOnClickListener { viewModel.sendAction(RemoteAction.LIGHT_OFF) }
    binding.btnLightOn.setOnClickListener { viewModel.sendAction(RemoteAction.LIGHT_ON) }

    binding.switchMethodStateBit.setOnCheckedChangeListener { _, checked ->
      if (suppressLightUiCallback) return@setOnCheckedChangeListener
      if (checked) {
        suppressLightUiCallback = true
        binding.switchMethodDiscrete.isChecked = false
        binding.switchMethodHybrid.isChecked = false
        suppressLightUiCallback = false
        viewModel.setLightControlMethod(LightControlMethod.STATE_BIT_VARIANT)
      } else if (!binding.switchMethodDiscrete.isChecked && !binding.switchMethodHybrid.isChecked) {
        suppressLightUiCallback = true
        binding.switchMethodStateBit.isChecked = true
        suppressLightUiCallback = false
      }
    }

    binding.switchMethodDiscrete.setOnCheckedChangeListener { _, checked ->
      if (suppressLightUiCallback) return@setOnCheckedChangeListener
      if (checked) {
        suppressLightUiCallback = true
        binding.switchMethodStateBit.isChecked = false
        binding.switchMethodHybrid.isChecked = false
        suppressLightUiCallback = false
        viewModel.setLightControlMethod(LightControlMethod.DISCRETE_PATTERN)
      } else if (!binding.switchMethodStateBit.isChecked && !binding.switchMethodHybrid.isChecked) {
        suppressLightUiCallback = true
        binding.switchMethodDiscrete.isChecked = true
        suppressLightUiCallback = false
      }
    }

    binding.switchMethodHybrid.setOnCheckedChangeListener { _, checked ->
      if (suppressLightUiCallback) return@setOnCheckedChangeListener
      if (checked) {
        suppressLightUiCallback = true
        binding.switchMethodStateBit.isChecked = false
        binding.switchMethodDiscrete.isChecked = false
        suppressLightUiCallback = false
        viewModel.setLightControlMethod(LightControlMethod.HYBRID_ALL)
      } else if (!binding.switchMethodStateBit.isChecked && !binding.switchMethodDiscrete.isChecked) {
        suppressLightUiCallback = true
        binding.switchMethodHybrid.isChecked = true
        suppressLightUiCallback = false
      }
    }

    binding.rgBitVariant.setOnCheckedChangeListener { _, checkedId ->
      if (suppressLightUiCallback) return@setOnCheckedChangeListener
      val variant = when (checkedId) { // bit 变体 Radio → LightBitVariant
        R.id.rbBit02Inv -> LightBitVariant.BIT_0x02_INVERTED
        R.id.rbBit40 -> LightBitVariant.BIT_0x40
        R.id.rbBit40Inv -> LightBitVariant.BIT_0x40_INVERTED
        else -> LightBitVariant.BIT_0x02
      }
      viewModel.setLightBitVariant(variant)
    }

    binding.btnCandidateOff.setOnClickListener { viewModel.sendDiscreteCandidate(lightOn = false) }
    binding.btnCandidateOn.setOnClickListener { viewModel.sendDiscreteCandidate(lightOn = true) }
    binding.btnNextCandidate.setOnClickListener { viewModel.nextDiscreteCandidate() }
    binding.btnMarkSavedOff.setOnClickListener { viewModel.saveCurrentCandidateAsLightOff() }
    binding.btnMarkSavedOn.setOnClickListener { viewModel.saveCurrentCandidateAsLightOn() }
    binding.btnUseSavedOff.setOnClickListener { viewModel.sendSavedDiscretePattern(lightOn = false) }
    binding.btnUseSavedOn.setOnClickListener { viewModel.sendSavedDiscretePattern(lightOn = true) }
  }

  /**
   * 做什么：订阅 acState / lightConfig / feedback LiveData。
   * 调用链：onCreate → observe → renderState / renderLightConfig。
   */
  private fun observeViewModel() {
    viewModel.acState.observe(this, ::renderState)
    viewModel.lightConfig.observe(this, ::renderLightConfig)
    viewModel.feedback.observe(this) { feedback ->
      if (feedback == null) {
        binding.tvFeedback.visibility = View.GONE
        return@observe
      }
      binding.tvFeedback.visibility = View.VISIBLE
      binding.tvFeedback.text = when (feedback) {
        "sent" -> getString(R.string.command_sent)
        "candidate_changed" -> getString(R.string.light_candidate_changed)
        "saved_off" -> getString(R.string.light_saved_off_ok)
        "saved_on" -> getString(R.string.light_saved_on_ok)
        else -> getString(R.string.command_failed, feedback)
      }
    }
  }

  /**
   * 做什么：根据 [config] 同步灯光实验区 UI（方式 Switch、bit Radio、候选信息）。
   * 调用链：lightConfig LiveData → 本方法。
   * 修改指引：参数 [config] 来自 Repository；改 UI 字段时同步改 layout id。
   */
  private fun renderLightConfig(config: LightControlConfig) {
    suppressLightUiCallback = true

    binding.switchMethodStateBit.isChecked = config.method == LightControlMethod.STATE_BIT_VARIANT
    binding.switchMethodDiscrete.isChecked = config.method == LightControlMethod.DISCRETE_PATTERN
    binding.switchMethodHybrid.isChecked = config.method == LightControlMethod.HYBRID_ALL

    val bitRadioId = when (config.bitVariant) { // 配置 → RadioButton id
      LightBitVariant.BIT_0x02 -> R.id.rbBit02
      LightBitVariant.BIT_0x02_INVERTED -> R.id.rbBit02Inv
      LightBitVariant.BIT_0x40 -> R.id.rbBit40
      LightBitVariant.BIT_0x40_INVERTED -> R.id.rbBit40Inv
    }
    binding.rgBitVariant.check(bitRadioId)

    val methodLabel = if (config.method == LightControlMethod.STATE_BIT_VARIANT) { // 顶部当前方式文案
      getString(R.string.light_method_state_bit) + " · " + config.bitVariant.label
    } else if (config.method == LightControlMethod.HYBRID_ALL) {
      getString(R.string.light_method_hybrid)
    } else {
      getString(R.string.light_method_discrete)
    }
    binding.tvActiveLightMethod.text = getString(R.string.light_active_method, methodLabel)

    binding.rgBitVariant.isEnabled = config.method == LightControlMethod.STATE_BIT_VARIANT
    binding.panelDiscrete.alpha = if (config.method != LightControlMethod.STATE_BIT_VARIANT) 1f else 0.7f

    val total = TclLightPatternLibrary.candidates.size // 候选总数
    val index = config.discreteCandidateIndex + 1 // 1-based 显示序号
    binding.tvCandidateInfo.text = getString(
      R.string.light_current_candidate,
      viewModel.currentCandidateLabel(),
      index,
      total,
    )
    binding.tvSavedCodes.text = getString(
      R.string.light_saved_codes,
      viewModel.savedLightOffLabel(),
      viewModel.savedLightOnLabel(),
    )
    binding.tvHybridSummary.text = getString(
      R.string.light_hybrid_summary,
      viewModel.hybridAttemptSummary(),
    )

    suppressLightUiCallback = false
  }

  /**
   * 做什么：根据 [state] 更新状态栏文案与模式/风速 Toggle 选中项。
   * 调用链：acState LiveData → 本方法 → updateModeSelection / updateFanSelection。
   * 修改指引：参数 [state] 各字段决定 tvStatus 与 Toggle。
   */
  private fun renderState(state: AcState) {
    updateModeSelection(state.mode)
    updateFanSelection(state.fanSpeed)

    if (!state.powerOn) {
      binding.tvStatus.text = getString(R.string.status_power_off)
      return
    }

    val modeLabel = when (state.mode) { // 状态栏模式中文
      AcMode.COOL -> getString(R.string.mode_cool)
      AcMode.HEAT -> getString(R.string.mode_heat)
      AcMode.AUTO -> getString(R.string.mode_auto)
      AcMode.FAN -> getString(R.string.mode_fan)
      AcMode.DRY -> "除湿"
    }
    val lightLabel = if (state.lightOn) { // 状态栏灯光中文
      getString(R.string.status_light_on)
    } else {
      getString(R.string.status_light_off)
    }
    binding.tvStatus.text = getString(
      R.string.status_format,
      modeLabel,
      state.temperatureCelsius,
      lightLabel,
    )
  }

  /**
   * 做什么：同步模式 ToggleGroup 选中按钮（不触发 sendAction）。
   * 修改指引：参数 [mode]；DRY 暂映射到 AUTO 按钮。
   */
  private fun updateModeSelection(mode: AcMode) {
    val checkedId = when (mode) { // AcMode → Toggle 按钮 id
      AcMode.COOL -> R.id.btnModeCool
      AcMode.HEAT -> R.id.btnModeHeat
      AcMode.AUTO -> R.id.btnModeAuto
      AcMode.FAN -> R.id.btnModeFan
      AcMode.DRY -> R.id.btnModeAuto
    }
    suppressModeCallback = true
    binding.toggleMode.check(checkedId)
    suppressModeCallback = false
  }

  /**
   * 做什么：同步风速 ToggleGroup（不触发 sendAction）。
   * 修改指引：参数 [fanSpeed]；MIN 映射到低风按钮。
   */
  private fun updateFanSelection(fanSpeed: AcFanSpeed) {
    val checkedId = when (fanSpeed) { // AcFanSpeed → Toggle 按钮 id
      AcFanSpeed.AUTO -> R.id.btnFanAuto
      AcFanSpeed.LOW -> R.id.btnFanLow
      AcFanSpeed.MED -> R.id.btnFanMed
      AcFanSpeed.HIGH -> R.id.btnFanHigh
      AcFanSpeed.MIN -> R.id.btnFanLow
    }
    suppressFanCallback = true
    binding.toggleFan.check(checkedId)
    suppressFanCallback = false
  }

  companion object {
    /** Intent Extra：设备 id（预留多设备） */
    const val EXTRA_DEVICE_ID = "extra_device_id"
    /** Intent Extra：设备显示名，用于 toolbar 标题 */
    const val EXTRA_DEVICE_NAME = "extra_device_name"
  }
}
