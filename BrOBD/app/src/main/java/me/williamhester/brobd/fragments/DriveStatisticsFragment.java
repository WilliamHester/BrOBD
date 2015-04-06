package me.williamhester.brobd.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

import io.realm.Realm;
import me.williamhester.brobd.R;
import me.williamhester.brobd.models.DataPoint;

/**
 * This fragment shows live data about the current Drive Session.
 *
 * @author William Hester
 */
public class DriveStatisticsFragment extends Fragment {

    // Update the display every second
    private static final long INTERVAL_MS = 500;

    private TextView mSpeed;
    private Handler mHandler = new Handler();
    private Realm mRealm;

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

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        mHandler.post(mStatsRunnable);
    }

    private Runnable mStatsRunnable = new Runnable() {
        @Override
        public void run() {
            // Get the data from the Realm and put the latest speed and RPM into their
            //      respective TextViews.
            Date latestDate = mRealm.where(DataPoint.class)
                    .maximumDate("date");
            if (latestDate != null) {
                DataPoint latest = mRealm.where(DataPoint.class)
                        .equalTo("date", latestDate)
                        .findFirst();
                if (latest != null) {
                    mSpeed.setText("" + latest.getSpeed());
                }
            }

            mHandler.postDelayed(mStatsRunnable, INTERVAL_MS);
        }
    };
}
