package com.alelk.pws.database.entity

import androidx.room.*

@Fts4(contentEntity = SongEntity::class, tokenizer = FtsOptions.TOKENIZER_PORTER)
@Entity(tableName = "psalms_fts")
data class SongFtsEntity(
  @PrimaryKey @ColumnInfo(name = "rowid") val id: Long,
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
            CREATE TRIGGER psalms_fts_bu BEFORE UPDATE ON psalms 
            BEGIN 
                DELETE FROM psalms_fts WHERE docid=old.rowid; 
            END;
        """

      private const val TRIGGER_BD_SCRIPT = """
            CREATE TRIGGER psalms_fts_bd BEFORE DELETE ON psalms 
            BEGIN 
                DELETE FROM psalms_fts WHERE docid=old.rowid; 
            END;
        """

      private const val TRIGGER_AU_SCRIPT = """
            CREATE TRIGGER psalms_fts_au AFTER UPDATE ON psalms 
            BEGIN 
                INSERT INTO psalms_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
        """

      private const val TRIGGER_AI_SCRIPT = """
            CREATE TRIGGER psalms_fts_ai AFTER INSERT ON psalms 
            BEGIN 
                INSERT INTO psalms_fts (docid, name, author, translator, composer, bibleref, text) 
                VALUES(new.rowid, new.name, new.author, new.translator, new.composer, new.bibleref, new.text); 
            END;
        """
    }
  }
}