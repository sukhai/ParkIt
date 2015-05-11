package com.csc413.group9.parkIt.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A SQLite database helper class that manage database creation and version management.
 *
 * Created by Su Khai Koh on 4/17/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Default database name.
     */
    public static final String DATABASE_NAME = "database.db";

    /**
     * Default database version.
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * The column ID for current location data (Primary key).
     */
    public static final String COLUMN_ID = "ID";

    // Parked section (For WarningTimer)

    /**
     * The table name for parked location data.
     */
    public static final String TABLE_NAME_PARKED = "Parked";

    /**
     * The column for the parked location address data.
     */
    public static final String COLUMN_PARKED_ADDRESS = "Address";

    /**
     * The column for the parked location latitude data.
     */
    public static final String COLUMN_PARKED_LATITUDE = "Latitude";

    /**
     * The column for the parked location longitude data.
     */
    public static final String COLUMN_PARKED_LONGITUDE = "Longitude";

    // Settings section

    /**
     * The table name for the user's settings data.
     */
    public static final String TABLE_NAME_SETTINGS = "Settings";

    /**
     * The column for the on-street highlight on the user's settings data.
     */
    public static final String COLUMN_SETTINGS_ONSTREET = "Onstreet";

    /**
     * The column for the off-street highlight on the user's settings data.
     */
    public static final String COLUMN_SETTINGS_OFFSTREET = "Offstreet";

    /**
     * The boolean value for true.
     */
    public static final int BOOLEAN_TRUE = 0;

    /**
     * The boolean value for false.
     */
    public static final int BOOLEAN_FALSE = 1;

    /**
     * Text type data.
     */
    private static final String TEXT_TYPE = " TEXT NOT NULL";

    /**
     * Boolean type data.
     */
    private static final String BOOL_TYPE = " INTEGER DEFAULT " + BOOLEAN_TRUE;

    /**
     * Real number type data.
     */
    private static final String REAL_TYPE = " REAL NOT NULL";

    /**
     * Comma ','.
     */
    private static final String COMMA = ",";

    /**
     * The SQL statement that drop the table if the table exists.
     */
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    /**
     * The SQL statement that create a table for user's parked location data.
     */
    private static final String SQL_CREATE_TABLE_PARKED =
            "CREATE TABLE " + TABLE_NAME_PARKED + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_PARKED_ADDRESS + TEXT_TYPE + COMMA +
                    COLUMN_PARKED_LATITUDE + REAL_TYPE + COMMA +
                    COLUMN_PARKED_LONGITUDE + REAL_TYPE +
                    " )";

    /**
     * The SQL statement that delete the table for user's parked location table.
     */
    private static final String SQL_DELETE_TABLE_PARKED =
            DROP_TABLE + TABLE_NAME_PARKED;

    /**
     * The SQL statement that create a table for user's settings data.
     */
    private static final String SQL_CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + TABLE_NAME_SETTINGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_SETTINGS_ONSTREET + BOOL_TYPE + COMMA +
                    COLUMN_SETTINGS_OFFSTREET + BOOL_TYPE +
                    " )";

    /**
     * The SQL statement that delete the table for user's settings table.
     */
    private static final String SQL_DELETE_TABLE_SETTINGS =
            DROP_TABLE + TABLE_NAME_SETTINGS;

    /**
     * Setup database.
     * @param context the context of this application
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_TABLE_PARKED);
        db.execSQL(SQL_CREATE_TABLE_SETTINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Clear all data
        db.execSQL(SQL_DELETE_TABLE_PARKED);
        db.execSQL(SQL_DELETE_TABLE_SETTINGS);

        // Recreate tables
        onCreate(db);
    }
}
