package com.biehuale.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置 Tab - ViewModel
 *
 * Phase 1 占位 — Phase 2 补充账户管理
 *
 * 职责：
 *  - 观察所有活跃账户
 *  - 新建账户
 *  - 发送一次性事件（创建成功 / 错误）
 *
 * 类别管理 / 主题 / 备份放在独立的子页面，SettingsViewModel 只管顶层 Tab
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    /**
     * 观察所有活跃账户（按 id 升序）
     */
    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    /**
     * 创建账户
     *
     * 校验：
     *  - 名字 1-20 字符
     *  - 余额 ≥ 0
     */
    fun createAccount(name: String, initialBalanceYuan: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 20) {
            _events.tryEmit(SettingsEvent.Error("账户名需 1-20 字符"))
            return
        }
        val cents = Money.parseToCents(initialBalanceYuan)
        if (cents == null) {
            _events.tryEmit(SettingsEvent.Error("金额格式错误"))
            return
        }

        viewModelScope.launch {
            try {
                accountRepository.create(
                    name = trimmed,
                    initialBalance = cents
                )
                _events.emit(SettingsEvent.AccountCreated)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Error(e.message ?: "创建失败"))
            }
        }
    }
}

/**
 * 一次性事件
 */
sealed interface SettingsEvent {
    data object AccountCreated : SettingsEvent
    data class Error(val message: String) : SettingsEvent
}
