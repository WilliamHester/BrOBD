package me.williamhester.brobd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;


/**
 * This activity is the one that the user will see when he/she opens the application. It launches a
 * new DriverSelectionFragment to allow them to select a driver then move to the LoggingActivity.
 *
 * @author William Hester
 */
public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (f == null) {
            f = DriverSelectionFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f, "DriverSelector")
                    .commit();
        }
    }

    /**
     * Called by the Driver selection Fragment to begin the data collection.
     *
     * @param driverName the string that represents the Driver's name
     */
    public void onDriverSelected(String driverName) {
        Intent service = new Intent(this, DriveLoggingService.class);
        Bundle extras = new Bundle();
        extras.putString("driver", driverName);
        service.putExtras(extras);
        startService(service);

        buildNotification();

        Intent i = new Intent(this, LoggingActivity.class);
        startActivity(i);
    }

    /**
     * Builds the notification that will show that runs when data is being collected by the
     * service. The "Stop" button on it will kill the service, and clicking it will bring
     * the user to the LoggingActivity, where they can see exactly what is going on currently.
     */
    private void buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_is_running))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true);

        Intent serviceKiller = new Intent(this, DriveLoggingService.class);
        Bundle args = new Bundle();
        args.putBoolean("stop", true);
        serviceKiller.putExtras(args);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceKiller, 0);
        builder.addAction(R.drawable.ic_stop_black_24dp, getResources().getString(R.string.stop),
                pendingIntent);


        Intent loggingIntent = new Intent(this, LoggingActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(loggingIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

}
