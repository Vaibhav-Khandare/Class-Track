package com.example.classtrack;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("APP_DEBUG", "MainActivity started");

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this);
            db = FirebaseFirestore.getInstance();
            Log.d("APP_DEBUG", "Firebase initialized");

            // Initialize views
            etUsername = findViewById(R.id.username);
            etPassword = findViewById(R.id.password);
            btnLogin = findViewById(R.id.loginbtn);

            if (etUsername == null || etPassword == null || btnLogin == null) {
                Toast.makeText(this, "UI elements not found!", Toast.LENGTH_LONG).show();
                Log.e("APP_DEBUG", "Some UI elements are null");
                return;
            }

            Log.d("APP_DEBUG", "All UI elements found");

            // Check if already logged in
            checkExistingLogin();

            // Set click listeners
            btnLogin.setOnClickListener(v -> {
                Log.d("APP_DEBUG", "Login button clicked");
                loginTeacher();
            });

            TextView tvRegister = findViewById(R.id.tvRegister);
            if (tvRegister != null) {
                tvRegister.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(intent);
                });
            }

            Log.d("APP_DEBUG", "MainActivity setup complete");

        } catch (Exception e) {
            Log.e("APP_CRASH", "Crash in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "App error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkExistingLogin() {
        try {
            SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

            if (isLoggedIn) {
                Log.d("APP_DEBUG", "Already logged in, going to dashboard");
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        } catch (Exception e) {
            Log.e("APP_DEBUG", "Error in checkExistingLogin: " + e.getMessage());
        }
    }

    private void loginTeacher() {
        try {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            Log.d("LOGIN_ATTEMPT", "Username: " + username + ", Password length: " + password.length());

            // Validation
            if (username.isEmpty()) {
                etUsername.setError("Enter username");
                etUsername.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Enter password");
                etPassword.requestFocus();
                return;
            }

            // Show loading state
            btnLogin.setText("Checking...");
            btnLogin.setEnabled(false);

            // SIMPLE TEST - Remove Firestore temporarily
            testLoginWithoutFirebase(username, password);

        } catch (Exception e) {
            Log.e("LOGIN_CRASH", "Crash in loginTeacher: " + e.getMessage(), e);
            btnLogin.setText("LOGIN");
            btnLogin.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testLoginWithoutFirebase(String username, String password) {
        // TEMPORARY: Test without Firebase
        Log.d("LOGIN_TEST", "Testing without Firebase");

        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            try {
                // For testing, accept these credentials
                if (username.equals("vaibhav") && password.equals("vaibhav@123")) {
                    Log.d("LOGIN_TEST", "Test credentials accepted");

                    // Save session
                    SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
                    editor.putString("username", username);
                    editor.putString("teacherName", "Vaibhav Khandare");
                    editor.putString("email", "vaibhavkhandare2007@gmail.com");
                    editor.putString("mobile", "9604184377");
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();

                    Toast.makeText(this, "Welcome Vaibhav!", Toast.LENGTH_SHORT).show();

                    // Go to Dashboard
                    Intent intent = new Intent(this, DashboardActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    btnLogin.setText("LOGIN");
                    btnLogin.setEnabled(true);
                }
            } catch (Exception e) {
                Log.e("LOGIN_TEST", "Error in test: " + e.getMessage());
                btnLogin.setText("LOGIN");
                btnLogin.setEnabled(true);
                Toast.makeText(this, "Test error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, 1500); // 1.5 second delay
    }
}