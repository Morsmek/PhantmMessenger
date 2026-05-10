package com.stagic.phantm.stego

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.EncryptedData
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.err
import com.stagic.phantm.ok
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * JVM/Android implementation of [SteganographyManager].
 *
 * Embed pipeline:
 *   1. Decode carrier with [ImageIO]
 *   2. Encrypt [plaintext] → nonce+ciphertext via [CryptoCore.secretBox]
 *   3. Embed payload in blue-channel LSBs via [SteganographyCore.embed]
 *   4. Re-encode as PNG → strips all JPEG EXIF metadata (AC-M11-3)
 *
 * Extract pipeline:
 *   1. Decode stego PNG with [ImageIO]
 *   2. Extract nonce+ciphertext from blue-channel LSBs
 *   3. Decrypt via [CryptoCore.secretBoxOpen]
 */
internal class JvmSteganographyManager(private val crypto: CryptoCore) : SteganographyManager {

    override fun embed(carrierBytes: ByteArray, plaintext: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        runCatching {
            val image = ImageIO.read(ByteArrayInputStream(carrierBytes))
                ?: return SteganographyError.InvalidCarrier("Cannot decode image").err()

            val encrypted = crypto.secretBox(plaintext, key)
            val payload = encrypted.nonce + encrypted.ciphertext

            val width = image.width
            val height = image.height
            val pixelCount = width * height

            if (pixelCount < SteganographyCore.requiredPixels(payload.size)) {
                return SteganographyError.InsufficientCapacity(
                    "Need ${SteganographyCore.requiredPixels(payload.size)} pixels, have $pixelCount"
                ).err()
            }

            val pixels = image.getRGB(0, 0, width, height, null, 0, width)
            val stegoPixels = SteganographyCore.embed(pixels, payload)

            val out = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            out.setRGB(0, 0, width, height, stegoPixels, 0, width)

            // Re-encode as PNG — drops all JPEG EXIF (AC-M11-3)
            val baos = ByteArrayOutputStream()
            ImageIO.write(out, "PNG", baos)
            baos.toByteArray().ok()
        }.getOrElse { SteganographyError.Unknown(it).err() }

    override fun extract(stegoBytes: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        runCatching {
            val image = ImageIO.read(ByteArrayInputStream(stegoBytes))
                ?: return SteganographyError.InvalidCarrier("Cannot decode stego image").err()

            val width = image.width
            val height = image.height
            val pixels = image.getRGB(0, 0, width, height, null, 0, width)

            val payload = SteganographyCore.extract(pixels)

            // Payload = nonce (24 bytes) + ciphertext
            val nonceSize = 24
            if (payload.size < nonceSize) {
                return SteganographyError.DecryptionFailed.err()
            }
            val nonce = payload.copyOf(nonceSize)
            val ciphertext = payload.copyOfRange(nonceSize, payload.size)

            when (val r = crypto.secretBoxOpen(EncryptedData(nonce, ciphertext), key)) {
                is com.stagic.phantm.PhantmResult.Ok -> r.value.ok()
                is com.stagic.phantm.PhantmResult.Err -> SteganographyError.DecryptionFailed.err()
            }
        }.getOrElse { SteganographyError.Unknown(it).err() }
}
