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
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.Bundle;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import com.google.android.gms.gcm.GcmListenerService;

        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Random;

public class MyGcmListenerService extends GcmListenerService {

    DBHelper dbHelper;

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
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

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }
        // Создадим объект Date
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm\ndd.MM.yyyy");

        // создаем объект для данных
        ContentValues cv = new ContentValues();

        cv.put("message_title", msg_title);
        cv.put("message_text", msg_text);
        cv.put("message_priority", msg_priority);
        cv.put("message_date", format1.format(new Date()));
        // вставляем запись и получаем ее ID
        long rowID = db.insert("notifications", null, cv);
        Log.i(TAG, "Row id: " + Long.toString(rowID));
        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
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
        if(priority.equals("1")){
            color = 0xFFFF0000;
            small_icon = R.mipmap.ic_red;
        }
        else if(priority.equals("2")){
            color = 0xFFFFFF00;
            small_icon = R.mipmap.ic_yellow;
        }
        else if(priority.equals("3")){
            color = 0xFF33CC00;
            small_icon = R.mipmap.ic_green;
        }
        else {
            color = 0xFF00FFFF;
            small_icon = R.mipmap.ic_gray;
        }
        Bitmap large_icon = BitmapFactory.decodeResource(getResources(), small_icon);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(small_icon)
                .setLargeIcon(large_icon)
                .setContentTitle(title)
                .setLights(color, 500, 500)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Random random = new Random();

        notificationManager.notify(random.nextInt() /* ID of notification */, notificationBuilder.build());
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
