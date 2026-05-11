package com.stagic.phantm.android.ui.panic

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.onboarding.PhantmGhostButton
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmDanger
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import kotlinx.coroutines.delay

@Composable
fun PanicWipeScreen(onCancel: () -> Unit, onWipe: () -> Unit = {}) {
    var count by remember { mutableIntStateOf(3) }
    val canWipe = count == 0

    LaunchedEffect(count) {
        if (count > 0) {
            delay(1000)
            count--
        }
    }

    val shake = rememberInfiniteTransition(label = "shake")
    val shakeX by shake.animateFloat(
        initialValue = 0f,
        targetValue = if (count > 0) 4f else 0f,
        animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse),
        label = "shakeX",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhantmBgBase),
    ) {
        // Dim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Warning icon
                Box(
                    modifier = Modifier
                        .scale(1f + shakeX * 0.01f)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(PhantmDanger.copy(alpha = 0.12f))
                        .border(1.dp, PhantmDanger.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚠", fontSize = 48.sp, color = PhantmDanger)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Wipe All Data?",
                    color = PhantmText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "This will permanently delete your identity, all messages, all contacts, and all encryption keys.",
                    color = PhantmText2,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Text(
                    text = " This action cannot be undone.",
                    color = PhantmText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                // What's being deleted
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PhantmBgElevated)
                        .border(1.dp, PhantmDivider, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "Identity (Phantm ID + keys)" to "7f3a9b8c…",
                        "Conversations" to "9 chats",
                        "Contacts" to "12 verified",
                        "Seed phrase backup" to "On device",
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("🗑", fontSize = 13.sp, color = PhantmDanger)
                            Text(label, color = PhantmText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(value, color = PhantmText2, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canWipe) PhantmDanger else Color(0xFF3B1E1E))
                        .clickable(enabled = canWipe) { onWipe() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (canWipe) "Wipe Everything" else "Wipe Everything ($count)",
                        color = if (canWipe) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                PhantmGhostButton(text = "Cancel", onClick = onCancel)
            }
        }
    }
}
