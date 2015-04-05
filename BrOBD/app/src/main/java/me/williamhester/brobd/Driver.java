package me.williamhester.brobd;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by william on 4/5/15.
 */
public class Driver extends RealmObject {

    @PrimaryKey
    private String name;

}
