package com.stagic.phantm.cover

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.transport.ConnectionState
import com.stagic.phantm.transport.TransportClient
import com.stagic.phantm.transport.TransportEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class CoverTrafficManagerImpl(
    private val crypto: CryptoCore,
    private val random: Random = Random,
) : CoverTrafficManager {

    private var job: Job? = null

    override var isRunning: Boolean = false
        private set

    override fun start(transport: TransportClient, config: CoverTrafficConfig) {
        stop()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        job = scope.launch {
            while (isActive) {
                delay(config.intervalMs)
                if (transport.connectionState.value == ConnectionState.CONNECTED) {
                    transport.send(generateCoverEnvelope(config))
                }
            }
        }
        isRunning = true
    }

    override fun stop() {
        job?.cancel()
        job = null
        isRunning = false
    }

    /**
     * Generate a cover envelope with:
     * - Random payload size in [config.minPayloadBytes, config.maxPayloadBytes]
     * - Payload = secretBox(randomBytes, ephemeralKey) — encrypted, no user data
     * - recipientId = COVER_RECIPIENT_ID so the relay discards it silently
     */
    internal fun generateCoverEnvelope(config: CoverTrafficConfig): TransportEnvelope {
        val payloadSize = random.nextInt(config.minPayloadBytes, config.maxPayloadBytes + 1)
        val plaintext = crypto.randomBytes(payloadSize)
        val ephemeralKey = SecretKey(crypto.randomBytes(32))
        val encrypted = crypto.secretBox(plaintext, ephemeralKey)
        val envelopeBytes = encrypted.nonce + encrypted.ciphertext
        return TransportEnvelope(recipientId = COVER_RECIPIENT_ID, bytes = envelopeBytes)
    }
}

fun createCoverTrafficManager(crypto: CryptoCore): CoverTrafficManager =
    CoverTrafficManagerImpl(crypto)
