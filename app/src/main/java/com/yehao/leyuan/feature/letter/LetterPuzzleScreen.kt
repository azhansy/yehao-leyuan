package com.yehao.leyuan.feature.letter

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.feature.animal.pickRandomPuzzleWord
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.celebration.VictoryCelebrationOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.OrangePop
import com.yehao.leyuan.ui.theme.PinkBubble
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlin.math.roundToInt

private val LetterColors = listOf(CherryRed, SkyBlue, GrassGreen, OrangePop, PinkBubble)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterPuzzleScreen() {
    val audio = LocalAppAudio.current
    val context = LocalContext.current
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
                audio.speakLettersThenWord(word)
                audio.speakVictoryPraise(append = true)
            }
            return
        }
        dragIndex = null
        dragOffset = Offset.Zero
    }

    val titleBrush = Brush.horizontalGradient(
        listOf(CherryRed, OrangePop, SkyBlue, GrassGreen)
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFDE7), Color(0xFFE1F5FE))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        showLevelSettings = true
                    },
                    enabled = !showGameOver,
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.85f),
                        contentColor = Color(0xFF7E57C2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "游戏等级",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "拼出单词!",
                    style = MaterialTheme.typography.displayLarge.merge(
                        TextStyle(brush = titleBrush, fontWeight = FontWeight.ExtraBold)
                    ),
                    fontSize = 42.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 52.dp, end = 16.dp)
                )
            }
            Text(
                text = "第 ${gameLevel} 级 · ${difficulty.summaryZh}",
                fontSize = 13.sp,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱ ${timeLeftSec}s",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timeLeftSec <= 5) Color(0xFFE53935) else Color(0xFF00897B)
                )
                Text(
                    text = "得分 $sessionScore",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )
                Text(
                    text = "已对 $wordsCleared 词",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF546E7A)
                )
            }

            Text(
                text = pick.emoji,
                fontSize = 88.sp,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !showGameOver
                    ) {
                        audio.playSoftClick()
                        audio.speak(word.lowercase())
                    }
            )
            Text(
                text = word.lowercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Bold
            )
            if (pick.chinese.isNotBlank()) {
                Text(
                    text = pick.chinese,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in letters.indices) {
                    val c = letters[i]
                    key("slot_${roundKey}_${i}_$c") {
                        val filled = placed[i] && placedChar[i] != null
                        val scale by animateFloatAsState(
                            targetValue = if (filled) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "slot"
                        )
                        Box(
                            modifier = Modifier
                                .size((72 * scale).dp)
                                .onGloballyPositioned { coords ->
                                    val p = coords.positionInRoot()
                                    val s = coords.size
                                    slotRects[i] = Rect(
                                        p.x,
                                        p.y,
                                        p.x + s.width,
                                        p.y + s.height
                                    )
                                }
                                .background(
                                    if (filled) Color(0xFFC8E6C9) else Color.White.copy(alpha = 0.9f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (filled) {
                                Text(
                                    text = "${placedChar[i]}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2E7D32)
                                )
                            } else {
                                Text(
                                    text = "?",
                                    fontSize = 28.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "拖动下面的字母到方框里",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF546E7A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val minSide = kotlin.math.min(maxWidth.value, maxHeight.value)

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
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
                                            IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                        }
                                        .onGloballyPositioned { coords ->
                                            val p = coords.positionInRoot()
                                            val s = coords.size
                                            dragRectInRoot = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                                        }
                                } else Modifier

                                Box(
                                    modifier = dragModifier
                                        .size(72.dp)
                                        .pointerInput(letterIndex, word, roundKey, placed[letterIndex], showGameOver) {
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
                                        }
                                        .background(
                                            LetterColors[letterIndex % LetterColors.size],
                                            RoundedCornerShape(18.dp)
                                        ),
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
