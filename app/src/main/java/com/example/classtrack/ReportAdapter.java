package com.example.classtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<StudentReport> reportList;
    private int totalLectures;

    public ReportAdapter(List<StudentReport> reportList, int totalLectures) {
        this.reportList = reportList;
        this.totalLectures = totalLectures;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 🔥 FIX: Use the new row_report_item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_report_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentReport student = reportList.get(position);

        // Calculate Percentage
        int percentage = (totalLectures > 0) ? (student.getPresentCount() * 100) / totalLectures : 0;

        holder.tvRoll.setText(String.valueOf(student.getRoll()));
        holder.tvName.setText(student.getName());

        // "Attended" column format: "5/12"
        String attendedStr = student.getPresentCount() + "/" + totalLectures;
        holder.tvAttended.setText(attendedStr);

        // "%" column
        holder.tvPercentage.setText(percentage + "%");

        // Optional: Color code low attendance (< 75%)
        if(percentage < 75) {
            holder.tvPercentage.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvPercentage.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName, tvAttended, tvPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tvRoll);
            tvName = itemView.findViewById(R.id.tvName);
            tvAttended = itemView.findViewById(R.id.tvAttended);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
        }
    }
}