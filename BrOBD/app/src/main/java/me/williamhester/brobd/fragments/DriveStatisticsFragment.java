package me.williamhester.brobd.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import me.williamhester.brobd.R;
import me.williamhester.brobd.models.DataPoint;
import me.williamhester.brobd.models.DataPointWrapper;
import me.williamhester.brobd.models.DriveSession;

/**
 * This fragment shows live data about the current Drive Session.
 *
 * @author William Hester
 */
public class DriveStatisticsFragment extends Fragment {

    // Update the display every second
    private static final long INTERVAL_MS = 500;
    private static final int MAX_SPEED_DATA_POINTS = 101;

    private GraphView mGraph;
    private TextView mSpeed;
    private Handler mHandler = new Handler();
    private Realm mRealm;

    private LineGraphSeries<DataPointWrapper> mSeries;

    public static DriveStatisticsFragment newInstance() {
        return new DriveStatisticsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRealm = Realm.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_drive_statistics, container, false);
        mSpeed = (TextView) v.findViewById(R.id.speed);
        mGraph = (GraphView) v.findViewById(R.id.speedGraph);
        onCreateGraph();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        mHandler.post(mStatsRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mGraph = null;
        mHandler.removeCallbacks(mStatsRunnable);
    }

    private void onCreateGraph() {
        Date date = mRealm.where(DriveSession.class)
                .maximumDate("startTime");
        List<DataPoint> points = mRealm.where(DataPoint.class)
                .greaterThan("date", date)
                .findAllSorted("date");

        // get the last 30 data points
        int start = points.size() >= MAX_SPEED_DATA_POINTS ? points.size() - MAX_SPEED_DATA_POINTS : 0;
        points = points.subList(start, points.size());

        mSeries = new LineGraphSeries<>(DataPointWrapper.fromDataPoints(points));
        mGraph.addSeries(mSeries);

        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0.0);
        mGraph.getViewport().setMaxY(120.0);

        mGraph.getViewport().setXAxisBoundsManual(true);
        double min = System.currentTimeMillis() / 1000L - 1000L; // Start seconds
        double max = System.currentTimeMillis() / 1000L - 1000L + MAX_SPEED_DATA_POINTS - 1; // end seconds
        Log.d("DriveStatisticsFragment", "min=" + min + " max=" + max);

        mGraph.getViewport().setMinX(min);
        mGraph.getViewport().setMaxX(max);
        mGraph.getViewport().setScrollable(true);

        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    long num = (long) value;
                    return new SimpleDateFormat("HH:mm:ss").format(new Date(num * 1000L));
                } else {
                    return String.valueOf((long) value);
                }
            }
        });
    }

    private Runnable mStatsRunnable = new Runnable() {
        private Date mLatestDate;

        @Override
        public void run() {
            // Get the data from the Realm and put the latest speed and RPM into their
            //      respective TextViews.
            long startTime = System.currentTimeMillis();

            Date tempLatest = mRealm.where(DataPoint.class)
                    .maximumDate("date");
            if (tempLatest != null && (mLatestDate == null || tempLatest.compareTo(mLatestDate) > 0)) {
                DataPoint latest = mRealm.where(DataPoint.class)
                        .equalTo("date", tempLatest)
                        .findFirst();
                if (latest != null) {
                    mSpeed.setText(latest.getSpeed() + " mph");
                }

                mLatestDate = tempLatest;
                DataPointWrapper wrapper = new DataPointWrapper(latest);
                mSeries.appendData(wrapper, true, MAX_SPEED_DATA_POINTS);
                Log.d("DriveStatisticsFragment", "x=" + wrapper.getX());
            }

            long endTime = System.currentTimeMillis();

            mHandler.postDelayed(mStatsRunnable, INTERVAL_MS - (endTime - startTime) - 1);
        }
    };
}
