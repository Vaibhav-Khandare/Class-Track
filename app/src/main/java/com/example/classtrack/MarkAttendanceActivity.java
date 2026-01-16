package com.example.classtrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;

public class MarkAttendanceActivity extends AppCompatActivity {

    TextView tvTitle;
    EditText etFromTime, etToTime, etDate;
    ToggleButton toggleAttendance;
    RecyclerView recyclerStudents;
    StudentAdapter adapter;
    ArrayList<Student> studentList;
    Button btnClear, btnSave, btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        // Title
        tvTitle = findViewById(R.id.tvTitle);

        String year = getIntent().getStringExtra("YEAR");
        if (year != null) {
            tvTitle.setText(year + " - Mark Attendance");
        }

        // Time inputs
        etFromTime = findViewById(R.id.etFromTime);
        etToTime = findViewById(R.id.etToTime);

        etFromTime.setOnClickListener(v -> showTimePicker(etFromTime));
        etToTime.setOnClickListener(v -> showTimePicker(etToTime));

        // Date input
        etDate = findViewById(R.id.etDate);
        etDate.setOnClickListener(v -> showDatePicker());

        // Toggle
        toggleAttendance = findViewById(R.id.toggleAttendance);

        // RecyclerView
        recyclerStudents = findViewById(R.id.recyclerStudents);
        studentList = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            studentList.add(new Student(i, "Student " + i, true));
        }

        adapter = new StudentAdapter(studentList);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);

        toggleAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setAll(isChecked);
        });

        // Buttons
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        btnEdit = findViewById(R.id.btnEdit);

        btnClear.setOnClickListener(v -> adapter.setAll(false));
        btnSave.setOnClickListener(v -> adapter.setEditable(false));
        btnEdit.setOnClickListener(v -> {
            // future implementation
        });
    }

    // TIME PICKER
    private void showTimePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute1) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    editText.setText(time);
                },
                hour,
                minute,
                false
        );
        dialog.show();
    }

    // DATE PICKER
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );
                    etDate.setText(date);
                },
                year, month, day
        );

        dialog.show();
    }
}
