package com.example.classtrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewAttendanceActivity extends AppCompatActivity {

    private TextView tvScreenTitle, tvTotalLectures;
    private EditText etDateFrom, etDateTo;
    private Spinner spinnerSubject;
    private Button btnShow, btnPrintPdf, btnPrintExcel;
    private RecyclerView rvReportList;
    private String selectedYear;

    // Firestore
    private FirebaseFirestore db;
    private ReportAdapter adapter;
    private List<StudentReport> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        db = FirebaseFirestore.getInstance();

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
        btnPrintPdf = findViewById(R.id.btnPrintPdf);
        btnPrintExcel = findViewById(R.id.btnPrintExcel);

        // 3. Setup UI
        tvScreenTitle.setText(selectedYear + " - Report");
        setupDatePickers();
        setupSpinner();

        rvReportList.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();

        // 4. Button Logic
        btnShow.setOnClickListener(v -> loadReportData());
    }

    private void setupDatePickers() {
        etDateFrom.setOnClickListener(v -> showDatePicker(etDateFrom));
        etDateTo.setOnClickListener(v -> showDatePicker(etDateTo));
    }

    private void showDatePicker(EditText targetField) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            targetField.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupSpinner() {
        int arrayId;
        if (selectedYear.equals("2nd Year")) {
            arrayId = R.array.subjects_2nd_year;
        } else if (selectedYear.equals("3rd Year")) {
            arrayId = R.array.subjects_3rd_year;
        } else {
            arrayId = R.array.subjects_1st_year;
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    // 🔥 MAIN LOGIC: Fetch Data from Firestore
    private void loadReportData() {
        String fromDateStr = etDateFrom.getText().toString();
        String toDateStr = etDateTo.getText().toString();
        String subject = spinnerSubject.getSelectedItem().toString();

        if (fromDateStr.isEmpty() || toDateStr.isEmpty()) {
            Toast.makeText(this, "Please select Date Range", Toast.LENGTH_SHORT).show();
            return;
        }

        btnShow.setEnabled(false);
        btnShow.setText("Loading...");

        Date startDate = parseDate(fromDateStr);
        Date endDate = parseDate(toDateStr);

        // Add 1 day to End Date to make sure we include the full day
        Calendar c = Calendar.getInstance();
        c.setTime(endDate);
        c.add(Calendar.DATE, 1);
        Date endDateInclusive = c.getTime();

        // 1. Query Firestore for Sessions
        db.collection("attendance_sessions")
                .whereEqualTo("batch", selectedYear)
                .whereEqualTo("subject", subject)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThan("timestamp", endDateInclusive)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    // 2. Process Data
                    int totalLecturesCount = querySnapshot.size();
                    tvTotalLectures.setText("Total Lectures: " + totalLecturesCount);

                    // Use a Map to aggregate data: RollNo -> StudentReport Object
                    Map<String, StudentReport> aggregationMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Get the list of students from this specific session
                        List<Map<String, Object>> studentsInSession = (List<Map<String, Object>>) doc.get("attendanceData");

                        if (studentsInSession != null) {
                            for (Map<String, Object> studentMap : studentsInSession) {
                                // Extract data
                                int roll = ((Long) studentMap.get("roll")).intValue();
                                String name = (String) studentMap.get("name");
                                String status = (String) studentMap.get("status"); // "P" or "A"

                                String key = String.valueOf(roll);

                                // If student not in map, add them
                                if (!aggregationMap.containsKey(key)) {
                                    aggregationMap.put(key, new StudentReport(roll, name, 0));
                                }

                                // If present, increment count
                                if ("P".equals(status)) {
                                    aggregationMap.get(key).incrementPresent();
                                }
                            }
                        }
                    }

                    // 3. Convert Map to List for Adapter
                    reportList = new ArrayList<>(aggregationMap.values());

                    // Sort by Roll Number
                    Collections.sort(reportList, (o1, o2) -> Integer.compare(o1.getRoll(), o2.getRoll()));

                    // 4. Update Adapter (Need to pass Custom Adapter logic here)
                    // You need to create a simple Adapter called ReportAdapter
                    ReportAdapter adapter = new ReportAdapter(reportList, totalLecturesCount);
                    rvReportList.setAdapter(adapter);

                    btnShow.setEnabled(true);
                    btnShow.setText("SHOW REPORT");
                    Toast.makeText(this, "Report Loaded!", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    btnShow.setEnabled(true);
                    btnShow.setText("SHOW REPORT");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("REPORT_ERROR", e.getMessage());
                });
    }

    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }
}