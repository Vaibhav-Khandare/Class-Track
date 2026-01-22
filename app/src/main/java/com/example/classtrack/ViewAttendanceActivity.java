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
import java.util.Calendar; // 🔥 THIS IMPORT WAS MISSING
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

    // Store all unique dates for columns
    private List<String> allSessionDates = new ArrayList<>();

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
                if (checkPermission()) exportToPdfLandscape();
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
        // We pass the specific EditText (From or To) so the method knows which one to update
        etDateFrom.setOnClickListener(v -> showDatePicker(etDateFrom));
        etDateTo.setOnClickListener(v -> showDatePicker(etDateTo));
    }

    private void showDatePicker(EditText targetField) {
        // 🔥 This requires java.util.Calendar import
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

        // Add 1 day to end date to ensure the query includes the full last day
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

                    allSessionDates.clear();
                    Map<String, StudentReport> aggregationMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Collect Date
                        String sessionDate = doc.getString("dateStr");
                        if (!allSessionDates.contains(sessionDate)) {
                            allSessionDates.add(sessionDate);
                        }

                        List<Map<String, Object>> studentsInSession = (List<Map<String, Object>>) doc.get("attendanceData");

                        if (studentsInSession != null) {
                            for (Map<String, Object> studentMap : studentsInSession) {
                                // Safe casting for Roll number
                                Long rollLong = (Long) studentMap.get("roll");
                                int roll = (rollLong != null) ? rollLong.intValue() : 0;

                                String name = (String) studentMap.get("name");
                                String status = (String) studentMap.get("status");

                                String key = String.valueOf(roll);

                                if (!aggregationMap.containsKey(key)) {
                                    aggregationMap.put(key, new StudentReport(roll, name, 0));
                                }

                                StudentReport report = aggregationMap.get(key);
                                if (report != null) {
                                    report.setStatusForDate(sessionDate, status);
                                    if ("P".equals(status)) {
                                        report.incrementPresent();
                                    }
                                }
                            }
                        }
                    }

                    // Sort dates properly
                    sortDates(allSessionDates);

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
    // 📄 LANDSCAPE PDF EXPORT
    // ----------------------------------------------------

    private void exportToPdfLandscape() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint tableLinePaint = new Paint();
        tableLinePaint.setColor(Color.LTGRAY);
        tableLinePaint.setStrokeWidth(1f);

        int pageWidth = 1684; // A4 Landscape roughly doubled
        int pageHeight = 1190;
        int pageNumber = 1;

        int MAX_DATES_PER_PAGE = 10;
        int totalDates = Math.max(1, allSessionDates.size());

        // Loop through chunks of dates (Horizontal Pagination)
        for (int i = 0; i < totalDates; i += MAX_DATES_PER_PAGE) {

            int endIndex = Math.min(i + MAX_DATES_PER_PAGE, allSessionDates.size());
            List<String> currentBatchDates = new ArrayList<>();
            if(!allSessionDates.isEmpty()) {
                currentBatchDates = allSessionDates.subList(i, endIndex);
            }
            boolean isLastBatch = (endIndex >= allSessionDates.size());

            // Start first page for this batch
            PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);
            Canvas canvas = myPage.getCanvas();

            drawLandscapeHeader(canvas, titlePaint, paint, pageNumber, currentBatchDates, isLastBatch);

            paint.setTextSize(20);
            paint.setColor(Color.BLACK);

            int y = 280;

            // Loop through students (Vertical Pagination)
            for (StudentReport s : reportList) {
                // If page is full, start new page BUT keep same date batch
                if (y > pageHeight - 100) {
                    pdfDocument.finishPage(myPage);
                    pageNumber++;
                    myPageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    myPage = pdfDocument.startPage(myPageInfo);
                    canvas = myPage.getCanvas();
                    drawLandscapeHeader(canvas, titlePaint, paint, pageNumber, currentBatchDates, isLastBatch);
                    y = 280;
                }

                // Draw Fixed Columns
                canvas.drawText(String.valueOf(s.getRoll()), 50, y, paint);

                String name = s.getName();
                if(name.length() > 25) name = name.substring(0, 22) + "...";
                canvas.drawText(name, 120, y, paint);

                // Draw Date Columns
                int xDate = 450;
                for (String date : currentBatchDates) {
                    String status = s.getStatusForDate(date);

                    if(status.equals("P")) paint.setColor(Color.parseColor("#2E7D32")); // Green
                    else if(status.equals("A")) paint.setColor(Color.RED);
                    else paint.setColor(Color.BLACK);

                    canvas.drawText(status, xDate + 10, y, paint);
                    paint.setColor(Color.BLACK);
                    xDate += 90;
                }

                // Draw Stats ONLY on the last batch of dates
                if (isLastBatch) {
                    int xStats = 450 + (currentBatchDates.size() * 90) + 20;
                    if(currentBatchDates.isEmpty()) xStats = 450;

                    canvas.drawText(s.getPresentCount() + "", xStats, y, paint);
                    canvas.drawText(totalLecturesCount + "", xStats + 100, y, paint);

                    int percent = (totalLecturesCount > 0) ? (s.getPresentCount() * 100) / totalLecturesCount : 0;
                    canvas.drawText(percent + "%", xStats + 200, y, paint);
                }

                canvas.drawLine(40, y + 15, pageWidth - 40, y + 15, tableLinePaint);
                y += 45;
            }

            pdfDocument.finishPage(myPage);
            pageNumber++;
        }

        savePdfFile(pdfDocument);
    }

    private void drawLandscapeHeader(Canvas canvas, Paint titlePaint, Paint paint, int pageNum, List<String> dates, boolean isLastBatch) {
        titlePaint.setTextSize(36);
        titlePaint.setColor(Color.BLUE);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Attendance Report - " + selectedYear, 50, 60, titlePaint);

        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        canvas.drawText("Subject: " + spinnerSubject.getSelectedItem().toString(), 50, 110, paint);
        canvas.drawText("Total Lectures: " + totalLecturesCount, 50, 150, paint);
        canvas.drawText("Page: " + pageNum, 1500, 60, paint);

        // Header Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#E0E0E0"));
        canvas.drawRect(40, 190, 1640, 240, bgPaint);

        // Header Text
        paint.setFakeBoldText(true);
        int yHeader = 220;
        canvas.drawText("Roll", 50, yHeader, paint);
        canvas.drawText("Name", 120, yHeader, paint);

        // Date Headers
        int xDate = 450;
        for (String date : dates) {
            String shortDate = date.length() >= 5 ? date.substring(0, 5) : date; // 21/01
            canvas.drawText(shortDate, xDate, yHeader, paint);
            xDate += 90;
        }

        // Stats Headers (Only on last batch)
        if (isLastBatch) {
            int xStats = 450 + (dates.size() * 90) + 20;
            if(dates.isEmpty()) xStats = 450;

            canvas.drawText("Attd", xStats, yHeader, paint);
            canvas.drawText("Total", xStats + 100, yHeader, paint);
            canvas.drawText("%", xStats + 200, yHeader, paint);
        }
        paint.setFakeBoldText(false);
    }

    private void savePdfFile(PdfDocument pdfDocument) {
        String subjectName = spinnerSubject.getSelectedItem().toString().replaceAll("\\s+", "_");
        String fileName = subjectName + "_Attendance_Report.pdf";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Saved: Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }

    // ----------------------------------------------------
    // 📊 EXCEL EXPORT
    // ----------------------------------------------------

    private void exportToExcel() {
        StringBuilder data = new StringBuilder();

        // 1. CSV Header
        data.append("Roll,Name");
        for (String date : allSessionDates) {
            data.append(",").append(date);
        }
        data.append(",Attended,Total Lectures,Percentage\n");

        // 2. Data Rows
        for (StudentReport s : reportList) {
            data.append(s.getRoll()).append(",");
            data.append("\"").append(s.getName()).append("\",");

            for (String date : allSessionDates) {
                data.append(s.getStatusForDate(date)).append(",");
            }

            int percent = (totalLecturesCount > 0) ? (s.getPresentCount() * 100) / totalLecturesCount : 0;
            data.append(s.getPresentCount()).append(",")
                    .append(totalLecturesCount).append(",")
                    .append(percent).append("%").append("\n");
        }

        try {
            String subjectName = spinnerSubject.getSelectedItem().toString().replaceAll("\\s+", "_");
            String fileName = subjectName + "_Attendance_Report.csv";

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            FileWriter writer = new FileWriter(file);
            writer.append(data.toString());
            writer.flush();
            writer.close();

            Toast.makeText(this, "Excel Saved: Downloads/" + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error saving Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sortDates(List<String> dates) {
        Collections.sort(dates, (d1, d2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.parse(d1).compareTo(sdf.parse(d2));
            } catch (ParseException e) {
                return 0;
            }
        });
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