package com.alelk.pws.database.support

import com.alelk.pws.database.support.dto.Favorite

interface PwsDatabaseVx {
  val databaseFileName: String

  fun getAllFavorites(): List<Favorite>
}

