package com.example.classtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkAttendanceActivity extends AppCompatActivity {

    // UI Variables
    TextView tvTitle, tvSummary;
    EditText etFromTime, etToTime, etDate;
    ToggleButton toggleAttendance;
    RecyclerView recyclerStudents;
    StudentAdapter adapter;
    ArrayList<Student> studentList;
    Button btnClear, btnSave;
    Spinner spinnerSubject;

    // Collapsible Logic Variables
    LinearLayout layoutCardHeader, layoutFormBody;
    ImageView btnExpandCollapse;
    boolean isFormExpanded = true;

    private FirebaseFirestore db;
    private String currentYearBatch;
    private String selectedBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        db = FirebaseFirestore.getInstance();

        // 1. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Get Data
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        selectedBranch = prefs.getString("selectedBranch", "Computer");
        currentYearBatch = getIntent().getStringExtra("YEAR");
        if (currentYearBatch == null) currentYearBatch = "1st Year";

        // 3. Initialize Views
        tvTitle = findViewById(R.id.tvTitle);
        tvSummary = findViewById(R.id.tvSummary);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        etFromTime = findViewById(R.id.etFromTime);
        etToTime = findViewById(R.id.etToTime);
        etDate = findViewById(R.id.etDate);
        toggleAttendance = findViewById(R.id.toggleAttendance);
        recyclerStudents = findViewById(R.id.recyclerStudents);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);

        // Collapsible Views
        layoutCardHeader = findViewById(R.id.layoutCardHeader);
        layoutFormBody = findViewById(R.id.layoutFormBody);
        btnExpandCollapse = findViewById(R.id.btnExpandCollapse);

        studentList = new ArrayList<>();

        // Set Title
        tvTitle.setText(selectedBranch + " (" + currentYearBatch + ")");

        // Setup Spinner
        setupSpinner();

        // Recycler Setup
        adapter = new StudentAdapter(studentList);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);

        // Fetch Data
        fetchStudentsFromFirestore();

        // 4. Listeners
        etFromTime.setOnClickListener(v -> showTimePicker(etFromTime));
        etToTime.setOnClickListener(v -> showTimePicker(etToTime));
        etDate.setOnClickListener(v -> showDatePicker());

        toggleAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.setAll(isChecked));
        btnClear.setOnClickListener(v -> adapter.setAll(false));

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAttendanceToFirestore();
            }
        });

        // Toggle Form Listener
        layoutCardHeader.setOnClickListener(v -> toggleForm());
    }

    // ---------------------------------------------------------
    //  🔥 UPDATED COLLAPSIBLE FORM LOGIC
    // ---------------------------------------------------------
    private void toggleForm() {
        if (isFormExpanded) {
            // --- COLLAPSE ACTION ---
            layoutFormBody.setVisibility(View.GONE);
            btnExpandCollapse.setImageResource(android.R.drawable.arrow_down_float);

            // 1. Get Subject
            String subject = "";
            if (spinnerSubject.getSelectedItem() != null) {
                subject = spinnerSubject.getSelectedItem().toString();
            }

            // 2. Get Date & Time
            String date = etDate.getText().toString();
            String time = "";
            if (!etFromTime.getText().toString().isEmpty() && !etToTime.getText().toString().isEmpty()) {
                time = "(" + etFromTime.getText().toString() + " - " + etToTime.getText().toString() + ")";
            }

            // 3. Build Summary String: "OSY • 08/02/2026 (12:00 - 01:00)"
            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append(subject);

            if (!date.isEmpty()) {
                summaryBuilder.append(" • ").append(date);
            }
            if (!time.isEmpty()) {
                summaryBuilder.append(" ").append(time);
            } else if (date.isEmpty()) {
                summaryBuilder.append(" • Tap to add details");
            }
            summaryBuilder.append(" • Tap to Edit");
            tvSummary.setText(summaryBuilder.toString());
            tvSummary.setVisibility(View.VISIBLE);

            isFormExpanded = false;
        } else {
            // --- EXPAND ACTION ---
            layoutFormBody.setVisibility(View.VISIBLE);
            btnExpandCollapse.setImageResource(android.R.drawable.arrow_up_float);
            tvSummary.setVisibility(View.GONE); // Hide summary when expanded
            isFormExpanded = true;
        }
    }

    private void setupSpinner() {
        int subjectArrayResId;
        if (currentYearBatch.equals("2nd Year")) {
            subjectArrayResId = R.array.subjects_2nd_year;
        } else if (currentYearBatch.equals("3rd Year")) {
            subjectArrayResId = R.array.subjects_3rd_year;
        } else {
            subjectArrayResId = R.array.subjects_1st_year;
        }

        ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(this,
                subjectArrayResId, android.R.layout.simple_spinner_item);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);
    }

    private void fetchStudentsFromFirestore() {
        String batchDocName = getCollectionCode();
        if (batchDocName == null) return;

        studentList.clear();
        adapter.notifyDataSetChanged();

        db.collection("Student")
                .document(batchDocName)
                .collection("student_list")
                .orderBy("roll", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            int roll = 0;
                            try {
                                roll = doc.getLong("roll").intValue();
                            } catch (Exception e) {
                                try {
                                    roll = Integer.parseInt(doc.getId());
                                } catch (NumberFormatException ex) { }
                            }
                            String name = doc.getString("name");
                            studentList.add(new Student(roll, name, true));
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No roll list found for " + selectedBranch, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String getCollectionCode() {
        String yearSuffix = "";
        if (currentYearBatch.equals("1st Year")) yearSuffix = "_1st";
        else if (currentYearBatch.equals("2nd Year")) yearSuffix = "_2nd";
        else if (currentYearBatch.equals("3rd Year")) yearSuffix = "_3rd";
        else return null;

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

    private boolean validateInputs() {
        if (etDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select Date", Toast.LENGTH_SHORT).show();
            if (!isFormExpanded) toggleForm();
            return false;
        }
        if (etFromTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select Start Time", Toast.LENGTH_SHORT).show();
            if (!isFormExpanded) toggleForm();
            return false;
        }
        return true;
    }

    private void saveAttendanceToFirestore() {
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        List<Map<String, Object>> attendanceRecords = new ArrayList<>();
        int presentCount = 0;

        for (Student s : studentList) {
            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("roll", s.getRoll());
            studentMap.put("name", s.getName());
            boolean isPresent = s.isPresent();
            studentMap.put("status", isPresent ? "P" : "A");
            if (isPresent) presentCount++;
            attendanceRecords.add(studentMap);
        }

        Date dateForQuery = parseDate(etDate.getText().toString());

        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("subject", spinnerSubject.getSelectedItem().toString());
        sessionMap.put("batch", currentYearBatch);
        sessionMap.put("branch", selectedBranch);
        sessionMap.put("dateStr", etDate.getText().toString());
        sessionMap.put("timestamp", dateForQuery);
        sessionMap.put("startTime", etFromTime.getText().toString());
        sessionMap.put("endTime", etToTime.getText().toString());
        sessionMap.put("totalStudents", studentList.size());
        sessionMap.put("presentCount", presentCount);
        sessionMap.put("attendanceData", attendanceRecords);

        db.collection("attendance_sessions")
                .add(sessionMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE ATTENDANCE");
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

    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            editText.setText(time);

            // Auto-Collapse Logic
            if (editText.getId() == R.id.etToTime && !etFromTime.getText().toString().isEmpty()) {
                if (isFormExpanded) {
                    toggleForm();
                }
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        dialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            etDate.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }
}