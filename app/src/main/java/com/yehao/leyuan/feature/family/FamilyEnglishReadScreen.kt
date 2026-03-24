package com.yehao.leyuan.feature.family

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.audio.AppAudio
import com.yehao.leyuan.ui.LocalAppAudio

/** 占位符：{{me}} {{father}} {{mother}} {{brother}}（弟弟）{{olderSister}} {{youngerSister}} */
private data class ReadTemplate(
    val titleZh: String,
    val descriptionZh: String,
    val lines: List<String>,
)

private val PlaceholderRegex = Regex("\\{\\{(\\w+)\\}\\}")

private fun slotLabelZh(key: String): String = when (key) {
    "me" -> "我的名字"
    "father" -> "爸爸"
    "mother" -> "妈妈"
    "brother" -> "弟弟"
    "olderSister" -> "姐姐"
    "youngerSister" -> "妹妹"
    else -> key
}

/** 展开区预览：已填名字显示原文，未填显示「待填」提示 */
private fun fillTemplateLineForPreview(line: String, displaySlots: Map<String, String>): String {
    var out = line
    PlaceholderRegex.findAll(line).map { it.groupValues[1] }.toSet().forEach { key ->
        val v = displaySlots[key]?.trim().orEmpty()
        val repl = if (v.isNotEmpty()) v else "（待填：${slotLabelZh(key)}）"
        out = out.replace("{{$key}}", repl)
    }
    return out
}

private fun buildSpokenLines(template: ReadTemplate, slots: Map<String, String>): List<String> {
    return template.lines.mapNotNull { line ->
        val keys = PlaceholderRegex.findAll(line).map { it.groupValues[1] }.toSet()
        if (keys.any { slots[it].isNullOrBlank() }) return@mapNotNull null
        var out = line
        slots.forEach { (k, v) ->
            out = out.replace("{{$k}}", v)
        }
        out.trim().takeIf { it.isNotEmpty() }
    }
}

/**
 * 停止当前有道朗读，并按已填名字播放指定模版。
 * @return 失败时的提示文案；成功返回 `null`（调用方可将 `hint` 置空）。
 */
private fun playFamilyTemplateIfReady(
    audio: AppAudio,
    tpl: ReadTemplate,
    slotsForSpeech: Map<String, String>,
    myNameRaw: String,
): String? {
    if (tpl.lines.any { it.contains("{{me}}") } && myNameRaw.isBlank()) {
        return "请先填写「我的名字」。"
    }
    val lines = buildSpokenLines(tpl, slotsForSpeech)
    if (lines.isEmpty()) {
        return "这几句里还有空着的名字，请把对应框填上名字再试。"
    }
    audio.stopYoudaoPlayback()
    lines.forEachIndexed { i, line ->
        audio.speak(line, append = i > 0, allowWordTokenFallback = false)
    }
    return null
}

private val ReadTemplates = listOf(
    ReadTemplate(
        titleZh = "我的家人",
        descriptionZh = "我的名字、爸爸、妈妈、弟弟、姐姐、妹妹（填了的才会读）",
        lines = listOf(
            "My name is {{me}}.",
            "My father is {{father}}.",
            "My mother is {{mother}}.",
            "My little brother is {{brother}}.",
            "My older sister is {{olderSister}}.",
            "My younger sister is {{youngerSister}}.",
        ),
    ),
    ReadTemplate(
        titleZh = "你好呀",
        descriptionZh = "简单打招呼，只需填自己的名字",
        lines = listOf(
            "Hello! My name is {{me}}.",
            "I am {{me}}.",
            "Nice to meet you!",
        ),
    ),
    ReadTemplate(
        titleZh = "这是爸爸妈妈",
        descriptionZh = "介绍爸爸和妈妈",
        lines = listOf(
            "This is my daddy. His name is {{father}}.",
            "This is my mommy. Her name is {{mother}}.",
            "I am {{me}}. I love my mommy and daddy.",
        ),
    ),
    ReadTemplate(
        titleZh = "早上好",
        descriptionZh = "和爸爸妈妈说早安",
        lines = listOf(
            "Good morning, {{father}}!",
            "Good morning, {{mother}}!",
            "Good morning! I am {{me}}.",
        ),
    ),
    ReadTemplate(
        titleZh = "我们一起玩",
        descriptionZh = "叫姐姐、弟弟、妹妹一起玩",
        lines = listOf(
            "I am {{me}}.",
            "{{olderSister}}, let us play together!",
            "{{brother}}, come here!",
            "{{youngerSister}}, let us sing a song!",
        ),
    ),
    ReadTemplate(
        titleZh = "我爱你",
        descriptionZh = "对爸爸妈妈说我爱你",
        lines = listOf(
            "Daddy, I love you! You are {{father}}.",
            "Mommy, I love you! You are {{mother}}.",
            "I am {{me}}. I love my family!",
        ),
    ),
    ReadTemplate(
        titleZh = "点名啦",
        descriptionZh = "一个一个念名字，像幼儿园点名",
        lines = listOf(
            "Here is {{me}}!",
            "Here is {{father}}!",
            "Here is {{mother}}!",
            "Here is {{brother}}!",
            "Here is {{olderSister}}!",
            "Here is {{youngerSister}}!",
        ),
    ),
    ReadTemplate(
        titleZh = "小小儿歌",
        descriptionZh = "短句、好记，适合跟读",
        lines = listOf(
            "{{me}} is my name. Hooray!",
            "Daddy {{father}} is strong!",
            "Mommy {{mother}} is sweet!",
            "My brother {{brother}} is cute!",
            "My sister {{olderSister}} is nice!",
            "My sister {{youngerSister}} is lovely!",
        ),
    ),
)

