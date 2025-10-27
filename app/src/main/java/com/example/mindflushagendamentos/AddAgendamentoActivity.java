package com.example.mindflushagendamentos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddAgendamentoActivity extends AppCompatActivity {

    EditText edtNomePaciente, edtData, edtHorarioInicio, edtHorarioTermino;
    Button btnSalvar;
    BancoHelper bancoHelper;

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
        bancoHelper = new BancoHelper(this);

        Intent intent = getIntent();
        String dataSelecionada = intent.getStringExtra("DATA_SELECIONADA");
        String horarioSelecionado = intent.getStringExtra("HORARIO_SELECIONADO");
        if (dataSelecionada != null) { edtData.setText(dataSelecionada); }
        if (horarioSelecionado != null) { edtHorarioInicio.setText(horarioSelecionado); }

        edtData.setOnClickListener(v -> mostrarSeletorDeData());
        edtHorarioInicio.setOnClickListener(v -> mostrarSeletorDeHora(true));
        edtHorarioTermino.setOnClickListener(v -> mostrarSeletorDeHora(false));

        btnSalvar.setOnClickListener(v -> salvarAgendamento());
    }

    private void salvarAgendamento() {
        String nome = edtNomePaciente.getText().toString();
        String data = edtData.getText().toString();
        String inicio = edtHorarioInicio.getText().toString();
        String termino = edtHorarioTermino.getText().toString();

        if (nome.isEmpty() || data.isEmpty() || inicio.isEmpty() || termino.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Agendamento> conflitos = bancoHelper.getConflitos(data, inicio, termino, -1L);

        if (!conflitos.isEmpty()) {
            mostrarPopupConflitoPrincipal(conflitos);
        } else {
            bancoHelper.inserirAgendamento(nome, data, inicio, termino, false);
            Toast.makeText(this, "Agendamento salvo!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void mostrarPopupConflitoPrincipal(ArrayList<Agendamento> conflitos) {
        new AlertDialog.Builder(this)
                .setTitle("Conflito de Horário")
                .setMessage("Já existe(m) " + conflitos.size() + " agendamento(s) neste intervalo. O que você deseja fazer?")
                .setPositiveButton("Sobrescrever...", (dialog, which) -> {
                    mostrarPopupSelecaoSobrescrever(conflitos);
                })
                .setNeutralButton("Permitir Conflito", (dialog, which) -> {
                    String nome = edtNomePaciente.getText().toString();
                    String data = edtData.getText().toString();
                    String inicio = edtHorarioInicio.getText().toString();
                    String termino = edtHorarioTermino.getText().toString();

                    for (Agendamento ag : conflitos) {
                        bancoHelper.setConflitoStatus(ag.getId(), true);
                    }
                    bancoHelper.inserirAgendamento(nome, data, inicio, termino, true);
                    Toast.makeText(this, "Agendamento em conflito salvo!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarPopupSelecaoSobrescrever(ArrayList<Agendamento> conflitos) {
        String[] itensDisplay = new String[conflitos.size()];
        boolean[] itensSelecionados = new boolean[conflitos.size()];
        ArrayList<Long> idsParaExcluir = new ArrayList<>();

        for (int i = 0; i < conflitos.size(); i++) {
            Agendamento ag = conflitos.get(i);
            itensDisplay[i] = ag.getHorarioInicio() + " - " + ag.getHorarioTermino() + " - " + ag.getNomePaciente();
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione qual(is) sobrescrever")
                .setMultiChoiceItems(itensDisplay, itensSelecionados, (dialog, which, isChecked) -> {
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

                    String nome = edtNomePaciente.getText().toString();
                    String data = edtData.getText().toString();
                    String inicio = edtHorarioInicio.getText().toString();
                    String termino = edtHorarioTermino.getText().toString();

                    ArrayList<Agendamento> conflitosRestantes = bancoHelper.getConflitos(data, inicio, termino, -1L);
                    boolean aindaEmConflito = !conflitosRestantes.isEmpty();

                    if (aindaEmConflito) {
                        for (Agendamento ag : conflitosRestantes) {
                            bancoHelper.setConflitoStatus(ag.getId(), true);
                        }
                    }

                    bancoHelper.inserirAgendamento(nome, data, inicio, termino, aindaEmConflito);
                    Toast.makeText(this, idsParaExcluir.size() + " agendamento(s) sobrescrito(s)!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
                String horaTerminoSugerida = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                edtHorarioTermino.setText(horaTerminoSugerida);
            } else {
                edtHorarioTermino.setText(horaFormatada);
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        timePickerDialog.show();
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
        boolean temAlteracao = !edtNomePaciente.getText().toString().isEmpty() ||
                !edtData.getText().toString().isEmpty() ||
                !edtHorarioInicio.getText().toString().isEmpty() ||
                !edtHorarioTermino.getText().toString().isEmpty();

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
}