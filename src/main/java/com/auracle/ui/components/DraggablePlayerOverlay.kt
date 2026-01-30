package com.auracle.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun DraggablePlayerOverlay(
    offsetFraction: Float,
    onDrag: (deltaFraction: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var heightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (offsetFraction * heightPx).roundToInt()) }
                .pointerInput(offsetFraction) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (heightPx > 0) {
                                onDrag(dragAmount / heightPx)
                            }
                        },
                        onDragEnd = { onDragEnd() }
                    )
                }
        ) {
            content()
        }
    }
}
