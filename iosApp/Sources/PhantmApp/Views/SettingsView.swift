import SwiftUI

/// Settings screen — iOS equivalent of AC-M13 Settings (AC-M13-6)
struct SettingsView: View {
    @State private var coverTrafficEnabled = true
    @State private var meshNetworkingEnabled = true
    @State private var decoyModeEnabled = false

    var body: some View {
        Form {
            Section(NSLocalizedString("settings_section_transport", comment: "")) {
                Toggle(isOn: $coverTrafficEnabled) {
                    LabeledContent(
                        NSLocalizedString("setting_cover_traffic", comment: ""),
                        value: NSLocalizedString("setting_cover_traffic_desc", comment: "")
                    )
                }
                .accessibilityLabel("\(NSLocalizedString("setting_cover_traffic", comment: "")): \(coverTrafficEnabled ? "on" : "off")")
                Toggle(isOn: $meshNetworkingEnabled) {
                    LabeledContent(
                        NSLocalizedString("setting_mesh_networking", comment: ""),
                        value: NSLocalizedString("setting_mesh_networking_desc", comment: "")
                    )
                }
                .accessibilityLabel("\(NSLocalizedString("setting_mesh_networking", comment: "")): \(meshNetworkingEnabled ? "on" : "off")")
            }
            Section(NSLocalizedString("settings_section_security", comment: "")) {
                Toggle(isOn: $decoyModeEnabled) {
                    LabeledContent(
                        NSLocalizedString("setting_decoy_mode", comment: ""),
                        value: NSLocalizedString("setting_decoy_mode_desc", comment: "")
                    )
                }
                .accessibilityLabel("\(NSLocalizedString("setting_decoy_mode", comment: "")): \(decoyModeEnabled ? "on" : "off")")
            }
        }
        .navigationTitle(NSLocalizedString("screen_settings", comment: ""))
    }
}
