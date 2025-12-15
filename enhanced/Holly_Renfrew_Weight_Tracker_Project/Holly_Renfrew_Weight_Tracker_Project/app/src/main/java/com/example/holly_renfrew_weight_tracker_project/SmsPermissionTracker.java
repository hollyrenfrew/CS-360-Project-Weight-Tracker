package com.example.holly_renfrew_weight_tracker_project;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsPermissionTracker extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;

    private SwitchMaterial switchSmsAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_permission_tracker);

        switchSmsAlerts = findViewById(R.id.switchSms);
        Button buttonDone = findViewById(R.id.buttonDone);

        SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("sms_alerts_enabled", false);
        switchSmsAlerts.setChecked(enabled);

        // Toggle behavior
        switchSmsAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestSmsPermission();
            }
        });

        // Save + close
        buttonDone.setOnClickListener(v -> {
            boolean enabledNow = switchSmsAlerts.isChecked();

            prefs.edit()
                    .putBoolean("sms_alerts_enabled", enabledNow)
                    .apply();

            Toast.makeText(this, "SMS alert settings updated", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show();

                // Turn switch off automatically if permission was denied
                switchSmsAlerts.setChecked(false);
            }
        }
    }
}
