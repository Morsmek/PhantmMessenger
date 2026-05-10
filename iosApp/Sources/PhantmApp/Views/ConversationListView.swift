import SwiftUI

/// AC-M13-1 (iOS): Conversation list with mock data (AC-M13-6)
struct ConversationListView: View {
    private let conversations = Conversation.mockList

    var body: some View {
        List(conversations) { conversation in
            NavigationLink(destination: MessageThreadView(conversation: conversation)) {
                ConversationRowView(conversation: conversation)
            }
            .accessibilityLabel(
                String(format: NSLocalizedString("cd_conversation_row", comment: ""),
                       conversation.displayName, conversation.unreadCount)
            )
        }
        .navigationTitle(NSLocalizedString("screen_conversations", comment: ""))
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                NavigationLink(destination: SettingsView()) {
                    Image(systemName: "gearshape")
                        .accessibilityLabel(NSLocalizedString("screen_settings", comment: ""))
                }
            }
        }
    }
}

private struct ConversationRowView: View {
    let conversation: Conversation

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(name: conversation.displayName)
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(conversation.displayName)
                        .font(.headline)
                    Spacer()
                    Text(conversation.timestamp, style: .time)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    if conversation.isEncrypted {
                        Image(systemName: "lock.fill")
                            .font(.caption2)
                            .foregroundStyle(Color.accentColor) // AC-M13-5: no hardcoded color
                            .accessibilityLabel(NSLocalizedString("cd_encrypted", comment: ""))
                    }
                    Text(conversation.lastMessage)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            if conversation.unreadCount > 0 {
                Text("\(conversation.unreadCount)")
                    .font(.caption2.bold())
                    .foregroundStyle(.white)
                    .padding(6)
                    .background(Color.accentColor, in: Circle())
                    .accessibilityLabel(
                        String(format: NSLocalizedString("cd_unread_count", comment: ""),
                               conversation.unreadCount)
                    )
            }
        }
        .padding(.vertical, 4)
    }
}

private struct AvatarView: View {
    let name: String

    var body: some View {
        Text(String(name.prefix(1)).uppercased())
            .font(.headline)
            .foregroundStyle(Color.accentColor)
            .frame(width: 48, height: 48)
            .background(Color.accentColor.opacity(0.2), in: Circle())
            .accessibilityLabel(name)
    }
}
