package com.alelk.pws.database.util;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localized strings provider
 *
 * Created by Alex Elkin on 18.07.17.
 */

public class LocalizedStringsProvider {

    private static final String RESOURCE_BUNDLE_NAME = "strings";
    private static final String LOG_TAG = LocalizedStringsProvider.class.getSimpleName();

    public static String getResource(String stringKey, Locale locale) {
        ResourceBundle rb;
        try {
            rb = getBundle(locale);
        } catch (MissingResourceException exc) {
            Log.w(LOG_TAG, "Cannot get resource '" + stringKey + "' for the locale " + locale +
                    ": No resource bundle found. Trying to get resource from default resource bundle..");
            rb = getBundle();
        }
        return rb != null ? rb.getString(stringKey) : null;
    }

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
    }

    private static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
    }
}
