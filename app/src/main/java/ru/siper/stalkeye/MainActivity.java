package ru.siper.stalkeye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;

    // Имена атрибутов списка для Map
    final String ATTRIBUTE_NAME_MESSAGE_TITLE = "title";
    final String ATTRIBUTE_NAME_MESSAGE_TEXT = "text";
    final String ATTRIBUTE_NAME_MESSAGE_DATE = "date";
    final String ATTRIBUTE_NAME_IMAGE = "image";

    ListView NotificationLV;
    DBHelper dbHelper;
    SharedPreferences sp;

    public void onUpdateButtonClick(View view)
    {
        update_notifications();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(Preferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setVisibility(View.GONE);
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                    NotificationLV.setVisibility(View.GONE);
                }
            }
        };
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        if (checkPlayServices()) {
            // Запуск сервиса для регистрации на сервере GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        update_notifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Preferences.REGISTRATION_COMPLETE));
        update_notifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    void update_notifications() {
        // Создаем объект для создания и управления версиями БД
        dbHelper = new DBHelper(this);

        // Подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Проверка на лимит записей в таблице, если есть - удаляем лишние записи
        if(sp.getBoolean("pref_db_limit_switch", false)){
            Cursor d = db.query("notifications", null, null, null, null, null,
                    "message_date_millis");
            int db_limit = Integer.parseInt(sp.getString("pref_db_limit_value", "20"));
            if(d.getCount() >= db_limit){
                if (d.moveToFirst()) {
                    int idColIndex = d.getColumnIndex("id");
                    for (int i = 0; i < (d.getCount() - db_limit); i++) {
                        int delCount = db.delete("notifications", "id = "
                                + d.getString(idColIndex), null);
                        Log.i(TAG, "deleted rows count = " + delCount);
                        d.moveToNext();
                    }
                }
            }
            d.close();
        }

        // Делаем запрос всех данных из таблицы notifications, получаем Cursor
        Cursor c = db.query("notifications", null, null, null, null, null,
                "message_date_millis DESC");
        // Массивы данных
        ArrayList <String> message_title = new ArrayList<>();
        ArrayList <String> message_text = new ArrayList<>();
        ArrayList <String> message_date = new ArrayList<>();
        ArrayList <Integer> message_image = new ArrayList<>();
        // Ставим позицию курсора на первую строку выборки
        if (c.moveToFirst()) {
            // Определяем номера столбцов по имени в выборке
            int titleColIndex = c.getColumnIndex("message_title");
            int textColIndex = c.getColumnIndex("message_text");
            int priorityColIndex = c.getColumnIndex("message_priority");
            int dateColIndex = c.getColumnIndex("message_date");
            do {
                message_title.add(c.getString(titleColIndex));
                message_text.add(c.getString(textColIndex));
                message_date.add(c.getString(dateColIndex));
                // Выбираем иконку в зависимости от приоритета уведомления
                switch(c.getString(priorityColIndex)) {
                    case "1":
                        message_image.add(R.mipmap.ic_red);
                        break;
                    case "2":
                        message_image.add(R.mipmap.ic_yellow);
                        break;
                    case "3":
                        message_image.add(R.mipmap.ic_green);
                        break;
                    case "4":
                        message_image.add(R.mipmap.ic_blue);
                        break;
                    default:
                        message_image.add(R.mipmap.ic_gray);
                        break;
                }
            } while (c.moveToNext());
        } else
            Log.i(TAG, "0 rows");
        c.close();
        db.close();

        // Упаковываем данные в понятную для адаптера структуру
        ArrayList<Map<String, Object>> data = new ArrayList<>(message_title.toArray().length);
        Map<String, Object> m;
        for (int i = 0; i < message_title.toArray().length; i++) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_NAME_MESSAGE_TITLE, message_title.toArray()[i]);
            m.put(ATTRIBUTE_NAME_MESSAGE_TEXT, message_text.toArray()[i]);
            m.put(ATTRIBUTE_NAME_MESSAGE_DATE, message_date.toArray()[i]);
            m.put(ATTRIBUTE_NAME_IMAGE, message_image.toArray()[i]);
            data.add(m);
        }

        // Массив имен атрибутов, из которых будут читаться данные
        String[] from = { ATTRIBUTE_NAME_MESSAGE_TITLE, ATTRIBUTE_NAME_MESSAGE_TEXT,
                ATTRIBUTE_NAME_MESSAGE_DATE, ATTRIBUTE_NAME_IMAGE };

        // Массив ID View-компонентов, в которые будут вставлять данные
        int[] to = { R.id.MessageTitle, R.id.MessageText, R.id.MessageDate, R.id.MessageImage };

        // Создаем адаптер
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.item, from, to);

        // Определяем список и присваиваем ему адаптер
        NotificationLV = (ListView) findViewById(R.id.NotificationLV);
        NotificationLV.setAdapter(sAdapter);
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // Конструктор суперкласса
            super(context, "notifications_db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Создаем таблицу с полями
            db.execSQL("create table notifications ("
                    + "id integer primary key,"
                    + "message_title text,"
                    + "message_text text,"
                    + "message_priority text,"
                    + "message_date_millis text,"
                    + "message_date text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Создаем верхнее меню, добавляем настройки
        MenuItem mi = menu.add(0, 1, 0, R.string.action_settings);
        mi.setIntent(new Intent(this, SettingsActivity.class));
        return super.onCreateOptionsMenu(menu);
    }

}
