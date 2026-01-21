package com.example.classtrack;

public class StudentReport {
    private int roll;
    private String name;
    private int presentCount;

    public StudentReport(int roll, String name, int presentCount) {
        this.roll = roll;
        this.name = name;
        this.presentCount = presentCount;
    }

    public int getRoll() { return roll; }
    public String getName() { return name; }
    public int getPresentCount() { return presentCount; }

    // Helper to increase count when we find a "P" in the database
    public void incrementPresent() {
        this.presentCount++;
    }
}