package com.example.runningtracker;

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


public class RTService extends Service {

    private static final String log = "G53MDP";

    RunningTracker rt;
    Intent noti;
    NotificationManager notiMan;
    NotificationCompat.Builder notiBuild;

    private rtBinder binder = new rtBinder();

    private static final String TAG = "G53MDP";
    private boolean isBound = true;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String recDate;

    String provider;

    Location startLocation;
    Location endLocation;

    public RTService() {}

    public class rtBinder extends Binder {
        RTService getService()
        {
            return RTService.this;
        }

        public void updateNoti(int status)
        {
            RTService.this.updateNoti(status);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(log, "RTService onCreate.");

//        notiBuilder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        notiBuilder();

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

        endLocation = startLocation;

        return START_STICKY;
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            endLocation = location;
            sendBroadcast();
            Log.d(log, "startLocation: " + startLocation.getLatitude() + ", " + startLocation.getLongitude());
            Log.d(log, "endLocation: " + endLocation.getLatitude() + ", " + endLocation.getLongitude());
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

//        Notification tap to open activity
        Intent mainActivity = new Intent(this, MainActivity.class);
        PendingIntent getActivity = PendingIntent.getActivity(this, 0, mainActivity, 0);

//        Intent to kill service
//        Intent closeIntent = new Intent(this, stopReceiver.class);
//        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notiBuild = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("RunningTracker")
                .setContentText("Current Status: Idle")
                .setContentIntent(getActivity);

//            add kill service action button on notification if MainActivity is destroyed
//        if(!isBound)
//            notiBuild.addAction(R.drawable.close, getString(R.string.kill), closePendingIntent);

        notiMan.notify(1, notiBuild.build());
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
                text = text + "Idle";
                break;
        }
        Log.d(log, "updateNoti: " + text);
        notiBuild.setContentText(text);
        notiMan.notify(1, notiBuild.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        isBound = true;
//        notiBuilder();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.i(TAG, "onUnbind");
        super.onDestroy();
        isBound = false;
//        notiBuilder();
        return true;
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
        isBound = true;
//        notiBuilder();
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "Service onDestroy");
    }
}

