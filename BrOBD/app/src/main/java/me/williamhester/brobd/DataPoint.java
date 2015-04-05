package me.williamhester.brobd;

import io.realm.RealmObject;

/**
 * Created by william on 4/5/15.
 */
public class DataPoint extends RealmObject {

    private int speed;
    private int rpm;
    private long epochTime;

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

    public long getEpochTime() {
        return epochTime;
    }

    public void setEpochTime(long epochTime) {
        this.epochTime = epochTime;
    }
}
