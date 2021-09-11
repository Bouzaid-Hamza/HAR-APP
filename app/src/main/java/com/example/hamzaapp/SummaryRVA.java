package com.example.hamzaapp;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SummaryRVA extends RecyclerView.Adapter<SummaryRVA.ViewHolder> {

    private ArrayList<String> summaryItems;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView statText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            statText = itemView.findViewById(R.id.summary_item);
            statText.setTypeface(Typeface.MONOSPACE);
        }
    }

    public void setSummaryItems(ArrayList<String> summaryItems) {
        this.summaryItems = summaryItems;
//        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
            .from(parent.getContext())
            .inflate(R.layout.summary_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.statText.setText(summaryItems.get(position));
    }

    @Override
    public int getItemCount() {
        return summaryItems.size();
    }
}
