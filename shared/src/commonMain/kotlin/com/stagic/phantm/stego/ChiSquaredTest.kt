package com.stagic.phantm.stego

/**
 * Westfeld-Pfitzmann Chi-squared steganalysis on blue-channel byte values.
 *
 * Tests pair equality across value pairs (0,1),(2,3),...,(254,255).
 * In natural images, adjacent pairs are roughly equal; LSB steganography forces equality,
 * driving χ² → 0. Detection threshold: χ² < 100.8 (5th percentile of χ²(df=127)).
 *
 * Reference: Westfeld & Pfitzmann, "Attacks on Steganographic Systems" (2000).
 */
object ChiSquaredTest {

    /** χ²(df=127) 5th-percentile lower-tail critical value. Values below this indicate detection. */
    private const val CHI2_LOWER_5PCT_DF127 = 100.8

    data class ChiSquaredResult(
        val chiSq: Double,
        /** true = payload statistically detectable (p < 0.05 lower-tail) */
        val detected: Boolean,
    )

    /**
     * Runs steganalysis on [blueValues] (one byte per pixel, blue channel 0–255).
     * Returns [ChiSquaredResult.detected] = true if embedding is statistically detectable.
     */
    fun analyze(blueValues: IntArray): ChiSquaredResult {
        val freq = IntArray(256)
        for (v in blueValues) freq[v and 0xFF]++

        var chiSq = 0.0
        var df = 0
        for (k in 0 until 128) {
            val n0 = freq[2 * k].toDouble()
            val n1 = freq[2 * k + 1].toDouble()
            val total = n0 + n1
            if (total > 0.0) {
                val expected = total / 2.0
                chiSq += (n0 - expected) * (n0 - expected) / expected +
                    (n1 - expected) * (n1 - expected) / expected
                df++
            }
        }

        return ChiSquaredResult(
            chiSq = chiSq,
            detected = chiSq < CHI2_LOWER_5PCT_DF127,
        )
    }
}
