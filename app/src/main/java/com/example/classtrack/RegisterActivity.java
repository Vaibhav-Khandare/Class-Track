package com.example.classtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etMobile, etEmail,
            etIdProof, etPassword, etConfirmPassword;

    private TextInputLayout tilUsername, tilMobile, tilEmail,
            tilPassword, tilConfirmPassword;

    private MaterialButton btnRegister, btnBrowse;
    private TextView tvLoginLink;

    // 🔥 Firebase Auth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        // 🔥 Firebase initialize
        mAuth = FirebaseAuth.getInstance();

        // 📂 File Picker for ID Proof
        ActivityResultLauncher<String> picker =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        etIdProof.setText("File Selected");
                    }
                });

        btnBrowse.setOnClickListener(v -> picker.launch("image/*"));

        // 🔁 Redirect to Login
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });

        // 🔘 Register Button
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                registerUserWithFirebase();
            }
        });
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etIdProof = findViewById(R.id.etIdProof);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tilUsername = findViewById(R.id.tilUsername);
        tilMobile = findViewById(R.id.tilMobile);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        btnBrowse = findViewById(R.id.btnBrowse);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private boolean validateForm() {
        boolean valid = true;

        if (etUsername.getText().toString().trim().isEmpty()) {
            tilUsername.setError("Username required");
            valid = false;
        } else tilUsername.setError(null);

        if (etMobile.getText().toString().trim().length() != 10) {
            tilMobile.setError("Enter 10 digit mobile");
            valid = false;
        } else tilMobile.setError(null);

        if (etEmail.getText().toString().trim().isEmpty()) {
            tilEmail.setError("Email required");
            valid = false;
        } else tilEmail.setError(null);

        if (etPassword.getText().toString().trim().length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else tilPassword.setError(null);

        if (!etPassword.getText().toString().trim()
                .equals(etConfirmPassword.getText().toString().trim())) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        } else tilConfirmPassword.setError(null);

        return valid;
    }

    // 🔥 Firebase Registration Logic (UPDATED)
    private void registerUserWithFirebase() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                RegisterActivity.this,
                                "Registration Successful. Please Login.",
                                Toast.LENGTH_SHORT
                        ).show();

                        // 🔐 IMPORTANT: logout after registration
                        mAuth.signOut();

                        // 👉 Go to Login screen
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();

                    } else {
                        Toast.makeText(
                                RegisterActivity.this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
