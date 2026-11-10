package com.deepseek.sdamobile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.sdamobile.data.SdaAccount
import com.deepseek.sdamobile.steam.SteamCrypto
import com.deepseek.sdamobile.ui.components.AccountAvatar
import com.deepseek.sdamobile.ui.components.CountdownRing
import com.deepseek.sdamobile.ui.components.Pill
import com.deepseek.sdamobile.ui.components.SdaCard
import com.deepseek.sdamobile.ui.theme.CardHigh
import com.deepseek.sdamobile.ui.theme.CodeStyle
import com.deepseek.sdamobile.ui.theme.Primary
import com.deepseek.sdamobile.ui.theme.TextPrimary
import com.deepseek.sdamobile.ui.theme.TextSecondary
import com.deepseek.sdamobile.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun CodesScreen(
    accounts: List<SdaAccount>,
    timeOffset: Long,
    onMessage: (String) -> Unit,
    onFinishActivation: (SdaAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(100)
        }
    }
    val clipboard = LocalClipboardManager.current
    val serverMillis = nowMillis + timeOffset * 1000
    val remaining = 30_000L - (serverMillis % 30_000L)
    val fraction = remaining / 30_000f
    val secondsLeft = ceil(remaining / 1000.0).toInt().coerceIn(1, 30)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Коды",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Steam Guard · ${accounts.size} ${accountWord(accounts.size)}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        items(accounts, key = { it.localId }) { account ->
            val code = runCatching {
                SteamCrypto.codeForTime(account.sharedSecret, serverMillis / 1000)
            }.getOrNull()
            AccountCodeCard(
                account = account,
                code = code,
                secondsLeft = secondsLeft,
                fraction = fraction,
                onCopy = {
                    if (code != null) {
                        clipboard.setText(AnnotatedString(code))
                        onMessage("Код для ${account.accountName} скопирован")
                    }
                },
                onFinish = { onFinishActivation(account) }
            )
        }
    }
}

@Composable
private fun AccountCodeCard(
    account: SdaAccount,
    code: String?,
    secondsLeft: Int,
    fraction: Float,
    onCopy: () -> Unit,
    onFinish: () -> Unit
) {
    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountAvatar(account.accountName.ifBlank { "Steam" }, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        account.accountName.ifBlank { "Steam" },
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            !account.fullyEnrolled -> "Активация не завершена"
                            account.steamId != 0L -> "Steam ID ${account.steamId}"
                            else -> "Steam Guard"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                CountdownRing(fraction = fraction, secondsLeft = secondsLeft)
            }

            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardHigh)
                    .clickable(enabled = code != null, onClick = onCopy)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STEAM GUARD",
                        color = Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = code ?: "— — — — —",
                        style = CodeStyle,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (code != null) "Нажмите, чтобы скопировать" else "Не удалось прочитать секрет",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (!account.fullyEnrolled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill("Не активно", Warning)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Завершите активацию в настройках",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onFinish) {
                        Text("Завершить", color = Warning, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun accountWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "аккаунт"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "аккаунта"
    else -> "аккаунтов"
}
