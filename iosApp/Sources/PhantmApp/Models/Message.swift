import Foundation

struct Message: Identifiable {
    let id: String
    let senderName: String
    let text: String
    let timestamp: Date
    let isMine: Bool
    var isEncrypted: Bool = true
}

extension Message {
    static func mockThread(contactId: String) -> [Message] { [
        Message(id: "1", senderName: contactId, text: "Hello! Key exchange complete.",
                timestamp: Date(timeIntervalSince1970: 1_700_000_010), isMine: false),
        Message(id: "2", senderName: "me", text: "Perfect — all messages are end-to-end encrypted.",
                timestamp: Date(timeIntervalSince1970: 1_700_000_020), isMine: true),
        Message(id: "3", senderName: contactId, text: "Cover traffic is running too.",
                timestamp: Date(timeIntervalSince1970: 1_700_000_030), isMine: false),
        Message(id: "4", senderName: "me", text: "Great — transport is CONNECTED.",
                timestamp: Date(timeIntervalSince1970: 1_700_000_040), isMine: true),
    ] }
}
