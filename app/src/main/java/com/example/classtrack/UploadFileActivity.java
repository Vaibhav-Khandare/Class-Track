package com.example.classtrack;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class UploadFileActivity extends AppCompatActivity {

    private Button btnPickPdf, btnUpload;
    private TextView tvFileName, tvHeader;
    private Spinner spinnerYear;
    private Uri pdfUri;
    private String selectedBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);

        // Get the branch passed from previous screen
        selectedBranch = getIntent().getStringExtra("BRANCH_NAME");

        // Init Views
        tvHeader = findViewById(R.id.tvHeader);
        tvFileName = findViewById(R.id.tvFileName);
        btnPickPdf = findViewById(R.id.btnPickPdf);
        btnUpload = findViewById(R.id.btnUpload);
        spinnerYear = findViewById(R.id.spinnerYear);

        tvHeader.setText("Upload List for\n" + selectedBranch);

        // Setup Year Spinner
        String[] years = {"1st Year", "2nd Year", "3rd Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        spinnerYear.setAdapter(adapter);

        // PDF Picker Logic
        ActivityResultLauncher<String> pdfPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        pdfUri = uri;
                        tvFileName.setText("Selected: " + uri.getLastPathSegment());
                        btnUpload.setEnabled(true);
                    }
                });

        btnPickPdf.setOnClickListener(v -> pdfPicker.launch("application/pdf"));

        btnUpload.setOnClickListener(v -> {
            String selectedYear = spinnerYear.getSelectedItem().toString();
            Toast.makeText(this, "Uploading for " + selectedBranch + " (" + selectedYear + ")...", Toast.LENGTH_SHORT).show();
            // Backend logic to parse PDF and save to Firebase will go here
        });
    }
}