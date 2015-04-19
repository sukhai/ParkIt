package com.example.sukhai.part_it.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Created by Su Khai Koh on 4/17/15.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private static DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    private int mOpenCounter;

    private DatabaseManager() {

    }

    public static synchronized void initializeInstance(Context context) {

        if (instance == null) {

            instance = new DatabaseManager();

            mDatabaseHelper = new DatabaseHelper(context);
        }
    }

    public static synchronized DatabaseManager getInstance() {

        if (instance == null) {
            System.err.println("DatabaseManager is not initialized. " +
                    "Call DatabaseManager.initializeInstance(...) first.");
        }

        return instance;
    }

    public synchronized SQLiteDatabase open() {

        mOpenCounter++;

        if(mOpenCounter == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void close() {

        mOpenCounter--;

        if(mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }

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
