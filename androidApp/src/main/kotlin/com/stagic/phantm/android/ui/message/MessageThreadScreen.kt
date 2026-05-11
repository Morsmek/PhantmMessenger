package com.stagic.phantm.android.ui.message

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stagic.phantm.android.ui.components.PhantmAvatar
import com.stagic.phantm.android.ui.components.VerifiedBadge
import com.stagic.phantm.android.ui.theme.PhantmAccent
import com.stagic.phantm.android.ui.theme.PhantmBgBase
import com.stagic.phantm.android.ui.theme.PhantmBgElevated
import com.stagic.phantm.android.ui.theme.PhantmBgSent
import com.stagic.phantm.android.ui.theme.PhantmDivider
import com.stagic.phantm.android.ui.theme.PhantmText
import com.stagic.phantm.android.ui.theme.PhantmText2
import com.stagic.phantm.android.ui.theme.PhantmTextDisabled

@Composable
fun MessageThreadScreen(
    contactId: String,
    onBack: () -> Unit,
    viewModel: MessageThreadViewModel = viewModel(factory = MessageThreadViewModelFactory(contactId)),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    Scaffold(containerColor = PhantmBgBase) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Thread header
            ThreadHeader(
                contactName = state.contactName,
                isVerified = state.isEndToEndVerified,
                onBack = onBack,
            )

            Box(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(PhantmDivider),
            )

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    DateSeparator("Yesterday")
                }
                item {
                    SystemMessage("Disappearing messages set to 24 hours")
                }
                items(state.messages, key = { it.messageId }) { message ->
                    MessageBubble(message = message)
                }
            }

            // Composer
            MessageComposer(onSend = {})
        }
    }
}

@Composable
private fun ThreadHeader(
    contactName: String,
    isVerified: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
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

        PhantmAvatar(name = contactName, size = 40.dp, online = true)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = contactName,
                    color = PhantmText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("🔒", fontSize = 11.sp)
                if (isVerified) VerifiedBadge()
            }
            Text(
                text = "7f3a9b…d2c1",
                color = PhantmText2,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text("⋮", color = PhantmText, fontSize = 20.sp)
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PhantmBgElevated)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(label, color = PhantmText2, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
        }
    }
}

@Composable
private fun SystemMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = PhantmText2, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun MessageBubble(message: MessageUiState) {
    val isOut = message.isMine

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = if (isOut) 20.dp else 4.dp,
                        bottomEnd = if (isOut) 4.dp else 20.dp,
                    ),
                )
                .background(if (isOut) PhantmBgSent else PhantmBgElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column {
                Text(
                    text = message.text,
                    color = PhantmText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "14:30",
                        color = PhantmText2,
                        fontSize = 11.sp,
                    )
                    if (isOut) {
                        Text("✓✓", color = PhantmAccent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val hasText = text.trim().isNotEmpty()

    Column {
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(PhantmDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PhantmBgBase)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Attach button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = PhantmText2, fontSize = 22.sp, fontWeight = FontWeight.Light)
            }

            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PhantmBgElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text("Message…", color = PhantmTextDisabled, fontSize = 15.sp)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = PhantmText, fontSize = 15.sp),
                    cursorBrush = SolidColor(PhantmAccent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Send / Mic button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (hasText) PhantmAccent else Color.Transparent)
                    .clickable {
                        if (hasText) {
                            onSend(text)
                            text = ""
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasText) "▶" else "🎤",
                    fontSize = if (hasText) 16.sp else 18.sp,
                    color = if (hasText) Color.White else PhantmText2,
                )
            }
        }
    }
}
