package com.stagic.phantm.android.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.components.PhantmAvatar
import com.stagic.phantm.android.ui.components.PhantmDividerLine
import com.stagic.phantm.android.ui.components.PhantmToggle
import com.stagic.phantm.android.ui.components.SectionHeader
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmBgSurface
import com.stagic.phantm.android.ui.theme.PhantmDanger
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPanicWipe: () -> Unit = {},
) {
    var readReceipts by remember { mutableStateOf(true) }
    var coverTraffic by remember { mutableStateOf(true) }
    var screenshot by remember { mutableStateOf(true) }
    var pushNotifs by remember { mutableStateOf(true) }
    var messagePreview by remember { mutableStateOf(false) }

    Scaffold(containerColor = PhantmBgBase) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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
                    text = "Settings",
                    color = PhantmText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Profile card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PhantmBgSurface)
                        .border(1.dp, PhantmDivider, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PhantmAvatar(name = "Alex K.", size = 64.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alex K.", color = PhantmText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "7f3a9b8c…2c1d0e8f",
                            color = PhantmText2,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = "Edit Profile  →",
                            color = PhantmAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { },
                        )
                    }
                }

                SectionHeader(title = "Privacy")
                SettingsRow(icon = "⏱", label = "Disappearing Messages", sub = "Default for new chats", value = "Off")
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsToggleRow(icon = "👁", label = "Read Receipts", checked = readReceipts, onToggle = { readReceipts = it })
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsToggleRow(icon = "〰", label = "Cover Traffic", sub = "Decoy packets prevent traffic analysis", checked = coverTraffic, onToggle = { coverTraffic = it }, accentIcon = true)
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsToggleRow(icon = "🚫", label = "Screenshot Protection", checked = screenshot, onToggle = { screenshot = it })

                SectionHeader(title = "Security")
                SettingsRow(icon = "🔑", label = "Change Passphrase")
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "🌱", label = "View Seed Phrase", sub = "Requires biometric auth")
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "📱", label = "Active Sessions", value = "2 devices")
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "🔄", label = "Rotate Identity Key", sub = "Last rotated 11 days ago")

                // Panic Wipe destructive row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PhantmDanger.copy(alpha = 0.06f))
                        .border(1.dp, PhantmDanger.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { onPanicWipe() }
                        .padding(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PhantmDanger.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⚠", fontSize = 18.sp, color = PhantmDanger)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Panic Wipe", color = PhantmDanger, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Erase all data immediately", color = PhantmDanger.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        Text("›", color = PhantmDanger, fontSize = 18.sp)
                    }
                }

                SectionHeader(title = "Notifications")
                SettingsToggleRow(icon = "🔔", label = "Push Notifications", checked = pushNotifs, onToggle = { pushNotifs = it })
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsToggleRow(icon = "👁", label = "Show Message Preview", sub = "Recommended off for max privacy", checked = messagePreview, onToggle = { messagePreview = it })
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "🔈", label = "Notification Sound", value = "Phantm Pulse")

                SectionHeader(title = "About")
                SettingsRow(icon = "👻", label = "Phantm", sub = "Version 0.1.0 (alpha)", hasChevron = false)
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "↗", label = "Open Source", sub = "github.com/phantm")
                PhantmDividerLine(Modifier.padding(start = 68.dp))
                SettingsRow(icon = "🔒", label = "Security Audit", sub = "View 2025 audit report")

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    sub: String? = null,
    value: String? = null,
    hasChevron: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(if (sub != null) 64.dp else 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PhantmBgElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = PhantmText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(sub, color = PhantmText2, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (value != null) {
            Text(value, color = PhantmText2, fontSize = 14.sp)
        }
        if (hasChevron) {
            Text("›", color = PhantmTextDisabled, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: String,
    label: String,
    sub: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentIcon: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(if (sub != null) 64.dp else 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PhantmBgElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 18.sp, color = if (accentIcon) PhantmAccent else Color.Unspecified)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = PhantmText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(sub, color = PhantmText2, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        PhantmToggle(checked = checked)
    }
}
