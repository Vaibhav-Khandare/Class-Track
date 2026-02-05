package com.example.classtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences; // 🔥 Added import
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkAttendanceActivity extends AppCompatActivity {

    TextView tvTitle;
    EditText etFromTime, etToTime, etDate;
    ToggleButton toggleAttendance;
    RecyclerView recyclerStudents;
    StudentAdapter adapter;
    ArrayList<Student> studentList;
    Button btnClear, btnSave;
    Spinner spinnerSubject;

    private FirebaseFirestore db;
    private String currentYearBatch; // e.g., "1st Year"
    private String selectedBranch;   // e.g., "Computer", "Mechanical A"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        db = FirebaseFirestore.getInstance();

        // 1. Get Selected Branch from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        selectedBranch = prefs.getString("selectedBranch", "Computer"); // Default to Computer

        // 2. Get Year from Intent
        currentYearBatch = getIntent().getStringExtra("YEAR");
        if (currentYearBatch == null) currentYearBatch = "1st Year";

        // Initialize UI
        tvTitle = findViewById(R.id.tvTitle);
        spinnerSubject = findViewById(R.id.spinnerSubject);
        etFromTime = findViewById(R.id.etFromTime);
        etToTime = findViewById(R.id.etToTime);
        etDate = findViewById(R.id.etDate);
        toggleAttendance = findViewById(R.id.toggleAttendance);
        recyclerStudents = findViewById(R.id.recyclerStudents);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);

        studentList = new ArrayList<>();

        // Update Title dynamically
        tvTitle.setText(selectedBranch + " (" + currentYearBatch + ")");

        // Setup Spinner
        setupSpinner();

        // Recycler Setup
        adapter = new StudentAdapter(studentList);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);

        // 3. Fetch Data based on Branch + Year
        fetchStudentsFromFirestore();

        // Listeners
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
    }

    private void setupSpinner() {
        int subjectArrayResId;
        // You might want to make subject arrays branch-specific later
        // For now, using Year logic
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

    // ---------------------------------------------------------
    //  🔥 DYNAMIC FETCH LOGIC
    // ---------------------------------------------------------
    private void fetchStudentsFromFirestore() {
        // 1. Generate the Collection ID (e.g., "ma_1st", "co_2nd")
        String batchDocName = getCollectionCode();

        if (batchDocName == null) {
            Toast.makeText(this, "Error generating batch code", Toast.LENGTH_SHORT).show();
            return;
        }

        studentList.clear();
        adapter.notifyDataSetChanged();

        // 2. Fetch from: Student -> [batchDocName] -> student_list
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
                        // 🔥 SPECIFIC REQUIREMENT: Toast if list is empty (e.g., Mechanical)
                        Toast.makeText(this, "No roll list found for " + selectedBranch + " " + currentYearBatch, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error fetching students", e);
                });
    }

    // Helper to map Branch + Year to Firestore Doc ID
    private String getCollectionCode() {
        String yearSuffix = "";
        if (currentYearBatch.equals("1st Year")) yearSuffix = "_1st";
        else if (currentYearBatch.equals("2nd Year")) yearSuffix = "_2nd";
        else if (currentYearBatch.equals("3rd Year")) yearSuffix = "_3rd";
        else return null;

        String branchPrefix = "gen"; // Default

        // Must match names in BranchSelectionActivity
        switch (selectedBranch) {
            case "Computer": branchPrefix = "co"; break;
            case "Electrical": branchPrefix = "el"; break;
            case "Civil": branchPrefix = "cv"; break;
            case "Electronics": branchPrefix = "ec"; break;
            case "Mechanical A": branchPrefix = "ma"; break;
            case "Mechanical B": branchPrefix = "mb"; break;
        }

        return branchPrefix + yearSuffix; // e.g., "ma_1st"
    }

    // ---------------------------------------------------------
    //  SAVE LOGIC (Unchanged)
    // ---------------------------------------------------------
    private boolean validateInputs() {
        if (etDate.getText().toString().isEmpty()) return false;
        if (etFromTime.getText().toString().isEmpty() || etToTime.getText().toString().isEmpty()) return false;
        if (spinnerSubject.getSelectedItem() == null) return false;
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
        sessionMap.put("branch", selectedBranch); // 🔥 Save Branch too
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
                    Toast.makeText(this, "Attendance Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE");
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