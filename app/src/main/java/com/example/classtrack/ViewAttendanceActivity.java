package com.example.classtrack;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
    private int totalLecturesCount = 0;

    private FirebaseFirestore db;
    private ReportAdapter adapter;
    private List<StudentReport> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        db = FirebaseFirestore.getInstance();

        selectedYear = getIntent().getStringExtra("YEAR");
        if (selectedYear == null) selectedYear = "1st Year";

        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvTotalLectures = findViewById(R.id.tvTotalLectures);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        spinnerSubject = findViewById(R.id.spinnerSubjectReport);
        btnShow = findViewById(R.id.btnShowReport);
        rvReportList = findViewById(R.id.rvReportList);
        btnPrintPdf = findViewById(R.id.btnPrintPdf);
        btnPrintExcel = findViewById(R.id.btnPrintExcel);

        tvScreenTitle.setText(selectedYear + " - Report");
        setupDatePickers();
        setupSpinner();

        rvReportList.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();

        btnShow.setOnClickListener(v -> loadReportData());

        btnPrintPdf.setOnClickListener(v -> {
            if (reportList.isEmpty()) {
                Toast.makeText(this, "No data to export!", Toast.LENGTH_SHORT).show();
            } else {
                if (checkPermission()) exportToPdf();
            }
        });

        btnPrintExcel.setOnClickListener(v -> {
            if (reportList.isEmpty()) {
                Toast.makeText(this, "No data to export!", Toast.LENGTH_SHORT).show();
            } else {
                if (checkPermission()) exportToExcel();
            }
        });
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

        Calendar c = Calendar.getInstance();
        c.setTime(endDate);
        c.add(Calendar.DATE, 1);
        Date endDateInclusive = c.getTime();

        db.collection("attendance_sessions")
                .whereEqualTo("batch", selectedYear)
                .whereEqualTo("subject", subject)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThan("timestamp", endDateInclusive)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    totalLecturesCount = querySnapshot.size();
                    tvTotalLectures.setText("Total Lectures: " + totalLecturesCount);

                    Map<String, StudentReport> aggregationMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        List<Map<String, Object>> studentsInSession = (List<Map<String, Object>>) doc.get("attendanceData");

                        if (studentsInSession != null) {
                            for (Map<String, Object> studentMap : studentsInSession) {
                                int roll = ((Long) studentMap.get("roll")).intValue();
                                String name = (String) studentMap.get("name");
                                String status = (String) studentMap.get("status");

                                String key = String.valueOf(roll);

                                if (!aggregationMap.containsKey(key)) {
                                    aggregationMap.put(key, new StudentReport(roll, name, 0));
                                }
                                if ("P".equals(status)) {
                                    aggregationMap.get(key).incrementPresent();
                                }
                            }
                        }
                    }

                    reportList = new ArrayList<>(aggregationMap.values());
                    Collections.sort(reportList, (o1, o2) -> Integer.compare(o1.getRoll(), o2.getRoll()));

                    adapter = new ReportAdapter(reportList, totalLecturesCount);
                    rvReportList.setAdapter(adapter);

                    btnShow.setEnabled(true);
                    btnShow.setText("SHOW REPORT");
                    Toast.makeText(this, "Report Loaded!", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    btnShow.setEnabled(true);
                    btnShow.setText("SHOW REPORT");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ----------------------------------------------------
    // 📄 UPDATED PDF EXPORT (Fixes 49 student limit)
    // ----------------------------------------------------

    private void exportToPdf() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // Page Configuration
        int pageWidth = 1120;
        int pageHeight = 2000;
        int pageNumber = 1;

        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();

        // --- DRAW HEADER (Function to keep it consistent) ---
        drawPdfHeader(canvas, titlePaint, paint, pageNumber);

        paint.setTextSize(20);
        paint.setColor(Color.BLACK);

        // Start Y position for student rows
        int y = 250;

        for (StudentReport s : reportList) {
            // 🛑 CHECK IF PAGE IS FULL
            if (y > 1850) {
                // 1. Finish current page
                pdfDocument.finishPage(myPage);

                // 2. Increment page number & create new page
                pageNumber++;
                myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                myPage = pdfDocument.startPage(myPageInfo);
                canvas = myPage.getCanvas();

                // 3. Draw Header on new page
                drawPdfHeader(canvas, titlePaint, paint, pageNumber);

                // 4. Reset Y position
                y = 250;
            }

            // Draw Student Row
            canvas.drawText(String.valueOf(s.getRoll()), 50, y, paint);
            canvas.drawText(s.getName(), 150, y, paint);
            canvas.drawText(s.getPresentCount() + "/" + totalLecturesCount, 700, y, paint);

            int percent = (totalLecturesCount > 0) ? (s.getPresentCount() * 100) / totalLecturesCount : 0;
            canvas.drawText(percent + "%", 900, y, paint);

            y += 40; // Move down for next student
        }

        pdfDocument.finishPage(myPage);

        // ✅ NEW FILENAME LOGIC
        String subjectName = spinnerSubject.getSelectedItem().toString();
        // Removes spaces for clean filename
        String safeSubjectName = subjectName.replaceAll("\\s+", "_");
        String fileName = safeSubjectName + "_Attendance_Report.pdf";

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }

    // Helper to draw the top part of the PDF page
    private void drawPdfHeader(Canvas canvas, Paint titlePaint, Paint paint, int pageNum) {
        titlePaint.setTextSize(30);
        titlePaint.setColor(Color.BLUE);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Attendance Report - " + selectedYear, 50, 60, titlePaint);

        paint.setTextSize(20);
        paint.setColor(Color.BLACK);
        canvas.drawText("Subject: " + spinnerSubject.getSelectedItem().toString(), 50, 100, paint);
        canvas.drawText("Total Lectures: " + totalLecturesCount, 50, 130, paint);
        canvas.drawText("Page: " + pageNum, 900, 60, paint);

        // Header Row
        paint.setFakeBoldText(true);
        int yHeader = 180;
        canvas.drawText("Roll", 50, yHeader, paint);
        canvas.drawText("Name", 150, yHeader, paint);
        canvas.drawText("Attended", 700, yHeader, paint);
        canvas.drawText("%", 900, yHeader, paint);
        canvas.drawLine(50, yHeader + 10, 1050, yHeader + 10, paint);

        paint.setFakeBoldText(false);
    }

    // ----------------------------------------------------
    // 📊 EXCEL EXPORT (CSV format renamed to .xls for convenience)
    // ----------------------------------------------------

    private void exportToExcel() {
        StringBuilder data = new StringBuilder();
        // CSV Header
        data.append("Roll No,Name,Present Count,Total Lectures,Percentage\n");

        for (StudentReport s : reportList) {
            int percent = (totalLecturesCount > 0) ? (s.getPresentCount() * 100) / totalLecturesCount : 0;
            data.append(s.getRoll()).append(",")
                    .append(s.getName()).append(",")
                    .append(s.getPresentCount()).append(",")
                    .append(totalLecturesCount).append(",")
                    .append(percent).append("%").append("\n");
        }

        try {
            // ✅ NEW FILENAME LOGIC
            String subjectName = spinnerSubject.getSelectedItem().toString();
            String safeSubjectName = subjectName.replaceAll("\\s+", "_");
            String fileName = safeSubjectName + "_Attendance_Report.csv";

            // NOTE: We use .csv because true .xlsx requires heavy external libraries.
            // .csv opens perfectly in Excel, Google Sheets, and WPS Office.

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            FileWriter writer = new FileWriter(file);
            writer.append(data.toString());
            writer.flush();
            writer.close();

            Toast.makeText(this, "Excel Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error saving Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            return false;
        }
        return true;
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