private val TemplateEmojis = listOf("👨‍👩‍👧", "👋", "💑", "☀️", "🎮", "💕", "📣", "🎵")

private val TemplateAccentPairs: List<Pair<Color, Color>> = listOf(
    Color(0xFFFF7043) to Color(0xFFFFF3E0),
    Color(0xFF42A5F5) to Color(0xFFE3F2FD),
    Color(0xFFAB47BC) to Color(0xFFF3E5F5),
    Color(0xFFFFCA28) to Color(0xFFFFFDE7),
    Color(0xFF66BB6A) to Color(0xFFE8F5E9),
    Color(0xFFEC407A) to Color(0xFFFCE4EC),
    Color(0xFF26C6DA) to Color(0xFFE0F7FA),
    Color(0xFF7E57C2) to Color(0xFFEDE7F6),
)

private val PreviewLineBubbleColors = listOf(
    Color(0xFFFFF9C4),
    Color(0xFFE1F5FE),
    Color(0xFFF8BBD0),
    Color(0xFFC8E6C9),
    Color(0xFFD1C4E9),
    Color(0xFFFFCCBC),
)

@Composable
fun FamilyEnglishReadScreen(onBack: () -> Unit = {}) {
    val audio = LocalAppAudio.current
    val context = LocalContext.current
    val prefs = remember { FamilyNamePrefs(context) }

    var prefsLoaded by remember { mutableStateOf(false) }

    var myName by remember { mutableStateOf("") }
    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var youngerBrother by remember { mutableStateOf("") }
    var olderSister by remember { mutableStateOf("") }
    var youngerSister by remember { mutableStateOf("") }

    var templateIndex by remember { mutableIntStateOf(0) }
    var expandedTemplateIndex by remember { mutableStateOf<Int?>(null) }
    var hint by remember { mutableStateOf("") }

    DisposableEffect(audio) {
        onDispose {
            audio.stopYoudaoPlayback()
        }
    }

    LaunchedEffect(Unit) {
        val s = prefs.load()
        myName = s.myName
        father = s.father
        mother = s.mother
        youngerBrother = s.youngerBrother
        olderSister = s.olderSister
        youngerSister = s.youngerSister
        templateIndex = prefs.loadTemplateIndex().coerceIn(0, ReadTemplates.lastIndex)
        prefsLoaded = true
    }

    LaunchedEffect(
        myName,
        father,
        mother,
        youngerBrother,
        olderSister,
        youngerSister,
        templateIndex,
        prefsLoaded,
    ) {
        if (!prefsLoaded) return@LaunchedEffect
        prefs.save(
            FamilyNameSlots(
                myName = myName,
                father = father,
                mother = mother,
                youngerBrother = youngerBrother,
                olderSister = olderSister,
                youngerSister = youngerSister,
            ),
            templateIndex = templateIndex,
        )
    }

    /** 界面预览用：显示用户输入的原文 */
    val slotsForDisplay = mapOf(
        "me" to myName.trim(),
        "father" to father.trim(),
        "mother" to mother.trim(),
        "brother" to youngerBrother.trim(),
        "olderSister" to olderSister.trim(),
        "youngerSister" to youngerSister.trim(),
    )
    /** 朗读用：中文名会转成拼音，便于有道英文发音接口朗读整句 */
    val slotsForSpeech = mapOf(
        "me" to myName.trim().toSpeechNameForEnglishTts(),
        "father" to father.trim().toSpeechNameForEnglishTts(),
        "mother" to mother.trim().toSpeechNameForEnglishTts(),
        "brother" to youngerBrother.trim().toSpeechNameForEnglishTts(),
        "olderSister" to olderSister.trim().toSpeechNameForEnglishTts(),
        "youngerSister" to youngerSister.trim().toSpeechNameForEnglishTts(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF8E1), Color(0xFFE1F5FE)),
                ),
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    audio.playSoftClick()
                    audio.stopYoudaoPlayback()
                    onBack()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color(0xFF5E35B1),
                ),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = "家庭英语",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF5E35B1),
                )
                Text(
                    text = "点卡片展开看英文，再点「听一听」或下面「开始朗读」播放",
                    fontSize = 13.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "朗读模版 · 点一下展开",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF5D4037),
        )
        Spacer(modifier = Modifier.height(10.dp))

        ReadTemplates.forEachIndexed { index, tpl ->
            val selected = index == templateIndex
            val expanded = expandedTemplateIndex == index
            val (accent, softBg) = TemplateAccentPairs[index % TemplateAccentPairs.size]
            val emoji = TemplateEmojis[index % TemplateEmojis.size]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (expanded) softBg else Color.White.copy(alpha = 0.92f),
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (expanded) 6.dp else 2.dp,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    brush = Brush.linearGradient(
                        if (selected) {
                            listOf(accent, accent.copy(alpha = 0.6f), Color(0xFFFFB74D))
                        } else {
                            listOf(Color(0xFFE0E0E0), Color(0xFFEEEEEE))
                        },
                    ),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audio.playSoftClick()
                                templateIndex = index
                                expandedTemplateIndex = if (expanded) null else index
                                hint = ""
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.25f),
                            modifier = Modifier.size(48.dp),
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 26.sp,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text(
                                text = tpl.titleZh,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3E2723),
                            )
                            Text(
                                text = tpl.descriptionZh,
                                fontSize = 12.sp,
                                color = Color(0xFF6D4C41),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            tint = accent,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        ) {
                            Text(
                                text = "今天要说的英语",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBF360C),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            tpl.lines.forEachIndexed { li, rawLine ->
                                val filled = fillTemplateLineForPreview(rawLine, slotsForDisplay)
                                val bubble = PreviewLineBubbleColors[li % PreviewLineBubbleColors.size]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .background(bubble, RoundedCornerShape(16.dp))
                                        .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accent.copy(alpha = 0.85f),
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Text(
                                            text = "${li + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(4.dp),
                                        )
                                    }
                                    Text(
                                        text = filled,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    audio.playSoftClick()
                                    templateIndex = index
                                    hint = playFamilyTemplateIfReady(
                                        audio = audio,
                                        tpl = tpl,
                                        slotsForSpeech = slotsForSpeech,
                                        myNameRaw = myName,
                                    ) ?: ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6D00),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("听一听", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text(" 🔊", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "家人的名字",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF37474F),
        )
        Spacer(modifier = Modifier.height(8.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF7E57C2),
            unfocusedBorderColor = Color(0xFFB39DDB),
            focusedLabelColor = Color(0xFF5E35B1),
        )

        val nameFocusRequesters = remember { List(6) { FocusRequester() } }

        @Composable
        fun NameField(
            value: String,
            onValueChange: (String) -> Unit,
            label: String,
            placeholder: String,
            focusRequester: FocusRequester,
            imeAction: ImeAction,
            onImeNext: () -> Unit,
        ) {
            val focusManager = LocalFocusManager.current
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(bottom = 10.dp),
                label = { Text(label, fontSize = 14.sp) },
                placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF9E9E9E)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { onImeNext() },
                    onDone = { focusManager.clearFocus() },
                ),
            )
        }

        NameField(
            myName,
            { myName = it },
            "我的名字",
            "中文或英文，如 小军 / Tom",
            nameFocusRequesters[0],
            ImeAction.Next,
        ) { nameFocusRequesters[1].requestFocus() }
        NameField(
            father,
            { father = it },
            "爸爸",
            "如 展先生 / David",
            nameFocusRequesters[1],
            ImeAction.Next,
        ) { nameFocusRequesters[2].requestFocus() }
        NameField(
            mother,
            { mother = it },
            "妈妈",
            "如 李女士 / Mary",
            nameFocusRequesters[2],
            ImeAction.Next,
        ) { nameFocusRequesters[3].requestFocus() }
        NameField(
            youngerBrother,
            { youngerBrother = it },
            "弟弟",
            "如 弟弟的名字",
            nameFocusRequesters[3],
            ImeAction.Next,
        ) { nameFocusRequesters[4].requestFocus() }
        NameField(
            olderSister,
            { olderSister = it },
            "姐姐",
            "如 姐姐的名字",
            nameFocusRequesters[4],
            ImeAction.Next,
        ) { nameFocusRequesters[5].requestFocus() }
        NameField(
            youngerSister,
            { youngerSister = it },
            "妹妹",
            "如 妹妹的名字",
            nameFocusRequesters[5],
            ImeAction.Done,
        ) { }

        if (hint.isNotEmpty()) {
            Text(
                text = hint,
                color = Color(0xFFC62828),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                audio.playSoftClick()
                val tpl = ReadTemplates.getOrNull(templateIndex) ?: return@Button
                hint = playFamilyTemplateIfReady(
                    audio = audio,
                    tpl = tpl,
                    slotsForSpeech = slotsForSpeech,
                    myNameRaw = myName,
                ) ?: ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7E57C2),
                contentColor = Color.White,
            ),
        ) {
            Text("开始朗读", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
