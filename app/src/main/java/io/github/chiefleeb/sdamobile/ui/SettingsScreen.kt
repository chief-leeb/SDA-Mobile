package io.github.chiefleeb.sdamobile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chiefleeb.sdamobile.data.MaFile
import io.github.chiefleeb.sdamobile.data.SdaAccount
import io.github.chiefleeb.sdamobile.i18n.AppLanguage
import io.github.chiefleeb.sdamobile.i18n.LanguageState
import io.github.chiefleeb.sdamobile.i18n.tr
import io.github.chiefleeb.sdamobile.ui.components.AccountAvatar
import io.github.chiefleeb.sdamobile.ui.components.Pill
import io.github.chiefleeb.sdamobile.ui.components.SdaCard
import io.github.chiefleeb.sdamobile.ui.components.SdaDialog
import io.github.chiefleeb.sdamobile.ui.theme.CardHigh
import io.github.chiefleeb.sdamobile.ui.theme.Danger
import io.github.chiefleeb.sdamobile.ui.theme.Line
import io.github.chiefleeb.sdamobile.ui.theme.Mint
import io.github.chiefleeb.sdamobile.ui.theme.Primary
import io.github.chiefleeb.sdamobile.ui.theme.TextPrimary
import io.github.chiefleeb.sdamobile.ui.theme.TextSecondary
import io.github.chiefleeb.sdamobile.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    accounts: List<SdaAccount>,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onSignIn: (SdaAccount) -> Unit,
    onFinishActivation: (SdaAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val vault by viewModel.vault.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportAccount by remember { mutableStateOf<SdaAccount?>(null) }
    var detailsAccount by remember { mutableStateOf<SdaAccount?>(null) }
    var revokeTarget by remember { mutableStateOf<SdaAccount?>(null) }
    var removeTarget by remember { mutableStateOf<SdaAccount?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val account = exportAccount
        exportAccount = null
        if (uri == null || account == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException(tr("Не удалось открыть файл"))
                    output.use { it.write(MaFile.encode(account).toByteArray()) }
                }
                viewModel.emit(
                    tr("maFile для ${account.accountName} сохранён", "maFile for ${account.accountName} saved")
                )
            } catch (e: Exception) {
                viewModel.emit(
                    tr(
                        "Ошибка экспорта: ${e.message ?: "неизвестная ошибка"}",
                        "Export error: ${e.message ?: "unknown error"}"
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    tr("Настройки"),
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("Безопасность и аккаунты"),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        item {
            LanguageCard(
                onSelect = { language ->
                    LanguageState.set(context, language)
                    (context as? android.app.Activity)?.recreate()
                }
            )
        }

        item {
            SecurityCard(
                timeOffset = vault.timeOffset,
                timeSyncedAt = vault.timeSyncedAt,
                syncing = viewModel.timeSyncing,
                onSync = viewModel::syncTimeNow
            )
        }

        item {
            Text(
                tr("АККАУНТЫ · ${accounts.size}", "ACCOUNTS · ${accounts.size}"),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        items(accounts, key = { it.localId }) { account ->
            AccountSettingsCard(
                account = account,
                busy = viewModel.busyAccountId == account.localId,
                menuEnabled = viewModel.busyAccountId == null,
                onSignIn = { onSignIn(account) },
                onFinish = { onFinishActivation(account) },
                onExport = {
                    exportAccount = account
                    exportLauncher.launch(MaFile.fileName(account))
                },
                onDetails = { detailsAccount = account },
                onRevoke = { revokeTarget = account },
                onRemove = { removeTarget = account }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(tr("Добавить"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(tr("Импорт maFile"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            SdaCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SDA Mobile · 1.1.0",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr("Независимое приложение для Steam Guard. Не связано с Valve. ") +
                                tr("Секреты хранятся только на этом устройстве и зашифрованы ключом Android Keystore."),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Подтверждения выполняются только вручную. Автоподтверждение отключено."),
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    detailsAccount?.let { account ->
        AccountDetailsDialog(
            account = account,
            onClose = { detailsAccount = null },
            onCopy = {
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(
                    android.content.ClipData.newPlainText("maFile", MaFile.encode(account))
                )
                viewModel.emit(tr("maFile скопирован в буфер обмена"))
            }
        )
    }

    revokeTarget?.let { account ->
        ConfirmDangerDialog(
            title = tr("Отозвать Steam Guard?"),
            message = tr(
                "Steam удалит мобильный аутентификатор с аккаунта ${account.accountName}. " +
                        "Коды перестанут работать, аккаунт будет удалён с устройства. " +
                        "Это действие нельзя отменить.",
                "Steam will remove the mobile authenticator from ${account.accountName}. " +
                        "Codes will stop working and the account will be removed from this device. " +
                        "This action cannot be undone."
            ),
            confirmText = tr("Отозвать"),
            onDismiss = { revokeTarget = null },
            onConfirm = {
                revokeTarget = null
                viewModel.revokeAuthenticator(account.localId)
            }
        )
    }

    removeTarget?.let { account ->
        ConfirmDangerDialog(
            title = tr("Удалить аккаунт?"),
            message = tr(
                "Аккаунт ${account.accountName} будет удалён только с этого устройства. " +
                        "Steam Guard на аккаунте останется активным. Перед удалением можно сохранить maFile.",
                "Account ${account.accountName} will be removed from this device only. " +
                        "Steam Guard will stay active on the account. You can save the maFile before removal."
            ),
            confirmText = tr("Удалить"),
            onDismiss = { removeTarget = null },
            onConfirm = {
                removeTarget = null
                viewModel.removeAccountLocally(account.localId)
            }
        )
    }
}

@Composable
private fun LanguageCard(onSelect: (AppLanguage) -> Unit) {
    val current = LanguageState.current
    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(
                tr("Язык", "Language"),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                tr("Язык интерфейса приложения", "App interface language"),
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            AppLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(language) }
                        .padding(horizontal = 4.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        language.nativeName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (current == language) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCard(
    timeOffset: Long,
    timeSyncedAt: Long,
    syncing: Boolean,
    onSync: () -> Unit
) {
    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Mint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("Локальное хранилище"),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "AES-256-GCM · Android Keystore",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Pill(tr("Защищено"), Mint)
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Line.copy(alpha = 0.8f))
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("Синхронизация времени"),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tr(
                            "Смещение ${formatOffset(timeOffset)} · ${formatSyncTime(timeSyncedAt)}",
                            "Offset ${formatOffset(timeOffset)} · ${formatSyncTime(timeSyncedAt)}"
                        ),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onSync,
                    enabled = !syncing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardHigh,
                        contentColor = Primary,
                        disabledContainerColor = CardHigh,
                        disabledContentColor = TextSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Синхр."), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSettingsCard(
    account: SdaAccount,
    busy: Boolean,
    menuEnabled: Boolean,
    onSignIn: () -> Unit,
    onFinish: () -> Unit,
    onExport: () -> Unit,
    onDetails: () -> Unit,
    onRevoke: () -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val status = statusOf(account)

    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountAvatar(account.accountName.ifBlank { "Steam" }, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        account.accountName.ifBlank { "Steam" },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (account.steamId != 0L) "SteamID ${account.steamId}"
                        else tr("SteamID неизвестен"),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(status.first, status.second)
                        if (account.fullyEnrolled && account.hasSession) {
                            Pill(tr("Коды"), Primary)
                        }
                    }
                }
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Warning,
                        strokeWidth = 2.dp
                    )
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }, enabled = menuEnabled) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = tr("Действия"),
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (account.hasSession) tr("Обновить сессию") else tr("Войти в Steam")) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onSignIn()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(tr("Детали maFile")) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onDetails()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(tr("Экспорт maFile")) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onExport()
                                }
                            )
                            if (!account.fullyEnrolled) {
                                DropdownMenuItem(
                                    text = { Text(tr("Завершить активацию")) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = Warning,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onFinish()
                                    }
                                )
                            }
                            HorizontalDivider(color = Line.copy(alpha = 0.8f))
                            DropdownMenuItem(
                                text = { Text(tr("Отозвать Steam Guard"), color = Danger) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = Danger,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                enabled = account.revocationCode.isNotBlank(),
                                onClick = {
                                    menuOpen = false
                                    onRevoke()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(tr("Удалить с устройства"), color = Danger) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = Danger,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onRemove()
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Line.copy(alpha = 0.8f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!account.fullyEnrolled) {
                    TextButton(onClick = onFinish, enabled = !busy) {
                        Text(tr("Завершить"), color = Warning, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onExport, enabled = !busy) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("maFile", color = Primary, fontSize = 12.sp)
                }
                TextButton(onClick = onSignIn, enabled = !busy) {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(tr("Войти"), color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AccountDetailsDialog(
    account: SdaAccount,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    var reveal by remember { mutableStateOf(false) }
    SdaDialog(onDismissRequest = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Данные maFile"),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) {
                Text(tr("Закрыть"), color = Primary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            DetailRow(tr("Логин"), account.accountName)
            DetailRow("SteamID", account.steamId.takeIf { it != 0L }?.toString() ?: tr("неизвестен"))
            DetailRow(tr("Устройство"), account.deviceId)
            DetailRow(tr("Серийный номер"), account.serialNumber)
            DetailRow(tr("Код отзыва"), account.revocationCode)
            DetailRow("shared_secret", if (reveal) account.sharedSecret else mask(account.sharedSecret), mono = true)
            DetailRow("identity_secret", if (reveal) account.identitySecret else mask(account.identitySecret), mono = true)
            DetailRow("secret_1", if (reveal) account.secret1 else mask(account.secret1), mono = true)
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { reveal = !reveal }) {
                Text(
                    if (reveal) tr("Скрыть секреты") else tr("Показать секреты"),
                    color = Primary,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Text(tr("Скопировать maFile"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    Column(Modifier.padding(vertical = 7.dp)) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        SelectionContainer {
            Text(
                value.ifBlank { "—" },
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ConfirmDangerDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SdaDialog(onDismissRequest = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            message,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена"), color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Danger,
                    contentColor = Color(0xFF2A0308)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(confirmText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun statusOf(account: SdaAccount): Pair<String, Color> = when {
    !account.fullyEnrolled -> tr("Активация") to Warning
    account.identitySecret.isBlank() -> tr("Только коды") to Warning
    !account.hasSession -> tr("Нужен вход") to TextSecondary
    else -> tr("Активен") to Mint
}

private fun mask(value: String): String =
    if (value.isBlank()) "" else "•".repeat(value.length.coerceAtMost(24))

private fun formatOffset(offset: Long): String {
    val sign = if (offset > 0) "+" else ""
    return tr("$sign$offset с", "$sign$offset s")
}

private fun formatSyncTime(epochSeconds: Long): String {
    if (epochSeconds <= 0) return tr("ещё не выполнялась")
    val formatter = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag(LanguageState.current.tag))
    return formatter.format(Date(epochSeconds * 1000))
}
