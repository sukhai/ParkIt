package com.example.sukhai.part_it.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A SQLite database helper class that manage database creation and version management.
 *
 * Created by Su Khai Koh on 4/17/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information
    public static final String DATABASE_NAME = "database.db";
    public static final int DATABASE_VERSION = 1;

    // Default last known location address, latitude, and longitude
    public static final String DEFAULT_LOCATION_ADDRESS = "Union Square San Francisco, CA, 94108, United States";
    public static final double DEFAULT_LOCATION_LATITUDE = 37.7881;
    public static final double DEFAULT_LOCATION_LONGITUDE = -122.4075;

    // Columns and table of the last known location table
    public static final String TABLE_NAME_LOCATION = "Location";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_LOCATION_ADDRESS = "Address";
    public static final String COLUMN_LOCATION_LATITUDE = "Latitude";
    public static final String COLUMN_LOCATION_LONGITUDE = "Longitude";

    // Columns and table of the recent searched location table
    public static final int RECENT_LOCATION_MAX_ENTRIES = 5;
    public static final String TABLE_NAME_RECENT = "Recent";
    public static final String COLUMN_RECENT_NAME = "Name";
    public static final String COLUMN_RECENT_ADDRESS = "Address";
    public static final String COLUMN_RECENT_PHONE = "Phone";
    public static final String COLUMN_RECENT_LATITUDE = "Latitude";
    public static final String COLUMN_RECENT_LONGITUDE = "Longitude";

    // Constants
    private static final String TEXT_TYPE = " TEXT NOT NULL";
    private static final String REAL_TYPE = " REAL NOT NULL";
    private static final String COMMA = ",";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    // SQL statement of the last known location table creation
    private static final String SQL_CREATE_TABLE_LOCATION =
            "CREATE TABLE " + TABLE_NAME_LOCATION + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_LOCATION_ADDRESS + TEXT_TYPE + COMMA +
                    COLUMN_LOCATION_LATITUDE + REAL_TYPE + COMMA +
                    COLUMN_LOCATION_LONGITUDE + REAL_TYPE +
                    " )";

    // SQL statement of the last known location table deletion
    private static final String SQL_DELETE_TABLE_LOCATION =
            DROP_TABLE + TABLE_NAME_LOCATION;

    // SQL statement of the recent searched location table
    private static final String SQL_CREATE_TABLE_RECENT =
            "CREATE TABLE " + TABLE_NAME_RECENT + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_RECENT_NAME + TEXT_TYPE + COMMA +
                    COLUMN_RECENT_ADDRESS + TEXT_TYPE + COMMA +
                    COLUMN_RECENT_PHONE + TEXT_TYPE + COMMA +
                    COLUMN_RECENT_LATITUDE + TEXT_TYPE + COMMA +
                    COLUMN_RECENT_LONGITUDE + TEXT_TYPE + COMMA +
                    " )";

    // SQL statement of the recent searched location table deletion
    private static final String SQL_DELETE_TABLE_RECENT =
            DROP_TABLE + TABLE_NAME_RECENT;

    public static final String SQL_DELETE_RECENT_ENTRIES =
            "DELETE FROM " + TABLE_NAME_RECENT +
                    " WHERE " + COLUMN_ID + " IN" +
                        " (SELECT " + COLUMN_ID +
                        " FROM " + TABLE_NAME_RECENT +
                        " ORDER BY " + COLUMN_ID + " DESC" +
                        " LIMIT -1 OFFSET " + RECENT_LOCATION_MAX_ENTRIES + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_TABLE_LOCATION);
//        db.execSQL(SQL_CREATE_TABLE_RECENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Clear all data
        db.execSQL(SQL_DELETE_TABLE_LOCATION);
        db.execSQL(SQL_DELETE_TABLE_RECENT);

        // Recreate tables
        onCreate(db);
    }
}
