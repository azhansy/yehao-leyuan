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
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.SkyBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlantGardenScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var plants by remember { mutableStateOf<List<AnimalItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        plants = withContext(Dispatchers.IO) { loadPlantsFromAssets(context) }
    }

    val titleBrush = Brush.linearGradient(
        listOf(Color(0xFF2E7D32), GrassGreen, SkyBlue, Color(0xFF00ACC1)),
    )

    VocabularyIdentificationContent(
        title = "植物园",
        subtitle = "点一点，听英文名字",
        items = plants,
        emptyMessage = "正在加载植物…\n若一直为空，请检查 assets/plants.json",
        onBack = onBack,
        backgroundGradient = listOf(Color(0xFFE8F5E9), Color(0xFFE0F7FA)),
        titleBrush = titleBrush,
        subtitleColor = Color(0xFF1B5E20),
        backIconTint = Color(0xFF2E7D32),
        backContainerColor = Color.White.copy(alpha = 0.9f),
    )
}
