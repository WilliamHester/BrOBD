package me.williamhester.brobd.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * This class represents one drive session, or a time that a driver started driving.
 *
 * @author William Hester
 */
public class DriveSession extends RealmObject {

    private Date startTime;
    private Driver driver;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
