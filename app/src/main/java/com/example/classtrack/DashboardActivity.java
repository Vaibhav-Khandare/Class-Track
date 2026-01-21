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

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Existing UI variables
    TextView tvWelcome;
    Button btnMark1, btnView1, btnMark2, btnView2, btnMark3, btnView3, btnLogout;

    // New Drawer variables
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

        // Setup Toolbar
        setSupportActionBar(toolbar);

        // Setup Toggle (Hamburger Icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup Navigation Click Listener
        navigationView.setNavigationItemSelectedListener(this);
        // --------------------

        // Fetch teacher name
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String name = prefs.getString("username", "Teacher");

        tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + name);

        // Initialize Buttons
        btnMark1 = findViewById(R.id.btnMark1);
        btnView1 = findViewById(R.id.btnView1);
        btnMark2 = findViewById(R.id.btnMark2);
        btnView2 = findViewById(R.id.btnView2);
        btnMark3 = findViewById(R.id.btnMark3);
        btnView3 = findViewById(R.id.btnView3);
        btnLogout = findViewById(R.id.btnLogout);

        // Button Listeners (Existing Logic)
        btnMark1.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "1st Year")));
        btnMark2.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "2nd Year")));
        btnMark3.setOnClickListener(v -> startActivity(new Intent(this, MarkAttendanceActivity.class).putExtra("YEAR", "3rd Year")));

        // VIEW Button Listeners (New Logic)
        btnView1.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "1st Year")));
        btnView2.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "2nd Year")));
        btnView3.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class).putExtra("YEAR", "3rd Year")));
        // Main Logout Button Logic
        btnLogout.setOnClickListener(v -> performLogout());
    }

    // Handle Drawer Item Clicks
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home, just close drawer
        } else if (id == R.id.nav_profile) {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Extracted Logout Logic to use in both Button and Drawer
    private void performLogout() {
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Handle Back Press to close drawer first
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}