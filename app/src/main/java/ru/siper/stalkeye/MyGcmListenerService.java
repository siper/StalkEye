package ru.siper.stalkeye;
/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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

    private static final String TAG = "MyGcmListenerService";

    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        // создаем объект для создания и управления версиями БД
        dbHelper = new DBHelper(this);

        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String msg_text = data.getString("message");
        String msg_title = data.getString("title");
        String msg_priority = data.getString("priority");

        // Создадим объект Date
        SimpleDateFormat date_format = new SimpleDateFormat("HH:mm\ndd.MM.yyyy", Locale.US);

        // создаем объект для данных
        ContentValues content_to_insert = new ContentValues();

        content_to_insert.put("message_title", msg_title);
        content_to_insert.put("message_text", msg_text);
        content_to_insert.put("message_priority", msg_priority);
        content_to_insert.put("message_date", date_format.format(new Date()));
        // вставляем запись и получаем ее ID
        long rowID = db.insert("notifications", null, content_to_insert);
        Log.i(TAG, "Row id: " + Long.toString(rowID));

        // [START_EXCLUDE]
        sendNotification(msg_title, msg_text, msg_priority);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String title, String message, String priority) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        int color;
        int small_icon;
        switch(Integer.parseInt(priority)) {
            case 1:
                small_icon = R.mipmap.ic_red;
                color = 0xFFFF0000;
                break;
            case 2:
                small_icon = R.mipmap.ic_yellow;
                color = 0xFFFFFF00;
                break;
            case 3:
                small_icon = R.mipmap.ic_green;
                color = 0xFF33CC00;
                break;
            default:
                small_icon = R.mipmap.ic_gray;
                color = 0xFF00FFFF;
                break;
        }
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(small_icon)
                .setContentTitle(title)
                .setLights(color, 500, 500)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

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
