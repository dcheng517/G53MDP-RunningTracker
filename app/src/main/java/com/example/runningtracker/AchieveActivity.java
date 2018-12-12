package com.example.runningtracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

/*
*   Shows top speed, furthest distance travelled in 1 record in this activity
*   Shows total distance travelled in curr week/month/year
*/

public class AchieveActivity extends AppCompatActivity {

    private static final String log = "G53MDP";

    private RTDBHandler rtDB;

    private TextView topSpeed;
    private TextView topSpeedDate;
    private TextView topSpeedDist;
    private TextView topSpeedDur;
    private TextView topSpeedSpeed;

    private TextView topDist;
    private TextView topDistDate;
    private TextView topDistDist;
    private TextView topDistDur;
    private TextView topDistSpeed;

    private TextView totWeek;
    private TextView totMonth;
    private TextView totYear;

    private String recDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achieve);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        get curr date from StatsActivity
        Intent intent = getIntent();
        recDate = intent.getExtras().getString("recDate");

        rtDB = new RTDBHandler(this, null, null, 1);

//        Top speed and details
        topSpeed = (TextView) findViewById(R.id.topSpeed);
        topSpeedDate = (TextView) findViewById(R.id.detSpeedDate);
        topSpeedDist = (TextView) findViewById(R.id.detSpeedDist);
        topSpeedDur = (TextView) findViewById(R.id.detSpeedDur);
        topSpeedSpeed = (TextView) findViewById(R.id.detSpeedSpeed);

//        Furthest distance and details
        topDist = (TextView) findViewById(R.id.topDist);
        topDistDate = (TextView) findViewById(R.id.detDistDate);
        topDistDist = (TextView) findViewById(R.id.detDistDist);
        topDistDur = (TextView) findViewById(R.id.detDistDur);
        topDistSpeed = (TextView) findViewById(R.id.detDistSpeed);

//        Total distance travelled for week/month/year
        totWeek = (TextView) findViewById(R.id.distWeek);
        totMonth = (TextView) findViewById(R.id.distMonth);
        totYear = (TextView) findViewById(R.id.distYear);

//        Populate all fields wit data
        populateTopSpeed();
        populateTopDist();
        populateTot(recDate);

    }

//    populate top speed textviews
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void populateTopSpeed()
    {
        RunningTracker rtSpeed = rtDB.topSpeed();

        String date = rtSpeed.getRt_date();
        float dist = rtSpeed.getRt_dist();
        float time = rtSpeed.getRt_time();
        float speed = (dist/time) * 3600000; // km/ms to km/h

        topSpeed.setText(String.format("%.2f", speed) + "km/h");
        topSpeedDate.setText(date);
        topSpeedDist.setText(String.format("%.2f", dist) + "KM");
        topSpeedDur.setText(durFormat(time));
        topSpeedSpeed.setText(String.format("%.2f", speed) + "km/h");
    }

//    populate top distance textviews
@SuppressLint({"DefaultLocale", "SetTextI18n"})
public void populateTopDist()
    {
        RunningTracker rtDist = rtDB.maxDist();

        String date = rtDist.getRt_date();
        float dist = rtDist.getRt_dist();
        float time = rtDist.getRt_time();
        float speed = (dist/time) * 3600000; // km/ms to km/h

        topDist.setText(String.format("%.2f", dist) + "KM");
        topDistDate.setText(String.format(date));;
        topDistDist.setText(String.format("%.2f", dist) + "KM");
        topDistDur.setText(durFormat(time));
        topDistSpeed.setText(String.format("%.2f", speed) + "km/h");
    }

//    populate total distance textviews
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void populateTot(String date)
    {
        float week, month, year;

        week = rtDB.distTotal(date, 2); // week
        month = rtDB.distTotal(date, 3); // month
        year = rtDB.distTotal(date, 4); // year

        totWeek.setText(String.format("%.2f", week) + "KM");
        totMonth.setText(String.format("%.2f", month) + "KM");
        totYear.setText(String.format("%.2f", year) + "KM");
    }

//    format time in ms to min:s:ms
    public String durFormat(float time)
    {
        int Seconds = (int) (time / 1000);
        int Minutes = Seconds / 60;
        Seconds = Seconds % 60;
        int MilliSeconds = (int) (time % 1000);

        String res = "" + Minutes + ":"
                + String.format("%02d", Seconds) + ":"
                + String.format("%03d", MilliSeconds);

        return res;
    }

}
