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
    private static final int DATABASE_VERSION = 2; // incremented for new columns

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_PHONE = "phone";

    // Weight entries table
    private static final String TABLE_WEIGHT = "weight_entries";
    private static final String COLUMN_WEIGHT_ID = "id";
    private static final String COLUMN_USER_REF = "user_id";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_DATE = "date";

    // Goals table
    private static final String TABLE_GOALS = "goals";
    private static final String COLUMN_GOAL_ID = "id";
    private static final String COLUMN_GOAL_WEIGHT = "goal_weight";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_USERNAME + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_PHONE + " TEXT UNIQUE)";
        db.execSQL(createUsersTable);

        String createWeightTable = "CREATE TABLE " + TABLE_WEIGHT + " (" +
                COLUMN_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_REF + " INTEGER, " +
                COLUMN_WEIGHT + " REAL, " +
                COLUMN_DATE + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_USER_REF + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createWeightTable);

        String createGoalsTable = "CREATE TABLE " + TABLE_GOALS + " (" +
                COLUMN_GOAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_REF + " INTEGER, " +
                COLUMN_GOAL_WEIGHT + " REAL, " +
                "FOREIGN KEY(" + COLUMN_USER_REF + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createGoalsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_EMAIL + " TEXT UNIQUE");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_PHONE + " TEXT UNIQUE");
        }
        // Existing tables will remain
    }

    // -----------------------------
    // User Methods
    // -----------------------------
    public boolean registerUser(String email, String username, String password, String phone) {
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, hash); // store HASH, not plaintext
        values.put(COLUMN_PHONE, phone);
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    public int loginUser(String usernameOrEmail, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_PASSWORD},
                "(" + COLUMN_USERNAME + "=? OR " + COLUMN_EMAIL + "=?)",
                new String[]{usernameOrEmail, usernameOrEmail},
                null, null, null);

        int userId = -1;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            String stored = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));

            boolean verified;
            if (stored != null && stored.startsWith("$2")) {
                // Normal path: stored is a BCrypt hash
                verified = BCrypt.verifyer().verify(password.toCharArray(), stored).verified;
            } else {
                // Migration path (old plaintext rows): accept once, then upgrade to hash
                verified = password.equals(stored);
                if (verified) {
                    String newHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
                    ContentValues v = new ContentValues();
                    v.put(COLUMN_PASSWORD, newHash);
                    db.update(TABLE_USERS, v, COLUMN_USER_ID + "=?", new String[]{String.valueOf(id)});
                }
            }
            if (verified) userId = id;
        }
        cursor.close();
        return userId;
    }


    public boolean isUsernameTaken(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isPhoneTaken(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_PHONE + "=?", new String[]{phone}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // -----------------------------
    // Weight Methods
    // -----------------------------

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
        Cursor cursor = db.query(TABLE_WEIGHT,
                new String[]{COLUMN_WEIGHT_ID, COLUMN_WEIGHT, COLUMN_DATE},
                COLUMN_USER_REF + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_DATE + " DESC");

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

    // -----------------------------
    // Goal Methods
    // -----------------------------
    public boolean setGoal(int userId, double goalWeight) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_GOALS, new String[]{COLUMN_GOAL_ID},
                COLUMN_USER_REF + "=?", new String[]{String.valueOf(userId)}, null, null, null);

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
        Cursor cursor = db.query(TABLE_GOALS,
                new String[]{COLUMN_GOAL_WEIGHT},
                COLUMN_USER_REF + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

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

    public boolean isEmailTaken(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public String getUserPhone(int userId) {
        String phone = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"phone"},
                "id=?", new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        }
        cursor.close();
        return phone;
    }

}
