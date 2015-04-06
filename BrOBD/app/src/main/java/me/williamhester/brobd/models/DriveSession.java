package me.williamhester.brobd.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * This class represents one drive session, or a time that a driver started driving.
 *
 * @author William Hester
 */
public class DriveSession extends RealmObject {

    @PrimaryKey
    private long startTime;
    private Driver driver;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
