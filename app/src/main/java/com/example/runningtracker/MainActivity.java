package com.example.runningtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/*
*   Main activity of app, containing a MapView showing the path travelled,
*   a stopwatch, a distance tracker and a speed indicator.
*
*   Binds to  RTService.java
*
*   Opens up the Stats Activity with the STATS button
*
*/

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final String log = "G53MDP";
    private final int REQUEST_PERMISSION_LOCATION = 1;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    Intent rtService;
    RTDBHandler rtdbHandler;
    RTService.rtBinder binder;
    private ServiceConnection rtConn = null;

//    Map variables
    private Polyline polyline;
    private ArrayList<LatLng> polyList;
    private List<Polyline> oldPoly;
    private MapView mapView;
    private GoogleMap gmap;
    private LatLng startLL;
    private LatLng endLL;

    private TextView stopwatch;
    private TextView tvDist;
    private TextView tvSpeed;
    private Button start;
    private Button pause;
    private Button save;

//    db handler to get topSpeed and maxDist
    private RTDBHandler rtDB;

//    runnable variables
    Handler handler;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int Seconds, Minutes, MilliSeconds ;

    float dist = 0;
    float distBuff = 0;
    float distTotal = 0;

    float speed = 0f;

    String recDate; // curr date

    Location startLocation;
    Location endLocation;

    float distance;
    float duration = 0;

    private boolean tracking = false;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
        } else {
//            Toast.makeText(MainActivity.this,
//                    "Permission (already) Granted!",
//                    Toast.LENGTH_SHORT).show();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        rtService = new Intent(this, RTService.class);
        rtDB = new RTDBHandler(this, null, null, 1); // db handler

        handler = new Handler(); // runnable handler

//        Polylines list
        polyList = new ArrayList<LatLng>(); // new tracks to be drawn
        oldPoly = new ArrayList<Polyline>(); // list of drawn tracks on map

        //        Get data from broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                Log.d(log, "onReceive broadcast");
                recDate = intent.getExtras().getString("recDate");
                if(startLocation==null) startLocation = intent.getParcelableExtra("startLocation");
                endLocation = intent.getParcelableExtra("endLocation");

                try{
                    endLL = new LatLng(endLocation.getLatitude(), endLocation.getLongitude());
                    moveCamera(endLL, 18.5f);
                }catch (NullPointerException e)
                {
                    Log.d(log, e.toString());
                }
            }
        }, new IntentFilter("RTbroadcast"));

        try{
            startLL = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
            polyList.add(startLL);
        }catch (NullPointerException e)
        {
            Log.d(log, e.toString());
        }

//        initiate map
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

//        connecting to the service
        if (rtConn != null) unbindService(rtConn);
        rtConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (RTService.rtBinder) service;
                Log.d(log, "Service Connected.");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(log, "Service Disconnected");
            }
        };
        bindService(rtService, rtConn, BIND_AUTO_CREATE);

        startService(rtService);

        stopwatch = (TextView) findViewById(R.id.tvStopwatch);
        tvDist = (TextView) findViewById(R.id.tvDistShow);
        tvSpeed = (TextView) findViewById(R.id.tvSpeedShow);
        start = (Button) findViewById(R.id.btnStart);
        pause = (Button) findViewById(R.id.btnPause);
        save = (Button) findViewById(R.id.btnSave);


    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMyLocationEnabled(true);
        gmap.setMinZoomPreference(18.5f);
        gmap.getUiSettings().setZoomControlsEnabled(true);
    }

//    moves camera on map and draws path
    public void moveCamera(LatLng latLng, float zoom) {
        polyList.add(endLL);

        //Google Map view animation when location is changed
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        gmap.animateCamera(cameraUpdate);

        PolylineOptions opt = new PolylineOptions();
        opt.width(4);
        opt.color(Color.BLUE);
        opt.geodesic(true);

//        only draw lines if it's tracking
        if (tracking)
        {
            // add lines to be drawn
            for (int i = 0; i<polyList.size(); i++)
            {
                LatLng point = polyList.get(i);
                opt.add(point);
            }

            polyline = gmap.addPolyline(opt); // drawing the lines

            oldPoly.add(polyline); // add drawn lines to a list to keep track, for removal
        }
    }

//    Stopwatch runnable
    public Runnable runStopwatch = new Runnable()
    {
        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime; // buffer for when paused

//            format ms to min:s:ms
            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);

//            set stopwatch text
            stopwatch.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }
    };

//    runnable for the distance travelled textview
    public Runnable runDist = new Runnable()
    {
        public void run()
        {
//            get distance travelled
            try{
                dist = endLocation.distanceTo(startLocation);
            } catch (NullPointerException e)
            {
                Log.d(log, e.toString());
            }

            distTotal = distBuff + dist; // buffer for when paused

            distTotal = (distTotal/1000); // format distance value to KM

            tvDist.setText(String.format("%.2f", distTotal) + "KM");

            handler.postDelayed(this, 0);
        }
    };

//        runnable for the speed textview
    public Runnable runSpeed = new Runnable()
    {
        public void run()
        {
            speed = (distTotal/UpdateTime) * 3600000;

            tvSpeed.setText(String.format("%.2f", speed) + "km/h");

//            delay by 2000ms to get more stable speed value
            handler.postDelayed(this, 2000);
        }
    };

    public void onClickStart(View view)
    {
        resetRunnables(); // reset all values if was stopped previously

        startService(rtService); // start tracking

        tracking = true;
        isPaused = false;

//        start timer
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runStopwatch, 0);
//        start distance runnable
        handler.postDelayed(runDist, 0);
