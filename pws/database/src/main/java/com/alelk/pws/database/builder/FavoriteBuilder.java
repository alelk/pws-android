package com.alelk.pws.database.builder;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.FavoritePsalm;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.entity.FavoriteEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;

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
