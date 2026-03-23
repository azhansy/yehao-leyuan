package com.yehao.leyuan.feature.math

import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private data class MathProblem(val left: Int, val right: Int, val add: Boolean) {
    val answer: Int get() = if (add) left + right else left - right
}

private val Problems = listOf(
    MathProblem(2, 3, true),
    MathProblem(1, 4, true),
    MathProblem(5, 2, false),
    MathProblem(4, 1, true),
    MathProblem(3, 1, false)
)

@Composable
fun SimpleMathScreen() {
    val audio = LocalAppAudio.current
    var problemIndex by remember { mutableIntStateOf(0) }
    val problem = Problems[problemIndex % Problems.size]

    var slotDigit by remember(problem) { mutableStateOf<Int?>(null) }
    var dragDigit by remember(problem) { mutableStateOf<Int?>(null) }
    var dragOffset by remember(problem) { mutableStateOf(Offset.Zero) }
    var dragRectInRoot by remember { mutableStateOf<Rect?>(null) }
    var slotRect by remember(problem) { mutableStateOf(Rect.Zero) }

    var feedback by remember(problem) { mutableStateOf<String?>(null) }
    var showReward by remember(problem) { mutableStateOf(false) }

    val titleBrush = Brush.horizontalGradient(
        listOf(CherryRed, OrangePop, SkyBlue, GrassGreen)
    )

    fun submit() {
        val d = slotDigit
        if (d == null) {
            feedback = "把数字拖到问号框里哦"
            return
        }
        if (d == problem.answer) {
            feedback = "太棒了！"
            showReward = true
            audio.playSuccess()
            audio.speakVictoryPraise(
                append = false,
                leadIn = "That's the right answer! You counted so well! Great math!"
            )
        } else {
            feedback = "再试一次"
            audio.playTryAgain()
            audio.speak("Try again")
        }
    }

    fun tryDropDigit(digit: Int) {
        val rect = dragRectInRoot ?: return
        val center = rect.center
        if (slotRect.contains(center)) {
            slotDigit = digit
            audio.playSoftClick()
        }
        dragDigit = null
        dragOffset = Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFEBEE), Color(0xFFE3F2FD))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "解答数学题!",
                style = MaterialTheme.typography.displayLarge.merge(
                    TextStyle(brush = titleBrush, fontWeight = FontWeight.ExtraBold)
                ),
                fontSize = 40.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${problem.left} ${if (problem.add) "+" else "-"} ${problem.right} =",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .onGloballyPositioned { coords ->
                            val p = coords.positionInRoot()
                            val s = coords.size
                            slotRect = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                        }
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slotDigit?.toString() ?: "?",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (slotDigit == null) Color(0xFFB0BEC5) else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "拖动数字到问号框",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF546E7A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0..4, 5..9).forEach { range ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            } else Modifier

                            Box(
                                modifier = dragMod
                                    .size(64.dp)
                                    .pointerInput(digit, problemIndex, slotDigit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragDigit = digit
                                                dragOffset = Offset.Zero
                                                audio.playSoftClick()
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                            },
                                            onDragEnd = { tryDropDigit(digit) },
                                            onDragCancel = {
                                                dragDigit = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SkyBlue, GrassGreen)
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$digit",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePop)
            ) {
                Text("提交", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            feedback?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (msg) {
                        "太棒了！" -> GrassGreen
                        "再试一次" -> CherryRed
                        else -> Color(0xFF546E7A)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        problemIndex = (problemIndex + 1) % Problems.size
                        slotDigit = null
                        feedback = null
                        showReward = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
                ) {
                    Text("下一题", fontSize = 18.sp)
                }
                Button(
                    onClick = {
                        slotDigit = null
                        feedback = null
                        showReward = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90A4AE))
                ) {
                    Text("清空", fontSize = 18.sp)
                }
            }
        }

        VictoryCelebrationOverlay(
            visible = showReward,
            onDismiss = { showReward = false },
            subtitle = "答对了！继续加油！",
            continueLabel = "继续"
        )
    }
}
