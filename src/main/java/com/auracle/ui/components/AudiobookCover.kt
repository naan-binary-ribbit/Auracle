package com.auracle.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun AudiobookCover(
    coverArt: ByteArray?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape? = null,
    fallbackIconSize: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (coverArt != null) {
            Image(
                painter = rememberAsyncImagePainter(coverArt),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(fallbackIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
