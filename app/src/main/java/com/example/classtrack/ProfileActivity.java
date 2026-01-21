package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    // UI
    ImageView imgProfile;
    TextView txtName, txtEmail, txtMobile;
    MaterialButton btnLogout;

    // Firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // INIT VIEWS
        imgProfile = findViewById(R.id.imgProfile);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtMobile = findViewById(R.id.txtMobile);
        btnLogout = findViewById(R.id.btnLogout);

        // FIREBASE
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // SESSION CHECK
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        // LOAD PROFILE DATA
        loadProfileData();

        // LOGOUT BUTTON CLICK
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadProfileData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("teachers")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        txtName.setText(document.getString("name"));
                        txtEmail.setText(document.getString("email"));
                        txtMobile.setText("Mobile: " + document.getString("mobile"));

                        String base64Image = document.getString("idProof");
                        if (base64Image != null && !base64Image.equals("No Image")) {
                            Bitmap bitmap = decodeBase64(base64Image);
                            if (bitmap != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private Bitmap decodeBase64(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // 🔥 IMPORTANT LOGOUT METHOD
    private void performLogout() {

        // 1. Firebase logout
        FirebaseAuth.getInstance().signOut();

        // 2. Clear SharedPreferences
        SharedPreferences.Editor editor =
                getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        // 3. Redirect to Login
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
