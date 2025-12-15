package com.example.holly_renfrew_weight_tracker_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

public class RegisterTrackerActivity extends AppCompatActivity {

    private static final String TAG = "RegisterTracker";

    private EditText editTextEmail, editTextUsername, editTextPassword, editTextPhone;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_tracker);

        dbHelper = new DatabaseHelper(this);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        Button buttonRegister = findViewById(R.id.buttonRegisterSubmit);
        buttonRegister.setOnClickListener(v -> attemptRegister());


        // --- BACK BUTTON ---
        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

    }

    private void attemptRegister() {
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must have at least one uppercase letter and one number", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (dbHelper.isEmailTaken(email)) {
                Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.isUsernameTaken(username)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.isPhoneTaken(phone)) {
                Toast.makeText(this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean registered = dbHelper.registerUser(email, username, password, phone);

            if (registered) {
                Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginTracker.class));
                finish();
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Database error during registration", e);
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidPassword(String password) {
        // At least one uppercase letter and one number
        return Pattern.compile("^(?=.*[A-Z])(?=.*\\d).+$").matcher(password).matches();
    }
}
