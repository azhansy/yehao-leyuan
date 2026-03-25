package com.yehao.leyuan.feature.dino

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.yehao.leyuan.feature.animal.AnimalItem
import com.yehao.leyuan.feature.animal.loadAnimalsFromAssets
import com.yehao.leyuan.feature.animal.loadPeopleRolesFromAssets
import com.yehao.leyuan.ui.LocalAppAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** 障碍：仅用恐龙类 emoji（Unicode 标准里主要是蜥脚类 🦕、暴龙 🦖） */
private val OBSTACLE_DINO_EMOJIS = listOf("🦕", "🦖",  "🦅","🦎","🐊","🦇")

private fun randomObstacleDinoEmoji(): String = OBSTACLE_DINO_EMOJIS.random()

private fun shortVerticalSegmentsOverlap(
    aCenterX: Float,
    aBottomY: Float,
    aHeight: Float,
    aWidth: Float,
    bCenterX: Float,
    bBottomY: Float,
    bHeight: Float,
    bWidth: Float,
): Boolean {
    val xHit = abs(aCenterX - bCenterX) <= (aWidth + bWidth) * 0.5f
    val aTop = aBottomY - aHeight
    val bTop = bBottomY - bHeight
    val yHit = aTop <= bBottomY && bTop <= aBottomY
    return xHit && yHit
}

private data class Obstacle(
    val x: Float,
    val w: Float,
    val h: Float,
    val emoji: String,
    val scoredGood: Boolean = false,
)

private fun gapLerp(min: Float, max: Float, t: Float): Float = min + (max - min) * t

/** 与盒子/layout 用的 px→dp 一致，再按 fontScale 转成 sp，避免把 px 数值误当 sp。 */
private fun Density.dpToSp(dp: Dp): TextUnit = (dp.value / fontScale).sp

