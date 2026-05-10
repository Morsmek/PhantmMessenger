package com.stagic.phantm.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stagic.phantm.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var coverTrafficEnabled by remember { mutableStateOf(true) }
    var decoyModeEnabled by remember { mutableStateOf(false) }
    var meshEnabled by remember { mutableStateOf(true) }

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
                title = { Text(stringResource(R.string.screen_settings)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SettingsSectionHeader(stringResource(R.string.settings_section_transport))
            SettingsToggleRow(
                label = stringResource(R.string.setting_cover_traffic),
                description = stringResource(R.string.setting_cover_traffic_desc),
                checked = coverTrafficEnabled,
                onCheckedChange = { coverTrafficEnabled = it },
            )
            SettingsToggleRow(
                label = stringResource(R.string.setting_mesh_networking),
                description = stringResource(R.string.setting_mesh_networking_desc),
                checked = meshEnabled,
                onCheckedChange = { meshEnabled = it },
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SettingsSectionHeader(stringResource(R.string.settings_section_security))
            SettingsToggleRow(
                label = stringResource(R.string.setting_decoy_mode),
                description = stringResource(R.string.setting_decoy_mode_desc),
                checked = decoyModeEnabled,
                onCheckedChange = { decoyModeEnabled = it },
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics {
                contentDescription = "$label: ${if (checked) "on" else "off"}"
            },
        )
    }
}
