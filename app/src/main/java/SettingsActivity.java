package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    SwitchMaterial switchDarkMode, switchNotifications;
    Button btnChangeUsername, btnChangePassword, btnLogout;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // SharedPreferences
        prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);

        // Views
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        // Load saved values
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        switchNotifications.setChecked(prefs.getBoolean("notifications", true));

        // DARK MODE
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // NOTIFICATIONS
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Notifications ON" : "Notifications OFF",
                    Toast.LENGTH_SHORT).show();
        });

        // CHANGE USERNAME
        btnChangeUsername.setOnClickListener(v -> showUsernameDialog());

        // CHANGE PASSWORD
        btnChangePassword.setOnClickListener(v -> showPasswordDialog());

        // LOGOUT
        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        });
    }

    // Username Dialog
    private void showUsernameDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new username");

        new AlertDialog.Builder(this)
                .setTitle("Change Username")
                .setView(input)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String username = input.getText().toString();
                    if (!username.isEmpty()) {
                        prefs.edit().putString("username", username).apply();
                        Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // Password Dialog
    private void showPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new password");

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(input)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!password.isEmpty()) {
                        prefs.edit().putString("password", password).apply();
                        Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}

