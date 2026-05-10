import SwiftUI

@main
struct PhantmApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(nil) // respects system dark/light mode (AC-M13-8)
        }
    }
}
