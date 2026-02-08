package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    // UI Components
    ImageView imgProfile;
    TextView txtName, txtEmail, txtMobile, txtBranch, txtUsername;
    Chip badgeHod;
    MaterialButton btnLogout;

    // Firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Views
        imgProfile = findViewById(R.id.imgProfile);
        txtName = findViewById(R.id.txtName);
        txtBranch = findViewById(R.id.txtBranch);
        badgeHod = findViewById(R.id.badgeHod); // New HOD Badge

        // Card Details
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        txtMobile = findViewById(R.id.txtMobile);

        btnLogout = findViewById(R.id.btnLogout);

        // 2. Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Check Session
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        // 4. Load Data
        loadProfileData();

        // 5. Logout Listener
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadProfileData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("teachers")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // --- Fetch Data ---
                        String name = document.getString("name");
                        String branch = document.getString("branch");
                        String username = document.getString("username");
                        String email = document.getString("email");
                        String mobile = document.getString("mobile");
                        String base64Image = document.getString("idProof");
                        Boolean isHod = document.getBoolean("isHod");

                        // --- Set Data to Views ---
                        txtName.setText(name != null ? name : "Teacher");
                        txtBranch.setText(branch != null ? branch : "Unknown Dept");
                        txtUsername.setText(username != null ? "@" + username : "--");
                        txtEmail.setText(email != null ? email : "--");
                        txtMobile.setText(mobile != null ? "+91 " + mobile : "--");

                        // --- HOD Badge Logic ---
                        if (isHod != null && isHod) {
                            badgeHod.setVisibility(View.VISIBLE);
                        } else {
                            badgeHod.setVisibility(View.GONE);
                        }

                        // --- Image Logic ---
                        if (base64Image != null && !base64Image.equals("No Image") && !base64Image.isEmpty()) {
                            Bitmap bitmap = decodeBase64(base64Image);
                            if (bitmap != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private Bitmap decodeBase64(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void performLogout() {
        // 1. Firebase Logout
        FirebaseAuth.getInstance().signOut();

        // 2. Clear Local Session
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        // 3. Redirect
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}