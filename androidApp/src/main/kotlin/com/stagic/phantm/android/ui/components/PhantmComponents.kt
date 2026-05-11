package com.stagic.phantm.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmAccent2
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgSurface
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmSuccess
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled

private val AvatarPalette = listOf(
    Color(0xFFE8006F), Color(0xFFFF4DA6), Color(0xFF7B2A5C),
    Color(0xFFC2185B), Color(0xFFAD1457), Color(0xFFD81B60),
)

@Composable
fun PhantmAvatar(
    name: String,
    size: Dp = 48.dp,
    online: Boolean = false,
) {
    val hash = name.sumOf { it.code }
    val bg = AvatarPalette[hash % AvatarPalette.size]
    val initials = name.trim().split("\\s+".toRegex())
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
    val fontSize = (size.value * 0.38f).sp

    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(PhantmBgBase)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(PhantmSuccess),
            )
        }
    }
}

@Composable
fun PhantmWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Simple "P" ghost icon as wordmark logo
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(listOf(PhantmAccent, PhantmAccent2)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            text = "phantm",
            color = PhantmText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = (-0.7).sp,
        )
    }
}

enum class NavTab { CHATS, CONTACTS, SETTINGS }

@Composable
fun PhantmBottomNav(
    activeTab: NavTab,
    chatBadge: Int = 0,
    onTabSelected: (NavTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PhantmBgSurface)
            .border(width = 1.dp, color = PhantmDivider, shape = RoundedCornerShape(0.dp))
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavTab.entries.forEach { tab ->
            val active = tab == activeTab
            val color = if (active) PhantmAccent else PhantmText2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box {
                        NavTabIcon(tab = tab, color = color)
                        if (tab == NavTab.CHATS && chatBadge > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-3).dp)
                                    .background(PhantmAccent, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = chatBadge.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        text = tab.label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabIcon(tab: NavTab, color: Color) {
    // Simple text-based icons sized 22dp
    val label = when (tab) {
        NavTab.CHATS -> "💬"
        NavTab.CONTACTS -> "👤"
        NavTab.SETTINGS -> "⚙"
    }
    Text(text = label, fontSize = 20.sp)
}

private val NavTab.label: String
    get() = when (this) {
        NavTab.CHATS -> "Chats"
        NavTab.CONTACTS -> "Contacts"
        NavTab.SETTINGS -> "Settings"
    }

@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(PhantmAccent, RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0x2622C55E), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "VERIFIED",
            color = PhantmSuccess,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        color = PhantmText2,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun PhantmDividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PhantmDivider),
    )
}

@Composable
fun PhantmToggle(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) PhantmAccent else PhantmTextDisabled.copy(alpha = 0.4f))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
