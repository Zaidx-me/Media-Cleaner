package com.zaidxme.whatsappcleaner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.zaidxme.whatsappcleaner.Constants
import com.zaidxme.whatsappcleaner.R
import com.zaidxme.whatsappcleaner.model.ListFile
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableCardStack(
    files: List<ListFile>,
    onKeepTop: () -> Unit,
    onDeleteTop: () -> Unit,
    onOpenFile: (ListFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleCards = remember(files) {
        files.take(3).reversed()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            visibleCards.forEachIndexed { index, file ->
                val stackDepth = visibleCards.lastIndex - index

                SwipeableMediaCard(
                    file = file,
                    isTop = stackDepth == 0,
                    stackDepth = stackDepth,
                    onSwipedLeft = onDeleteTop,
                    onSwipedRight = onKeepTop,
                    onOpenFile = { onOpenFile(file) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeHint(icon = R.drawable.clean, label = "Swipe left: delete")
            SwipeHint(icon = R.drawable.open_in, label = "Tap: open")
            SwipeHint(icon = R.drawable.check_circle, label = "Swipe right: keep")
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SwipeableMediaCard(
    file: ListFile,
    isTop: Boolean,
    stackDepth: Int,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onOpenFile: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(file.filePath) { Animatable(0f) }
    val offsetY = remember(file.filePath) { Animatable(0f) }
    val swipeThreshold = 280f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .padding(top = (stackDepth * 10).dp)
            .zIndex((10 - stackDepth).toFloat())
            .graphicsLayer {
                scaleX = 1f - (stackDepth * 0.04f)
                scaleY = 1f - (stackDepth * 0.04f)
                alpha = 1f - (stackDepth * 0.12f)
                rotationZ = if (isTop) offsetX.value * 0.035f else 0f
            }
            .offset {
                IntOffset(
                    x = if (isTop) offsetX.value.roundToInt() else 0,
                    y = if (isTop) offsetY.value.roundToInt() else 0
                )
            }
            .pointerInput(isTop, file.filePath) {
                if (!isTop) return@pointerInput

                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.12f)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value <= -swipeThreshold -> {
                                    offsetX.animateTo(-1400f, tween(180))
                                    onSwipedLeft()
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }

                                offsetX.value >= swipeThreshold -> {
                                    offsetX.animateTo(1400f, tween(180))
                                    onSwipedRight()
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }

                                else -> {
                                    offsetX.animateTo(0f, tween(180))
                                    offsetY.animateTo(0f, tween(180))
                                }
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = {
            if (isTop) onOpenFile()
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                file.extension.lowercase() in Constants.EXTENSIONS_IMAGE -> {
                    GlideImage(
                        model = file,
                        contentScale = ContentScale.Crop,
                        loading = placeholder(R.drawable.image),
                        failure = placeholder(R.drawable.error),
                        contentDescription = "media"
                    )
                }

                file.extension.lowercase() in Constants.EXTENSIONS_VIDEO -> {
                    GlideImage(
                        model = file,
                        contentScale = ContentScale.Crop,
                        loading = placeholder(R.drawable.image),
                        failure = placeholder(R.drawable.error),
                        contentDescription = "video"
                    )

                    Icon(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                            .padding(10.dp),
                        painter = painterResource(id = R.drawable.video),
                        contentDescription = "video",
                    )
                }

                file.extension.lowercase() in Constants.EXTENSIONS_AUDIO -> {
                    Icon(
                        modifier = Modifier
                            .size(96.dp)
                            .align(Alignment.Center),
                        painter = painterResource(id = R.drawable.audio),
                        contentDescription = "audio"
                    )
                }

                file.extension.lowercase() in Constants.EXTENSIONS_DOCS -> {
                    Icon(
                        modifier = Modifier
                            .size(96.dp)
                            .align(Alignment.Center),
                        painter = painterResource(id = R.drawable.document),
                        contentDescription = "document"
                    )
                }

                else -> {
                    Icon(
                        modifier = Modifier
                            .size(96.dp)
                            .align(Alignment.Center),
                        painter = painterResource(id = R.drawable.unknown),
                        contentDescription = "unknown"
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(12.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${file.extension.uppercase()}  •  ${file.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SwipeHint(icon: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
