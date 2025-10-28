package com.example.mindflushagendamentos;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

interface ListItem {}

class HeaderItem implements ListItem {
    private final String date;
    public HeaderItem(String date) { this.date = date; }
    public String getDate() { return date; }
}

class AgendamentoItem implements ListItem {
    private final Agendamento agendamento;
    public AgendamentoItem(Agendamento agendamento) { this.agendamento = agendamento; }
    public Agendamento getAgendamento() { return agendamento; }
}

public class WeeklyAgendamentoAdapter extends ArrayAdapter<ListItem> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_AGENDAMENTO = 1;

    public WeeklyAgendamentoAdapter(Context context, List<ListItem> items) {
        super(context, 0, items);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof HeaderItem ? TYPE_HEADER : TYPE_AGENDAMENTO;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        int type = getItemViewType(position);

        if (type == TYPE_HEADER) {
            HeaderItem header = (HeaderItem) getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_week_header, parent, false);
            }
            TextView tvHeader = convertView.findViewById(R.id.tvWeekDayHeader);
            tvHeader.setText(header.getDate());
            return convertView;
        } else {
            AgendamentoItem agendamentoItem = (AgendamentoItem) getItem(position);
            Agendamento agendamento = agendamentoItem.getAgendamento();
            
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_agendamento, parent, false);
                holder = new ViewHolder();
                holder.tvHorarioInicio = convertView.findViewById(R.id.tvHorarioInicio);
                holder.tvHorarioTermino = convertView.findViewById(R.id.tvHorarioTermino);
                holder.tvNomePaciente = convertView.findViewById(R.id.tvNomePaciente);
                holder.ivConflictIcon = convertView.findViewById(R.id.ivConflictIcon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (agendamento != null) {
                holder.tvHorarioInicio.setText(agendamento.getHorarioInicio());
                holder.tvHorarioTermino.setText(agendamento.getHorarioTermino());
                holder.tvNomePaciente.setText(agendamento.getNomePaciente());

                if (agendamento.isInConflict()) {
                    holder.ivConflictIcon.setVisibility(View.VISIBLE);
                    convertView.setBackgroundColor(Color.parseColor("#FFFDE7"));
                } else {
                    holder.ivConflictIcon.setVisibility(View.GONE);
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                }
            }
            return convertView;
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position) instanceof AgendamentoItem;
    }

    private static class ViewHolder {
        TextView tvHorarioInicio;
        TextView tvHorarioTermino;
        TextView tvNomePaciente;
        ImageView ivConflictIcon;
    }
}