//        start speed runnable, delay by 2000ms to get a more stable speed value
        handler.postDelayed(runSpeed, 2000);

//        show PAUSE and STOP buttons, hide START button
        start.setVisibility(View.INVISIBLE);
        pause.setVisibility(View.VISIBLE);
        pause.setText("PAUSE");
        save.setVisibility(View.VISIBLE);

        binder.updateNoti(1);
    }

    public void onClickPause(View view)
    {
        if(!isPaused) // if not PAUSED
        {
            tracking = false;
            isPaused = true;

//            Pause stopwatch
            TimeBuff = TimeBuff + MillisecondTime;
            handler.removeCallbacks(runStopwatch);
//            Pause distance runnable
            distBuff = distBuff + dist;
            handler.removeCallbacks(runDist);
//            Pause speed runnable
            handler.removeCallbacks(runSpeed);

            pause.setText("RESUME"); // set button text to RESUME

            binder.updateNoti(2);

        } else // if PAUSED
        {
            onClickStart(view); // resume service``
            tracking = true;
            isPaused = false;

//            change button text to PAUSE
            pause.setText("PAUSE");

            binder.updateNoti(1);
        }
    }

    public void onClickSave(View view)
    {
        tracking = false;
        isPaused = false;

//        stop stopwatch and distance and speed runnable
        handler.removeCallbacks(runStopwatch);
        handler.removeCallbacks(runDist);
        handler.removeCallbacks(runSpeed);

//        show START button, hide PAUSE and STOP buttons
        start.setVisibility(View.VISIBLE);
        pause.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);

//        db handler
        rtdbHandler = new RTDBHandler(this, null, null, 1);
        distance = distTotal;
        duration = UpdateTime;

        compRec(distance, duration);

//            adding new row into table
        RunningTracker res = new RunningTracker(recDate, duration, distance);
        rtdbHandler.addRT(res);
        Toast.makeText(MainActivity.this, "Record saved!", Toast.LENGTH_SHORT).show();

        binder.updateNoti(3);
    }

//    starts StatsActivity
    public void onClickStats(View view)
    {
        Intent detIntent = new Intent(this, StatsActivity.class);
        detIntent.putExtra("recDate", recDate); // pass current date to StatsActivity
        startActivity(detIntent);
    }

//    reset button method
    public void onClickReset(View view)
    {
//        cannot reset if tracking
        if (tracking)
            Toast.makeText(MainActivity.this, "Cannot reset while running.", Toast.LENGTH_SHORT).show();
        else
        {
            resetRunnables();

//              show START button, hide PAUSE and STOP buttons
            start.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
            save.setVisibility(View.INVISIBLE);
        }
    }

//    reset all variables in Stopwatch and Distance runnables
    public void resetRunnables()
    {
//        Stopwatch variables
        MillisecondTime = 0L ;
        StartTime = 0L ;
        TimeBuff = 0L ;
        UpdateTime = 0L ;
        Seconds = 0 ;
        Minutes = 0 ;
        MilliSeconds = 0 ;
        stopwatch.setText("00:00:00");

//        Distance variables
        dist = 0f;
        distBuff = 0f;
        distTotal = 0f;
        startLocation = null;
        endLocation = null;
        tvDist.setText("0.00KM");

//        Speed variables
        speed = 0f;
        tvSpeed.setText("0.00km/h");

//        clear off path lines on map
        for(Polyline line : oldPoly)
        {
            line.remove();
        }
        polyList.clear();
        oldPoly.clear();
    }

//    compare new records with current records
    public void compRec(float newDist, float newDur)
    {
        rtDB = new RTDBHandler(this, null, null, 1);
//        current distance record
        RunningTracker currDist = rtDB.maxDist();
        float maxDist = currDist.getRt_dist();

//        current speed record
        RunningTracker currSpeed = rtDB.topSpeed();
        float dist = currSpeed.getRt_dist();
        float time = currSpeed.getRt_time();
        float topSpeed = (dist/time) * 3600000;
        float newSpeed = (newDist/newDur) * 3600000;

        if (newDist > maxDist && newSpeed > topSpeed) // if both records broken
        {
            Toast.makeText(MainActivity.this,
                    "WOW! You've just set a new record for furthest distance travelled with "
                    + String.format("%.2f", newDist)  + "KM and a new top speed at "
                    + String.format("%.2f", newSpeed) +"km/h!", Toast.LENGTH_LONG).show();
        } else if(newDist > maxDist) // if max distance record broken
        {
            Toast.makeText(MainActivity.this, "WOW! You've just set a new record for furthest distance travelled with " + String.format("%.2f", newDist) + "KM!", Toast.LENGTH_LONG).show();
        } else if(newSpeed > topSpeed) // if top speed record broken
        {
            Toast.makeText(MainActivity.this, "WOW! You've just set a new record of your top speed with " + String.format("%.2f", newSpeed) + "km/h!", Toast.LENGTH_LONG).show();
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
        stopService(rtService);
        unbindService(rtConn);
        super.onDestroy();
        Log.d(log, "MainActivity onDestoy");
        finish();
    }
}