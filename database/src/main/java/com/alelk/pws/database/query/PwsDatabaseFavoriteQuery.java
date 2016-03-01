package com.alelk.pws.database.query;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alelk.pws.database.data.FavoritePsalm;
import com.alelk.pws.database.data.entity.FavoriteEntity;
import com.alelk.pws.database.data.entity.PsalmEntity;
import com.alelk.pws.database.data.entity.PsalmNumberEntity;
import com.alelk.pws.database.exception.PwsDatabaseException;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;

import java.util.HashSet;
import java.util.Set;

import static com.alelk.pws.database.table.PwsFavoritesTable.*;

/**
 * Created by Alex Elkin on 19.02.2016.
 */
public class PwsDatabaseFavoriteQuery extends PwsDatabaseQueryUtils  implements PwsDatabaseQuery<FavoritePsalm, FavoriteEntity>{

    private final static String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_POSITION,
            COLUMN_PSALMNUMBERID};

    private static final String LOG_TAG = PwsDatabaseFavoriteQuery.class.getSimpleName();
    private SQLiteDatabase mDatabase;
    private Cursor mCursor;

    public PwsDatabaseFavoriteQuery(SQLiteDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }


    @Override
         protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public FavoriteEntity insert(FavoritePsalm pwsObject) throws PwsDatabaseException {
        final String METHOD_NAME = "insert";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        final ContentValues contentValues = new ContentValues();
        FavoriteEntity favoriteEntity = null;
        PsalmEntity psalmEntity = new PwsDatabasePsalmQuery(mDatabase).selectByNumbers(pwsObject.getPsalm());
        if (psalmEntity == null) {
            Log.d(LOG_TAG, METHOD_NAME + ": No psalm entity found for psalm " + pwsObject.getPsalm());
            // // TODO: 20.02.2016 throw exception
            return null;
        }
        PsalmNumberEntity psalmNumberEntity = new PwsDatabasePsalmNumberQuery(mDatabase, psalmEntity.getId(), pwsObject.getBookEdition()).selectByNumber(pwsObject.getNumber());
        return favoriteEntity;
    }

    @Override
    public FavoriteEntity selectById(long id) throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectById";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        FavoriteEntity favoriteEntity = null;
        try {
            mCursor = mDatabase.query(TABLE_FAVORITES, ALL_COLUMNS, COLUMN_ID + " = " + id, null, null, null, "1");
            if (mCursor.moveToFirst()) {
                favoriteEntity = cursorToFavoriteEntity(mCursor);
            }
            Log.d(LOG_TAG, METHOD_NAME + ": Favorite entity selected (id = '" + id + "'): " + favoriteEntity);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return favoriteEntity;
    }

    public Set<FavoriteEntity> selectAll() throws PwsDatabaseIncorrectValueException {
        final String METHOD_NAME = "selectAll";
        validateSQLiteDatabaseNotNull(METHOD_NAME, mDatabase);
        Set<FavoriteEntity> favoriteEntities = null;
        try {
            mCursor = mDatabase.query(TABLE_FAVORITES, ALL_COLUMNS, null, null, null, null, null);
            if (mCursor.moveToFirst()) {
                favoriteEntities = new HashSet<>(mCursor.getCount());
                do {
                    favoriteEntities.add(cursorToFavoriteEntity(mCursor));
                } while (mCursor.moveToNext());
                Log.v(LOG_TAG, METHOD_NAME + ": Count of favorites selected : " + favoriteEntities.size());
            } else {
                Log.v(LOG_TAG, METHOD_NAME + ": No favorites selected");
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return favoriteEntities;
    }

    private FavoriteEntity cursorToFavoriteEntity(Cursor cursor) {
        FavoriteEntity favoriteEntity = new FavoriteEntity();
        favoriteEntity.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        favoriteEntity.setPosition(cursor.getLong(cursor.getColumnIndex(COLUMN_POSITION)));
        favoriteEntity.setPsalmNumberId(cursor.getLong(cursor.getColumnIndex(COLUMN_PSALMNUMBERID)));
        return favoriteEntity;
    }

    private void fillContentValues(ContentValues values, int position, long psalmNumberId) {
        values.put(COLUMN_POSITION, position);
        values.put(COLUMN_PSALMNUMBERID, psalmNumberId);
    }
}
