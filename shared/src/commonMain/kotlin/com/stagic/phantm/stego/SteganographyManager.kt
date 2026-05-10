package com.stagic.phantm.stego

import com.stagic.phantm.crypto.SecretKey

/**
 * Embeds and extracts encrypted payloads inside image carriers using LSB steganography.
 *
 * Embed: encrypts [plaintext] with [key], embeds nonce+ciphertext in carrier image blue-channel LSBs,
 * re-encodes as PNG (stripping EXIF metadata).
 * Extract: reads embedded bytes from stego image, decrypts with [key].
 */
interface SteganographyManager {
    /** Embeds [plaintext] into [carrierBytes] (JPEG or PNG). Returns PNG-encoded stego image. */
    fun embed(carrierBytes: ByteArray, plaintext: ByteArray, key: SecretKey): SteganographyResult<ByteArray>

    /** Extracts and decrypts payload from [stegoBytes] produced by [embed]. */
    fun extract(stegoBytes: ByteArray, key: SecretKey): SteganographyResult<ByteArray>
}
