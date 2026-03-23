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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.GrapePurple
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlinx.coroutines.delay

@Composable
fun AnimalIdentificationScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val context = LocalContext.current
    var animals by remember { mutableStateOf<List<AnimalItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        animals = loadAnimalsFromAssets(context)
    }

    var pressedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(pressedIndex) {
        if (pressedIndex >= 0) {
            delay(280)
            pressedIndex = -1
        }
    }

    val titleBrush = Brush.linearGradient(
        listOf(GrapePurple, CherryRed, SkyBlue, GrassGreen)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF3E5F5), Color(0xFFE8F5E9))
                )
            )
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick = {
                    audio.playSoftClick()
                    onBack()
                },
                modifier = Modifier.align(Alignment.CenterStart),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    contentColor = Color(0xFF5E35B1),
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "动物王国",
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
            text = "点一点，听英文名字",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5E35B1),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        if (animals.isEmpty()) {
            Text(
                text = "正在加载动物…\n若一直为空，请检查 assets/animals.json",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF78909C),
                fontSize = 18.sp
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp
            ) {
                itemsIndexed(
                    items = animals,
                    key = { index, item -> "${item.english}_$index" }
                ) { index, item ->
                    val selected = pressedIndex == index
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "animalScale"
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
                                indication = null
                            ) {
                                pressedIndex = index
                                audio.playSoftClick()
                                audio.speakAnimal(item.english)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 120.dp + heightExtra)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            item.tint.copy(alpha = 0.35f),
                                            Color.White
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.emoji,
                                fontSize = when (index % 4) {
                                    0 -> 70.sp
                                    1 -> 58.sp
                                    2 -> 64.sp
                                    else -> 62.sp
                                }
                            )
                        }
                        Text(
                            text = item.english,
                            fontSize = (20 + (index % 3)).sp,
                            fontWeight = FontWeight.Bold,
                            color = item.tint,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                        if (item.chinese.isNotEmpty()) {
                            Text(
                                text = item.chinese,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF546E7A),
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
