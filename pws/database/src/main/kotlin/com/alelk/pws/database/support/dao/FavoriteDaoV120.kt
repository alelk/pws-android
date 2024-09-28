package com.alelk.pws.database.support.dao

import androidx.room.Dao
import androidx.room.Query
import com.alelk.pws.database.support.dto.Favorite

@Dao
interface FavoriteDaoV120 {

  @Query(
    """
    select f.position as position, pn.number as number, b.edition as edition
    from favorites as f 
    inner join psalmnumbers as pn on f.psalmnumberid=pn._id 
    inner join books as b on pn.bookid=b._id
    """
  )
  fun getAllFavorites(): List<Favorite>
}