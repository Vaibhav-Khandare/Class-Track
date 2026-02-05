package com.example.classtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class UploadStudentListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_student_list);

        // Initialize Buttons
        setupBranchButton(R.id.btnComputer, "Computer");
        setupBranchButton(R.id.btnElectrical, "Electrical");
        setupBranchButton(R.id.btnCivil, "Civil");
        setupBranchButton(R.id.btnElectronics, "Electronics");
        setupBranchButton(R.id.btnMechA, "Mechanical A");
        setupBranchButton(R.id.btnMechB, "Mechanical B");
    }

    private void setupBranchButton(int btnId, String branchName) {
        Button btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            // Pass the selected branch to the next screen
            Intent intent = new Intent(this, UploadFileActivity.class);
            intent.putExtra("BRANCH_NAME", branchName);
            startActivity(intent);
        });
    }
}