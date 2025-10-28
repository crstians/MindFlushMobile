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
import java.util.ArrayList;

public class AgendamentoAdapter extends ArrayAdapter<Agendamento> {

    public AgendamentoAdapter(Context context, ArrayList<Agendamento> agendamentos) {
        super(context, 0, agendamentos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
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

        Agendamento agendamento = getItem(position);

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

    private static class ViewHolder {
        TextView tvHorarioInicio;
        TextView tvHorarioTermino;
        TextView tvNomePaciente;
        ImageView ivConflictIcon;
    }
}