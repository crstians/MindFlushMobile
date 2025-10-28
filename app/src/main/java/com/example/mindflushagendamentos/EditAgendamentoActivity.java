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

public class EditAgendamentoActivity extends AppCompatActivity {

    private static final String TAG = "EditAgendamentoActivity";
    private static final int EDIT_CONFLICT_REQUEST_CODE = 2;

    private EditText edtNomePaciente, edtData, edtHorarioInicio, edtHorarioTermino;
    private Button btnAtualizar, btnExcluir;
    private ProgressBar progressBar;
    private AgendamentoRepository agendamentoRepository;
    private String agendamentoDocumentId;
    private Agendamento agendamentoAtual;

    private String nomeOriginal = "", dataOriginal = "", horarioInicioOriginal = "", horarioTerminoOriginal = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_agendamento);

        Toolbar toolbar = findViewById(R.id.toolbar_edit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Editar Agendamento");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        edtNomePaciente = findViewById(R.id.edtNomePacienteEdit);
        edtData = findViewById(R.id.edtDataEdit);
        edtHorarioInicio = findViewById(R.id.edtHorarioInicioEdit);
        edtHorarioTermino = findViewById(R.id.edtHorarioTerminoEdit);
        btnAtualizar = findViewById(R.id.btnAtualizar);
        btnExcluir = findViewById(R.id.btnExcluirEdit);
        progressBar = findViewById(R.id.progressBarEdit);
        agendamentoRepository = new AgendamentoRepository();

        edtData.setOnClickListener(v -> mostrarSeletorDeData());
        edtHorarioInicio.setOnClickListener(v -> mostrarSeletorDeHora(true));
        edtHorarioTermino.setOnClickListener(v -> mostrarSeletorDeHora(false));

        agendamentoDocumentId = getIntent().getStringExtra("AGENDAMENTO_DOC_ID");
        if (agendamentoDocumentId != null && !agendamentoDocumentId.isEmpty()) {
            carregarDadosDoAgendamento();
        } else {
            Toast.makeText(this, "Erro: ID do agendamento não encontrado.", Toast.LENGTH_LONG).show();
            finish();
        }

        btnAtualizar.setOnClickListener(v -> tentarAtualizarAgendamento());
        btnExcluir.setOnClickListener(v -> mostrarPopupConfirmacaoExclusao());
    }

    private void carregarDadosDoAgendamento() {
        setLoading(true);
        agendamentoRepository.getAgendamentoPorId(agendamentoDocumentId)
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    agendamentoAtual = documentSnapshot.toObject(Agendamento.class);
                    if (agendamentoAtual != null) {
                        nomeOriginal = agendamentoAtual.getNomePaciente();
                        dataOriginal = agendamentoAtual.getData();
                        horarioInicioOriginal = agendamentoAtual.getHorarioInicio();
                        horarioTerminoOriginal = agendamentoAtual.getHorarioTermino();
                        edtNomePaciente.setText(nomeOriginal);
                        edtData.setText(dataOriginal);
                        edtHorarioInicio.setText(horarioInicioOriginal);
                        edtHorarioTermino.setText(horarioTerminoOriginal);
                    }
                } else {
                    Toast.makeText(this, "Agendamento não encontrado.", Toast.LENGTH_SHORT).show();
                }
                setLoading(false);
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Falha ao carregar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void tentarAtualizarAgendamento() {
        String nome = edtNomePaciente.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String inicio = edtHorarioInicio.getText().toString().trim();
        String termino = edtHorarioTermino.getText().toString().trim();

        if (nome.isEmpty() || data.isEmpty() || inicio.isEmpty() || termino.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Agendamento agendamentoAtualizado = new Agendamento(nome, data, inicio, termino, false);
        agendamentoAtualizado.setDocumentId(agendamentoDocumentId); 

        agendamentoRepository.getConflitos(data, inicio, termino)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Agendamento> conflitos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (document.getId().equals(agendamentoDocumentId)) continue;
                        
                        Agendamento agExistente = document.toObject(Agendamento.class);
                        if (agendamentoAtualizado.getHorarioInicio().compareTo(agExistente.getHorarioTermino()) < 0 &&
                            agendamentoAtualizado.getHorarioTermino().compareTo(agExistente.getHorarioInicio()) > 0) {
                            conflitos.add(agExistente);
                        }
                    }

                    if (!conflitos.isEmpty()) {
                        setLoading(false);
                        mostrarPopupConflitoPrincipal(agendamentoAtualizado, conflitos);
                    } else {
                        agendamentoAtualizado.setInConflict(false);
                        atualizarNoFirestore(agendamentoAtualizado, null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Erro ao verificar conflitos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarPopupConflitoPrincipal(Agendamento agendamentoAtualizado, ArrayList<Agendamento> conflitos) {
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

        String message = "O novo horário conflita com " + conflitos.size() + " agendamento(s). O que deseja fazer?";
        tvMessage.setText(message);

        btnPermitir.setOnClickListener(v -> {
            permitirConflito(agendamentoAtualizado, conflitos);
            dialog.dismiss();
        });

        btnSobrescrever.setOnClickListener(v -> {
            mostrarPopupSelecaoSobrescrever(agendamentoAtualizado, conflitos);
            dialog.dismiss();
        });

        btnReagendar.setOnClickListener(v -> {
            mostrarPopupSelecaoReagendar(conflitos);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void mostrarPopupSelecaoSobrescrever(Agendamento agendamentoAtualizado, ArrayList<Agendamento> conflitos) {
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
                    sobrescreverAgendamentos(agendamentoAtualizado, agendamentosParaExcluir, conflitos);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    
    private void mostrarPopupSelecaoReagendar(ArrayList<Agendamento> conflitos) {
        if (conflitos.size() == 1) {
            Agendamento agendamentoParaReagendar = conflitos.get(0);
            Intent intent = new Intent(EditAgendamentoActivity.this, EditAgendamentoActivity.class);
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
                    Intent intent = new Intent(EditAgendamentoActivity.this, EditAgendamentoActivity.class);
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
            Toast.makeText(this, "Conflito resolvido. Tentando atualizar seu agendamento novamente...", Toast.LENGTH_LONG).show();
            tentarAtualizarAgendamento();
        }
    }
    
    private void permitirConflito(Agendamento agendamentoAtualizado, List<Agendamento> conflitosExistentes) {
        agendamentoAtualizado.setInConflict(true);
        for(Agendamento ag : conflitosExistentes) {
            ag.setInConflict(true);
        }
        atualizarNoFirestore(agendamentoAtualizado, conflitosExistentes, null);
    }
    
    private void sobrescreverAgendamentos(Agendamento agendamentoAtualizado, List<Agendamento> agendamentosParaExcluir, List<Agendamento> conflitosOriginais) {
        List<Agendamento> conflitosRestantes = conflitosOriginais.stream()
                .filter(ag -> !agendamentosParaExcluir.contains(ag))
                .collect(Collectors.toList());

        if (!conflitosRestantes.isEmpty()) {
            agendamentoAtualizado.setInConflict(true);
            for (Agendamento ag : conflitosRestantes) {
                ag.setInConflict(true);
            }
            atualizarNoFirestore(agendamentoAtualizado, conflitosRestantes, agendamentosParaExcluir);
        } else {
            agendamentoAtualizado.setInConflict(false);
            atualizarNoFirestore(agendamentoAtualizado, null, agendamentosParaExcluir);
        }
    }
    
    private void atualizarNoFirestore(Agendamento agendamentoParaSalvar, @Nullable List<Agendamento> agendamentosParaAtualizar, @Nullable List<Agendamento> agendamentosParaExcluir) {
        setLoading(true);
        agendamentoRepository.executarOperacaoBatch(agendamentoParaSalvar, agendamentosParaAtualizar, agendamentosParaExcluir)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Operação realizada com sucesso!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Falha na operação: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarPopupConfirmacaoExclusao() {
        if (agendamentoAtual == null) return;
        String detalhes = "Paciente: " + agendamentoAtual.getNomePaciente() + "\nData: " + agendamentoAtual.getData();

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão?")
                .setMessage("Deseja realmente excluir este agendamento?\n\n" + detalhes)
                .setPositiveButton("Sim, Excluir", (dialog, which) -> excluirDoFirestore())
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirDoFirestore() {
        setLoading(true);
        agendamentoRepository.excluirAgendamento(agendamentoDocumentId)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Agendamento excluído!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) { 
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE); 
        btnAtualizar.setEnabled(!loading); 
        btnExcluir.setEnabled(!loading); 
    }

    private void mostrarSeletorDeData() { Calendar cal = Calendar.getInstance(); new DatePickerDialog(this, (view, year, month, dayOfMonth) -> { String dataFormatada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year); edtData.setText(dataFormatada); }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show(); }
    private void mostrarSeletorDeHora(boolean isInicio) { Calendar cal = Calendar.getInstance(); new TimePickerDialog(this, (view, hourOfDay, minute) -> { String horaFormatada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute); if (isInicio) { edtHorarioInicio.setText(horaFormatada); cal.set(Calendar.HOUR_OF_DAY, hourOfDay + 1); cal.set(Calendar.MINUTE, minute); edtHorarioTermino.setText(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))); } else { edtHorarioTermino.setText(horaFormatada); } }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show(); }
    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { verificarAlteracoesEVoltar(); return true; } return super.onOptionsItemSelected(item); }
    @Override public void onBackPressed() { verificarAlteracoesEVoltar(); }
    private void verificarAlteracoesEVoltar() { boolean temAlteracao = !edtNomePaciente.getText().toString().equals(nomeOriginal) || !edtData.getText().toString().equals(dataOriginal) || !edtHorarioInicio.getText().toString().equals(horarioInicioOriginal) || !edtHorarioTermino.getText().toString().equals(horarioTerminoOriginal); if (temAlteracao) { new AlertDialog.Builder(this).setTitle("Descartar Alterações?").setMessage("Você tem alterações não salvas. Deseja mesmo sair?").setPositiveButton("Sim, Descartar", (dialog, which) -> { setResult(RESULT_CANCELED); finish(); }).setNegativeButton("Não, Ficar", null).show(); } else { setResult(RESULT_CANCELED); finish(); } }
}