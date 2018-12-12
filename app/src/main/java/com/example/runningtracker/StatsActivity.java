package com.example.runningtracker;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/*
*   Shows all records for the day selected in a ListView. Uses a CalendarView to select date.
*   Shows total distance travelled for the day on the bottom right of the screen.
*   Able to navigate to the AchieveActivity to see top records
*/

public class StatsActivity extends AppCompatActivity {

    private static final String log = "G53MDP";

    private RTDBHandler rtDB;

    private CalendarView cv;

    private ListView lv;
    private TextView empty;

    private ImageButton btnTrophy;

    private TextView dist;
    private TextView duration;
    private TextView speed;
    private TextView totDay;

    private String recDate;
    private String selDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        rtDB = new RTDBHandler(this, null, null, 1);

//        retrieve curr date from MainActivity
        Intent intent = getIntent();
        recDate = intent.getExtras().getString("recDate");

        lv = (ListView) findViewById(R.id.list);
        empty = findViewById(R.id.empty);

        dist = (TextView) findViewById(R.id.topDist);
        duration = (TextView) findViewById(R.id.duration);
        speed = (TextView) findViewById(R.id.speed);
        totDay = (TextView) findViewById(R.id.totDay);

//        populate ListView with records for the day
        setLogsList(recDate);

//        CalendarView listener, used to select date to show data
        cv = (CalendarView) findViewById(R.id.cv);
        cv.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
//                set date to yyyy-MM-dd
//                month+1 as it is indexed from 0, +1 to get th real current month
                selDate = year + "-" + String.format("%02d", (month+1)) + "-" + String.format("%02d", dayOfMonth);
                setLogsList(selDate);
                populateTV(0); // clear the textviews
            }
        });

//        to start the achievement activity
        btnTrophy = (ImageButton) findViewById(R.id.btnTrophy);
        btnTrophy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent record = new Intent(StatsActivity.this, AchieveActivity.class);
                record.putExtra("recDate", recDate);
                startActivity(record);
            }
        });

    }

    //    set listview with all logs for the day
    public void setLogsList(String selDate) {

        Log.d(log, "selDate " + selDate);
        List<RunningTracker> allLogs = rtDB.allLogs(selDate);
        float totDistDay = rtDB.distTotal(selDate, 1);

        if (!allLogs.isEmpty()) // if there are records for the day
        {
//            show total distance for the day selected
            totDay.setText(String.format("%.2f", totDistDay) + "KM");

            lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allLogs));
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt,
                                        long mylng) {
                    RunningTracker selectedFromList = (RunningTracker) (lv.getItemAtPosition(myItemInt));
                    Log.d(log, "Selected runningtracker with ID: " + Integer.toString(selectedFromList.getRt_id()));

                    populateTV(selectedFromList.getRt_id());
                }
            });

//            long click listener for record deletion
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    RunningTracker selectedFromList = (RunningTracker) (lv.getItemAtPosition(position));
                    delete(selectedFromList.getRt_id());

                    return false;
                }
            });

        } else{
//            show a "No records" message if no records for day selected
            lv.setAdapter(null);
            lv.setEmptyView(empty);
            totDay.setText("No records.");
        }

    }

//    populate the textview with the info from db
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void populateTV(int id)
    {
        if (id != 0)
        {
            RunningTracker curr = rtDB.queryRec(id);

            float time = curr.getRt_time();
            float distance = curr.getRt_dist(); // distance in KM

            float Speed = (distance / time) * 3600000; // convert from m/sec to km/h

            dist.setText(String.format("%.2f", distance) + "KM");
            duration.setText(durFormat(curr.getRt_time()));
            speed.setText(String.format("%.2f", Speed) + "km/h");

        } else if (id == 0) // if no records selected, reset all fields
        {
            dist.setText("");
            duration.setText("");
            speed.setText("");
        }

    }

//        long hold on list item to alert user for deletion confirmation
    public void delete(final int del_id)
    {
        AlertDialog.Builder delAlert = new AlertDialog.Builder(this);
        delAlert
                .setTitle("Confirm deletion?")
                .setMessage("Deleted records will not be recoverable." +
                        "Are you sure you want to delete this record?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rtDB.deleteRT(del_id);
                        Log.d(log, "deleteRunningTracker: " + del_id);
                        Toast.makeText(StatsActivity.this, "Log deleted", Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(getIntent()); // to stay on StatsActivity
                    }
                })
                .setNegativeButton("No", null);
        delAlert.show();
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
