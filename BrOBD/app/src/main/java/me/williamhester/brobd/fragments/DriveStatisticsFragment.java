package me.williamhester.brobd.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Highlight;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import me.williamhester.brobd.R;
import me.williamhester.brobd.models.DataPoint;
import me.williamhester.brobd.models.DriveSession;

/**
 * This fragment shows live data about the current Drive Session.
 *
 * @author William Hester
 */
public class DriveStatisticsFragment extends Fragment implements OnChartValueSelectedListener {

    // Update the display every second
    private static final long INTERVAL_MS = 500;
    private static final int MAX_DATA_POINTS = 101;

    private LineChart mChart;
    private TextView mAverageSpeedText;
    private TextView mCurrentSpeedText;
    private TextView mMaxSpeedText;
    private TextView mCurrentDistanceText;
    private TextView mAverageRpmText;
    private TextView mMaxRpmText;
    private TextView mAverageThrottleText;
    private TextView mMaxThrottleText;
    private TextView mElapsedTimeText;
    private Handler mHandler = new Handler();
    private Realm mRealm;

    private int mMaxSpeed;
    private int mMaxRpm;
    private float mMaxThrottle;
    private int mMaxMph;
    private double mAverageSpeedSum;
    private double mAverageRpmSum;
    private double mAverageThrottleSum;
    private int mDataPointCount;

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
        mCurrentSpeedText = (TextView) v.findViewById(R.id.live_speed);
        mMaxSpeedText = (TextView) v.findViewById(R.id.max_speed);
        mChart = (LineChart) v.findViewById(R.id.speedGraph);
        mCurrentDistanceText = (TextView) v.findViewById(R.id.live_distance);

        mAverageSpeedText = (TextView) v.findViewById(R.id.average_speed);
        mAverageRpmText = (TextView) v.findViewById(R.id.average_rpm);
        mAverageThrottleText = (TextView) v.findViewById(R.id.average_throttle);

        mMaxSpeedText = (TextView) v.findViewById(R.id.max_speed);
        mMaxRpmText = (TextView) v.findViewById(R.id.max_rpm);
        mMaxThrottleText = (TextView) v.findViewById(R.id.max_throttle);

        mElapsedTimeText = (TextView) v.findViewById(R.id.live_time);
        initAveragesAndMaxima();

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

