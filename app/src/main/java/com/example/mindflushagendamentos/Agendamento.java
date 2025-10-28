package com.example.mindflushagendamentos;

import com.google.firebase.firestore.DocumentId;

public class Agendamento {

    @DocumentId
    private String documentId;
    private String nomePaciente;
    private String data;
    private String horarioInicio;
    private String horarioTermino;
    private boolean isInConflict;

    public Agendamento() {
    }

    public Agendamento(String nomePaciente, String data, String horarioInicio, String horarioTermino, boolean isInConflict) {
        this.nomePaciente = nomePaciente;
        this.data = data;
        this.horarioInicio = horarioInicio;
        this.horarioTermino = horarioTermino;
        this.isInConflict = isInConflict;
    }

    public String getDocumentId() {
        return documentId;
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

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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