package com.yehao.leyuan.feature.math

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.celebration.VictoryCelebrationOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.OrangePop
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class MathOp { ADD, SUB, MUL, DIV }

private data class MathProblem(
    val left: Int,
    val right: Int,
    val op: MathOp,
) {
    val answer: Int
        get() = when (op) {
            MathOp.ADD -> left + right
            MathOp.SUB -> left - right
            MathOp.MUL -> left * right
            MathOp.DIV -> left / right
        }

    fun opSymbol(): String = when (op) {
        MathOp.ADD -> "+"
        MathOp.SUB -> "-"
        MathOp.MUL -> "×"
        MathOp.DIV -> "÷"
    }
}

private data class MathLevelInfo(
    val id: Int,
    val title: String,
    val subtitle: String,
)

private val MathLevels = listOf(
    MathLevelInfo(1, "等级 1", "10 以内 · 加法"),
    MathLevelInfo(2, "等级 2", "10 以内 · 加法（含 10）"),
    MathLevelInfo(3, "等级 3", "100 以内 · 加法 · 入门"),
    MathLevelInfo(4, "等级 4", "100 以内 · 加法"),
    MathLevelInfo(5, "等级 5", "加减混合 · 小数字"),
    MathLevelInfo(6, "等级 6", "加减混合 · 100 以内"),
    MathLevelInfo(7, "等级 7", "表内 · 乘除"),
    MathLevelInfo(8, "等级 8", "乘除 · 进阶"),
    MathLevelInfo(9, "等级 9", "四则混合"),
)

private val DigitCardGradients = listOf(
    listOf(Color(0xFFFF8A65), Color(0xFFFFD54F)),
    listOf(Color(0xFF4FC3F7), Color(0xFF81C784)),
    listOf(Color(0xFFBA68C8), Color(0xFFF06292)),
    listOf(Color(0xFF64B5F6), Color(0xFF4DD0E1)),
    listOf(Color(0xFFFFB74D), Color(0xFFFF8A65)),
    listOf(Color(0xFF81C784), Color(0xFFAED581)),
    listOf(Color(0xFF7986CB), Color(0xFF9575CD)),
    listOf(Color(0xFFFF8A80), Color(0xFFFFB74D)),
    listOf(Color(0xFF4DB6AC), Color(0xFF64B5F6)),
    listOf(Color(0xFFF48FB1), Color(0xFFCE93D8)),
)

private val DigitCardTilt = listOf(-6f, -3f, -7f, -2f, -5f, 4f, 2f, 6f, 3f, 7f)

/** 等级 ≥2 时答案可能出现 10，需要十位 + 个位两个框 */
private fun needsTwoAnswerSlots(level: Int): Boolean = level >= 2

private fun generateProblem(level: Int): MathProblem {
    val r = Random.Default
    fun ri(a: Int, b: Int) = r.nextInt(a, b + 1)

    return when (level) {
        1 -> {
            var p: MathProblem
            do {
                val a = ri(0, 9)
                val b = ri(0, 9)
                p = MathProblem(a, b, MathOp.ADD)
            } while (p.answer !in 1..9)
            p
        }
        2 -> {
            val sum = ri(2, 10)
            val a = ri(1, sum - 1)
            MathProblem(a, sum - a, MathOp.ADD)
        }
        3 -> {
            val a = ri(10, 45)
            val bHi = minOf(9, 99 - a)
            if (bHi < 1) return MathProblem(10, 1, MathOp.ADD)
            val b = ri(1, bHi)
            MathProblem(a, b, MathOp.ADD)
        }
        4 -> {
            val a = ri(10, 89)
            val b = ri(1, 99 - a)
            MathProblem(a, b, MathOp.ADD)
        }
        5 -> {
            if (r.nextBoolean()) {
                val sum = ri(2, 10)
                val a = ri(1, sum - 1)
                MathProblem(a, sum - a, MathOp.ADD)
            } else {
                val left = ri(3, 12)
                val right = ri(1, left - 1)
                MathProblem(left, right, MathOp.SUB)
            }
        }
        6 -> {
            if (r.nextBoolean()) {
                val a = ri(11, 80)
                val b = ri(1, 99 - a)
                MathProblem(a, b, MathOp.ADD)
            } else {
                val left = ri(20, 99)
                val right = ri(1, left - 1)
                MathProblem(left, right, MathOp.SUB)
            }
        }
        7 -> {
            if (r.nextBoolean()) {
                val a = ri(2, 9)
                val b = ri(2, 9)
                MathProblem(a, b, MathOp.MUL)
            } else {
                val b = ri(2, 9)
                val q = ri(2, 9)
                MathProblem(b * q, b, MathOp.DIV)
            }
        }
        8 -> {
            if (r.nextBoolean()) {
                val a = ri(10, 24)
                val b = ri(2, minOf(9, 99 / a))
                MathProblem(a, b, MathOp.MUL)
            } else {
                val b = ri(3, 12)
                val q = ri(2, minOf(9, 99 / b))
                MathProblem(b * q, b, MathOp.DIV)
            }
        }
        else -> {
            when (r.nextInt(0, 4)) {
                0 -> {
                    val a = ri(10, 40)
                    val b = ri(1, minOf(9, 99 - a))
                    MathProblem(a, b, MathOp.ADD)
                }
                1 -> {
                    val left = ri(15, 60)
                    val right = ri(1, minOf(left - 1, 20))
                    MathProblem(left, right, MathOp.SUB)
                }
                2 -> {
                    val a = ri(2, 9)
                    val b = ri(2, 9)
                    MathProblem(a, b, MathOp.MUL)
                }
                else -> {
                    val b = ri(2, 9)
                    val q = ri(2, 9)
                    MathProblem(b * q, b, MathOp.DIV)
                }
            }
        }
    }
}

