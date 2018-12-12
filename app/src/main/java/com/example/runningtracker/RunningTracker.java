package com.example.runningtracker;

/*
*   RunningTracker object methods. To set/get the data from a RunningTracker object.
*/

public class RunningTracker {

    private int rt_id;
    private String rt_date;
    private float rt_dist;
    private float rt_time;

    public RunningTracker() {
    }

    public RunningTracker(int id, String date, float time, float dist) {
        this.rt_id = id;
        this.rt_date = date;
        this.rt_dist = dist;
        this.rt_time = time;
    }

    public RunningTracker(String date, float duration, float dist) {
        this.rt_date = date;
        this.rt_dist = dist;
        this.rt_time = duration;
    }

    //    return time of log only
    @Override
    public String toString() {
        String timestamp = this.getRt_date();
        return "Timestamp: " + timestamp.substring(11, 16);

    }

    public void setRt_id(int id) {
        this.rt_id = id;
    }

    public int getRt_id() {
        return this.rt_id;
    }

    public void setRt_date(String date) {
        this.rt_date = date;
    }

    public String getRt_date() {
        return this.rt_date;
    }

    public void setRt_time(float time) {
        this.rt_time = time;
    }

    public float getRt_time() {
        return this.rt_time;
    }

    public void setRt_dist(float dist) {
        this.rt_dist = dist;
    }

    public float getRt_dist() {
        return this.rt_dist;
    }

}
