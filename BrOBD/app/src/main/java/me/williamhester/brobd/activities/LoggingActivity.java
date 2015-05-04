package me.williamhester.brobd.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import me.williamhester.brobd.R;
import me.williamhester.brobd.fragments.DriveStatisticsFragment;
import me.williamhester.brobd.services.DriveLoggingService;
import me.williamhester.brobd.services.FakeDriveLoggingService;
import me.williamhester.brobd.singletons.BusManager;

/**
 * This is the activity that is running when a user wants to see their current data and the
 * DriveLoggingService is running. Going back from this activity will stop the logging service.
 *
 * @author William Hester
 */
public class LoggingActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bus bus = BusManager.getInstance();
        bus.register(this);

        setContentView(R.layout.activity_container);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (f == null) {
            f = DriveStatisticsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f, "DriveStats")
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.stop_data_collection)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent serviceKiller = new Intent(LoggingActivity.this,
                                FakeDriveLoggingService.class);
                        Bundle args = new Bundle();
                        args.putBoolean("stop", true);
                        serviceKiller.putExtras(args);
                        startService(serviceKiller);
                        LoggingActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Bus bus = BusManager.getInstance();
        bus.unregister(this);
    }

    /**
     * Called by the DriveLoggingService when it is stopped. This finishes the Activity.
     *
     * @param cancel throw this away for now.
     */
    @Subscribe
    public void onLoggingStopped(DriveLoggingService.LoggingStoppedEvent cancel) {
        finish();
    }

    @Subscribe
    public void onLoggingFailedToConnect(DriveLoggingService.CouldNotConnectEvent e) {
        Toast.makeText(this, "Could not connect to bluetooth adapter", Toast.LENGTH_LONG).show();
        Log.d("LoggingActivity", "Could not connect to bluetooth adapter");
        finish();
    }
}
