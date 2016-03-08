package ru.siper.stalkeye;
/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MyGcmListenerService extends GcmListenerService {

    DBHelper dbHelper;
    SharedPreferences sp;

    private static final String TAG = "MyGcmListenerService";

    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        // создаем объект для создания и управления версиями БД
        dbHelper = new DBHelper(this);
        // создаем объект для создания и управления настройками
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String msg_text = data.getString("message");
        String msg_title = data.getString("title");
        String msg_priority = data.getString("priority");
        String msg_url = data.getString("subtext");

        // Проверяем длинну строки, если она больше 30 символов, то переносим по следующему пробелу
        if(msg_text.length() > 20) {
            int str_index = msg_text.indexOf(" ", 30);
            if(str_index != -1) {
                msg_text = msg_text.substring(0, str_index) + "\n" + msg_text.substring(str_index+1);
            }
        } else {
            Log.i(TAG, "Message is empty");
        }

        // Создадим объект Date
        Date date = new Date();
        SimpleDateFormat date_format = new SimpleDateFormat("HH:mm\ndd.MM.yyyy", Locale.US);

        // создаем объект для данных
        ContentValues content_to_insert = new ContentValues();

        content_to_insert.put("message_title", msg_title);
        content_to_insert.put("message_text", msg_text);
        content_to_insert.put("message_priority", msg_priority);
        content_to_insert.put("message_date", date_format.format(date));
        content_to_insert.put("message_date_millis", String.valueOf(date.getTime()));
        // вставляем запись и получаем ее ID
        long rowID = db.insert("notifications", null, content_to_insert);
        Log.i(TAG, "Row id: " + Long.toString(rowID));

        // [START_EXCLUDE]
        if (sp.getBoolean("pref_notifications_switch", true)) {
            sendNotification(msg_title, msg_text, msg_priority, msg_url);
        }
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String title, String message, String priority, String url) {

        int small_icon;
        switch (Integer.parseInt(priority)) {
            case 1:
                small_icon = R.mipmap.ic_red;
                break;
            case 2:
                small_icon = R.mipmap.ic_yellow;
                break;
            case 3:
                small_icon = R.mipmap.ic_green;
                break;
            case 4:
                small_icon = R.mipmap.ic_blue;
                break;
            default:
                small_icon = R.mipmap.ic_gray;
                break;
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(small_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri);

        // Проверка на наличие url
        if (url.startsWith("http://")) {
            Context context = getApplicationContext();
            Intent notificationIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.setContentIntent(pendingIntent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationBuilder.setContentIntent(pendingIntent);
        }

        // Вибрация
        if (sp.getBoolean("pref_notifications_vibro_switch", true)) {
            long[] vibro_array = new long[2];
            long vibro = Long.parseLong(sp.getString("pref_notifications_vibro_value", "200"));
            vibro_array[0] = vibro;
            vibro_array[1] = vibro;
            notificationBuilder.setVibrate(vibro_array);
        }

        // LED
        if (sp.getBoolean("pref_notification_led_switch", true)) {
            int color;
            switch (sp.getString("pref_notifications_led_color_set", "4")) {
                case "1":
                    color = R.color.led_red;
                    break;
                case "2":
                    color = R.color.led_blue;
                    break;
                case "3":
                    color = R.color.led_aqua;
                    break;
                case "4":
                    color = R.color.led_white;
                    break;
                case "5":
                    color = R.color.led_green;
                    break;
                case "6":
                    color = R.color.led_yellow;
                    break;
                case "7":
                    color = R.color.led_violet;
                    break;
                default:
                    color = R.color.led_white;
                    break;
            }
            notificationBuilder.setLights(getColor(color), 500, 500);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Random notification_id = new Random();

        notificationManager.notify(notification_id.nextInt(), notificationBuilder.build());
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "notifications_db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
