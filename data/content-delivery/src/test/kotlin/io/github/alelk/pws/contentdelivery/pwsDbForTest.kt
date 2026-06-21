package io.github.alelk.pws.contentdelivery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alelk.pws.database.PwsDatabase

/** Creates a fresh in-memory [PwsDatabase] for a single test run. */
fun inMemoryPwsDb(): PwsDatabase {
  val context = ApplicationProvider.getApplicationContext<Context>()
  return Room.inMemoryDatabaseBuilder(context, PwsDatabase::class.java)
    .allowMainThreadQueries()
    .build()
}
