package com.example.holly_renfrew_weight_tracker_project;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DashboardTracker extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;

    private DatabaseHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int userId;
    private LineChart lineChart;
    private TextView textCurrentGoal;
    private EditText editTextNewWeight, editTextNewGoal;

    private boolean triggerBelowGoal = true;

    private WeightAdapter weightAdapter;
    private List<WeightEntry> weightEntries = new ArrayList<>();

    private SharedPreferences securePrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_dashboard);

        dbHelper = new DatabaseHelper(this);

        // -----------------------------
        // SECURE SHARED PREFS (ENCRYPTED)
        // -----------------------------
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            securePrefs = getSharedPreferences("WeightTrackerFallback", MODE_PRIVATE);
        }

        // -----------------------------
        // GET USER ID
        // -----------------------------
        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // -----------------------------
        // LINK UI ELEMENTS
        // -----------------------------
        textCurrentGoal = findViewById(R.id.textCurrentGoal);
        editTextNewWeight = findViewById(R.id.editTextNewWeight);
        editTextNewGoal = findViewById(R.id.editTextNewGoal);
        lineChart = findViewById(R.id.lineChart);

        Button buttonAddEntry = findViewById(R.id.buttonAddEntry);
        Button buttonUpdateGoal = findViewById(R.id.buttonUpdateGoal);
        Button buttonSettings = findViewById(R.id.buttonSettings);

        RecyclerView weightGridView = findViewById(R.id.weightGridView);
        RadioGroup radioGroupGoalTrigger = findViewById(R.id.radioGroupGoalTrigger);
        RadioButton radioBelowGoal = findViewById(R.id.radioBelowGoal);
        RadioButton radioAboveGoal = findViewById(R.id.radioAboveGoal);

        radioBelowGoal.setChecked(true);
        triggerBelowGoal = true;

        radioGroupGoalTrigger.setOnCheckedChangeListener((group, checkedId) ->{
                    if (checkedId == radioBelowGoal.getId()) {
                        triggerBelowGoal = true;
                    } else if (checkedId == radioAboveGoal.getId()) {
                        triggerBelowGoal = false;
                    }
                });

        // -----------------------------
        // LOAD DATA FROM DB (BACKGROUND THREAD)
        // -----------------------------
        executor.execute(() -> {
            weightEntries = dbHelper.getAllWeightEntries(userId);
            runOnUiThread(() -> {
                setupRecycler(weightGridView);
                updateGoalDisplay();
                updateChart();
            });
        });

        // -----------------------------
        // BUTTON LISTENERS
        // -----------------------------
        buttonAddEntry.setOnClickListener(v -> addNewWeightEntry());
        buttonUpdateGoal.setOnClickListener(v -> updateGoal());
        buttonSettings.setOnClickListener(v ->
                startActivity(new Intent(DashboardTracker.this, SmsPermissionTracker.class))
        );
    }


    // ------------------------------------------------------------
    // SETUP RECYCLER
    // ------------------------------------------------------------
    private void setupRecycler(RecyclerView rv) {
        weightAdapter = new WeightAdapter(this, dbHelper, weightEntries, new WeightAdapter.OnWeightChangeListener() {
            @Override
            public void onWeightUpdated() {
                reloadWeights();
            }

            @Override
            public void onWeightDeleted() {
                reloadWeights();
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(weightAdapter);
    }


    // ------------------------------------------------------------
    // RELOAD WEIGHTS IN BACKGROUND
    // ------------------------------------------------------------
    private void reloadWeights() {
        executor.execute(() -> {
            weightEntries = dbHelper.getAllWeightEntries(userId);
            runOnUiThread(() -> {
                weightAdapter.setWeights(weightEntries);
                updateChart();
                checkAndSendSMS(getLatestWeight());
            });
        });
    }


    // ------------------------------------------------------------
    // LATEST WEIGHT
    // ------------------------------------------------------------
    private double getLatestWeight() {
        if (!weightEntries.isEmpty()) {
            return weightEntries.get(0).getWeight();
        }
        return 0;
    }


    // ------------------------------------------------------------
    // ADD NEW WEIGHT ENTRY (THREAD SAFE)
    // ------------------------------------------------------------
    private void addNewWeightEntry() {
        String weightStr = editTextNewWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            Toast.makeText(this, R.string.enter_a_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        executor.execute(() -> {
            long newId = dbHelper.addWeightAndGetId(userId, weight, date);

            if (newId == -1) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.failed_to_add_weight, Toast.LENGTH_SHORT).show());
                return;
            }

            weightEntries = dbHelper.getAllWeightEntries(userId);

            runOnUiThread(() -> {
                weightAdapter.setWeights(weightEntries);
                editTextNewWeight.setText("");
                updateChart();
                checkAndSendSMS(weight);

                Toast.makeText(this,
                        getString(R.string.weight_added_fmt, weight),
                        Toast.LENGTH_SHORT).show();
            });
        });
    }


    // ------------------------------------------------------------
    // UPDATE GOAL
    // ------------------------------------------------------------
    private void updateGoal() {
        String goalStr = editTextNewGoal.getText().toString().trim();
        if (goalStr.isEmpty()) {
            Toast.makeText(this, R.string.enter_a_goal_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        double goal = Double.parseDouble(goalStr);

        executor.execute(() -> {
            boolean updated = dbHelper.setGoal(userId, goal);
            runOnUiThread(() -> {
                if (updated) {
                    Toast.makeText(this, R.string.goal_updated, Toast.LENGTH_SHORT).show();
                    updateGoalDisplay();
                    updateChart();
                    checkAndSendSMS(getLatestWeight());
                }
            });
        });
    }


    // ------------------------------------------------------------
    // DISPLAY GOAL
    // ------------------------------------------------------------
    private void updateGoalDisplay() {
        double goal = dbHelper.getGoal(userId);
        textCurrentGoal.setText(getString(R.string.goal_label_fmt, goal));
    }


    // ------------------------------------------------------------
    // UPDATE CHART
    // ------------------------------------------------------------
    private void updateChart() {
        List<Entry> entries = new ArrayList<>();
        List<String> formattedLabels = new ArrayList<>();

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        for (int i = 0; i < weightEntries.size(); i++) {
            WeightEntry we = weightEntries.get(weightEntries.size() - 1 - i); // oldest → newest
            entries.add(new Entry(i, (float) we.getWeight()));

            try {
                Date d = parser.parse(we.getDate());
                assert d != null;
                formattedLabels.add(formatter.format(d));
            } catch (Exception e) {
                formattedLabels.add("");
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.weight_over_time_label));
        dataSet.setColor(ContextCompat.getColor(this, R.color.headlineColor));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.headlineColor));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.headlineColor));

        lineChart.setData(new LineData(dataSet));

        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                int index = Math.round(v);
                if (index >= 0 && index < formattedLabels.size()) {
                    return formattedLabels.get(index);
                }
                return "";
            }
        });

        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();

        float goal = (float) dbHelper.getGoal(userId);
        LimitLine goalLine = new LimitLine(goal);
        lineChart.getAxisLeft().removeAllLimitLines();
        lineChart.getAxisLeft().addLimitLine(goalLine);
    }


    // ------------------------------------------------------------
    // SMS TRIGGER
    // ------------------------------------------------------------
    private void checkAndSendSMS(double currentWeight) {
        boolean smsEnabled = securePrefs.getBoolean("sms_alerts_enabled", false);
        if (!smsEnabled) return;

        double goal = dbHelper.getGoal(userId);
        boolean shouldSend =
                (triggerBelowGoal && currentWeight <= goal) ||
                        (!triggerBelowGoal && currentWeight >= goal);

        if (!shouldSend) return;

        String phone = dbHelper.getUserPhone(userId);
        if (phone == null || phone.isEmpty()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {

            String message = triggerBelowGoal
                    ? getString(R.string.sms_below_fmt, currentWeight)
                    : getString(R.string.sms_above_fmt, currentWeight);

            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }
}
