package com.example.mainactivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private ArrayList<Task> taskList;

    public HistoryAdapter(ArrayList<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // CHANGE: Use our new custom layout 'history_item' instead of simple_list_item_2
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);

        // 1. Set Name (Black and Bold)
        holder.tvName.setText(task.name);

        // 2. Set Time (Clean format)
        holder.tvTime.setText(String.format("%d:%02d %s", task.hour, task.minute, task.amPm));

        // 3. Handle Urgency Dot Color
        // Note: Ensure you have these drawable resources (red_circle, yellow_circle, green_circle)
        // If not, you can replace .setBackgroundResource with .setBackgroundColor(Color.RED) etc.
        switch (task.urgency) {
            case "High":
                holder.urgencyView.setBackgroundResource(R.drawable.red_circle);
                break;
            case "Medium":
                holder.urgencyView.setBackgroundResource(R.drawable.yellow_circle);
                break;
            case "Low":
                holder.urgencyView.setBackgroundResource(R.drawable.green_circle);
                break;
            default:
                holder.urgencyView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvStatus;
        View urgencyView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Connect to the IDs defined in history_item.xml
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            tvName = itemView.findViewById(R.id.tvHistoryName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            urgencyView = itemView.findViewById(R.id.viewUrgencyParams);
        }
    }
}
