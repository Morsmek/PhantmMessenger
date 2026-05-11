package com.stagic.phantm.android.ui.conversation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stagic.phantm.android.ui.components.NavTab
import com.stagic.phantm.android.ui.components.PhantmAvatar
import com.stagic.phantm.android.ui.components.PhantmBottomNav
import com.stagic.phantm.android.ui.components.PhantmDividerLine
import com.stagic.phantm.android.ui.components.PhantmWordmark
import com.stagic.phantm.android.ui.components.UnreadBadge
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmSuccess
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled

@Composable
fun ConversationListScreen(
    onConversationClick: (contactId: String) -> Unit,
    onNewChat: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val viewModel: ConversationListViewModel = viewModel()
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(
        containerColor = PhantmBgBase,
        bottomBar = {
            PhantmBottomNav(activeTab = NavTab.CHATS, chatBadge = conversations.sumOf { it.unreadCount }, onTabSelected = { tab ->
                if (tab == NavTab.SETTINGS) onSettingsClick()
            })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // App bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhantmWordmark()
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔍", fontSize = 18.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onNewChat() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✏️", fontSize = 18.sp)
                    }
                }

                // Cover traffic pill
                CoverTrafficPill(on = true)

                // Conversation list
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it.contactId }) { conv ->
                        ConversationRow(conv = conv, onClick = { onConversationClick(conv.contactId) })
                        PhantmDividerLine()
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .size(56.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(PhantmAccent)
                    .clickable { onNewChat() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✏️", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun CoverTrafficPill(on: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PhantmBgElevated)
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (on) PhantmSuccess else PhantmTextDisabled),
        )
        Text(
            text = "Cover Traffic: ${if (on) "ON" else "OFF"}",
            color = if (on) PhantmSuccess else PhantmText2,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Text(text = "3 chats secured", color = PhantmText2, fontSize = 12.sp)
        Text("›", color = PhantmText2, fontSize = 14.sp)
    }
}

@Composable
private fun ConversationRow(conv: ConversationUiState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PhantmAvatar(name = conv.displayName, size = 48.dp, online = conv.isOnline)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = conv.displayName,
                    color = PhantmText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (conv.isEncrypted) {
                    Text("🔒", fontSize = 11.sp)
                }
                if (conv.hasTimer) {
                    Text("⏱", fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = conv.formattedTime,
                    color = PhantmText2,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = conv.lastMessage,
                    color = if (conv.isGhostMessage) PhantmTextDisabled else PhantmText2,
                    fontSize = 14.sp,
                    fontStyle = if (conv.isGhostMessage) FontStyle.Italic else FontStyle.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conv.unreadCount > 0) {
                    UnreadBadge(conv.unreadCount)
                }
            }
        }
    }
}
