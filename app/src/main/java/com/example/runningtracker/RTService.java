package com.example.runningtracker;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/*
*   Main service file. Contains LocationManager to track movement.
*   Sends the data to Main Activity through a broadcast receiver
*   Contains a notification that can't be dismissed when service is running.
*/

public class RTService extends Service {

    private static final String log = "G53MDP";

    Intent noti;
    NotificationManager notiMan;
    NotificationCompat.Builder notiBuild;

    private rtBinder binder = new rtBinder();

    private static final String TAG = "G53MDP";

//    format date to SQLite format
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String recDate;

    String provider;

    Location startLocation;
    Location endLocation;

    public RTService() {}

    public class rtBinder extends Binder {
        public void updateNoti(int status)
        {
            RTService.this.updateNoti(status);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(log, "RTService onCreate.");

        notiBuilder();
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
//        get the current date
        recDate = sdf.format(Calendar.getInstance().getTime());
        Log.d(log, "recDate = " + recDate);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1, // minimum time interval between updates
                    1, // minimum distance between updates, in metres
                    locationListener);
            startLocation = locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            Log.d(log, e.toString());
        }

//      initialize endLocation
        endLocation = startLocation;
        sendBroadcast();

        return START_STICKY;
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            endLocation = location;
            Log.d(log, "onLocationChanged.");
            sendBroadcast();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
//            Log.d(log, "onStatusChanged: " + provider + " " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
//            Log.d(log, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
//            Log.d(log, "onProviderDisabled: " + provider);
        }
    };

//    broadcast the variables to the main activity
    public void sendBroadcast()
    {
        Intent broadcast = new Intent("RTbroadcast");
        broadcast.putExtra("recDate", recDate);
        broadcast.putExtra("startLocation", startLocation);
        broadcast.putExtra("endLocation", endLocation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
//        Log.d(log,"sendBroadcast");
    }

    //    Notification method
    public void notiBuilder()
    {
        noti = new Intent(this, MainActivity.class);
        notiMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

//        Notification tap to open main activity
        Intent mainActivity = new Intent(this, MainActivity.class);
        PendingIntent getActivity = PendingIntent.getActivity(this, 0, mainActivity, 0);

        notiBuild = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("RunningTracker")
                .setContentText("Current Status: Idle")
                .setContentIntent(getActivity);

        notiMan.notify(1, notiBuild.build());
        startForeground(1, notiBuild.build()); // notification can't be dismissed
    }

//     update current status of media player
    private void updateNoti(int status)
    {
        String text = "Current Status: ";

        switch (status){
            case 1:
                text = text + "Tracking";
                break;
            case 2:
                text = text + "Paused";
                break;
            case 3:
                text = text + "Idle";
        }
        Log.d(log, "updateNoti: " + text);
        notiBuild.setContentText(text);
        notiMan.notify(1, notiBuild.build());
        startForeground(1, notiBuild.build()); // notification can't be dismissed
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        notiBuilder();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.i(TAG, "onUnbind");
        return true;
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
        notiBuilder();
    }

    @Override
    public void onDestroy()
    {
        notiMan.cancel(1); // remove notification when service onDestroy
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
    }
}

