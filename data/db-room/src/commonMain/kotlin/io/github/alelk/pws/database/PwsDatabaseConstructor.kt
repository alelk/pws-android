package io.github.alelk.pws.database

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING") // room compiler generates actual implementations
expect object PwsDatabaseConstructor : RoomDatabaseConstructor<PwsDatabase> {
  override fun initialize(): PwsDatabase
}