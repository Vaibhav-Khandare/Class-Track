package com.example.classtrack;

import android.content.Intent;
import android.database.Cursor; // For getting file name
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns; // For getting file name
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // 🔥 Important for the new Bar

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UploadFileActivity extends AppCompatActivity {

    TextView tvFileName; // Removed tvHeader as it's static in XML now
    Spinner spinnerYear;
    Button btnPickFile, btnUpload;

    private FirebaseFirestore db;
    private Uri fileUri;
    private String selectedBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);

        db = FirebaseFirestore.getInstance();

        // 1. Get Branch Name
        selectedBranch = getIntent().getStringExtra("BRANCH_NAME");
        if (selectedBranch == null) selectedBranch = "Unknown";

        // 2. Setup Toolbar (The Blue Bar at the top)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show Back Arrow
            getSupportActionBar().setTitle("Upload " + selectedBranch + " List");
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // Handle Back Click

        // 3. Initialize Views
        tvFileName = findViewById(R.id.tvFileName);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnPickFile = findViewById(R.id.btnPickPdf); // This is the invisible overlay button
        btnUpload = findViewById(R.id.btnUpload);

        // 4. Setup Spinner
        String[] years = {"1st Year", "2nd Year", "3rd Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        spinnerYear.setAdapter(adapter);

        // 5. Listeners
        btnPickFile.setOnClickListener(v -> openFilePicker());

        btnUpload.setOnClickListener(v -> {
            if (fileUri != null) {
                readExcelAndUpload(fileUri);
            } else {
                Toast.makeText(this, "Please select a file first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // Filter for .xlsx
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();

            // 🔥 Advanced: Get the actual file name to show the user
            String fileName = getFileName(fileUri);
            tvFileName.setText(fileName);

            // Enable upload button now that file is picked
            btnUpload.setEnabled(true);
            btnUpload.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))); // Optional visual cue
        }
    }

    // Helper method to get the file name from URI
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void readExcelAndUpload(Uri uri) {
        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading... Please Wait");

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            WriteBatch batch = db.batch();
            String collectionCode = getCollectionCode();

            DataFormatter formatter = new DataFormatter();
            int count = 0;

            // Skip Header
            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // 1. Roll No
                String rollStr = formatter.formatCellValue(row.getCell(0));
                // 2. Enrollment No
                String enrollStr = formatter.formatCellValue(row.getCell(1));
                // 3. Name
                String name = formatter.formatCellValue(row.getCell(2));

                if (rollStr.isEmpty() || name.isEmpty()) continue;

                try {
                    int roll = Integer.parseInt(rollStr.trim());

                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("roll", roll);
                    studentMap.put("name", name.trim());
                    studentMap.put("enrollmentNo", enrollStr.trim());

                    String docPath = "Student/" + collectionCode + "/student_list/" + roll;
                    batch.set(db.document(docPath), studentMap);
                    count++;

                } catch (NumberFormatException e) {
                    Log.e("Upload", "Skipping invalid row: " + rollStr);
                }
            }

            int finalCount = count;
            batch.commit().addOnSuccessListener(aVoid -> {
                tvFileName.setText("Success! " + finalCount + " students added.");
                Toast.makeText(this, "Database Updated Successfully!", Toast.LENGTH_LONG).show();
                btnUpload.setText("UPLOAD COMPLETE");
                btnUpload.setEnabled(true);
            }).addOnFailureListener(e -> {
                tvFileName.setText("Upload Failed");
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnUpload.setEnabled(true);
                btnUpload.setText("RETRY UPLOAD");
            });

            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
            tvFileName.setText("Error Reading File");
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnUpload.setEnabled(true);
            btnUpload.setText("RETRY UPLOAD");
        }
    }

    private String getCollectionCode() {
        String yearSuffix = "";
        String selectedYear = spinnerYear.getSelectedItem().toString();

        if (selectedYear.equals("1st Year")) yearSuffix = "_1st";
        else if (selectedYear.equals("2nd Year")) yearSuffix = "_2nd";
        else if (selectedYear.equals("3rd Year")) yearSuffix = "_3rd";

        String branchPrefix = "gen";
        switch (selectedBranch) {
            case "Computer": branchPrefix = "co"; break;
            case "Electrical": branchPrefix = "el"; break;
            case "Civil": branchPrefix = "cv"; break;
            case "Electronics": branchPrefix = "ec"; break;
            case "Mechanical A": branchPrefix = "ma"; break;
            case "Mechanical B": branchPrefix = "mb"; break;
        }
        return branchPrefix + yearSuffix;
    }
}