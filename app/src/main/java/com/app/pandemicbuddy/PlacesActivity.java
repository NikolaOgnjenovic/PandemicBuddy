package com.app.pandemicbuddy;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.pandemicbuddy.location.LocationFinder;
import com.app.pandemicbuddy.location.LocationSystem;
import com.app.pandemicbuddy.location.SavedLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class PlacesActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowLongClickListener {
    private GoogleMap googleMap;

    private Location currentLocation; //Тренутна локација уређаја
    private String currentLocationName; //Име које се чува на маркеру при чувању локације

    //Toast упозорења
    private String noLocationNameToast;
    private String retryCurrentLocationToast;
    private String renameLocationToast;

    private List<SavedLocation> savedLocations;
    private LocationSystem locationSystem;
    private LocationFinder locationFinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager languageManager = new LanguageManager(PlacesActivity.this);
        languageManager.checkLocale();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        //Учитај сачуване локације и референцирај листу сачуваних локација
        locationSystem = new LocationSystem(this);

        locationFinder = new LocationFinder(this);
        locationFinder.run();

        noLocationNameToast = this.getResources().getString(R.string.noLocationNameToast);
        retryCurrentLocationToast = this.getResources().getString(R.string.retryCurrentLocationToast);
        renameLocationToast = this.getResources().getString(R.string.renameLocationToast);

        //Узми supportMapFragment и обавести програм кад је мапа спремна
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(this);
        }

        ImageButton addMarkerButton = findViewById(R.id.addMarker);
        addMarkerButton.setOnClickListener(v -> {
            getCurrentLocation();
            if (currentLocation != null) {
                showAddLocationPopup();
            } else {
                Toast.makeText(PlacesActivity.this, retryCurrentLocationToast, Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> {
            InfoPopup infoPopup = new InfoPopup(PlacesActivity.this);
            infoPopup.showHelpDialog();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap gMap) {
        googleMap = gMap;

        loadMarkersFromSavedLocations(); //Прикажи маркере свих сачуваних локација

        getAndZoomCurrentLocation();
        //Ако није детектована тренутна локација, зумирај на кућну лоакцију или ако она не постоји на прву сачувану локацију
        if (currentLocation == null) {
            //Ако постоји бар 1 сачувана локација
            if (savedLocations.size() > 0) {
                LatLng zoomLatLng;
                SavedLocation homeLocation = locationSystem.getLocation("home");
                if (homeLocation != null) {
                    zoomLatLng = new LatLng(homeLocation.getLatitude(), homeLocation.getLongitude()); //Координате сачуване кућне локације
                } else {
                    zoomLatLng = new LatLng(savedLocations.get(0).getLatitude(), savedLocations.get(0).getLongitude()); //Координате прве сачуване локације
                }

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomLatLng, 17)); //Зумирај мапу на изабрану локацијуу
            }
        }

        ImageButton currentLocationButton = findViewById(R.id.currentLocationButton);
        currentLocationButton.setOnClickListener(v -> {
            getCurrentLocation();
            if (currentLocation != null) {
                getAndZoomCurrentLocation();
            } else {
                Toast.makeText(PlacesActivity.this, retryCurrentLocationToast, Toast.LENGTH_SHORT).show();
            }
        });

        //Зове се када се неки маркер помера
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker arg0) {
            }

            //Кад је завршено померање маркера
            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                try {
                    Log.d("System out", "onMarkerDragEnd..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude);
                    System.out.println("[МРМИ]: Стара локација: " + locationSystem.getLocation(marker.getTitle()).getLatitude() + ", " + locationSystem.getLocation(marker.getTitle()).getLongitude());

                    //Промени координате локације која је сачувана у локцаионом систему користећи нове координате маркера
                    locationSystem.getLocation(marker.getTitle()).setLatitude(marker.getPosition().latitude);
                    locationSystem.getLocation(marker.getTitle()).setLongitude(marker.getPosition().longitude);

                    System.out.println("[МРМИ]: Померам локацију имена " + marker.getTitle());
                    System.out.println("[МРМИ]: Нова локација: " + locationSystem.getLocation(marker.getTitle()).getLatitude() + ", " + locationSystem.getLocation(marker.getTitle()).getLongitude());

                    locationSystem.saveLocations(); //Сачувај локације у систему
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMarkerDrag(@NonNull Marker arg0) {
            }
        });

        //Избриши маркер кад се дуго стисне његов назив
        googleMap.setOnInfoWindowLongClickListener(this);
    }

    //Избриши маркер и локацију из система кад корисник дуго држи прст на називу маркера
    @Override
    public void onInfoWindowLongClick(Marker marker) {
        marker.remove();
        locationSystem.removeLocation(marker.getTitle());
    }

    //Питај корисника да ли жели да изађе са тренутног екрана када притисне дугме за враћање назад
    @Override
    public void onBackPressed() {
        //Прикажи упозорење
        InfoPopup infoPopup = new InfoPopup(PlacesActivity.this);
        infoPopup.showBackButtonDialog(false);
    }

    //Прикаже прозор за унос локације
    private void showAddLocationPopup() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.add_location_popup);

        final EditText locationNameInput = dialog.findViewById(R.id.locationNameInput); //Текст за унос имена локације
        final SwitchCompat isHomeLocationSwitch = dialog.findViewById(R.id.isHomeLocationSwitch); //Дугме које одређује да ли је локација која се чува кућна или не

        //Дугме за отказивање додавања локације
        ImageButton cancelButton = dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        //Дугме за чување изабране локације
        ImageButton yesButton = dialog.findViewById(R.id.yesButton);
        yesButton.setOnClickListener(v -> {
            currentLocationName = locationNameInput.getText().toString();

            //Стави назив на ? ако корисник није унео назив
            if (currentLocationName.equals("")) {
                Toast.makeText(PlacesActivity.this, noLocationNameToast, Toast.LENGTH_SHORT).show();
            } else if (locationSystem.getLocation(currentLocationName) != null) {
                Toast.makeText(PlacesActivity.this, renameLocationToast, Toast.LENGTH_SHORT).show();
            } else {
                saveLocation(isHomeLocationSwitch.isChecked()); ////Провери да ли је локација која се уноси кућна и додаје је у систем као и њен маркер

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    //=================== ДОДАВАЊЕ МАРКЕРА И ДОБИЈАЊЕ ТРЕНУТНЕ ЛОКАЦИЈЕ =============================

    private void getCurrentLocation() {
        currentLocation = locationFinder.getCurrentLocation();
    }

    //Дода маркер на тренутној лоакцији уређаја и сачува је у листи локација
    private void saveLocation(boolean settingHomeLocation) {
        //Ако су пронађене тренутне координате уређаја
        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()); //Узми тренутне координате
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17)); //Зумирај мапу на тренутну локацију
            MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng).title(currentLocationName).draggable(true); //Постави подешавања маркера

            //Ако се мења кућна лоакција
            if (settingHomeLocation) {
                //Ако већ постоји кућна локација сачувана у систему
                SavedLocation temp = locationSystem.findHomeLocation();
                if (temp != null) {
                    //Избриши стару кућну локацију и поново прикажи маркере
                    locationSystem.removeLocation(temp.getName());
                    locationSystem.saveLocations();
                    googleMap.clear();
                    loadMarkersFromSavedLocations();
                }

                //Постави посебну иконицу кућног маркера и назови маркер "home" у систему локација ради лакшег проналажења
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon("home_location", 50, 50)));
            }
            googleMap.addMarker(markerOptions); //Прикажи маркер одабраних подешавања

            System.out.println("[МРМИ]: Чувам локацију имена " + currentLocationName);

            //Додај и сачувај локацију у систему локација
            SavedLocation sl = new SavedLocation(currentLocationName, currentLocation).setHome(settingHomeLocation);
            locationSystem.addLocation(sl);
            locationSystem.saveLocations();
        } else {
            System.out.println("[MRMI]: Не постоје тренутне координате уређаја");
        }
    }

    //Прикаже маркере сачуваних локација користећи LocationSystem
    private void loadMarkersFromSavedLocations() {

        locationSystem.loadLocations();
        savedLocations = locationSystem.getLocations();

        System.out.println("[MRMI]: Број сачуваних локација: " + savedLocations.size());

        //Направи фајл са локацијама ако не постоји
        if (savedLocations == null) {
            locationSystem.saveLocations();
            savedLocations = locationSystem.getLocations();
        } else {
            for (SavedLocation savedLocation : savedLocations) {
                LatLng currentLatLng = new LatLng(savedLocation.getLatitude(), savedLocation.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng).title(savedLocation.getName()).draggable(true); //Постави позицију, назив и особине маркера
                if (savedLocation.isHome()) {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcon("home_location", 50, 50)));
                }
                googleMap.addMarker(markerOptions); //Постави маркер учитане локације на мапу
                System.out.println("[MRMI]: Учитао локацију " + savedLocation.getName() + " на координатама " + savedLocation.getLatitude() + " , " + savedLocation.getLongitude());
            }
        }
    }

    //Преувелича иконицу маркера за локацију
    public Bitmap resizeMapIcon(String iconName, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(iconName, "drawable", getPackageName()));
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

    private void getAndZoomCurrentLocation() {
        getCurrentLocation();
        if (currentLocation != null) {
            //Зумирај мапу на тренутну локацију
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 17));
        }
    }
}