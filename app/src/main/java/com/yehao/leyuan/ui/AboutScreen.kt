package com.yehao.leyuan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.BuildConfig
import com.yehao.leyuan.audio.TtsVoicePrefs
import com.yehao.leyuan.audio.TtsVoiceProfile

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audio = LocalAppAudio.current
    val prefs = remember { TtsVoicePrefs(context) }
    var selected by remember { mutableStateOf(prefs.getProfile()) }
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8EAF6), Color(0xFFFFF8E1)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    audio.playSoftClick()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "烨浩乐园",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1565C0),
            )
            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                fontSize = 15.sp,
                color = Color(0xFF607D8B),
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )

            Text(
                text = "亲子益智小游戏合集。朗读声音若在某些手机上无声或不自然，可在下方切换。",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF455A64),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "朗读声音",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    TtsVoiceProfile.entries.forEach { profile ->
                        val checked = profile == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    audio.playSoftClick()
                                    selected = profile
                                    prefs.setProfile(profile)
                                    audio.applyVoiceProfile(profile)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = checked,
                                onClick = {
                                    audio.playSoftClick()
                                    selected = profile
                                    prefs.setProfile(profile)
                                    audio.applyVoiceProfile(profile)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1565C0),
                                ),
                            )
                            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(
                                    text = profile.labelZh,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF263238),
                                )
                                Text(
                                    text = profile.descriptionZh,
                                    fontSize = 13.sp,
                                    color = Color(0xFF78909C),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
