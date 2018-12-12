package com.example.runningtracker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*
*   Database handler for the app. Insert/Query/Delete methods in here.
*   Uses content resolver to communicate with the content provider.
*/

public class RTDBHandler extends SQLiteOpenHelper {

    private static final String log = "G53MDP";

    private ContentResolver myCR;

    private static final int DB_VER = 1;
    private static final String DB_NAME = "runningtracker.db";

    private static final String sql_create =
            "CREATE TABLE if not exists " + ProviderContract.RT_TABLE + " (" +
                    ProviderContract.RT_ID + " INTEGER PRIMARY KEY, " +
                    ProviderContract.RT_DATE + " TEXT, " +
                    ProviderContract.RT_TIME + " REAL, " +
                    ProviderContract.RT_DIST + " REAL);";

    public RTDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version)
    {
        super(context, DB_NAME, factory, DB_VER);
        myCR = context.getContentResolver();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(log, "Creating table.");
        db.execSQL(sql_create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(log, "Upgrading table");
        db.execSQL("DROP TABLE IF EXISTS " + ProviderContract.RT_TABLE);
        onCreate(db);
    }

//        query row by id
    public RunningTracker queryRec (int id)
    {
        String[] projection =
                {
                        ProviderContract.RT_ID,
                        ProviderContract.RT_DATE,
                        ProviderContract.RT_DIST,
                        ProviderContract.RT_TIME
                };
        String selection = "rt_id = \"" + id + "\"" ;

        Cursor cursor = myCR.query(ProviderContract.CONTENT_URI, projection,
                selection, null, null);

        RunningTracker rt = new RunningTracker();

        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                cursor.moveToFirst();
                rt.setRt_id(Integer.parseInt(cursor.getString(0)));
                rt.setRt_date(cursor.getString(1));
                rt.setRt_dist(cursor.getFloat(2));
                rt.setRt_time(cursor.getFloat(3));
                cursor.close();
            }else
                rt = null;
        }

        return rt;
    }

//        returns a list of all logs on that day
    public List<RunningTracker> allLogs(String selDate)
    {
        List<RunningTracker> logList = new ArrayList<>();

        String[] projection =
                {
                        ProviderContract.RT_ID,
                        ProviderContract.RT_DATE,
                        ProviderContract.RT_DIST,
                        ProviderContract.RT_TIME
                };
        String selection = "DATE(rt_date) = DATE('" + selDate + "')";

        int id;
        String date;
        float dist;
        float duration;

        Cursor cursor = myCR.query(ProviderContract.CONTENT_URI, projection, selection,
                null, null);

        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    id = cursor.getInt(0);
                    date = cursor.getString(1);
                    dist = cursor.getFloat(2);
                    duration = cursor.getFloat(3);

                    RunningTracker res = new RunningTracker(id, date, dist, duration);
                    logList.add(res);
                }while (cursor.moveToNext());
            }
        }

        return logList;
    }

//    total distance in a day/week/month/year
    public float distTotal(String date, int reqID)
    {
        String[] projection = {ProviderContract.DIST_TOTAL}; // SUM(rt_dist)
        String selection = "";
        float ret = 0;

        switch (reqID)
        {
//            total dist of current day
            case 1:
                selection = "DATE(rt_date) = DATE('" + date + "')";

                break;
//            total dist of current week
            case 2:
                selection = "DATE(rt_date) BETWEEN DATE('" + date
                        + "', 'weekday 0', '-6 days') AND DATE('" + date
                        + "', 'weekday 0')";

                break;
//            total dist of current month
            case 3:
                selection = "DATE(rt_date) BETWEEN DATE('" + date
                        + "', 'start of month') AND DATE('" + date
                        + "', 'start of month', '+1 month', '-1 day')";

                break;
//            total dist of current year
            case 4:
                selection = "DATE(rt_date) BETWEEN DATE('" + date
                        + "', 'start of year') AND DATE('" + date
                        + "', 'start of year', '+12 months', '-1 day')";
            default:
                break;
        }

        if(!selection.isEmpty())
        {
            Cursor cursor = myCR.query(ProviderContract.CONTENT_URI, projection,
                    selection, null, null);

            if(cursor != null)
            {
                if(cursor.moveToFirst())
                {
                    cursor.moveToFirst();
                    ret = cursor.getFloat(0);
                    cursor.close();
                }else
                    ret = 0;
            }
        }

        return ret;
    }

//    query record with top speed
    public RunningTracker topSpeed()
    {
        String[] projection =
                {
                    ProviderContract.RT_ID,
                    ProviderContract.RT_DATE,
                    ProviderContract.RT_DIST,
                    ProviderContract.RT_TIME
                };

//      selection = (rt_dist/rt_time) = (SELECT MAX(rt_dist/rt_time) FROM runningtracker);
        String selection =
                ProviderContract.SPEED + " = " + // (rt_dist/rt_time)
                        "(SELECT MAX(" + ProviderContract.SPEED + ") FROM " //
                        + ProviderContract.RT_TABLE +")";

        Cursor cursor = myCR.query(ProviderContract.CONTENT_URI, projection,
                selection, null, null);

        RunningTracker rt = new RunningTracker();

        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                cursor.moveToFirst();
                rt.setRt_id(Integer.parseInt(cursor.getString(0)));
                rt.setRt_date(cursor.getString(1));
                rt.setRt_dist(cursor.getFloat(2));
                rt.setRt_time(cursor.getFloat(3));
                cursor.close();
            }else
                rt = null;
        }

        return rt;
    }

//    query record with max distance
    public RunningTracker maxDist()
    {
        String[] projection =
                {
                        ProviderContract.RT_ID,
                        ProviderContract.RT_DATE,
                        ProviderContract.RT_DIST,
                        ProviderContract.RT_TIME
                };
        String selection = "rt_dist = (SELECT MAX(" + ProviderContract.RT_DIST
                + ") FROM " + ProviderContract.RT_TABLE +")";

        Cursor cursor = myCR.query(ProviderContract.CONTENT_URI, projection,
                selection, null, null);

        RunningTracker rt = new RunningTracker();

        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                cursor.moveToFirst();
                rt.setRt_id(Integer.parseInt(cursor.getString(0)));
                rt.setRt_date(cursor.getString(1));
                rt.setRt_dist(cursor.getFloat(2));
                rt.setRt_time(cursor.getFloat(3));
                cursor.close();
            }else
                rt = null;
        }

        return rt;
    }

//        add new row to db
    public void addRT(RunningTracker rt)
    {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.RT_DATE, rt.getRt_date());
        values.put(ProviderContract.RT_DIST, rt.getRt_dist());
        values.put(ProviderContract.RT_TIME, rt.getRt_time());

        myCR.insert(ProviderContract.CONTENT_URI, values);
    }

//        delete row from db by id
    public boolean deleteRT(int id)
    {
        boolean result = false;
        String selection = "rt_id = \"" + Integer.toString(id) + "\"";

        int rowsDeleted = myCR.delete(ProviderContract.CONTENT_URI,
                selection, null);

        if(rowsDeleted>0)
            result = true;

        return result;
    }

}
