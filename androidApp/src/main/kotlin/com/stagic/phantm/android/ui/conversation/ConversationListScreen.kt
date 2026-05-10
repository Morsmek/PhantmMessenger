package com.stagic.phantm.android.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stagic.phantm.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (contactId: String) -> Unit,
    viewModel: ConversationListViewModel = viewModel(),
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.screen_conversations)) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(conversations, key = { it.contactId }) { conversation ->
                ConversationRow(
                    state = conversation,
                    onClick = { onConversationClick(conversation.contactId) },
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun ConversationRow(state: ConversationUiState, onClick: () -> Unit) {
    val rowDesc = stringResource(R.string.cd_conversation_row, state.displayName, state.unreadCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.cd_open_conversation, state.displayName)) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = rowDesc },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(name = state.displayName)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTimestamp(state.timestampMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.isEncrypted) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_encrypted),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Text(
                    text = state.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
        if (state.unreadCount > 0) {
            Spacer(Modifier.width(8.dp))
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.cd_unread_count, state.unreadCount) },
            ) {
                Text(state.unreadCount.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AvatarCircle(name: String) {
    val initial = name.firstOrNull()?.uppercaseChar() ?: '?'
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = name },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
