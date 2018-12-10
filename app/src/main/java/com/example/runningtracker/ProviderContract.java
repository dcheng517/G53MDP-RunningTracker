package com.example.runningtracker;

import android.net.Uri;

public final class ProviderContract {

    public final static String AUTHORITY = "com.example.runningtracker.provider.MyContentProvider";
    public final static String RT_TABLE = "runningtracker";
    public final static Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(RT_TABLE).build();

    public static final String RT_ID = "rt_id";
    public static final String RT_DATE = "rt_date";
    public static final String RT_DIST = "rt_dist";
    public static final String RT_TIME = "rt_time";
    public static final String DIST_TOTAL = "SUM(rt_dist)";
//    public static final String TIME = "(julianday(rt_end)-julianday(rt_start)*24 AS time)";
//    public static final String SPEED = "(rt_dist/(julianday(rt_end)-julianday(rt_start)*24)) AS top_speed";
}
