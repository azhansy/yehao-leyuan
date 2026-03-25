@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.yehao.leyuan.feature.race

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.yehao.leyuan.feature.animal.AnimalItem
import com.yehao.leyuan.feature.animal.loadAnimalsFromAssets
import com.yehao.leyuan.feature.animal.loadPeopleRolesFromAssets
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.yehao.leyuan.ui.LocalAppAudio
import kotlin.math.roundToInt
import kotlin.random.Random

private data class Obstacle(
    val lane: Int,
    val y: Float,
    val emoji: String,
    /** 障碍块着色（来自动物 tint） */
    val accent: Color,
)

private val RoadDark = Color(0xFF37474F)
private val RoadStripe = Color(0xFFECEFF1)
private val CarBody = Color(0xFFE53935)
private val CarWindow = Color(0xFF90CAF9)
private val RockColor = Color(0xFF6D4C41)

private val DefaultRaceTheme = AnimalItem(
    emoji = "🐶",
    english = "Dog",
    chinese = "狗",
    tint = CarBody,
)

private fun loadRaceVocabularyPool(context: Context): List<AnimalItem> =
    loadAnimalsFromAssets(context) + loadPeopleRolesFromAssets(context)

private fun emojiTextStyle(fontSize: TextUnit) = TextStyle(
    fontSize = fontSize,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeight = fontSize * 1.15f,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/**
 * 在 [modifier] 给定区域内，把字号从大到小试算，使 emoji 完整落在框内（不超出、不裁切内容）。
 */
@Composable
private fun EmojiFitInBox(
    emoji: String,
    modifier: Modifier = Modifier,
    maxSp: TextUnit,
    minSp: TextUnit = 8.sp,
) {
    val measurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        val fittedSp = remember(emoji, maxW, maxH, maxSp, minSp, layoutDirection, measurer) {
            if (maxW <= 0 || maxH <= 0) return@remember minSp
            val minI = minSp.value.toInt().coerceAtLeast(6)
            val maxI = maxOf(minI, maxSp.value.toInt().coerceAtMost(240))
            fun fits(spI: Int): Boolean {
                val style = emojiTextStyle(spI.sp)
                val out = measurer.measure(
                    text = AnnotatedString(emoji),
                    style = style,
                    constraints = Constraints(maxWidth = maxW, maxHeight = maxH),
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    layoutDirection = layoutDirection,
                )
                return out.size.width <= maxW && out.size.height <= maxH &&
                    !out.didOverflowWidth && !out.didOverflowHeight
            }
            var lo = minI
            var hi = maxI
            var best = minI
            while (lo <= hi) {
                val mid = (lo + hi + 1) / 2
                if (fits(mid)) {
                    best = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            best.sp
        }
        Text(
            text = emoji,
            style = emojiTextStyle(fittedSp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 局内加速：秒数 → [0,1]，约 100s 趋近上限 */
private fun sessionRamp01(seconds: Float): Float =
    (seconds * 0.01f).coerceIn(0f, 1f)

private val FallbackObstacleEmojis = listOf(
    "🦁", "🐘", "🐕", "🐱", "🐻", "🐰", "🐸", "🐧", "🐯", "🐮",
    "🐷", "🐵", "🐔", "🦆", "🐢", "🐍", "🦉", "🐴", "🦓", "🦒",
)

private fun pickRandomVocabulary(pool: List<AnimalItem>): Pair<String, Color> {
    val a = pool.randomOrNull()
    if (a != null) return a.emoji to a.tint
    return FallbackObstacleEmojis.random() to RockColor
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun LaneDodgeScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val context = LocalContext.current
    val prefs = remember { RacePrefs(context) }
    var gameLevel by remember { mutableIntStateOf(prefs.getLevel()) }
    var showLevelSettings by remember { mutableStateOf(false) }
    var sessionKey by remember { mutableIntStateOf(0) }

    var playerLane by remember(sessionKey) { mutableIntStateOf(1) }
    var playerTheme by remember(sessionKey) { mutableStateOf(DefaultRaceTheme) }
    val obstacles = remember(sessionKey) { mutableStateListOf<Obstacle>() }
    var gameOver by remember(sessionKey) { mutableStateOf(false) }
    var score by remember(sessionKey) { mutableIntStateOf(0) }
    var spawnMsLeft by remember(sessionKey) { mutableFloatStateOf(800f) }
    val playerLaneRef by rememberUpdatedState(playerLane)

    LaunchedEffect(sessionKey, gameLevel) {
        val pool = withContext(Dispatchers.IO) { loadRaceVocabularyPool(context) }
        playerTheme = pool.randomOrNull() ?: DefaultRaceTheme
        gameOver = false
        playerLane = 1
        obstacles.clear()
        score = 0
        spawnMsLeft = Random.nextLong(400, 900).toFloat()
        val diff = raceDifficultyForLevel(gameLevel)
        var lastFrame = 0L
        var crashed = false
        var sessionSeconds = 0f
        while (!crashed) {
            withFrameMillis { now ->
                if (lastFrame == 0L) {
                    lastFrame = now
                    return@withFrameMillis
                }
                val dt = (now - lastFrame) / 1000f
                lastFrame = now
                if (dt <= 0f || dt > 0.2f) return@withFrameMillis

                sessionSeconds += dt
                val ramp = sessionRamp01(sessionSeconds)
                // 等级决定基础速度，局内统一再叠乘渐进加速（上限约 +95%）
                val speedMult = 1f + ramp * 0.95f
                val scroll = diff.scrollSpeed * speedMult

                score += (dt * 22f * speedMult).roundToInt()

                for (i in obstacles.indices.reversed()) {
                    val o = obstacles[i]
                    obstacles[i] = o.copy(y = o.y + scroll * dt)
                }
                obstacles.removeAll { it.y > 1.12f }

                spawnMsLeft -= dt * 1000f
                if (spawnMsLeft <= 0f && obstacles.size < 8) {
                    val (em, accent) = pickRandomVocabulary(pool)
                    obstacles.add(Obstacle(Random.nextInt(0, 3), -0.14f, em, accent))
                    // 随局内 ramp 略缩短出障间隔（最多约加快 28%）
                    val spawnTight = 1f - 0.28f * ramp
                    val smin = (diff.spawnMinMs * spawnTight).toLong().coerceAtLeast(280L)
                    val smax = (diff.spawnMaxMs * spawnTight).toLong().coerceAtLeast(smin + 120L)
                    spawnMsLeft = Random.nextLong(smin, smax).toFloat()
                }

                // 与底部赛车可视区域大致对齐（车身加高后略扩大判定带）
                val playerYTop = 0.66f
                val playerYBottom = 0.95f
                val obsH = 0.11f
                for (o in obstacles) {
                    if (o.lane != playerLaneRef) continue
                    val top = o.y
                    val bottom = o.y + obsH
                    if (bottom > playerYTop && top < playerYBottom) {
                        crashed = true
                        gameOver = true
                        audio.playTryAgain()
                        audio.speak("Watch out, try again")
                        prefs.updateHighScore(score)
                        return@withFrameMillis
                    }
                }
            }
        }
    }

    fun moveLeft() {
        if (gameOver) return
        if (playerLane > 0) {
            playerLane--
            audio.playSoftClick()
            if (prefs.getPlaySteerTts()) audio.speak("left")
        }
    }

    fun moveRight() {
        if (gameOver) return
        if (playerLane < 2) {
            playerLane++
            audio.playSoftClick()
            if (prefs.getPlaySteerTts()) audio.speak("right")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = {
                    audio.playSoftClick()
                    onBack()
                },
                modifier = Modifier.align(Alignment.CenterStart),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF37474F),
                ),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            IconButton(
                onClick = { showLevelSettings = true },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF0277BD),
                ),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "等级设置")
            }
            Text(
                text = "赛车躲避",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 100.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF01579B),
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "得分 $score　最高 ${prefs.getHighScore()}　·　第 ${gameLevel} 级",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            fontSize = 15.sp,
            color = Color(0xFF455A64),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "点自己的赛车，听上面的英文单词",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            fontSize = 12.sp,
            color = Color(0xFF78909C),
            textAlign = TextAlign.Center,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val density = LocalDensity.current
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val laneW = w / 3f
            val obsW = (laneW * 0.64f).coerceAtLeast(24f)
            val obsHpx = (h * 0.11f).coerceAtLeast(20f)
            val carW = (laneW * 0.78f).coerceAtLeast(36f)
            val carHpx = (h * 0.22f).coerceAtLeast(56f)
            val obsWdp = with(density) { obsW.toDp() }
            val obsHdp = with(density) { obsHpx.toDp() }
            val carWdp = with(density) { carW.toDp() }
            val carHdp = with(density) { carHpx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RoadDark, RoundedCornerShape(20.dp)),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                ) {
                    repeat(3) { laneIndex ->
                        Box(Modifier.weight(1f).fillMaxHeight())
                        if (laneIndex < 2) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .padding(vertical = 4.dp)
                                    .background(RoadStripe, RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }

                for (o in obstacles) {
                    val ox = laneW * o.lane + laneW * 0.18f
                    val oy = o.y * h
                    val obstacleEmojiMaxSp =
                        (obsHpx * 0.52f / density.fontScale).coerceAtLeast(14f).sp
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
                            .size(width = obsWdp, height = obsHdp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        o.accent.copy(alpha = 0.55f),
                                        RockColor,
                                    ),
                                ),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmojiFitInBox(
                            emoji = o.emoji,
                            modifier = Modifier.fillMaxSize(),
                            maxSp = obstacleEmojiMaxSp,
                        )
                    }
                }

                val px = laneW * playerLane + laneW * 0.11f
                val py = h * 0.70f
                val carEmojiMaxSp =
                    (carHpx * 0.5f / density.fontScale).coerceAtLeast(16f).sp
                val theme = playerTheme
                Box(
                    modifier = Modifier
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .size(width = carWdp, height = carHdp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            audio.playSoftClick()
                            audio.speakAnimal(theme.english)
                        }
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    theme.tint.copy(alpha = 0.72f),
                                    CarBody.copy(alpha = 0.95f),
                                ),
                            ),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            EmojiFitInBox(
                                emoji = theme.emoji,
                                modifier = Modifier.fillMaxSize(),
                                maxSp = carEmojiMaxSp,
                            )
                        }
                        Text(
                            text = theme.english,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                            .fillMaxWidth(0.55f)
                            .height(4.dp)
                            .background(CarWindow.copy(alpha = 0.85f), RoundedCornerShape(2.dp)),
                    )
                }

                if (gameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp),
                        ) {
                            Text(
                                text = "碰到障碍啦",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "本局得分 $score",
                                fontSize = 17.sp,
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Button(
                                onClick = { sessionKey++ },
                                modifier = Modifier.padding(top = 20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CarBody),
                            ) {
                                Text("再来一局", fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { moveLeft() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !gameOver,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0288D1),
                    disabledContainerColor = Color(0xFFB0BEC5),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "左 Left")
                Spacer(Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("左", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Left",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
            Button(
                onClick = { moveRight() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !gameOver,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0288D1),
                    disabledContainerColor = Color(0xFFB0BEC5),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("右", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Right",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = "右 Right")
            }
        }
        }
    }

    if (showLevelSettings) {
        RaceLevelSettingsDialog(
            currentLevel = gameLevel,
            playSteerTts = prefs.getPlaySteerTts(),
            onPlaySteerTtsChange = { prefs.setPlaySteerTts(it) },
            onDismiss = { showLevelSettings = false },
            onSelectLevel = { lv ->
                prefs.setLevel(lv)
                gameLevel = lv
                sessionKey++
                showLevelSettings = false
            },
        )
    }
}
