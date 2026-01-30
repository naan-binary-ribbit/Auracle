package com.auracle.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ExpressiveProgressBar(
    progress: Float, // 0.0 to 1.0
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    isWaveActive: Boolean = true
) {
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = (draggingProgress ?: progress).coerceIn(0f, 1f)
    val waveActive = isWaveActive || draggingProgress != null
    val thumbScale by animateFloatAsState(
        targetValue = if (draggingProgress != null) 1.15f else 1f,
        animationSpec = tween(100),
        label = "thumbScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onProgressChange(offset.x / size.width)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingProgress = offset.x / size.width
                    },
                    onDragEnd = {
                        draggingProgress?.let { onProgressChange(it) }
                        draggingProgress = null
                    },
                    onDragCancel = {
                        draggingProgress = null
                    },
                    onDrag = { change, _ ->
                        draggingProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val heightPx = size.height
            val centerY = heightPx / 2
            val progressWidth = width * displayProgress
            val strokeWidthPx = minOf(4.dp.toPx(), heightPx * 0.5f)
            val thumbRadiusPx = minOf(8.dp.toPx(), heightPx * 0.45f)
            val waveAmplitudePx = minOf(4.dp.toPx(), heightPx * 0.2f)

            // Draw Inactive (Straight)
            drawLine(
                color = inactiveColor,
                start = Offset(progressWidth, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )

            // Draw Active (Wavy if playing or dragging, else straight)
            if (waveActive && displayProgress > 0) {
                val path = Path()
                path.moveTo(0f, centerY)
                
                val waveFrequency = 0.05f
                
                for (x in 0..progressWidth.toInt()) {
                    val y = centerY + sin(x * waveFrequency + phase) * waveAmplitudePx
                    path.lineTo(x.toFloat(), y)
                }
                
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            } else {
                drawLine(
                    color = activeColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressWidth, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw Thumb (slightly larger while dragging)
            drawCircle(
                color = activeColor,
                radius = thumbRadiusPx * thumbScale,
                center = Offset(progressWidth, centerY)
            )
        }
    }
}
