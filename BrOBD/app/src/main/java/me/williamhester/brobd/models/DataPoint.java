package me.williamhester.brobd.models;

import android.support.annotation.NonNull;

import java.util.Date;

import io.realm.RealmObject;

/**
 * This class represents exactly one interval at which the app is running.
 * It holds all of the information that could be collected about the car.
 *
 * @author William Hester
 */
public class DataPoint extends RealmObject {

    private Date date;
    private int speed;
    private int rpm;
    private float throttle;

    @NonNull
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getRpm() {
        return rpm;
    }

    public void setRpm(int rpm) {
        this.rpm = rpm;
    }

    public float getThrottle() {
        return throttle;
    }

    public void setThrottle(float throttle) {
        this.throttle = throttle;
    }
}
