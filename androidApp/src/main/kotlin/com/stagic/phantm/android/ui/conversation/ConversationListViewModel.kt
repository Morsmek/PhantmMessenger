package com.stagic.phantm.android.ui.conversation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConversationUiState(
    val contactId: String,
    val displayName: String,
    val lastMessage: String,
    val timestampMs: Long,
    val unreadCount: Int = 0,
    val isEncrypted: Boolean = true,
)

class ConversationListViewModel : ViewModel() {

    private val _conversations = MutableStateFlow(MOCK_CONVERSATIONS)
    val conversations: StateFlow<List<ConversationUiState>> = _conversations.asStateFlow()

    private companion object {
        val MOCK_CONVERSATIONS = listOf(
            ConversationUiState("alice", "Alice", "See you at the relay 🔐", 1_700_000_100_000L, unreadCount = 2),
            ConversationUiState("bob", "Bob", "Key exchange complete", 1_700_000_050_000L, unreadCount = 0),
            ConversationUiState("carol", "Carol", "Cover traffic is running", 1_699_999_900_000L, unreadCount = 1),
        )
    }
}
