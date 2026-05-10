package com.stagic.phantm.integration

import com.stagic.phantm.PhantmResult
import com.stagic.phantm.crypto.createCryptoCore
import com.stagic.phantm.identity.IdentityError
import com.stagic.phantm.identity.PlatformContext
import com.stagic.phantm.identity.createIdentityManager
import com.stagic.phantm.panic.createPanicManager
import com.stagic.phantm.protocol.MessagePayload
import com.stagic.phantm.protocol.MessageType
import com.stagic.phantm.protocol.createMessageProtocol
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * AC-M14-3: Panic wipe integration test — after wipe, no messages recoverable.
 *
 * Exercises M12 (PanicManager) together with M02 (IdentityManager) and M07 (MessageProtocol):
 *   Alice creates identity → encrypts a message for Bob →
 *   panic wipe triggered → identity is destroyed →
 *   all subsequent key-load attempts fail → stored ciphertext unrecoverable.
 */
class PanicWipeIntegrationTest {

    @Test
    fun afterPanicWipe_identityIsDestroyed() = runTest {
        val crypto = createCryptoCore()
        val identityManager = createIdentityManager(crypto, PlatformContext())
        val panicManager = createPanicManager(identityManager, PlatformContext())

        // Establish identity
        val createResult = identityManager.createIdentity()
        assertIs<PhantmResult.Ok<*>>(createResult)
        assertTrue(identityManager.hasIdentity(), "Identity must exist before wipe")

        // Trigger panic wipe
        val wipeResult = panicManager.triggerPanicWipe()
        assertIs<PhantmResult.Ok<Unit>>(wipeResult)

        // Identity must be gone
        assertFalse(identityManager.hasIdentity(), "Identity must not exist after wipe")
        assertIs<PhantmResult.Err<IdentityError>>(
            identityManager.loadIdentity(),
            "loadIdentity must fail after wipe",
        )
    }

    @Test
    fun afterPanicWipe_privateKeysIrrecoverable() = runTest {
        val crypto = createCryptoCore()
        val identityManager = createIdentityManager(crypto, PlatformContext())
        val panicManager = createPanicManager(identityManager, PlatformContext())

        identityManager.createIdentity()
        val beforePrivKeys = identityManager.loadPrivateKeys()
        assertIs<PhantmResult.Ok<*>>(beforePrivKeys)

        panicManager.triggerPanicWipe()

        // Private keys must not be loadable after wipe
        val afterPrivKeys = identityManager.loadPrivateKeys()
        assertIs<PhantmResult.Err<IdentityError>>(afterPrivKeys, "loadPrivateKeys must fail after wipe")
        assertIs<IdentityError.NoIdentityFound>(afterPrivKeys.error)
    }

    @Test
    fun afterPanicWipe_storedCiphertextUnrecoverable() = runTest {
        val aliceCrypto = createCryptoCore()
        val bobCrypto = createCryptoCore()

        val aliceManager = createIdentityManager(aliceCrypto, PlatformContext())
        val bobManager = createIdentityManager(bobCrypto, PlatformContext())
        val aliceId = (aliceManager.createIdentity() as PhantmResult.Ok).value
        val bobId = (bobManager.createIdentity() as PhantmResult.Ok).value
        val alicePrivKeys = (aliceManager.loadPrivateKeys() as PhantmResult.Ok).value

        // Alice encrypts a sensitive message for Bob
        val aliceProtocol = createMessageProtocol(aliceCrypto)
        val sensitiveText = "Highly sensitive message — must be irrecoverable after wipe"
        val envelopeBytes = (aliceProtocol.encrypt(
            payload = MessagePayload(type = MessageType.TEXT, text = sensitiveText, timestampMs = 1L),
            senderId = aliceId.id,
            recipientId = bobId.id,
            senderEd25519PrivKey = alicePrivKeys.ed25519PrivKey,
            recipientX25519Pub = bobId.x25519PublicKey,
            recipientMlKemPub = bobId.mlKem768PublicKey,
        ) as PhantmResult.Ok).value
        alicePrivKeys.zero()

        // Panic wipe on Bob's device
        val bobPanic = createPanicManager(bobManager, PlatformContext())
        bobPanic.triggerPanicWipe()

        // After wipe, Bob cannot load private keys to decrypt
        val bobPrivKeysAfterWipe = bobManager.loadPrivateKeys()
        assertIs<PhantmResult.Err<IdentityError>>(bobPrivKeysAfterWipe)

        // Attempting to decrypt with random keys fails (wrong key → MAC failure)
        val bobProtocol = createMessageProtocol(bobCrypto)
        val randomX25519 = bobCrypto.generateX25519KeyPair()
        val randomMlKem = bobCrypto.generateMlKem768KeyPair()
        val decryptWithWrongKeys = bobProtocol.decrypt(
            envelopeBytes = envelopeBytes,
            recipientId = bobId.id,
            recipientX25519PrivKey = randomX25519.privateKey,
            recipientMlKemPrivKey = randomMlKem.privateKey,
            senderEd25519Pub = aliceId.ed25519PublicKey,
        )
        assertIs<PhantmResult.Err<*>>(decryptWithWrongKeys, "Decryption with wrong keys must fail")
    }

    @Test
    fun decoyModeActivation_doesNotWipeIdentity() = runTest {
        val crypto = createCryptoCore()
        val identityManager = createIdentityManager(crypto, PlatformContext())
        val panicManager = createPanicManager(identityManager, PlatformContext())

        identityManager.createIdentity()
        panicManager.activateDecoyMode()

        assertTrue(panicManager.isDecoyModeActive, "Decoy mode must be active")
        assertTrue(identityManager.hasIdentity(), "Decoy mode must not destroy identity")
    }
}
