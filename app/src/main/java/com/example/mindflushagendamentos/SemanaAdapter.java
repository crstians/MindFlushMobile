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

// Este adapter aceita uma lista de Objetos, que podem ser Strings (para cabeçalhos) ou Agendamentos.
public class SemanaAdapter extends ArrayAdapter<Object> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public SemanaAdapter(Context context, ArrayList<Object> items) {
        super(context, 0, items);
    }

    // Informa ao ListView que temos dois tipos diferentes de layouts.
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    // Para cada item da lista, decide se ele é um cabeçalho (tipo 0) ou um agendamento (tipo 1).
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
        // Pega o objeto (String ou Agendamento) na posição atual
        Object item = getItem(position);
        int type = getItemViewType(position);

        // O ViewHolder é um padrão de otimização para evitar chamadas repetitivas de findViewById()
        ViewHolderHeader holderHeader = null;
        ViewHolderItem holderItem = null;

        if (convertView == null) {
            // Se a view não existe, cria uma nova baseada no tipo (cabeçalho ou item)
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
                    holderItem.tvAgendamentoInfo = convertView.findViewById(R.id.tvAgendamentoInfo);
                    holderItem.ivConflictIcon = convertView.findViewById(R.id.ivConflictIcon);
                    convertView.setTag(holderItem);
                    break;
            }
        } else {
            // Se a view já existe (está sendo reciclada), apenas pega os holders
            switch (type) {
                case TYPE_HEADER:
                    holderHeader = (ViewHolderHeader) convertView.getTag();
                    break;
                case TYPE_ITEM:
                    holderItem = (ViewHolderItem) convertView.getTag();
                    break;
            }
        }

        // Agora, preenche os dados na view correta
        if (item != null) {
            switch (type) {
                case TYPE_HEADER:
                    // Se for um cabeçalho, preenche o TextView do cabeçalho
                    String headerText = (String) item;
                    holderHeader.tvHeader.setText(headerText);
                    break;
                case TYPE_ITEM:
                    // Se for um agendamento, preenche o TextView do item e ajusta o ícone/cor de conflito
                    Agendamento agendamento = (Agendamento) item;
                    String info = "   " + agendamento.getHorarioInicio() + " - " + agendamento.getHorarioTermino() + " - " + agendamento.getNomePaciente();
                    holderItem.tvAgendamentoInfo.setText(info);

                    if (agendamento.isInConflict()) {
                        holderItem.ivConflictIcon.setVisibility(View.VISIBLE);
                        convertView.setBackgroundColor(Color.parseColor("#FFFFE0")); // Amarelo Claro
                    } else {
                        holderItem.ivConflictIcon.setVisibility(View.GONE);
                        convertView.setBackgroundColor(Color.TRANSPARENT);
                    }
                    break;
            }
        }

        return convertView;
    }

    // Classes internas para o padrão ViewHolder
    private static class ViewHolderHeader {
        TextView tvHeader;
    }

    private static class ViewHolderItem {
        TextView tvAgendamentoInfo;
        ImageView ivConflictIcon;
    }
}