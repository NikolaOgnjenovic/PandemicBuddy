package com.app.pandemicbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.pandemicbuddy.location.service.ServiceHandler;

import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    //Дневни савети
    private TextView dailyTipTextView;
    private String[] dailyTips; //Дневни савети који се приказују
    private final Random random = new Random(); //За генерисање насумичних бројева
    private int currentTipIndex; //Индекс тренутног савета у низу савета, коришћен за мењање савета приказаон при промени датума

    private SharedPreferences sharedPreferences; //Референца SharedPreferences-a: лаког начина чувања простих података

    private final String[] locationRequests = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private PermissionHandler permissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Учитај језик активитија
        LanguageManager languageManager = new LanguageManager(MainActivity.this);
        languageManager.checkLocale();
        dailyTips = getResources().getStringArray(R.array.dailyTips);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Код за захтевање дозволе коришћења локације уређаја
        int locationRequestCode = 500;
        permissionHandler = new PermissionHandler(MainActivity.this, locationRequests, locationRequestCode, getString(R.string.currentLocationUsageText));

        sharedPreferences = getSharedPreferences("SharedPreferences", MODE_PRIVATE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //Закључај екран у portrait mode

        ServiceHandler.activityTest |= 4;

        //Ако је потребно, промени дневни савет приказан на екрану
        dailyTipTextView = findViewById(R.id.dailyTipText);
        checkDailyTip();

        //Дугмићи и њихова функционалност
        ImageButton mapsButton, settingsButton, infoButton;

        //Упали активити са мапом сачуваних локација
        mapsButton = findViewById(R.id.mapsButton);
        mapsButton.setOnClickListener(v -> permissionHandler.handlePermissions());

        //Упали активити са подешавањима
        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Settings.class)));

        //Прикаже прозор информација и правила понашања везаних за ковид-19
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> {
            InfoPopup infoPopup = new InfoPopup(MainActivity.this);
            infoPopup.showRulebookDialog();
        });
    }

    @Override
    protected void onDestroy() {
        ServiceHandler.activityTest &= 3;
        super.onDestroy();
    }

    //Питај корисника да ли жели да изађе са тренутног екрана када притисне дугме за враћање назад
    @Override
    public void onBackPressed() {
        //Прикажи упозорење
        InfoPopup infoPopup = new InfoPopup(MainActivity.this);
        infoPopup.showBackButtonDialog(true);
    }

    //Проверава да ли треба да се промени приказани дневни савет
    private void checkDailyTip() {
        //Узми тренутни дан преко и нађи последњи сачувани дан у уређају
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH), lastSavedDay = sharedPreferences.getInt("LastSavedDay", -1);

        System.out.println("[MRMI]: Today: " + currentDay + " Last saved day: " + lastSavedDay);

        //Ако не постоји последњи дан сачуван у уређају
        if (lastSavedDay == -1) {
            changeDailyTip(-1); //Прикажи насумичан савет из низа савета

            //Сачувај тренутни дан на уређају
            lastSavedDay = currentDay;
            sharedPreferences.edit().putInt("LastSavedDay", lastSavedDay).apply();

            //Такође сачувај индекс тренутног савета како би се он приказао при поноцном улажењу у апликацију истог дана
            sharedPreferences.edit().putInt("CurrentTipIndex", currentTipIndex).apply();

            System.out.println("[MRMI]: Last day is -1, current tip index: " + currentTipIndex);
        }
        //Ако се сачуван дан и данас не поклапају
        else if (lastSavedDay != currentDay) {
            //Сачувај тренутни дан у уређају
            lastSavedDay = currentDay;
            sharedPreferences.edit().putInt("LastSavedDay", lastSavedDay).apply();

            //Пронађи индекс тренутног савета са уређаја
            currentTipIndex = sharedPreferences.getInt("CurrentTipIndex", -1);

            //Промени приказан савет у насумичан савет индекса који није тренутни
            changeDailyTip(currentTipIndex);

            //Сачувај индекс тренутног савета који промени функција changeDailyTip()
            sharedPreferences.edit().putInt("CurrentTipIndex", currentTipIndex).apply();

            System.out.println("[MRMI]: Last day is " + lastSavedDay + ", current tip index: " + currentTipIndex);
        }
        //Ако се дани поклапају
        else {
            currentTipIndex = sharedPreferences.getInt("CurrentTipIndex", random.nextInt(dailyTips.length)); //Нађи индекс тренутног савета у низу савета

            dailyTipTextView.setText(dailyTips[currentTipIndex]); //Прикажи савет

            System.out.println("[MRMI]: Last day is " + lastSavedDay + ", current tip index: " + currentTipIndex);
        }
    }

    //Мења тренутни савет у зависности од аргумента функције
    private void changeDailyTip(int skippedTipIndex) {
        int randomIndex = random.nextInt(dailyTips.length); //Индекс насумично изабарног савета
        if (skippedTipIndex != -1) { //Ако је индекс валидан (!= -1) све док се не нађе индекс другачији од датог, узимај га насумично
            while (randomIndex == skippedTipIndex) {
                randomIndex = random.nextInt(dailyTips.length); //Узми насумичан индекс
            }
        }

        currentTipIndex = randomIndex; //Промени индекс тренутног савета
        dailyTipTextView.setText(dailyTips[randomIndex]); //Прикажи изабран насумичан савет
    }
}
