package com.yehao.leyuan.feature.animaljigsaw

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.random.Random

enum class SilhouetteShape {
    Circle,
    RoundRect,
    TriUp,
    TriDown,
    TriLeft,
    TriRight,
}

data class PieceDef(
    val id: Int,
    val shape: SilhouetteShape,
    /** 中心 x，相对板宽 0～1 */
    val cx: Float,
    /** 中心 y，相对板高 0～1 */
    val cy: Float,
    /** 宽，相对板宽 0～1 */
    val w: Float,
    /** 高，相对板高 0～1 */
    val h: Float,
    val color: Color,
)

data class AnimalPuzzleTemplate(
    val nameZh: String,
    val nameEn: String,
    val emoji: String,
    val pieces: List<PieceDef>,
)

/** 限时（秒）：仅随等级变化，1 级最长 30 秒，等级越高越短（最低 12 秒） */
fun timeLimitSecondsForLevel(level: Int): Int {
    val lv = level.coerceIn(1, 9)
    return (30f - (lv - 1) * 2.25f).roundToInt().coerceIn(12, 30)
}

/** 全等级统一：块数从模板随机，不再按等级增减 */
private val ALL_PIECE_COUNTS = 3..8

fun pieceCountRangeForLevel(@Suppress("UNUSED_PARAMETER") level: Int): IntRange = ALL_PIECE_COUNTS

/** 全等级统一吸附，儿童友好 */
fun slotInflationForLevel(@Suppress("UNUSED_PARAMETER") level: Int): Float = 1.3f

