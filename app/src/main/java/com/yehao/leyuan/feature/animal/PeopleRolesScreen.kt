package com.yehao.leyuan.feature.animal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PeopleRolesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<AnimalItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) { loadPeopleRolesFromAssets(context) }
    }

    val titleBrush = Brush.linearGradient(
        listOf(Color(0xFF3949AB), Color(0xFF5C6BC0), SkyBlue, Color(0xFF00838F)),
    )

    VocabularyIdentificationContent(
        title = "职业与称呼",
        subtitle = "家人、老师、医生、警察…点一点听英文",
        items = items,
        emptyMessage = "正在加载…\n若一直为空，请检查 assets/people_roles.json",
        onBack = onBack,
        backgroundGradient = listOf(Color(0xFFE8EAF6), Color(0xFFE0F7FA)),
        titleBrush = titleBrush,
        subtitleColor = Color(0xFF283593),
        backIconTint = Color(0xFF3949AB),
        backContainerColor = Color.White.copy(alpha = 0.9f),
    )
}
