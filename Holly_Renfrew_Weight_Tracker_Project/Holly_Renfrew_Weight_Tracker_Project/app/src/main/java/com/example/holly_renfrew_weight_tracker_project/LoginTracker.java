package com.example.holly_renfrew_weight_tracker_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginTracker extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private CheckBox checkboxRememberMe;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_login);

        dbHelper = new DatabaseHelper(this);

        // SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        // AUTO-LOGIN CHECK HERE
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);
        int savedId = prefs.getInt("userId", -1);
        if (remember && savedId != -1) {
            goToDashboard(savedId);
            return; // skip rest of login screen
        }

        // --- listeners below ---
        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = dbHelper.loginUser(username, password);
            if (userId != -1) {
                if (checkboxRememberMe.isChecked()) {
                    prefs.edit()
                            .putInt("userId", userId)      // ✅ only save userId
                            .putBoolean(KEY_REMEMBER, true)
                            .apply();
                } else {
                    prefs.edit().clear().apply();
                }
                goToDashboard(userId);
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginTracker.this, RegisterTrackerActivity.class);
            startActivity(intent);
        });
    }

    private void goToDashboard(int userId) {
        Intent intent = new Intent(LoginTracker.this, DashboardTracker.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}
