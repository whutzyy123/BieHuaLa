package com.biehuale.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountManageViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    /**
     * 账户列表 + 一次聚合余额（无 N+1 / 不订全表流水）
     */
    val uiState: StateFlow<AccountManageUiState> = combine(
        accountRepository.observeActive(),
        accountRepository.observeActiveBalances()
    ) { accounts, balanceRows ->
        val balanceMap = balanceRows.associate { it.accountId to it.balance }
        val items = accounts.map { account ->
            AccountItem(
                account = account,
                balance = balanceMap[account.id] ?: 0L
            )
        }
        AccountManageUiState(accounts = items, isLoading = false)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountManageUiState(isLoading = true)
        )

    private val _events = MutableSharedFlow<AccountManageEvent>(extraBufferCapacity = 1)
    val events = _events

    fun create(name: String, initialBalanceYuan: String, icon: String?, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 20) {
            _events.tryEmit(AccountManageEvent.Error("账户名需 1-20 字符"))
            return
        }
        val cents = Money.parseToCents(initialBalanceYuan) ?: run {
            _events.tryEmit(AccountManageEvent.Error("金额格式错误"))
            return
        }
        if (cents < 0L) {
            _events.tryEmit(AccountManageEvent.Error("初始余额不能为负"))
            return
        }
        viewModelScope.launch {
            try {
                accountRepository.create(
                    name = trimmed,
                    initialBalance = cents,
                    icon = icon,
                    colorHex = colorHex
                )
                _events.emit(AccountManageEvent.AccountSaved)
            } catch (e: Exception) {
                _events.emit(AccountManageEvent.Error(e.message ?: "创建失败"))
            }
        }
    }

    fun update(id: Long, name: String, initialBalanceYuan: String, icon: String?, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 20) {
            _events.tryEmit(AccountManageEvent.Error("账户名需 1-20 字符"))
            return
        }
        val cents = Money.parseToCents(initialBalanceYuan) ?: run {
            _events.tryEmit(AccountManageEvent.Error("金额格式错误"))
            return
        }
        if (cents < 0L) {
            _events.tryEmit(AccountManageEvent.Error("初始余额不能为负"))
            return
        }
        viewModelScope.launch {
            try {
                val existing = accountRepository.getById(id) ?: run {
                    _events.emit(AccountManageEvent.Error("账户不存在"))
                    return@launch
                }
                accountRepository.update(
                    existing.copy(
                        name = trimmed,
                        initialBalance = cents,
                        icon = icon,
                        colorHex = colorHex
                    )
                )
                _events.emit(AccountManageEvent.AccountSaved)
            } catch (e: Exception) {
                _events.emit(AccountManageEvent.Error(e.message ?: "更新失败"))
            }
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch {
            try {
                accountRepository.archive(id)
                _events.emit(AccountManageEvent.AccountArchived)
            } catch (e: Exception) {
                _events.emit(AccountManageEvent.Error(e.message ?: "归档失败"))
            }
        }
    }
}

data class AccountItem(
    val account: AccountEntity,
    val balance: Long
)

data class AccountManageUiState(
    val accounts: List<AccountItem> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface AccountManageEvent {
    data object AccountSaved : AccountManageEvent
    data object AccountArchived : AccountManageEvent
    data class Error(val message: String) : AccountManageEvent
}
