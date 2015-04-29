package com.alelk.pws.database.query;

import android.text.TextUtils;

/**
 * Created by alelkin on 29.04.2015.
 */
public abstract class PwsDatabaseQueryUtils {
    protected boolean isVersionMatches(String oldVersion, String newVersion) {
        if (TextUtils.equals(oldVersion, newVersion)) {
            return true;
        }
        return false;
    }
}
