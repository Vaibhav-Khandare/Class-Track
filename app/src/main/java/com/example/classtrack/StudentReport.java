package com.example.classtrack;

import java.util.HashMap;
import java.util.Map;

public class StudentReport {

    private String enrollmentNo;
    private int roll;
    private String name;
    private int presentCount;

    // date -> P / A
    private Map<String, String> dateStatusMap = new HashMap<>();

    public StudentReport(String enrollmentNo, int roll, String name, int presentCount) {
        this.enrollmentNo = enrollmentNo;
        this.roll = roll;
        this.name = name;
        this.presentCount = presentCount;
    }

    public String getEnrollmentNo() {
        return enrollmentNo;
    }

    public int getRoll() {
        return roll;
    }

    public String getName() {
        return name;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void incrementPresent() {
        presentCount++;
    }

    public void setStatusForDate(String date, String status) {
        dateStatusMap.put(date, status);
    }

    public String getStatusForDate(String date) {
        return dateStatusMap.getOrDefault(date, "-");
    }

    // 🔴 IMPORTANT: duplicate increment stop
    public boolean hasDate(String date) {
        return dateStatusMap.containsKey(date);
    }
}
