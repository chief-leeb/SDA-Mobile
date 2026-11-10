package com.deepseek.sdamobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseek.sdamobile.ui.components.GradientButton
import com.deepseek.sdamobile.ui.components.ShieldMark
import com.deepseek.sdamobile.ui.theme.Bg
import com.deepseek.sdamobile.ui.theme.Card
import com.deepseek.sdamobile.ui.theme.Line
import com.deepseek.sdamobile.ui.theme.Mint
import com.deepseek.sdamobile.ui.theme.Primary
import com.deepseek.sdamobile.ui.theme.TextPrimary
import com.deepseek.sdamobile.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onAdd: () -> Unit,
    onImport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C1A2B), Bg, Bg)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Primary.copy(alpha = 0.18f), Mint.copy(alpha = 0.08f))
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                ShieldMark(size = 52.dp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Steam Guard\nв твоём кармане",
                color = TextPrimary,
                fontSize = 38.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Мобильный аутентификатор Steam с современным интерфейсом. " +
                        "Коды, подтверждения обменов и маркета — в одном приложении.",
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 23.sp
            )

            Spacer(Modifier.height(32.dp))

            FeatureRow("Коды Steam Guard работают офлайн")
            FeatureRow("Подтверждение обменов и сделок маркета")
            FeatureRow("Секреты остаются только на этом устройстве")

            Spacer(Modifier.height(40.dp))

            GradientButton(
                text = "Добавить аккаунт",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Text(
                    "Импортировать maFile",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Данные зашифрованы ключом Android Keystore. Приложение не связано с Valve.",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Mint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(Mint, CircleShape)
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(text, color = TextPrimary.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}

@Composable
fun StorageErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShieldMark(size = 64.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Хранилище недоступно",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        GradientButton("Повторить", onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "Если ошибка сохраняется, не удаляйте данные приложения. " +
                        "Попробуйте перезапустить его или восстановить maFile на другом устройстве.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}
