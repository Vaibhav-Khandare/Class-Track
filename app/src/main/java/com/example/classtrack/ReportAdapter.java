package com.example.classtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<StudentReport> reportList;
    private int totalLectures; // We need this to calculate Percentage

    public ReportAdapter(List<StudentReport> reportList, int totalLectures) {
        this.reportList = reportList;
        this.totalLectures = totalLectures;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse the same row layout you used for marking attendance,
        // OR create a new layout named 'row_report.xml' if you want different styling.
        // For now, I'm assuming you have a layout with 3 textviews.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_student_attendance, parent, false);
        // Note: You might need to hide the Checkbox in this layout or use a new layout
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentReport student = reportList.get(position);

        // Calculate Percentage
        int percentage = (totalLectures > 0) ? (student.getPresentCount() * 100) / totalLectures : 0;

        holder.tvRoll.setText(String.valueOf(student.getRoll()));
        holder.tvName.setText(student.getName());

        // Use the Checkbox text area or a specific TextView to show stats
        // If reusing row_student_attendance, hide checkbox and use a TextView
        String stat = student.getPresentCount() + "/" + totalLectures;
        holder.tvStatus.setText(stat);

        holder.tvPercentage.setText(percentage + "%");
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvName, tvStatus, tvPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tvRoll);
            tvName = itemView.findViewById(R.id.tvName);

            // You might need to add these IDs to your XML layout
            // tvStatus could be a new TextView in your row xml
            // tvPercentage could be another TextView in your row xml
            tvStatus = itemView.findViewById(R.id.tvRoll); // Placeholder, create specific ID in XML
            tvPercentage = itemView.findViewById(R.id.tvName); // Placeholder

            // NOTE: Ideally, create a layout called 'row_report_item.xml' with 4 TextViews
        }
    }
}