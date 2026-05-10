package com.stagic.phantm.stego

import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.crypto.SecretKey
import com.stagic.phantm.err

/** iOS stub — full implementation requires UIKit/CoreImage interop via Swift bridge. */
internal class IosSteganographyManager(private val crypto: CryptoCore) : SteganographyManager {

    override fun embed(carrierBytes: ByteArray, plaintext: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        SteganographyError.Unknown(NotImplementedError("iOS steganography requires Swift/UIKit bridge")).err()

    override fun extract(stegoBytes: ByteArray, key: SecretKey): SteganographyResult<ByteArray> =
        SteganographyError.Unknown(NotImplementedError("iOS steganography requires Swift/UIKit bridge")).err()
}
