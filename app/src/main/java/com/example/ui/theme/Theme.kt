package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val title: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val previewBg: Color
) {
    TERRACOTTA(
        title = "Terracotta Warm",
        description = "Organic cream, vinyl terracotta & sage",
        primaryColor = TerracottaPrimary,
        secondaryColor = SageSecondary,
        previewBg = BackgroundCream
    ),
    CYBER_MIDNIGHT(
        title = "Cyber Midnight",
        description = "Deep OLED black, electric cyan & violet neon",
        primaryColor = CyberNeonCyan,
        secondaryColor = CyberElectricViolet,
        previewBg = CyberBgDark
    ),
    EMERALD_GOLD(
        title = "Emerald Gold",
        description = "Luxury deep forest & champagne gold",
        primaryColor = EmeraldPrimary,
        secondaryColor = EmeraldGoldSecondary,
        previewBg = EmeraldBgDark
    ),
    ROYAL_AMETHYST(
        title = "Royal Amethyst",
        description = "Nocturnal velvet purple & vivid fuchsia",
        primaryColor = AmethystPrimary,
        secondaryColor = AmethystSecondary,
        previewBg = AmethystBgDark
    ),
    OCEANIC_AZURE(
        title = "Oceanic Azure",
        description = "Deep cobalt sea, aqua tint & crisp blue",
        primaryColor = OceanPrimary,
        secondaryColor = OceanSecondary,
        previewBg = OceanBgDark
    ),
    CRIMSON_EMBER(
        title = "Crimson Ember",
        description = "Midnight rock, flaming ruby & amber glow",
        primaryColor = CrimsonPrimary,
        secondaryColor = CrimsonSecondary,
        previewBg = CrimsonBgDark
    )
}

