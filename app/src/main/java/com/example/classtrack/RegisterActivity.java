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

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etMobile, etEmail, etIdProof, etPassword, etConfirmPassword;
    private TextInputLayout tilUsername, tilMobile, tilEmail, tilIdProof, tilPassword, tilConfirmPassword;
    private MaterialButton btnRegister, btnBrowse;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        // 1. File Picker for ID Proof
        ActivityResultLauncher<String> picker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if(uri != null) etIdProof.setText("File Selected: " + uri.getLastPathSegment());
                });

        btnBrowse.setOnClickListener(v -> picker.launch("image/*"));

        // 2. Redirect to Login (MainActivity)
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 3. Register Button with enhanced validation
        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                // Logic to save to Firebase goes here
                Toast.makeText(this, "Processing Registration...", Toast.LENGTH_SHORT).show();
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

        // Parent Layouts for showing errors
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

        if (etUsername.getText().toString().isEmpty()) {
            tilUsername.setError("Username required");
            valid = false;
        } else { tilUsername.setError(null); }

        if (etMobile.getText().toString().length() < 10) {
            tilMobile.setError("Enter 10 digit mobile");
            valid = false;
        } else { tilMobile.setError(null); }

        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        } else { tilConfirmPassword.setError(null); }

        return valid;
    }
}