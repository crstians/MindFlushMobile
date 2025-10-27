package com.example.mindflushagendamentos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EditAgendamentoActivity extends AppCompatActivity {

    EditText edtNomePaciente, edtData, edtHorarioInicio, edtHorarioTermino;
    Button btnAtualizar, btnExcluir;
    BancoHelper bancoHelper;
    long agendamentoId;

    private String nomeOriginal = "";
    private String dataOriginal = "";
    private String horarioInicioOriginal = "";
    private String horarioTerminoOriginal = "";

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
        bancoHelper = new BancoHelper(this);

        edtData.setOnClickListener(v -> mostrarSeletorDeData());
        edtHorarioInicio.setOnClickListener(v -> mostrarSeletorDeHora(true));
        edtHorarioTermino.setOnClickListener(v -> mostrarSeletorDeHora(false));

        agendamentoId = getIntent().getLongExtra("AGENDAMENTO_ID", -1);
        if (agendamentoId != -1) {
            carregarDadosDoAgendamento();
        }

        btnAtualizar.setOnClickListener(v -> mostrarPopupConfirmacaoEdicao());
        btnExcluir.setOnClickListener(v -> mostrarPopupConfirmacaoExclusao(agendamentoId));
    }

    private void mostrarPopupConfirmacaoEdicao() {
        String nomeNovo = edtNomePaciente.getText().toString();
        String dataNova = edtData.getText().toString();
        String inicioNovo = edtHorarioInicio.getText().toString();
        String terminoNovo = edtHorarioTermino.getText().toString();

        StringBuilder detalhes = new StringBuilder();
        if (!nomeNovo.equals(nomeOriginal)) detalhes.append("Paciente: '").append(nomeOriginal).append("' -> '").append(nomeNovo).append("'\n");
        if (!dataNova.equals(dataOriginal)) detalhes.append("Data: '").append(dataOriginal).append("' -> '").append(dataNova).append("'\n");
        if (!inicioNovo.equals(horarioInicioOriginal)) detalhes.append("Início: '").append(horarioInicioOriginal).append("' -> '").append(inicioNovo).append("'\n");
        if (!terminoNovo.equals(horarioTerminoOriginal)) detalhes.append("Término: '").append(horarioTerminoOriginal).append("' -> '").append(terminoNovo).append("'\n");

        if (detalhes.length() == 0) {
            Toast.makeText(this, "Nenhuma alteração foi feita.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Alterações?")
                .setMessage("Você está alterando os seguintes dados:\n\n" + detalhes.toString())
                .setPositiveButton("Sim, Confirmar", (dialog, which) -> atualizarAgendamento())
                .setNegativeButton("Não", null)
                .show();
    }

    private void mostrarPopupConfirmacaoExclusao(long idParaExcluir) {
        String detalhes = "Paciente: " + nomeOriginal + "\nData: " + dataOriginal + "\nHorário: " + horarioInicioOriginal + " - " + horarioTerminoOriginal;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão?")
                .setMessage("Tem certeza de que deseja excluir este agendamento?\n\n" + detalhes)
                .setPositiveButton("Sim, Excluir", (dialog, which) -> {
                    bancoHelper.excluirAgendamento(idParaExcluir);
                    Toast.makeText(this, "Agendamento excluído!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void carregarDadosDoAgendamento() {
        Cursor cursor = bancoHelper.getAgendamentoPorId(agendamentoId);
        if (cursor != null && cursor.moveToFirst()) {
            nomeOriginal = cursor.getString(1);
            dataOriginal = cursor.getString(2);
            horarioInicioOriginal = cursor.getString(3);
            horarioTerminoOriginal = cursor.getString(4);

            edtNomePaciente.setText(nomeOriginal);
            edtData.setText(dataOriginal);
            edtHorarioInicio.setText(horarioInicioOriginal);
            edtHorarioTermino.setText(horarioTerminoOriginal);
            cursor.close();
        }
    }

    private void atualizarAgendamento() {
        String nome = edtNomePaciente.getText().toString();
        String data = edtData.getText().toString();
        String inicio = edtHorarioInicio.getText().toString();
        String termino = edtHorarioTermino.getText().toString();

        if (nome.isEmpty() || data.isEmpty() || inicio.isEmpty() || termino.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Agendamento> conflitos = bancoHelper.getConflitos(data, inicio, termino, agendamentoId);

        if (!conflitos.isEmpty()) {
            mostrarPopupConflitoPrincipal(conflitos);
        } else {
            realizarAtualizacao(false);
        }
    }

    private void mostrarPopupConflitoPrincipal(ArrayList<Agendamento> conflitos) {
        new AlertDialog.Builder(this)
                .setTitle("Conflito de Horário")
                .setMessage("O novo horário entra em conflito com " + conflitos.size() + " agendamento(s) existente(s). O que deseja fazer?")
                .setPositiveButton("Sobrescrever...", (dialog, which) -> mostrarPopupSelecaoSobrescrever(conflitos))
                .setNeutralButton("Permitir Conflito", (dialog, which) -> {
                    for (Agendamento ag : conflitos) {
                        bancoHelper.setConflitoStatus(ag.getId(), true);
                    }
                    realizarAtualizacao(true);
                    Toast.makeText(this, "Agendamento em conflito salvo!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarPopupSelecaoSobrescrever(ArrayList<Agendamento> conflitos) {
        String[] itensDisplay = new String[conflitos.size()];
        ArrayList<Long> idsParaExcluir = new ArrayList<>();

        for (int i = 0; i < conflitos.size(); i++) {
            Agendamento ag = conflitos.get(i);
            itensDisplay[i] = ag.getHorarioInicio() + " - " + ag.getHorarioTermino() + " - " + ag.getNomePaciente();
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione qual(is) sobrescrever")
                .setMultiChoiceItems(itensDisplay, null, (dialog, which, isChecked) -> {
                    long idSelecionado = conflitos.get(which).getId();
                    if (isChecked) {
                        idsParaExcluir.add(idSelecionado);
                    } else {
                        idsParaExcluir.remove(idSelecionado);
                    }
                })
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (idsParaExcluir.isEmpty()) {
                        Toast.makeText(this, "Nenhum agendamento selecionado para sobrescrever.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (long id : idsParaExcluir) {
                        bancoHelper.excluirAgendamento(id);
                    }

                    String data = edtData.getText().toString();
                    String inicio = edtHorarioInicio.getText().toString();
                    String termino = edtHorarioTermino.getText().toString();

                    ArrayList<Agendamento> conflitosRestantes = bancoHelper.getConflitos(data, inicio, termino, agendamentoId);
                    boolean aindaEmConflito = !conflitosRestantes.isEmpty();

                    if(aindaEmConflito) {
                        for (Agendamento ag : conflitosRestantes) {
                            bancoHelper.setConflitoStatus(ag.getId(), true);
                        }
                    }

                    realizarAtualizacao(aindaEmConflito);
                    Toast.makeText(this, idsParaExcluir.size() + " agendamento(s) sobrescrito(s)!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void realizarAtualizacao(boolean isInConflict) {
        String nome = edtNomePaciente.getText().toString();
        String data = edtData.getText().toString();
        String inicio = edtHorarioInicio.getText().toString();
        String termino = edtHorarioTermino.getText().toString();

        int resultado = bancoHelper.atualizarAgendamento(agendamentoId, nome, data, inicio, termino, isInConflict);
        if (resultado > 0) {
            Toast.makeText(this, "Agendamento atualizado!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erro ao atualizar!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            verificarAlteracoesEVoltar();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        verificarAlteracoesEVoltar();
    }

    private void verificarAlteracoesEVoltar() {
        boolean temAlteracao = !edtNomePaciente.getText().toString().equals(nomeOriginal) ||
                !edtData.getText().toString().equals(dataOriginal) ||
                !edtHorarioInicio.getText().toString().equals(horarioInicioOriginal) ||
                !edtHorarioTermino.getText().toString().equals(horarioTerminoOriginal);

        if (temAlteracao) {
            new AlertDialog.Builder(this)
                    .setTitle("Descartar Alterações?")
                    .setMessage("Você tem alterações não salvas. Deseja mesmo sair?")
                    .setPositiveButton("Sim, Descartar", (dialog, which) -> finish())
                    .setNegativeButton("Não, Ficar", null)
                    .show();
        } else {
            finish();
        }
    }

    private void mostrarSeletorDeData() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dataFormatada = dayOfMonth + "/" + (month + 1) + "/" + year;
            edtData.setText(dataFormatada);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void mostrarSeletorDeHora(boolean isInicio) {
        Calendar cal = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String horaFormatada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (isInicio) {
                edtHorarioInicio.setText(horaFormatada);
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay + 1);
                cal.set(Calendar.MINUTE, minute);
                edtHorarioTermino.setText(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
            } else {
                edtHorarioTermino.setText(horaFormatada);
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }
}