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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardTracker extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;

    private int userId;
    private DatabaseHelper dbHelper;

    private TextView textCurrentGoal;
    private EditText editTextNewWeight, editTextNewGoal;
    private LineChart lineChart;
    private boolean triggerBelowGoal = true;

    private WeightAdapter weightAdapter;
    private List<WeightEntry> weightEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_dashboard);

        // Get user ID from intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId == -1) {
            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        // Initialize views
        textCurrentGoal = findViewById(R.id.textCurrentGoal);
        editTextNewWeight = findViewById(R.id.editTextNewWeight);
        editTextNewGoal = findViewById(R.id.editTextNewGoal);
        Button buttonAddEntry = findViewById(R.id.buttonAddEntry);
        Button buttonUpdateGoal = findViewById(R.id.buttonUpdateGoal);
        RecyclerView weightGridView = findViewById(R.id.weightGridView);
        lineChart = findViewById(R.id.lineChart);

        RadioGroup radioGroupGoalTrigger = findViewById(R.id.radioGroupGoalTrigger);
        RadioButton radioBelowGoal = findViewById(R.id.radioBelowGoal);
        RadioButton radioAboveGoal = findViewById(R.id.radioAboveGoal);

        // Default to Lose Weight
        radioBelowGoal.setChecked(true);
        triggerBelowGoal = true;

        radioGroupGoalTrigger.setOnCheckedChangeListener((group, checkedId) ->
                triggerBelowGoal = (checkedId == R.id.radioBelowGoal));

        // Load weights from DB
        weightEntries = dbHelper.getAllWeightEntries(userId);
        updateGoalDisplay();

        // Setup RecyclerView adapter
        weightAdapter = new WeightAdapter(this, dbHelper, weightEntries, new WeightAdapter.OnWeightChangeListener() {
            @Override
            public void onWeightUpdated() {
                weightEntries = dbHelper.getAllWeightEntries(userId);
                weightAdapter.setWeights(weightEntries);
                updateChart();
                checkAndSendSMS(getLatestWeight());
            }

            @Override
            public void onWeightDeleted() {
                weightEntries = dbHelper.getAllWeightEntries(userId);
                weightAdapter.setWeights(weightEntries);
                updateChart();
            }
        });

        weightGridView.setLayoutManager(new LinearLayoutManager(this));
        weightGridView.setAdapter(weightAdapter);

        // Initial chart
        updateChart();

        // Button listeners
        buttonAddEntry.setOnClickListener(v -> addNewWeightEntry());
        buttonUpdateGoal.setOnClickListener(v -> updateGoal());

        Button buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardTracker.this, SmsPermissionTracker.class);
            startActivity(intent);
        });
    }

    // Get newest weight (top of list)
    private double getLatestWeight() {
        if (!weightEntries.isEmpty()) {
            return weightEntries.get(0).getWeight();
        }
        return 0;
    }

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

        if (userId == -1) {
            Toast.makeText(this, R.string.invalid_user_id, Toast.LENGTH_SHORT).show();
            return;
        }

        // Full timestamp with seconds
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        long newId = dbHelper.addWeightAndGetId(userId, weight, date);
        if (newId == -1) {
            Toast.makeText(this, R.string.failed_to_add_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        // Reload all weights from DB
        weightEntries = dbHelper.getAllWeightEntries(userId);
        weightAdapter.setWeights(weightEntries);

        editTextNewWeight.setText("");

        // Update chart with new data
        updateChart();

        // Send SMS if applicable
        checkAndSendSMS(weight);

        Toast.makeText(this, getString(R.string.weight_added_fmt, weight), Toast.LENGTH_SHORT).show();

        //  Goal check based on radio button selection
        double goalWeight = dbHelper.getGoal(userId);

        if (triggerBelowGoal) {
            // "Lose weight" (below goal)
            if (weight <= goalWeight) {
                new AlertDialog.Builder(DashboardTracker.this, R.style.Theme_Holly_Renfrew_Weight_Tracker_Dialog)
                        .setTitle(R.string.congrats_title)
                        .setMessage(getString(R.string.met_goal_below_fmt, goalWeight))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        } else {
            // "Gain weight" (above goal)
            if (weight >= goalWeight) {
                new AlertDialog.Builder(DashboardTracker.this, R.style.Theme_Holly_Renfrew_Weight_Tracker_Dialog)
                        .setTitle(R.string.congrats_title)
                        .setMessage(getString(R.string.met_goal_above_fmt, goalWeight))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }

    }



    private void updateGoal() {
        String goalStr = editTextNewGoal.getText().toString().trim();
        if (goalStr.isEmpty()) {
            Toast.makeText(this, R.string.enter_a_goal_weight, Toast.LENGTH_SHORT).show();
            return;
        }

        double goalWeight = Double.parseDouble(goalStr);
        if (dbHelper.setGoal(userId, goalWeight)) {
            Toast.makeText(this, R.string.goal_updated, Toast.LENGTH_SHORT).show();
            updateGoalDisplay();
            updateChart();
            checkAndSendSMS(getLatestWeight());
        }
    }

    private void updateGoalDisplay() {
        double goalWeight = dbHelper.getGoal(userId);
        textCurrentGoal.setText(getString(R.string.goal_label_fmt, goalWeight));
    }

    private void updateChart() {
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < weightEntries.size(); i++) {
            int reversedIndex = weightEntries.size() - 1 - i; // oldest first
            WeightEntry we = weightEntries.get(reversedIndex);
            entries.add(new Entry(i, (float) we.getWeight()));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.weight_over_time_label));
        dataSet.setColor(ContextCompat.getColor(this, R.color.headlineColor));
        dataSet.setLineWidth(2f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.headlineColor));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.headlineColor));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // X-axis formatter to show date/time
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                if (index >= 0 && index < weightEntries.size()) {
                    String dbDate = weightEntries.get(index).getDate(); // "yyyy-MM-dd HH:mm:ss"
                    try {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date date = parser.parse(dbDate);
                        if (date != null) {
                            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
                            return formatter.format(date);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return "";
            }
        });

        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setLabelRotationAngle(-45f);
        lineChart.getXAxis().setTextColor(ContextCompat.getColor(this, R.color.headlineColor));
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.headlineColor));
        lineChart.getAxisRight().setTextColor(ContextCompat.getColor(this, R.color.headlineColor));
        lineChart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.headlineColor));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);


        // Goal line
        float goalWeight = (float) dbHelper.getGoal(userId);
        lineChart.getAxisLeft().removeAllLimitLines();
        LimitLine goalLine = new LimitLine(goalWeight);
        goalLine.setLineColor(ContextCompat.getColor(this, R.color.buttonColor));
        goalLine.setLineWidth(2f);
        lineChart.getAxisLeft().addLimitLine(goalLine);

        // Refresh chart
        lineChart.getData().notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }




    // Send SMS to user's registered phone
    private void checkAndSendSMS(double currentWeight) {
        // First check if user even wants SMS alerts
        SharedPreferences prefs = getSharedPreferences("WeightTrackerPrefs", MODE_PRIVATE);
        boolean smsEnabled = prefs.getBoolean("sms_alerts_enabled", false);
        if (!smsEnabled) return;

        double goalWeight = dbHelper.getGoal(userId);
        boolean shouldSend = (triggerBelowGoal && currentWeight <= goalWeight)
                || (!triggerBelowGoal && currentWeight >= goalWeight);
        if (!shouldSend) return;

        String smsNumber = dbHelper.getUserPhone(userId); // fetch user's phone
        if (smsNumber == null || smsNumber.isEmpty()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {

            String smsText = triggerBelowGoal
                    ? getString(R.string.sms_below_fmt, currentWeight)
                    : getString(R.string.sms_above_fmt, currentWeight);

            SmsManager smsManager = getSystemService(SmsManager.class);
            smsManager.sendTextMessage(smsNumber, null, smsText, null, null);

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }
}
