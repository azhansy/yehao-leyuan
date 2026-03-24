package com.yehao.leyuan.feature.animal

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.ui.LocalAppAudio

/**
 * 动物王国 / 植物园共用的瀑布流：emoji + 英文名 + 中文，点击朗读。
 */
@Composable
fun VocabularyIdentificationContent(
    title: String,
    subtitle: String,
    items: List<AnimalItem>,
    emptyMessage: String,
    onBack: () -> Unit,
    backgroundGradient: List<Color>,
    titleBrush: Brush,
    subtitleColor: Color,
    backIconTint: Color,
    backContainerColor: Color,
) {
    val audio = LocalAppAudio.current
    var pressedIndex by remember(items) { mutableIntStateOf(-1) }

    DisposableEffect(audio) {
        onDispose {
            audio.stopYoudaoPlayback()
        }
    }

    LaunchedEffect(pressedIndex) {
        if (pressedIndex >= 0) {
            delay(280)
            pressedIndex = -1
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(backgroundGradient)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        audio.stopYoudaoPlayback()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = backContainerColor,
                        contentColor = backIconTint,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.merge(
                        TextStyle(brush = titleBrush, fontWeight = FontWeight.ExtraBold),
                    ),
                    fontSize = 38.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp, vertical = 8.dp),
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = subtitleColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )

            if (items.isEmpty()) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF78909C),
                    fontSize = 18.sp,
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item -> "${item.english}_${item.emoji}_$index" },
                    ) { index, item ->
                        val selected = pressedIndex == index
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "vocabScale",
                        )
                        val heightExtra = when (index % 5) {
                            0 -> 36.dp
                            1 -> 0.dp
                            2 -> 20.dp
                            3 -> 8.dp
                            else -> 24.dp
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    pressedIndex = index
                                    audio.playSoftClick()
                                    audio.speakAnimal(item.english)
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 120.dp, height = 120.dp + heightExtra)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                item.tint.copy(alpha = 0.35f),
                                                Color.White,
                                            ),
                                        ),
                                        shape = RoundedCornerShape(28.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item.emoji,
                                    fontSize = when (index % 4) {
                                        0 -> 70.sp
                                        1 -> 58.sp
                                        2 -> 64.sp
                                        else -> 62.sp
                                    },
                                )
                            }
                            Text(
                                text = item.english,
                                fontSize = (20 + (index % 3)).sp,
                                fontWeight = FontWeight.Bold,
                                color = item.tint,
                                modifier = Modifier.padding(top = 10.dp),
                                textAlign = TextAlign.Center,
                            )
                            if (item.chinese.isNotEmpty()) {
                                Text(
                                    text = item.chinese,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF546E7A),
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
