package com.example.classtrack;

public class Student {

    private int roll;
    private String name;
    private boolean present;

    // 🔹 Constructor
    public Student(int roll, String name, boolean present) {
        this.roll = roll;
        this.name = name;
        this.present = present;
    }

    // 🔹 Getter & Setter for Roll
    public int getRoll() {
        return roll;
    }

    public void setRoll(int roll) {
        this.roll = roll;
    }

    // 🔹 Getter & Setter for Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // 🔹 Getter & Setter for Present
    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }
}