        mChart = null;
        mHandler.removeCallbacks(mStatsRunnable);
    }

    private void initAveragesAndMaxima() {
        Date date = mRealm.where(DriveSession.class)
                .maximumDate("startTime");
        List<DataPoint> points = mRealm.where(DataPoint.class)
                .greaterThan("date", date)
                .findAllSorted("date");

        for (DataPoint p : points) {
            mAverageSpeedSum += p.getSpeed();
            mAverageRpmSum += p.getRpm();
            mAverageThrottleSum += p.getThrottle();

            if (p.getSpeed() > mMaxSpeed) {
                mMaxSpeed = p.getSpeed();
            }
            if (p.getRpm() > mMaxRpm) {
                mMaxRpm = p.getRpm();
            }
            if (p.getThrottle() > mMaxThrottle) {
                mMaxThrottle = p.getThrottle();
            }
        }
        mMaxSpeedText.setText(Math.round(mMaxSpeed) + " MPH");
        mMaxRpmText.setText(Math.round(mMaxRpm) + " RPM");
        mMaxThrottleText.setText(String.format("%.2f%s", mMaxThrottle, "%"));

        mDataPointCount = points.size();

        updateAveragesAndCurrents();
    }

    private void updateAveragesAndCurrents() {
        mAverageSpeedText.setText(Math.round(mAverageSpeedSum / mDataPointCount) + " MPH");
        mAverageRpmText.setText(Math.round(mAverageRpmSum / mDataPointCount) + " RPM");
        mAverageThrottleText.setText(Math.round(mAverageThrottleSum / mDataPointCount) + "%");

        mCurrentDistanceText.setText(String.format("%.2f miles driven", mAverageSpeedSum / 3600.0));
        mElapsedTimeText.setText(
                "Elapsed time: "
                        + String.valueOf(mDataPointCount / 3600) + ':'
                        + String.format("%02d", mDataPointCount / 60 % 60) + ':'
                        + String.format("%02d", mDataPointCount % 60)
        );
    }

    private void onCreateGraph() {
        mChart.setOnChartValueSelectedListener(this);
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        mChart.setDescription("");

        LineData data = new LineData();

        mChart.setData(data);

        initData();

        Typeface tf = Typeface.DEFAULT;

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTypeface(tf);
    }

    private void initData() {
        LineData data = mChart.getData();

        if (data != null) {
            LineDataSet speedSet = data.getDataSetByIndex(0);
            LineDataSet rpmSet = data.getDataSetByIndex(1);

            if (speedSet == null) {
                speedSet = createSpeedSet();
                data.addDataSet(speedSet);
            }

            if (rpmSet == null) {
                rpmSet = createRpmSet();
                data.addDataSet(rpmSet);
            }

            Date date = mRealm.where(DriveSession.class)
                    .maximumDate("startTime");
            List<DataPoint> points = mRealm.where(DataPoint.class)
                    .greaterThan("date", date)
                    .findAllSorted("date");

            mMaxMph = mRealm.where(DataPoint.class)
                    .greaterThan("date", date)
                    .findAllSorted("date")
                    .max("speed")
                    .intValue();

            mMaxSpeedText.setText(mMaxMph + " MPH");

            // get the last 30 data points
            int start = points.size() >= MAX_DATA_POINTS ? points.size() - MAX_DATA_POINTS : 0;
            points = points.subList(start, points.size());

            for (DataPoint p : points) {
                data.addXValue("");
                data.addEntry(new Entry(p.getSpeed(), speedSet.getEntryCount()), 0);
                data.addEntry(new Entry(p.getRpm(), rpmSet.getEntryCount()), 1);
            }

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRange(MAX_DATA_POINTS);
            mChart.moveViewToX(data.getXValCount() - MAX_DATA_POINTS - 1);
        }
    }

    private void appendData(int speed, int rpm) {
        LineData data = mChart.getData();

        if (data != null) {

            LineDataSet speedSet = data.getDataSetByIndex(0);
            LineDataSet rpmSet = data.getDataSetByIndex(1);

            // add a new x-value first
            data.addXValue("");
            data.addEntry(new Entry(speed, speedSet.getEntryCount()), 0);
            data.addEntry(new Entry(rpm, rpmSet.getEntryCount()), 1);

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRange(MAX_DATA_POINTS);
            mChart.moveViewToX(data.getXValCount() - MAX_DATA_POINTS - 1);
        }
    }

    private LineDataSet createSpeedSet() {
        LineDataSet set = new LineDataSet(null, "Speed");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set;
    }

    private LineDataSet createRpmSet() {
        LineDataSet set = new LineDataSet(null, "RPM");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return set;
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
                    mCurrentSpeedText.setText(latest.getSpeed() + " MPH");
                    appendData(latest.getSpeed(), latest.getRpm());

                    mAverageSpeedSum += latest.getSpeed();
                    mAverageRpmSum += latest.getRpm();
                    mAverageThrottleSum += latest.getThrottle();

                    if (latest.getSpeed() > mMaxSpeed) {
                        mMaxSpeed = latest.getSpeed();
                        mMaxSpeedText.setText(mMaxMph + " MPH");
                    }
                    if (latest.getRpm() > mMaxRpm) {
                        mMaxRpm = latest.getRpm();
                        mMaxRpmText.setText(mMaxRpm + " RPM");
                    }
                    if (latest.getThrottle() > mMaxThrottle) {
                        mMaxThrottle = latest.getThrottle();
                        mMaxThrottleText.setText(String.format("%.2f%s", mMaxThrottle, "%"));
                    }
                    if (latest.getSpeed() > mMaxMph) {
                        mMaxMph = latest.getSpeed();
                    }
                    mDataPointCount++;

                    updateAveragesAndCurrents();
                }
                mLatestDate = tempLatest;
            }

            long endTime = System.currentTimeMillis();

            mHandler.postDelayed(mStatsRunnable, INTERVAL_MS - (endTime - startTime) - 1);
        }
    };

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
