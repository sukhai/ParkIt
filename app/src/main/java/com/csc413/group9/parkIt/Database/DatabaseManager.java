package com.csc413.group9.parkIt.Database;

import android.content.Context;
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

    /**
     * The instance of this class (DatabaseManager).
     */
    private static DatabaseManager instance;

    /**
     * The database helper.
     */
    private static DatabaseHelper mDatabaseHelper;

    /**
     * The SQLite database.
     */
    private SQLiteDatabase mDatabase;

    /**
     * Number of database connections opened.
     */
    private int mOpenCounter;

    /**
     * No-args constructor (Default constructor).
     */
    private DatabaseManager() {
        // Default constructor
    }

    /**
     * Initialize the DatabaseManager.
     * @param context The context of the application
     */
    public static synchronized void initializeInstance(Context context) {

        if (instance == null) {

            instance = new DatabaseManager();

            mDatabaseHelper = new DatabaseHelper(context);
        }
    }

    /**
     * Get the instance of this class (DatabaseManager).
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

        // Only open a connection to the database if no other threads are using it
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
}
