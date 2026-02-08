package com.example.classtrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter; // Import
import android.widget.AutoCompleteTextView; // Import
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etMobile, etEmail, etIdProof, etPassword, etConfirmPassword;
    private TextInputLayout tilUsername, tilMobile, tilEmail, tilIdProof, tilPassword, tilConfirmPassword, tilBranch;

    // 🔥 New Variable for Branch Dropdown
    private AutoCompleteTextView acBranch;

    private MaterialButton btnRegister, btnBrowse;
    private TextView tvLoginLink;
    private CheckBox cbHod;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupBranchDropdown(); // 🔥 Call the setup function

        ActivityResultLauncher<String> picker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        etIdProof.setText("File Selected: " + uri.getLastPathSegment());
                    }
                });

        btnBrowse.setOnClickListener(v -> picker.launch("image/*"));

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> {
            if (validateForm()) {
                registerUser();
            }
        });
    }

    // 🔥 Logic to populate the Dropdown
    private void setupBranchDropdown() {
        String[] branches = {"Computer", "Electrical", "Civil", "Electronics", "Mechanical A", "Mechanical B"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, branches);
        acBranch.setAdapter(adapter);
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        btnRegister.setEnabled(false);
        btnRegister.setText("Processing...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveToFirestore(user.getUid());
                    } else {
                        Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                        btnRegister.setText("REGISTER");
                    }
                });
    }

    private void saveToFirestore(String uid) {
        String base64Image = "";
        if (selectedFileUri != null) {
            base64Image = encodeImage(selectedFileUri);
        } else {
            base64Image = "No Image";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("username", etUsername.getText().toString().trim());
        map.put("email", etEmail.getText().toString().trim());

        // 🔥 SAVE SELECTED BRANCH
        map.put("branch", acBranch.getText().toString().trim());

        map.put("mobile", etMobile.getText().toString().trim());
        map.put("password", etPassword.getText().toString().trim());
        map.put("name", etUsername.getText().toString().trim());
        map.put("idProof", base64Image);
        map.put("isHod", cbHod.isChecked());

        // Always save to "teachers" collection
        db.collection("teachers").document(uid)
                .set(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRegister.setEnabled(true);
                    btnRegister.setText("REGISTER");
                });
    }

    private String encodeImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / height;
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 600, (int)(600/ratio), false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            return null;
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);

        // 🔥 Init Branch Views
        tilBranch = findViewById(R.id.tilBranch);
        acBranch = findViewById(R.id.acBranch);

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
        cbHod = findViewById(R.id.cbHod);
    }

    private boolean validateForm() {
        boolean valid = true;

        if (etUsername.getText().toString().isEmpty()) { tilUsername.setError("Username required"); valid = false; } else { tilUsername.setError(null); }
        if (etMobile.getText().toString().length() < 10) { tilMobile.setError("Enter 10 digit mobile"); valid = false; } else { tilMobile.setError(null); }
        if (etEmail.getText().toString().isEmpty()) { tilEmail.setError("Email required"); valid = false; } else { tilEmail.setError(null); }

        // 🔥 Validate Branch Selection
        if (acBranch.getText().toString().isEmpty()) { tilBranch.setError("Select Branch"); valid = false; } else { tilBranch.setError(null); }

        if (etPassword.getText().toString().isEmpty()) { tilPassword.setError("Password required"); valid = false; } else { tilPassword.setError(null); }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) { tilConfirmPassword.setError("Passwords do not match"); valid = false; } else { tilConfirmPassword.setError(null); }

        return valid;
    }
}