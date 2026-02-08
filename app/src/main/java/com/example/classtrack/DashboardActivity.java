package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // UI variables
    TextView tvWelcome, tvDepartment;
    Button btnMark1, btnView1, btnMark2, btnView2, btnMark3, btnView3, btnLogout;

    // Drawer variables
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --- DRAWER SETUP ---
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        // --------------------

        // 🔥 FETCH DATA FROM SHARED PREFS
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String name = prefs.getString("username", "Teacher");
        String branch = prefs.getString("selectedBranch", "Unknown Department");
        boolean isHod = prefs.getBoolean("isHod", false);

        // Initialize and Set Text
        tvWelcome = findViewById(R.id.tvWelcome);
        tvDepartment = findViewById(R.id.tvDepartment);

        tvWelcome.setText("Welcome, " + name);
        tvDepartment.setText(branch + " Department");

        // HOD Logic: Show upload option if HOD
        if (isHod) {
            navigationView.getMenu().findItem(R.id.nav_upload_pdf).setVisible(true);
        }

        // Initialize Buttons
        btnMark1 = findViewById(R.id.btnMark1);
        btnView1 = findViewById(R.id.btnView1);
        btnMark2 = findViewById(R.id.btnMark2);
        btnView2 = findViewById(R.id.btnView2);
        btnMark3 = findViewById(R.id.btnMark3);
        btnView3 = findViewById(R.id.btnView3);
        btnLogout = findViewById(R.id.btnLogout);

        // Button Listeners
        btnMark1.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "1st Year")));
        btnMark2.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "2nd Year")));
        btnMark3.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "3rd Year")));

        btnView1.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "1st Year")));
        btnView2.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "2nd Year")));
        btnView3.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "3rd Year")));

        btnLogout.setOnClickListener(v -> performLogout());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_upload_pdf) {

            // 🔥 HOD SMART UPLOAD LOGIC 🔥
            SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            String savedBranch = prefs.getString("selectedBranch", null);

            if (savedBranch != null && !savedBranch.isEmpty()) {
                // Case 1: Branch is already known -> Go directly to File Upload screen
                Intent intent = new Intent(this, UploadFileActivity.class);
                intent.putExtra("BRANCH_NAME", savedBranch); // Pass the branch automatically
                startActivity(intent);
            } else {
                // Case 2: Branch is unknown -> Go to Selection screen first
                startActivity(new Intent(this, UploadStudentListActivity.class));
            }

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}