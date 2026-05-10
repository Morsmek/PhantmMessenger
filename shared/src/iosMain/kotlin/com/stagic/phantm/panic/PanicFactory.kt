package com.stagic.phantm.panic

import com.stagic.phantm.identity.IdentityManager
import com.stagic.phantm.identity.PlatformContext

actual fun createPanicManager(identityManager: IdentityManager, context: PlatformContext): PanicManager =
    throw NotImplementedError("PanicManager is not yet implemented for iOS")
