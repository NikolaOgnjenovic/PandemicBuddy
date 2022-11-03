package com.app.pandemicbuddy.location;

import android.location.Location;

public interface EventHandler {
    void handle(Location location);
}
