package com.aether.sdamobile.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.sdamobile.SdaApp
import com.aether.sdamobile.data.SdaAccount
import com.aether.sdamobile.steam.ConfirmationsService
import com.aether.sdamobile.steam.SessionManager
import com.aether.sdamobile.steam.SteamAuthService
import com.aether.sdamobile.steam.SteamConfirmation
import com.aether.sdamobile.steam.SteamGuardService
import com.aether.sdamobile.steam.SteamNotLoggedInException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmationsUiState(
    val accountId: String? = null,
    val loading: Boolean = false,
    val items: List<SteamConfirmation> = emptyList(),
    val error: String? = null,
    val needsLogin: Boolean = false,
    val actionIds: Set<String> = emptySet(),
    val loadedAt: Long = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SdaApp
    val repository = app.repository
    val vault = repository.vault

    var tab by mutableStateOf(0)
    var selectedAccountId by mutableStateOf<String?>(null)

    private val _confirmations = MutableStateFlow(ConfirmationsUiState())
    val confirmations: StateFlow<ConfirmationsUiState> = _confirmations.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    var timeSyncing by mutableStateOf(false)
        private set

    var busyAccountId by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { syncTimeQuiet() }
    }

    fun emit(message: String) {
        _messages.tryEmit(message)
    }

    fun selectAccount(id: String?) {
        selectedAccountId = id
        _confirmations.value = ConfirmationsUiState(accountId = id)
    }

    fun account(id: String?): SdaAccount? = id?.let { repository.account(it) }

    fun refreshConfirmations(accountId: String) {
        val account = repository.account(accountId) ?: return
        if (_confirmations.value.loading && _confirmations.value.accountId == accountId) return
        viewModelScope.launch {
            _confirmations.value = _confirmations.value.copy(
                accountId = accountId,
                loading = true,
                error = null,
                needsLogin = false
            )
            try {
                if (System.currentTimeMillis() / 1000 - vault.value.timeSyncedAt > 300) {
                    syncTimeQuiet()
                }
                val items = ConfirmationsService.fetch(
                    account = repository.account(accountId) ?: account,
                    timeOffset = vault.value.timeOffset,
                    repository = repository
                )
                if (_confirmations.value.accountId != accountId) return@launch
                _confirmations.value = ConfirmationsUiState(
                    accountId = accountId,
                    items = items,
                    loadedAt = System.currentTimeMillis()
                )
            } catch (e: SteamNotLoggedInException) {
                if (_confirmations.value.accountId != accountId) return@launch
                _confirmations.value = _confirmations.value.copy(
                    loading = false,
                    needsLogin = true,
                    error = null
                )
            } catch (e: Exception) {
                if (_confirmations.value.accountId != accountId) return@launch
                _confirmations.value = _confirmations.value.copy(
                    loading = false,
                    error = e.message ?: "Не удалось загрузить подтверждения"
                )
            }
        }
    }

    fun accept(accountId: String, confirmation: SteamConfirmation) =
        respond(accountId, confirmation, true)

    fun deny(accountId: String, confirmation: SteamConfirmation) =
        respond(accountId, confirmation, false)

    private fun respond(accountId: String, confirmation: SteamConfirmation, accept: Boolean) {
        val account = repository.account(accountId) ?: return
        if (_confirmations.value.accountId != accountId) return
        if (_confirmations.value.actionIds.contains(confirmation.id)) return
        viewModelScope.launch {
            _confirmations.value = _confirmations.value.copy(
                actionIds = _confirmations.value.actionIds + confirmation.id,
                error = null
            )
            try {
                val ok = ConfirmationsService.respond(
                    account = account,
                    confirmation = confirmation,
                    accept = accept,
                    timeOffset = vault.value.timeOffset,
                    repository = repository
                )
                if (ok) {
                    if (_confirmations.value.accountId != accountId) return@launch
                    _confirmations.value = _confirmations.value.copy(
                        items = _confirmations.value.items.filterNot { it.id == confirmation.id },
                        actionIds = _confirmations.value.actionIds - confirmation.id,
                        loadedAt = System.currentTimeMillis()
                    )
                    emit(if (accept) "Подтверждено" else "Отклонено")
                } else {
                    if (_confirmations.value.accountId != accountId) return@launch
                    _confirmations.value = _confirmations.value.copy(
                        actionIds = _confirmations.value.actionIds - confirmation.id,
                        error = "Steam не подтвердил действие"
                    )
                }
            } catch (e: SteamNotLoggedInException) {
                if (_confirmations.value.accountId != accountId) return@launch
                _confirmations.value = _confirmations.value.copy(
                    actionIds = _confirmations.value.actionIds - confirmation.id,
                    needsLogin = true
                )
            } catch (e: Exception) {
                if (_confirmations.value.accountId != accountId) return@launch
                _confirmations.value = _confirmations.value.copy(
                    actionIds = _confirmations.value.actionIds - confirmation.id,
                    error = e.message ?: "Не удалось выполнить действие"
                )
            }
        }
    }

    fun syncTimeNow() {
        if (timeSyncing) return
        viewModelScope.launch {
            timeSyncing = true
            try {
                syncTime()
                emit("Время синхронизировано со Steam")
            } catch (e: Exception) {
                emit("Ошибка синхронизации: ${e.message}")
            } finally {
                timeSyncing = false
            }
        }
    }

    private suspend fun syncTime() {
        val server = SteamAuthService.queryServerTime()
        val offset = server - System.currentTimeMillis() / 1000
        repository.setTimeOffset(offset)
    }

    private suspend fun syncTimeQuiet() {
        runCatching { syncTime() }
    }

    fun removeAccountLocally(accountId: String) {
        repository.removeAccount(accountId)
        viewModelScope.launch { runCatching { repository.flush() } }
        if (selectedAccountId == accountId) selectedAccountId = null
        if (_confirmations.value.accountId == accountId) {
            _confirmations.value = ConfirmationsUiState()
        }
        emit("Аккаунт удалён с устройства")
    }

    fun revokeAuthenticator(accountId: String) {
        val account = repository.account(accountId) ?: return
        if (account.revocationCode.isBlank()) {
            emit("В maFile нет кода отзыва")
            return
        }
        if (busyAccountId != null) return
        viewModelScope.launch {
            busyAccountId = accountId
            try {
                val token = SessionManager.freshAccessToken(account, repository, force = true)
                SteamGuardService.removeAuthenticator(
                    accessToken = token,
                    steamId = account.steamId,
                    revocationCode = account.revocationCode
                )
                repository.removeAccount(accountId)
                repository.flush()
                if (selectedAccountId == accountId) selectedAccountId = null
                if (_confirmations.value.accountId == accountId) {
                    _confirmations.value = ConfirmationsUiState()
                }
                emit("Steam Guard отозван. Аккаунт удалён")
            } catch (e: Exception) {
                emit(e.message ?: "Не удалось отозвать аутентификатор")
            } finally {
                busyAccountId = null
            }
        }
    }
}
