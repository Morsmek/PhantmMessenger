import Foundation

struct Conversation: Identifiable {
    let id: String
    let displayName: String
    let lastMessage: String
    let timestamp: Date
    var unreadCount: Int
    var isEncrypted: Bool = true
}

// MARK: - Mock data

extension Conversation {
    static let mockList: [Conversation] = [
        Conversation(id: "alice", displayName: "Alice", lastMessage: "See you at the relay 🔐",
                     timestamp: Date(timeIntervalSince1970: 1_700_000_100), unreadCount: 2),
        Conversation(id: "bob", displayName: "Bob", lastMessage: "Key exchange complete",
                     timestamp: Date(timeIntervalSince1970: 1_700_000_050), unreadCount: 0),
        Conversation(id: "carol", displayName: "Carol", lastMessage: "Cover traffic is running",
                     timestamp: Date(timeIntervalSince1970: 1_699_999_900), unreadCount: 1),
    ]
}
