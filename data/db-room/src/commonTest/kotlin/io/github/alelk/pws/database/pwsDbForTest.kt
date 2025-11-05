package io.github.alelk.pws.database

expect fun pwsDbForTest(inMemory: Boolean = true, name: String = "test-data/pws.db"): PwsDatabase