package com.aether.sdamobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.sdamobile.ui.components.SdaCard
import com.aether.sdamobile.ui.theme.Card
import com.aether.sdamobile.ui.theme.CardHigh
import com.aether.sdamobile.ui.theme.Line
import com.aether.sdamobile.ui.theme.Mint
import com.aether.sdamobile.ui.theme.Primary
import com.aether.sdamobile.ui.theme.TextPrimary
import com.aether.sdamobile.ui.theme.TextSecondary
import com.aether.sdamobile.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismiss: () -> Unit,
    onAddToSteam: () -> Unit,
    onImport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Card,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Line)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
        ) {
            Text(
                "Добавить аккаунт",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Подключите Steam Guard к телефону или перенесите готовый maFile.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(20.dp))

            SheetOption(
                title = "Подключить к Steam",
                subtitle = "Вход в аккаунт и создание мобильного аутентификатора",
                icon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(21.dp)
                    )
                },
                accent = Primary,
                onClick = onAddToSteam
            )
            Spacer(Modifier.height(12.dp))
            SheetOption(
                title = "Импортировать maFile",
                subtitle = "Файл из SDA или другого генератора Steam Guard",
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = Mint,
                        modifier = Modifier.size(21.dp)
                    )
                },
                accent = Mint,
                onClick = onImport
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Warning.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Добавление аутентификатора отключит текущий Steam Guard на аккаунте. " +
                            "Убедитесь, что у вас есть доступ к почте и телефону. Код отзыва будет показан после активации.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    SdaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = CardHigh,
        borderColor = Line
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
