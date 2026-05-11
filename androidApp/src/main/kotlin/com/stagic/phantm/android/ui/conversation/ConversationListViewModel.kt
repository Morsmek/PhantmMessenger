package com.stagic.phantm.android.ui.conversation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConversationUiState(
    val contactId: String,
    val displayName: String,
    val lastMessage: String,
    val formattedTime: String,
    val unreadCount: Int = 0,
    val isEncrypted: Boolean = true,
    val hasTimer: Boolean = false,
    val isOnline: Boolean = false,
    val isGhostMessage: Boolean = false,
)

class ConversationListViewModel : ViewModel() {

    private val _conversations = MutableStateFlow(MOCK_CONVERSATIONS)
    val conversations: StateFlow<List<ConversationUiState>> = _conversations.asStateFlow()

    private companion object {
        val MOCK_CONVERSATIONS = listOf(
            ConversationUiState("alex", "Alex K.", "Got the package. Burning it now.", "14:32", unreadCount = 3, hasTimer = true, isOnline = true),
            ConversationUiState("jordan", "Jordan M.", "See you at the location at 19:00 ✓", "13:18", unreadCount = 0, isOnline = true),
            ConversationUiState("river", "River T.", "Voice message · 0:42", "12:04", unreadCount = 1, hasTimer = true),
            ConversationUiState("cipher-group", "Cipher Group", "Maya: rotated my keys this morning", "11:51", unreadCount = 7),
            ConversationUiState("sam", "Sam L.", "Image", "Yesterday"),
            ConversationUiState("morgan", "Morgan H.", "You: ack", "Mon", hasTimer = true),
            ConversationUiState("devon", "Devon P.", "Deleted message", "Sun", isGhostMessage = true),
            ConversationUiState("kai", "Kai R.", "I rotated the fingerprint, can you verify?", "08 May"),
        )
    }
}
