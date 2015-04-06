package me.williamhester.brobd;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

/**
 * This is the activity that is running when a user wants to see their current data and the
 * DriveLoggingService is running. Going back from this activity will stop the logging service.
 *
 * @author William Hester
 */
public class LoggingActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (f == null) {
            f = DriveStatisticsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f, "DriveStats")
                    .commit();
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
                                DriveLoggingService.class);
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
}
