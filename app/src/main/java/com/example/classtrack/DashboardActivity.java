package com.example.classtrack;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get saved teacher data
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String teacherName = prefs.getString("teacherName", "Teacher");

        // Display teacher info
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + teacherName);

        // Setup buttons
        Button btnMarkAttendance = findViewById(R.id.btnMarkAttendance);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnMarkAttendance.setOnClickListener(v ->
                Toast.makeText(this, "Attendance feature coming soon",
                        Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            // Clear login session
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // Go back to login
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}