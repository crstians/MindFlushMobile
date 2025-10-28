package com.example.mindflushagendamentos;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgendamentoRepository {

    private final CollectionReference agendamentosCollection;
    private final FirebaseFirestore db;

    public AgendamentoRepository() {
        db = FirebaseFirestore.getInstance();
        this.agendamentosCollection = db.collection("agendamentos");
    }

    public Task<DocumentSnapshot> getAgendamentoPorId(String documentId) {
        return agendamentosCollection.document(documentId).get();
    }
    
    public Task<Void> excluirAgendamento(String documentId) {
        return agendamentosCollection.document(documentId).delete();
    }

    public Task<QuerySnapshot> getAgendamentosPorData(String data) {
        return agendamentosCollection.whereEqualTo("data", data)
                .orderBy("horarioInicio")
                .get();
    }

    public Task<QuerySnapshot> getAgendamentosEntreDatas(LocalDate start, LocalDate end) {
        List<String> datasDaSemana = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            datasDaSemana.add(currentDate.format(formatter));
            currentDate = currentDate.plusDays(1);
        }

        if (datasDaSemana.isEmpty()) {
            return Tasks.forResult(null);
        }

        return agendamentosCollection.whereIn("data", datasDaSemana).get();
    }

    public Task<QuerySnapshot> getConflitos(String data, String inicio, String termino) {
        return agendamentosCollection.whereEqualTo("data", data).get();
    }

    public Task<Void> executarOperacaoBatch(Agendamento agendamentoParaSalvar, List<Agendamento> agendamentosParaAtualizar, List<Agendamento> agendamentosParaExcluir) {
        WriteBatch batch = db.batch();

        if (agendamentoParaSalvar != null) {
            DocumentReference docRef;
            if (agendamentoParaSalvar.getDocumentId() == null || agendamentoParaSalvar.getDocumentId().isEmpty()) {
                docRef = agendamentosCollection.document();
                agendamentoParaSalvar.setDocumentId(docRef.getId());
            } else {
                docRef = agendamentosCollection.document(agendamentoParaSalvar.getDocumentId());
            }
            batch.set(docRef, agendamentoParaSalvar);
        }

        if (agendamentosParaAtualizar != null && !agendamentosParaAtualizar.isEmpty()) {
            for (Agendamento ag : agendamentosParaAtualizar) {
                if (ag.getDocumentId() != null && !ag.getDocumentId().isEmpty()) {
                    DocumentReference docRef = agendamentosCollection.document(ag.getDocumentId());
                    batch.update(docRef, "inConflict", ag.isInConflict());
                }
            }
        }

        if (agendamentosParaExcluir != null && !agendamentosParaExcluir.isEmpty()) {
            for (Agendamento ag : agendamentosParaExcluir) {
                if (ag.getDocumentId() != null && !ag.getDocumentId().isEmpty()) {
                    DocumentReference docRef = agendamentosCollection.document(ag.getDocumentId());
                    batch.delete(docRef);
                }
            }
        }
        return batch.commit();
    }
    
    public Task<Void> resolverConflitosAposExclusao(Agendamento agendamentoParaExcluir, List<Agendamento> agendamentosParaAtualizar) {
        WriteBatch batch = db.batch();

        DocumentReference docParaExcluirRef = agendamentosCollection.document(agendamentoParaExcluir.getDocumentId());
        batch.delete(docParaExcluirRef);

        if (agendamentosParaAtualizar != null && !agendamentosParaAtualizar.isEmpty()) {
            for (Agendamento ag : agendamentosParaAtualizar) {
                DocumentReference docRef = agendamentosCollection.document(ag.getDocumentId());
                batch.update(docRef, "inConflict", ag.isInConflict());
            }
        }

        return batch.commit();
    }
}