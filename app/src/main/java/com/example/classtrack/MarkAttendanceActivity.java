package com.example.classtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

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

    // 🔥 Firestore Instance
    private FirebaseFirestore db;
    private String currentYearBatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        // 1. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // UI Initialization
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

        // 2. Get Year from Intent (Default to 1st Year if null)
        currentYearBatch = getIntent().getStringExtra("YEAR");
        if (currentYearBatch == null) currentYearBatch = "1st Year";

        tvTitle.setText(currentYearBatch + " - Mark Attendance");

        // 3. Setup Spinner (Subject List)
        // We only switch the Spinner resource here. Data loading happens in fetchStudentsFromFirestore().
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

        // 4. Recycler Setup
        adapter = new StudentAdapter(studentList);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);

        // 5. 🔥 FETCH STUDENTS FROM FIRESTORE
        fetchStudentsFromFirestore();

        // Event Listeners
        etFromTime.setOnClickListener(v -> showTimePicker(etFromTime));
        etToTime.setOnClickListener(v -> showTimePicker(etToTime));
        etDate.setOnClickListener(v -> showDatePicker());

        toggleAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.setAll(isChecked));
        btnClear.setOnClickListener(v -> adapter.setAll(false));

        // Save Button Logic
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAttendanceToFirestore();
            }
        });
    }

    // ---------------------------------------------------------
    //  🔥 NEW: FETCH DATA FROM FIRESTORE
    // ---------------------------------------------------------
    private void fetchStudentsFromFirestore() {
        // A. Map "1st Year" -> "co_1st" (Firestore Document Names)
        String batchDocName = "";
        if (currentYearBatch.equals("1st Year")) batchDocName = "co_1st";
        else if (currentYearBatch.equals("2nd Year")) batchDocName = "co_2nd";
        else if (currentYearBatch.equals("3rd Year")) batchDocName = "co_3rd";
        else return;

        // B. Fetch: Student -> [batchDocName] -> student_list
        db.collection("Student")
                .document(batchDocName)
                .collection("student_list")
                .orderBy("roll", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            // Handle Roll No safely
                            int roll;
                            try {
                                roll = doc.getLong("roll").intValue();
                            } catch (Exception e) {
                                // Fallback: try parsing ID if 'roll' field is missing/string
                                try {
                                    roll = Integer.parseInt(doc.getId());
                                } catch (NumberFormatException nfe) {
                                    roll = 0; // Last resort fallback
                                }
                            }

                            String name = doc.getString("name");
                            // Default status is true (Present)
                            studentList.add(new Student(roll, name, true));
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No students found for " + currentYearBatch, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error fetching students", e);
                });
    }

    // ---------------------------------------------------------
    //  🔥 FIRESTORE SAVING LOGIC (Saves the Session)
    // ---------------------------------------------------------

    private boolean validateInputs() {
        if (etDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a Date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etFromTime.getText().toString().isEmpty() || etToTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select Session Time", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spinnerSubject.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a Subject", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveAttendanceToFirestore() {
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // A. Prepare the list of students with their status
        List<Map<String, Object>> attendanceRecords = new ArrayList<>();
        int presentCount = 0;

        for (Student s : studentList) {
            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("roll", s.getRoll());
            studentMap.put("name", s.getName());

            // Check status
            boolean isPresent = s.isPresent();
            studentMap.put("status", isPresent ? "P" : "A");

            if(isPresent) presentCount++;

            attendanceRecords.add(studentMap);
        }

        // B. Parse Date String to Real Date Object
        Date dateForQuery = parseDate(etDate.getText().toString());

        // C. Create the Session Document
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("subject", spinnerSubject.getSelectedItem().toString());
        sessionMap.put("batch", currentYearBatch); // "1st Year", etc.
        sessionMap.put("dateStr", etDate.getText().toString());
        sessionMap.put("timestamp", dateForQuery); // CRITICAL: For Date Range Query
        sessionMap.put("startTime", etFromTime.getText().toString());
        sessionMap.put("endTime", etToTime.getText().toString());
        sessionMap.put("totalStudents", studentList.size());
        sessionMap.put("presentCount", presentCount);
        sessionMap.put("attendanceData", attendanceRecords); // Store the array

        // D. Push to Firestore
        db.collection("attendance_sessions")
                .add(sessionMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MarkAttendanceActivity.this, "Attendance Saved Successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MarkAttendanceActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    // ---------------------------------------------------------
    //  UI HELPERS
    // ---------------------------------------------------------
    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
            editText.setText(time);
        }, hour, minute, false);
        dialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            etDate.setText(date);
        }, year, month, day);
        dialog.show();
    }
}