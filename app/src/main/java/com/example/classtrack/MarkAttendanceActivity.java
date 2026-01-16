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

        studentList.add(new Student(1, "BOLE MAHESH GULABRAO", true));
        studentList.add(new Student(2, "PATIL TRISHA VIJAY", true));
        studentList.add(new Student(3, "FALAKE ISHWARI VILAS", true));
        studentList.add(new Student(4, "LAHASE NANDINI GAUTAM", true));
        studentList.add(new Student(5, "WASE GAURI JANRAO", true));
        studentList.add(new Student(6, "LOHAR KUNAL VINOD", true));
        studentList.add(new Student(7, "TATHOD SHRAVANI PRAMOD", true));
        studentList.add(new Student(8, "CHAVHAN RADHIKA SANTOSH", true));
        studentList.add(new Student(9, "MATRE SUPRIYA DIPAK", true));
        studentList.add(new Student(10, "LAHUDKAR SHRUTI BHAGWAN", true));
        studentList.add(new Student(11, "KALE ROSHAN RAJENDRA", true));
        studentList.add(new Student(12, "WANKHADE NAMRATA BHASKAR", true));
        studentList.add(new Student(13, "KSHIRSAGAR SHRADDHA RAMESHWAR", true));
        studentList.add(new Student(14, "BOCHARE GAURAV BHAGWAT", true));
        studentList.add(new Student(15, "DESHMUKH KHUSHBU RANJIT", true));
        studentList.add(new Student(16, "WANARE AMRUTA SANTOSH", true));
        studentList.add(new Student(17, "FALKE SHIVAM GAJANAN", true));
        studentList.add(new Student(18, "VAKTE SHRUTI KIRAN", true));
        studentList.add(new Student(19, "DASHRATHE HARSHADA RAJENDRA", true));
        studentList.add(new Student(20, "SHEDGE OM YOGESH", true));
        studentList.add(new Student(21, "PATIL SNEHAL SUNIL", true));
        studentList.add(new Student(22, "SHINDE PRANALI SANJAY", true));
        studentList.add(new Student(23, "BHOSALE MAYURI DATTATRAY", true));
        studentList.add(new Student(24, "JADHAV AKSHAY ANIL", true));
        studentList.add(new Student(25, "PAWAR ROHIT SHANKAR", true));
        studentList.add(new Student(26, "THAKARE PRATIKSHA RAMESH", true));
        studentList.add(new Student(27, "JAGTAP NIKITA RAJESH", true));
        studentList.add(new Student(28, "CHAVAN SHUBHAM VIJAY", true));
        studentList.add(new Student(29, "NIKAM ANKITA SURESH", true));
        studentList.add(new Student(30, "KOLHE PRASHANT ASHOK", true));
        studentList.add(new Student(31, "BHAGAT POOJA RAMESH", true));
        studentList.add(new Student(32, "GAVALI SACHIN SHIVAJI", true));
        studentList.add(new Student(33, "SHELKE TEJAS SANJAY", true));
        studentList.add(new Student(34, "MORE PRIYANKA GANESH", true));
        studentList.add(new Student(35, "BANSODE ROHINI MAHADEV", true));
        studentList.add(new Student(36, "KARANDE AMOL BALASAHEB", true));
        studentList.add(new Student(37, "NAGARE RUTUJA KISHOR", true));
        studentList.add(new Student(38, "KEDAR ANIKET RAVINDRA", true));
        studentList.add(new Student(39, "SALUNKE SNEHA SURESH", true));
        studentList.add(new Student(40, "KUMBHAR VISHAL VITTHAL", true));
        studentList.add(new Student(41, "CHINCHOLKAR SNEHAL MANOJ", true));
        studentList.add(new Student(42, "JADHAV KOMAL MAHESH", true));
        studentList.add(new Student(43, "PAWAR AKASH DNYANESHWAR", true));
        studentList.add(new Student(44, "DHORE MAYUR SANJAY", true));
        studentList.add(new Student(45, "GORE PRITI SHIVAJI", true));
        studentList.add(new Student(46, "MALI RAHUL TUKARAM", true));
        studentList.add(new Student(47, "TUPKARI SAYALI SHASHIKANT", true));
        studentList.add(new Student(48, "PANDIT ANKUSH BHIMRAO", true));
        studentList.add(new Student(49, "KAMBLE SHUBHAM RAMESH", true));
        studentList.add(new Student(50, "DESAI KAJAL VIKAS", true));
        studentList.add(new Student(51, "CHAVAN NITIN SANJAY", true));
        studentList.add(new Student(52, "KULKARNI RUTUJA MILIND", true));
        studentList.add(new Student(53, "PATIL AKSHATA RAJENDRA", true));
        studentList.add(new Student(54, "GHULE OMKAR MADHUKAR", true));
        studentList.add(new Student(55, "SURYAWANSHI SWAPNIL SHANKAR", true));
        studentList.add(new Student(56, "JAGDALE PAYAL BALAJI", true));
        studentList.add(new Student(57, "BAVISKAR ANIKET SHIVAJI", true));
        studentList.add(new Student(58, "SHETE DIPALI DATTATRAY", true));
        studentList.add(new Student(59, "PAWAR SACHIN VASANT", true));
        studentList.add(new Student(60, "LOKHANDE POOJA PRAVIN", true));
        studentList.add(new Student(61, "BHALEKAR ROHIT ANAND", true));
        studentList.add(new Student(62, "GIRI SHUBHAM RAVINDRA", true));
        studentList.add(new Student(63, "KATE PRIYA RAMESH", true));
        studentList.add(new Student(64, "SABLE SURAJ KAILAS", true));
        studentList.add(new Student(65, "TIDKE RANI SUNIL", true));
        studentList.add(new Student(66, "NIMBALKAR AMIT VILAS", true));
        studentList.add(new Student(67, "WAGHMARE PAYAL BHASKAR", true));
        studentList.add(new Student(68, "KHAIRE ABHIJEET SANTOSH", true));
        studentList.add(new Student(69, "PATIL SIDDHI VIJAY", true));


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
            adapter.setEditable(true);
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
