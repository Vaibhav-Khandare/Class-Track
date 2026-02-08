package com.example.classtrack;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            etUsername = findViewById(R.id.username);
            etPassword = findViewById(R.id.password);
            btnLogin = findViewById(R.id.loginbtn);

            // 1. Check if already logged in when app starts
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                navigateUser();
            }

            // 2. Login Button Logic
            btnLogin.setOnClickListener(v -> loginWithUsername());

            TextView tvRegister = findViewById(R.id.tvRegister);
            if (tvRegister != null) {
                tvRegister.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
            }

        } catch (Exception e) {
            Log.e("APP_CRASH", "Error: " + e.getMessage());
        }
    }

    private void loginWithUsername() {
        String usernameInput = etUsername.getText().toString().trim();
        String passwordInput = etPassword.getText().toString().trim();

        if (usernameInput.isEmpty()) { etUsername.setError("Enter Username"); return; }
        if (passwordInput.isEmpty()) { etPassword.setError("Enter Password"); return; }

        btnLogin.setText("Finding User...");
        btnLogin.setEnabled(false);

        db.collection("teachers")
                .whereEqualTo("username", usernameInput)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String emailFound = document.getString("email");

                        if (emailFound != null) {
                            performFirebaseAuth(emailFound, passwordInput, document);
                        } else {
                            resetLoginButton();
                            Toast.makeText(this, "Error: Username has no email linked.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        resetLoginButton();
                        etUsername.setError("Username not found");
                    }
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performFirebaseAuth(String email, String password, DocumentSnapshot userDoc) {
        btnLogin.setText("Verifying...");
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveToPreferences(userDoc);
                    } else {
                        resetLoginButton();
                        etPassword.setError("Wrong Password");
                        Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveToPreferences(DocumentSnapshot document) {
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();

        String name = document.getString("name");
        String email = document.getString("email");
        String username = document.getString("username");
        String branch = document.getString("branch"); // Get branch from Firestore
        boolean isHod = Boolean.TRUE.equals(document.getBoolean("isHod"));

        editor.putString("teacherName", name != null ? name : "Unknown");
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putBoolean("isHod", isHod);
        editor.putBoolean("isLoggedIn", true);

        // 🔥 THE FIX: Save the branch to SharedPreferences immediately!
        if (branch != null && !branch.isEmpty()) {
            editor.putString("selectedBranch", branch);
        }

        editor.apply();

        Toast.makeText(MainActivity.this, "Welcome " + (name != null ? name : "Teacher"), Toast.LENGTH_SHORT).show();

        navigateUser();
    }

    private void navigateUser() {
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String savedBranch = prefs.getString("selectedBranch", null);

        Intent intent;
        // Check if branch is already saved (either from Register or previous selection)
        if (savedBranch != null && !savedBranch.isEmpty()) {
            intent = new Intent(MainActivity.this, DashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, BranchSelectionActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private void resetLoginButton() {
        btnLogin.setText("LOGIN AS TEACHER");
        btnLogin.setEnabled(true);
    }
}