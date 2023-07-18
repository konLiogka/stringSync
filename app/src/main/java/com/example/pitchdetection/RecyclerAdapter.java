package com.example.pitchdetection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    static private List<String> items;


    public RecyclerAdapter(List<String> items) {
        RecyclerAdapter.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tuning_textview, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String item = items.get(position);
        holder.tuningOption.setText(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    static private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        RecyclerAdapter.listener = listener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tuningOption;

        private String clickedItem ;

        public ViewHolder(View itemView) {
            super(itemView);
            tuningOption = itemView.findViewById(R.id.tuningOption);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    clickedItem = items.get(position);

                    String[] notes= {""};
                    if(!clickedItem.equals("Automatic Tuning")){
                        String notesString = clickedItem.substring(clickedItem.indexOf("(") + 1, clickedItem.indexOf(")"));

                        notes = notesString.split(" ");
                    }
                        listener.onItemClick(notes, clickedItem);


                }
            });

        }





    }

    public interface OnItemClickListener {
        void onItemClick(String[] notes, String clickedItem);
    }
}