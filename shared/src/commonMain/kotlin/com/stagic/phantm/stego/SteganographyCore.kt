package com.stagic.phantm.stego

/**
 * Platform-agnostic LSB steganography core operating on ARGB pixel arrays.
 *
 * Format embedded in blue-channel LSBs (MSB first):
 *   [4 bytes big-endian payload length][payload bytes]
 *
 * Capacity = (pixelCount / 8) bytes (1 bit per pixel in blue channel LSB).
 */
internal object SteganographyCore {

    private const val LENGTH_HEADER_BYTES = 4
    private const val BITS_PER_PIXEL = 1

    /** Returns pixel count required to embed [payloadBytes] bytes. */
    fun requiredPixels(payloadBytes: Int): Int = (LENGTH_HEADER_BYTES + payloadBytes) * 8

    /**
     * Embeds [payload] into [pixels] (ARGB, in-place copy returned).
     * Throws [IllegalArgumentException] if carrier capacity is insufficient.
     */
    fun embed(pixels: IntArray, payload: ByteArray): IntArray {
        val totalBytes = LENGTH_HEADER_BYTES + payload.size
        require(pixels.size >= totalBytes * 8) {
            "Carrier too small: need ${totalBytes * 8} pixels, have ${pixels.size}"
        }
        val out = pixels.copyOf()
        val allBytes = ByteArray(totalBytes)
        // 4-byte big-endian length header
        val len = payload.size
        allBytes[0] = (len shr 24).toByte()
        allBytes[1] = (len shr 16).toByte()
        allBytes[2] = (len shr 8).toByte()
        allBytes[3] = len.toByte()
        payload.copyInto(allBytes, LENGTH_HEADER_BYTES)

        var pixelIdx = 0
        for (byte in allBytes) {
            for (bit in 7 downTo 0) {
                val b = (byte.toInt() ushr bit) and 1
                // Replace blue channel LSB; preserve A, R, G
                out[pixelIdx] = (out[pixelIdx] and 0xFFFFFFFE.toInt()) or b
                pixelIdx++
            }
        }
        return out
    }

    /**
     * Extracts the embedded payload from [pixels].
     * Throws [IllegalStateException] if pixel count is insufficient to read the header.
     */
    fun extract(pixels: IntArray): ByteArray {
        check(pixels.size >= LENGTH_HEADER_BYTES * 8) { "Image too small to contain stego header" }

        fun readByte(startPixel: Int): Byte {
            var value = 0
            for (i in 0 until 8) {
                value = (value shl 1) or (pixels[startPixel + i] and 1)
            }
            return value.toByte()
        }

        // Read 4-byte length header
        var len = 0
        for (i in 0 until 4) {
            len = (len shl 8) or (readByte(i * 8).toInt() and 0xFF)
        }
        check(pixels.size >= (LENGTH_HEADER_BYTES + len) * 8) {
            "Image too small to contain claimed payload of $len bytes"
        }

        return ByteArray(len) { i -> readByte((LENGTH_HEADER_BYTES + i) * 8) }
    }
}
