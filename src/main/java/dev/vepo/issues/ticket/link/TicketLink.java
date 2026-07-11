package dev.vepo.issues.ticket.link;

import java.time.Instant;
import java.util.Objects;

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
@Table(name = "tb_ticket_links")
public class TicketLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_ticket_id", nullable = false)
    private Ticket source;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_ticket_id", nullable = false)
    private Ticket target;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 32)
    private TicketLinkType linkType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    public TicketLink() {}

    public TicketLink(Ticket source, Ticket target, TicketLinkType linkType, User createdBy) {
        this.source = source;
        this.target = target;
        this.linkType = linkType;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getSource() {
        return source;
    }

    public void setSource(Ticket source) {
        this.source = source;
    }

    public Ticket getTarget() {
        return target;
    }

    public void setTarget(Ticket target) {
        this.target = target;
    }

    public TicketLinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(TicketLinkType linkType) {
        this.linkType = linkType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof TicketLink otherLink) {
            return Objects.equals(id, otherLink.id);
        }
        return false;
    }
}
