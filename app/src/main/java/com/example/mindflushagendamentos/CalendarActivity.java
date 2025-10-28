package com.example.mindflushagendamentos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private enum ViewMode {
        MONTH, WEEK, DAY
    }
    private ViewMode currentViewMode = ViewMode.MONTH;

    // Componentes da UI
    CalendarView calendarView;
    ListView listViewHorarios;
    FloatingActionButton btnAddAgendamento;
    Button btnViewMes, btnViewSemana, btnViewDia;
    RelativeLayout navigationControls;
    ImageButton btnAnterior, btnProximo;
    TextView tvDataNavegacao;

    // Variáveis de dados
    BancoHelper bancoHelper;
    Date objetoDataSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        bancoHelper = new BancoHelper(this);
        calendarView = findViewById(R.id.calendarView);
        listViewHorarios = findViewById(R.id.listViewHorarios);
        btnAddAgendamento = findViewById(R.id.btnAddAgendamento);
        btnViewMes = findViewById(R.id.btnViewMes);
        btnViewSemana = findViewById(R.id.btnViewSemana);
        btnViewDia = findViewById(R.id.btnViewDia);
        navigationControls = findViewById(R.id.navigation_controls);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnProximo = findViewById(R.id.btnProximo);
        tvDataNavegacao = findViewById(R.id.tvDataNavegacao);

        objetoDataSelecionada = new Date();
        calendarView.setDate(objetoDataSelecionada.getTime());

        configurarListeners();
        atualizarVisibilidadeEControles();
    }

    private void configurarListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            objetoDataSelecionada = cal.getTime();
            carregarAgendamentosDoDia(new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(objetoDataSelecionada));
        });

        btnViewMes.setOnClickListener(v -> {
            currentViewMode = ViewMode.MONTH;
            atualizarVisibilidadeEControles();
        });
        btnViewDia.setOnClickListener(v -> {
            currentViewMode = ViewMode.DAY;
            atualizarVisibilidadeEControles();
        });
        btnViewSemana.setOnClickListener(v -> {
            currentViewMode = ViewMode.WEEK;
            atualizarVisibilidadeEControles();
        });

        btnAnterior.setOnClickListener(v -> navegarData(-1));
        btnProximo.setOnClickListener(v -> navegarData(1));

        btnAddAgendamento.setOnClickListener(v -> abrirTelaDeAdicionar());

        listViewHorarios.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof Agendamento) {
                Agendamento agendamentoClicado = (Agendamento) item;
                mostrarPopupOpcoes(agendamentoClicado.getId());
            }
        });
    }

    private void navegarData(int direcao) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(objetoDataSelecionada);
        if (currentViewMode == ViewMode.DAY) {
            cal.add(Calendar.DAY_OF_YEAR, direcao);
        } else if (currentViewMode == ViewMode.WEEK) {
            cal.add(Calendar.WEEK_OF_YEAR, direcao);
        }
        objetoDataSelecionada = cal.getTime();
        calendarView.setDate(objetoDataSelecionada.getTime(), true, true);
        atualizarVisibilidadeEControles();
    }

    private void atualizarVisibilidadeEControles() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) listViewHorarios.getLayoutParams();
        if (currentViewMode == ViewMode.MONTH) {
            navigationControls.setVisibility(View.GONE);
            calendarView.setVisibility(View.VISIBLE);
            params.addRule(RelativeLayout.BELOW, R.id.calendarView);
            carregarAgendamentosDoDia(new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(objetoDataSelecionada));
        } else {
            navigationControls.setVisibility(View.VISIBLE);
            calendarView.setVisibility(View.GONE);
            params.addRule(RelativeLayout.BELOW, R.id.navigation_controls);
            if (currentViewMode == ViewMode.DAY) {
                carregarAgendamentosDoDia(new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(objetoDataSelecionada));
            } else { // WEEK
                carregarAgendamentosDaSemana(objetoDataSelecionada);
            }
        }
        listViewHorarios.setLayoutParams(params);
    }

    private void carregarAgendamentosDaSemana(Date dataBase) {
        SimpleDateFormat sdfNav = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTime(dataBase);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String inicioSemana = sdfNav.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 6);
        String fimSemana = sdfNav.format(cal.getTime());
        tvDataNavegacao.setText(inicioSemana + " - " + fimSemana);

        ArrayList<Object> itensDaLista = new ArrayList<>();
        cal.setTime(dataBase);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        SimpleDateFormat sdfQuery = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        SimpleDateFormat sdfHeader = new SimpleDateFormat("EEEE, dd 'de' MMMM", Locale.getDefault());

        int totalAgendamentosSemana = 0;
        for (int i = 0; i < 7; i++) {
            String dataAtual = sdfQuery.format(cal.getTime());
            Cursor cursor = bancoHelper.listarAgendamentosPorData(dataAtual);
            if (cursor != null && cursor.getCount() > 0) {
                itensDaLista.add(sdfHeader.format(cal.getTime()).toUpperCase());
                cursor.moveToFirst();
                do {
                    long id = cursor.getLong(0);
                    String nome = cursor.getString(1);
                    String data = cursor.getString(2);
                    String inicio = cursor.getString(3);
                    String termino = cursor.getString(4);
                    boolean isInConflict = cursor.getInt(5) == 1;
                    itensDaLista.add(new Agendamento(id, nome, data, inicio, termino, isInConflict));
                    totalAgendamentosSemana++;
                } while (cursor.moveToNext());
                cursor.close();
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        SemanaAdapter semanaAdapter = new SemanaAdapter(this, itensDaLista);
        listViewHorarios.setAdapter(semanaAdapter);

        if (totalAgendamentosSemana == 0) {
            Toast.makeText(this, "Nenhum agendamento para esta semana.", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarAgendamentosDoDia(String data) {
        SimpleDateFormat sdfNav = new SimpleDateFormat("EEEE, dd 'de' MMMM", Locale.getDefault());
        tvDataNavegacao.setText(sdfNav.format(objetoDataSelecionada));

        ArrayList<Agendamento> listaAgendamentos = new ArrayList<>();
        Cursor cursor = bancoHelper.listarAgendamentosPorData(data);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String nome = cursor.getString(1);
                String inicio = cursor.getString(3);
                String termino = cursor.getString(4);
                boolean isInConflict = cursor.getInt(5) == 1;
                listaAgendamentos.add(new Agendamento(id, nome, data, inicio, termino, isInConflict));
            } while (cursor.moveToNext());
            cursor.close();
        }

        AgendamentoAdapter agendamentoAdapter = new AgendamentoAdapter(this, listaAgendamentos);
        listViewHorarios.setAdapter(agendamentoAdapter);

        if (listaAgendamentos.isEmpty()) {
            Toast.makeText(this, "Nenhum agendamento para este dia.", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirTelaDeAdicionar() {
        Intent intent = new Intent(this, AddAgendamentoActivity.class);
        intent.putExtra("DATA_SELECIONADA", new SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(objetoDataSelecionada));
        startActivity(intent);
    }

    private void mostrarPopupOpcoes(long idDoAgendamento) {
        new AlertDialog.Builder(this)
                .setTitle("O que deseja fazer?")
                .setPositiveButton("Editar", (dialog, which) -> {
                    Intent intent = new Intent(CalendarActivity.this, EditAgendamentoActivity.class);
                    intent.putExtra("AGENDAMENTO_ID", idDoAgendamento);
                    startActivity(intent);
                })
                .setNegativeButton("Excluir", (dialog, which) -> {
                    mostrarPopupConfirmacaoExclusao(idDoAgendamento);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void mostrarPopupConfirmacaoExclusao(long idParaExcluir) {
        Cursor cursor = bancoHelper.getAgendamentoPorId(idParaExcluir);
        if (cursor == null || !cursor.moveToFirst()) {
            Toast.makeText(this, "Agendamento não encontrado.", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
            return;
        }

        String nome = cursor.getString(1);
        String data = cursor.getString(2);
        String inicio = cursor.getString(3);
        String termino = cursor.getString(4);
        cursor.close();

        String detalhes = "Paciente: " + nome + "\nData: " + data + "\nHorário: " + inicio + " - " + termino;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão?")
                .setMessage("Tem certeza de que deseja excluir o seguinte agendamento?\n\n" + detalhes)
                .setPositiveButton("Sim, Excluir", (dialog, which) -> {
                    int resultado = bancoHelper.excluirAgendamento(idParaExcluir);
                    if (resultado > 0) {
                        bancoHelper.reavaliarConflitosDoDia(data);
                        Toast.makeText(this, "Agendamento excluído!", Toast.LENGTH_SHORT).show();
                        atualizarVisibilidadeEControles();
                    } else {
                        Toast.makeText(this, "Erro ao excluir!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarVisibilidadeEControles();
    }
}