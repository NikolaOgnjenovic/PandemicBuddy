package com.app.pandemicbuddy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.pandemicbuddy.location.LocationFinder;
import com.app.pandemicbuddy.location.service.ServiceHandler;

public class PermissionHandler {
    private final int permissionRequestCode;
    private final String[] permissions;
    private final Activity activity;
    private final Context context;
    private final String dialogText;

    public PermissionHandler(Activity activity, String[] permissions, int permissionRequestCode, String dialogText) {
        this.activity = activity;
        this.permissions = permissions;
        this.permissionRequestCode = permissionRequestCode;
        this.context = activity.getBaseContext();
        this.dialogText = dialogText;
    }

    //IZ KNJIGE
    public void handlePermissions() {
        if (!checkPermissions()) {
            System.out.println("Permissions missing, showing explanation");
            showLocationExplanation(context.getString(R.string.locationUsage), dialogText);
        } else {
            System.out.println("Permissions already granted, activity name: " + activity.getClass().getSimpleName());
            if (activity.getClass().getSimpleName().equals("Settings")) {
                ServiceHandler.startTrackingService(activity);
            } else {
                LocationFinder locationFinder = new LocationFinder(context);
                locationFinder.run();
                locationFinder.start();
                Intent placesIntent = new Intent(context, PlacesActivity.class);
                placesIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(placesIntent);
            }
        }
    }

    //Returns false if >0 of the permissions in a list is denied
    public boolean checkPermissions() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
                System.out.println("Permission " + permission + " denied.");
                return false;
            }
        }
        return true;
    }

    //Shows a dialog which tells the user why the app needs the permissions
    public void showLocationExplanation(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.accept), (dialogInterface, i) -> {
                    System.out.println("ACCEPTED INFO");
                    requestMultiplePermissions();
                })
                .setNegativeButton(context.getString(R.string.decline), ((dialogInterface, i) -> System.out.println("Denied")/*view.setChecked(false))*/));
        builder.create().show();
    }

    //Requests an array of permissions
    private void requestMultiplePermissions() {
        System.out.println("Requesting");
        ActivityCompat.requestPermissions(activity, permissions, permissionRequestCode);
    }
}
