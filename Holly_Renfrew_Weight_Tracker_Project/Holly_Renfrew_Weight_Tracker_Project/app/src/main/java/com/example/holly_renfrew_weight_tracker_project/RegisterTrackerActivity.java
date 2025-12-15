package com.example.holly_renfrew_weight_tracker_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterTrackerActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextUsername, editTextPassword, editTextPhone;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_tracker);

        dbHelper = new DatabaseHelper(this);

        // MATCHED TO XML EXACTLY
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);

        Button buttonRegisterSubmit = findViewById(R.id.buttonRegisterSubmit);
        Button buttonBack = findViewById(R.id.buttonBack);

        buttonRegisterSubmit.setOnClickListener(v -> registerUser());

        buttonBack.setOnClickListener(v ->
                startActivity(new Intent(RegisterTrackerActivity.this, LoginTracker.class)));
    }


    // ---------------------------------------------------------
    // USER REGISTRATION LOGIC
    // ---------------------------------------------------------
    private void registerUser() {

        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim().toLowerCase();
        String password = editTextPassword.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Empty field check
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Phone format
        if (!Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Username taken?
        if (dbHelper.isUsernameTaken(username)) {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Email taken?
        if (dbHelper.isEmailTaken(email)) {
            Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Phone taken?
        if (dbHelper.isPhoneTaken(phone)) {
            Toast.makeText(this, "Phone number already used", Toast.LENGTH_SHORT).show();
            return;
        }

        // Password strength check
        if (!isStrongPassword(password)) {
            Toast.makeText(this,
                    "Password must be 8+ chars, include uppercase, a number, and a symbol.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Register the user
        boolean success = dbHelper.registerUser(email, username, password, phone);

        if (success) {
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterTrackerActivity.this, LoginTracker.class));
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }


    // ---------------------------------------------------------
    // PASSWORD STRENGTH CHECK
    // ---------------------------------------------------------
    private boolean isStrongPassword(String password) {

        if (password.length() < 8) return false;

        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSymbol = true;
        }

        return hasUpper && hasDigit && hasSymbol;
    }
}
