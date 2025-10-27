package com.example.mindflushagendamentos;

public class Agendamento {

    private long id;
    private String nomePaciente;
    private String data;
    private String horarioInicio;
    private String horarioTermino;
    private boolean isInConflict;

    public Agendamento(long id, String nomePaciente, String data, String horarioInicio, String horarioTermino, boolean isInConflict) {
        this.id = id;
        this.nomePaciente = nomePaciente;
        this.data = data;
        this.horarioInicio = horarioInicio;
        this.horarioTermino = horarioTermino;
        this.isInConflict = isInConflict;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getNomePaciente() {
        return nomePaciente;
    }

    public String getData() {
        return data;
    }

    public String getHorarioInicio() {
        return horarioInicio;
    }

    public String getHorarioTermino() {
        return horarioTermino;
    }

    public boolean isInConflict() {
        return isInConflict;
    }

    // Setters (não são estritamente necessários para esta fase, mas é uma boa prática tê-los)
    public void setId(long id) {
        this.id = id;
    }

    public void setNomePaciente(String nomePaciente) {
        this.nomePaciente = nomePaciente;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setHorarioInicio(String horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public void setHorarioTermino(String horarioTermino) {
        this.horarioTermino = horarioTermino;
    }

    public void setInConflict(boolean inConflict) {
        isInConflict = inConflict;
    }
}