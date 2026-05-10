package com.stagic.phantm.android.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stagic.phantm.android.R
import com.stagic.phantm.android.ui.qr.QrCodeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactVerificationScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactVerificationViewModel = viewModel(
        factory = ContactVerificationViewModelFactory(contactId),
    ),
) {
    val state by viewModel.state.collectAsState()

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
                title = { Text(stringResource(R.string.screen_verify_contact, state.displayName)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.verify_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            // QR code of Ed25519 fingerprint (AC-M13-3)
            QrCodeCanvas(
                data = state.ed25519Fingerprint,
                size = 220.dp,
                modifier = Modifier.semantics {
                    contentDescription = stringResource(R.string.cd_fingerprint_qr, state.displayName)
                },
            )

            Text(
                text = stringResource(R.string.fingerprint_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )

            // Selectable fingerprint hex — user can verify manually
            SelectionContainer {
                Text(
                    text = state.ed25519Fingerprint.chunked(8).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.cd_fingerprint_text, state.ed25519Fingerprint)
                    },
                )
            }

            Spacer(Modifier.weight(1f))

            if (state.isVerified) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = stringResource(R.string.cd_contact_verified),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(R.string.contact_verified),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    onClick = { /* trigger out-of-band fingerprint verification flow */ },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.cd_mark_verified_button)
                    },
                ) {
                    Text(stringResource(R.string.mark_verified))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
