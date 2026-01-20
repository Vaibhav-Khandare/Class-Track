package com.example.classtrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etMobile, etEmail, etIdProof, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnBrowse;
    private TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Initialize Firebase (NO STORAGE NEEDED)
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        // 2. File Picker
        ActivityResultLauncher<String> picker =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        etIdProof.setText("Image Selected");
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

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        btnRegister.setEnabled(false);
        btnRegister.setText("Processing...");

        // 3. Create User in Auth
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

        // 4. Convert Image to String (Base64)
        if (selectedFileUri != null) {
            base64Image = encodeImage(selectedFileUri);
            if (base64Image == null) {
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            base64Image = "No Image";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("username", etUsername.getText().toString().trim());
        map.put("email", etEmail.getText().toString().trim());
        map.put("mobile", etMobile.getText().toString().trim());
        map.put("password", etPassword.getText().toString().trim());
        map.put("name", etUsername.getText().toString().trim());
        // 5. Store the LONG string in database
        map.put("idProof", base64Image);

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

    // Helper: Converts Image URI -> String
    private String encodeImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Resize to prevent "File too large" errors (Max 600px)
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / height;
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 600, (int)(600/ratio), false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etIdProof = findViewById(R.id.etIdProof);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private boolean validateForm() {
        if (etEmail.getText().toString().isEmpty()) return false;
        if (etPassword.getText().toString().isEmpty()) return false;
        return true;
    }
}