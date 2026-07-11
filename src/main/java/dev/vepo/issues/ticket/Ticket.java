package dev.vepo.issues.ticket;

import static java.util.Collections.emptySet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.vepo.issues.categories.Category;
import dev.vepo.issues.phase.Phase;
import dev.vepo.issues.phase.Version;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.user.User;
import dev.vepo.issues.workflow.WorkflowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_tickets")
@SuppressWarnings("java:S107")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, columnDefinition = "VARCHAR(25)")
    private String identifier;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private WorkflowStatus status;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "assignee_id", nullable = true)
    private User assignee;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tb_tickets_subscribers", joinColumns = @JoinColumn(name = "ticket_id"), inverseJoinColumns = @JoinColumn(name = "subscriber_id"))
    private Set<User> subscribers;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observed_version_id")
    private Version observedVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_version_id")
    private Version targetVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 16)
    private TicketType ticketType = TicketType.TASK;

    @Column(name = "backlog_rank", nullable = false)
    private int backlogRank = 0;

    public Ticket() {}

    public Ticket(String identifier, String title, String description, Category category, User author, User assignee, Project project, WorkflowStatus status) {
        this.identifier = identifier;
        this.title = title;
        this.description = description;
        this.category = category;
        this.author = author;
        this.assignee = assignee;
        this.project = project;
        this.status = status;
        this.priority = TicketPriority.MEDIUM;
        this.ticketType = TicketType.TASK;
        this.backlogRank = 0;
        this.createdAt = this.updatedAt = LocalDateTime.now();
        this.deleted = false;
        this.subscribers = emptySet();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Version getObservedVersion() {
        return observedVersion;
    }

    public void setObservedVersion(Version observedVersion) {
        this.observedVersion = observedVersion;
    }

    public Version getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(Version targetVersion) {
        this.targetVersion = targetVersion;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public int getBacklogRank() {
        return backlogRank;
    }

    public void setBacklogRank(int backlogRank) {
        this.backlogRank = backlogRank;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Set<User> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<User> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (Objects.nonNull(other) && other instanceof Ticket otherTicket) {
            return Objects.equals(id, otherTicket.id);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Ticket [id=%d, title=%s, description=%s, createdAt=%s, updatedAt=%s, category=%s, status=%s, author=%s, assignee=%s, subscribers=%s, project=%s, deleted=%b]".formatted(id,
                                                                                                                                                                                        title,
                                                                                                                                                                                        deleted,
                                                                                                                                                                                        createdAt,
                                                                                                                                                                                        updatedAt,
                                                                                                                                                                                        category,
                                                                                                                                                                                        status,
                                                                                                                                                                                        author,
                                                                                                                                                                                        assignee,
                                                                                                                                                                                        subscribers,
                                                                                                                                                                                        project,
                                                                                                                                                                                        deleted);
    }
}