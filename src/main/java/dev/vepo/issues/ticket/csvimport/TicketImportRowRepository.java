package dev.vepo.issues.ticket.csvimport;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class TicketImportRowRepository {

    @PersistenceContext
    private EntityManager em;

    public List<TicketImportRow> findByImportId(long importId) {
        return em.createQuery("FROM TicketImportRow WHERE ticketImport.id = :importId ORDER BY rowNumber", TicketImportRow.class)
                 .setParameter("importId", importId)
                 .getResultList();
    }

    public Optional<TicketImportRow> findByIdAndImportId(long rowId, long importId) {
        return em.createQuery("FROM TicketImportRow WHERE id = :rowId AND ticketImport.id = :importId", TicketImportRow.class)
                 .setParameter("rowId", rowId)
                 .setParameter("importId", importId)
                 .getResultStream()
                 .findFirst();
    }
}
