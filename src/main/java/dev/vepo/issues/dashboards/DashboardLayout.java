package dev.vepo.issues.dashboards;

import java.time.Instant;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_dashboard_layouts", uniqueConstraints = @UniqueConstraint(name = "tb_dashboard_layouts_user_project_UK", columnNames = { "user_id", "project_id" }))
public class DashboardLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String widgets;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DashboardLayout() {}

    public DashboardLayout(User user, Project project, String widgets) {
        this.user = user;
        this.project = project;
        this.widgets = widgets;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getWidgets() {
        return widgets;
    }

    public void setWidgets(String widgets) {
        this.widgets = widgets;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
