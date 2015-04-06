package me.williamhester.brobd.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import me.williamhester.brobd.R;
import me.williamhester.brobd.fragments.DriverSelectionFragment;
import me.williamhester.brobd.services.DriveLoggingService;


/**
 * This activity is the one that the user will see when he/she opens the application. It launches a
 * new DriverSelectionFragment to allow them to select a driver then move to the LoggingActivity.
 *
 * @author William Hester
 */
public class MainActivity extends ActionBarActivity {

    public static final int REQUEST_ENABLE_BT = 1;

    private String mDriver;

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
        if (savedInstanceState != null) {
            mDriver = savedInstanceState.getString("driver");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bluetooth) {
            showBluetoothPicker();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("driver", mDriver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            // Handle the request to enable bluetooth
            if (resultCode == RESULT_OK) {
                showBluetoothPicker();
            } else {
                // If the user canceled the request, tell them that they can't do anything
                //    until they accept the request.
                Toast.makeText(this, R.string.you_must_enable_bluetooth, Toast.LENGTH_LONG).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Called by the Driver selection Fragment to begin the data collection.
     *
     * @param driverName the string that represents the Driver's name
     */
    public void onDriverSelected(String driverName) {
        final SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String selectedDevice = prefs.getString("bluetooth_address", null);
        if (selectedDevice == null) {
            mDriver = driverName;
            showBluetoothPicker();
            return;
        }
        mDriver = null;

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
     * Shows the dialog to allow the user to select the serial device they wish to use.
     */
    private void showBluetoothPicker() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        final ArrayList<String> deviceAddresses = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                names.add(device.getName());
                deviceAddresses.add(device.getAddress());
            }
        }

        final SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String selectedDevice = prefs.getString("bluetooth_address", null);
        int pos = deviceAddresses.indexOf(selectedDevice);

        new AlertDialog.Builder(this)
                .setSingleChoiceItems(names.toArray(new String[names.size()]), pos, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        int position = ((AlertDialog) d).getListView().getCheckedItemPosition();
                        if (position >= 0) {
                            String deviceAddress = deviceAddresses.get(position);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("bluetooth_address", deviceAddress);
                            editor.commit();
                            if (mDriver != null) {
                                onDriverSelected(mDriver);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setTitle(R.string.select_bluetooth_device)
                .show();
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
