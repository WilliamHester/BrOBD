package me.williamhester.brobd;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;

import io.realm.Realm;

/**
 * This service runs in the background on a separate thread to collect information about the car.
 * The data is stored into DataPoints. The driver should be passed as a String as an extra in the
 * intent. When terminated, the service kills the notification that displays that it is running.
 *
 * @author William Hester
 */
public class DriveLoggingService extends Service {

    private Handler mHandler;
    private static final long LOGGING_INTERVAL = 1000L;
    private static int fakeSpeed = 0;
    private boolean mRunning = false;

    private Realm mRealm;

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("DriveLogger");
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        if (extras.getBoolean("stop")) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(0);
            stopSelf();
            return 0;
        }
        if (mRunning) {
            return 0;
        }

        final String driverName = extras.getString("driver");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Instantiate the Realm instance on the appropriate thread
                mRealm = Realm.getInstance(getApplicationContext());
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Driver driver = realm.where(Driver.class)
                                .equalTo("name", driverName)
                                .findFirst();
                        DriveSession session = realm.createObject(DriveSession.class);
                        session.setDriver(driver);
                        session.setStartTime(System.currentTimeMillis());
                    }
                });
            }
        });
        mHandler.post(mDriveLogger);
        mRunning = true;
        return 0;
    }

    private final Runnable mDriveLogger = new Runnable() {
        public void run() {
            long startTime = System.currentTimeMillis();
            // Get the current data and store it to the Realm
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Log.d("DriveStatisticsFragment", "creating Realm object");
                    DataPoint dataPoint = mRealm.createObject(DataPoint.class);
                    dataPoint.setDate(new Date(System.currentTimeMillis()));
                    dataPoint.setRpm((int) (Math.random() * 7000));
                    dataPoint.setSpeed(fakeSpeed++ % 90);
                }
            });

            long endTime = System.currentTimeMillis();

            mHandler.postDelayed(mDriveLogger, LOGGING_INTERVAL - (endTime - startTime) - 1);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mDriveLogger);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
