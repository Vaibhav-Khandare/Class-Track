package com.example.classtrack;

import java.util.HashMap;
import java.util.Map;

public class StudentReport {
    private int roll;
    private String name;
    private int presentCount;
    // New: Map to store "21/01/2026" -> "P"
    private Map<String, String> dateStatusMap = new HashMap<>();

    public StudentReport(int roll, String name, int presentCount) {
        this.roll = roll;
        this.name = name;
        this.presentCount = presentCount;
    }

    public int getRoll() { return roll; }
    public String getName() { return name; }
    public int getPresentCount() { return presentCount; }

    public void incrementPresent() {
        this.presentCount++;
    }

    // New methods for date-wise tracking
    public void setStatusForDate(String date, String status) {
        dateStatusMap.put(date, status);
    }

    public String getStatusForDate(String date) {
        // Returns "P", "A", or "-" if not found
        return dateStatusMap.getOrDefault(date, "-");
    }
}