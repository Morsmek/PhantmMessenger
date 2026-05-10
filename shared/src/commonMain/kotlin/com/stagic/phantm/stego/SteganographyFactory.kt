package com.stagic.phantm.stego

import com.stagic.phantm.crypto.CryptoCore

expect fun createSteganographyManager(crypto: CryptoCore): SteganographyManager
