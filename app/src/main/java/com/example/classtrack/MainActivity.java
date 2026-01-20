package com.example.classtrack;

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

    // Firebase Instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            // Setup Views
            etUsername = findViewById(R.id.username);
            etPassword = findViewById(R.id.password);
            btnLogin = findViewById(R.id.loginbtn);

            checkExistingLogin();

            btnLogin.setOnClickListener(v -> loginWithUsername());

            TextView tvRegister = findViewById(R.id.tvRegister);
            if (tvRegister != null) {
                tvRegister.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                });
            }

        } catch (Exception e) {
            Log.e("APP_CRASH", "Error: " + e.getMessage());
        }
    }

    private void checkExistingLogin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (currentUser != null && isLoggedIn) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
    }

    // --- NEW LOGIC STARTS HERE ---

    private void loginWithUsername() {
        String usernameInput = etUsername.getText().toString().trim();
        String passwordInput = etPassword.getText().toString().trim();

        if (usernameInput.isEmpty()) {
            etUsername.setError("Enter Username");
            return;
        }
        if (passwordInput.isEmpty()) {
            etPassword.setError("Enter Password");
            return;
        }

        btnLogin.setText("Finding User...");
        btnLogin.setEnabled(false);

        // STEP 1: Find the email associated with this username
        db.collection("teachers")
                .whereEqualTo("username", usernameInput) // Look for the username field
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Username found! Get the email.
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String emailFound = document.getString("email");

                        if (emailFound != null) {
                            // STEP 2: Use the found email + entered password to Authenticate
                            performFirebaseAuth(emailFound, passwordInput, document);
                        } else {
                            btnLogin.setText("LOGIN");
                            btnLogin.setEnabled(true);
                            Toast.makeText(this, "Error: Username has no email linked.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Username does not exist
                        btnLogin.setText("LOGIN");
                        btnLogin.setEnabled(true);
                        etUsername.setError("Username not found");
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setText("LOGIN");
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void performFirebaseAuth(String email, String password, DocumentSnapshot userDoc) {
        btnLogin.setText("Verifying...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login Success
                        Log.d("LOGIN", "Login success");
                        saveToPreferences(userDoc);
                    } else {
                        // Password likely wrong
                        btnLogin.setText("LOGIN");
                        btnLogin.setEnabled(true);
                        etPassword.setError("Wrong Password");
                        Toast.makeText(this, "Authentication Failed (Wrong Password)", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveToPreferences(DocumentSnapshot document) {
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();

        String name = document.getString("name");
        String email = document.getString("email");
        String mobile = document.getString("mobile");
        String username = document.getString("username");

        editor.putString("teacherName", name != null ? name : "Unknown");
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("mobile", mobile);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        Toast.makeText(MainActivity.this, "Welcome " + (name != null ? name : "Teacher"), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}