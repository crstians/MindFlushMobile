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
        db.close();
    }

    // MUDANÇA: O antigo 'verificarConflito' foi substituído por 'getConflitos'
    // Este novo método retorna uma LISTA de todos os agendamentos conflitantes.
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
}