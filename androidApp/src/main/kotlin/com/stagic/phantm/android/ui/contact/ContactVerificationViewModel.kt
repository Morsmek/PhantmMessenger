package com.stagic.phantm.android.ui.contact

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ContactVerificationUiState(
    val contactId: String,
    val displayName: String,
    /** Hex-encoded Ed25519 public key fingerprint (64 chars = 32 bytes). */
    val ed25519Fingerprint: String,
    val isVerified: Boolean,
)

class ContactVerificationViewModel(private val contactId: String) : ViewModel() {

    private val _state = MutableStateFlow(mockState(contactId))
    val state: StateFlow<ContactVerificationUiState> = _state.asStateFlow()

    private companion object {
        fun mockState(contactId: String) = ContactVerificationUiState(
            contactId = contactId,
            displayName = contactId.replaceFirstChar { it.uppercaseChar() },
            // 32-byte Ed25519 public key fingerprint — mock hex string
            ed25519Fingerprint = "a3f2b84c91e05d6278bc340fa1e9d72c4b58f063e2a197d4c8561b3e09f74a2d",
            isVerified = false,
        )
    }
}

class ContactVerificationViewModelFactory(private val contactId: String) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ContactVerificationViewModel(contactId) as T
}
