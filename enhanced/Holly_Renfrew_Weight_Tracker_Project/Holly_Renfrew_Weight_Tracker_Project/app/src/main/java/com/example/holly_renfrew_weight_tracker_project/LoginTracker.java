package com.example.holly_renfrew_weight_tracker_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class LoginTracker extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private CheckBox checkboxRememberMe;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String TAG = "LoginTracker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_login);

        dbHelper = new DatabaseHelper(this);

        // -------------------------------------------------------------
        // ENCRYPTED SHARED PREFERENCES (Enhancement #1)
        // -------------------------------------------------------------
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    this,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (Exception e) {
            Log.e(TAG, "EncryptedSharedPreferences failed. Falling back to normal prefs.", e);
            prefs = getSharedPreferences("fallback_prefs", MODE_PRIVATE);
        }

        // -------------------------------------------------------------
        // VIEW SETUP
        // -------------------------------------------------------------
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        // -------------------------------------------------------------
        // AUTO-LOGIN
        // -------------------------------------------------------------
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);
        int savedUserId = prefs.getInt("user_id", -1);

        if (remember && savedUserId != -1) {
            goToDashboard(savedUserId);
            return;
        }

        // -------------------------------------------------------------
        // LOGIN BUTTON
        // -------------------------------------------------------------
        buttonLogin.setOnClickListener(v -> handleLogin());

        // -------------------------------------------------------------
        // REGISTER BUTTON
        // -------------------------------------------------------------
        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginTracker.this, RegisterTrackerActivity.class);
            startActivity(intent);
        });
    }

    // -------------------------------------------------------------------------
    // LOGIN LOGIC (Enhancements #2–#5 integrated)
    // -------------------------------------------------------------------------
    private void handleLogin() {

        String usernameRaw = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (usernameRaw.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enhancement #3: Normalize username
        String username = usernameRaw.toLowerCase();

        // Enhancement #2: Lockout + rate limiting is in DatabaseHelper
        int result = dbHelper.loginUser(username, password);

        if (result == -2) {
            Toast.makeText(this, "Account temporarily locked. Try again later.", Toast.LENGTH_LONG).show();
            return;
        }

        if (result == -1) {
            Toast.makeText(this, "Invalid login information", Toast.LENGTH_SHORT).show();
            return;
        }

        // SUCCESS LOGIN
        if (checkboxRememberMe.isChecked()) {
            prefs.edit()
                    .putInt("user_id", result)
                    .putBoolean(KEY_REMEMBER, true)
                    .apply();
        } else {
            prefs.edit().clear().apply();
        }

        goToDashboard(result);
    }

    // -------------------------------------------------------------------------
    private void goToDashboard(int userId) {
        Intent intent = new Intent(LoginTracker.this, DashboardTracker.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}
