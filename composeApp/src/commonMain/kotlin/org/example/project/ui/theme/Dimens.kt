package org.example.project.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized sizing tokens. All UI dimensions should reference these instead of
 * hardcoded dp values so spacing stays on a consistent 4pt grid.
 *
 * Scales:
 *  - Spacing  : 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32  (padding, gaps, spacers)
 *  - Radius   : 4 / 8 / 12 / 16 / 24 / 32           (corner rounding)
 *  - Icon     : 16 / 24 / 28 / 32                   (icon glyph sizes)
 *  - Stroke   : 0.5 / 1 / 2                         (borders, dividers, indicators)
 *  - Size.*   : fixed component dimensions (cover art, buttons, bars…)
 */
object Dimens {
    // ── Spacing (4pt grid) ───────────────────────────
    val spaceXxs = 2.dp
    val spaceXs = 4.dp
    val spaceS = 8.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXl = 20.dp
    val spaceXxl = 24.dp
    val space3xl = 32.dp

    // ── Corner radius ────────────────────────────────
    val radiusS = 4.dp
    val radiusM = 8.dp
    val radiusL = 12.dp
    val radiusXl = 16.dp
    val radius2xl = 24.dp
    val radius3xl = 32.dp

    // ── Icon sizes ───────────────────────────────────
    val iconXs = 16.dp
    val iconS = 24.dp
    val iconM = 28.dp
    val iconL = 32.dp
    val iconXl = 40.dp

    // ── Stroke / hairline ────────────────────────────
    val borderHairline = 0.5.dp
    val strokeThin = 1.dp
    val strokeThick = 2.dp
    val strokeXThick = 3.dp

    // ── Elevation ────────────────────────────────────
    val elevationLow = 1.dp
    val elevationMedium = 4.dp
    val elevationHigh = 8.dp

    // ── Fixed component sizes ────────────────────────
    object Size {
        val coverThumb = 48.dp        // song-row / mini-player artwork (was 48 & 50)
        val coverCardWidth = 128.dp   // home recently-played card (was 130)
        val albumArtLarge = 320.dp    // now-playing artwork
        val playlistCoverInset = 80.dp // horizontal inset framing the playlist cover
        val iconChip = 56.dp          // liked-songs hero chip
        val heroCardHeight = 100.dp   // liked-songs banner
        val playButton = 64.dp        // primary play/pause FAB
        val miniPlayerHeight = 64.dp  // mini player bar (was 65)
        val bottomBarHeight = 64.dp   // bottom nav bar (was 60)
        val dragHandleWidth = 32.dp   // sheet/pager grab handle (was 36)
        val pillWidth = 96.dp         // pill-shaped chip / history pill (was 99)
        val sliderThumbMin = 16.dp    // progress thumb at rest (was 14)
        val sliderThumbMax = 20.dp    // progress thumb while dragging
        val equalizerBarHeight = 20.dp
    }
}
