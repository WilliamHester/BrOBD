package me.williamhester.brobd.singletons;

import com.squareup.otto.Bus;

/**
 * This class manages the instance of the Bus that is used across the app.
 *
 * @author William Hester
 */
public class BusManager {

    private static Bus mBus;

    public static Bus getInstance() {
        if (mBus == null) {
            mBus = new Bus();
        }
        return mBus;
    }

}
