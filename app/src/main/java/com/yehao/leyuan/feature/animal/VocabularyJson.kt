package com.yehao.leyuan.feature.animal

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import kotlin.random.Random

data class AnimalItem(
    val emoji: String,
    val english: String,
    /** 中文名，展示用；缺省时为空字符串 */
    val chinese: String,
    val tint: Color
)

private const val ASSET_ANIMALS = "animals.json"
private const val ASSET_PEOPLE_ROLES = "people_roles.json"

/** Load emoji + english + optional chinese + tint from a JSON array asset. */
fun loadEmojiEnglishAsset(context: Context, assetFileName: String): List<AnimalItem> {
    return try {
        val text = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val hex = o.getString("tint")
                val argb = AndroidColor.parseColor(hex)
                add(
                    AnimalItem(
                        emoji = o.getString("emoji"),
                        english = o.getString("english"),
                        chinese = o.optString("chinese", ""),
                        tint = Color(
                            red = AndroidColor.red(argb) / 255f,
                            green = AndroidColor.green(argb) / 255f,
                            blue = AndroidColor.blue(argb) / 255f,
                            alpha = AndroidColor.alpha(argb) / 255f
                        )
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun loadAnimalsFromAssets(context: Context): List<AnimalItem> =
    loadEmojiEnglishAsset(context, ASSET_ANIMALS)

/** 职业与人物称呼（家人、老师、医生、警察等），原植物园词库已替换 */
fun loadPeopleRolesFromAssets(context: Context): List<AnimalItem> =
    loadEmojiEnglishAsset(context, ASSET_PEOPLE_ROLES)

data class PuzzleWordPick(
    val emoji: String,
    val word: String,
    /** 词条中文释义（来自 JSON），供拼图页小字展示 */
    val chinese: String = ""
)

/**
 * Random English word from animals + plants JSON, letters only (for spelling).
 * [maxLetters] keeps tiles usable on small screens.
 */
fun pickRandomPuzzleWord(
    context: Context,
    random: Random = Random.Default,
    maxLetters: Int = 8
): PuzzleWordPick {
    val merged = loadAnimalsFromAssets(context) + loadPeopleRolesFromAssets(context)
    val candidates = merged.mapNotNull { item ->
        val lettersOnly = item.english.filter { it.isLetter() }
        if (lettersOnly.length in 2..maxLetters) {
            PuzzleWordPick(
                emoji = item.emoji,
                word = lettersOnly.uppercase(),
                chinese = item.chinese
            )
        } else {
            null
        }
    }.distinctBy { it.word }

    if (candidates.isEmpty()) {
        return PuzzleWordPick(emoji = "🐱", word = "CAT", chinese = "猫")
    }
    return candidates[random.nextInt(candidates.size)]
}
