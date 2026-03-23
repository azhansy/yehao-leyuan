package com.yehao.leyuan.feature.letter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yehao.leyuan.ui.theme.SkyBlue

@Composable
fun LetterPuzzleGameOverDialog(
    level: Int,
    totalScore: Int,
    wordsCleared: Int,
    onPlayAgain: () -> Unit
) {
    Dialog(onDismissRequest = onPlayAgain) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "时间到！",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE53935)
                )
                Text(
                    text = "本局结束 · 看看你的成绩",
                    fontSize = 16.sp,
                    color = Color(0xFF546E7A),
                    modifier = Modifier.padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "总得分",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF607D8B)
                )
                Text(
                    text = "$totalScore",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1565C0)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "拼对单词：$wordsCleared 个",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF37474F)
                )
                Text(
                    text = "当前等级：第 $level 级",
                    fontSize = 16.sp,
                    color = Color(0xFF78909C),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
                ) {
                    Text("再来一局", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将重新开始计分与倒计时",
                    fontSize = 13.sp,
                    color = Color(0xFF90A4AE)
                )
            }
        }
    }
}
