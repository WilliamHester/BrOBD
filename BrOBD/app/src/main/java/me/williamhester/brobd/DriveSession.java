package me.williamhester.brobd;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by william on 4/5/15.
 */
public class DriveSession extends RealmObject {

    @PrimaryKey
    private long startTime;
    private long endTime;
    private Driver driver;

    public DriveSession(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
