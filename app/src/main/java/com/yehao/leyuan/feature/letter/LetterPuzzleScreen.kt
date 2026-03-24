package com.yehao.leyuan.feature.letter

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.feature.animal.pickRandomPuzzleWord
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.celebration.VictoryCelebrationOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.CreamBackground
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.OrangePop
import com.yehao.leyuan.ui.theme.PinkBubble
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlin.math.roundToInt

/**
 * 字母拼图 — UI 对齐 Stitch 稿结构（线索区 / 作答条 / 字母池分层）。
 * 参考：https://stitch.withgoogle.com/projects/13385921442945725597?node-id=135425461d06438294207d4f1f3abde6
 */
private val LetterColors = listOf(CherryRed, SkyBlue, GrassGreen, OrangePop, PinkBubble)

/**
 * 与同应用内「家庭成员英语」「地图首页」等一致的竖直渐变：奶油黄 → 浅水色 → 浅天蓝。
 * （与家庭成员英语页、地图首页等使用的暖色→天蓝渐变同一思路。）
 */
private val LetterPuzzleBackgroundGradient = listOf(
    CreamBackground,
    Color(0xFFB2EBF2),
    Color(0xFFE1F5FE),
)

/** Google Stitch / M3 常见导出：浅底、蓝强调、白卡片、作答条与字母池分区 */
private val StitchInk = Color(0xFF202124)
private val StitchMuted = Color(0xFF5F6368)
private val StitchAccent = Color(0xFF1967D2)
private val StitchCard = Color.White
private val StitchSlotStrip = Color(0xFFE8F0FE)
private val StitchSlotBorder = Color(0xFF669DF6)
private val StitchSlotEmpty = Color.White
private val StitchPoolSurface = Color(0xFFF1F3F4)
private val StitchChipBg = Color.White.copy(alpha = 0.92f)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterPuzzleScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val context = LocalContext.current

    DisposableEffect(audio) {
        onDispose {
            audio.stopYoudaoPlayback()
        }
    }
    val prefs = remember { LetterPuzzlePrefs(context) }
    var gameLevel by remember { mutableIntStateOf(prefs.getLevel()) }
    val difficulty = remember(gameLevel) { difficultyForLevel(gameLevel) }
    var roundKey by remember { mutableIntStateOf(0) }
    var showLevelSettings by remember { mutableStateOf(false) }
    var roundSolved by remember { mutableStateOf(false) }
    var timeLeftSec by remember { mutableIntStateOf(0) }
    var sessionScore by remember { mutableIntStateOf(0) }
    var wordsCleared by remember { mutableIntStateOf(0) }
    var showGameOver by remember { mutableStateOf(false) }
    var lastRoundPoints by remember { mutableIntStateOf(0) }

    val latestRoundSolved by rememberUpdatedState(roundSolved)
    val latestRoundKey by rememberUpdatedState(roundKey)
    val scope = rememberCoroutineScope()

    val pick = remember(roundKey, gameLevel) {
        val d = difficultyForLevel(gameLevel)
        pickRandomPuzzleWord(context, maxLetters = d.maxLetters)
    }
    val word = pick.word
    val letters = remember(word) { word.toList() }
    val poolIndices = remember(roundKey, word, difficulty.shufflePool) {
        if (difficulty.shufflePool) word.indices.shuffled() else word.indices.toList()
    }

    val placed = remember(roundKey, word) { mutableStateListOf(*Array(letters.size) { false }) }
    val placedChar = remember(roundKey, word) { mutableStateListOf(*Array<Char?>(letters.size) { null }) }

    var showVictoryCelebration by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragRectInRoot by remember { mutableStateOf<Rect?>(null) }

    val slotRects = remember(roundKey, word) {
        mutableStateListOf<Rect>().apply { repeat(letters.size) { add(Rect.Zero) } }
    }

    LaunchedEffect(roundKey, gameLevel, word) {
        val limit = difficultyForLevel(gameLevel).timeLimitSeconds
        timeLeftSec = limit
        while (timeLeftSec > 0) {
            delay(1000)
            if (latestRoundSolved) break
            timeLeftSec--
        }
        if (!latestRoundSolved && timeLeftSec <= 0) {
            showGameOver = true
            audio.playTryAgain()
            audio.speak("Time is up. Good try!")
        }
    }

    fun trySnap(char: Char, letterIndex: Int) {
        if (showGameOver) return
        val rect = dragRectInRoot ?: return
        val center = rect.center
        for (i in slotRects.indices) {
            if (placed[i]) continue
            if (!slotRects[i].contains(center)) continue
            if (letters[i] != char) continue
            if (i != letterIndex) continue
            placed[i] = true
            placedChar[i] = char
            audio.playSuccess()
            dragIndex = null
            dragOffset = Offset.Zero
            if (placed.all { it }) {
                roundSolved = true
                val pts = scoreForSolvedWord(timeLeftSec, word.length, gameLevel)
                lastRoundPoints = pts
                sessionScore += pts
                wordsCleared++
                showVictoryCelebration = true
                val tokenRound = roundKey
                val w = word
                scope.launch {
                    delay(1000)
                    if (tokenRound != latestRoundKey || !latestRoundSolved) return@launch
                    audio.speakLettersThenWord(w)
                    audio.speakVictoryPraise(append = true)
                }
            }
            return
        }
        dragIndex = null
        dragOffset = Offset.Zero
    }

    fun restartSessionAfterGameOver() {
        showGameOver = false
        sessionScore = 0
        wordsCleared = 0
        roundSolved = false
        showVictoryCelebration = false
        dragIndex = null
        dragOffset = Offset.Zero
        dragRectInRoot = null
        roundKey++
        audio.playSoftClick()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(LetterPuzzleBackgroundGradient)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        audio.stopYoudaoPlayback()
                        onBack()
                    },
                    enabled = !showGameOver,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = StitchCard,
                        contentColor = StitchInk
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "字母拼图",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        showLevelSettings = true
                    },
                    enabled = !showGameOver,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = StitchCard,
                        contentColor = StitchAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "游戏等级",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = "第 ${gameLevel} 级 · ${difficulty.summaryZh}",
                fontSize = 13.sp,
                color = StitchMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchChipBg,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = "⏱ ${timeLeftSec}s",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (timeLeftSec <= 5) Color(0xFFD93025) else Color(0xFF137333)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchChipBg,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = "得分 $sessionScore",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchAccent
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchChipBg,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = "已对 $wordsCleared 词",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = StitchMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "看一看，拼一拼",
                        style = MaterialTheme.typography.labelLarge,
                        color = StitchMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = pick.emoji,
                        fontSize = 96.sp,
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !showGameOver
                            ) {
                                audio.playSoftClick()
                                audio.speakAnimal(word)
                            }
                    )
                    if (pick.chinese.isNotBlank()) {
                        Text(
                            text = pick.chinese,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchInk,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = word.lowercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = StitchAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "把字母拖进横条里",
                style = MaterialTheme.typography.labelMedium,
                color = StitchMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = StitchSlotStrip,
                shadowElevation = 2.dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in letters.indices) {
                        val c = letters[i]
                        key("slot_${roundKey}_${i}_$c") {
                            val filled = placed[i] && placedChar[i] != null
                            val scale by animateFloatAsState(
                                targetValue = if (filled) 1.06f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "slot"
                            )
                            Card(
                                modifier = Modifier
                                    .size((74 * scale).dp)
                                    .onGloballyPositioned { coords ->
                                        val p = coords.positionInRoot()
                                        val s = coords.size
                                        slotRects[i] = Rect(
                                            p.x,
                                            p.y,
                                            p.x + s.width,
                                            p.y + s.height
                                        )
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (filled) Color(0xFFC6F6D5) else StitchSlotEmpty
                                ),
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (filled) Color(0xFF137333) else StitchSlotBorder
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (filled) 3.dp else 1.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (filled) {
                                        Text(
                                            text = "${placedChar[i]}",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0D652D)
                                        )
                                    } else {
                                        Text(
                                            text = "_",
                                            fontSize = 26.sp,
                                            color = StitchSlotBorder.copy(alpha = 0.45f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = StitchPoolSurface,
                tonalElevation = 1.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "👆 按住下面的圆字母，拖到横条空格中",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 14.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val minSide = kotlin.math.min(maxWidth.value, maxHeight.value)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (letterIndex in poolIndices) {
                                val char = letters[letterIndex]
                                key("pool_${roundKey}_${letterIndex}_$char") {
                                    val isPlaced = placed[letterIndex] && placedChar[letterIndex] == char
                                    if (isPlaced) {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    } else {
                                        val isDragging = dragIndex == letterIndex
                                        val dragModifier = if (isDragging) {
                                            Modifier
                                                .offset {
                                                    IntOffset(
                                                        dragOffset.x.roundToInt(),
                                                        dragOffset.y.roundToInt()
                                                    )
                                                }
                                                .onGloballyPositioned { coords ->
                                                    val p = coords.positionInRoot()
                                                    val s = coords.size
                                                    dragRectInRoot = Rect(
                                                        p.x,
                                                        p.y,
                                                        p.x + s.width,
                                                        p.y + s.height
                                                    )
                                                }
                                        } else Modifier

                                        Card(
                                            modifier = dragModifier
                                                .size(72.dp)
                                                .pointerInput(
                                                    letterIndex,
                                                    word,
                                                    roundKey,
                                                    placed[letterIndex],
                                                    showGameOver
                                                ) {
                                                    if (showGameOver || placed[letterIndex]) return@pointerInput
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            dragIndex = letterIndex
                                                            dragOffset = Offset.Zero
                                                            audio.playSoftClick()
                                                            audio.speakDraggingLetter(char)
                                                        },
                                                        onDrag = { change, amount ->
                                                            change.consume()
                                                            dragOffset += amount
                                                        },
                                                        onDragEnd = {
                                                            trySnap(char, letterIndex)
                                                        },
                                                        onDragCancel = {
                                                            dragIndex = null
                                                            dragOffset = Offset.Zero
                                                        }
                                                    )
                                                },
                                            shape = CircleShape,
                                            colors = CardDefaults.cardColors(
                                                containerColor = LetterColors[letterIndex % LetterColors.size]
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$char",
                                                    color = Color.White,
                                                    fontSize = (minSide * 0.12f).coerceIn(28f, 40f).sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VictoryCelebrationOverlay(
            visible = showVictoryCelebration,
            onDismiss = {
                showVictoryCelebration = false
                roundSolved = false
                roundKey++
            },
            subtitle = "本词 +$lastRoundPoints 分！你拼出了 ${word.lowercase()}！做得好！",
            continueLabel = "下一词"
        )

        if (showGameOver) {
            LetterPuzzleGameOverDialog(
                level = gameLevel,
                totalScore = sessionScore,
                wordsCleared = wordsCleared,
                onPlayAgain = { restartSessionAfterGameOver() }
            )
        }

        if (showLevelSettings) {
            LetterPuzzleLevelSettingsDialog(
                currentLevel = gameLevel,
                onDismiss = { showLevelSettings = false },
                onSelectLevel = { lv ->
                    prefs.setLevel(lv)
                    gameLevel = lv
                    sessionScore = 0
                    wordsCleared = 0
                    showVictoryCelebration = false
                    showGameOver = false
                    roundSolved = false
                    dragIndex = null
                    dragOffset = Offset.Zero
                    dragRectInRoot = null
                    roundKey++
                    showLevelSettings = false
                    audio.playSoftClick()
                }
            )
        }
    }
}
