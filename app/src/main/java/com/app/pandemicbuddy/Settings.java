package com.app.pandemicbuddy;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.pandemicbuddy.location.service.ServiceHandler;

public class Settings extends AppCompatActivity {

    private LanguageManager languageManager;

    private static int[] notificationToggleIcons;
    private ToggleButton notificationOne, notificationTwo, notificationThree; //Паљење и гашење појединачних нотификација
    private SharedPreferences.Editor sharedPreferencesEditor;
    private SwitchCompat trackerSwitch; //Контролише праћење локације и слања нотифиакција
    private SwitchCompat dailyNotificationsSwitch; //Контролише слање дневних нотификација са саветима
    private final int REQUEST_PERMISSION_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languageManager = new LanguageManager(this);
        languageManager.checkLocale();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ServiceHandler.activityTest |= 2;

        //Референцирај шерд префс и тогл дугмиће за нотификације
        final SharedPreferences sharedPreferences = getSharedPreferences("SharedPreferences", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        notificationOne = findViewById(R.id.notificationOne);
        notificationTwo = findViewById(R.id.notificationTwo);
        notificationThree = findViewById(R.id.notificationThree);
        trackerSwitch = findViewById(R.id.trackerSwitch);
        dailyNotificationsSwitch = findViewById(R.id.dailyNotificationsSwitch);

        Button serbianLatinButton = findViewById(R.id.serbianLatinButton), serbianCyrillicButton = findViewById(R.id.serbianCyrillicButton);
        ImageButton englishButton = findViewById(R.id.englishButton);
        //Постави језик на енглески
        englishButton.setOnClickListener(v -> languageManager.setLocale("en", true));
        //Постави језик на српску латиницу
        serbianLatinButton.setOnClickListener(v -> languageManager.setLocale("sr", true));
        //Постави језик на српску ћирилицу
        serbianCyrillicButton.setOnClickListener(v -> languageManager.setLocale("sr-rRS", true));

        notificationToggleIcons = new int[]{
                R.drawable.put_on_mask,
                R.drawable.disinfect_clothes,
                R.drawable.disinfect_hands,
                R.drawable.mask_notification_disabled,
                R.drawable.clothes_notification_disabled,
                R.drawable.hands_notification_disabled
        };

        loadButtonStates(); //Учита сачувана стања дугмића за нотификације сачувана у уређају

        //Сачувај промене стања коришћења нотификација у уређају и промени иконицу одговарајућег дугмета
        notificationOne.setOnClickListener(v -> {
            sharedPreferencesEditor.putBoolean("notificationOne", notificationOne.isChecked()).apply();
            if (notificationOne.isChecked()) {
                notificationOne.setBackgroundResource(notificationToggleIcons[0]);
            } else {
                notificationOne.setBackgroundResource(notificationToggleIcons[3]);
            }
        });
        notificationTwo.setOnClickListener(v -> {
            sharedPreferencesEditor.putBoolean("notificationTwo", notificationTwo.isChecked()).apply();
            if (notificationTwo.isChecked()) {
                notificationTwo.setBackgroundResource(notificationToggleIcons[1]);
            } else {
                notificationTwo.setBackgroundResource(notificationToggleIcons[4]);
            }
        });
        notificationThree.setOnClickListener(v -> {
            sharedPreferencesEditor.putBoolean("notificationThree", notificationThree.isChecked()).apply();
            if (notificationThree.isChecked()) {
                notificationThree.setBackgroundResource(notificationToggleIcons[2]);
            } else {
                notificationThree.setBackgroundResource(notificationToggleIcons[5]);
            }
        });

        //Покрени/заустави сервис за праћење локације и слање нотификација везаних за локацију
        trackerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableLocationTracking(ServiceHandler.getPermissions(), REQUEST_PERMISSION_LOCATION);
                //ServiceHandler.startTrackingService(Settings.this);
            } else {
                ServiceHandler.stopTrackingService(Settings.this);
            }
            sharedPreferencesEditor.putBoolean("trackerSwitch", isChecked).apply();
        });

        dailyNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferencesEditor.putBoolean("sendDailyNotifications", isChecked).apply();
            if (isChecked) {
                ServiceHandler.startDailyNotification(Settings.this);
            } else {
                ServiceHandler.stopDailyNotification(Settings.this);
            }
        });
    }

    //IZ KNJIGE
    private void enableLocationTracking(String[] permissions, int permissionRequestCode) {
        if (!checkPermissions(permissions)) {
            showLocationExplanation(getString(R.string.locationUsage), getString(R.string.locationUsageText), permissions, permissionRequestCode);
        } else {
            System.out.println("Permissions already granted!");
            ServiceHandler.startTrackingService(Settings.this);
        }
    }

    //Returns false if >0 of the permissions in a list is denied
    private boolean checkPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                System.out.println("Permission " + permission + " denied.");
                return false;
            }
        }
        return true;
    }

    //Shows a dialog which tells the user why the app needs the permissions
    private void showLocationExplanation(String title, String message, final String[] permissions, final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.accept), (dialogInterface, i) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestMultiplePermissions(permissions, permissionRequestCode);
                    }
                })
                .setNegativeButton(getString(R.string.decline), ((dialogInterface, i) -> trackerSwitch.setChecked(false)));
        builder.create().show();
    }

    //Requests an array of permissions
    private void requestMultiplePermissions(String[] permissions, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this, permissions, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ServiceHandler.startTrackingService(Settings.this);
            } else {
                trackerSwitch.setChecked(false);
            }
        }
    }

    //Учита сачувана стања дугмића за нотификације сачувана у уређају и промени стања дугмића, као и прекидача за сервис за праћење
    private void loadButtonStates() {
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPreferences", MODE_PRIVATE);

        notificationOne.setChecked(sharedPreferences.getBoolean("notificationOne", true));
        notificationTwo.setChecked(sharedPreferences.getBoolean("notificationTwo", true));
        notificationThree.setChecked(sharedPreferences.getBoolean("notificationThree", true));
        setNotificationButtonIcons();
        trackerSwitch.setChecked(sharedPreferences.getBoolean("trackerSwitch", false));
        dailyNotificationsSwitch.setChecked(sharedPreferences.getBoolean("sendDailyNotifications", false));
    }

    //Питај корисника да ли жели да изађе са тренутног екрана када притисне дугме за враћање назад
    @Override
    public void onBackPressed() {
        //Прикажи упозорење
        InfoPopup infoPopup = new InfoPopup(Settings.this);
        infoPopup.showBackButtonDialog(false);
    }

    //Постави иконице сва 3 дугмета за нотификације
    private void setNotificationButtonIcons() {
        if (notificationOne.isChecked()) {
            notificationOne.setBackgroundResource(notificationToggleIcons[0]);
        } else {
            notificationOne.setBackgroundResource(notificationToggleIcons[3]);
        }
        if (notificationTwo.isChecked()) {
            notificationTwo.setBackgroundResource(notificationToggleIcons[1]);
        } else {
            notificationTwo.setBackgroundResource(notificationToggleIcons[4]);
        }
        if (notificationThree.isChecked()) {
            notificationThree.setBackgroundResource(notificationToggleIcons[2]);
        } else {
            notificationThree.setBackgroundResource(notificationToggleIcons[5]);
        }
    }

    @Override
    protected void onDestroy() {
        ServiceHandler.activityTest &= 5;
        super.onDestroy();
    }
}