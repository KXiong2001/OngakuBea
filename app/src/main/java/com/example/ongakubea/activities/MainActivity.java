package com.example.ongakubea.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;

import com.example.ongakubea.R;
import com.example.ongakubea.utils.VideoItemsDbHelper;

public class MainActivity extends Activity {

    private static final String PREF_ACCOUNT_NAME = "accountName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String accountName = getSharedPreferences("p1", Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        boolean accountSelected = accountName != null;
        initializeDatabase();
        new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                Intent loginPage;
                if (accountSelected) {
                    loginPage = new Intent(MainActivity.this, PlaylistActvity.class);
                } else {
                    loginPage = new Intent(MainActivity.this, LoginActivity.class);
                }
                startActivity(loginPage);
            }
        }.start();
    }

    private void initializeDatabase() {
        VideoItemsDbHelper dbHelper = new VideoItemsDbHelper(this);
    }
}