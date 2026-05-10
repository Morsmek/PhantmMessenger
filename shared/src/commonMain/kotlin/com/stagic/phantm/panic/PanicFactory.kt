package com.stagic.phantm.panic

import com.stagic.phantm.identity.IdentityManager
import com.stagic.phantm.identity.PlatformContext

/** Returns the platform-specific [PanicManager]. */
expect fun createPanicManager(identityManager: IdentityManager, context: PlatformContext): PanicManager
