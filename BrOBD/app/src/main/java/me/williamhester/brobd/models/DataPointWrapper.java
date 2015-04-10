package me.williamhester.brobd.models;

import com.jjoe64.graphview.series.DataPointInterface;

import java.util.List;

/**
 * Created by william on 4/6/15.
 */
public class DataPointWrapper implements DataPointInterface {

    private double speed;
    private double time;

    public static DataPointWrapper[] fromDataPoints(List<DataPoint> points) {
        DataPointWrapper[] wrappers = new DataPointWrapper[points.size()];
        for (int i = 0; i < points.size(); i++) {
            wrappers[i] = new DataPointWrapper(points.get(i));
        }
        return wrappers;
    }

    public DataPointWrapper(DataPoint dataPoint) {
        speed = dataPoint.getSpeed();
        time = dataPoint.getDate().getTime();
    }

    @Override
    public double getX() {
        return time / 1000;
    }

    @Override
    public double getY() {
        return speed;
    }
}
