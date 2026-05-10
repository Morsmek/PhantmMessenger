package com.stagic.phantm.cover

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Kolmogorov-Smirnov test utilities for AC-M06-1.
 *
 * Tests whether an observed sample could be drawn from a reference distribution
 * by comparing the empirical CDF against the expected CDF.
 */

/**
 * Compute the KS statistic D for [samples] against a continuous reference CDF [expectedCdf].
 * D = sup_x |F_n(x) - F(x)|
 */
fun ksStatistic(samples: List<Double>, expectedCdf: (Double) -> Double): Double {
    val sorted = samples.sorted()
    val n = sorted.size.toDouble()
    var maxD = 0.0
    sorted.forEachIndexed { i, x ->
        val expected = expectedCdf(x)
        maxD = maxOf(maxD, abs((i + 1) / n - expected), abs(i / n - expected))
    }
    return maxD
}

/**
 * KS critical value at significance level [alpha] for sample size [n]
 * using the asymptotic approximation c(α) / sqrt(n).
 *
 * Standard c values: α=0.10 → 1.224, α=0.05 → 1.358, α=0.01 → 1.628.
 */
fun ksCriticalValue(n: Int, alpha: Double = 0.05): Double {
    val c = when {
        alpha <= 0.01 -> 1.628
        alpha <= 0.05 -> 1.358
        alpha <= 0.10 -> 1.224
        else -> 1.073
    }
    return c / sqrt(n.toDouble())
}

/**
 * Returns `true` if [samples] are consistent with Uniform([lo], [hi]) at the
 * given [alpha] significance level (two-sided KS test, fails to reject H₀).
 */
fun passesUniformKsTest(
    samples: List<Double>,
    lo: Double,
    hi: Double,
    alpha: Double = 0.05,
): Boolean {
    val range = hi - lo
    val cdf = { x: Double -> ((x - lo) / range).coerceIn(0.0, 1.0) }
    val d = ksStatistic(samples, cdf)
    return d < ksCriticalValue(samples.size, alpha)
}
