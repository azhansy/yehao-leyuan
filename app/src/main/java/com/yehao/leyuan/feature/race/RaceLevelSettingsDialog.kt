package com.yehao.leyuan.feature.race

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun RaceLevelSettingsDialog(
    currentLevel: Int,
    playSteerTts: Boolean,
    onPlaySteerTtsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSelectLevel: (Int) -> Unit,
) {
    var steerTts by remember(playSteerTts) { mutableStateOf(playSteerTts) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "赛车躲避 · 等级",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F),
                )
                Text(
                    text = "1 级最轻松，9 级最难；已保存在本机",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF607D8B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "点左/右时朗读",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF263238),
                        )
                        Text(
                            text = "默认关闭；开启后点左/右会读 Left / Right，仍保留轻触声",
                            fontSize = 13.sp,
                            color = Color(0xFF78909C),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Switch(
                        checked = steerTts,
                        onCheckedChange = {
                            steerTts = it
                            onPlaySteerTtsChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0277BD),
                            checkedTrackColor = Color(0xFF81D4FA),
                        ),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    for (lv in 1..9) {
                        val d = raceDifficultyForLevel(lv)
                        val selected = lv == currentLevel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (selected) Color(0xFFB3E5FC) else Color.White.copy(alpha = 0.7f),
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { onSelectLevel(lv) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$lv",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color(0xFF0277BD) else Color(0xFF455A64),
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "第 ${lv} 级",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF263238),
                                )
                                Text(
                                    text = d.summaryZh,
                                    fontSize = 14.sp,
                                    color = Color(0xFF607D8B),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("关闭", fontSize = 17.sp)
                }
            }
        }
    }
}
