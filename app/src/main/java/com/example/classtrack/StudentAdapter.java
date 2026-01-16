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
    private boolean editable = true; // controls checkbox edit mode

    public StudentAdapter(ArrayList<Student> students) {
        this.students = students;
    }

    // 🔹 Enable / Disable checkbox editing
    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyDataSetChanged();
    }

    // 🔹 Set all students present or absent
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

        // VERY IMPORTANT to avoid checkbox recycling issues
        holder.cbPresent.setOnCheckedChangeListener(null);

        holder.cbPresent.setChecked(student.isPresent());
        holder.cbPresent.setEnabled(editable);

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

    // 🔹 ViewHolder
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
