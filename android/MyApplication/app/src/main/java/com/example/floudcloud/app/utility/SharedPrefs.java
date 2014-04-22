package com.example.floudcloud.app.utility;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class SharedPrefs {
    private static String SP_NAME = "floud";
    private static Context context;

    public static final String PREF_PATH = "PATH";
    public static final String PREF_TIMESTAMP = "TIMESTAMP";

    public static void init(Context context) {
        SharedPrefs.context = context;
    }

    public static boolean addItem(String key, String value) {
        return putStringInPreferences(key, value);
    }

    public static boolean addTimestamp(long timestamp) {
        return putLongInPreferences(PREF_TIMESTAMP, timestamp);
    }

    public static long getTimestamp() {
        return getLongFromPreferences(0, PREF_TIMESTAMP);
    }

    public static String getItem(String defaultValue, String key) {
        return getStringFromPreferences(defaultValue, key);
    }

    private static boolean putStringInPreferences(String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();

        return true;
    }

    private static boolean putLongInPreferences(String key, long value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.commit();

        return true;
    }

    private static long getLongFromPreferences(long defaultValue, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(key, defaultValue);
    }

    private static String getStringFromPreferences(String defaultValue, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }
}
