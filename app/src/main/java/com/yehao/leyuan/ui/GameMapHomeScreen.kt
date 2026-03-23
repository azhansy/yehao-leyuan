package com.yehao.leyuan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yehao.leyuan.navigation.AppDestinations
import com.yehao.leyuan.ui.theme.SkyBlue

private data class SpotStyle(
    val emoji: String,
    val blurb: String,
    val icon: ImageVector,
    val gradient: List<Color>,
)

private fun styleFor(dest: AppDestinations): SpotStyle = when (dest) {
    AppDestinations.LetterPuzzle -> SpotStyle(
        emoji = "🔤",
        blurb = "拖动字母拼单词",
        icon = Icons.Rounded.TextFields,
        gradient = listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6)),
    )
    AppDestinations.Animals -> SpotStyle(
        emoji = "🦁",
        blurb = "点点小动物听英文名",
        icon = Icons.Rounded.Pets,
        gradient = listOf(Color(0xFF66BB6A), Color(0xFF43A047)),
    )
    AppDestinations.Shapes -> SpotStyle(
        emoji = "⭐",
        blurb = "形状拖一拖配对",
        icon = Icons.Rounded.Category,
        gradient = listOf(Color(0xFFAB47BC), Color(0xFF8E24AA)),
    )
    AppDestinations.Math -> SpotStyle(
        emoji = "🔢",
        blurb = "简单加减算一算",
        icon = Icons.Rounded.Calculate,
        gradient = listOf(Color(0xFFFF7043), Color(0xFFE53935)),
    )
    AppDestinations.Racing -> SpotStyle(
        emoji = "🏎️",
        blurb = "三车道躲小动物，越久越快",
        icon = Icons.Rounded.DirectionsCar,
        gradient = listOf(Color(0xFFFFB74D), Color(0xFFFF9800)),
    )
    else -> SpotStyle("🎮", "", Icons.Rounded.TextFields, listOf(SkyBlue, SkyBlue))
}

@Composable
fun GameMapHomeScreen(navController: NavController) {
    val audio = LocalAppAudio.current
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF87CEEB), Color(0xFFB2EBF2), Color(0xFFC8E6C9)),
                ),
            ),
    ) {
        // 远景「云朵」装饰
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 24.dp, y = 56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(x = 220.dp, y = 88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF81C784).copy(alpha = 0.55f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Text(
                text = "烨浩乐园",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1B5E20),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "乐园地图 · 点一块地盘开始玩",
                fontSize = 16.sp,
                color = Color(0xFF33691E),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                textAlign = TextAlign.Center,
            )

            AppDestinations.games.forEachIndexed { index, dest ->
                val style = styleFor(dest)
                val zigzag = if (index % 2 == 0) 0.dp else 20.dp
                MapGameSpotCard(
                    title = dest.label,
                    style = style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = zigzag, bottom = 14.dp),
                    onClick = {
                        audio.playSoftClick()
                        navController.navigate(dest.route)
                    },
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable {
                        audio.playSoftClick()
                        navController.navigate(AppDestinations.About.route)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF546E7A),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "关于与朗读设置",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF37474F),
                        )
                        Text(
                            text = "版本信息 · 切换朗读声音（小米等可试「跟随系统」）",
                            fontSize = 13.sp,
                            color = Color(0xFF78909C),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text("›", fontSize = 20.sp, color = Color(0xFF90A4AE))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MapGameSpotCard(
    title: String,
    style: SpotStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            style.gradient[0].copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.3f),
                            style.gradient.last().copy(alpha = 0.18f),
                        ),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(style.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = style.emoji, fontSize = 34.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.gradient[0],
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238),
                    )
                }
                Text(
                    text = style.blurb,
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = "▶",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = style.gradient[0],
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}
