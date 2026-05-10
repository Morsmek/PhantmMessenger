package com.stagic.phantm.stego

import com.stagic.phantm.crypto.CryptoCore

actual fun createSteganographyManager(crypto: CryptoCore): SteganographyManager =
    JvmSteganographyManager(crypto)
