package io.github.alelk.pws.database

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT") // room compiler generates actual implementations
expect object PwsDatabaseConstructor : RoomDatabaseConstructor<PwsDatabase> {
  override fun initialize(): PwsDatabase
}