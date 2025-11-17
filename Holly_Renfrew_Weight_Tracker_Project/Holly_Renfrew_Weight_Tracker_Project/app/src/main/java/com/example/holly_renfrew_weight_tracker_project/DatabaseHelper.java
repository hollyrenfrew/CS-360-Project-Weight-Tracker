package com.example.holly_renfrew_weight_tracker_project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 3;   // ★ Enhancement 2 upgrades DB schema

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_PHONE = "phone";

    // ★ ENHANCEMENT 2: new security columns
    private static final String COLUMN_FAILED_ATTEMPTS = "failed_attempts";
    private static final String COLUMN_LOCKED_UNTIL = "locked_until";

    // Weight & Goal tables
    private static final String TABLE_WEIGHT = "weight_entries";
    private static final String TABLE_GOALS = "goals";

    private static final String COLUMN_WEIGHT_ID = "id";
    private static final String COLUMN_USER_REF = "user_id";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_DATE = "date";

    private static final String COLUMN_GOAL_ID = "id";
    private static final String COLUMN_GOAL_WEIGHT = "goal_weight";


    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // --------------------------------------------------------------
    // CREATE DATABASE
    // --------------------------------------------------------------
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Users table (includes new security columns)
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_PHONE + " TEXT UNIQUE, " +
                COLUMN_FAILED_ATTEMPTS + " INTEGER DEFAULT 0, " +
                COLUMN_LOCKED_UNTIL + " INTEGER DEFAULT 0 " +
                ")";

        db.execSQL(createUsersTable);

        // Weight entries
        db.execSQL(
                "CREATE TABLE weight_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "weight REAL, " +
                        "date TEXT)"
        );

        // Goals
        db.execSQL(
                "CREATE TABLE goals (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "goal_weight REAL)"
        );
    }


    // --------------------------------------------------------------
    // DATABASE UPGRADE
    // --------------------------------------------------------------
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Enhancement 2 schema update
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + COLUMN_FAILED_ATTEMPTS + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + COLUMN_LOCKED_UNTIL + " INTEGER DEFAULT 0");
        }
    }


    // --------------------------------------------------------------
    // REGISTER USER
    // --------------------------------------------------------------
    public boolean registerUser(String email, String username, String password, String phone) {

        // Normalize for uniqueness (Enhancement 4)
        email = email.toLowerCase();
        username = username.toLowerCase();

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, hash);
        values.put(COLUMN_PHONE, phone);

        return db.insert(TABLE_USERS, null, values) != -1;
    }


    // --------------------------------------------------------------
    // LOGIN USER  — Includes Enhancement 2 Lockout System
    // --------------------------------------------------------------
    public int loginUser(String usernameOrEmail, String password) {

        usernameOrEmail = usernameOrEmail.toLowerCase();  // Normalize (Enhancement 4)

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{
                        COLUMN_USER_ID,
                        COLUMN_PASSWORD,
                        COLUMN_FAILED_ATTEMPTS,
                        COLUMN_LOCKED_UNTIL
                },
                COLUMN_USERNAME + "=? OR " + COLUMN_EMAIL + "=?",
                new String[]{usernameOrEmail, usernameOrEmail},
                null, null, null
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            return -1; // Not found
        }

        int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
        int failedAttempts = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_ATTEMPTS));
        long lockedUntil = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCKED_UNTIL));

        long now = System.currentTimeMillis();

        // ★ Enhancement 2 — LOCKOUT CHECK
        if (lockedUntil > now) {
            cursor.close();
            return -2; // Account locked
        }

        boolean verified = BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified;

        if (verified) {

            // ★ Enhancement 2 — Reset counters on successful login
            ContentValues resetValues = new ContentValues();
            resetValues.put(COLUMN_FAILED_ATTEMPTS, 0);
            resetValues.put(COLUMN_LOCKED_UNTIL, 0);
            db.update(TABLE_USERS, resetValues, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});

            cursor.close();
            return userId;
        }

        // WRONG PASSWORD — increment attempts
        failedAttempts++;

        ContentValues values = new ContentValues();
        values.put(COLUMN_FAILED_ATTEMPTS, failedAttempts);

        // ★ Enhancement 2 — Lock after 5 failures
        if (failedAttempts >= 5) {
            long lockFor10Min = now + (10 * 60 * 1000);
            values.put(COLUMN_LOCKED_UNTIL, lockFor10Min);
        }

        db.update(TABLE_USERS, values, COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});

        cursor.close();
        return -1;
    }


    // --------------------------------------------------------------
    // DUPLICATE CHECKS
    // --------------------------------------------------------------
    public boolean isUsernameTaken(String username) {
        username = username.toLowerCase();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    public boolean isPhoneTaken(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_PHONE + "=?",
                new String[]{phone},
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    public boolean isEmailTaken(String email) {
        email = email.toLowerCase();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    // --------------------------------------------------------------
    // WEIGHT LOGIC — Unchanged except cleaned up
    // --------------------------------------------------------------
    public boolean updateWeight(int weightId, double newWeight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, newWeight);
        return db.update(TABLE_WEIGHT, values, COLUMN_WEIGHT_ID + "=?", new String[]{String.valueOf(weightId)}) > 0;
    }


    public boolean deleteWeight(int weightId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WEIGHT, COLUMN_WEIGHT_ID + "=?", new String[]{String.valueOf(weightId)}) > 0;
    }


    public List<WeightEntry> getAllWeightEntries(int userId) {

        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_WEIGHT,
                new String[]{COLUMN_WEIGHT_ID, COLUMN_WEIGHT, COLUMN_DATE},
                COLUMN_USER_REF + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_DATE + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_ID));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                entries.add(new WeightEntry(id, weight, date));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return entries;
    }


    // --------------------------------------------------------------
    // GOALS LOGIC
    // --------------------------------------------------------------
    public boolean setGoal(int userId, double goalWeight) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE_GOALS,
                new String[]{COLUMN_GOAL_ID},
                COLUMN_USER_REF + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_REF, userId);
        values.put(COLUMN_GOAL_WEIGHT, goalWeight);

        boolean result;
        if (cursor.moveToFirst()) {
            int goalId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_ID));
            result = db.update(TABLE_GOALS, values, COLUMN_GOAL_ID + "=?", new String[]{String.valueOf(goalId)}) > 0;
        } else {
            result = db.insert(TABLE_GOALS, null, values) != -1;
        }

        cursor.close();
        return result;
    }


    public double getGoal(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_GOALS,
                new String[]{COLUMN_GOAL_WEIGHT},
                COLUMN_USER_REF + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        double goal = 0;
        if (cursor.moveToFirst()) {
            goal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_GOAL_WEIGHT));
        }

        cursor.close();
        return goal;
    }


    public long addWeightAndGetId(int userId, double weight, String date) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_REF, userId);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_DATE, date);

        return db.insert(TABLE_WEIGHT, null, values);
    }


    public String getUserPhone(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String phone = null;

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"phone"},
                "id=?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        }

        cursor.close();
        return phone;
    }
}
