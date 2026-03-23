package com.yehao.leyuan.feature.shape

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.celebration.VictoryCelebrationOverlay
import com.yehao.leyuan.ui.theme.CherryRed
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.OrangePop
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private enum class ShapeKind(val labelEn: String, val labelZh: String) {
    Circle("Circle", "圆形"),
    Square("Square", "方形"),
    Triangle("Triangle", "三角"),
    Star("Star", "星星")
}

@Composable
fun ShapeMatchingScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    var layoutRevision by remember { mutableIntStateOf(0) }
    val slotKinds = remember(layoutRevision) { ShapeKind.entries.shuffled() }
    val poolKinds = remember(layoutRevision) { ShapeKind.entries.shuffled() }
    val slotCount = ShapeKind.entries.size

    val matched = remember(layoutRevision) { mutableStateListOf(*Array(slotCount) { false }) }
    var dragKind by remember { mutableStateOf<ShapeKind?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragRectInRoot by remember { mutableStateOf<Rect?>(null) }

    val slotRects = remember(layoutRevision) {
        mutableStateListOf<Rect>().apply { repeat(slotCount) { add(Rect.Zero) } }
    }

    fun isShapePlaced(kind: ShapeKind): Boolean =
        slotKinds.indices.any { j -> matched[j] && slotKinds[j] == kind }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showVictoryCelebration by remember { mutableStateOf(false) }

    fun trySnap(kind: ShapeKind) {
        val rect = dragRectInRoot ?: return
        val center = rect.center
        for (i in slotRects.indices) {
            if (matched[i]) continue
            if (!slotRects[i].contains(center)) continue
            if (slotKinds[i] != kind) continue
            matched[i] = true
            audio.playSuccess()
            dragKind = null
            dragOffset = Offset.Zero
            scope.launch {
                snackbarHostState.showSnackbar("配对成功！${kind.labelZh} / ${kind.labelEn}")
            }
            if (matched.all { it }) {
                showVictoryCelebration = true
                audio.speakVictoryPraise(
                    append = false,
                    leadIn = "You matched every shape! So smart! Well done!"
                )
            }
            return
        }
        dragKind = null
        dragOffset = Offset.Zero
    }

    val titleBrush = Brush.horizontalGradient(
        listOf(SkyBlue, GrassGreen, OrangePop, CherryRed)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8EAF6), Color(0xFFFFF3E0))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                IconButton(
                    onClick = {
                        audio.playSoftClick()
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.88f),
                        contentColor = Color(0xFF3949AB),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "配对形状!",
                    style = MaterialTheme.typography.displayLarge.merge(
                        TextStyle(brush = titleBrush, fontWeight = FontWeight.ExtraBold),
                    ),
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 52.dp),
                )
            }
            Text(
                text = "拖到上面相同轮廓的框里",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF455A64),
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in slotKinds.indices) {
                    val kind = slotKinds[i]
                    key("shape_slot_${layoutRevision}_${i}_$kind") {
                        val done = matched[i]
                        val scale by animateFloatAsState(
                            targetValue = if (done) 1.06f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "shapeSlot"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((88 * scale).dp)
                                    .onGloballyPositioned { coords ->
                                        val p = coords.positionInRoot()
                                        val s = coords.size
                                        slotRects[i] = Rect(p.x, p.y, p.x + s.width, p.y + s.height)
                                    }
                                    .background(
                                        if (done) Color(0xFFC8E6C9) else Color.White.copy(alpha = 0.92f),
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ShapeOutline(kind = kind, modifier = Modifier.size(56.dp), alpha = if (done) 1f else 0.45f)
                            }
                            Text(
                                text = kind.labelZh,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF37474F),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (kind in poolKinds) {
                        key("shape_drag_${layoutRevision}_$kind") {
                            if (isShapePlaced(kind)) {
                                Spacer(modifier = Modifier.size(88.dp))
                            } else {
                                val isDragging = dragKind == kind
                                val dragMod = if (isDragging) {
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
                                        .size(88.dp)
                                        .pointerInput(kind, layoutRevision, isShapePlaced(kind)) {
                                            if (isShapePlaced(kind)) return@pointerInput
                                            detectDragGestures(
                                                onDragStart = {
                                                    dragKind = kind
                                                    dragOffset = Offset.Zero
                                                    audio.playSoftClick()
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount
                                                },
                                                onDragEnd = { trySnap(kind) },
                                                onDragCancel = {
                                                    dragKind = null
                                                    dragOffset = Offset.Zero
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ShapeFilled(kind = kind, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )

        VictoryCelebrationOverlay(
            visible = showVictoryCelebration,
            onDismiss = {
                showVictoryCelebration = false
                layoutRevision++
            },
            subtitle = "所有形状都配对成功啦！",
            continueLabel = "再玩一次"
        )
    }
}

@Composable
private fun ShapeOutline(kind: ShapeKind, modifier: Modifier, alpha: Float) {
    val stroke = Stroke(width = 5f)
    val color = Color(0xFF37474F).copy(alpha = alpha)
    Canvas(modifier = modifier) {
        when (kind) {
            ShapeKind.Circle -> drawCircle(color = color, style = stroke, radius = size.minDimension / 2f - 4f)
            ShapeKind.Square -> drawRect(color = color, style = stroke, topLeft = Offset(4f, 4f), size = Size(size.width - 8f, size.height - 8f))
            ShapeKind.Triangle -> {
                val p = Path().apply {
                    moveTo(size.width / 2f, 6f)
                    lineTo(size.width - 6f, size.height - 6f)
                    lineTo(6f, size.height - 6f)
                    close()
                }
                drawPath(p, color = color, style = stroke)
            }
            ShapeKind.Star -> drawPath(starPath(size), color = color, style = stroke)
        }
    }
}

@Composable
private fun ShapeFilled(kind: ShapeKind, modifier: Modifier) {
    val fill = when (kind) {
        ShapeKind.Circle -> SkyBlue
        ShapeKind.Square -> CherryRed
        ShapeKind.Triangle -> GrassGreen
        ShapeKind.Star -> OrangePop
    }
    when (kind) {
        ShapeKind.Circle -> Box(
            modifier = modifier
                .clip(CircleShape)
                .background(fill)
        )
        ShapeKind.Square -> Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(fill)
        )
        ShapeKind.Triangle -> Canvas(modifier = modifier) {
            val p = Path().apply {
                moveTo(size.width / 2f, 4f)
                lineTo(size.width - 4f, size.height - 4f)
                lineTo(4f, size.height - 4f)
                close()
            }
            drawPath(p, color = fill)
        }
        ShapeKind.Star -> Canvas(modifier = modifier) {
            drawPath(starPath(size), color = fill)
        }
    }
}

private fun starPath(size: Size): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = size.minDimension / 2f - 4f
    val inner = outer * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val ang = Math.toRadians((i * 36 - 90).toDouble()).toFloat()
        val x = cx + r * cos(ang)
        val y = cy + r * sin(ang)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
