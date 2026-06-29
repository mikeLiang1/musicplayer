package org.example.project.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    // ── Backgrounds ──────────────────────────────────
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val backgroundElevated: Color,
    val backgroundSurface: Color,

    // ── Text ─────────────────────────────────────────
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDim: Color,

    // ── Accent (Violet) ───────────────────────────────
    val accentPrimary: Color,
    val accentDark: Color,
    val accentContainer: Color,
    val onAccent: Color,
    val onAccentContainer: Color,

    // ── Tertiary (Rose) ───────────────────────────────
    val rose: Color,
    val roseContainer: Color,
    val onRose: Color,

    // ── Icons ─────────────────────────────────────────
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconMuted: Color,
    val iconActive: Color,

    // ── Functional ────────────────────────────────────
    val divider: Color,
    val dividerSubtle: Color,
    val error: Color,
    val onError: Color,
)

// ── Dark ─────────────────────────────────────────────
val DarkAppColors = AppColors(
    backgroundPrimary   = Color(0xFF0D0B14),  // was 0A0A0F, more purple
    backgroundSecondary = Color(0xFF151220),  // was 13131A, more purple
    backgroundElevated  = Color(0xFF1E1A2E),  // was 1C1C26, more purple
    backgroundSurface   = Color(0xFF2A2440),  // was 2A2A38, more purple

    textPrimary         = Color(0xFFF0F0FA),
    textSecondary       = Color(0xFFCAC4D8),
    textMuted           = Color(0xFF948FA8),
    textDim             = Color(0xFF4A4560),  // was 4A4A5E, slight purple shift

    accentPrimary       = Color(0xFF9D8FFF),
    accentDark          = Color(0xFF6B5AE0),
    accentContainer     = Color(0xFF3D2EAD),
    onAccent            = Color(0xFF0F0F18),
    onAccentContainer   = Color(0xFFE7E0FF),  // high-contrast on accentContainer (was 9D8FFF == accentPrimary)

    rose                = Color(0xFFF0A8BC),
    roseContainer       = Color(0xFF4A1A2E),
    onRose              = Color(0xFF3A0A1E),

    iconPrimary         = Color(0xFFF0F0FA),
    iconSecondary       = Color(0xFFCAC4D8),
    iconMuted           = Color(0xFF948FA8),
    iconActive          = Color(0xFF9D8FFF),

    divider             = Color(0xFF2A2440),
    dividerSubtle       = Color(0x12B0A8FF),  // purple tinted subtle divider
    error               = Color(0xFFFFB4AB),
    onError             = Color(0xFF690005),
)
// ── Light ────────────────────────────────────────────
val LightAppColors = AppColors(
    backgroundPrimary = Color(0xFFF8F8FF),
    backgroundSecondary = Color(0xFFFFFFFF),
    backgroundElevated = Color(0xFFEEEEF8),
    backgroundSurface = Color(0xFFE4E4F0),

    textPrimary = Color(0xFF1A1A2A),
    textSecondary = Color(0xFF4A4560),
    textMuted = Color(0xFF7A7490),
    textDim = Color(0xFFAAAAAC),

    accentPrimary = Color(0xFF6B5AE0),
    accentDark = Color(0xFF3D2EAD),
    accentContainer = Color(0xFFE8E0FF),
    onAccent = Color(0xFFFFFFFF),
    onAccentContainer = Color(0xFF1A0080),

    rose = Color(0xFFBF5A7A),
    roseContainer = Color(0xFFFFD9E4),
    onRose = Color(0xFFFFFFFF),

    iconPrimary = Color(0xFF1A1A2A),
    iconSecondary = Color(0xFF4A4560),
    iconMuted = Color(0xFF7A7490),
    iconActive = Color(0xFF6B5AE0),

    divider = Color(0xFFE4E4F0),
    dividerSubtle = Color(0x0F000000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

