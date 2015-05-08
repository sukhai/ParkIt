package com.csc413.group9.parkIt.Features;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.csc413.group9.parkIt.Database.DatabaseManager;

/**
 * Created by Su Khai Koh on 5/6/15.
 */
public class StreetHighlightSettings {

    public StreetHighlightSettings() {

    }

    public void setHighlighted(boolean onStreet, boolean offStreet) {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        // Delete all entries
        db.delete(DatabaseHelper.TABLE_NAME_SETTINGS, null, null);

        int onStreetValue = onStreet ? DatabaseHelper.BOOLEAN_TRUE : DatabaseHelper.BOOLEAN_FALSE;
        int offStreetValue = offStreet ? DatabaseHelper.BOOLEAN_TRUE : DatabaseHelper.BOOLEAN_FALSE;

        ContentValues settings = new ContentValues();
        settings.put(DatabaseHelper.COLUMN_SETTINGS_ONSTREET, onStreetValue);
        settings.put(DatabaseHelper.COLUMN_SETTINGS_OFFSTREET, offStreetValue);

        // Insert the new settings into the database
        db.insert(DatabaseHelper.TABLE_NAME_SETTINGS, null, settings);

        DatabaseManager.getInstance().close();
    }

    public boolean isOnStreetHighlighted() {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_SETTINGS, new String[]{"*"},
                null, null, null, null, null, null);

        int value = DatabaseHelper.BOOLEAN_TRUE;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                value = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_SETTINGS_ONSTREET));
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();

        return value == DatabaseHelper.BOOLEAN_TRUE;
    }

    public boolean isOffStreetHighlighted() {

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME_SETTINGS, new String[]{"*"},
                null, null, null, null, null, null);

        int value = DatabaseHelper.BOOLEAN_TRUE;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                value = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_SETTINGS_OFFSTREET));
            }

            cursor.close();
        }

        DatabaseManager.getInstance().close();

        return value == DatabaseHelper.BOOLEAN_TRUE;
    }
}
