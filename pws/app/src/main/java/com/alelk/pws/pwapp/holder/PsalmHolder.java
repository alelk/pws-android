package com.alelk.pws.pwapp.holder;

import android.database.Cursor;
import android.text.TextUtils;

import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.Psalm;

import java.util.Arrays;

/**
 * Created by AlexElkin on 27.05.2016.
 */
public class PsalmHolder implements PwsHolder {
    private long mPsalmNumberId;
    private int mPsalmNumber;
    private String mPsalmName;
    private String mPsalmText;
    private String mPsalmAuthor;
    private String mPsalmTranslator;
    private String mPsalmComposer;
    private String mPsalmLocale;
    private String[] mPsalmTonalities;
    private String mBibleRef;
    private String mBookName;
    private boolean isFavoritePsalm;

    public PsalmHolder(){};

    public PsalmHolder(long psalmNumberId, int psalmNumber, String psalmName, String psalmText, String psalmAuthor, String psalmTranslator, String psalmComposer, String psalmLocale, String[] psalmTonalities, String bibleRef, String bookName, boolean isFavoritePsalm) {
        mPsalmNumberId = psalmNumberId;
        mPsalmNumber = psalmNumber;
        mPsalmName = psalmName;
        mPsalmText = psalmText;
        mPsalmAuthor = psalmAuthor;
        mPsalmTranslator = psalmTranslator;
        mPsalmComposer = psalmComposer;
        mPsalmLocale = psalmLocale;
        mPsalmTonalities = psalmTonalities;
        mBibleRef = bibleRef;
        mBookName = bookName;
        this.isFavoritePsalm = isFavoritePsalm;
    }

    public PsalmHolder(Cursor cursor, boolean isFavoritePsalm) {
        setFavoritePsalm(isFavoritePsalm);
        if (cursor != null && !cursor.isClosed()) {
            int index;
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMNUMBER_ID)) != -1) {
                mPsalmNumberId = cursor.getLong(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMNUMBER)) != -1) {
                mPsalmNumber = cursor.getInt(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMNAME)) != -1) {
                mPsalmName = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMTEXT)) != -1) {
                mPsalmText = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMAUTHOR)) != -1) {
                mPsalmAuthor = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMTRANSLATOR)) != -1) {
                mPsalmTranslator = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMCOMPOSER)) != -1) {
                mPsalmComposer = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMLOCALE)) != -1) {
                mPsalmLocale = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMANNOTATION)) != -1) {
                mBibleRef = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_BOOKDISPLAYNAME)) != -1) {
                mBookName = cursor.getString(index);
            }
            if ((index = cursor.getColumnIndex(Psalm.COLUMN_PSALMTONALITIES)) != -1) {
                String tons = cursor.getString(index);
                if (!TextUtils.isEmpty(tons)) {
                    mPsalmTonalities = tons.split("\\|");
                }
            }
        }
    }

    public long getPsalmNumberId() {
        return mPsalmNumberId;
    }

    public void setPsalmNumberId(long psalmNumberId) {
        mPsalmNumberId = psalmNumberId;
    }

    public int getPsalmNumber() {
        return mPsalmNumber;
    }

    public void setPsalmNumber(int psalmNumber) {
        mPsalmNumber = psalmNumber;
    }

    public String getPsalmName() {
        return mPsalmName;
    }

    public void setPsalmName(String psalmName) {
        mPsalmName = psalmName;
    }

    public String getPsalmText() {
        return mPsalmText;
    }

    public void setPsalmText(String psalmText) {
        mPsalmText = psalmText;
    }

    public String getPsalmAuthor() {
        return mPsalmAuthor;
    }

    public void setPsalmAuthor(String psalmAuthor) {
        mPsalmAuthor = psalmAuthor;
    }

    public String getPsalmTranslator() {
        return mPsalmTranslator;
    }

    public void setPsalmTranslator(String psalmTranslator) {
        mPsalmTranslator = psalmTranslator;
    }

    public String getPsalmComposer() {
        return mPsalmComposer;
    }

    public void setPsalmComposer(String psalmComposer) {
        mPsalmComposer = psalmComposer;
    }

    public String getPsalmLocale() {
        return mPsalmLocale;
    }

    public void setPsalmLocale(String psalmLocale) {
        mPsalmLocale = psalmLocale;
    }

    public String[] getPsalmTonalities() {
        return mPsalmTonalities;
    }

    public void setPsalmTonalities(String[] psalmTonalities) {
        mPsalmTonalities = psalmTonalities;
    }

    public String getBibleRef() {
        return mBibleRef;
    }

    public void setBibleRef(String bibleRef) {
        mBibleRef = bibleRef;
    }

    public String getBookName() {
        return mBookName;
    }

    public void setBookName(String bookName) {
        mBookName = bookName;
    }

    public boolean isFavoritePsalm() {
        return isFavoritePsalm;
    }

    public void setFavoritePsalm(boolean favoritePsalm) {
        isFavoritePsalm = favoritePsalm;
    }

    @Override
    public String toString() {
        return "PsalmHolder{" +
                "mPsalmNumberId=" + mPsalmNumberId +
                ", mPsalmNumber=" + mPsalmNumber +
                ", mPsalmName='" + mPsalmName + '\'' +
                ", mPsalmText: " + (mPsalmText == null ? null : mPsalmText.length() + " symbols") +
                ", mPsalmAuthor='" + mPsalmAuthor + '\'' +
                ", mPsalmTranslator='" + mPsalmTranslator + '\'' +
                ", mPsalmComposer='" + mPsalmComposer + '\'' +
                ", mPsalmLocale='" + mPsalmLocale + '\'' +
                ", mPsalmTonalities=" + Arrays.toString(mPsalmTonalities) +
                ", mBibleRef='" + mBibleRef + '\'' +
                ", mBookName='" + mBookName + '\'' +
                ", isFavoritePsalm=" + isFavoritePsalm +
                '}';
    }
}
