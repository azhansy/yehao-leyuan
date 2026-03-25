package com.yehao.leyuan.feature.animaljigsaw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.celebration.VictoryCelebrationOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.CreamBackground
import com.yehao.leyuan.ui.theme.DeepText
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.GrapePurple
import com.yehao.leyuan.ui.theme.OrangePop
import com.yehao.leyuan.ui.theme.PinkBubble
import com.yehao.leyuan.ui.theme.SkyBlue
import com.yehao.leyuan.ui.theme.SunnyYellow
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.launch

/** 与字母拼图、家庭成员页一致的页面底；槽位描边用主题天蓝，醒目又不花 */
private val AnimalJigsawBackgroundGradient = listOf(
    CreamBackground,
    Color(0xFFB2EBF2),
    Color(0xFFE1F5FE),
)

private val SlotOutlineColor = SkyBlue
private val TrayPieceDp = 56.dp

@Composable
private fun SilhouetteShapeCanvas(
    shape: SilhouetteShape,
    color: Color,
    modifier: Modifier = Modifier,
    outlineOnly: Boolean,
) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp))) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 3.5f.dp.toPx())
        val style = if (outlineOnly) stroke else Fill
        val drawColor = if (outlineOnly) SlotOutlineColor else color
        when (shape) {
            SilhouetteShape.Circle -> {
                val r = min(w, h) / 2f * 0.92f
                drawCircle(
                    color = drawColor,
                    radius = r,
                    center = Offset(w / 2f, h / 2f),
                    style = style,
                )
            }
            SilhouetteShape.RoundRect -> {
                val cr = min(w, h) * 0.22f
                if (outlineOnly) {
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset(2f, 2f),
                        size = Size(w - 4f, h - 4f),
                        cornerRadius = CornerRadius(cr, cr),
                        style = stroke,
                    )
                } else {
                    drawRoundRect(
                        color = drawColor,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        cornerRadius = CornerRadius(cr, cr),
                        style = Fill,
                    )
                }
            }
            SilhouetteShape.TriUp -> {
                val p = Path().apply {
                    moveTo(w / 2f, 4f)
                    lineTo(w - 4f, h - 4f)
                    lineTo(4f, h - 4f)
                    close()
                }
                drawPath(p, drawColor, style = style)
            }
            SilhouetteShape.TriDown -> {
                val p = Path().apply {
                    moveTo(4f, 4f)
                    lineTo(w - 4f, 4f)
                    lineTo(w / 2f, h - 4f)
                    close()
                }
                drawPath(p, drawColor, style = style)
            }
            SilhouetteShape.TriLeft -> {
                val p = Path().apply {
                    moveTo(w - 4f, 4f)
                    lineTo(w - 4f, h - 4f)
                    lineTo(4f, h / 2f)
                    close()
                }
                drawPath(p, drawColor, style = style)
            }
            SilhouetteShape.TriRight -> {
                val p = Path().apply {
                    moveTo(4f, 4f)
                    lineTo(4f, h - 4f)
                    lineTo(w - 4f, h / 2f)
                    close()
                }
                drawPath(p, drawColor, style = style)
            }
        }
    }
}

