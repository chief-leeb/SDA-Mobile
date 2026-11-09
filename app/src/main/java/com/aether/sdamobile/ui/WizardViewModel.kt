package com.aether.sdamobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.sdamobile.SdaApp
import com.aether.sdamobile.data.SdaAccount
import com.aether.sdamobile.data.SteamSession
import com.aether.sdamobile.steam.AddAuthenticatorResult
import com.aether.sdamobile.steam.AllowedGuard
import com.aether.sdamobile.steam.AuthSession
import com.aether.sdamobile.steam.LinkedAuthenticator
import com.aether.sdamobile.steam.SessionManager
import com.aether.sdamobile.steam.SteamAuthService
import com.aether.sdamobile.steam.SteamCrypto
import com.aether.sdamobile.steam.SteamGuardService
import com.aether.sdamobile.steam.SteamNotLoggedInException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class WizardMode { ADD, LOGIN, FINALIZE }

enum class WizardStep { CREDENTIALS, GUARD, WAITING, PHONE, EMAIL_CONFIRM, SMS, DONE, ERROR }

data class GuardChoice(
    val type: Int,
    val title: String,
    val hint: String
)

data class WizardUiState(
    val mode: WizardMode = WizardMode.ADD,
    val step: WizardStep = WizardStep.CREDENTIALS,
    val accountId: String? = null,
    val username: String = "",
    val password: String = "",
    val code: String = "",
    val smsCode: String = "",
    val phone: String = "",
    val phoneCountry: String = "",
    val guardChoices: List<GuardChoice> = emptyList(),
    val selectedGuard: Int = 0,
    val busy: Boolean = false,
    val statusText: String? = null,
    val error: String? = null,
    val emailHint: String? = null,
    val revocationCode: String? = null,
    val createdAccountName: String? = null,
    val pendingAccountId: String? = null,
    val canRetryAdd: Boolean = false,
    val critical: Boolean = false
)

class WizardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SdaApp).repository
    private val _state = MutableStateFlow(WizardUiState())
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    private var authSession: AuthSession? = null
    private var accessToken = ""
    private var refreshToken = ""
    private var steamId = 0L
    private var linked: LinkedAuthenticator? = null
    private var pendingId: String? = null
    private var deviceId = ""
    private var pollJob: Job? = null
    private var emailJob: Job? = null

    fun start(mode: WizardMode, accountId: String? = null) {
        cancelJobs()
        authSession = null
        accessToken = ""
        refreshToken = ""
        steamId = 0L
        linked = null
        pendingId = null
        deviceId = ""
        val account = accountId?.let { repository.account(it) }
        if (mode == WizardMode.FINALIZE && account != null) {
            pendingId = account.localId
            linked = account.toLinked()
            steamId = account.steamId
            accessToken = account.accessToken
            refreshToken = account.refreshToken
            _state.value = WizardUiState(
                mode = mode,
                step = WizardStep.SMS,
                accountId = accountId,
                username = account.accountName,
                revocationCode = account.revocationCode,
                pendingAccountId = account.localId,
                statusText = "Введите код из SMS, который отправил Steam при добавлении аутентификатора."
            )
            return
        }
        _state.value = WizardUiState(
            mode = mode,
            accountId = accountId,
            username = account?.accountName.orEmpty()
        )
    }

    fun onUsernameChange(value: String) = update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = update { it.copy(password = value, error = null) }
    fun onCodeChange(value: String) = update { it.copy(code = value, error = null) }
    fun onSmsChange(value: String) = update { it.copy(smsCode = value, error = null) }
    fun onPhoneChange(value: String) = update { it.copy(phone = value, error = null) }
    fun onPhoneCountryChange(value: String) = update { it.copy(phoneCountry = value.uppercase(), error = null) }

    fun selectGuard(type: Int) = update { it.copy(selectedGuard = type) }

    fun begin() {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            update { it.copy(error = "Введите логин и пароль Steam") }
            return
        }
        viewModelScope.launch {
            update {
                it.copy(
                    busy = true,
                    error = null,
                    step = WizardStep.WAITING,
                    statusText = "Подключаемся к Steam…"
                )
            }
            try {
                val session = SteamAuthService.beginSession(
                    current.username.trim(),
                    current.password
                )
                authSession = session
                steamId = session.steamId
                val choices = buildGuardChoices(session.guards, current.mode)
                val autoConfirm = session.guards.any {
                    it.type == SteamAuthService.GUARD_DEVICE_CONFIRM ||
                            it.type == SteamAuthService.GUARD_EMAIL_CONFIRM ||
                            it.type == SteamAuthService.GUARD_NONE
                }

                if (choices.isNotEmpty()) {
                    update {
                        it.copy(
                            step = WizardStep.GUARD,
                            busy = false,
                            statusText = null,
                            guardChoices = choices,
                            selectedGuard = choices.first().type,
                            error = null
                        )
                    }
                } else if (autoConfirm || session.guards.isEmpty()) {
                    startPolling(
                        if (session.guards.any { it.type == SteamAuthService.GUARD_DEVICE_CONFIRM })
                            "Подтвердите вход в приложении Steam…"
                        else "Ожидаем подтверждение Steam…"
                    )
                } else {
                    startPolling("Ожидаем ответ Steam…")
                }
            } catch (e: Exception) {
                failCredentials(e)
            }
        }
    }

    fun submitGuardCode() {
        val current = _state.value
        val session = authSession ?: return
        if (current.code.isBlank()) {
            update { it.copy(error = "Введите код Steam Guard") }
            return
        }
        viewModelScope.launch {
            update {
                it.copy(
                    busy = true,
                    error = null,
                    statusText = "Проверяем код…",
                    step = WizardStep.WAITING
                )
            }
            try {
                SteamAuthService.submitGuardCode(session, current.code.trim(), current.selectedGuard)
                update { it.copy(code = "") }
                startPolling("Ожидаем подтверждение входа…")
            } catch (e: Exception) {
                update {
                    it.copy(
                        busy = false,
                        step = WizardStep.GUARD,
                        statusText = null,
                        error = readableError(e)
                    )
                }
            }
        }
    }

    fun submitSmsCode() {
        val current = _state.value
        val sms = current.smsCode.trim()
        if (sms.isBlank()) {
            update { it.copy(error = "Введите код из SMS") }
            return
        }
        val account = pendingId?.let { repository.account(it) }
        val data = linked ?: account?.toLinked()
        if (data == null || pendingId == null) {
            update { it.copy(error = "Нет сохранённых данных аутентификатора") }
            return
        }

        viewModelScope.launch {
            update {
                it.copy(
                    step = WizardStep.WAITING,
                    busy = true,
                    error = null,
                    statusText = "Активируем Steam Guard…",
                    critical = true
                )
            }
            try {
                val token = if (account != null) {
                    SessionManager.freshAccessToken(account, repository)
                } else {
                    accessToken
                }
                if (token.isBlank()) throw SteamNotLoggedInException("Сессия устарела")
                val offset = syncTimeOrDefault()
                val serverTime = SteamGuardService.finalizeAuthenticator(
                    accessToken = token,
                    steamId = account?.steamId ?: steamId,
                    sharedSecret = data.sharedSecret,
                    activationCode = sms,
                    timeOffset = offset
                )
                val accountSteamId = account?.steamId ?: steamId
                val session = SteamSession(
                    steamId = accountSteamId,
                    accessToken = token,
                    refreshToken = account?.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken,
                    sessionId = account?.session?.sessionId?.takeIf { it.isNotBlank() }
                        ?: SteamCrypto.newSessionId()
                )
                repository.updateAccount(pendingId!!) {
                    it.copy(
                        fullyEnrolled = true,
                        status = 1,
                        serverTime = serverTime,
                        session = session,
                        accessTokenAt = System.currentTimeMillis() / 1000,
                        deviceId = it.deviceId.ifBlank { deviceId }
                    )
                }
                repository.flush()
                val name = repository.account(pendingId!!)?.accountName.orEmpty()
                update {
                    it.copy(
                        step = WizardStep.DONE,
                        busy = false,
                        statusText = null,
                        error = null,
                        createdAccountName = name,
                        smsCode = "",
                        critical = false
                    )
                }
            } catch (e: Exception) {
                update {
                    it.copy(
                        step = WizardStep.SMS,
                        busy = false,
                        statusText = null,
                        error = readableError(e),
                        critical = false
                    )
                }
            }
        }
    }

    fun submitPhone() {
        val current = _state.value
        if (!current.phone.startsWith("+") || current.phone.length < 8) {
            update { it.copy(error = "Введите номер в формате +79001234567") }
            return
        }
        viewModelScope.launch {
            update {
                it.copy(
                    step = WizardStep.WAITING,
                    busy = true,
                    error = null,
                    statusText = "Привязываем номер к аккаунту…"
                )
            }
            try {
                val country = current.phoneCountry.takeIf { it.length == 2 }
                    ?: SteamGuardService.getUserCountry(accessToken, steamId)
                    ?: throw Exception("Не удалось определить страну. Укажите ISO-код страны (например RU)")
                val result = SteamGuardService.setPhoneNumber(accessToken, current.phone.trim(), country)
                update {
                    it.copy(
                        step = WizardStep.EMAIL_CONFIRM,
                        busy = true,
                        statusText = "Подтвердите почту, затем вернитесь в приложение",
                        emailHint = result.confirmationEmail.takeIf { it.isNotBlank() },
                        phone = result.formattedPhone
                    )
                }
                startEmailPolling()
            } catch (e: Exception) {
                update {
                    it.copy(
                        step = WizardStep.PHONE,
                        busy = false,
                        error = readableError(e)
                    )
                }
            }
        }
    }

    fun retryEmailCheck() {
        if (emailJob?.isActive == true) return
        startEmailPolling()
    }

    fun retryAdd() {
        if (_state.value.canRetryAdd) {
            addAuthenticator()
        }
    }

    fun cancel() {
        cancelJobs()
    }

    fun reset() {
        cancelJobs()
        _state.value = WizardUiState()
    }

    private fun startPolling(message: String) {
        pollJob?.cancel()
        update {
            it.copy(
                step = WizardStep.WAITING,
                busy = true,
                error = null,
                statusText = message
            )
        }
        pollJob = viewModelScope.launch {
            try {
                val deadline = System.currentTimeMillis() + 180_000
                while (isActive) {
                    val session = authSession ?: throw SteamNotLoggedInException()
                    val result = SteamAuthService.poll(session)
                    if (result.newClientId != null) {
                        authSession = session.copy(clientId = result.newClientId)
                    }
                    if (!result.refreshToken.isNullOrBlank()) {
                        onAuthenticated(result.refreshToken, result.accessToken, result.accountName)
                        return@launch
                    }
                    if (System.currentTimeMillis() > deadline) {
                        throw Exception("Время ожидания истекло. Попробуйте войти снова")
                    }
                    delay(((authSession?.pollInterval ?: 5).coerceAtLeast(2)) * 1000L)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val hasCode = _state.value.guardChoices.isNotEmpty()
                update {
                    it.copy(
                        step = if (hasCode) WizardStep.GUARD else WizardStep.CREDENTIALS,
                        busy = false,
                        statusText = null,
                        error = readableError(e)
                    )
                }
            }
        }
    }

    private suspend fun onAuthenticated(
        newRefreshToken: String,
        newAccessToken: String?,
        newAccountName: String?
    ) {
        refreshToken = newRefreshToken
        steamId = steamId.takeIf { it != 0L }
            ?: SteamCrypto.jwtSteamId(newRefreshToken)
            ?: 0L
        val name = newAccountName?.takeIf { it.isNotBlank() }
            ?: _state.value.username.trim()
        if (_state.value.mode == WizardMode.LOGIN) {
            update {
                it.copy(
                    step = WizardStep.WAITING,
                    busy = true,
                    statusText = "Сохраняем сессию…"
                )
            }
            finishLogin(name)
            return
        }

        update {
            it.copy(
                step = WizardStep.WAITING,
                busy = true,
                statusText = "Получаем токен доступа…"
            )
        }
        accessToken = newAccessToken?.takeIf { it.isNotBlank() }
            ?: SteamAuthService.generateAccessToken(newRefreshToken, steamId)
        if (steamId == 0L) {
            update {
                it.copy(
                    step = WizardStep.ERROR,
                    busy = false,
                    statusText = null,
                    error = "Steam не вернул SteamID аккаунта",
                    createdAccountName = name,
                    canRetryAdd = false,
                    critical = false
                )
            }
            return
        }
        addAuthenticator()
    }

    private suspend fun finishLogin(name: String) {
        val id = _state.value.accountId
        val account = id?.let { repository.account(it) }
        if (account == null) {
            update {
                it.copy(
                    step = WizardStep.ERROR,
                    busy = false,
                    statusText = null,
                    error = "Аккаунт не найден в хранилище",
                    canRetryAdd = false,
                    critical = false
                )
            }
            return
        }
        val token = accessToken
        if (account.steamId != 0L && steamId != 0L && account.steamId != steamId) {
            update {
                it.copy(
                    step = WizardStep.ERROR,
                    busy = false,
                    statusText = null,
                    error = "Вы вошли в другой аккаунт Steam. Выберите нужный аккаунт и повторите вход.",
                    canRetryAdd = false,
                    critical = false
                )
            }
            return
        }
        repository.updateAccount(account.localId) {
            it.copy(
                accountName = it.accountName.ifBlank { name },
                deviceId = it.deviceId.ifBlank { SteamCrypto.deviceIdFor(steamId.toString()) },
                session = SteamSession(
                    steamId = steamId,
                    accessToken = token,
                    refreshToken = refreshToken,
                    sessionId = it.session?.sessionId?.takeIf { sessionId -> sessionId.isNotBlank() }
                        ?: SteamCrypto.newSessionId()
                ),
                accessTokenAt = System.currentTimeMillis() / 1000
            )
        }
        repository.flush()
        update {
            it.copy(
                step = WizardStep.DONE,
                busy = false,
                statusText = null,
                error = null,
                createdAccountName = name,
                pendingAccountId = account.localId,
                critical = false
            )
        }
    }

    private fun addAuthenticator() {
        if (accessToken.isBlank()) {
            update {
                it.copy(
                    error = "Сессия устарела. Войдите снова",
                    step = WizardStep.CREDENTIALS,
                    critical = false
                )
            }
            return
        }
        if (deviceId.isBlank()) deviceId = SteamCrypto.newDeviceId()
        viewModelScope.launch {
            update {
                it.copy(
                    step = WizardStep.WAITING,
                    busy = true,
                    error = null,
                    statusText = "Добавляем мобильный аутентификатор…",
                    canRetryAdd = false,
                    critical = true
                )
            }
            try {
                val offset = syncTimeOrDefault()
                val serverTime = System.currentTimeMillis() / 1000 + offset
                when (val result = SteamGuardService.addAuthenticator(
                    accessToken = accessToken,
                    steamId = steamId,
                    deviceId = deviceId,
                    serverTime = serverTime
                )) {
                    is AddAuthenticatorResult.Success -> savePending(result.data)
                    AddAuthenticatorResult.NoPhone -> update {
                        it.copy(
                            step = WizardStep.PHONE,
                            busy = false,
                            statusText = null,
                            error = null,
                            critical = false
                        )
                    }
                    AddAuthenticatorResult.AlreadyPresent -> update {
                        it.copy(
                            step = WizardStep.ERROR,
                            busy = false,
                            statusText = null,
                            error = "На аккаунте уже есть мобильный аутентификатор. " +
                                    "Удалите его в Steam или импортируйте maFile.",
                            canRetryAdd = false,
                            critical = false
                        )
                    }
                    is AddAuthenticatorResult.Failure -> update {
                        it.copy(
                            step = WizardStep.ERROR,
                            busy = false,
                            statusText = null,
                            error = result.message,
                            canRetryAdd = true,
                            critical = false
                        )
                    }
                }
            } catch (e: Exception) {
                update {
                    it.copy(
                        step = WizardStep.ERROR,
                        busy = false,
                        statusText = null,
                        error = readableError(e),
                        canRetryAdd = true,
                        critical = false
                    )
                }
            }
        }
    }

    private suspend fun savePending(data: LinkedAuthenticator) {
        val name = _state.value.username.trim().ifBlank { "Steam" }
        val now = System.currentTimeMillis() / 1000
        val session = SteamSession(
            steamId = steamId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            sessionId = SteamCrypto.newSessionId()
        )
        val existing = repository.vault.value.accounts.firstOrNull {
            it.steamId != 0L && it.steamId == steamId
        }
        val account = existing?.copy(
            sharedSecret = data.sharedSecret,
            identitySecret = data.identitySecret,
            secret1 = data.secret1,
            serialNumber = data.serialNumber,
            revocationCode = data.revocationCode,
            uri = data.uri,
            tokenGid = data.tokenGid,
            deviceId = data.deviceId,
            serverTime = data.serverTime,
            accountName = name,
            fullyEnrolled = false,
            status = 1,
            session = session,
            accessTokenAt = now
        ) ?: SdaAccount(
            sharedSecret = data.sharedSecret,
            identitySecret = data.identitySecret,
            secret1 = data.secret1,
            serialNumber = data.serialNumber,
            revocationCode = data.revocationCode,
            uri = data.uri,
            tokenGid = data.tokenGid,
            deviceId = data.deviceId,
            serverTime = data.serverTime,
            accountName = name,
            fullyEnrolled = false,
            status = 1,
            session = session,
            accessTokenAt = now,
            addedAt = now
        )
        repository.addAccount(account)
        repository.flush()
        val id = repository.vault.value.accounts.firstOrNull {
            it.sharedSecret == data.sharedSecret
        }?.localId ?: account.localId
        pendingId = id
        linked = data
        update {
            it.copy(
                step = WizardStep.SMS,
                busy = false,
                statusText = "Steam отправил SMS с кодом активации. Введите его ниже.",
                revocationCode = data.revocationCode,
                pendingAccountId = id,
                error = null,
                critical = false
            )
        }
    }

    private fun startEmailPolling() {
        emailJob?.cancel()
        emailJob = viewModelScope.launch {
            var waitedSeconds = 0
            var transientErrors = 0
            try {
                while (isActive && waitedSeconds < 300) {
                    delay(5000)
                    waitedSeconds += 5
                    val waiting = try {
                        SteamGuardService.isWaitingForEmailConfirmation(accessToken)
                    } catch (e: Exception) {
                        transientErrors++
                        if (transientErrors >= 4) throw e
                        continue
                    }
                    transientErrors = 0
                    if (!waiting) {
                        update {
                            it.copy(
                                statusText = "Почта подтверждена. Отправляем SMS…",
                                busy = true,
                                error = null
                            )
                        }
                        SteamGuardService.sendPhoneVerificationCode(accessToken)
                        delay(2000)
                        addAuthenticator()
                        return@launch
                    }
                }
                update {
                    it.copy(
                        step = WizardStep.EMAIL_CONFIRM,
                        busy = false,
                        error = "Почта не подтверждена. Подтвердите её и попробуйте снова."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update {
                    it.copy(
                        step = WizardStep.EMAIL_CONFIRM,
                        busy = false,
                        error = readableError(e)
                    )
                }
            }
        }
    }

    private suspend fun syncTimeOrDefault(): Long {
        return try {
            val server = SteamAuthService.queryServerTime()
            val offset = server - System.currentTimeMillis() / 1000
            repository.setTimeOffset(offset)
            offset
        } catch (_: Exception) {
            repository.vault.value.timeOffset
        }
    }

    private fun failCredentials(e: Exception) {
        update {
            it.copy(
                step = WizardStep.CREDENTIALS,
                busy = false,
                statusText = null,
                error = readableError(e)
            )
        }
    }

    private fun cancelJobs() {
        pollJob?.cancel()
        pollJob = null
        emailJob?.cancel()
        emailJob = null
    }

    private fun update(block: (WizardUiState) -> WizardUiState) {
        _state.value = block(_state.value)
    }

    private fun buildGuardChoices(guards: List<AllowedGuard>, mode: WizardMode): List<GuardChoice> {
        val result = mutableListOf<GuardChoice>()
        guards.firstOrNull { it.type == SteamAuthService.GUARD_DEVICE_CODE }?.let {
            result += GuardChoice(
                type = SteamAuthService.GUARD_DEVICE_CODE,
                title = "Код из приложения",
                hint = "Код текущего аутентификатора Steam"
            )
        }
        guards.firstOrNull { it.type == SteamAuthService.GUARD_EMAIL_CODE }?.let {
            result += GuardChoice(
                type = SteamAuthService.GUARD_EMAIL_CODE,
                title = "Код из письма",
                hint = it.message?.let { domain -> "Проверьте почту ($domain)" } ?: "Проверьте почту"
            )
        }
        if (mode != WizardMode.LOGIN) {
            result.sortBy { if (it.type == SteamAuthService.GUARD_EMAIL_CODE) 0 else 1 }
        }
        return result
    }

    private fun readableError(e: Exception): String = when (e) {
        is SteamNotLoggedInException -> e.message ?: "Требуется вход в Steam"
        else -> e.message ?: "Неизвестная ошибка"
    }
}

private fun SdaAccount.toLinked(): LinkedAuthenticator = LinkedAuthenticator(
    sharedSecret = sharedSecret,
    identitySecret = identitySecret,
    secret1 = secret1,
    serialNumber = serialNumber,
    revocationCode = revocationCode,
    uri = uri,
    tokenGid = tokenGid,
    deviceId = deviceId,
    serverTime = serverTime
)
