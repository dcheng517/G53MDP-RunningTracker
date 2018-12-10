package com.example.runningtracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final String log = "G53MDP";
    private final int REQUEST_PERMISSION_LOCATION = 1;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    Intent rtService;
    RTDBHandler rtdbHandler;
    RTService.rtBinder binder;
    private ServiceConnection rtConn = null;

    private MapView mapView;
    private GoogleMap gmap;

    private TextView stopwatch;
    private TextView tvDist;
    private TextView tvSpeed;
    private Button startStop;
    private Button stats;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;

    Handler handler;

    int Seconds, Minutes, MilliSeconds ;

    String recDate;

    Location startLocation;
    Location endLocation;

    float distance;
    float duration = 0;

    private boolean isBound;
    private boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rtService = new Intent(this, RTService.class);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        handler = new Handler();

        if(checkPerm()) startService(rtService);

        if (rtConn != null) unbindService(rtConn);
        rtConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (RTService.rtBinder) service;
                isBound = true;
                Log.d(log, "Service Connected.");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
                Log.d(log, "Service Disconnected");
            }
        };
        bindService(rtService, rtConn, BIND_AUTO_CREATE);

        stopwatch = (TextView) findViewById(R.id.tvStopwatch);
        tvDist = (TextView) findViewById(R.id.tvDistShow);
        tvSpeed = (TextView) findViewById(R.id.tvSpeedShow);
        startStop = (Button) findViewById(R.id.btnStart);
        stats = (Button) findViewById(R.id.btnStats);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(12);
        LatLng ny = new LatLng(52.954752000, -1.199029000);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            stopwatch.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };

    public Runnable distRun = new Runnable() {

        public void run()
        {
            float dist = endLocation.distanceTo(startLocation);

            tvDist.setText(String.valueOf(dist));

            handler.postDelayed(this, 0);
        }

    };

//    handles button onClick method change
    public void btnHandler(View view)
    {
        if(flag) // true when not tracking, start tracking
        {
            onClickStart(view);
            startStop.setText("STOP");
            binder.updateNoti(1);
            flag = false;
        } else { // false when tracking, stop tracking
            onClickStop(view);
            startStop.setText("START");
            binder.updateNoti(2);
            flag = true;
        }
    }

    public void onClickStart(View view)
    {
        startService(rtService); // start tracking

//        start timer
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
//        reset.setEnabled(false);

//        Get data from broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                Log.d(log, "onReceive broadcast");
                recDate = intent.getExtras().getString("recDate");
                startLocation = intent.getParcelableExtra("startLocation");
                endLocation = intent.getParcelableExtra("endLocation");

                handler.postDelayed(distRun, 0);
            }
        }, new IntentFilter("RTbroadcast"));

    }

    public void onClickStop(View view)
    {
        handler.removeCallbacks(runnable);

//        db handler
        rtdbHandler = new RTDBHandler(getBaseContext(), null, null, 1);
        distance = endLocation.distanceTo(startLocation);

//        adding new row into table
//        RunningTracker res = new RunningTracker(recDate, duration, distance);
//        rtdbHandler.addRT(res);
//        Log.d(log, "Record added.");

        Log.d(log, "datetime " + recDate);
        Log.d(log, "distance " + distance);

    }

//    public void onClickStats()
//    {
//        Intent detIntent = new Intent(this, );
//
//    }

//    check for permission, returns a bool
    public boolean checkPerm()
    {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);

            return false;
        } else {
            Toast.makeText(MainActivity.this,
                    "Permission (already) Granted!",
                    Toast.LENGTH_SHORT).show();

            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        mapView.onDestroy();
        unbindService(rtConn);
        super.onDestroy();
        Log.d(log, "MainActivity onDestoy");
    }
}
