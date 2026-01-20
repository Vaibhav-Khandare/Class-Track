package com.example.classtrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

public class ViewAttendanceActivity extends AppCompatActivity {

    private TextView tvScreenTitle, tvTotalLectures;
    private EditText etDateFrom, etDateTo;
    private Spinner spinnerSubject;
    // UPDATED: Changed btnPrint to btnPrintPdf and added btnPrintExcel
    private Button btnShow, btnPrintPdf, btnPrintExcel;
    private RecyclerView rvReportList;
    private String selectedYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        // 1. Receive Year Data
        selectedYear = getIntent().getStringExtra("YEAR");
        if(selectedYear == null) selectedYear = "1st Year";

        // 2. Initialize Views
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvTotalLectures = findViewById(R.id.tvTotalLectures);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        spinnerSubject = findViewById(R.id.spinnerSubjectReport);
        btnShow = findViewById(R.id.btnShowReport);
        rvReportList = findViewById(R.id.rvReportList);

        // UPDATED: Initialize new button IDs
        btnPrintPdf = findViewById(R.id.btnPrintPdf);
        btnPrintExcel = findViewById(R.id.btnPrintExcel);


        // 3. Setup UI
        tvScreenTitle.setText(selectedYear + " - Report");
        setupDatePickers();
        setupSpinner();

        rvReportList.setLayoutManager(new LinearLayoutManager(this));

        // 4. Button Logic
        btnShow.setOnClickListener(v -> loadReportData());

        // Listener for PDF
        btnPrintPdf.setOnClickListener(v -> {
            Toast.makeText(this, "Generating PDF for " + selectedYear + "...", Toast.LENGTH_SHORT).show();
            // TODO: Implement PDF Generation here later
        });

        // Listener for Excel (New)
        btnPrintExcel.setOnClickListener(v -> {
            Toast.makeText(this, "Generating Excel for " + selectedYear + "...", Toast.LENGTH_SHORT).show();
            // TODO: Implement Excel Generation here later
        });
    }

    private void setupDatePickers() {
        // Simple logic to open DatePicker when clicking the EditTexts
        etDateFrom.setOnClickListener(v -> showDatePicker(etDateFrom));
        etDateTo.setOnClickListener(v -> showDatePicker(etDateTo));
    }

    private void showDatePicker(EditText targetField) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            targetField.setText(day + "/" + (month + 1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupSpinner() {
        // Choose the correct array based on the selected year
        int arrayId;
        if (selectedYear.equals("2nd Year")) {
            arrayId = R.array.subjects_2nd_year;
        } else if (selectedYear.equals("3rd Year")) {
            arrayId = R.array.subjects_3rd_year;
        } else {
            // Default to 1st year
            arrayId = R.array.subjects_1st_year;
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayId, // Uses the variable we set above
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    private void loadReportData() {
        // This is where we will query Firebase/Database later.
        // For now, we just validate the input.
        if (etDateFrom.getText().toString().isEmpty() || etDateTo.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select Date Range", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Loading data for " + spinnerSubject.getSelectedItem(), Toast.LENGTH_SHORT).show();
        tvTotalLectures.setText("Total Lectures: 12 (Mock Data)");

        // TODO: Populate RecyclerView with Adapter here
    }
}