/** 地面线（屏高比例）。原 0.76 时沙地带约 24%H；减半后约 12%H。 */
private const val GROUND_Y_FRACTION = 0.88f

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DinoRunScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val audio = LocalAppAudio.current
    val density = LocalDensity.current
    val prefs = remember { DinoPrefs(context) }

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    var gameLevel by remember { mutableIntStateOf(prefs.getLevel()) }
    var gameSession by remember { mutableIntStateOf(0) }
    var showLevelDialog by remember { mutableStateOf(false) }

    var vocabPool by remember { mutableStateOf<List<AnimalItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        vocabPool = withContext(Dispatchers.IO) {
            loadAnimalsFromAssets(context) + loadPeopleRolesFromAssets(context)
        }
    }

    var score by remember(gameSession) { mutableIntStateOf(0) }
    var dinoFeetY by remember(gameSession) { mutableFloatStateOf(0f) }
    var dinoVy by remember(gameSession) { mutableFloatStateOf(0f) }
    val obstacles = remember(gameSession) { mutableStateListOf<Obstacle>() }
    var bgScroll by remember(gameSession) { mutableFloatStateOf(0f) }

    var overlayLesson by remember { mutableStateOf<AnimalItem?>(null) }
    var showLessonOverlay by remember { mutableStateOf(false) }
    var invUntilUptime by remember { mutableLongStateOf(0L) }
    var goodJobCooldownUntil by remember { mutableLongStateOf(0L) }
    var nearMissStreak by remember(gameSession) { mutableIntStateOf(0) }

    val lessonScrollState = rememberScrollState()
    val levelDialogScrollState = rememberScrollState()
    LaunchedEffect(showLessonOverlay) {
        if (showLessonOverlay) {
            lessonScrollState.scrollTo(0)
        }
    }

    LaunchedEffect(showLessonOverlay, overlayLesson?.english) {
        if (!showLessonOverlay) return@LaunchedEffect
        val eng = overlayLesson?.english?.trim().orEmpty()
        if (eng.isEmpty()) return@LaunchedEffect
        delay(300L)
        if (showLessonOverlay && overlayLesson?.english?.trim() == eng) {
            audio.speakAnimal(eng)
        }
    }

    val dinoBounce by animateFloatAsState(
        targetValue = if (dinoVy < -100f) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dinoBounce",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        val diff = remember(gameLevel, W, H) { dinoDifficultyForLevel(gameLevel, W, H) }
        val groundY = H * GROUND_Y_FRACTION
        /** 障碍整体上移，减轻贴地与裁切感 */
        val obstacleLiftPx = with(density) { 14.dp.toPx() }
        val dinoX = W * 0.11f
        val dinoW = W * 0.14f
        val dinoH = H * 0.22f
        /** 绘制区高于逻辑身高，保证霸王龙全身（含脚）不被裁切 */
        val dinoDrawH = dinoH * 1.42f
        val footX = dinoX + dinoW * 0.5f

        // pointerInput 的 key 不含 gameLevel 时，切换难度会沿用旧闭包，groundY/diff 过期导致误判「在空中」无法起跳
        val onTapJump: () -> Unit = lambda@{
            if (showLessonOverlay) return@lambda
            if (!dinoFeetY.isFinite() || dinoFeetY <= 0f || dinoFeetY > groundY) {
                dinoFeetY = groundY
            }
            if (dinoFeetY < groundY - 10f) return@lambda
            dinoVy = diff.jumpVelocity
            audio.speak("jump", append = true)
        }
        val latestOnTapJump by rememberUpdatedState(newValue = onTapJump)

        fun continueAfterLesson() {
            showLessonOverlay = false
            overlayLesson = null
            invUntilUptime = android.os.SystemClock.uptimeMillis() + 2800L
            nearMissStreak = 0
            obstacles.removeAll { it.x < dinoX + dinoW * 2.5f }
        }

        LaunchedEffect(gameSession, gameLevel, W, H, obstacleLiftPx) {
            dinoFeetY = groundY
            dinoVy = 0f
            obstacles.clear()
            score = 0
            bgScroll = 0f
            showLessonOverlay = false
            overlayLesson = null
            invUntilUptime = 0L
            nearMissStreak = 0
            val ow = W * 0.0275f
            val oh = H * (0.045f + Random.nextFloat() * 0.0125f)
            obstacles.add(
                Obstacle(
                    x = W * 1.02f,
                    w = ow,
                    h = oh,
                    emoji = randomObstacleDinoEmoji(),
                ),
            )
            var lastFrame = 0L
            while (isActive) {
                withFrameMillis { now ->
                    if (lastFrame == 0L) {
                        lastFrame = now
                        return@withFrameMillis
                    }
                    var dt = (now - lastFrame) / 1000f
                    lastFrame = now
                    if (dt <= 0f || dt > 0.12f) dt = 0.016f

                    if (showLessonOverlay) {
                        return@withFrameMillis
                    }

                    val inv = android.os.SystemClock.uptimeMillis() < invUntilUptime

                    dinoVy += diff.gravity * dt
                    dinoFeetY += dinoVy * dt
                    if (dinoFeetY >= groundY) {
                        dinoFeetY = groundY
                        dinoVy = 0f
                    }

                    bgScroll = (bgScroll + diff.scrollSpeedPx * 0.4f * dt) % 3000f

                    val step = diff.scrollSpeedPx * dt
                    for (i in obstacles.indices) {
                        val o = obstacles[i]
                        obstacles[i] = o.copy(x = o.x - step)
                    }
                    obstacles.removeAll { it.x + it.w < -60f }

                    val rightEdge = obstacles.maxOfOrNull { it.x + it.w } ?: 0f
                    val gap = W * gapLerp(diff.spawnGapMin, diff.spawnGapMax, Random.nextFloat())
                    if (rightEdge < W - gap || obstacles.isEmpty()) {
                        val ow2 = W * (0.025f + Random.nextFloat() * 0.009f)
                        val oh2 = H * (0.0425f + Random.nextFloat() * 0.0175f)
                        obstacles.add(
                            Obstacle(
                                x = W * 1.06f,
                                w = ow2,
                                h = oh2,
                                emoji = randomObstacleDinoEmoji(),
                            ),
                        )
                    }

                    for (i in obstacles.indices) {
                        val o = obstacles[i]
                        if (!o.scoredGood && o.x + o.w < footX) {
                            obstacles[i] = o.copy(scoredGood = true)
                            nearMissStreak += 1
                            val up = android.os.SystemClock.uptimeMillis()
                            if (nearMissStreak >= 3 && up > goodJobCooldownUntil) {
                                nearMissStreak = 0
                                goodJobCooldownUntil = up + 2400L
                                audio.playSuccess()
                                audio.speak("Good job", append = true)
                            }
                        }
                    }

                    score += kotlin.math.max(1, (diff.scrollSpeedPx * dt * 0.15f).roundToInt())

                    if (!inv) {
                        val playerSegCenterX = footX
                        val playerSegBottomY = dinoFeetY
                        val playerSegHeight = dinoDrawH * 0.5f
                        val playerSegWidth = dinoW * 0.28f
                        for (o in obstacles) {
                            val obstacleDrawH = o.h * 2.2f
                            val obstacleVisualW = maxOf(o.w, o.h * 1.4f, W * 0.078f)
                            val obstacleSegCenterX = o.x + o.w * 0.5f
                            val obstacleSegBottomY = groundY - obstacleLiftPx
                            val obstacleSegHeight = obstacleDrawH * 0.5f
                            val obstacleSegWidth = maxOf(obstacleVisualW * 0.3f, W * 0.02f)
                            if (
                                shortVerticalSegmentsOverlap(
                                    aCenterX = playerSegCenterX,
                                    aBottomY = playerSegBottomY,
                                    aHeight = playerSegHeight,
                                    aWidth = playerSegWidth,
                                    bCenterX = obstacleSegCenterX,
                                    bBottomY = obstacleSegBottomY,
                                    bHeight = obstacleSegHeight,
                                    bWidth = obstacleSegWidth,
                                )
                            ) {
                                showLessonOverlay = true
                                overlayLesson = vocabPool.randomOrNull()
                                audio.playTryAgain()
                                return@withFrameMillis
                            }
                        }
                    }
                }
            }
        }

        val sandBleedBelow = with(density) { 28.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { clip = false }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { latestOnTapJump() })
                },
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f),
            ) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF29B6F6), Color(0xFF81D4FA), Color(0xFFE1F5FE)),
                        startY = 0f,
                        endY = size.height,
                    ),
                    size = size,
                )
                val gy = size.height * GROUND_Y_FRACTION
                drawRect(Color(0xFFFFD54F), topLeft = Offset(0f, gy), size = Size(size.width, size.height - gy))
                drawRect(Color(0xFFFFCA28), topLeft = Offset(0f, gy), size = Size(size.width, 10f))
                var sx = -bgScroll % 80f
                while (sx < size.width + 80f) {
                    drawRect(
                        Color(0xFFF9A825).copy(alpha = 0.45f),
                        topLeft = Offset(sx, gy + 16f),
                        size = Size(28f, size.height - gy - 16f),
                    )
                    sx += 56f
                }
            }

            Text(
                text = "☁️",
                fontSize = 42.sp,
                modifier = Modifier
                    .zIndex(3f)
                    .offset(
                        x = with(density) { ((bgScroll * 0.08f) % (W * 0.4f)).toDp() },
                        y = 12.dp,
                    )
                    .padding(start = 8.dp),
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = "☁️",
                fontSize = 36.sp,
                modifier = Modifier
                    .zIndex(3f)
                    .offset(
                        x = with(density) { ((bgScroll * 0.05f + W * 0.35f) % (W * 0.5f)).toDp() },
                        y = 28.dp,
                    )
                    .padding(start = 8.dp),
                color = Color.White.copy(alpha = 0.7f),
            )

            Row(
                modifier = Modifier
                    .zIndex(4f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        onBack()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color(0xFFC62828),
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "🦖 霸王龙跑跑",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF01579B),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "$score",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(end = 8.dp),
                )
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        showLevelDialog = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color(0xFF0277BD),
                    ),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "难度")
                }
            }

            Text(
                text = "霸王龙点屏跳跃 · 第 ${gameLevel} 级",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F),
                modifier = Modifier
                    .zIndex(4f)
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp),
            )

            for (o in obstacles) {
                val oDrawH = o.h * 2.2f
                val oVisualW = maxOf(o.w, o.h * 1.4f, W * 0.078f)
                val visualLeft = o.x + o.w * 0.5f - oVisualW * 0.5f
                val oBoxH = oDrawH + sandBleedBelow
                val obstacleFontSize = with(density) {
                    val rawDp = minOf(oDrawH.toDp(), oVisualW.toDp()) * 0.9f
                    val clampedDp = rawDp.coerceIn(44.sp.toDp(), 102.sp.toDp())
                    dpToSp(clampedDp)
                }
                val obstacleLineHeight = with(density) {
                    dpToSp(obstacleFontSize.toDp() * 1.22f)
                }
                Box(
                    modifier = Modifier
                        .zIndex(2f)
                        .offset(
                            x = with(density) { visualLeft.toDp() },
                            y = with(density) { (groundY - oDrawH - obstacleLiftPx).toDp() },
                        )
                        .width(with(density) { oVisualW.toDp() })
                        .height(with(density) { oBoxH.toDp() })
                        .graphicsLayer { clip = false },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = o.emoji,
                        fontSize = obstacleFontSize,
                        lineHeight = obstacleLineHeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = with(density) { sandBleedBelow.toDp() }),
                    )
                }
            }

            val invShow = android.os.SystemClock.uptimeMillis() < invUntilUptime
            val playerBoxH = dinoDrawH + sandBleedBelow
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .offset(
                        x = with(density) { dinoX.toDp() },
                        y = with(density) { (dinoFeetY - dinoDrawH).toDp() },
                    )
                    .width(with(density) { dinoW.toDp() })
                    .height(with(density) { playerBoxH.toDp() })
                    .scale(dinoBounce)
                    .then(
                        if (invShow) Modifier.background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        else Modifier,
                    )
                    .graphicsLayer { clip = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                val playerFontSize = with(density) {
                    val rawDp = dinoDrawH.toDp() * 0.78f
                    val clampedDp = rawDp.coerceIn(56.sp.toDp(), 112.sp.toDp())
                    dpToSp(clampedDp)
                }
                val playerLineHeight = with(density) {
                    dpToSp(playerFontSize.toDp() * 1.22f)
                }
                Text(
                    text = "🦖",
                    fontSize = playerFontSize,
                    lineHeight = playerLineHeight,
                    modifier = Modifier
                        .padding(bottom = with(density) { sandBleedBelow.toDp() })
                        .graphicsLayer { scaleX = -1f },
                )
            }

            if (showLessonOverlay) {
                val item = overlayLesson
                val cardMaxH = with(density) { (H * 0.88f).toDp() }
                Box(
                    modifier = Modifier
                        .zIndex(8f)
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .widthIn(max = 300.dp)
                            .heightIn(max = cardMaxH)
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(24.dp))
                            .verticalScroll(lessonScrollState)
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Text(
                            "哎呀，碰到啦！",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD84315),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "稍后会朗读英语单词，听完点下面继续跑",
                            fontSize = 16.sp,
                            color = Color(0xFF5D4037),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            textAlign = TextAlign.Center,
                        )
                        if (item != null) {
                            Text(
                                text = item.emoji,
                                fontSize = 64.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = item.english,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1565C0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                textAlign = TextAlign.Center,
                            )
                            if (item.chinese.isNotBlank()) {
                                Text(
                                    text = item.chinese,
                                    fontSize = 20.sp,
                                    color = Color(0xFF6D4C41),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            Text(
                                "🎈",
                                fontSize = 56.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Keep going!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            onClick = {
                                audio.playSoftClick()
                                continueAfterLesson()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        ) {
                            Text("继续跑！", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (showLevelDialog) {
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.widthIn(max = 320.dp),
            title = { Text("霸王龙跑跑 · 难度 1～9", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(levelDialogScrollState),
                ) {
                    Text(
                        "数字越大，障碍略快、间距略短。本页横屏，退出本游戏即恢复竖屏。",
                        fontSize = 14.sp,
                        color = Color(0xFF607D8B),
                    )
                    Spacer(Modifier.height(8.dp))
                    for (lv in 1..9) {
                        val d = dinoDifficultyForLevel(lv, 800f, 480f)
                        val sel = lv == gameLevel
                        TextButton(
                            onClick = {
                                audio.playSoftClick()
                                prefs.setLevel(lv)
                                gameLevel = lv
                                gameSession = gameSession + 1
                                showLevelDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "第 $lv 级 · ${d.summaryZh}",
                                fontWeight = if (sel) FontWeight.Black else FontWeight.Normal,
                                color = if (sel) Color(0xFFE65100) else Color(0xFF37474F),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelDialog = false }) { Text("关闭") }
            },
        )
    }
}
