package com.stagic.phantm.stego

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.crypto.createCryptoCore
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SteganographyTest {

    private val crypto = createCryptoCore()
    private val manager = createSteganographyManager(crypto)

    // ── Carrier helpers ───────────────────────────────────────────────────────

    /** Solid-color JPEG. JPEG is lossy — blue values will vary slightly from [b]. */
    private fun solidJpeg(r: Int, g: Int, b: Int, width: Int = 300, height: Int = 300): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val color = Color(r, g, b).rgb
        for (y in 0 until height) for (x in 0 until width) img.setRGB(x, y, color)
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "JPEG", baos)
        return baos.toByteArray()
    }

    /** Solid-color PNG. Lossless — all pixels have exactly the given color. */
    private fun solidPng(r: Int, g: Int, b: Int, width: Int = 300, height: Int = 300): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val color = Color(r, g, b).rgb
        for (y in 0 until height) for (x in 0 until width) img.setRGB(x, y, color)
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "PNG", baos)
        return baos.toByteArray()
    }

    private fun randomKey(): SecretKey = SecretKey(crypto.randomBytes(32))

    // ── AC-M11-1: Embed in JPEG carrier ───────────────────────────────────────

    @Test
    fun embed_succeeds_and_returns_png() {
        val key = randomKey()
        val result = manager.embed(solidJpeg(100, 150, 80), "hello world".encodeToByteArray(), key)
        assertIs<PhantmResult.Ok<ByteArray>>(result)
        // PNG magic: 0x89 'P' 'N' 'G'
        val bytes = result.value
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(0x4E.toByte(), bytes[2])
        assertEquals(0x47.toByte(), bytes[3])
    }

    @Test
    fun embed_invalid_carrier_returns_error() {
        val result = manager.embed(byteArrayOf(1, 2, 3), "hi".encodeToByteArray(), randomKey())
        assertIs<PhantmResult.Err<SteganographyError>>(result)
        assertIs<SteganographyError.InvalidCarrier>(result.error)
    }

    @Test
    fun embed_carrier_too_small_returns_error() {
        // 10×10 = 100 pixels → capacity = 12 bytes; encrypted payload >> 12
        val tiny = solidJpeg(100, 100, 100, 10, 10)
        val bigPayload = ByteArray(500) { it.toByte() }
        val result = manager.embed(tiny, bigPayload, randomKey())
        assertIs<PhantmResult.Err<SteganographyError>>(result)
        assertIs<SteganographyError.InsufficientCapacity>(result.error)
    }

    // ── AC-M11-2: Round-trip with correct key; wrong key fails ────────────────

    @Test
    fun embed_extract_roundtrip_correct_key() {
        val key = randomKey()
        val plaintext = "Phantm steganography test payload — secret message".encodeToByteArray()
        val carrier = solidJpeg(120, 80, 200)
        val embedded = (manager.embed(carrier, plaintext, key) as PhantmResult.Ok).value
        val extracted = manager.extract(embedded, key)
        assertIs<PhantmResult.Ok<ByteArray>>(extracted)
        assertContentEquals(plaintext, extracted.value)
    }

    @Test
    fun extract_wrong_key_fails() {
        val key = randomKey()
        val wrongKey = randomKey()
        val carrier = solidJpeg(100, 100, 100)
        val embedded = (manager.embed(carrier, "secret".encodeToByteArray(), key) as PhantmResult.Ok).value
        val result = manager.extract(embedded, wrongKey)
        assertIs<PhantmResult.Err<SteganographyError>>(result)
        assertEquals(SteganographyError.DecryptionFailed, result.error)
    }

    @Test
    fun extract_invalid_stego_image_returns_error() {
        val result = manager.extract(byteArrayOf(0, 1, 2, 3), randomKey())
        assertIs<PhantmResult.Err<SteganographyError>>(result)
    }

    @Test
    fun roundtrip_binary_payload() {
        val key = randomKey()
        val payload = ByteArray(64) { (it * 7 + 13).toByte() }
        val carrier = solidJpeg(50, 100, 150)
        val embedded = (manager.embed(carrier, payload, key) as PhantmResult.Ok).value
        val extracted = (manager.extract(embedded, key) as PhantmResult.Ok).value
        assertContentEquals(payload, extracted)
    }

    // ── AC-M11-3: EXIF metadata stripped ──────────────────────────────────────

    @Test
    fun embed_output_is_png_strips_jpeg_exif() {
        val key = randomKey()
        val carrier = solidJpeg(200, 100, 50)
        val stegoBytes = (manager.embed(carrier, "test".encodeToByteArray(), key) as PhantmResult.Ok).value

        // Output must be PNG (JPEG EXIF cannot survive re-encoding as PNG)
        assertEquals(0x89.toByte(), stegoBytes[0], "Must be PNG magic byte 0")
        assertEquals(0x50.toByte(), stegoBytes[1], "Must be PNG magic byte 1")

        // No JPEG APP1 (EXIF) marker 0xFF 0xE1 in PNG output
        val hasJpegApp1 = stegoBytes.zipWithNext().any { (a, b) ->
            a == 0xFF.toByte() && b == 0xE1.toByte()
        }
        assertFalse(hasJpegApp1, "Stego PNG output must not contain JPEG APP1/EXIF marker")
    }

    @Test
    fun stego_output_contains_no_exif_string() {
        val key = randomKey()
        val carrier = solidJpeg(200, 200, 200)
        val stegoBytes = (manager.embed(carrier, "payload".encodeToByteArray(), key) as PhantmResult.Ok).value

        // Verify the output is a decodable image (not corrupt)
        assertTrue(ImageIO.read(ByteArrayInputStream(stegoBytes)) != null)

        // "Exif" ASCII signature must not appear in stego bytes
        val asString = stegoBytes.toString(Charsets.ISO_8859_1)
        assertFalse(asString.contains("Exif"), "EXIF signature must not be present in stego output")
    }

    // ── AC-M11-4: Chi-squared steganalysis does not detect embedded payload ────

    @Test
    fun chiSquaredTest_doesNotDetect_atLowEmbeddingDensity() {
        // Solid-color PNG carrier (lossless): all blue values identical →
        // pair (b/2) has n₀≫n₁=0 → huge χ² → not detected at low embedding density.
        val key = randomKey()
        val carrier = solidPng(100, 120, 80, 400, 400)   // 160K pixels; blue=80 (even value)
        val payload = ByteArray(100) { it.toByte() }      // ~824 bits → <1% density
        val stegoBytes = (manager.embed(carrier, payload, key) as PhantmResult.Ok).value

        val stegoImage = ImageIO.read(ByteArrayInputStream(stegoBytes))
        val width = stegoImage.width
        val height = stegoImage.height
        val pixels = stegoImage.getRGB(0, 0, width, height, null, 0, width)
        val blueValues = IntArray(pixels.size) { pixels[it] and 0xFF }

        val result = ChiSquaredTest.analyze(blueValues)
        assertFalse(
            result.detected,
            "Chi-squared test must NOT detect payload in solid-color carrier at low density (χ²=${result.chiSq})",
        )
        assertTrue(result.chiSq > 100.8, "χ² must exceed 100.8 (not-detected threshold); got ${result.chiSq}")
    }

    @Test
    fun chiSquaredTest_plainCarrier_notDetected() {
        // Build pixel array directly for exact control — all blue = 80
        val width = 200
        val height = 200
        val color = Color(100, 150, 80).rgb
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) for (x in 0 until width) img.setRGB(x, y, color)
        val pixels = img.getRGB(0, 0, width, height, null, 0, width)
        val blueValues = IntArray(pixels.size) { pixels[it] and 0xFF }

        val result = ChiSquaredTest.analyze(blueValues)
        assertFalse(result.detected, "Plain carrier must not be detected as stego (χ²=${result.chiSq})")
        assertTrue(result.chiSq > 100.8, "Plain carrier χ² must exceed threshold; got ${result.chiSq}")
    }

    // ── SteganographyCore unit tests ──────────────────────────────────────────

    @Test
    fun core_embedExtract_identity() {
        val payload = ByteArray(10) { (it + 1).toByte() }
        val pixels = IntArray(SteganographyCore.requiredPixels(payload.size)) { 0xFF_00_00_00.toInt() }
        val out = SteganographyCore.embed(pixels, payload)
        val extracted = SteganographyCore.extract(out)
        assertContentEquals(payload, extracted)
    }

    @Test
    fun core_requiredPixels_formula() {
        // (headerBytes=4 + payloadBytes) * bitsPerByte=8
        assertEquals(32, SteganographyCore.requiredPixels(0))
        assertEquals(40, SteganographyCore.requiredPixels(1))
        assertEquals(112, SteganographyCore.requiredPixels(10))
    }

    @Test
    fun core_embedExtract_empty_payload() {
        val pixels = IntArray(SteganographyCore.requiredPixels(0)) { 0xFF_AA_BB_CC.toInt() }
        val out = SteganographyCore.embed(pixels, ByteArray(0))
        assertContentEquals(ByteArray(0), SteganographyCore.extract(out))
    }
}
