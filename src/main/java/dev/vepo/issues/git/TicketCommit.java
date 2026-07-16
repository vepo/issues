package dev.vepo.issues.git;

import java.time.Instant;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_ticket_commits", uniqueConstraints = @UniqueConstraint(name = "tb_ticket_commits_ticket_sha_UK", columnNames = { "ticket_id", "sha" }))
public class TicketCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 64)
    private String sha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_user_id")
    private User matchedUser;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "commit_url", length = 1024)
    private String commitUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketCommit() {}

    public TicketCommit(Ticket ticket,
                        Project project,
                        String sha,
                        String message,
                        String authorName,
                        String authorEmail,
                        User matchedUser,
                        Instant committedAt,
                        String commitUrl) {
        this.ticket = ticket;
        this.project = project;
        this.sha = sha;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.matchedUser = matchedUser;
        this.committedAt = committedAt;
        this.commitUrl = commitUrl;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Project getProject() {
        return project;
    }

    public String getSha() {
        return sha;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public User getMatchedUser() {
        return matchedUser;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
