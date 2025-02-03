package io.github.alelk.pws.database.common.entity

import androidx.room.*

@Fts4(contentEntity = SongEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61, tokenizerArgs = ["remove_diacritics=1"])
@Entity(tableName = "songs_fts")
data class SongFtsEntity(
  @PrimaryKey @ColumnInfo(name = "rowid") val id: Int,
  @ColumnInfo(name = "name") val name: String?,
  @ColumnInfo(name = "author") val author: String?,
  @ColumnInfo(name = "translator") val translator: String?,
  @ColumnInfo(name = "composer") val composer: String?,
  @ColumnInfo(name = "bibleref") val bibleRef: String?,
  @ColumnInfo(name = "text") val text: String
) {
  companion object {

    // fixme:
    // SQL Scripts for Trigger Management
    object Triggers {

      private const val TRIGGER_BU_SCRIPT = """
            CREATE TRIGGER songs_fts_bu BEFORE UPDATE ON psalms 
            BEGIN 
                DELETE FROM songs_fts WHERE docid=old.rowid; 
            END;
        """

      private const val TRIGGER_BD_SCRIPT = """
            CREATE TRIGGER songs_fts_bd BEFORE DELETE ON psalms 
            BEGIN 
                DELETE FROM songs_fts WHERE docid=old.rowid; 
            END;
        """

      private const val TRIGGER_AU_SCRIPT = """
            CREATE TRIGGER songs_fts_au AFTER UPDATE ON psalms 
            BEGIN 
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
        """

      private const val TRIGGER_AI_SCRIPT = """
            CREATE TRIGGER songs_fts_ai AFTER INSERT ON psalms 
            BEGIN 
                INSERT INTO songs_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
        """
    }
  }
}