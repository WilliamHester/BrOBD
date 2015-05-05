package me.williamhester.brobd.services;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import io.realm.Realm;
import me.williamhester.brobd.models.DataPoint;
import me.williamhester.brobd.models.DriveSession;
import me.williamhester.brobd.models.Driver;
import me.williamhester.brobd.singletons.BusManager;
import me.williamhester.obd.commands.SpeedObdCommand;
import me.williamhester.obd.commands.engine.EngineRPMObdCommand;
import me.williamhester.obd.commands.engine.ThrottlePositionObdCommand;
import me.williamhester.obd.commands.fuel.FuelConsumptionRateObdCommand;
import me.williamhester.obd.commands.protocol.EchoOffObdCommand;
import me.williamhester.obd.commands.protocol.LineFeedOffObdCommand;
import me.williamhester.obd.commands.protocol.SelectProtocolObdCommand;
import me.williamhester.obd.commands.protocol.TimeoutObdCommand;
import me.williamhester.obd.enums.ObdProtocols;

/**
 * This service runs in the background on a separate thread to collect information about the car.
 * The data is stored into DataPoints. The driver should be passed as a String as an extra in the
 * intent. When terminated, the service kills the notification that displays that it is running.
 *
 * @author William Hester
 */
public class DriveLoggingService extends Service {

    private static final long LOGGING_INTERVAL = 1000L;

    // The Handler that will control the logging loop.
    private Handler mHandler;
    private boolean mRunning = false;
    private Realm mRealm;
    private BluetoothSocket mSocket;
    private InputStream mIn;
    private OutputStream mOut;

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
                String address = getSharedPreferences("prefs", MODE_PRIVATE)
                        .getString("bluetooth_address", null);
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = btAdapter.getRemoteDevice(address);

                // Cancel any active device discovery, as it will crash the app if a connection
                //     is attempted when discovery is active.
                btAdapter.cancelDiscovery();

                try {
                    final Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                    mSocket = (BluetoothSocket) m.invoke(device, 1);
                    mSocket.connect();

                    mIn = mSocket.getInputStream();
                    mOut = mSocket.getOutputStream();

                    new EchoOffObdCommand().run(mIn, mOut);
                    new LineFeedOffObdCommand().run(mIn, mOut);
                    new TimeoutObdCommand(255).run(mIn, mOut);
                    new SelectProtocolObdCommand(ObdProtocols.AUTO).run(mIn, mOut);
                } catch (final IOException | InterruptedException | NoSuchMethodException |
                        IllegalAccessException | InvocationTargetException e) {
                    // TODO: Split these into more descriptive fail notifications, but for now
                    // Gotta catch 'em all
                    postFailedToUiThread();
                    return;
                }

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
        BusManager.getInstance().post(new LoggingStoppedEvent());

        // Kill the service (this)
        stopSelf();
    }

    private final Runnable mDriveLogger = new Runnable() {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            // Get the current data and store it to the Realm
            final EngineRPMObdCommand rpm = new EngineRPMObdCommand();
            final SpeedObdCommand speed = new SpeedObdCommand();
            final ThrottlePositionObdCommand throttle = new ThrottlePositionObdCommand();
            try {
                rpm.run(mIn, mOut);
                speed.run(mIn, mOut);
                throttle.run(mIn, mOut);
            } catch (IOException|InterruptedException e) {
                Log.d("DriveLoggingService", "Logging has failed");
                postFailedToUiThread();
                return;
            }
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    DataPoint dataPoint = mRealm.createObject(DataPoint.class);
                    dataPoint.setDate(new Date(System.currentTimeMillis()));
                    dataPoint.setRpm(rpm.getRPM());
                    dataPoint.setSpeed(Math.round(speed.getImperialSpeed()));
                    dataPoint.setThrottle(throttle.getPercentage());
                }
            });

            long endTime = System.currentTimeMillis();

            mHandler.postDelayed(mDriveLogger, LOGGING_INTERVAL - (endTime - startTime) - 1);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                // Well, we tried.
            }
        }
        mHandler.removeCallbacks(mDriveLogger);
    }

    private void postFailedToUiThread() {
        new Handler(Looper.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        stopService();
                        BusManager.getInstance().post(new LoggingStoppedEvent());
                    }
                });
    }

    private void toastToUiThread(final String message) {
        new Handler(Looper.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DriveLoggingService.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * @return a float.
     */
    public float getMpg(SpeedObdCommand speed, FuelConsumptionRateObdCommand fuel) {
        return 235.2f / (100 / speed.getMetricSpeed()) * fuel.getLitersPerHour();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class LoggingStoppedEvent {}

    public static class CouldNotConnectEvent {
        public CouldNotConnectEvent() {
            Log.d("CouldNotConnectEvent", "Could not connect to the specified address");
        }
    }

}
