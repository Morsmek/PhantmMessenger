package com.stagic.phantm.android.ui.message

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MessageUiState(
    val messageId: String,
    val senderName: String,
    val text: String,
    val timestampMs: Long,
    val isMine: Boolean,
    val isDecrypted: Boolean = true,
)

data class ThreadUiState(
    val contactId: String,
    val contactName: String,
    val isEndToEndVerified: Boolean,
    val messages: List<MessageUiState>,
)

class MessageThreadViewModel(private val contactId: String) : ViewModel() {

    private val _state = MutableStateFlow(mockThread(contactId))
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    private companion object {
        fun mockThread(contactId: String) = ThreadUiState(
            contactId = contactId,
            contactName = contactId.replaceFirstChar { it.uppercaseChar() },
            isEndToEndVerified = true,
            messages = listOf(
                MessageUiState("1", contactId, "Hello! Key exchange complete.", 1_700_000_010_000L, isMine = false),
                MessageUiState("2", "me", "Perfect — all messages are end-to-end encrypted.", 1_700_000_020_000L, isMine = true),
                MessageUiState("3", contactId, "Cover traffic is running too.", 1_700_000_030_000L, isMine = false),
                MessageUiState("4", "me", "Great — transport is CONNECTED.", 1_700_000_040_000L, isMine = true),
            ),
        )
    }
}

class MessageThreadViewModelFactory(private val contactId: String) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MessageThreadViewModel(contactId) as T
}
