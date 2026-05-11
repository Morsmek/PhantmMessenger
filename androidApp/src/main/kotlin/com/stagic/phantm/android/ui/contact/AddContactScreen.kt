package com.stagic.phantm.android.ui.contact

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagic.phantm.android.ui.components.PhantmAvatar
import com.stagic.phantm.android.ui.components.SectionHeader
import com.stagic.phantm.android.ui.onboarding.PhantmGhostButton
import com.stagic.phantm.android.ui.onboarding.PhantmPrimaryButton
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmSuccess
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled

private val RECENT_CONTACTS = listOf(
    Triple("Alex K.", true, "Last seen 2m ago"),
    Triple("Jordan M.", true, "Last seen 4m ago"),
    Triple("River T.", false, "Last seen 1h ago"),
    Triple("Sam L.", false, "Last seen yesterday"),
)

@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onContactSelected: (name: String) -> Unit = {},
) {
    var phantmId by remember { mutableStateOf("") }

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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    text = "New Chat",
                    color = PhantmText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 18.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PhantmBgElevated)
                        .border(1.dp, PhantmDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🔍", fontSize = 16.sp, color = PhantmText2)
                        Box(modifier = Modifier.weight(1f)) {
                            Text("Search by name or Phantm ID", color = PhantmTextDisabled, fontSize = 16.sp)
                        }
                    }
                }

                // Scan QR button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, PhantmAccent, RoundedCornerShape(12.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("📷", fontSize = 18.sp)
                        Text("Scan QR Code", color = PhantmAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                SectionHeader(title = "Recent Contacts")

                RECENT_CONTACTS.forEach { (name, online, sub) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactSelected(name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PhantmAvatar(name = name, size = 48.dp, online = online)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(name, color = PhantmText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("🔒", fontSize = 11.sp)
                                Text("✓", color = PhantmSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(sub, color = PhantmText2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, PhantmDivider, RoundedCornerShape(999.dp))
                                .clickable { onContactSelected(name) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("Chat", color = PhantmAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                SectionHeader(title = "Add by Phantm ID")

                // Phantm ID input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PhantmBgElevated)
                        .border(1.dp, PhantmDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (phantmId.isEmpty()) {
                        Text("7f3a9b8c…66 chars", color = PhantmTextDisabled, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = phantmId,
                        onValueChange = { phantmId = it },
                        textStyle = TextStyle(color = PhantmText, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        cursorBrush = SolidColor(PhantmAccent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PhantmPrimaryButton(text = "Verify & Add", onClick = {}, enabled = phantmId.isNotBlank())
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
