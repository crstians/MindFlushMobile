package com.example.mindflushagendamentos;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AddAgendamentoActivity extends AppCompatActivity {

    private static final String TAG = "AddAgendamentoActivity";
    private static final int EDIT_CONFLICT_REQUEST_CODE = 1;

    private EditText edtNomePaciente, edtData, edtHorarioInicio, edtHorarioTermino;
    private Button btnSalvar;
    private ProgressBar progressBar;
    private AgendamentoRepository agendamentoRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_agendamento);

        Toolbar toolbar = findViewById(R.id.toolbar_add);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Novo Agendamento");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        edtNomePaciente = findViewById(R.id.edtNomePacienteAdd);
        edtData = findViewById(R.id.edtDataAdd);
        edtHorarioInicio = findViewById(R.id.edtHorarioInicioAdd);
        edtHorarioTermino = findViewById(R.id.edtHorarioTerminoAdd);
        btnSalvar = findViewById(R.id.btnSalvarAdd);
        progressBar = findViewById(R.id.progressBarAdd);
        agendamentoRepository = new AgendamentoRepository();

        Intent intent = getIntent();
        String dataSelecionada = intent.getStringExtra("DATA_SELECIONADA");
        String horarioSelecionado = intent.getStringExtra("HORARIO_SELECIONADO");
        if (dataSelecionada != null) { edtData.setText(dataSelecionada); }
        if (horarioSelecionado != null) { edtHorarioInicio.setText(horarioSelecionado); }

        edtData.setOnClickListener(v -> mostrarSeletorDeData());
        edtHorarioInicio.setOnClickListener(v -> mostrarSeletorDeHora(true));
        edtHorarioTermino.setOnClickListener(v -> mostrarSeletorDeHora(false));

        btnSalvar.setOnClickListener(v -> tentarSalvarAgendamento());
    }

    private void tentarSalvarAgendamento() {
        Log.d(TAG, "Iniciando tentativa de salvar agendamento...");
        String nome = edtNomePaciente.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String inicio = edtHorarioInicio.getText().toString().trim();
        String termino = edtHorarioTermino.getText().toString().trim();

        if (nome.isEmpty() || data.isEmpty() || inicio.isEmpty() || termino.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (inicio.compareTo(termino) >= 0) {
            Toast.makeText(this, "O horário de término deve ser após o horário de início.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Agendamento novoAgendamento = new Agendamento(nome, data, inicio, termino, false);

        agendamentoRepository.getConflitos(data, inicio, termino)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Agendamento> conflitos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Agendamento agExistente = document.toObject(Agendamento.class);
                        if (novoAgendamento.getHorarioInicio().compareTo(agExistente.getHorarioTermino()) < 0 &&
                            novoAgendamento.getHorarioTermino().compareTo(agExistente.getHorarioInicio()) > 0) {
                            conflitos.add(agExistente);
                        }
                    }

                    if (!conflitos.isEmpty()) {
                        Log.d(TAG, "CONFLITO DETECTADO. Agendamentos conflitantes: " + conflitos.size());
                        setLoading(false);
                        mostrarPopupConflitoPrincipal(novoAgendamento, conflitos);
                    } else {
                        Log.d(TAG, "Nenhum conflito de sobreposição encontrado. Prosseguindo para salvar.");
                        salvarNoFirestore(novoAgendamento, null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao verificar conflitos no Firestore.", e);
                    setLoading(false);
                    Toast.makeText(this, "Erro ao verificar conflitos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarPopupConflitoPrincipal(Agendamento novoAgendamento, ArrayList<Agendamento> conflitos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_conflict_options, null);

        builder.setView(dialogView);
        builder.setTitle("Conflito de Horário");
        
        final AlertDialog dialog = builder.create();

        TextView tvMessage = dialogView.findViewById(R.id.tvConflictMessage);
        Button btnPermitir = dialogView.findViewById(R.id.btnPermitirConflito);
        Button btnSobrescrever = dialogView.findViewById(R.id.btnSobrescrever);
        Button btnReagendar = dialogView.findViewById(R.id.btnReagendar);

        String message = "O horário selecionado conflita com " + conflitos.size() + " agendamento(s). O que deseja fazer?";
        tvMessage.setText(message);

        btnPermitir.setOnClickListener(v -> {
            permitirConflito(novoAgendamento, conflitos);
            dialog.dismiss();
        });

        btnSobrescrever.setOnClickListener(v -> {
            mostrarPopupSelecaoSobrescrever(novoAgendamento, conflitos);
            dialog.dismiss();
        });

        btnReagendar.setOnClickListener(v -> {
            mostrarPopupSelecaoReagendar(conflitos);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void mostrarPopupSelecaoSobrescrever(Agendamento novoAgendamento, ArrayList<Agendamento> conflitos) {
        String[] itensDisplay = new String[conflitos.size()];
        final ArrayList<Agendamento> agendamentosParaExcluir = new ArrayList<>();

        for (int i = 0; i < conflitos.size(); i++) {
            Agendamento ag = conflitos.get(i);
            itensDisplay[i] = ag.getHorarioInicio() + " - " + ag.getHorarioTermino() + " (" + ag.getNomePaciente() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione qual(is) sobrescrever")
                .setMultiChoiceItems(itensDisplay, null, (dialog, which, isChecked) -> {
                    if (isChecked) agendamentosParaExcluir.add(conflitos.get(which));
                    else agendamentosParaExcluir.remove(conflitos.get(which));
                })
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (agendamentosParaExcluir.isEmpty()) {
                        Toast.makeText(this, "Nenhum agendamento selecionado.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sobrescreverAgendamentos(novoAgendamento, agendamentosParaExcluir, conflitos);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    private void mostrarPopupSelecaoReagendar(ArrayList<Agendamento> conflitos) {
        if (conflitos.size() == 1) {
            Agendamento agendamentoParaReagendar = conflitos.get(0);
            Intent intent = new Intent(AddAgendamentoActivity.this, EditAgendamentoActivity.class);
            intent.putExtra("AGENDAMENTO_DOC_ID", agendamentoParaReagendar.getDocumentId());
            startActivityForResult(intent, EDIT_CONFLICT_REQUEST_CODE);
            return;
        }

        String[] itensDisplay = new String[conflitos.size()];
        for (int i = 0; i < conflitos.size(); i++) {
            Agendamento ag = conflitos.get(i);
            itensDisplay[i] = ag.getHorarioInicio() + " - " + ag.getHorarioTermino() + " (" + ag.getNomePaciente() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione qual conflito reagendar")
                .setItems(itensDisplay, (dialog, which) -> {
                    Agendamento agendamentoParaReagendar = conflitos.get(which);
                    Intent intent = new Intent(AddAgendamentoActivity.this, EditAgendamentoActivity.class);
                    intent.putExtra("AGENDAMENTO_DOC_ID", agendamentoParaReagendar.getDocumentId());
                    startActivityForResult(intent, EDIT_CONFLICT_REQUEST_CODE);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_CONFLICT_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Conflito resolvido. Tentando salvar seu agendamento novamente...", Toast.LENGTH_LONG).show();
            tentarSalvarAgendamento();
        }
    }

    private void permitirConflito(Agendamento novoAgendamento, List<Agendamento> conflitosExistentes) {
        novoAgendamento.setInConflict(true);
        for(Agendamento ag : conflitosExistentes) {
            ag.setInConflict(true);
        }
        salvarNoFirestore(novoAgendamento, conflitosExistentes, null);
    }
    
    private void sobrescreverAgendamentos(Agendamento novoAgendamento, List<Agendamento> agendamentosParaExcluir, List<Agendamento> conflitosOriginais) {
        List<Agendamento> conflitosRestantes = conflitosOriginais.stream()
                .filter(ag -> !agendamentosParaExcluir.contains(ag))
                .collect(Collectors.toList());

        if (!conflitosRestantes.isEmpty()) {
            novoAgendamento.setInConflict(true);
            for (Agendamento ag : conflitosRestantes) {
                ag.setInConflict(true);
            }
            salvarNoFirestore(novoAgendamento, conflitosRestantes, agendamentosParaExcluir);
        } else {
            novoAgendamento.setInConflict(false);
            salvarNoFirestore(novoAgendamento, null, agendamentosParaExcluir);
        }
    }

    private void salvarNoFirestore(Agendamento agendamentoParaSalvar, @Nullable List<Agendamento> agendamentosParaAtualizar, @Nullable List<Agendamento> agendamentosParaExcluir) {
        Log.d(TAG, "Iniciando a operação de escrita em lote (batch write)...");
        setLoading(true);
        agendamentoRepository.executarOperacaoBatch(agendamentoParaSalvar, agendamentosParaAtualizar, agendamentosParaExcluir)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Operação em lote concluída com sucesso!");
                    setLoading(false);
                    Toast.makeText(this, "Operação realizada com sucesso!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha na operação de escrita em lote.", e);
                    setLoading(false);
                    Toast.makeText(this, "Falha na operação: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) { if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE); btnSalvar.setEnabled(!loading); }
    private void mostrarSeletorDeData() { Calendar cal = Calendar.getInstance(); new DatePickerDialog(this, (view, year, month, dayOfMonth) -> { String dataFormatada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year); edtData.setText(dataFormatada); }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show(); }
    private void mostrarSeletorDeHora(boolean isInicio) { Calendar cal = Calendar.getInstance(); new TimePickerDialog(this, (view, hourOfDay, minute) -> { String horaFormatada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute); if (isInicio) { edtHorarioInicio.setText(horaFormatada); cal.set(Calendar.HOUR_OF_DAY, hourOfDay + 1); cal.set(Calendar.MINUTE, minute); edtHorarioTermino.setText(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))); } else { edtHorarioTermino.setText(horaFormatada); } }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show(); }
    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { verificarAlteracoesEVoltar(); return true; } return super.onOptionsItemSelected(item); }
    @Override public void onBackPressed() { verificarAlteracoesEVoltar(); }
    private void verificarAlteracoesEVoltar() { boolean temAlteracao = !edtNomePaciente.getText().toString().isEmpty() || !edtData.getText().toString().isEmpty() || !edtHorarioInicio.getText().toString().isEmpty() || !edtHorarioTermino.getText().toString().isEmpty(); if (temAlteracao) { new AlertDialog.Builder(this).setTitle("Descartar Alterações?").setMessage("Você tem alterações não salvas. Deseja mesmo sair?").setPositiveButton("Sim, Descartar", (dialog, which) -> finish()).setNegativeButton("Não, Ficar", null).show(); } else { finish(); } }
}