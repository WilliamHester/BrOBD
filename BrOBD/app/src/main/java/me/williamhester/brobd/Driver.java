package me.williamhester.brobd;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * This class represents a Driver. For now, it's only a String.
 *
 * @author William Hester
 */
public class Driver extends RealmObject {

    @PrimaryKey
    private String name;

    public Driver() { }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