@Composable
fun AnimalJigsawScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { AnimalJigsawPrefs(context) }
    var gameLevel by remember { mutableIntStateOf(prefs.getLevel()) }
    var session by remember { mutableIntStateOf(0) }
    var showLevelDialog by remember { mutableStateOf(false) }

    val template = remember(gameLevel, session) { pickRandomTemplate(gameLevel, Random) }
    val pieceCount = template.pieces.size
    val placed = remember(session) { mutableStateListOf<Boolean>().apply { repeat(pieceCount) { add(false) } } }
    val slotRects = remember(session) {
        mutableStateListOf<Rect>().apply { repeat(pieceCount) { add(Rect.Zero) } }
    }

    var dragPieceId by remember(session) { mutableStateOf<Int?>(null) }
    var dragOffset by remember(session) { mutableStateOf(Offset.Zero) }
    var dragRectInRoot by remember(session) { mutableStateOf<Rect?>(null) }
    var showVictory by remember(session) { mutableStateOf(false) }
    var showTimeUp by remember(session) { mutableStateOf(false) }

    val trayOrder = remember(session) { template.pieces.map { it.id }.shuffled() }
    val inflation = slotInflationForLevel(gameLevel)
    val timeLimitSec = remember(gameLevel) { timeLimitSecondsForLevel(gameLevel) }
    val sessionClockStart = remember(session) { android.os.SystemClock.elapsedRealtime() }
    var uiTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(session, gameLevel) {
        while (true) {
            delay(320)
            uiTick++
        }
    }

    LaunchedEffect(session, gameLevel) {
        showTimeUp = false
        val won = withTimeoutOrNull(timeLimitSec * 1000L) {
            snapshotFlow { placed.all { it } }.first { it }
        }
        if (won != true && !placed.all { it }) {
            showTimeUp = true
            audio.playTryAgain()
        }
    }

    fun slotIndexForPieceId(id: Int): Int = template.pieces.indexOfFirst { it.id == id }

    fun trySnap(pieceId: Int) {
        if (showTimeUp) return
        val rect = dragRectInRoot ?: return
        val center = rect.center
        val idx = slotIndexForPieceId(pieceId)
        if (idx < 0 || placed[idx]) return
        val r = slotRects[idx]
        if (r.isEmpty) return
        val cx = r.center.x
        val cy = r.center.y
        val hw = r.width / 2f * inflation
        val hh = r.height / 2f * inflation
        val hit = Rect(cx - hw, cy - hh, cx + hw, cy + hh)
        if (!hit.contains(center)) {
            audio.playTryAgain()
            return
        }
        placed[idx] = true
        audio.playSuccess()
        dragPieceId = null
        dragOffset = Offset.Zero
        if (placed.all { it }) {
            showVictory = true
            scope.launch {
                audio.speakVictoryPraise(
                    append = false
                )
            }
        }
    }

    fun newPuzzle() {
        session += 1
        showVictory = false
        showTimeUp = false
    }

    val remainingSec = remember(timeLimitSec, sessionClockStart, uiTick) {
        val elapsed = ((android.os.SystemClock.elapsedRealtime() - sessionClockStart) / 1000L).toInt().coerceAtLeast(0)
        (timeLimitSec - elapsed).coerceAtLeast(0)
    }

    val titleBrush = Brush.horizontalGradient(
        listOf(SkyBlue, GrassGreen, SunnyYellow, OrangePop, PinkBubble, GrapePurple, CherryRed),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AnimalJigsawBackgroundGradient)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color(0xFFC62828),
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        showLevelDialog = true
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color(0xFF0277BD),
                    ),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "难度")
                }
                Text(
                    text = "动物拼图",
                    style = MaterialTheme.typography.headlineSmall.merge(
                        TextStyle(brush = titleBrush, fontWeight = FontWeight.Black),
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "${template.emoji} ${template.nameZh} · 第 ${gameLevel} 级 · ${pieceCount} 块 · 限时 ${timeLimitSec} 秒",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
            )
            if (!showVictory && !showTimeUp) {
                Text(
                    text = "还剩 $remainingSec 秒",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (remainingSec <= 5) CherryRed else OrangePop,
                    modifier = Modifier.padding(bottom = 6.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "把彩色碎片拖到上面虚线框里，形状要对上哦",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DeepText.copy(alpha = 0.88f),
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val bw = maxWidth
                val bh = maxHeight
                Box(modifier = Modifier.fillMaxSize()) {
                    template.pieces.forEachIndexed { index, piece ->
                        val ox = bw * (piece.cx - piece.w / 2f)
                        val oy = bh * (piece.cy - piece.h / 2f)
                        val sw = bw * piece.w
                        val sh = bh * piece.h
                        Box(
                            modifier = Modifier
                                .offset(ox, oy)
                                .size(sw, sh)
                                .onGloballyPositioned { coords ->
                                    val p = coords.positionInRoot()
                                    val s = coords.size
                                    slotRects[index] = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                                },
                        ) {
                            if (placed[index]) {
                                SilhouetteShapeCanvas(
                                    shape = piece.shape,
                                    color = piece.color,
                                    modifier = Modifier.fillMaxSize(),
                                    outlineOnly = false,
                                )
                            } else {
                                SilhouetteShapeCanvas(
                                    shape = piece.shape,
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxSize(),
                                    outlineOnly = true,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "碎片区",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = GrassGreen,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )

            val trayScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(trayScroll)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                trayOrder.chunked(5).forEach { rowIds ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    ) {
                        for (pid in rowIds) {
                            val piece = template.pieces.find { it.id == pid } ?: continue
                            val idx = slotIndexForPieceId(pid)
                            if (idx >= 0 && placed[idx]) continue
                            val dragging = dragPieceId == pid
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
                                    .size(TrayPieceDp)
                                    .pointerInput(pid, session, showTimeUp) {
                                        if (showTimeUp) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = {
                                                dragPieceId = pid
                                                dragOffset = Offset.Zero
                                                audio.playSoftClick()
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                            },
                                            onDragEnd = {
                                                if (!showTimeUp) trySnap(pid)
                                                dragPieceId = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                dragPieceId = null
                                                dragOffset = Offset.Zero
                                            },
                                        )
                                    }
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.linearGradient(
                                            listOf(SkyBlue, PinkBubble, OrangePop, GrassGreen),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color.White, CreamBackground.copy(alpha = 0.65f)),
                                        ),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(4.dp),
                            ) {
                                SilhouetteShapeCanvas(
                                    shape = piece.shape,
                                    color = piece.color,
                                    modifier = Modifier.fillMaxSize(),
                                    outlineOnly = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        VictoryCelebrationOverlay(
            visible = showVictory,
            onDismiss = {
                audio.playSoftClick()
                newPuzzle()
            },
            subtitle = "拼图完成！",
            continueLabel = "下一关图案",
        )

        if (showTimeUp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(24.dp))
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                ) {
                    Text(
                        text = "⏰ 时间到啦！",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F),
                    )
                    Text(
                        text = "规定时间内没拼完，再试一次吧",
                        fontSize = 16.sp,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            audio.playSoftClick()
                            newPuzzle()
                        },
                        modifier = Modifier.padding(top = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                    ) {
                        Text("再来一局", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (showLevelDialog) {
            AlertDialog(
                onDismissRequest = { showLevelDialog = false },
                title = { Text("拼图难度 1～9 级", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = "每关拼图块数随机。等级只影响倒计时：1 级最长 30 秒，等级越高时间越短（最短 12 秒），超时未完成即失败。",
                            fontSize = 13.sp,
                            color = Color(0xFF607D8B),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        for (lv in 1..9) {
                            val sec = timeLimitSecondsForLevel(lv)
                            val sel = lv == gameLevel
                            TextButton(
                                onClick = {
                                    audio.playSoftClick()
                                    prefs.setLevel(lv)
                                    gameLevel = lv
                                    newPuzzle()
                                    showLevelDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "第 $lv 级（限时 ${sec} 秒）",
                                    fontWeight = if (sel) FontWeight.Black else FontWeight.Normal,
                                    color = if (sel) OrangePop else Color(0xFF37474F),
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
}
