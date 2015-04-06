package me.williamhester.brobd.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

import io.realm.Realm;
import me.williamhester.brobd.activities.MainActivity;
import me.williamhester.brobd.R;
import me.williamhester.brobd.models.Driver;

/**
 * This fragment displays all of the drivers and allows the user to select one or create a new one.
 * When "Go" is pressed, an instance of LoggingActivity is started along with the
 * DriveLoggingService.
 *
 * TODO: in the future, show statistics about the selected driver
 *
 * @author William Hester
 */
public class DriverSelectionFragment extends Fragment {

    /**
     * @return returns a new instance of the DriverSelectionFragment
     */
    public static DriverSelectionFragment newInstance() {
        return new DriverSelectionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_driver_selection, container, false);

        final Spinner spinner = (Spinner) v.findViewById(R.id.driver_spinner);
        final DriverAdapter adapter = new DriverAdapter();
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == parent.getAdapter().getCount() - 1) {
                    // "New driver" was selected
                    showNewDriverDialog(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button go = (Button) v.findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinner.getSelectedItemPosition() == adapter.getCount() - 1) {
                    showNewDriverDialog(adapter);
                } else {
                    ((MainActivity) getActivity()).onDriverSelected((String) spinner.getSelectedItem());
                }
            }
        });

        return v;
    }

    private void showNewDriverDialog(final DriverAdapter adapter) {
        View v = View.inflate(getActivity(), R.layout.view_new_driver, null);
        final EditText name = (EditText) v.findViewById(R.id.driver_name);
        name.setHint(R.string.name);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.new_driver)
                .setView(v)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String entered = name.getText().toString();
                        if (entered.length() == 0) {
                            return;
                        }
                        if (adapter.contains(entered)) {
                            Toast.makeText(getActivity(), R.string.driver_exists,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Realm realm = Realm.getInstance(getActivity());
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Driver driver = realm.createObject(Driver.class);
                                driver.setName(name.getText().toString());
                            }
                        });
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private class DriverAdapter extends ArrayAdapter<String> {

        private List<Driver> mDrivers;

        public DriverAdapter() {
            super(getActivity(), android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);

            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            Realm realm = Realm.getInstance(getActivity());
            mDrivers = realm.allObjects(Driver.class);

            super.notifyDataSetChanged();
        }

        @Override
        public String getItem(int position) {
            if (position == mDrivers.size()) {
                return getResources().getString(R.string.new_driver);
            } else {
                return mDrivers.get(position).getName();
            }
        }

        @Override
        public int getCount() {
            return mDrivers.size() + 1; // Have to account for the "New driver" option
        }

        /**
         * Says whether or not the driver with the specified name is in the list of drivers.
         *
         * @param name the name of the driver
         * @return whether or not the driver with the specified name is in the list of drivers.
         */
        public boolean contains(String name) {
            for (Driver d : mDrivers) {
                if (d.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }
}
