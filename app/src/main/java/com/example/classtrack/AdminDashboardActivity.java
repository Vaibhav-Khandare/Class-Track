package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnUploadList, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvWelcome = findViewById(R.id.tvAdminWelcome);
        btnUploadList = findViewById(R.id.btnUploadList);
        btnLogout = findViewById(R.id.btnAdminLogout);

        // Set Welcome Name
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String name = prefs.getString("username", "Admin");
        tvWelcome.setText("Welcome, " + name);

        // Upload Button Logic
        btnUploadList.setOnClickListener(v -> {
            // Toast.makeText(this, "Feature Coming Soon: Upload PDF", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this,UploadStudentListActivity.class)); // We will build this next
        });

        // Logout Logic
        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}