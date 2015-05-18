package com.csc413.group9.parkIt.Features;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.csc413.group9.parkIt.Database.DatabaseHelper;
import com.csc413.group9.parkIt.Database.DatabaseManager;

/**
 * A class that handle the user settings for the street highlights. The user can set either to
 * highlight on-street parking, off-street parking, or both types of parking on the Google map. The
 * settings will be stored and retrieved from the database. So the data will be available again
 * if the app restarts.
 *
 * Created by Su Khai Koh on 5/6/15.
 */
public class StreetHighlightSettings {

    /**
     * Set whether to highlight or not the on-street and off-street parking. The settings will be
     * stored in the database.
     * @param onStreet highlight on-street parking?
     * @param offStreet highlight off-street parking?
     */
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

    /**
     * Check whether the on-street parking is highlighted. This will check the stored value from the
     * database.
     * @return true if the on-street parking should be highlighted, false otherwise
     */
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

    /**
     * Check whether the off-street parking is highlighted. This will check the stored value from
     * the database.
     * @return true if the off-street parking should be highlighted, false otherwise
     */
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
