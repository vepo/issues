package dev.vepo.issues.ticket.history;

import java.time.Instant;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ticket_history")
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    public Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public TicketHistoryAction action;

    @Column(length = 64)
    public String field;

    @Column(name = "old_value", columnDefinition = "TEXT")
    public String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    public String newValue;

    @Column(name = "reference_id")
    public Long referenceId;

    @Column(nullable = false)
    public Instant timestamp;

    @Column(name = "via_agent", nullable = false)
    public boolean viaAgent;

    public TicketHistory() {}

    public TicketHistory(Ticket ticket,
                         User user,
                         TicketHistoryAction action,
                         String field,
                         String oldValue,
                         String newValue,
                         Long referenceId,
                         Instant timestamp,
                         boolean viaAgent) {
        this.ticket = ticket;
        this.user = user;
        this.action = action;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.referenceId = referenceId;
        this.timestamp = timestamp;
        this.viaAgent = viaAgent;
    }
}
