package com.stagic.phantm.android.ui.onboarding

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmAccent2
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled
import com.stagic.phantm.android.ui.theme.PhantmWarning

private val SEED_WORDS = listOf(
    "forest", "horizon", "cipher", "velvet",
    "orbit", "mountain", "breeze", "shadow",
    "phantom", "silver", "token", "narrow",
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> OnboardStep1(onNext = { step = 1 })
        1 -> OnboardStep2(onNext = { step = 2 }, onSkip = { step = 2 })
        2 -> OnboardStep3(onComplete = onComplete)
    }
}

@Composable
private fun StepDots(active: Int, total: Int = 3) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (i == active) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (i == active) PhantmAccent else PhantmDivider),
            )
        }
    }
}

@Composable
private fun OnboardStep1(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhantmBgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StepDots(active = 0)
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(PhantmAccent, PhantmAccent2))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("P", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Your Identity.\nYour Device.",
                    color = PhantmText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.5).sp,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "No phone number. No email. Your keys are generated locally and never leave this device.",
                    color = PhantmText2,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PhantmPrimaryButton(text = "Generate Identity", onClick = onNext)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Learn how this works  →", color = PhantmText2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun OnboardStep2(onNext: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhantmBgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StepDots(active = 1)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Back Up Your Identity",
                color = PhantmText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )

            Text(
                text = "You'll need this 12-word phrase to restore your identity on a new device.",
                color = PhantmText2,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Seed grid
            val rows = SEED_WORDS.chunked(3)
            rows.forEachIndexed { rowIdx, rowWords ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowWords.forEachIndexed { colIdx, word ->
                        val idx = rowIdx * 3 + colIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PhantmBgElevated)
                                .border(1.dp, PhantmAccent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = String.format("%02d", idx + 1),
                                    color = PhantmAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = word,
                                    color = PhantmText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
                if (rowIdx < rows.size - 1) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Warning box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PhantmWarning.copy(alpha = 0.10f))
                    .border(1.dp, PhantmWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("⚠", fontSize = 18.sp, color = PhantmWarning)
                Column {
                    Text("Write this down.", color = PhantmWarning, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Anyone with this phrase can access your identity. Phantm cannot recover it for you.",
                        color = Color(0xFFFCD34D),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            PhantmPrimaryButton(text = "I've Written It Down", onClick = onNext)
            Spacer(Modifier.height(8.dp))
            PhantmGhostButton(text = "Skip for Now", onClick = onSkip)
        }
    }
}

@Composable
private fun OnboardStep3(onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val initials = name.trim().split("\\s+".toRegex())
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PhantmBgBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StepDots(active = 2)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "What should contacts call you?",
                color = PhantmText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )

            Text(
                text = "This is shown to people you've connected with. Change it anytime.",
                color = PhantmText2,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Spacer(Modifier.height(36.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PhantmAccent, PhantmAccent2))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Name field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PhantmBgElevated)
                    .border(1.dp, PhantmDivider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (name.isEmpty()) {
                    Text("Display name", color = PhantmTextDisabled, fontSize = 16.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { if (it.length <= 32) name = it },
                        textStyle = TextStyle(color = PhantmText, fontSize = 16.sp),
                        cursorBrush = SolidColor(PhantmAccent),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Text("${name.length}/32", color = PhantmTextDisabled, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            PhantmPrimaryButton(
                text = "Enter Phantm",
                onClick = { if (name.trim().isNotEmpty()) onComplete() },
                enabled = name.trim().isNotEmpty(),
            )
        }
    }
}

@Composable
fun PhantmPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) PhantmAccent else PhantmAccent.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PhantmGhostButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PhantmDivider, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = PhantmText2, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
