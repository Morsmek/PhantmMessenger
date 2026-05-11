package com.stagic.phantm.android.ui.qr

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.onboarding.PhantmGhostButton
import com.stagic.phantm.android.ui.onboarding.PhantmPrimaryButton
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmSuccess
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2

@Composable
fun QrIdentityScreen(
    name: String = "Alex K.",
    phantmId: String = "7f3a9b8c…2c1d0e8f",
    fingerprint: String = "a3f2b84c91e05d6278bc340fa1e9d72c4b58f063e2a197d4c8561b3e09f74a2d",
    onBack: () -> Unit,
) {
    val accentGlow = PhantmAccent.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhantmBgBase),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", color = PhantmText, fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = "My Phantm ID",
                    color = PhantmText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⋮", color = PhantmText, fontSize = 20.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // QR code box with accent border + glow
                val accentColor = PhantmAccent
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = accentGlow,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                                style = Stroke(width = 6.dp.toPx()),
                            )
                        }
                        .padding(4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    QrCodeCanvas(
                        data = fingerprint,
                        size = 220.dp,
                        cellColor = Color.Black,
                    )

                    // Center logo overlay
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PhantmBgBase)
                            .border(3.dp, Color.White, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("P", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = name,
                    color = PhantmText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )

                // ID chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PhantmBgElevated)
                        .border(1.dp, PhantmDivider, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🔒", fontSize = 13.sp, color = PhantmSuccess)
                    Text(
                        text = phantmId,
                        color = PhantmText2,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("📋", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PhantmGhostButton(text = "Copy ID", onClick = {})
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PhantmPrimaryButton(text = "Share", onClick = {})
                    }
                }

                Text(
                    text = "Ask contacts to scan this to connect with you securely. Your ID never leaves your device.",
                    color = PhantmText2,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
