import SwiftUI

/// AC-M13-2 (iOS): Message thread with E2E indicator (AC-M13-6)
struct MessageThreadView: View {
    let conversation: Conversation
    private var messages: [Message] { Message.mockThread(contactId: conversation.id) }

    var body: some View {
        VStack(spacing: 0) {
            E2EStatusBanner(verified: true)
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 6) {
                        ForEach(messages) { message in
                            MessageBubbleView(message: message)
                                .id(message.id)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                }
                .onAppear {
                    if let last = messages.last {
                        proxy.scrollTo(last.id, anchor: .bottom)
                    }
                }
            }
        }
        .navigationTitle(conversation.displayName)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct E2EStatusBanner: View {
    let verified: Bool

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: verified ? "lock.fill" : "lock.open")
                .font(.caption2)
            Text(NSLocalizedString(verified ? "e2e_verified" : "e2e_unverified", comment: ""))
                .font(.caption)
        }
        .foregroundStyle(verified ? Color.accentColor : Color(uiColor: .systemRed))
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity)
        .background(Color(uiColor: .systemBackground).opacity(0.9))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(NSLocalizedString(verified ? "e2e_verified" : "e2e_unverified", comment: ""))
    }
}

private struct MessageBubbleView: View {
    let message: Message

    var body: some View {
        HStack {
            if message.isMine { Spacer(minLength: 60) }
            VStack(alignment: message.isMine ? .trailing : .leading, spacing: 2) {
                Text(message.text)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(
                        message.isMine ? Color.accentColor : Color(uiColor: .secondarySystemBackground),
                        in: RoundedRectangle(cornerRadius: 16)
                    )
                    .foregroundStyle(message.isMine ? Color.white : Color.primary)
                Text(message.timestamp, style: .time)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel(
                String(format: NSLocalizedString(message.isMine ? "cd_my_message" : "cd_their_message", comment: ""),
                       message.senderName, message.text)
            )
            if !message.isMine { Spacer(minLength: 60) }
        }
    }
}
