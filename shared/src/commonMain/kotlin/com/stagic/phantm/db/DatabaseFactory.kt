package com.stagic.phantm.db

import app.cash.sqldelight.db.SqlDriver
import com.stagic.phantm.crypto.CryptoCore
import com.stagic.phantm.groups.GroupDao
import com.stagic.phantm.identity.PlatformContext

/** Create a platform-specific [SqlDriver] backed by a SQLCipher-encrypted database. */
expect fun createSqlDriver(context: PlatformContext, crypto: CryptoCore): SqlDriver

/** Aggregate of all DAOs, tied to a single [PhantmDatabase] instance. */
class PhantmDaoFactory(driver: SqlDriver) {
    private val database = PhantmDatabase(driver)
    val messages: MessageDao = SqlDelightMessageDao(database)
    val contacts: ContactDao = SqlDelightContactDao(database)
    val sessions: SessionDao = SqlDelightSessionDao(database)
    val groups: GroupDao = SqlDelightGroupDao(database)
}
