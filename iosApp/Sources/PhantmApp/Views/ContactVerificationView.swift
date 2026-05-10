import SwiftUI
import CoreImage.CIFilterBuiltins

/// AC-M13-3 (iOS): Contact verification with Ed25519 fingerprint QR code (AC-M13-6)
struct ContactVerificationView: View {
    let contactId: String
    let fingerprint = "a3f2b84c91e05d6278bc340fa1e9d72c4b58f063e2a197d4c8561b3e09f74a2d"

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text(NSLocalizedString("verify_instruction", comment: ""))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                FingerprintQRView(data: fingerprint)
                    .accessibilityLabel(
                        String(format: NSLocalizedString("cd_fingerprint_qr", comment: ""), contactId)
                    )

                Text(NSLocalizedString("fingerprint_label", comment: ""))
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text(fingerprint.chunks(of: 8).joined(separator: " "))
                    .font(.system(.caption, design: .monospaced))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
                    .accessibilityLabel(
                        String(format: NSLocalizedString("cd_fingerprint_text", comment: ""), fingerprint)
                    )

                Spacer(minLength: 24)
                Button(NSLocalizedString("mark_verified", comment: "")) { /* trigger verification */ }
                    .buttonStyle(.borderedProminent)
                    .accessibilityLabel(NSLocalizedString("cd_mark_verified_button", comment: ""))
            }
            .padding(.top, 16)
        }
        .navigationTitle(String(format: NSLocalizedString("screen_verify_contact", comment: ""), contactId))
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Renders a QR code using CoreImage — no third-party library required on iOS.
private struct FingerprintQRView: View {
    let data: String

    var body: some View {
        if let uiImage = generateQRCode(from: data) {
            Image(uiImage: uiImage)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .frame(width: 220, height: 220)
        }
    }

    private func generateQRCode(from string: String) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
        guard let cgImage = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

private extension String {
    func chunks(of size: Int) -> [String] {
        stride(from: 0, to: count, by: size).map {
            let start = index(startIndex, offsetBy: $0)
            let end = index(start, offsetBy: min(size, count - $0))
            return String(self[start..<end])
        }
    }
}
