package io.github.chiefleeb.sdamobile.ui

import io.github.chiefleeb.sdamobile.i18n.tr

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
import io.github.chiefleeb.sdamobile.data.SdaAccount
import io.github.chiefleeb.sdamobile.steam.SteamConfirmation
import io.github.chiefleeb.sdamobile.ui.components.AccountAvatar
import io.github.chiefleeb.sdamobile.ui.components.EmptyHint
import io.github.chiefleeb.sdamobile.ui.components.ErrorBanner
import io.github.chiefleeb.sdamobile.ui.components.GradientButton
import io.github.chiefleeb.sdamobile.ui.components.Pill
import io.github.chiefleeb.sdamobile.ui.components.SdaCard
import io.github.chiefleeb.sdamobile.ui.theme.CardHigh
import io.github.chiefleeb.sdamobile.ui.theme.Danger
import io.github.chiefleeb.sdamobile.ui.theme.Line
import io.github.chiefleeb.sdamobile.ui.theme.Mint
import io.github.chiefleeb.sdamobile.ui.theme.Primary
import io.github.chiefleeb.sdamobile.ui.theme.TextPrimary
import io.github.chiefleeb.sdamobile.ui.theme.TextSecondary
import io.github.chiefleeb.sdamobile.ui.theme.Warning
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
                        tr("Подтверждения"),
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Обмены и сделки маркета"),
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
                            contentDescription = tr("Обновить"),
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
                    title = tr("Нет аккаунта"),
                    subtitle = tr("Добавьте аккаунт Steam, чтобы видеть подтверждения обменов и маркета.")
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
                            tr("Подтверждения недоступны"),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr("В maFile нет identity_secret. Коды Steam Guard работают, ") +
                                    tr("но принимать обмены и сделки маркета это приложение не сможет. ") +
                                    tr("Импортируйте полный maFile из SDA."),
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
                        tr("Загружаем подтверждения…"),
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
                        Text(tr("Повторить"), color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items.isEmpty() -> item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyHint(
                        title = tr("Подтверждений нет"),
                        subtitle = tr("Здесь появятся запросы на обмен предметами и продажу на маркете.")
                    )
                    TextButton(onClick = onRefresh) {
                        Text(tr("Обновить"), color = Primary, fontWeight = FontWeight.SemiBold)
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
                tr("Нужен вход в Steam"),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                tr(
                    "Сессия для ${account.accountName.ifBlank { "аккаунта" }} истекла. ",
                    "Session for ${account.accountName.ifBlank { "account" }} expired. "
                ) +
                        tr("Войдите заново, чтобы загрузить подтверждения. Ключи Steam Guard останутся на устройстве."),
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))
            GradientButton(
                text = tr("Войти в Steam"),
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
        confirmation.isMarket -> tr("Маркет")
        confirmation.typeName.contains("trade", ignoreCase = true) -> tr("Обмен")
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
                        confirmation.headline.ifBlank { tr("Запрос Steam") },
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
                        tr("Отклонить"),
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
                        Text(tr("Подтвердить"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        diff < 60 -> tr("только что")
        diff < 3600 -> tr(
            "${TimeUnit.SECONDS.toMinutes(diff)} мин назад",
            "${TimeUnit.SECONDS.toMinutes(diff)} min ago"
        )
        diff < 86_400 -> tr(
            "${TimeUnit.SECONDS.toHours(diff)} ч назад",
            "${TimeUnit.SECONDS.toHours(diff)} h ago"
        )
        else -> tr(
            "${TimeUnit.SECONDS.toDays(diff)} дн назад",
            "${TimeUnit.SECONDS.toDays(diff)} d ago"
        )
    }
}
