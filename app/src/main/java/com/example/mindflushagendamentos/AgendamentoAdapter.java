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
        // Pega o objeto Agendamento para esta posição na lista
        Agendamento agendamento = getItem(position);

        // Verifica se uma view existente está sendo reutilizada, senão, infla uma nova
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_agendamento, parent, false);
        }

        // Pega as referências para os componentes do nosso layout customizado
        TextView tvAgendamentoInfo = convertView.findViewById(R.id.tvAgendamentoInfo);
        ImageView ivConflictIcon = convertView.findViewById(R.id.ivConflictIcon);

        // Se o agendamento não for nulo, preenche os dados na view
        if (agendamento != null) {
            // Monta o texto a ser exibido
            String info = agendamento.getHorarioInicio() + " - " + agendamento.getHorarioTermino() + " - " + agendamento.getNomePaciente();
            tvAgendamentoInfo.setText(info);

            // --- AQUI ESTÁ A LÓGICA PRINCIPAL DO DESTAQUE VISUAL ---
            if (agendamento.isInConflict()) {
                // Se o agendamento está em conflito:
                // 1. Mostra o ícone de alerta
                ivConflictIcon.setVisibility(View.VISIBLE);
                // 2. Muda a cor de fundo da linha para um amarelo claro
                convertView.setBackgroundColor(Color.parseColor("#FFFFE0")); // Light Yellow
            } else {
                // Se não está em conflito, garante que tudo volte ao normal
                // 1. Esconde o ícone de alerta
                ivConflictIcon.setVisibility(View.GONE);
                // 2. Define a cor de fundo como transparente (padrão)
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        // Retorna a view completa para ser desenhada na tela
        return convertView;
    }
}