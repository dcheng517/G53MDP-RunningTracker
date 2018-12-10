package com.example.runningtracker.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.example.runningtracker.ProviderContract;
import com.example.runningtracker.RTDBHandler;

public class MyContentProvider extends ContentProvider {

    public static final String log = "G53MDP";

    private RTDBHandler rtdbHandler;
    private SQLiteDatabase rtDB;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int RT = 1;
    public static final int RT_ID = 2;

    static {
        uriMatcher.addURI(ProviderContract.AUTHORITY, ProviderContract.RT_TABLE, RT);
        uriMatcher.addURI(ProviderContract.AUTHORITY, ProviderContract.RT_TABLE + "/#", RT_ID);
    }

    @Override
    public boolean onCreate() {
        Log.d(log, "Content Provider onCreate.");
        rtdbHandler = new RTDBHandler(this.getContext(), null, null, 1);
        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        rtDB = rtdbHandler.getWritableDatabase();
        int rowsDeleted = 0;

        switch (uriType)
        {
            case RT:
                rowsDeleted = rtDB.delete(ProviderContract.RT_TABLE, selection, selectionArgs);
                break;
            case RT_ID:
                String id = uri.getLastPathSegment();

                if(selection.isEmpty())
                    rowsDeleted = rtDB.delete(ProviderContract.RT_TABLE,
                            ProviderContract.RT_ID + " = " + id,
                            null);
                else
                    rowsDeleted = rtDB.delete(ProviderContract.RT_TABLE,
                            ProviderContract.RT_ID + " = " + id + " and " + selection,
                            selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        rtDB = rtdbHandler.getWritableDatabase();
        long id = 0;
        switch(uriType)
        {
            case RT:
                id = rtDB.insert(ProviderContract.RT_TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(ProviderContract.RT_TABLE + "/" + id);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(ProviderContract.RT_TABLE);

        int uriType = uriMatcher.match(uri);
        rtDB = rtdbHandler.getReadableDatabase();

        switch (uriType)
        {
            case RT_ID:
                queryBuilder.appendWhere(ProviderContract.RT_ID + "=" + uri.getLastPathSegment());
                break;
            case RT:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor cursor = queryBuilder.query(rtDB, projection, selection,
                selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        rtDB = rtdbHandler.getWritableDatabase();
        int rowsUpdated = 0;

        switch (uriType)
        {
            case RT:
                rowsUpdated = rtDB.update(ProviderContract.RT_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case RT_ID:
                String id = uri.getLastPathSegment();

                if(selection.isEmpty())
                    rowsUpdated = rtDB.update(ProviderContract.RT_TABLE,
                            values,
                            ProviderContract.RT_ID + " = " + id,
                            null);
                else
                    rowsUpdated = rtDB.update(ProviderContract.RT_TABLE,
                            values,
                            ProviderContract.RT_ID + " = " + id + " and " + selection,
                            selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(ProviderContract.BASE_URI, null);
        return rowsUpdated;
    }
}
