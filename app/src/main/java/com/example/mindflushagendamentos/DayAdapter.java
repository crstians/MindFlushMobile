package com.example.mindflushagendamentos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final ArrayList<LocalDate> days;
    private final OnItemListener onItemListener;
    private int selectedPosition = -1;

    public DayAdapter(ArrayList<LocalDate> days, OnItemListener onItemListener) {
        this.days = days;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_day_week, parent, false);
        return new DayViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE", new Locale("pt", "BR"));
        holder.tvDayName.setText(date.format(formatter).toUpperCase());

        holder.itemView.setSelected(selectedPosition == position);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
        notifyItemChanged(selectedPosition);
    }

    class DayViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView tvDayName;
        private final TextView tvDayNumber;
        private final OnItemListener onItemListener;

        public DayViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemListener.onItemClick(getAdapterPosition(), days.get(getAdapterPosition()));
        }
    }

    public interface OnItemListener {
        void onItemClick(int position, LocalDate date);
    }
}
