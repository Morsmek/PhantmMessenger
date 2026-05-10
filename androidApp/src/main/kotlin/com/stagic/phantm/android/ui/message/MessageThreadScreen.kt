package com.stagic.phantm.android.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun MessageThreadScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: MessageThreadViewModel = viewModel(factory = MessageThreadViewModelFactory(contactId)),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = stringResource(R.string.cd_back)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Column {
                        Text(state.contactName, style = MaterialTheme.typography.titleMedium)
                        E2EStatusRow(verified = state.isEndToEndVerified)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.messages, key = { it.messageId }) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun E2EStatusRow(verified: Boolean) {
    val label = if (verified) stringResource(R.string.e2e_verified) else stringResource(R.string.e2e_unverified)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = if (verified) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            tint = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun MessageBubble(message: MessageUiState) {
    val bubbleDesc = stringResource(
        if (message.isMine) R.string.cd_my_message else R.string.cd_their_message,
        message.senderName, message.text,
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp,
            ),
            color = if (message.isMine)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .semantics { contentDescription = bubbleDesc },
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isMine)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatTimestamp(message.timestampMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (message.isMine)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
