package io.github.alelk.pws.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.alelk.pws.database.helper.PwsDatabaseHelper

object PwsDatabaseProvider {
  @Volatile
  private var INSTANCE: PwsDatabase? = null

  fun getDatabase(context: Context): PwsDatabase {
    return INSTANCE ?: synchronized(this) {
      PwsDatabaseHelper(context)
      val instance =
        Room.databaseBuilder(
          context.applicationContext,
          PwsDatabase::class.java,
          PwsDatabaseHelper.DATABASE_NAME
        )
          .addCallback(callback = databaseCallbacks)
          .build()
      INSTANCE = instance
      instance
    }
  }
}

private val databaseCallbacks = object : Callback() {
  override fun onCreate(connection: SQLiteConnection) {
    super.onCreate(connection)
    runCatching {
      println("insert data to songs fts table")
      connection.execSQL(
        """
              INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, text)
              SELECT _id, name, author, translator, composer, bibleref, text FROM psalms
            """.trimIndent()
      )

      println("setup fts table triggers")
      connection.execSQL(
        """
              CREATE TRIGGER songs_fts_bu BEFORE UPDATE ON psalms 
              BEGIN 
                  DELETE FROM songs_fts WHERE docid=old.rowid; 
              END;
            """.trimIndent()
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_bd BEFORE DELETE ON psalms 
            BEGIN 
                DELETE FROM songs_fts WHERE docid=old.rowid; 
            END;
            """
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_au AFTER UPDATE ON psalms 
            BEGIN 
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
            """
      )
      connection.execSQL(
        """
            CREATE TRIGGER songs_fts_ai AFTER INSERT ON psalms 
            BEGIN 
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
            """
      )
    }.onFailure { e -> System.err.println(e.message) }
  }
}
