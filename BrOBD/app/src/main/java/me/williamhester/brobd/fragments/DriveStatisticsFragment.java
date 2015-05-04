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
    private TextView mSpeed;
    private TextView mMaxSpeed;
    private Handler mHandler = new Handler();
    private Realm mRealm;

    private int mMaxMph;

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
        mMaxSpeed = (TextView) v.findViewById(R.id.max_speed);
        mChart = (LineChart) v.findViewById(R.id.speedGraph);

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

    private void onCreateGraph() {mChart.setOnChartValueSelectedListener(this);
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

            mMaxSpeed.setText("Max Speed: " + mMaxMph + " mph");

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
        set.setCircleColor(Color.RED);
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
        set.setCircleColor(Color.BLUE);
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
                    mSpeed.setText(latest.getSpeed() + " mph");
                    appendData(latest.getSpeed(), latest.getRpm());
                    if (latest.getSpeed() > mMaxMph) {
                        mMaxMph = latest.getSpeed();
                        mMaxSpeed.setText("Max Speed: " + mMaxMph + " mph");
                    }
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
