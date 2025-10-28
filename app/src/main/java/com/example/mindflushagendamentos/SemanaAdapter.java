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

public class SemanaAdapter extends ArrayAdapter<Object> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public SemanaAdapter(Context context, ArrayList<Object> items) {
        super(context, 0, items);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof String) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Object item = getItem(position);
        int type = getItemViewType(position);

        ViewHolderHeader holderHeader = null;
        ViewHolderItem holderItem = null;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            switch (type) {
                case TYPE_HEADER:
                    convertView = inflater.inflate(R.layout.list_header_semana, parent, false);
                    holderHeader = new ViewHolderHeader();
                    holderHeader.tvHeader = convertView.findViewById(R.id.tvHeader);
                    convertView.setTag(holderHeader);
                    break;
                case TYPE_ITEM:
                    convertView = inflater.inflate(R.layout.list_item_agendamento, parent, false);
                    holderItem = new ViewHolderItem();
                    // MUDANÇA: Usando os novos IDs do layout do item
                    holderItem.tvHorarioInicio = convertView.findViewById(R.id.tvHorarioInicio);
                    holderItem.tvHorarioTermino = convertView.findViewById(R.id.tvHorarioTermino);
                    holderItem.tvNomePaciente = convertView.findViewById(R.id.tvNomePaciente);
                    holderItem.ivConflictIcon = convertView.findViewById(R.id.ivConflictIcon);
                    convertView.setTag(holderItem);
                    break;
            }
        } else {
            switch (type) {
                case TYPE_HEADER:
                    holderHeader = (ViewHolderHeader) convertView.getTag();
                    break;
                case TYPE_ITEM:
                    holderItem = (ViewHolderItem) convertView.getTag();
                    break;
            }
        }

        if (item != null) {
            switch (type) {
                case TYPE_HEADER:
                    String headerText = (String) item;
                    holderHeader.tvHeader.setText(headerText);
                    break;
                case TYPE_ITEM:
                    Agendamento agendamento = (Agendamento) item;
                    // MUDANÇA: Preenchendo os novos TextViews separados
                    holderItem.tvHorarioInicio.setText(agendamento.getHorarioInicio());
                    holderItem.tvHorarioTermino.setText(agendamento.getHorarioTermino());
                    holderItem.tvNomePaciente.setText(agendamento.getNomePaciente());

                    if (agendamento.isInConflict()) {
                        holderItem.ivConflictIcon.setVisibility(View.VISIBLE);
                        convertView.setBackgroundColor(Color.parseColor("#FFFDE7")); // Amarelo Claro
                    } else {
                        holderItem.ivConflictIcon.setVisibility(View.GONE);
                        convertView.setBackgroundColor(Color.TRANSPARENT);
                    }
                    break;
            }
        }

        return convertView;
    }

    private static class ViewHolderHeader {
        TextView tvHeader;
    }

    // MUDANÇA: ViewHolder do item atualizado para refletir o novo layout
    private static class ViewHolderItem {
        TextView tvHorarioInicio;
        TextView tvHorarioTermino;
        TextView tvNomePaciente;
        ImageView ivConflictIcon;
    }
}