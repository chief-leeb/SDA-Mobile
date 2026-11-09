package com.aether.sdamobile.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.sdamobile.data.SdaAccount
import com.aether.sdamobile.steam.SteamConfirmation
import com.aether.sdamobile.ui.components.AccountAvatar
import com.aether.sdamobile.ui.components.EmptyHint
import com.aether.sdamobile.ui.components.ErrorBanner
import com.aether.sdamobile.ui.components.GradientButton
import com.aether.sdamobile.ui.components.Pill
import com.aether.sdamobile.ui.components.SdaCard
import com.aether.sdamobile.ui.theme.CardHigh
import com.aether.sdamobile.ui.theme.Danger
import com.aether.sdamobile.ui.theme.Line
import com.aether.sdamobile.ui.theme.Mint
import com.aether.sdamobile.ui.theme.Primary
import com.aether.sdamobile.ui.theme.TextPrimary
import com.aether.sdamobile.ui.theme.TextSecondary
import com.aether.sdamobile.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

@Composable
fun ConfirmationsScreen(
    accounts: List<SdaAccount>,
    selected: SdaAccount?,
    state: ConfirmationsUiState,
    onSelectAccount: (String?) -> Unit,
    onRefresh: () -> Unit,
    onAccept: (SteamConfirmation) -> Unit,
    onDeny: (SteamConfirmation) -> Unit,
    onSignIn: (SdaAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(selected?.localId) {
        while (isActive) {
            delay(60_000)
            onRefresh()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Подтверждения",
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Обмены и сделки маркета",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = selected != null && !state.loading
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Обновить",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (accounts.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.localId }) { account ->
                        AccountChip(
                            account = account,
                            selected = account.localId == selected?.localId,
                            onClick = { onSelectAccount(account.localId) }
                        )
                    }
                }
            }
        }

        val account = selected
        if (account == null) {
            item {
                EmptyHint(
                    title = "Нет аккаунта",
                    subtitle = "Добавьте аккаунт Steam, чтобы видеть подтверждения обменов и маркета."
                )
            }
            return@LazyColumn
        }

        val active = state.accountId == account.localId
        val loading = !active || (state.loading && state.items.isEmpty())
        val error = state.error.takeIf { active }
        val items = state.items.takeIf { active }.orEmpty()

        when {
            account.identitySecret.isBlank() -> item {
                SdaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Подтверждения недоступны",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "В maFile нет identity_secret. Коды Steam Guard работают, " +
                                    "но принимать обмены и сделки маркета это приложение не сможет. " +
                                    "Импортируйте полный maFile из SDA.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            state.needsLogin && active -> item {
                LoginRequiredCard(account = account, onSignIn = { onSignIn(account) })
            }

            loading -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Primary, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Загружаем подтверждения…",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            error != null -> item {
                Column {
                    ErrorBanner(error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text("Повторить", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items.isEmpty() -> item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyHint(
                        title = "Подтверждений нет",
                        subtitle = "Здесь появятся запросы на обмен предметами и продажу на маркете."
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Обновить", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> items(items, key = { it.id }) { confirmation ->
                ConfirmationCard(
                    confirmation = confirmation,
                    busy = state.actionIds.contains(confirmation.id),
                    onAccept = { onAccept(confirmation) },
                    onDeny = { onDeny(confirmation) }
                )
            }
        }
    }
}

@Composable
private fun AccountChip(account: SdaAccount, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) Primary.copy(alpha = 0.65f) else Line
    val background = if (selected) Primary.copy(alpha = 0.13f) else CardHigh
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountAvatar(account.accountName.ifBlank { "Steam" }, size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            account.accountName.ifBlank { "Steam" },
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoginRequiredCard(account: SdaAccount, onSignIn: () -> Unit) {
    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Warning.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Нужен вход в Steam",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Сессия для ${account.accountName.ifBlank { "аккаунта" }} истекла. " +
                        "Войдите заново, чтобы загрузить подтверждения. Ключи Steam Guard останутся на устройстве.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))
            GradientButton(
                text = "Войти в Steam",
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConfirmationCard(
    confirmation: SteamConfirmation,
    busy: Boolean,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    val label = when {
        confirmation.isMarket -> "Маркет"
        confirmation.typeName.contains("trade", ignoreCase = true) -> "Обмен"
        confirmation.typeName.isNotBlank() -> confirmation.typeName
        else -> "Steam"
    }
    val accent = if (confirmation.isMarket) Primary else Mint
    val icon = if (confirmation.isMarket) Icons.Filled.ShoppingCart else Icons.Filled.Share

    SdaCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(label, accent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            timeAgo(confirmation.creationTime),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        confirmation.headline.ifBlank { "Запрос Steam" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                    if (confirmation.summary.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            confirmation.summary.joinToString(" · "),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = Line.copy(alpha = 0.8f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDeny, enabled = !busy) {
                    Text(
                        "Отклонить",
                        color = if (busy) TextSecondary.copy(alpha = 0.5f) else Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onAccept,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color(0xFF04131F),
                        disabledContainerColor = Primary.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF04131F).copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF04131F),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Подтвердить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun timeAgo(epochSeconds: Long): String {
    if (epochSeconds <= 0) return ""
    val diff = (System.currentTimeMillis() / 1000 - epochSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${TimeUnit.SECONDS.toMinutes(diff)} мин назад"
        diff < 86_400 -> "${TimeUnit.SECONDS.toHours(diff)} ч назад"
        else -> "${TimeUnit.SECONDS.toDays(diff)} дн назад"
    }
}
