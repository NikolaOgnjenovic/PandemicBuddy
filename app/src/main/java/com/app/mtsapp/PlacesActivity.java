package com.app.mtsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class PlacesActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FusedLocationProviderClient flpClient; //Коришћен за добијање тренутне локације уређаја
    private int locationRequestCode = 100; //Код за захтевање дозволе коришћења локације уређаја
    private int gpsRequestCode = 200; //Код за паљење GPS локације уређаја
    private GoogleMap googleMap;
    private LatLng currentLatLng = null; //Тренутне координате уређаја
    private String[] locationRequests = {"Manifest.permission.ACCESS_FINE_LOCATION", "Manifest.permission.ACCESS_COARSE_LOCATION"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        //Ако апликација има дозволе потребне за узимање локације
        if (hasLocationPermission()) {
            //Узми supportMapFragment и обавести програм кад је мапа спремна
            SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (supportMapFragment != null) {
                supportMapFragment.getMapAsync(this);
            }

            Button addMarkerButton = findViewById(R.id.addMarker);
            addMarkerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    turnOnGPS(); //Упали GPS локацију на уређају
                }
            });
        } else {
            //Ако нема, затражи их
            requestLocationPermissions();
        }
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        flpClient = LocationServices.getFusedLocationProviderClient(PlacesActivity.this); //Референцира провајдер тренутне локације
        googleMap = gMap;
    }


    //=================== ПРОВЕРАВАЊЕ И ДОБИЈАЊЕ ДОЗВОЛА ЗА ЛОКАЦИЈУ И ПАЉЕЊЕ ЛОКАЦИЈЕ НА УРЕЂАЈУ =============================

    //Проверава да ли апликација има дозволе потребне за добијање локације
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == (PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == (PackageManager.PERMISSION_GRANTED);
    }

    //Захтева дозволе потребне за добијање локације
    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, locationRequests, locationRequestCode);
    }

    //Обрађује захтеве за дозволе
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Ако су у питању локације
        if (requestCode == locationRequestCode) {
            if (grantResults.length > 0) {
                //Ако су дозвољене
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //Обавести корисника да је одбио захтев за дозволе локације и врати га на главни екран (MainActivity)
                    Toast.makeText(this, "Одбијена дозвола за локацију", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                }
            }
        }
    }

    /*Проверава да ли је GPS локација уређаја упаљења. Ако јесте, дода маркер на тренутној локацији а ако није затражи од корисника да дозволи
    апликацији да користи GPS локацију уређаја*/
    private void turnOnGPS() {
        //Направи захтев за проверу и добијање GPS локације уређаја
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5000).setFastestInterval(2000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext()).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {

                    LocationSettingsResponse response = task.getResult(ApiException.class); //Response се прави да би се проверило да ли је GPS локација већ упаљења

                    //Ако је GPS локација већ упаљења неће се throw-ати било какав exception
                    System.out.println("[MRMI]: GPS локација је већ упаљена, додајем маркер");
                    addMarker(); //Додај маркер на тренутној локацији уређаја
                } catch (ApiException apiE) {
                    switch (apiE.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            //Упали мени који пита корисника да ли апликација сме да упали GPS локацију уређаја
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) apiE;
                                resolvableApiException.startResolutionForResult(PlacesActivity.this, gpsRequestCode);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            System.out.println("[MRMI]: SETTINGS_CHANGE_UNAVAILABLE");
                            break;
                    }
                }

            }
        });
    }

    //Обрађује одлуке корисника при дијалогу за коришћење GPS лоакције уређаја
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == gpsRequestCode) {
            //Ако је корисник одбио коришћење GPS локације обавести га о неопходности коришћења њих за додавање тренутне локације
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Коришћење GPSа је обавезно за додавање тренутне локације", Toast.LENGTH_LONG).show();
            } else {
                //Ако је пристао мораће да сачека пар секунди да се GPS повеже са уређајем и да поново стисне дугме за додавање тренутне локације
                System.out.println("[MRMI]: Упаљена GPS локациај уређаја");
            }
        }
    }


    //=================== ДОДАВАЊЕ МАРКЕРА И ДОБИЈАЊЕ ТРЕНУТНЕ ЛОКАЦИЈЕ =============================

    //Пронађе тренутну локацију и смести је у променљиву currentLatLng
    private void getCurrentLocation() {
        try {
            @SuppressLint("MissingPermission") Task<Location> task = flpClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if (location != null) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude()); //Узми тренутне координате
                        System.out.println("[MRMI]: Променио тренутне координате на: " + currentLatLng.latitude + " , " + currentLatLng.longitude);
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Неуспело тражење тренутне локације", Toast.LENGTH_SHORT).show();
        }
    }

    //Дода маркер на тренутној лоакцији уређаја
    private void addMarker() {
        getCurrentLocation(); //Пронађи тренутну локацију уређаја

        //Ако су пронађене тренутне координате уређаја
        if (currentLatLng != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17)); //Зумирај мапу на тренутну локацију
            MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng).title("Тренутна локација").draggable(true); //Постави подешавања маркера
            googleMap.addMarker(markerOptions); //Постави маркер тренутне локације на мапу
        } else {
            System.out.println("[MRMI]: Не постоје тренутне координате уређаја");
        }
    }
}