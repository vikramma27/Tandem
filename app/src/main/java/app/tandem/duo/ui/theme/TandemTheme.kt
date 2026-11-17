package app.tandem.duo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Legacy palette constants retained for components that are being migrated.
val ObsidianBg = Color(0xFF11161A)
val CharcoalSurface = Color(0xFF1B2228)
val ElevatedSlate = Color(0xFF273139)
val TerracottaCoral = Color(0xFFE17A5F)
val ChampagneGold = Color(0xFFE4BD68)
val SageMint = Color(0xFF7BC9A5)
val CrimsonPulse = Color(0xFFE66B76)
val IvoryText = Color(0xFFF4F1EA)
val MutedPewter = Color(0xFFA5ADB2)

private val TandemLightColors = lightColorScheme(
    primary = Color(0xFFB85F49),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6DED6),
    onPrimaryContainer = Color(0xFF40170F),
    secondary = Color(0xFF8D6B2D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E6C8),
    onSecondaryContainer = Color(0xFF2B1E08),
    tertiary = Color(0xFF39785E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6EFE2),
    onTertiaryContainer = Color(0xFF0E2A1D),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF252A2B),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF252A2B),
    surfaceVariant = Color(0xFFEDE9E2),
    onSurfaceVariant = Color(0xFF626B6B),
    outline = Color(0xFFD8D2C9),
    error = Color(0xFFBA3D4A)
)

private val TandemDarkColors = darkColorScheme(
    primary = Color(0xFFF08B70),
    onPrimary = Color(0xFF35140D),
    primaryContainer = Color(0xFF693426),
    onPrimaryContainer = Color(0xFFFFDAD0),
    secondary = Color(0xFFE4BD68),
    onSecondary = Color(0xFF2B210D),
    secondaryContainer = Color(0xFF54431F),
    onSecondaryContainer = Color(0xFFF7E3B3),
    tertiary = Color(0xFF83CBA7),
    onTertiary = Color(0xFF0C2A1C),
    tertiaryContainer = Color(0xFF24513D),
    onTertiaryContainer = Color(0xFFC5EED8),
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = CharcoalSurface,
    onSurface = IvoryText,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = MutedPewter,
    outline = Color(0xFF465159),
    error = CrimsonPulse
)

private val TandemTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Composable
fun TandemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TandemDarkColors else TandemLightColors,
        typography = TandemTypography,
        content = content
    )
}