fun getThemeColorScheme(preset: AppThemePreset, isDark: Boolean): ColorScheme {
    return when (preset) {
        AppThemePreset.TERRACOTTA -> {
            if (isDark) {
                darkColorScheme(
                    primary = TerracottaLight,
                    onPrimary = Color(0xFF3F1306),
                    primaryContainer = TerracottaDark,
                    onPrimaryContainer = Color(0xFFFFDBD1),
                    secondary = SageLight,
                    onSecondary = Color(0xFF0F3825),
                    secondaryContainer = SageDark,
                    onSecondaryContainer = Color(0xFFCCE8D9),
                    tertiary = WarmSandTertiary,
                    background = BackgroundDark,
                    onBackground = TextDarkPrimary,
                    surface = SurfaceDark,
                    onSurface = TextDarkPrimary,
                    surfaceVariant = SurfaceCardDark,
                    onSurfaceVariant = TextDarkMuted,
                    outline = BorderDark
                )
            } else {
                lightColorScheme(
                    primary = TerracottaPrimary,
                    onPrimary = Color.White,
                    primaryContainer = SurfaceCardHover,
                    onPrimaryContainer = TextEspresso,
                    secondary = SageSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFE2F0E8),
                    onSecondaryContainer = Color(0xFF1F4432),
                    tertiary = WarmSandTertiary,
                    background = BackgroundCream,
                    onBackground = TextEspresso,
                    surface = SurfaceCream,
                    onSurface = TextEspresso,
                    surfaceVariant = SurfaceCard,
                    onSurfaceVariant = TextMuted,
                    outline = BorderSubtle
                )
            }
        }
        AppThemePreset.CYBER_MIDNIGHT -> {
            if (isDark) {
                darkColorScheme(
                    primary = CyberNeonCyan,
                    onPrimary = Color(0xFF00382E),
                    primaryContainer = Color(0xFF005345),
                    onPrimaryContainer = Color(0xFF70FFF0),
                    secondary = CyberElectricViolet,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF4C1885),
                    onSecondaryContainer = Color(0xFFEADBFF),
                    tertiary = CyberHotPink,
                    background = CyberBgDark,
                    onBackground = CyberTextDark,
                    surface = CyberSurfaceDark,
                    onSurface = CyberTextDark,
                    surfaceVariant = CyberCardDark,
                    onSurfaceVariant = CyberTextMutedDark,
                    outline = CyberBorderDark
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF00897B),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0F7F4),
                    onPrimaryContainer = Color(0xFF00382E),
                    secondary = CyberElectricViolet,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFF1E6FF),
                    onSecondaryContainer = Color(0xFF330C63),
                    tertiary = CyberHotPink,
                    background = CyberBgLight,
                    onBackground = CyberTextLight,
                    surface = CyberSurfaceLight,
                    onSurface = CyberTextLight,
                    surfaceVariant = CyberCardLight,
                    onSurfaceVariant = CyberTextMutedLight,
                    outline = CyberBorderLight
                )
            }
        }
        AppThemePreset.EMERALD_GOLD -> {
            if (isDark) {
                darkColorScheme(
                    primary = EmeraldLight,
                    onPrimary = Color(0xFF042113),
                    primaryContainer = EmeraldPrimary,
                    onPrimaryContainer = Color(0xFFB5E4CC),
                    secondary = EmeraldGoldSecondary,
                    onSecondary = Color(0xFF3B2E05),
                    secondaryContainer = Color(0xFF5A4808),
                    onSecondaryContainer = Color(0xFFFBEBC2),
                    tertiary = EmeraldChampagne,
                    background = EmeraldBgDark,
                    onBackground = EmeraldTextDark,
                    surface = EmeraldSurfaceDark,
                    onSurface = EmeraldTextDark,
                    surfaceVariant = EmeraldCardDark,
                    onSurfaceVariant = EmeraldTextMutedDark,
                    outline = EmeraldBorderDark
                )
            } else {
                lightColorScheme(
                    primary = EmeraldPrimary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD6EDE0),
                    onPrimaryContainer = Color(0xFF0E261A),
                    secondary = Color(0xFFB08C1E),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFDF4DC),
                    onSecondaryContainer = Color(0xFF3E3106),
                    tertiary = EmeraldChampagne,
                    background = EmeraldBgLight,
                    onBackground = EmeraldTextLight,
                    surface = EmeraldSurfaceLight,
                    onSurface = EmeraldTextLight,
                    surfaceVariant = EmeraldCardLight,
                    onSurfaceVariant = EmeraldTextMutedLight,
                    outline = EmeraldBorderLight
                )
            }
        }
        AppThemePreset.ROYAL_AMETHYST -> {
            if (isDark) {
                darkColorScheme(
                    primary = AmethystLight,
                    onPrimary = Color(0xFF2E004F),
                    primaryContainer = AmethystPrimary,
                    onPrimaryContainer = Color(0xFFF3DDFF),
                    secondary = AmethystSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF75083B),
                    onSecondaryContainer = Color(0xFFFFD9E6),
                    tertiary = AmethystTertiary,
                    background = AmethystBgDark,
                    onBackground = AmethystTextDark,
                    surface = AmethystSurfaceDark,
                    onSurface = AmethystTextDark,
                    surfaceVariant = AmethystCardDark,
                    onSurfaceVariant = AmethystTextMutedDark,
                    outline = AmethystBorderDark
                )
            } else {
                lightColorScheme(
                    primary = AmethystPrimary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFF5E6FF),
                    onPrimaryContainer = Color(0xFF3B0068),
                    secondary = AmethystSecondary,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFE5F0),
                    onSecondaryContainer = Color(0xFF6B0034),
                    tertiary = AmethystTertiary,
                    background = AmethystBgLight,
                    onBackground = AmethystTextLight,
                    surface = AmethystSurfaceLight,
                    onSurface = AmethystTextLight,
                    surfaceVariant = AmethystCardLight,
                    onSurfaceVariant = AmethystTextMutedLight,
                    outline = AmethystBorderLight
                )
            }
        }
        AppThemePreset.OCEANIC_AZURE -> {
            if (isDark) {
                darkColorScheme(
                    primary = OceanSecondary,
                    onPrimary = Color(0xFF00344F),
                    primaryContainer = OceanPrimary,
                    onPrimaryContainer = Color(0xFFCCEFFF),
                    secondary = OceanLight,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF00496B),
                    onSecondaryContainer = Color(0xFFBEE7FF),
                    tertiary = OceanMint,
                    background = OceanBgDark,
                    onBackground = OceanTextDark,
                    surface = OceanSurfaceDark,
                    onSurface = OceanTextDark,
                    surfaceVariant = OceanCardDark,
                    onSurfaceVariant = OceanTextMutedDark,
                    outline = OceanBorderDark
                )
            } else {
                lightColorScheme(
                    primary = OceanPrimary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD4EFFF),
                    onPrimaryContainer = Color(0xFF002236),
                    secondary = Color(0xFF0089B3),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFE0F6FF),
                    onSecondaryContainer = Color(0xFF003748),
                    tertiary = OceanMint,
                    background = OceanBgLight,
                    onBackground = OceanTextLight,
                    surface = OceanSurfaceLight,
                    onSurface = OceanTextLight,
                    surfaceVariant = OceanCardLight,
                    onSurfaceVariant = OceanTextMutedLight,
                    outline = OceanBorderLight
                )
            }
        }
        AppThemePreset.CRIMSON_EMBER -> {
            if (isDark) {
                darkColorScheme(
                    primary = CrimsonPrimary,
                    onPrimary = Color(0xFF4A0000),
                    primaryContainer = Color(0xFF7A0909),
                    onPrimaryContainer = Color(0xFFFFDAD6),
                    secondary = CrimsonSecondary,
                    onSecondary = Color(0xFF422C00),
                    secondaryContainer = Color(0xFF664400),
                    onSecondaryContainer = Color(0xFFFFE0A0),
                    tertiary = CrimsonGold,
                    background = CrimsonBgDark,
                    onBackground = CrimsonTextDark,
                    surface = CrimsonSurfaceDark,
                    onSurface = CrimsonTextDark,
                    surfaceVariant = CrimsonCardDark,
                    onSurfaceVariant = CrimsonTextMutedDark,
                    outline = CrimsonBorderDark
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFD92B2B),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFE6E4),
                    onPrimaryContainer = Color(0xFF4D0000),
                    secondary = Color(0xFFD98200),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFF2D6),
                    onSecondaryContainer = Color(0xFF472A00),
                    tertiary = CrimsonGold,
                    background = CrimsonBgLight,
                    onBackground = CrimsonTextLight,
                    surface = CrimsonSurfaceLight,
                    onSurface = CrimsonTextLight,
                    surfaceVariant = CrimsonCardLight,
                    onSurfaceVariant = CrimsonTextMutedLight,
                    outline = CrimsonBorderLight
                )
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    preset: AppThemePreset = AppThemePreset.TERRACOTTA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(preset, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
