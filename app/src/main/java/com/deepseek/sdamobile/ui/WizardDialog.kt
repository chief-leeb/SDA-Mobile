package com.deepseek.sdamobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepseek.sdamobile.steam.SteamAuthService
import com.deepseek.sdamobile.ui.components.ErrorBanner
import com.deepseek.sdamobile.ui.components.GradientButton
import com.deepseek.sdamobile.ui.components.SdaCard
import com.deepseek.sdamobile.ui.components.SdaTextField
import com.deepseek.sdamobile.ui.theme.Bg
import com.deepseek.sdamobile.ui.theme.CardHigh
import com.deepseek.sdamobile.ui.theme.Danger
import com.deepseek.sdamobile.ui.theme.Line
import com.deepseek.sdamobile.ui.theme.Mint
import com.deepseek.sdamobile.ui.theme.Primary
import com.deepseek.sdamobile.ui.theme.TextPrimary
import com.deepseek.sdamobile.ui.theme.TextSecondary
import com.deepseek.sdamobile.ui.theme.Warning

@Composable
fun WizardDialog(
    viewModel: WizardViewModel,
    mode: WizardMode,
    accountId: String?,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(mode, accountId) {
        viewModel.start(mode, accountId)
    }

    Dialog(
        onDismissRequest = { if (!state.critical) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.busy && !state.critical,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose, enabled = !state.critical) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Закрыть",
                            tint = if (state.critical) TextSecondary.copy(alpha = 0.35f) else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stepTitle(state),
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stepSubtitle(state),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 22.dp, end = 22.dp, bottom = 28.dp)
                ) {
                    when (state.step) {
                        WizardStep.CREDENTIALS -> CredentialsStep(state, viewModel)
                        WizardStep.GUARD -> GuardStep(state, viewModel)
                        WizardStep.WAITING -> WaitingStep(state, onClose)
                        WizardStep.PHONE -> PhoneStep(state, viewModel)
                        WizardStep.EMAIL_CONFIRM -> EmailConfirmStep(state, viewModel, onClose)
                        WizardStep.SMS -> SmsStep(state, viewModel)
                        WizardStep.DONE -> DoneStep(state, onClose)
                        WizardStep.ERROR -> ErrorStep(state, viewModel, onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialsStep(state: WizardUiState, viewModel: WizardViewModel) {
    Spacer(Modifier.height(12.dp))
    if (state.mode == WizardMode.LOGIN) {
        InfoNote(
            "Вход нужен только для подтверждений обменов и маркета. " +
                    "Ключи Steam Guard уже хранятся на устройстве.",
            Primary
        )
    } else {
        InfoNote(
            "Steam Guard будет перенесён на этот телефон. " +
                    "Старый аутентификатор на аккаунте перестанет работать.",
            Warning
        )
    }

    Spacer(Modifier.height(20.dp))
    SdaTextField(
        value = state.username,
        onValueChange = viewModel::onUsernameChange,
        label = "Логин Steam",
        placeholder = "account_name",
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    SdaTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        label = "Пароль",
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    state.error?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }

    Spacer(Modifier.height(22.dp))
    GradientButton(
        text = "Продолжить",
        onClick = viewModel::begin,
        enabled = state.username.isNotBlank() &&
                state.password.isNotBlank() &&
                !state.busy,
        loading = state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(14.dp))
    Text(
        "Пароль отправляется напрямую на серверы Steam и не сохраняется в приложении.",
        color = TextSecondary.copy(alpha = 0.8f),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GuardStep(state: WizardUiState, viewModel: WizardViewModel) {
    Spacer(Modifier.height(12.dp))
    Text(
        "Steam требует подтвердить вход. Выберите доступный способ.",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
    Spacer(Modifier.height(16.dp))

    state.guardChoices.forEach { choice ->
        val selected = state.selectedGuard == choice.type
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) Primary.copy(alpha = 0.09f) else CardHigh)
                .border(
                    1.dp,
                    if (selected) Primary.copy(alpha = 0.55f) else Line,
                    RoundedCornerShape(18.dp)
                )
                .clickable { viewModel.selectGuard(choice.type) }
                .padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Primary,
                    unselectedColor = TextSecondary
                )
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    choice.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    choice.hint,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SdaTextField(
        value = state.code,
        onValueChange = viewModel::onCodeChange,
        label = if (state.selectedGuard == SteamAuthService.GUARD_EMAIL_CODE) "Код из письма" else "Код Steam Guard",
        placeholder = "XXXXX",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    state.error?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }

    Spacer(Modifier.height(20.dp))
    GradientButton(
        text = "Подтвердить",
        onClick = viewModel::submitGuardCode,
        enabled = state.code.isNotBlank() && !state.busy,
        loading = state.busy,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WaitingStep(state: WizardUiState, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(46.dp),
            color = Primary,
            strokeWidth = 3.dp
        )
        Spacer(Modifier.height(22.dp))
        Text(
            state.statusText ?: "Подключаемся к Steam…",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (state.mode) {
                WizardMode.LOGIN -> "Обычно подтверждение занимает несколько секунд."
                WizardMode.FINALIZE -> "Steam проверяет код активации."
                WizardMode.ADD -> "Steam создаёт мобильный аутентификатор для аккаунта."
            },
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(30.dp))
        if (state.critical) {
            Text(
                "Не закрывайте приложение, пока Steam не ответит. Это займёт до 30 секунд.",
                color = Warning,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Text("Отмена", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PhoneStep(state: WizardUiState, viewModel: WizardViewModel) {
    Spacer(Modifier.height(12.dp))
    InfoNote(
        "Для создания мобильного аутентификатора Steam должен знать номер телефона аккаунта. " +
                "Если номера нет, привяжите его здесь — Steam отправит SMS.",
        Primary
    )

    Spacer(Modifier.height(20.dp))
    SdaTextField(
        value = state.phoneCountry,
        onValueChange = viewModel::onPhoneCountryChange,
        label = "Страна (ISO, необязательно)",
        placeholder = "RU",
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    SdaTextField(
        value = state.phone,
        onValueChange = viewModel::onPhoneChange,
        label = "Номер телефона",
        placeholder = "+79001234567",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    state.error?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }

    Spacer(Modifier.height(22.dp))
    GradientButton(
        text = "Привязать номер",
        onClick = viewModel::submitPhone,
        enabled = state.phone.length >= 8 && !state.busy,
        loading = state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(14.dp))
    Text(
        "Номер используется только Steam для защиты аккаунта. Приложение его не хранит.",
        color = TextSecondary.copy(alpha = 0.8f),
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}

@Composable
private fun EmailConfirmStep(
    state: WizardUiState,
    viewModel: WizardViewModel,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Email,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Подтвердите почту",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Steam отправил письмо${state.emailHint?.let { " на $it" } ?: ""}. " +
                    "Перейдите по ссылке и подтвердите привязку номера, затем вернитесь сюда.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        state.statusText?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                it,
                color = Primary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        state.error?.let {
            Spacer(Modifier.height(14.dp))
            ErrorBanner(it)
        }
        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = if (state.busy) "Ожидаем подтверждение…" else "Проверить снова",
            onClick = viewModel::retryEmailCheck,
            enabled = !state.busy,
            loading = state.busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose, enabled = !state.busy) {
            Text("Отмена", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SmsStep(state: WizardUiState, viewModel: WizardViewModel) {
    Spacer(Modifier.height(12.dp))
    InfoNote(
        if (state.mode == WizardMode.FINALIZE) {
            "Steam отправил SMS с кодом активации для ${state.username}. Введите его, чтобы завершить подключение."
        } else {
            "Steam отправил SMS с кодом активации. Введите его ниже, чтобы включить Steam Guard."
        },
        Primary
    )

    state.revocationCode?.takeIf { it.isNotBlank() }?.let { code ->
        Spacer(Modifier.height(16.dp))
        RevocationCard(code)
    }

    state.statusText?.let {
        Spacer(Modifier.height(14.dp))
        Text(
            it,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    Spacer(Modifier.height(18.dp))
    SdaTextField(
        value = state.smsCode,
        onValueChange = viewModel::onSmsChange,
        label = "Код из SMS",
        placeholder = "XXXXX",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    )

    state.error?.let {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(it)
    }

    Spacer(Modifier.height(22.dp))
    GradientButton(
        text = "Активировать",
        onClick = viewModel::submitSmsCode,
        enabled = state.smsCode.isNotBlank() && !state.busy,
        loading = state.busy,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DoneStep(state: WizardUiState, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.12f))
                .border(1.dp, Mint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Mint,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            when (state.mode) {
                WizardMode.LOGIN -> "Сессия сохранена"
                WizardMode.FINALIZE -> "Активация завершена"
                WizardMode.ADD -> "Steam Guard подключён"
            },
            color = TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.createdAccountName ?: state.username,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when (state.mode) {
                WizardMode.LOGIN -> "Подтверждения обменов и маркета теперь доступны."
                WizardMode.FINALIZE -> "Коды Steam Guard и подтверждения готовы к работе."
                WizardMode.ADD -> "Коды Steam Guard готовы к работе. Подтверждения включатся вместе с сессией."
            },
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        state.revocationCode?.takeIf { it.isNotBlank() }?.let { code ->
            Spacer(Modifier.height(22.dp))
            RevocationCard(code)
        }

        Spacer(Modifier.height(26.dp))
        GradientButton(
            text = "Готово",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorStep(
    state: WizardUiState,
    viewModel: WizardViewModel,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Danger.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Danger,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Не удалось завершить",
            color = TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            state.error ?: "Неизвестная ошибка",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        if (state.canRetryAdd) {
            GradientButton(
                text = "Повторить",
                onClick = viewModel::retryAdd,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Text("Закрыть", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfoNote(text: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun RevocationCard(code: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Warning.copy(alpha = 0.08f))
            .border(1.dp, Warning.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Warning,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Код отзыва Steam Guard",
                color = Warning,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                code,
                color = TextPrimary,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                }
            ) {
                Text("Копировать", color = Warning, fontSize = 12.sp)
            }
        }
        Text(
            "Сохраните код в надёжном месте. Он нужен, чтобы отозвать аутентификатор без доступа к телефону.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

private fun stepTitle(state: WizardUiState): String = when (state.step) {
    WizardStep.CREDENTIALS -> if (state.mode == WizardMode.LOGIN) "Вход в Steam" else "Новый аккаунт"
    WizardStep.GUARD -> "Подтверждение входа"
    WizardStep.WAITING -> "Подключение"
    WizardStep.PHONE -> "Номер телефона"
    WizardStep.EMAIL_CONFIRM -> "Почта"
    WizardStep.SMS -> "Код из SMS"
    WizardStep.DONE -> "Готово"
    WizardStep.ERROR -> "Ошибка"
}

private fun stepSubtitle(state: WizardUiState): String = when (state.step) {
    WizardStep.CREDENTIALS -> if (state.mode == WizardMode.LOGIN) {
        "Обновление сессии для подтверждений"
    } else {
        "Создание мобильного аутентификатора"
    }
    WizardStep.GUARD -> "Steam Guard"
    WizardStep.WAITING -> "Не закрывайте приложение"
    WizardStep.PHONE -> "Привязка к аккаунту"
    WizardStep.EMAIL_CONFIRM -> "Проверьте почту"
    WizardStep.SMS -> "Активация Steam Guard"
    WizardStep.DONE -> "SDA Mobile"
    WizardStep.ERROR -> "Проверьте данные"
}
