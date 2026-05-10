package com.stagic.phantm.db

import app.cash.sqldelight.db.SqlDriver
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.identity.PlatformContext

actual fun createSqlDriver(context: PlatformContext, crypto: CryptoCore): SqlDriver {
    throw NotImplementedError("M03 iOS driver requires NativeSqliteDriver + SQLCipher — not yet wired")
}
