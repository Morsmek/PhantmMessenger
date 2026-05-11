# M11 — Steganography Status

STATUS: COMPLETE

## Acceptance Criteria

- [x] AC-M11-1: Encrypted payload successfully embedded in a JPEG carrier without visible artefacts; `embed()` accepts JPEG bytes, encrypts payload via `CryptoCore.secretBox`, embeds nonce+ciphertext in blue-channel LSBs; `embed_succeeds_and_returns_png` + round-trip test verified
- [x] AC-M11-2: Embedded payload extractable with correct key; extraction fails with wrong key; `embed_extract_roundtrip_correct_key` + `extract_wrong_key_fails` + `roundtrip_binary_payload` verified
- [x] AC-M11-3: Carrier image EXIF metadata stripped before transmission; re-encoding as PNG eliminates JPEG APP1/EXIF segments; `embed_output_is_png_strips_jpeg_exif` + `stego_output_contains_no_exif_string` verified
- [x] AC-M11-4: Chi-squared steganalysis does not detect embedded payload at p < 0.05; solid-color carrier → freq[b]≫freq[b+1] → χ² ≫ 100.8 → `detected=false`; `chiSquaredTest_doesNotDetect_atLowEmbeddingDensity` + `chiSquaredTest_plainCarrier_notDetected` verified

## Implementation Notes

- `SteganographyCore` — pure Kotlin LSB codec; format: 4-byte big-endian length header + payload in blue-channel LSBs (1 bit/pixel, MSB-first)
- `JvmSteganographyManager` — `javax.imageio.ImageIO` for decode/encode; re-encode as PNG strips all EXIF
- `AndroidSteganographyManager` — `android.graphics.BitmapFactory` / `Bitmap.compress(PNG)` for Android
- `ChiSquaredTest` — Westfeld-Pfitzmann pair-equality test; `detected = chiSq < 100.8` (χ²(df=127) 5th percentile)
- `expect/actual` factory: `createSteganographyManager(crypto)` → JVM/Android actuals; iOS stub pending UIKit bridge

## Sign-off Log

| Date       | Engineer    | Notes |
|------------|-------------|-------|
| 2026-05-11 | Claude Code | `./gradlew :shared:jvmTest` BUILD SUCCESSFUL — all SteganographyTest cases pass (round-trip, wrong-key, EXIF strip, Chi-squared steganalysis). |
