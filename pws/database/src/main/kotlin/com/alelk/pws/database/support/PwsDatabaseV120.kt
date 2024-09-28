package com.alelk.pws.database.support

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alelk.pws.database.support.dao.FavoriteDaoV120
import com.alelk.pws.database.support.dto.Favorite
import com.alelk.pws.database.support.entity.BookEntityV120
import com.alelk.pws.database.support.entity.FavoriteEntityV120
import com.alelk.pws.database.support.entity.HistoryEntityV120
import com.alelk.pws.database.support.entity.SongNumberEntityV120

@Database(
  version = 5,
  entities = [
    BookEntityV120::class,
    FavoriteEntityV120::class,
    HistoryEntityV120::class,
    SongNumberEntityV120::class
  ]
)
abstract class PwsDatabaseV120 : RoomDatabase(), PwsDatabaseVx {
  override val databaseFileName: String = "pws.1.2.0.db"

  abstract fun favoriteDao(): FavoriteDaoV120

  override fun getAllFavorites(): List<Favorite> = favoriteDao().getAllFavorites()
}

