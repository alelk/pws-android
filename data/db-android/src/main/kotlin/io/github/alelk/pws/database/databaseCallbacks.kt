package io.github.alelk.pws.database

import androidx.room.RoomDatabase.Callback
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val databaseCallbacks = object : Callback() {
  override fun onCreate(connection: SQLiteConnection) {
    super.onCreate(connection)
    runCatching {
      println("insert data to songs fts table")
      connection.execSQL(
        """
              INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, lyric)
              SELECT id, name, author, translator, composer, bibleref, lyric FROM songs
            """.trimIndent()
      )

      println("setup fts table triggers")
      connection.execSQL(
        """
              CREATE TRIGGER songs_fts_bu BEFORE UPDATE ON songs
              BEGIN
                  DELETE FROM songs_fts WHERE docid=old.rowid;
              END;
            """.trimIndent()
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_bd BEFORE DELETE ON songs
            BEGIN
                DELETE FROM songs_fts WHERE docid=old.rowid;
            END;
            """
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_au AFTER UPDATE ON songs
            BEGIN
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, lyric)
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.lyric);
            END;
            """
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_ai AFTER INSERT ON songs
            BEGIN
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, lyric)
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.lyric);
            END;
            """
      )
    }.onFailure { e -> System.err.println(e.message) }
  }
}