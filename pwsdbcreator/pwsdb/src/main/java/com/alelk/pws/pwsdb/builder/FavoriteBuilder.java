package com.alelk.pws.pwsdb.builder;

import com.alelk.pws.pwsdb.data.BookEdition;
import com.alelk.pws.pwsdb.data.FavoritePsalm;
import com.alelk.pws.pwsdb.data.Psalm;
import com.alelk.pws.pwsdb.data.entity.FavoriteEntity;

/**
 * Created by AlexElkin on 20.02.2016.
 */
public class FavoriteBuilder implements PwsBuilder<FavoritePsalm, FavoriteEntity>{
    private FavoriteEntity mFavoriteEntity;
    private Psalm mPsalm;
    private BookEdition mBookEdition;

    public FavoriteBuilder(BookEdition bookEdition) {
        mBookEdition = bookEdition;
    }

    public FavoriteBuilder (BookEdition bookEdition, FavoriteEntity favoriteEntity) {
        mFavoriteEntity = favoriteEntity;
        mBookEdition = bookEdition;
    }

    public FavoriteBuilder (BookEdition bookEdition, FavoriteEntity favoriteEntity, Psalm psalm) {
        mFavoriteEntity = favoriteEntity;
        mBookEdition = bookEdition;
        mPsalm = psalm;
    }

    @Override
    public PwsBuilder<FavoritePsalm, FavoriteEntity> appendEntity(FavoriteEntity entity) {
        mFavoriteEntity = entity;
        return this;
    }

    public PwsBuilder<FavoritePsalm, FavoriteEntity> appendPsalm(Psalm psalm) {
        mPsalm = psalm;
        return this;
    }

    @Override
    public FavoritePsalm toObject() {
        FavoritePsalm favoritePsalm = new FavoritePsalm(mPsalm, mBookEdition, (int) mFavoriteEntity.getPosition());
        return favoritePsalm;
    }
}
