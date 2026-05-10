package com.stagic.phantm.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.identity.PlatformContext

/**
 * JVM test driver: unencrypted in-memory SQLite via JDBC.
 * SQLCipher is Android-only; tests exercise the schema and DAO logic
 * without native encryption, relying on M01 tests for crypto correctness.
 */
actual fun createSqlDriver(context: PlatformContext, crypto: CryptoCore): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PhantmDatabase.Schema.create(driver)
    return driver
}
