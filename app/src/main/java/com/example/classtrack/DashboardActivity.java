package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnMark1, btnView1, btnMark2, btnView2, btnMark3, btnView3, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Fetch teacher name
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String name = prefs.getString("username", "Teacher");

        // Initialize Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + name);

        btnMark1 = findViewById(R.id.btnMark1);
        btnView1 = findViewById(R.id.btnView1);
        btnMark2 = findViewById(R.id.btnMark2);
        btnView2 = findViewById(R.id.btnView2);
        btnMark3 = findViewById(R.id.btnMark3);
        btnView3 = findViewById(R.id.btnView3);
        btnLogout = findViewById(R.id.btnLogout);

        // Set Click Listeners
        btnMark1.setOnClickListener(v -> {
            Intent intent = new Intent(this, MarkAttendanceActivity.class);
            intent.putExtra("YEAR", "1st Year");
            startActivity(intent);
        });

        Button btnMark2 = findViewById(R.id.btnMark2);
        btnMark2.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MarkAttendanceActivity.class);
            intent.putExtra("YEAR", "2nd Year"); // This triggers the PDF student list
            startActivity(intent);
        });

        // Logic for 3rd Year Button
        Button btnMark3 = findViewById(R.id.btnMark3);
        btnMark3.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MarkAttendanceActivity.class);
            intent.putExtra("YEAR", "3rd Year"); // This triggers the NEW 3rd year logic
            startActivity(intent);
        });

//        btnMark1.setOnClickListener(v -> Toast.makeText(this, "Marking 1st Year", Toast.LENGTH_SHORT).show());
        btnView1.setOnClickListener(v -> Toast.makeText(this, "Viewing 1st Year", Toast.LENGTH_SHORT).show());

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