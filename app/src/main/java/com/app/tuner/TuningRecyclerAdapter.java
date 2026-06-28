package com.app.tuner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TuningRecyclerAdapter extends RecyclerView.Adapter<TuningRecyclerAdapter.ViewHolder> {
    private List<String> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String[] notes, String clickedItem, RecyclerView recyclerView);
    }

    public TuningRecyclerAdapter(List<String> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateItems(List<String> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tuning_textview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        holder.tuningOption.setText(item);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                String[] notes = {};
                if (!item.equals("Automatic Tuning")) {
                    try {
                        String notesString = item.substring(item.indexOf("(") + 1, item.indexOf(")"));
                        notes = notesString.split(" ");
                    } catch (Exception e) {
                    }
                }
                RecyclerView recyclerView = v.findViewById(R.id.recyclerViewer);
                listener.onItemClick(notes, item, recyclerView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tuningOption;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tuningOption = itemView.findViewById(R.id.tuningOption);
        }
    }
}