package com.deepseek.sdamobile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.sdamobile.data.AccountRepository
import com.deepseek.sdamobile.data.MaFile
import com.deepseek.sdamobile.data.MaFileException
import com.deepseek.sdamobile.data.SdaAccount
import com.deepseek.sdamobile.ui.components.ErrorBanner
import com.deepseek.sdamobile.ui.components.GradientButton
import com.deepseek.sdamobile.ui.components.SdaDialog
import com.deepseek.sdamobile.ui.components.SdaTextField
import com.deepseek.sdamobile.ui.theme.Line
import com.deepseek.sdamobile.ui.theme.TextPrimary
import com.deepseek.sdamobile.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportDialog(
    repository: AccountRepository,
    onDismiss: () -> Unit,
    onImported: (SdaAccount) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun importText(raw: String) {
        if (busy) return
        if (raw.isBlank()) {
            error = "Вставьте содержимое maFile или выберите файл"
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val account = withContext(Dispatchers.Default) { MaFile.parse(raw) }
                val existing = repository.vault.value.accounts.firstOrNull {
                    it.sharedSecret == account.sharedSecret ||
                            (account.steamId != 0L && it.steamId == account.steamId)
                }
                if (existing != null) {
                    throw MaFileException(
                        "Аккаунт ${existing.accountName} уже добавлен на это устройство"
                    )
                }
                repository.addAccount(account)
                repository.flush()
                onImported(account)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось импортировать maFile"
            } finally {
                busy = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        scope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    } ?: throw IllegalStateException("Не удалось прочитать файл")
                }
                text = raw
                busy = false
                importText(raw)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось прочитать файл"
                busy = false
            }
        }
    }

    SdaDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        dismissOnBackPress = !busy,
        dismissOnClickOutside = !busy
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Импорт maFile",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Выберите файл *.maFile или вставьте его JSON-содержимое. " +
                        "Импортируются только незашифрованные maFiles.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))

            GradientButton(
                text = if (busy) "Читаем файл…" else "Выбрать maFile",
                onClick = { filePicker.launch(arrayOf("*/*")) },
                enabled = !busy,
                loading = busy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Line)
                Text(
                    "  или вставьте JSON  ",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                HorizontalDivider(Modifier.weight(1f), color = Line)
            }

            Spacer(Modifier.height(14.dp))

            SdaTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = "Содержимое maFile",
                placeholder = "{\n  \"shared_secret\": \"…\",\n  \"account_name\": \"…\"\n}",
                singleLine = false,
                minLines = 5,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 128.dp, max = 190.dp)
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(it)
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Text("Отмена", color = TextSecondary, fontSize = 13.sp)
                }
                GradientButton(
                    text = "Импортировать",
                    onClick = { importText(text) },
                    enabled = text.isNotBlank() && !busy,
                    loading = busy,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Зашифрованные maFiles из SDA нужно предварительно расшифровать в самом SDA.",
                color = TextSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