/** 0–9 英文，拖拽与个位数朗读用 */
private fun digitToEnglish(digit: Int): String = when (digit) {
    0 -> "zero"
    1 -> "one"
    2 -> "two"
    3 -> "three"
    4 -> "four"
    5 -> "five"
    6 -> "six"
    7 -> "seven"
    8 -> "eight"
    9 -> "nine"
    else -> digit.toString()
}

@Composable
private fun AnswerSlotBox(
    label: String,
    value: Int?,
    modifier: Modifier = Modifier,
    onPositioned: (Rect) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF78909C),
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .onGloballyPositioned { coords ->
                    val p = coords.positionInRoot()
                    val s = coords.size
                    onPositioned(Rect(p.x, p.y, p.x + s.width, p.y + s.height))
                }
                .background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value?.toString() ?: "?",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = if (value == null) Color(0xFFB0BEC5) else Color(0xFF2E7D32),
            )
        }
    }
}

@Composable
fun SimpleMathScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val scope = rememberCoroutineScope()
    var dragSpeakJob by remember { mutableStateOf<Job?>(null) }
    var level by remember { mutableIntStateOf(1) }
    var problem by remember(level) { mutableStateOf(generateProblem(level)) }

    val twoSlots = needsTwoAnswerSlots(level)
    var slotTens by remember(problem) { mutableStateOf<Int?>(null) }
    var slotOnes by remember(problem) { mutableStateOf<Int?>(null) }
    var dragDigit by remember(problem) { mutableStateOf<Int?>(null) }
    var dragOffset by remember(problem) { mutableStateOf(Offset.Zero) }
    var dragRectInRoot by remember(problem) { mutableStateOf<Rect?>(null) }
    var slotTensRect by remember(problem) { mutableStateOf(Rect.Zero) }
    var slotOnesRect by remember(problem) { mutableStateOf(Rect.Zero) }

    var feedback by remember(problem) { mutableStateOf<String?>(null) }
    var showReward by remember(problem) { mutableStateOf(false) }
    var showLevelSettings by remember { mutableStateOf(false) }

    val titleBrush = Brush.horizontalGradient(
        listOf(CherryRed, OrangePop, SkyBlue, GrassGreen),
    )

    fun readAnswerForTts(value: Int) {
        audio.speak(value.toString(), append = false)
        if (value in 0..9) {
            audio.speak(digitToEnglish(value), append = true)
        }
    }

    fun submittedAnswer(): Int? {
        return if (twoSlots) {
            val ones = slotOnes ?: return null
            val tens = slotTens ?: 0
            tens * 10 + ones
        } else {
            slotOnes
        }
    }

    fun submit() {
        val guess = submittedAnswer()
        if (guess == null) {
            feedback = if (twoSlots) "把数字拖到个位框里哦（十位没有就空着或拖 0）" else "把数字拖到问号框里哦"
            audio.speak("put a number in the box", append = true)
            return
        }
        if (guess == problem.answer) {
            feedback = "太棒了！"
            showReward = true
            audio.playSuccess()
            readAnswerForTts(guess)
            audio.speakVictoryPraise(
                append = true
            )
        } else {
            feedback = "再试一次"
            audio.playTryAgain()
            audio.speak("Try again", append = true)
        }
    }

    fun tryDropDigit(digit: Int) {
        val rect = dragRectInRoot ?: return
        val center = rect.center
        var droppedOnOnes = false
        if (twoSlots) {
            when {
                slotTensRect.contains(center) -> slotTens = digit
                slotOnesRect.contains(center) -> {
                    slotOnes = digit
                    droppedOnOnes = true
                }
            }
        } else if (slotOnesRect.contains(center)) {
            slotOnes = digit
            droppedOnOnes = true
        }
        dragDigit = null
        dragOffset = Offset.Zero
        if (droppedOnOnes) submit()
    }

    fun nextQuestion() {
        problem = generateProblem(level)
        slotTens = null
        slotOnes = null
        feedback = null
        showReward = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFEBEE), Color(0xFFE3F2FD)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        audio.speak("back")
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color(0xFFC62828),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        showLevelSettings = true
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color(0xFF37474F),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "设置等级",
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "解答数学题!",
                    style = MaterialTheme.typography.displayLarge.merge(
                        TextStyle(brush = titleBrush, fontWeight = FontWeight.ExtraBold),
                    ),
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 52.dp),
                )
            }

            Text(
                text = "等级 $level · ${MathLevels[level - 1].subtitle} · 点齿轮可改",
                fontSize = 13.sp,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${problem.left} ${problem.opSymbol()} ${problem.right} =",
                    fontSize = if (problem.left >= 100 || problem.right >= 100) 28.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5),
                )
                Spacer(modifier = Modifier.size(10.dp))
                if (twoSlots) {
                    AnswerSlotBox(
                        label = "十位",
                        value = slotTens,
                        onPositioned = { slotTensRect = it },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    AnswerSlotBox(
                        label = "个位",
                        value = slotOnes,
                        onPositioned = { slotOnesRect = it },
                    )
                } else {
                    AnswerSlotBox(
                        label = "",
                        value = slotOnes,
                        modifier = Modifier,
                        onPositioned = { slotOnesRect = it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (twoSlots) {
                    "先拖十位（需要时），再拖个位；松手在个位后会自动判对错"
                } else {
                    "拖动数字到答案框，松手后自动判对错"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(0..4, 5..9).forEach { range ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (digit in range) {
                            val dragging = dragDigit == digit
                            val dragMod = if (dragging) {
                                Modifier
                                    .offset {
                                        IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                    }
                                    .onGloballyPositioned { coords ->
                                        val p = coords.positionInRoot()
                                        val s = coords.size
                                        dragRectInRoot = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                                    }
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = dragMod
                                    .size(64.dp)
                                    .pointerInput(digit, problem, slotTens, slotOnes) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragSpeakJob?.cancel()
                                                dragDigit = digit
                                                dragOffset = Offset.Zero
                                                audio.playSoftClick()
                                                val word = digitToEnglish(digit)
                                                dragSpeakJob = scope.launch {
                                                    delay(200)
                                                    audio.speak(word, append = false)
                                                }
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                            },
                                            onDragEnd = { tryDropDigit(digit) },
                                            onDragCancel = {
                                                dragDigit = null
                                                dragOffset = Offset.Zero
                                            },
                                        )
                                    }
                                    .background(
                                        Brush.linearGradient(
                                            DigitCardGradients[digit],
                                        ),
                                        RoundedCornerShape(16.dp),
                                    )
                                    .graphicsLayer {
                                        rotationZ = if (dragging) 0f else DigitCardTilt[digit]
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$digit",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color(0xFF000000).copy(alpha = 0.25f),
                                            offset = Offset(0f, 3f),
                                            blurRadius = 4f,
                                        ),
                                    ),
                                    modifier = Modifier
                                        .offset(y = (-1).dp)
                                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            feedback?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (msg) {
                        "太棒了！" -> GrassGreen
                        "再试一次" -> CherryRed
                        else -> Color(0xFF546E7A)
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        audio.playSoftClick()
                        audio.speak("next question")
                        nextQuestion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                ) {
                    Text("下一题", fontSize = 18.sp)
                }
                Button(
                    onClick = {
                        audio.playSoftClick()
                        audio.speak("clear")
                        slotTens = null
                        slotOnes = null
                        feedback = null
                        showReward = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE)),
                ) {
                    Text("清空", fontSize = 18.sp)
                }
            }
        }

        VictoryCelebrationOverlay(
            visible = showReward,
            onDismiss = {
                audio.playSoftClick()
                audio.speak("next question")
                nextQuestion()
            },
            subtitle = "答对了！继续加油！",
            continueLabel = "继续",
        )

        if (showLevelSettings) {
            AlertDialog(
                onDismissRequest = { showLevelSettings = false },
                title = { Text("选择等级", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MathLevels.forEach { info ->
                            val sel = info.id == level
                            TextButton(
                                onClick = {
                                    audio.playSoftClick()
                                    level = info.id
                                    problem = generateProblem(info.id)
                                    slotTens = null
                                    slotOnes = null
                                    feedback = null
                                    showReward = false
                                    showLevelSettings = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Text(
                                        text = "${info.title}",
                                        fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (sel) OrangePop else Color(0xFF37474F),
                                    )
                                    Text(
                                        text = info.subtitle,
                                        fontSize = 12.sp,
                                        color = Color(0xFF78909C),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLevelSettings = false }) {
                        Text("关闭")
                    }
                },
            )
        }
    }
}
