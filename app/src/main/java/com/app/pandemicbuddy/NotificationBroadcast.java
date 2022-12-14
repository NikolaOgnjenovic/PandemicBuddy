package com.app.pandemicbuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Random;

public class NotificationBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LanguageManager languageManager = new LanguageManager(context);
        languageManager.checkLocale();

        String[] dailyTips = context.getResources().getStringArray(R.array.dailyTips);
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        int currentTipIndex = sharedPreferences.getInt("CurrentTipIndex", -1);

        notifyRandomTip(currentTipIndex, context, dailyTips);

        Intent temp = new Intent(context, NotificationBroadcast.class);
        temp.setPackage("com.app.mtsapp");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, temp, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        long triggerMillis = c.getTimeInMillis() + AlarmManager.INTERVAL_DAY;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    //Мења тренутни савет у насумичан савет и
    private void notifyRandomTip(int skippedTipIndex, Context context, String[] dailyTips) {
        Random random = new Random();

        int randomIndex = random.nextInt(dailyTips.length); //Индекс насумично изабарног савета
        if (skippedTipIndex != -1) { //Ако је индекс валидан (!= -1) све док се не нађе индекс другачији од датог, узимај га насумично
            while (randomIndex == skippedTipIndex) {
                randomIndex = random.nextInt(dailyTips.length); //Узми насумичан индекс
            }
        }

        NotificationSender sender = new NotificationSender(context);
        sender.showNotification(dailyTips[randomIndex]);
    }
}
