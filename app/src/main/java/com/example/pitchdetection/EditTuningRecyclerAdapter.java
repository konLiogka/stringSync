package com.example.pitchdetection;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EditTuningRecyclerAdapter extends RecyclerView.Adapter<EditTuningRecyclerAdapter.ViewHolder> {
    private List<String> items;
    private MainActivity activity;

    public EditTuningRecyclerAdapter(List<String> items, MainActivity activity) {
        if (items != null && items.size() > 1) {
            this.items = items.subList(1, items.size());
        } else {
            this.items = items;
        }
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.edittuning_textview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        holder.editTuningText.setText(item);

        holder.editButton.setOnClickListener(v -> {
            if (activity != null) {
                activity.onClickCustomTView(item, position + 1); // +1 to account for Automatic Tuning
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Confirmation");
            builder.setMessage("Are you sure you want to remove this item?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                if (activity != null) {
                    activity.removeTuning(activity, position + 1);
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateItems(List<String> newItems) {
        if (newItems != null && newItems.size() > 1) {
            this.items = newItems.subList(1, newItems.size());
        } else {
            this.items = newItems;
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView editTuningText;
        ImageButton editButton;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            editTuningText = itemView.findViewById(R.id.editTuningText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}