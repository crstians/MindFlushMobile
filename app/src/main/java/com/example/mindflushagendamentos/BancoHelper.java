package com.example.mindflushagendamentos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class BancoHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mindflush.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_NAME = "agendamentos";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NOME_PACIENTE = "nome_paciente";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_HORARIO_INICIO = "horario_inicio";
    private static final String COLUMN_HORARIO_TERMINO = "horario_termino";
    private static final String COLUMN_IS_IN_CONFLICT = "is_in_conflict";


    public BancoHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NOME_PACIENTE + " TEXT, "
                + COLUMN_DATA + " TEXT, "
                + COLUMN_HORARIO_INICIO + " TEXT, "
                + COLUMN_HORARIO_TERMINO + " TEXT, "
                + COLUMN_IS_IN_CONFLICT + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long inserirAgendamento(String nome, String data, String inicio, String termino, boolean isInConflict) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOME_PACIENTE, nome);
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_HORARIO_INICIO, inicio);
        values.put(COLUMN_HORARIO_TERMINO, termino);
        values.put(COLUMN_IS_IN_CONFLICT, isInConflict ? 1 : 0);
        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public int atualizarAgendamento(long id, String nome, String data, String inicio, String termino, boolean isInConflict) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOME_PACIENTE, nome);
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_HORARIO_INICIO, inicio);
        values.put(COLUMN_HORARIO_TERMINO, termino);
        values.put(COLUMN_IS_IN_CONFLICT, isInConflict ? 1 : 0);
        return db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor listarAgendamentosPorData(String data) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATA + " = ? ORDER BY " + COLUMN_HORARIO_INICIO + " ASC", new String[]{data});
    }

    public int excluirAgendamento(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor getAgendamentoPorId(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void setConflitoStatus(long id, boolean isInConflict) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_IN_CONFLICT, isInConflict ? 1 : 0);
        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        // Deixamos o db aberto se este método for chamado dentro de um loop
    }

    public ArrayList<Agendamento> getConflitos(String data, String inicio, String termino, long idParaExcluir) {
        ArrayList<Agendamento> conflitos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_DATA + " = ? AND " +
                COLUMN_HORARIO_INICIO + " < ? AND " +
                COLUMN_HORARIO_TERMINO + " > ? AND " +
                COLUMN_ID + " != ?";
        String[] selectionArgs = new String[]{data, termino, inicio, String.valueOf(idParaExcluir)};

        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String nome = cursor.getString(1);
                String dataAg = cursor.getString(2);
                String inicioAg = cursor.getString(3);
                String terminoAg = cursor.getString(4);
                boolean isInConflict = cursor.getInt(5) == 1;
                conflitos.add(new Agendamento(id, nome, dataAg, inicioAg, terminoAg, isInConflict));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return conflitos;
    }

    // NOVO MÉTODO: Reavalia todos os agendamentos de um dia e atualiza seu status de conflito.
    public void reavaliarConflitosDoDia(String data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<Agendamento> agendamentosDoDia = new ArrayList<>();

        // 1. Pega todos os agendamentos do dia
        Cursor cursor = listarAgendamentosPorData(data);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String nome = cursor.getString(1);
                String dataAg = cursor.getString(2);
                String inicioAg = cursor.getString(3);
                String terminoAg = cursor.getString(4);
                boolean isInConflict = cursor.getInt(5) == 1;
                agendamentosDoDia.add(new Agendamento(id, nome, dataAg, inicioAg, terminoAg, isInConflict));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // 2. Para cada agendamento, verifica se ele tem conflito com qualquer outro no mesmo dia
        for (Agendamento agAtual : agendamentosDoDia) {
            boolean encontrouConflito = false;
            for (Agendamento agOutro : agendamentosDoDia) {
                if (agAtual.getId() == agOutro.getId()) {
                    continue; // Não comparar um agendamento com ele mesmo
                }
                // Lógica de conflito: (StartA < EndB) and (EndA > StartB)
                if (agAtual.getHorarioInicio().compareTo(agOutro.getHorarioTermino()) < 0 &&
                        agAtual.getHorarioTermino().compareTo(agOutro.getHorarioInicio()) > 0) {
                    encontrouConflito = true;
                    break; // Se encontrou um conflito, não precisa verificar os outros
                }
            }
            // 3. Atualiza o status de conflito no banco de dados
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_IN_CONFLICT, encontrouConflito ? 1 : 0);
            db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(agAtual.getId())});
        }
        db.close();
    }
}