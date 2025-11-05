package io.github.alelk.pws.database

import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction

interface TestDbPatch {
  val initVersion: Int? get() = null
  val targetVersion: Int? get() = null
  fun apply(db: SQLiteDatabase)

  object V1xInsertCustomTags : TestDbPatch {
    override fun apply(db: SQLiteDatabase) {
      db.transaction(exclusive = true) {
        db.execSQL(
          """INSERT INTO tags (id, color, name, predefined, priority) 
            |VALUES ('custom-00002', '#118844', 'Custom tag 2', false, 0),
            |   ('custom-00003', '#118855', 'Custom tag 3', false, 0),
            |   ('custom-00004', '#118866', 'Custom tag 4', false, 0)""".trimMargin()
        )
        db.execSQL(
          """INSERT INTO song_number_tags (song_number_id, tag_id) 
            |VALUES (20, 'custom-00002'), (21, 'custom-00002'), (22, 'custom-00002'), (30, 'custom-00003'), (40, 'custom-00004')""".trimMargin()
        )

      }
    }
  }

  object V6toV7 : TestDbPatch {
    override val initVersion: Int = 6
    override val targetVersion: Int = 7
    override fun apply(db: SQLiteDatabase) {
      check(db.version == initVersion) { "Database version mismatch: expected $initVersion, actual ${db.version}" }
      db.transaction(exclusive = true) {
        db.execSQL("DROP TABLE IF EXISTS psalms_fts")
        db.execSQL("DROP TABLE IF EXISTS songs_fts")
        db.execSQL(
          """CREATE VIRTUAL TABLE songs_fts 
             USING fts4(name, bibleref, text, author, composer, translator, tokenize=porter, content=`psalms`)""".trimIndent()
        )
        db.execSQL(
          """INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, text)
             SELECT _id, name, author, translator, composer, bibleref, text FROM psalms""".trimIndent()
        )
      }
      db.version = targetVersion
    }
  }

  object V7toV8 : TestDbPatch {
    override val initVersion: Int = 7
    override val targetVersion: Int = 8
    override fun apply(db: SQLiteDatabase) {
      check(db.version == initVersion) { "Database version mismatch: expected $initVersion, actual ${db.version}" }
      db.transaction(exclusive = true) {
        db.execSQL(
          """CREATE TABLE favorites_new (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    position INTEGER NOT NULL,
                    psalmnumberid INTEGER NOT NULL,
                    FOREIGN KEY (psalmnumberid) REFERENCES psalmnumbers(_id) ON DELETE CASCADE ON UPDATE NO ACTION)""".trimIndent()
        )
        db.execSQL("""INSERT INTO favorites_new (position, psalmnumberid) SELECT position, psalmnumberid FROM favorites""".trimIndent())
        db.execSQL("DROP TABLE favorites")
        db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
        db.execSQL("CREATE INDEX idx_favorites_position ON favorites(position)")
        db.execSQL("CREATE INDEX idx_favorites_psalmnumberid ON favorites(psalmnumberid)")
        db.execSQL("CREATE INDEX index_favorites_psalmnumberid ON favorites(psalmnumberid)")
      }
      db.version = targetVersion
    }
  }

  object V8toV9 : TestDbPatch {
    override val initVersion: Int = 8
    override val targetVersion: Int = 9
    override fun apply(db: SQLiteDatabase) {
      check(db.version == initVersion) { "Database version mismatch: expected $initVersion, actual ${db.version}" }
      db.transaction(exclusive = true) {
        db.execSQL("ALTER TABLE favorites ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 1")
      }
      db.version = targetVersion
    }
  }

  object V9toV10 : TestDbPatch {
    override val initVersion: Int = 9
    override val targetVersion: Int = 10
    override fun apply(db: SQLiteDatabase) {
      check(db.version == initVersion) { "Database version mismatch: expected $initVersion, actual ${db.version}" }
      db.transaction(exclusive = true) {
        db.execSQL("DROP TABLE IF EXISTS songs_fts")
        db.execSQL(
          """CREATE VIRTUAL TABLE songs_fts 
            |USING fts4(name, bibleref, text, author, composer, translator, tokenize=unicode61 `remove_diacritics=1`, content=`psalms`)""".trimMargin()
        )
        db.execSQL(
          """INSERT OR REPLACE INTO songs_fts (rowid, name, author, translator, composer, bibleref, text) 
            |SELECT _id, name, author, translator, composer, bibleref, text FROM psalms""".trimMargin()
        )
      }
      db.version = targetVersion
    }
  }
}