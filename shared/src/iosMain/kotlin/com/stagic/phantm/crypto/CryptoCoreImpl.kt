package com.stagic.phantm.crypto

/**
 * iOS crypto implementation placeholder.
 * Full implementation requires libsodium.xcframework + liboqs cinterop bindings.
 * Wire up before building M02.
 */
internal class CryptoCoreImpl : CryptoCore {
    override fun generateX25519KeyPair(): KeyPair = notImplemented()
    override fun generateEd25519KeyPair(): KeyPair = notImplemented()
    override fun generateMlKem768KeyPair(): KeyPair = notImplemented()
    override fun hybridEncapsulate(recipientX25519Pub: PublicKey, recipientMlKemPub: PublicKey): HybridKemResult =
        notImplemented()
    override fun hybridDecapsulate(
        ciphertext: HybridKemCiphertext,
        x25519PrivKey: PrivateKey,
        mlKemPrivKey: PrivateKey,
    ): CryptoResult<SharedSecret> = notImplemented()
    override fun secretBox(plaintext: ByteArray, key: SecretKey): EncryptedData = notImplemented()
    override fun secretBoxOpen(encrypted: EncryptedData, key: SecretKey): CryptoResult<ByteArray> = notImplemented()
    override fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLen: Int): SecretKey =
        notImplemented()
    override fun sign(message: ByteArray, signingKey: PrivateKey): Signature = notImplemented()
    override fun verify(message: ByteArray, signature: Signature, verifyKey: PublicKey): CryptoResult<Unit> =
        notImplemented()
    override fun randomBytes(size: Int): ByteArray = notImplemented()

    private fun notImplemented(): Nothing =
        throw NotImplementedError("M01 iOS native bindings not yet implemented — requires libsodium cinterop")
}
