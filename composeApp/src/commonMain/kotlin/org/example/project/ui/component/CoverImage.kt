package org.example.project.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.example.project.ui.theme.appColors

/**
 * All artwork in the app goes through here.
 *
 * The fallback [icon] is drawn *underneath* the image on a [appColors.backgroundSurface] tile,
 * so it shows while loading and stays put if the request fails. It is deliberately not passed
 * to `AsyncImage`'s `placeholder`/`error`: Material `ImageVector`s are black-filled, and an
 * untinted painter stretched by `ContentScale.Crop` renders as a full-bleed black glyph — which
 * on a dark theme reads as a hole in the layout rather than as missing artwork.
 *
 * Covers are square. The aspect ratio also gives the tile a height when a caller constrains
 * width only (e.g. `fillMaxWidth()`), which the fallback needs in order to lay out at all.
 */
@Composable
fun CoverImage(
    data: Any?,
    modifier: Modifier = Modifier,
    size: Dp = Dp.Unspecified,
    shape: Shape = MaterialTheme.shapes.small,
    icon: ImageVector = Icons.Rounded.MusicNote,
    onClick: (() -> Unit)? = null
) {
    val context = LocalPlatformContext.current
    val request = remember(data) {
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .clip(shape)
            .background(appColors.backgroundSurface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = appColors.iconMuted,
            modifier = Modifier.fillMaxSize(0.4f)
        )

        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
