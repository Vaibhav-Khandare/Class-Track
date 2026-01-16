package com.example.classtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    List<Student> students;
    boolean editable = true;

    public StudentAdapter(List<Student> students) {
        this.students = students;
    }

    public void setAll(boolean present) {
        for (Student s : students) s.present = present;
        notifyDataSetChanged();
    }

    public void setEditable(boolean value) {
        editable = value;
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
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        Student s = students.get(i);
        h.tvRoll.setText(String.valueOf(s.roll));
        h.tvName.setText(s.name);
        h.cbPresent.setChecked(s.present);
        h.cbPresent.setEnabled(editable);

        h.cbPresent.setOnCheckedChangeListener((b, checked) -> s.present = checked);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName;
        CheckBox cbPresent;

        ViewHolder(View v) {
            super(v);
            tvRoll = v.findViewById(R.id.tvRoll);
            tvName = v.findViewById(R.id.tvName);
            cbPresent = v.findViewById(R.id.cbPresent);
        }
    }
}
