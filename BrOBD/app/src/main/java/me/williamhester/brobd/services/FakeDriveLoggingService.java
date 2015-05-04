package me.williamhester.brobd.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.Date;

import io.realm.Realm;
import me.williamhester.brobd.models.DataPoint;
import me.williamhester.brobd.models.DriveSession;
import me.williamhester.brobd.models.Driver;
import me.williamhester.brobd.singletons.BusManager;

/**
 * This service runs in the background on a separate thread to collect information about the car.
 * The data is stored into DataPoints. The driver should be passed as a String as an extra in the
 * intent. When terminated, the service kills the notification that displays that it is running.
 *
 * @author William Hester
 */
public class FakeDriveLoggingService extends Service {

    private static final long LOGGING_INTERVAL = 1000L;

    // The Handler that will control the logging loop.
    private Handler mHandler;
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
            stopService();
            return 0;
        }
        if (mRunning) {
            // Don't restart the service.
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
                        session.setStartTime(new Date(System.currentTimeMillis()));
                    }
                });
                mRunning = true;
                mHandler.post(mDriveLogger);
            }
        });
        return 0;
    }

    private void stopService() {
        // Remove the notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);

        // Kill the LoggingActivity
        BusManager.getInstance().post(new DriveLoggingService.LoggingStoppedEvent());

        // Kill the service (this)
        stopSelf();
    }

    private final Runnable mDriveLogger = new Runnable() {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    DataPoint dataPoint = mRealm.createObject(DataPoint.class);
                    dataPoint.setDate(new Date(System.currentTimeMillis()));
                    dataPoint.setRpm((int) (15 * (System.currentTimeMillis() / 100 % 60)));
                    dataPoint.setSpeed((int) ((System.currentTimeMillis() / 1000 % 60)));
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

    private void postFailedToUiThread() {
        new Handler(Looper.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        stopService();
                        BusManager.getInstance().post(new DriveLoggingService.LoggingStoppedEvent());
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class CouldNotConnectEvent {
        public CouldNotConnectEvent() {
            Log.d("CouldNotConnectEvent", "Could not connect to the specified address");
        }
    }

}
