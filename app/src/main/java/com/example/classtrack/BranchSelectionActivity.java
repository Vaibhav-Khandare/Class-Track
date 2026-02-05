package com.example.classtrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth; // Import Firebase Auth

public class BranchSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_selection);

        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String name = prefs.getString("teacherName", "Teacher");

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if(tvWelcome != null) tvWelcome.setText("Hello " + name + ",\nSelect Your Department");

        // Setup Branch Buttons
        setupBranchButton(R.id.btnComputer, "Computer");
        setupBranchButton(R.id.btnElectrical, "Electrical");
        setupBranchButton(R.id.btnCivil, "Civil");
        setupBranchButton(R.id.btnElectronics, "Electronics");
        setupBranchButton(R.id.btnMechanicalA, "Mechanical A");
        setupBranchButton(R.id.btnMechanicalB, "Mechanical B");

        // 🔥 LOGOUT LOGIC
        Button btnLogout = findViewById(R.id.btnBranchLogout);
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void setupBranchButton(int btnId, String branchName) {
        Button btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
            editor.putString("selectedBranch", branchName);
            editor.apply();

            Toast.makeText(this, "Selected: " + branchName, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // 2. Clear Local Session
        SharedPreferences.Editor editor = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        // 3. Go back to Login Screen
        Intent intent = new Intent(this, MainActivity.class);
        // Clear back stack so user can't press back to return here
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}