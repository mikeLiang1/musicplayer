package org.example.project.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.pressableCard(onClick: () -> Unit) = composed {

    val scale = remember { Animatable(1f) }

    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    // Shrink immediately
                    scale.animateTo(
                        0.95f,
                        animationSpec = tween(durationMillis = 90)
                    )

                    // Wait until the finger is released or the gesture is cancelled
                    val released = tryAwaitRelease()

                    // Bounce back
                    scale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )

                    // Only invoke click if the gesture completed normally
                    if (released) {
                        onClick()
                    }
                }
            )
        }
}
