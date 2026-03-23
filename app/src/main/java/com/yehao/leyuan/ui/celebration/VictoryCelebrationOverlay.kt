package com.yehao.leyuan.ui.celebration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yehao.leyuan.ui.theme.GrassGreen
import com.yehao.leyuan.ui.theme.OrangePop
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun VictoryCelebrationOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    continueLabel: String = "继续"
) {
    if (!visible) return

    val party = rememberInfiniteTransition(label = "victoryParty")
    val pulse by party.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val sway by party.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sway"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    Color(0xFFFFFDE7),
                    RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val wobble = (sin(sway * 2 * PI).toFloat() * 8f).roundToInt()
                Text(
                    text = "🎉",
                    fontSize = 80.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        rotationZ = sway * 12f - 6f
                    }
                )
                Text(
                    text = "🎊",
                    fontSize = 64.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.1f - (pulse - 0.88f) * 0.5f
                        scaleY = 1.1f - (pulse - 0.88f) * 0.5f
                        translationY = wobble.toFloat()
                    }
                )
                Text(
                    text = "✨",
                    fontSize = 56.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = -sway * 20f
                        scaleX = pulse * 0.95f
                        scaleY = pulse * 0.95f
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "你太棒了！",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OrangePop,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You are so great!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            subtitle?.let { line ->
                Text(
                    text = line,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF37474F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(continueLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
