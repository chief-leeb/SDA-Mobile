package io.github.chiefleeb.sdamobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF070C15)
val Card = Color(0xFF0F1826)
val CardHigh = Color(0xFF162235)
val Line = Color(0xFF223149)
val Primary = Color(0xFF66C0F4)
val Mint = Color(0xFF7DE2C3)
val TextPrimary = Color(0xFFEDF4FC)
val TextSecondary = Color(0xFF93A6BE)
val Danger = Color(0xFFFF6B7A)
val Warning = Color(0xFFFFC66D)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF04131F),
    primaryContainer = Color(0xFF123048),
    onPrimaryContainer = Color(0xFFD4ECFF),
    secondary = Mint,
    onSecondary = Color(0xFF032019),
    secondaryContainer = Color(0xFF12362D),
    onSecondaryContainer = Color(0xFFD5FBF0),
    tertiary = Color(0xFFA9A7FF),
    background = Bg,
    onBackground = TextPrimary,
    surface = Card,
    onSurface = TextPrimary,
    surfaceVariant = CardHigh,
    onSurfaceVariant = TextSecondary,
    outline = Line,
    error = Danger,
    onError = Color(0xFF2A0308),
    errorContainer = Color(0xFF3A1119),
    onErrorContainer = Color(0xFFFFD9DD)
)

val CodeStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    letterSpacing = 5.sp,
    color = TextPrimary
)

private val SdaTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp
    )
)

private val SdaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SdaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SdaTypography,
        shapes = SdaShapes,
        content = content
    )
}
