package com.app.mtsapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.mtsapp.location.LocationFinder;
import com.app.mtsapp.location.LocationSystem;
import com.app.mtsapp.location.SavedLocation;

import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity{

    private LocationFinder finder;
    private LocationSystem locationSystem;

    private TextView tvLatitude,tvLongitude;

    private TextView dailyTipTextView;

    //Dnevni saveti
    private String[] dailyTips = {"Tip one", "Tip two", "Tip three", "Tip four", "Tip five"}; //Dnevni saveti koji se prikazuju na glavnom ekranu
    private Random random = new Random(); //Za generisanje nasumicnih brojeva
    private int currentTipIndex; //Indeks trenutnog saveta u nizu saveta, koriscen za menjanje saveta prikazanog pri promeni datuma

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //Lock activity into portrait mode

        finder = new LocationFinder(this);
        locationSystem = new LocationSystem(this);
        tvLatitude = (TextView) findViewById(R.id.tvLatitude);
        tvLongitude = (TextView) findViewById(R.id.tvLongitude);
        Button getLocation = (Button) findViewById(R.id.getLocation);
        Button checkLocations = (Button) findViewById(R.id.checkLocation);//Proveri da li ima sacuvanih lokacija
        Button saveLocations = (Button) findViewById(R.id.saveLocation);//Sacuva podatke o imenovanim lokacijama u memoriji telefona

        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = finder.getCurrentLocation();
                if(location!=null){
                    tvLatitude.setText(String.valueOf(location.getLatitude()));
                    tvLongitude.setText(String.valueOf(location.getLongitude()));
                    SavedLocation temp = new SavedLocation(location);
                    locationSystem.addLocation(temp);
                }
            }
        });

        checkLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationSystem.loadLocations();
                for(SavedLocation sl : locationSystem.getLocations()){
                    Log.i("[Loaded location]", sl.getName()+ " "+
                                                          sl.getLastDate()+" "+
                                                          sl.getAltitude()+" "+
                                                          sl.getLatitude()+" "+
                                                          sl.getLongitude());
                }
            }
        });

        saveLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationSystem.saveLocations();
                Log.i("[Location save]", " Locations saved!!");
            }
        });

        dailyTipTextView = findViewById(R.id.dailyTipText);
        checkDailyTip();

        Button notifyButton = findViewById(R.id.notifyButton);
        notifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSender notificationSender = new NotificationSender(MainActivity.this);
                notificationSender.showNotification(0);
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSender notificationSender = new NotificationSender(MainActivity.this);
                notificationSender.showNotification(1);
                Intent placesIntent = new Intent(MainActivity.this, PlacesActivity.class);
                startActivity(placesIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finder.stopLocating();
    }

    //Proverava da li treba da se promeni prikazani dnevni savet
    private void checkDailyTip() {
        //Referenca SharedPreferences-a: lakog nacina cuvanja prostih podataka
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPreferences", MODE_PRIVATE);

        //Uzmi trenutni dan preko Calendar.getInstance() i nadji poslednji sacuvani dan u SharedPreferences
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH), lastSavedDayD = sharedPreferences.getInt("LastSavedDayD", -1);

        System.out.println("[MRMI]: Today: " + currentDay + " Last saved day: " + lastSavedDayD);

        //Ako ne postoji poslednji dan sacuvan na uredjaju
        if(lastSavedDayD == -1) {
            changeDailyTip(-1); //Prikazi prvi savet iz niza saveta

            //Sacuvaj trenutni dan na uredjaju
            lastSavedDayD = currentDay;
            sharedPreferences.edit().putInt("LastSavedDayD", lastSavedDayD).apply();

            //Takodje sacuvaj indeks trenutnog saveta kako bi se on prikazao pri ponovnom ulazenju u aplikaciju istog dana
            sharedPreferences.edit().putInt("CurrentTipIndex", currentTipIndex).apply();

            System.out.println("[MRMI]: Last day is -1, current tip index: " + currentTipIndex);
        }
        //Ako se sacuvan dan i danas ne poklapaju
        else if(lastSavedDayD!=currentDay) {
            //Sacuvaj trenutni dan na uredjaju
            lastSavedDayD=currentDay;
            sharedPreferences.edit().putInt("LastSavedDayD", lastSavedDayD).apply();

            //Pronadji indeks trenutnog saveta sa uredjaja i
            currentTipIndex = sharedPreferences.getInt("CurrentTipIndex", -1);

            //Promeni prikazan savet u nasumican savet indeksa koji nije trenutni
            changeDailyTip(currentTipIndex);

            //Sacuvaj indeks trenutnog saveta koji promeni funkcija changeDailyTip()
            sharedPreferences.edit().putInt("CurrentTipIndex", currentTipIndex).apply();

            System.out.println("[MRMI]: Last day is " + lastSavedDayD + ", current tip index: " + currentTipIndex);
        }
        //Ako se dani poklapaju
        else {
            currentTipIndex = sharedPreferences.getInt("CurrentTipIndex", random.nextInt(dailyTips.length)); //Nadji indeks trenutnog saveta

            dailyTipTextView.setText(dailyTips[currentTipIndex]); //I prikazi ga

            System.out.println("[MRMI]: Last day is " + lastSavedDayD + ", current tip index: " + currentTipIndex);
        }
    }

    //Menja trenutni savet u zavisnosti od argumenta
    private void changeDailyTip(int skippedTipIndex) {
        int randomIndex=random.nextInt(dailyTips.length); //Indeks nasumicno izabranog saveta
        if(skippedTipIndex!=-1){ //Ako je indeks validan (!= -1) sve dok se ne nadje indeks drugaciji od datog, uzimaj ga nasumicno
            do {
                randomIndex = random.nextInt(dailyTips.length); //Uzmi nasumican indeks
                System.out.println("[MRMI]: Random index: " + randomIndex + " current index: " + skippedTipIndex);
            }while(randomIndex==skippedTipIndex); //Ponavljaj proces sve dok se ne nadje indeks drugaciji od skippedTipIndex (radi izbegavanja ponavljanja saveta uzastopno)
        }

        currentTipIndex = randomIndex; //Promeni indeks trenutnog saveta
        dailyTipTextView.setText(dailyTips[randomIndex]); //Prikazi izabran nasumican savet
    }
}
