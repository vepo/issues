package dev.vepo.issues.project;

import dev.vepo.issues.user.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_project_members")
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id = new ProjectMemberId();

    @MapsId("projectId")
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @MapsId("userId")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ProjectMember() {}

    public ProjectMember(Project project, User user) {
        this.project = project;
        this.user = user;
        this.id = new ProjectMemberId(project.getId(), user.getId());
    }

    public ProjectMemberId getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }
}
