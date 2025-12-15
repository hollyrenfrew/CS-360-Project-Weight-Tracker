package com.example.holly_renfrew_weight_tracker_project;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsPermissionTracker extends AppCompatActivity {

    private SharedPreferences prefs;

    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_SMS_ALERTS = "sms_alerts_enabled";
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_permission_tracker);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SwitchCompat switchSms = findViewById(R.id.switchSms);
        Button buttonDone = findViewById(R.id.buttonDone);

        //  Restore saved state
        boolean enabled = prefs.getBoolean(KEY_SMS_ALERTS, false);
        switchSms.setChecked(enabled);

        //  Save when toggled
        switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SMS_ALERTS, isChecked).apply();

            if (isChecked) {
                // Request permission if needed
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SEND_SMS},
                            SMS_PERMISSION_CODE);
                }
            }
        });

        //  Done button closes the screen
        buttonDone.setOnClickListener(v -> finish());
    }
}
