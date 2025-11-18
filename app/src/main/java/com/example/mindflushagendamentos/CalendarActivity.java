package com.example.mindflushagendamentos;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarActivity extends AppCompatActivity implements DayAdapter.OnItemListener {

    private enum ViewMode { MONTH, WEEK, DAY }
    private ViewMode currentViewMode = ViewMode.MONTH;

    private CalendarView calendarView;
    private ListView listViewHorarios;
    private FloatingActionButton btnAddAgendamento;
    private Button btnViewMes, btnViewSemana, btnViewDia;
    private RelativeLayout navigationControls;
    private ImageButton btnAnterior, btnProximo;
    private TextView tvDataNavegacao;
    private ProgressBar progressBar;
    private RecyclerView rvDays;
    private DayAdapter dayAdapter;

    private AgendamentoRepository agendamentoRepository;
    private LocalDate selectedDate;
    private FirebaseAuth mAuth;


    private ActivityResultLauncher<Intent> agendamentoActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        mAuth = FirebaseAuth.getInstance();

        agendamentoRepository = new AgendamentoRepository();
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
        progressBar = findViewById(R.id.progressBarCalendar);
        rvDays = findViewById(R.id.rvDays);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvDays);

        agendamentoActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        atualizarDadosDaView();
                    }
                });

        selectedDate = LocalDate.now();
        calendarView.setDate(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

        configurarListeners();
        atualizarVisibilidadeEControles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calendar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_today) {
            selectedDate = LocalDate.now();
            calendarView.setDate(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), true, true);
            atualizarDadosDaView();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void logout() {
        mAuth.signOut();
        
        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(CalendarActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void configurarListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            atualizarDadosDaView();
        });

        btnViewMes.setOnClickListener(v -> { currentViewMode = ViewMode.MONTH; atualizarVisibilidadeEControles(); });
        btnViewDia.setOnClickListener(v -> { currentViewMode = ViewMode.DAY; atualizarVisibilidadeEControles(); });
        btnViewSemana.setOnClickListener(v -> { currentViewMode = ViewMode.WEEK; atualizarVisibilidadeEControles(); });

        btnAnterior.setOnClickListener(v -> navegarData(-1));
        btnProximo.setOnClickListener(v -> navegarData(1));

        btnAddAgendamento.setOnClickListener(v -> abrirTelaDeAdicionar());

        listViewHorarios.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            Agendamento agendamento = null;
            if (item instanceof Agendamento) {
                agendamento = (Agendamento) item;
            } else if (item instanceof AgendamentoItem) {
                agendamento = ((AgendamentoItem) item).getAgendamento();
            }
            if (agendamento != null) {
                mostrarPopupOpcoes(agendamento);
            }
        });
    }
    
    private void navegarData(int direcao) {
        if (currentViewMode == ViewMode.DAY) {
            selectedDate = selectedDate.plusDays(direcao);
        } else if (currentViewMode == ViewMode.WEEK) {
            selectedDate = selectedDate.plusWeeks(direcao);
        }
        calendarView.setDate(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), true, true);
        atualizarDadosDaView();
    }

    private void atualizarVisibilidadeEControles() {
        btnViewMes.setSelected(currentViewMode == ViewMode.MONTH);
        btnViewSemana.setSelected(currentViewMode == ViewMode.WEEK);
        btnViewDia.setSelected(currentViewMode == ViewMode.DAY);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) listViewHorarios.getLayoutParams();
        
        switch (currentViewMode) {
            case MONTH:
                navigationControls.setVisibility(View.GONE);
                calendarView.setVisibility(View.VISIBLE);
                rvDays.setVisibility(View.GONE);
                params.addRule(RelativeLayout.BELOW, R.id.calendarView);
                atualizarDadosDaView();
                break;
            case DAY:
                navigationControls.setVisibility(View.VISIBLE);
                calendarView.setVisibility(View.GONE);
                rvDays.setVisibility(View.VISIBLE);
                params.addRule(RelativeLayout.BELOW, R.id.rvDays);
                setupDayViewSlidingWindow();
                break;
            case WEEK:
                navigationControls.setVisibility(View.VISIBLE);
                calendarView.setVisibility(View.GONE);
                rvDays.setVisibility(View.GONE);
                params.addRule(RelativeLayout.BELOW, R.id.navigation_controls);
                atualizarDadosDaView();
                break;
        }
        listViewHorarios.setLayoutParams(params);
    }
    
    private void atualizarDadosDaView() {
        if (mAuth.getCurrentUser() == null) {
            // Se o usuário for nulo, não tente carregar dados. Pode redirecionar para login.
            Intent intent = new Intent(CalendarActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (currentViewMode == ViewMode.MONTH) {
            carregarAgendamentos(selectedDate);
        } else if (currentViewMode == ViewMode.DAY) {
            setupDayViewSlidingWindow();
        } else if (currentViewMode == ViewMode.WEEK) {
            LocalDate startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            DateTimeFormatter rangeFormatter = DateTimeFormatter.ofPattern("dd/MM");
            String rangeText = startOfWeek.format(rangeFormatter) + " - " + endOfWeek.format(rangeFormatter);
            tvDataNavegacao.setText(rangeText);
            carregarAgendamentosDaSemana(startOfWeek, endOfWeek);
        }
    }

    private void setupDayViewSlidingWindow() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int itemWidth = (int) (56 * displayMetrics.density);
        int padding = (screenWidth / 2) - (itemWidth / 2);
        rvDays.setPadding(padding, 0, padding, 0);

        ArrayList<LocalDate> days = new ArrayList<>();
        for (int i = -10; i <= 10; i++) { 
            days.add(selectedDate.plusDays(i));
        }

        dayAdapter = new DayAdapter(days, this);
        rvDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDays.setAdapter(dayAdapter);
        
        int selectedPos = days.indexOf(selectedDate);
        if(selectedPos != -1) {
            ((LinearLayoutManager) rvDays.getLayoutManager()).scrollToPositionWithOffset(selectedPos, 0);
            updateDaySpecificUI(selectedDate, selectedPos);
        }
    }

    @Override
    public void onItemClick(int position, LocalDate date) {
        if (currentViewMode == ViewMode.DAY) {
            if (!date.equals(selectedDate)) {
                selectedDate = date;
                setupDayViewSlidingWindow();
            }
        }
    }
    
    private void updateDaySpecificUI(LocalDate date, int position) {
        dayAdapter.setSelectedPosition(position);
        DateTimeFormatter navFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", new Locale("pt", "BR"));
        tvDataNavegacao.setText(date.format(navFormatter));
        carregarAgendamentos(date);
    }

    private void carregarAgendamentos(LocalDate date) {
        setLoading(true);
        DateTimeFormatter firestoreFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
        String dataFormatada = date.format(firestoreFormatter);

        agendamentoRepository.getAgendamentosPorData(dataFormatada)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Agendamento> listaAgendamentos = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            listaAgendamentos.add(document.toObject(Agendamento.class));
                        }
                    }
                    listViewHorarios.setAdapter(new AgendamentoAdapter(this, listaAgendamentos));
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Erro ao carregar agendamentos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarAgendamentosDaSemana(LocalDate start, LocalDate end) {
        setLoading(true);
        agendamentoRepository.getAgendamentosEntreDatas(start, end)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Map<LocalDate, List<Agendamento>> agendamentosPorDia = new LinkedHashMap<>();
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Agendamento ag = document.toObject(Agendamento.class);
                        try {
                            LocalDate data = LocalDate.parse(ag.getData(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            if (!agendamentosPorDia.containsKey(data)) {
                                agendamentosPorDia.put(data, new ArrayList<>());
                            }
                            agendamentosPorDia.get(data).add(ag);
                        } catch (Exception e) { /* Ignora */ }
                    }
                }

                ArrayList<ListItem> items = new ArrayList<>();
                DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM", new Locale("pt", "BR"));
                
                List<LocalDate> diasOrdenados = new ArrayList<>(agendamentosPorDia.keySet());
                diasOrdenados.sort(Comparator.naturalOrder());

                for (LocalDate dia : diasOrdenados) {
                    String headerText = dia.format(headerFormatter);
                    headerText = headerText.substring(0, 1).toUpperCase() + headerText.substring(1);
                    items.add(new HeaderItem(headerText));
                    
                    List<Agendamento> ags = agendamentosPorDia.get(dia);
                    ags.sort(Comparator.comparing(Agendamento::getHorarioInicio));
                    for (Agendamento ag : ags) {
                        items.add(new AgendamentoItem(ag));
                    }
                }
                listViewHorarios.setAdapter(new WeeklyAgendamentoAdapter(this, items));
                setLoading(false);
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Erro ao carregar agendamentos da semana: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void abrirTelaDeAdicionar() {
        Intent intent = new Intent(this, AddAgendamentoActivity.class);
        String dataFormatada = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()));
        intent.putExtra("DATA_SELECIONADA", dataFormatada);
        agendamentoActivityResultLauncher.launch(intent);
    }
    
    private void abrirTelaDeEditar(Agendamento agendamento) {
        Intent intent = new Intent(this, EditAgendamentoActivity.class);
        intent.putExtra("AGENDAMENTO_DOC_ID", agendamento.getDocumentId());
        agendamentoActivityResultLauncher.launch(intent);
    }

    private void mostrarPopupOpcoes(Agendamento agendamento) {
        new AlertDialog.Builder(this)
                .setTitle("O que deseja fazer?")
                .setPositiveButton("Editar", (dialog, which) -> abrirTelaDeEditar(agendamento))
                .setNegativeButton("Excluir", (dialog, which) -> mostrarPopupConfirmacaoExclusao(agendamento))
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void mostrarPopupConfirmacaoExclusao(Agendamento agendamento) {
        String detalhes = "Paciente: " + agendamento.getNomePaciente() + "\nData: " + agendamento.getData() + "\nHorário: " + agendamento.getHorarioInicio() + " - " + agendamento.getHorarioTermino();
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão?")
                .setMessage("Deseja realmente excluir este agendamento?\n\n" + detalhes)
                .setPositiveButton("Sim, Excluir", (dialog, which) -> excluirAgendamentoEVerificarConflitos(agendamento))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirAgendamentoEVerificarConflitos(final Agendamento agendamentoParaExcluir) {
        setLoading(true);
        if (!agendamentoParaExcluir.isInConflict()) {
            agendamentoRepository.excluirAgendamento(agendamentoParaExcluir.getDocumentId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Agendamento excluído!", Toast.LENGTH_SHORT).show();
                    atualizarDadosDaView();
                })
                .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
            return;
        }

        agendamentoRepository.getAgendamentosPorData(agendamentoParaExcluir.getData())
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Agendamento> todosAgendamentosDoDia = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) { todosAgendamentosDoDia.add(document.toObject(Agendamento.class)); }

                List<Agendamento> agendamentosParaAtualizar = new ArrayList<>();
                List<Agendamento> conflitosOriginais = todosAgendamentosDoDia.stream()
                    .filter(ag -> !ag.getDocumentId().equals(agendamentoParaExcluir.getDocumentId()) && ag.isInConflict() && isSobreposto(ag, agendamentoParaExcluir))
                    .collect(Collectors.toList());

                for (Agendamento orfao : conflitosOriginais) {
                    boolean aindaTemConflito = todosAgendamentosDoDia.stream()
                        .anyMatch(outroAg -> !outroAg.getDocumentId().equals(agendamentoParaExcluir.getDocumentId()) && !outroAg.getDocumentId().equals(orfao.getDocumentId()) && isSobreposto(orfao, outroAg));
                    
                    if (!aindaTemConflito) {
                        orfao.setInConflict(false);
                        agendamentosParaAtualizar.add(orfao);
                    }
                }
                agendamentoRepository.resolverConflitosAposExclusao(agendamentoParaExcluir, agendamentosParaAtualizar)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Agendamento excluído e conflitos resolvidos.", Toast.LENGTH_SHORT).show();
                        atualizarDadosDaView();
                    })
                    .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, "Erro na operação em lote: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
            })
            .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, "Erro ao buscar agendamentos: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
    }

    private boolean isSobreposto(Agendamento ag1, Agendamento ag2) {
        return ag1.getHorarioInicio().compareTo(ag2.getHorarioTermino()) < 0 &&
               ag1.getHorarioTermino().compareTo(ag2.getHorarioInicio()) > 0;
    }
    
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            listViewHorarios.setVisibility(loading ? View.GONE : View.VISIBLE);
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        atualizarDadosDaView();
    }
}