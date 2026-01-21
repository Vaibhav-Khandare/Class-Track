package com.example.classtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private ArrayList<Student> students;
    private boolean editable = true; // controls if checkboxes are clickable

    public StudentAdapter(ArrayList<Student> students) {
        this.students = students;
    }

    // 🔹 Helper to Enable / Disable checkbox editing (used after saving)
    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyDataSetChanged();
    }

    // 🔹 Helper to Set all students present or absent (Select All button)
    public void setAll(boolean present) {
        for (Student s : students) {
            s.setPresent(present);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_student_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);

        holder.tvRoll.setText(String.valueOf(student.getRoll()));
        holder.tvName.setText(student.getName());

        // 1. Remove previous listener to avoid bugs during scrolling
        holder.cbPresent.setOnCheckedChangeListener(null);

        // 2. Set current state from your Student model
        holder.cbPresent.setChecked(student.isPresent());
        holder.cbPresent.setEnabled(editable);

        // 3. Add listener to update your model when user taps checkbox
        holder.cbPresent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (editable) {
                student.setPresent(isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName;
        CheckBox cbPresent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tvRoll);
            tvName = itemView.findViewById(R.id.tvName);
            cbPresent = itemView.findViewById(R.id.cbPresent);
        }
    }
}