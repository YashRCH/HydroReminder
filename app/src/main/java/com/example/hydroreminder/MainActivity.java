package com.example.hydroreminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor accelerometer, gyroscope, proximity;
    boolean userNearPhone = true;
    long lastReminderTime = 0;
    final int reminderInterval = 60 * 60 * 1000; // 1 hour

    ProgressBar progressBar;
    Button drinkButton, updateGoalButton;
    TextView mlCounter, requiredIntake;
    EditText ageInput, heightInput;
    Spinner sexSpinner;

    int waterProgress = 0;
    int waterGoal = 0;
    final int mlPerGlass = 200;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        progressBar = findViewById(R.id.progressBar);
        drinkButton = findViewById(R.id.drinkButton);
        updateGoalButton = findViewById(R.id.updateGoalButton);
        mlCounter = findViewById(R.id.mlCounter);
        requiredIntake = findViewById(R.id.requiredIntake);
        ageInput = findViewById(R.id.ageInput);
        heightInput = findViewById(R.id.heightInput);
        sexSpinner = findViewById(R.id.sexSpinner);

        sharedPreferences = getSharedPreferences("HydroPrefs", MODE_PRIVATE);
        waterProgress = sharedPreferences.getInt("progress", 0);
        waterGoal = sharedPreferences.getInt("goal", 10); // Default goal

        progressBar.setProgress(waterProgress * 100 / waterGoal);
        updateMLText();
        updateRequiredIntakeText();

        drinkButton.setOnClickListener(view -> {
            if (waterProgress < waterGoal) {
                waterProgress++;
                progressBar.setProgress(waterProgress * 100 / waterGoal);
                updateMLText();
                saveProgress();
                Toast.makeText(MainActivity.this, "Good job! 💧", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "You've reached your daily goal! 🎉", Toast.LENGTH_SHORT).show();
            }
        });

        updateGoalButton.setOnClickListener(view -> {
            String ageStr = ageInput.getText().toString().trim();
            String heightStr = heightInput.getText().toString().trim();
            String sex = sexSpinner.getSelectedItem().toString();

            if (!ageStr.isEmpty() && !heightStr.isEmpty()) {
                int age = Integer.parseInt(ageStr);
                int height = Integer.parseInt(heightStr);
                waterGoal = calculateWaterGoal(age, height, sex);
                sharedPreferences.edit().putInt("goal", waterGoal).apply();
                progressBar.setProgress(waterProgress * 100 / waterGoal);
                updateRequiredIntakeText();
                Toast.makeText(MainActivity.this, "Goal updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please enter age and height", Toast.LENGTH_SHORT).show();
            }
        });

        resetAtMidnight();
    }

    private int calculateWaterGoal(int age, int heightCm, String sex) {
        double multiplier = (age < 18) ? 15 : 20;
        if (sex.equalsIgnoreCase("Female")) {
            multiplier *= 0.9;
        }
        double goalMl = heightCm * multiplier;
        return (int) Math.ceil(goalMl / mlPerGlass); // Convert ml to glass count
    }

    private void updateMLText() {
        int totalMl = waterProgress * mlPerGlass;
        mlCounter.setText("Water Intake: " + totalMl + "ml");
    }

    private void updateRequiredIntakeText() {
        int goalMl = waterGoal * mlPerGlass;
        requiredIntake.setText("Required Intake: " + goalMl + "ml");
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("progress", waterProgress);
        editor.apply();
    }

    private void resetAtMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 5);
        long resetTime = calendar.getTimeInMillis() + 24 * 60 * 60 * 1000;

        new Handler().postDelayed(() -> {
            waterProgress = 0;
            progressBar.setProgress(0);
            updateMLText();
            saveProgress();
        }, resetTime - System.currentTimeMillis());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (gyroscope != null)
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (proximity != null)
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            userNearPhone = event.values[0] < proximity.getMaximumRange();
        }

        if (userNearPhone && currentTime - lastReminderTime > reminderInterval) {
            sendReminder();
            lastReminderTime = currentTime;
        }
    }

    private void sendReminder() {
        Toast.makeText(this, "Hydration Reminder: Time to drink water! 💧", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
