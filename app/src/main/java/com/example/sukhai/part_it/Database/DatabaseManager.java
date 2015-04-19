package com.example.sukhai.part_it.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * A class that handle opening and closing of the database connection.
 *
 * This is a singleton class, so DatabaseManager.initializeInstance(Context) must be called first
 * before any interaction with the methods in this class.
 *
 * In order to connect and disconnect from the database from other classes, you must call
 * DatabaseManager.getInstance().open() to open the database connection and
 * DatabaseManager.getInstance().close() to close the database connection.
 *
 * Example of usage:
 *
 *      SQLiteDatabase db = DatabaseManager.getInstance().open();
 *          ...
 *          ...
 *      DatabaseManager.getInstance().close();
 *
 * Created by Su Khai Koh on 4/17/15.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private static DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    private int mOpenCounter;               // Number of connections opened

    private DatabaseManager() {
        // Default constructor
    }

    /**
     * Initialize this class.
     * @param context The context of the application
     */
    public static synchronized void initializeInstance(Context context) {

        if (instance == null) {

            instance = new DatabaseManager();

            mDatabaseHelper = new DatabaseHelper(context);
        }
    }

    /**
     * Get the instance of this class.
     * @return the instance of this class
     */
    public static synchronized DatabaseManager getInstance() {

        if (instance == null) {
            System.err.println("DatabaseManager is not initialized. " +
                    "Call DatabaseManager.initializeInstance(...) first.");
        }

        return instance;
    }

    /**
     * Open a connection to the database. This method will make sure there is only one connection
     * open through out the application life time.
     * @return the database that is connected to
     */
    public synchronized SQLiteDatabase open() {

        mOpenCounter++;

        if(mOpenCounter == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    /**
     * Close the connection to the database.
     */
    public synchronized void close() {

        mOpenCounter--;

        if(mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    /**
     * Check whether the given table in the database is empty (no rows) or not.
     * @param tableName the table to be checked in the database
     * @return true if the table is empty (no rows), otherwise false.
     */
    public boolean isEmpty(String tableName) {

        boolean empty = true;

        SQLiteDatabase db = DatabaseManager.getInstance().open();

        String count = "SELECT count(*) FROM " + tableName;

        Cursor cursor = db.rawQuery(count, null);
        cursor.moveToFirst();

        if (cursor.getInt(0) > 0)
            empty = false;

        DatabaseManager.getInstance().close();

        return empty;
    }
}
