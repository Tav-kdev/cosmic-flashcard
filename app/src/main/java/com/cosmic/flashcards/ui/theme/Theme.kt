package com.cosmic.flashcards.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cosmic.flashcards.R

// ---------------------------------------------------------------- palette

val Space950 = Color(0xFF05050F)
val Space900 = Color(0xFF0A0A1A)
val Space800 = Color(0xFF12122B)
val Space700 = Color(0xFF1A1A3A)

val NeonCyan = Color(0xFF22D3EE)
val NeonPurple = Color(0xFFA78BFA)
val NeonPink = Color(0xFFF472B6)
val Amber = Color(0xFFFBBF24)
val Emerald = Color(0xFF34D399)
val Rose = Color(0xFFFB7185)

val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val TextFaint = Color(0xFF4B5563)

/** Translucent card fill — the "glass" in the web app. */
val GlassFill = Color(0xB812122B)
val GlassBorder = Color(0x1FA78BFA)
val GlassBorderActive = Color(0x5922D3EE)

/** The page background, matching the web app's radial gradient. */
val CosmicBackground = Brush.verticalGradient(
    0f to Color(0xFF0B1220),
    0.45f to Color(0xFF080A16),
    1f to Space950,
)

/** One deck/tag accent, expanded into everything the UI needs from it. */
data class Accent(
    val key: String,
    val label: String,
    val solid: Color,
    val tileStart: Color,
    val tileEnd: Color,
    val border: Color,
) {
    val tile: Brush get() = Brush.linearGradient(listOf(tileStart, tileEnd))
    val chipFill: Color get() = solid.copy(alpha = 0.12f)
    val chipBorder: Color get() = solid.copy(alpha = 0.28f)
}

object Accents {
    val CYAN = Accent("cyan", "Cyan", NeonCyan, Color(0x4006B6D4), Color(0x402563EB), Color(0x4006B6D4))
    val PURPLE = Accent("purple", "Purple", NeonPurple, Color(0x40A855F7), Color(0x40DB2777), Color(0x40A855F7))
    val PINK = Accent("pink", "Pink", NeonPink, Color(0x40EC4899), Color(0x40E11D48), Color(0x40EC4899))
    val AMBER = Accent("amber", "Amber", Amber, Color(0x40F59E0B), Color(0x40EA580C), Color(0x40F59E0B))
    val EMERALD = Accent("emerald", "Emerald", Emerald, Color(0x4010B981), Color(0x400D9488), Color(0x4010B981))

    val ALL = listOf(CYAN, PURPLE, PINK, AMBER, EMERALD)

    fun of(key: String?): Accent = ALL.firstOrNull { it.key == key } ?: CYAN
}

/** Emoji offered when naming a deck. */
val DECK_EMOJI = listOf(
    "🧠", "💻", "🌌", "🔬", "📚", "🧪", "🎓", "🗂️", "🌍", "⚗️",
    "🩺", "⚖️", "🎹", "🗣️", "📐", "🛰️", "🧬", "🏛️", "💊", "🔭",
)

// ------------------------------------------------------------- typography

/**
 * Orbitron is bundled for the display face (the wordmark and the big numbers).
 * Body text uses the platform font, which keeps the app feeling native and
 * saves shipping a second family.
 */
val Display = FontFamily(Font(R.font.orbitron, FontWeight.Normal))

private val CosmicTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.8.sp),
)

private val CosmicColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Space950,
    secondary = NeonPurple,
    onSecondary = Space950,
    tertiary = NeonPink,
    background = Space950,
    onBackground = TextPrimary,
    surface = Space800,
    onSurface = TextPrimary,
    surfaceVariant = Space700,
    onSurfaceVariant = TextSecondary,
    error = Rose,
    outline = GlassBorder,
)

@Composable
fun CosmicTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalContext.current
    SideEffect {
        (view as? Activity)?.window?.let { window ->
            // The app is dark-only by design, so force light icons in the
            // status and navigation bars regardless of system theme.
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CosmicColors,
        typography = CosmicTypography,
        content = content,
    )
}