private val AllTemplates: List<AnimalPuzzleTemplate> = listOf(
    AnimalPuzzleTemplate(
        nameZh = "小鸡",
        nameEn = "chick",
        emoji = "🐤",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.Circle, 0.5f, 0.38f, 0.26f, 0.24f, Color(0xFFFFEA00)),
            PieceDef(1, SilhouetteShape.RoundRect, 0.5f, 0.62f, 0.34f, 0.30f, Color(0xFFFFAB00)),
            PieceDef(2, SilhouetteShape.TriRight, 0.72f, 0.4f, 0.14f, 0.1f, Color(0xFFFF6D00)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小兔",
        nameEn = "bunny",
        emoji = "🐰",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.Circle, 0.48f, 0.42f, 0.22f, 0.22f, Color(0xFFFFC1E3)),
            PieceDef(1, SilhouetteShape.TriUp, 0.38f, 0.22f, 0.1f, 0.16f, Color(0xFFFF80AB)),
            PieceDef(2, SilhouetteShape.TriUp, 0.58f, 0.22f, 0.1f, 0.16f, Color(0xFFFF80AB)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小鱼",
        nameEn = "fish",
        emoji = "🐟",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.55f, 0.5f, 0.48f, 0.26f, Color(0xFF00E5FF)),
            PieceDef(1, SilhouetteShape.TriLeft, 0.16f, 0.5f, 0.2f, 0.22f, Color(0xFF2979FF)),
            PieceDef(2, SilhouetteShape.Circle, 0.68f, 0.46f, 0.09f, 0.09f, Color(0xFFFFEA00)),
            PieceDef(3, SilhouetteShape.TriUp, 0.52f, 0.32f, 0.12f, 0.11f, Color(0xFFD500F9)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小鸟",
        nameEn = "bird",
        emoji = "🐦",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.5f, 0.52f, 0.36f, 0.28f, Color(0xFF40C4FF)),
            PieceDef(1, SilhouetteShape.TriRight, 0.74f, 0.5f, 0.14f, 0.12f, Color(0xFFFF6D00)),
            PieceDef(2, SilhouetteShape.Circle, 0.38f, 0.46f, 0.1f, 0.1f, Color(0xFF212121)),
            PieceDef(3, SilhouetteShape.TriDown, 0.48f, 0.68f, 0.14f, 0.12f, Color(0xFF651FFF)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小青蛙",
        nameEn = "frog",
        emoji = "🐸",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.5f, 0.58f, 0.44f, 0.32f, Color(0xFF00E676)),
            PieceDef(1, SilhouetteShape.Circle, 0.5f, 0.34f, 0.26f, 0.2f, Color(0xFF76FF03)),
            PieceDef(2, SilhouetteShape.Circle, 0.38f, 0.36f, 0.1f, 0.1f, Color(0xFFFFFFFF)),
            PieceDef(3, SilhouetteShape.Circle, 0.62f, 0.36f, 0.1f, 0.1f, Color(0xFFFFFFFF)),
            PieceDef(4, SilhouetteShape.Circle, 0.5f, 0.4f, 0.07f, 0.07f, Color(0xFF212121)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小猫咪",
        nameEn = "cat",
        emoji = "🐱",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.Circle, 0.5f, 0.4f, 0.3f, 0.3f, Color(0xFFFF9100)),
            PieceDef(1, SilhouetteShape.RoundRect, 0.52f, 0.68f, 0.36f, 0.28f, Color(0xFFFF6D00)),
            PieceDef(2, SilhouetteShape.TriUp, 0.36f, 0.22f, 0.12f, 0.14f, Color(0xFFFF3D00)),
            PieceDef(3, SilhouetteShape.TriUp, 0.64f, 0.22f, 0.12f, 0.14f, Color(0xFFFF3D00)),
            PieceDef(4, SilhouetteShape.RoundRect, 0.82f, 0.62f, 0.16f, 0.1f, Color(0xFFFF4081)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小狗狗",
        nameEn = "puppy",
        emoji = "🐶",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.48f, 0.42f, 0.32f, 0.3f, Color(0xFFFFCC80)),
            PieceDef(1, SilhouetteShape.RoundRect, 0.52f, 0.68f, 0.34f, 0.26f, Color(0xFFFF8A65)),
            PieceDef(2, SilhouetteShape.RoundRect, 0.32f, 0.36f, 0.14f, 0.12f, Color(0xFF8D6E63)),
            PieceDef(3, SilhouetteShape.RoundRect, 0.64f, 0.36f, 0.14f, 0.12f, Color(0xFF8D6E63)),
            PieceDef(4, SilhouetteShape.Circle, 0.5f, 0.48f, 0.08f, 0.08f, Color(0xFF212121)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小乌龟",
        nameEn = "turtle",
        emoji = "🐢",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.Circle, 0.48f, 0.48f, 0.4f, 0.36f, Color(0xFF76FF03)),
            PieceDef(1, SilhouetteShape.RoundRect, 0.22f, 0.5f, 0.14f, 0.12f, Color(0xFF00C853)),
            PieceDef(2, SilhouetteShape.RoundRect, 0.74f, 0.46f, 0.14f, 0.1f, Color(0xFF1DE9B6)),
            PieceDef(3, SilhouetteShape.RoundRect, 0.4f, 0.72f, 0.1f, 0.08f, Color(0xFF69F0AE)),
            PieceDef(4, SilhouetteShape.RoundRect, 0.56f, 0.72f, 0.1f, 0.08f, Color(0xFF69F0AE)),
            PieceDef(5, SilhouetteShape.RoundRect, 0.72f, 0.72f, 0.1f, 0.08f, Color(0xFF69F0AE)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小蜜蜂",
        nameEn = "bee",
        emoji = "🐝",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.5f, 0.5f, 0.22f, 0.32f, Color(0xFFFFEA00)),
            PieceDef(1, SilhouetteShape.RoundRect, 0.5f, 0.5f, 0.22f, 0.1f, Color(0xFF212121)),
            PieceDef(2, SilhouetteShape.RoundRect, 0.5f, 0.58f, 0.22f, 0.1f, Color(0xFF212121)),
            PieceDef(3, SilhouetteShape.Circle, 0.34f, 0.42f, 0.12f, 0.12f, Color(0xFFFFFFFF)),
            PieceDef(4, SilhouetteShape.Circle, 0.66f, 0.42f, 0.12f, 0.12f, Color(0xFFFFFFFF)),
            PieceDef(5, SilhouetteShape.TriUp, 0.5f, 0.22f, 0.08f, 0.1f, Color(0xFFFF6D00)),
            PieceDef(6, SilhouetteShape.TriDown, 0.5f, 0.78f, 0.1f, 0.12f, Color(0xFFFF4081)),
        ),
    ),
    AnimalPuzzleTemplate(
        nameZh = "小蝴蝶",
        nameEn = "butterfly",
        emoji = "🦋",
        pieces = listOf(
            PieceDef(0, SilhouetteShape.RoundRect, 0.5f, 0.5f, 0.1f, 0.28f, Color(0xFFFF4081)),
            PieceDef(1, SilhouetteShape.Circle, 0.28f, 0.38f, 0.22f, 0.2f, Color(0xFFE040FB)),
            PieceDef(2, SilhouetteShape.Circle, 0.72f, 0.38f, 0.22f, 0.2f, Color(0xFFE040FB)),
            PieceDef(3, SilhouetteShape.Circle, 0.26f, 0.62f, 0.2f, 0.18f, Color(0xFF7C4DFF)),
            PieceDef(4, SilhouetteShape.Circle, 0.74f, 0.62f, 0.2f, 0.18f, Color(0xFF7C4DFF)),
            PieceDef(5, SilhouetteShape.TriUp, 0.42f, 0.22f, 0.08f, 0.1f, Color(0xFF00E5FF)),
            PieceDef(6, SilhouetteShape.TriUp, 0.58f, 0.22f, 0.08f, 0.1f, Color(0xFF00E5FF)),
            PieceDef(7, SilhouetteShape.Circle, 0.5f, 0.5f, 0.08f, 0.08f, Color(0xFFFFEA00)),
        ),
    ),
)

fun pickRandomTemplate(@Suppress("UNUSED_PARAMETER") level: Int, random: Random = Random): AnimalPuzzleTemplate {
    val candidates = AllTemplates.filter { it.pieces.size in ALL_PIECE_COUNTS }
    val pool = if (candidates.isNotEmpty()) candidates else AllTemplates
    return pool[random.nextInt(pool.size)]
}
