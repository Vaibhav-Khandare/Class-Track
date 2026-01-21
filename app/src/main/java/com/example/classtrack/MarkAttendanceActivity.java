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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

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
    Button btnClear, btnSave; // Removed btnEdit
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
        // Removed btnEdit findView

        studentList = new ArrayList<>();

        // LOGIC TO SWITCH BETWEEN 1ST, 2ND, AND 3RD YEAR
        currentYearBatch = getIntent().getStringExtra("YEAR");
        if (currentYearBatch == null) currentYearBatch = "1st Year"; // Default fallback

        tvTitle.setText(currentYearBatch + " - Mark Attendance");

        int subjectArrayResId;

        if (currentYearBatch.equals("2nd Year")) {
            subjectArrayResId = R.array.subjects_2nd_year;
            load2ndYearStudents();
        } else if (currentYearBatch.equals("3rd Year")) {
            subjectArrayResId = R.array.subjects_3rd_year;
            load3rdYearStudents();
        } else {
            subjectArrayResId = R.array.subjects_1st_year;
            load1stYearStudents();
        }

        // Set the spinner adapter dynamically
        ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(this,
                subjectArrayResId, android.R.layout.simple_spinner_item);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);

        // Recycler Setup
        adapter = new StudentAdapter(studentList);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);

        // Event Listeners
        etFromTime.setOnClickListener(v -> showTimePicker(etFromTime));
        etToTime.setOnClickListener(v -> showTimePicker(etToTime));

        // Fixed: removed parameter from call to match definition below
        etDate.setOnClickListener(v -> showDatePicker());

        toggleAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.setAll(isChecked));
        btnClear.setOnClickListener(v -> adapter.setAll(false));
        // Removed btnEdit listener

        // 🔥 SAVE BUTTON LOGIC
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveAttendanceToFirestore();
            }
        });
    }

    // ---------------------------------------------------------
    //  🔥 FIRESTORE SAVING LOGIC
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
        sessionMap.put("dateStr", etDate.getText().toString()); // For display (e.g., 21/01/2026)
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

                    // 🔥 Close the activity to return to Dashboard
                    finish();
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
            return new Date(); // Fallback to current time
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

    // 🔹 FIXED: Removed parameter to fix the error you saw
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

    // ---------------------------------------------------------
    //  FULL STUDENT DATA
    // ---------------------------------------------------------

    private void load1stYearStudents() {
        studentList.clear();
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
    }

    private void load2ndYearStudents() {
        studentList.clear();
        studentList.add(new Student(1, "INGLE SWAPNIL SIDDHARTH", true));
        studentList.add(new Student(2, "LAHUDKAR VAISHNAVI SUNIL", true));
        studentList.add(new Student(3, "MENGADE PAVAN PARMESHWAR", true));
        studentList.add(new Student(4, "TALE ANJALI GOPAL", true));
        studentList.add(new Student(5, "KANOJE PRATYUSHA RAVINDRA", true));
        studentList.add(new Student(6, "WARADE VEDIKA ANANT", true));
        studentList.add(new Student(7, "GAWAI SHOURYA BHARAT", true));
        studentList.add(new Student(8, "KINGE ANUSHKA SHRIKRUSHNA", true));
        studentList.add(new Student(9, "WAGHMARE ANKITA SANJAY", true));
        studentList.add(new Student(10, "GIRHE PUNAM SUKHNANDAN", true));
        studentList.add(new Student(11, "GAVANDE BHAVNA PURUSHOTTAM", true));
        studentList.add(new Student(12, "CHATE VAISHNAVI DATTATRAY", true));
        studentList.add(new Student(13, "PATIL BHAKTI BHAGWAN", true));
        studentList.add(new Student(14, "DOIFODE RUTUJA BADRINARAYAN", true));
        studentList.add(new Student(15, "GAIKWAD TANVI PRABHAKAR", true));
        studentList.add(new Student(16, "PADMANE SHUBHAM SHIVDAS", true));
        studentList.add(new Student(17, "SONUNE TEJAL PRALHAD", true));
        studentList.add(new Student(18, "KOTHALKAR RUSHIKESH RAHUL", true));
        studentList.add(new Student(19, "RAJGURU SHWETA DIGAMBAR", true));
        studentList.add(new Student(20, "LAKDE RAHUL RAMESH", true));
        studentList.add(new Student(21, "BELOKAR SHRIRAJ DINKAR", true));
        studentList.add(new Student(22, "GOUR RIYA SANTOSHSINGH", true));
        studentList.add(new Student(23, "JADHAV SACHIN ARJUN", true));
        studentList.add(new Student(24, "WADHE SHWETA SANTOSH", true));
        studentList.add(new Student(25, "UDAR TANISHQ ATUL", true));
        studentList.add(new Student(26, "PANDE SHRADDHA MANGESH", true));
        studentList.add(new Student(27, "NIMBOLE RASIKA DNYANDEO", true));
        studentList.add(new Student(28, "SHINGADE APURVA DINESH", true));
        studentList.add(new Student(29, "BILEWAR BHARTI VILAS", true));
        studentList.add(new Student(30, "ZOPE NISHTHA UDDHAV", true));
        studentList.add(new Student(31, "KHARCHE JIDNYASA ABHAY", true));
        studentList.add(new Student(32, "GAVATE OM ANIL", true));
        studentList.add(new Student(33, "DEVKAR SNEHAL MOHAN", true));
        studentList.add(new Student(34, "SHIRNATH KHUSHI SANJAY", true));
        studentList.add(new Student(35, "KATARE SHREYASH SUBHASH", true));
        studentList.add(new Student(36, "PATIL TEJAL DARBARSING", true));
        studentList.add(new Student(37, "SOLUNKE GOVIND GANGADHAR", true));
        studentList.add(new Student(38, "HIWRALE PRIYAL DEVANAND", true));
        studentList.add(new Student(39, "DAHORE GAYATRI VASANT", true));
        studentList.add(new Student(40, "DAHORE GAURI VASANT", true));
        studentList.add(new Student(41, "GAWANDE GAURAV GAJANAN", true));
        studentList.add(new Student(42, "SHELAKE BHAVANA KISHOR", true));
        studentList.add(new Student(43, "KELODE SWARAJ KAILAS", true));
        studentList.add(new Student(44, "BARELA NITU LALU", true));
        studentList.add(new Student(45, "SHELKE KHUSHAL VIJAY", true));
        studentList.add(new Student(46, "PATIL SAKSHI KISHOR", true));
        studentList.add(new Student(47, "KALE DURVESH NARENDRA", true));
        studentList.add(new Student(48, "GALWADE JANHAVI SHASHIKANT", true));
        studentList.add(new Student(49, "MOHAMMAD FAIZAL RAZA MOHAMMAD AKBAR", true));
        studentList.add(new Student(50, "MURHEKAR PRANJALI MAHADEO", true));
        studentList.add(new Student(51, "MAHALE GAURI CHAKRADHAR", true));
        studentList.add(new Student(52, "KHANDARE SRUSHTI RAHUL", true));
        studentList.add(new Student(53, "DAWALE DIVYA PRAMOD", true));
        studentList.add(new Student(54, "KHANDARE GAYATRI NARAYAN", true));
        studentList.add(new Student(55, "JADHAV SARITA NANDUSING", true));
        studentList.add(new Student(56, "TAYDE UJWAL RAJU", true));
        studentList.add(new Student(57, "JOSHI VIRAJ ASHOK", true));
        studentList.add(new Student(58, "JADHAV HARIOM MURLIDHAR", true));
        studentList.add(new Student(59, "KULKARNI SIDDHANT VIVEK", true));
        studentList.add(new Student(60, "INGLE LUCKY TEJRAO", true));
        studentList.add(new Student(61, "BHOPALE SHRUTI VILAS", true));
        studentList.add(new Student(62, "UGALE RENUKA SANJAY", true));
        studentList.add(new Student(63, "PATIL VARAD CHANDRAKANT", true));
        studentList.add(new Student(64, "DHATRAK AASTHA CHANDRASHEKHAR", true));
        studentList.add(new Student(65, "BHOSALE RAJESHWARI DNYANESHWAR", true));
        studentList.add(new Student(66, "GALDHAR VISHWANATH KASHINATH", true));
        studentList.add(new Student(67, "BAWASKAR SAKSHI YOGESH", true));
        studentList.add(new Student(68, "FUNDKAR YASH SACHIN", true));
        studentList.add(new Student(69, "DEVALE SHREYASH ANIL", true));
        studentList.add(new Student(70, "TAYADE SUSHRIYA PRAKASH", true));
        studentList.add(new Student(71, "BHOSALE HARSH GANESH", true));
        studentList.add(new Student(72, "SHUKLA VAIBHAV VINOD", true));
        studentList.add(new Student(73, "KELLEWAD SAMIKSHA BALAJI", true));
        studentList.add(new Student(74, "TIKAR PURVA RAJENDRA", true));
        studentList.add(new Student(75, "WAGH SHUBHAM SHANKAR", true));
    }

    private void load3rdYearStudents() {
        studentList.clear();
        studentList.add(new Student(1, "MOHAMMAD WASIF SHAIKH MUKHTAR", true));
        studentList.add(new Student(2, "BAHEKAR BHAKTI RAMESH", true));
        studentList.add(new Student(3, "BHAGAT ATHARV VINOD", true));
        studentList.add(new Student(4, "BHAGAT KARTIKI RAMESH", true));
        studentList.add(new Student(5, "BHALERAO YASH RAJESH", true));
        studentList.add(new Student(6, "BODKHE TANUJA ANIL", true));
        studentList.add(new Student(7, "BOHARPI MAYURI PRAMOD", true));
        studentList.add(new Student(8, "CHANDANE OM SURESH", true));
        studentList.add(new Student(9, "CHAUDHARI SARTHAK GAJANAN", true));
        studentList.add(new Student(10, "CHIPADE PRAJWAL GAJANAN", true));
        studentList.add(new Student(11, "DABERAO NAGESH RAMESHWAR", true));
        studentList.add(new Student(12, "DABERAO SANKET VIJAY", true));
        studentList.add(new Student(13, "DALVI ACHAL GANESH", true));
        studentList.add(new Student(14, "DESHMUKH SAKSHI BHASKAR", true));
        studentList.add(new Student(15, "DHANDE LINA MOHAN", true));
        studentList.add(new Student(16, "DHISLE GAURI ASHOK", true));
        studentList.add(new Student(17, "DIPKE CHAITANYA MILIND", true));
        studentList.add(new Student(18, "DIWASE KRUSHNA SATISH", true));
        studentList.add(new Student(19, "DIWNALE SWARADA RAJENDRA", true));
        studentList.add(new Student(20, "FUNDE PRACHI GANPAT", true));
        studentList.add(new Student(21, "GAWAI NAMAMI SATISH", true));
        studentList.add(new Student(22, "GAWANDE KHUSHI RAJENDRA", true));
        studentList.add(new Student(23, "GORE VINAYAK NANDKISHOR", true));
        studentList.add(new Student(24, "GOTMARE AADITI BHAGWAT", true));
        studentList.add(new Student(25, "INGLE GAURI JITENDRA", true));
        studentList.add(new Student(26, "INGLE NILESH SANTOSH", true));
        studentList.add(new Student(27, "INGLE SUMEDH MAHENDRA", true));
        studentList.add(new Student(28, "JAMODE PRAJWAL SANDEEP", true));
        studentList.add(new Student(29, "KADAM PRANJAL ASHOK", true));
        studentList.add(new Student(30, "KALDATE ARTI GOPAL", true));
        studentList.add(new Student(31, "KALE PRAGATI VILAS", true));
        studentList.add(new Student(32, "KATKAR SHRUTI SHIVAJI", true));
        studentList.add(new Student(33, "KHANDARE VAIBHAV PRAMOD", true));
        studentList.add(new Student(34, "LOD ARATI DILIP", true));
        studentList.add(new Student(35, "LOKHANDE PRERANA DNYANESHVAR", true));
        studentList.add(new Student(36, "LOTHE OM SHRIKRUSHNA", true));
        studentList.add(new Student(37, "MAHAJAN KALPESH SANJAY", true));
        studentList.add(new Student(38, "MAHOKAR PRERNA PRAMOD", true));
        studentList.add(new Student(39, "NARKHEDE SHUBHANGI CHANDRAKANT", true));
        studentList.add(new Student(40, "NEMANE KHUSHI MADHUSUDAN", true));
        studentList.add(new Student(41, "PATKAR RUTUJA PRAMOD", true));
        studentList.add(new Student(42, "PAWAR INDRANIL RAJENDRA", true));
        studentList.add(new Student(43, "RABDE TRUPTI SWAROOPSING", true));
        studentList.add(new Student(44, "RAHATE PRATHAMESH NATTHU", true));
        studentList.add(new Student(45, "RATHI PAYAL KAILAS", true));
        studentList.add(new Student(46, "RAUT SNEHAL ANIL", true));
        studentList.add(new Student(47, "SAWALE SAMIKSHA SOPAN", true));
        studentList.add(new Student(48, "SHEGOKAR LAXMAN SANTOSH", true));
        studentList.add(new Student(49, "SHIRALE KOMAL SHRAVAN", true));
        studentList.add(new Student(50, "SHUKLA SHAAN SANTOSHKUMAR", true));
        studentList.add(new Student(51, "SURALKAR VRUSHALI NINA", true));
        studentList.add(new Student(52, "SURYAWANSHI NILESH JAYRAM", true));
        studentList.add(new Student(53, "TAYADE YASH SUNIL", true));
        studentList.add(new Student(54, "THOSARE ROHAN DILIP", true));
        studentList.add(new Student(55, "VANSUTRE SAKSHI SUNIL", true));
        studentList.add(new Student(56, "WAKADKAR HARSHAL SHYAM", true));
        studentList.add(new Student(57, "WAKODE ASMITA ANANT", true));
        studentList.add(new Student(58, "DHARPAWAR ROHAN SANJAY", true));
        studentList.add(new Student(59, "CHINCHOLKAR VRUSHALI MOHAN", true));
        studentList.add(new Student(60, "KAKADE GOPAL RAJENDRA", true));
        studentList.add(new Student(61, "THAKARE AYUSH BHASKAR", true));
        studentList.add(new Student(62, "ALI SHIFA MOHD ATHAR", true));
        studentList.add(new Student(63, "WANKHADE PRATIKSHA SHYAM", true));
    }
}