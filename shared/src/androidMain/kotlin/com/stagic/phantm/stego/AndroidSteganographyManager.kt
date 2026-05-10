package com.stagic.phantm.stego

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.EncryptedData
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.err
import com.stagic.phantm.ok
import java.io.ByteArrayOutputStream

/**
 * Android implementation of [SteganographyManager] using [BitmapFactory] and [Bitmap].
 * Re-encodes as PNG to strip JPEG EXIF metadata (AC-M11-3).
 */
internal class AndroidSteganographyManager(private val crypto: CryptoCore) : SteganographyManager {

    override fun embed(carrierBytes: ByteArray, plaintext: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        runCatching {
            val bitmap = BitmapFactory.decodeByteArray(carrierBytes, 0, carrierBytes.size)
                ?: return SteganographyError.InvalidCarrier("Cannot decode image").err()

            val encrypted = crypto.secretBox(plaintext, key)
            val payload = encrypted.nonce + encrypted.ciphertext

            val width = bitmap.width
            val height = bitmap.height
            val pixelCount = width * height

            if (pixelCount < SteganographyCore.requiredPixels(payload.size)) {
                return SteganographyError.InsufficientCapacity(
                    "Need ${SteganographyCore.requiredPixels(payload.size)} pixels, have $pixelCount"
                ).err()
            }

            val pixels = IntArray(pixelCount)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val stegoPixels = SteganographyCore.embed(pixels, payload)

            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            out.setPixels(stegoPixels, 0, width, 0, 0, width, height)

            val baos = ByteArrayOutputStream()
            out.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray().ok()
        }.getOrElse { SteganographyError.Unknown(it).err() }

    override fun extract(stegoBytes: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        runCatching {
            val bitmap = BitmapFactory.decodeByteArray(stegoBytes, 0, stegoBytes.size)
                ?: return SteganographyError.InvalidCarrier("Cannot decode stego image").err()

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val payload = SteganographyCore.extract(pixels)

            val nonceSize = 24
            if (payload.size < nonceSize) return SteganographyError.DecryptionFailed.err()
            val nonce = payload.copyOf(nonceSize)
            val ciphertext = payload.copyOfRange(nonceSize, payload.size)

            when (val result = crypto.secretBoxOpen(EncryptedData(nonce, ciphertext), key)) {
                is com.stagic.phantm.PhantmResult.Ok -> result.value.ok()
                is com.stagic.phantm.PhantmResult.Err -> SteganographyError.DecryptionFailed.err()
            }
        }.getOrElse { SteganographyError.Unknown(it).err() }
}
