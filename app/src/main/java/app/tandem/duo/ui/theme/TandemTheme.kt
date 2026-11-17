package app.tandem.duo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ObsidianBg = Color(0xFF0D0E12)
val CharcoalSurface = Color(0xFF16181F)
val ElevatedSlate = Color(0xFF1F222C)
val TerracottaCoral = Color(0xFFE06D53)
val ChampagneGold = Color(0xFFD4AF37)
val SageMint = Color(0xFF52B788)
val CrimsonPulse = Color(0xFFEF476F)
val IvoryText = Color(0xFFF4F1EA)
val MutedPewter = Color(0xFF9499A6)

private val TandemColorScheme = darkColorScheme(
    primary = TerracottaCoral,
    secondary = ChampagneGold,
    tertiary = SageMint,
    background = ObsidianBg,
    surface = CharcoalSurface,
    surfaceVariant = ElevatedSlate,
    onPrimary = Color(0xFF0D0E12),
    onSecondary = Color(0xFF0D0E12),
    onBackground = IvoryText,
    onSurface = IvoryText,
    onSurfaceVariant = MutedPewter,
    error = CrimsonPulse
)

val TandemTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        letterSpacing = (-0.8).sp,
        color = IvoryText
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp,
        color = IvoryText
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp,
        color = IvoryText
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = IvoryText
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = MutedPewter
    )
)

@Composable
fun TandemTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TandemColorScheme,
        typography = TandemTypography,
        content = content
    )
}
