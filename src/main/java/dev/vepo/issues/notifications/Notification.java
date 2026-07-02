package dev.vepo.issues.notifications;

import java.time.Instant;
import java.util.Objects;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "receive_id", nullable = false)
    private User receive;

    @ManyToOne
    @JoinColumn(name = "reffer_id", nullable = false)
    private Ticket reffer;

    @Column(columnDefinition = "VARCHAR(56)")
    private String type;

    @Column
    private String content;

    @Column(columnDefinition = "boolean default false")
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Notification(String type, User receive, Ticket reffer, String content) {
        this.type = type;
        this.receive = receive;
        this.reffer = reffer;
        this.content = content;
        read = false;
        createdAt = Instant.now();
    }

    public Notification() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getReceive() {
        return receive;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setReceive(User receive) {
        this.receive = receive;
    }

    public Ticket getReffer() {
        return reffer;
    }

    public void setReffer(Ticket reffer) {
        this.reffer = reffer;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        } else {
            return Objects.equals(this.id, ((Notification) obj).id);
        }
    }

    @Override
    public String toString() {
        return "Notification [id=%d, type=%s, receive=%s, reffer=%s, content=%s, read=%b, createdAt=%s]".formatted(id,
                                                                                                                   type,
                                                                                                                   receive,
                                                                                                                   reffer,
                                                                                                                   content,
                                                                                                                   read,
                                                                                                                   createdAt);
    }

}
