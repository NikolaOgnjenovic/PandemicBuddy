package com.app.pandemicbuddy.location;

import android.location.Location;

import java.io.Serializable;

public class SavedLocation implements Serializable {
    private String name;
    private double latitude, longitude;

    private boolean home;

    public SavedLocation(String name, Location location) {
        this(name, location.getLatitude(), location.getLongitude());
    }

    public SavedLocation(String name, double latitude, double longitude) {
        this.setName(name);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    public double distanceTo(Location location){
        if (location == null) {
            return -1.0;
        }

        Location temp = new Location("temp");
        temp.setLatitude(this.latitude);
        temp.setLongitude(this.longitude);

        return temp.distanceTo(location);
    }

    public double distanceTo(SavedLocation location) {
        if (location == null) {
            return -1.0;
        }

        Location temp = new Location("temp");
        temp.setLatitude(this.latitude);
        temp.setLongitude(this.longitude);

        Location temp2 = new Location("temp2");
        temp2.setLatitude(location.latitude);
        temp2.setLongitude(location.longitude);

        return temp.distanceTo(temp2);
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isHome() {
        return home;
    }

    public SavedLocation setHome(boolean home) {
        this.home = home;
        return this;
    }
}
