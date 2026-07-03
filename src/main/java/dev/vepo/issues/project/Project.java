package dev.vepo.issues.project;

import dev.vepo.issues.ticket.TicketPriority;
import dev.vepo.issues.workflow.Workflow;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_projects", uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "tb_project_UK", columnNames = "name"))
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(15)")
    private String prefix;

    @Column(name = "name", nullable = false, unique = true, columnDefinition = "VARCHAR(64)")
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "ticket_template_enabled", nullable = false)
    private boolean ticketTemplateEnabled;

    @Column(name = "ticket_template_title")
    private String ticketTemplateTitle;

    @Column(name = "ticket_template_description", columnDefinition = "text")
    private String ticketTemplateDescription;

    @Column(name = "ticket_template_category_id")
    private Long ticketTemplateCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_template_priority", length = 16)
    private TicketPriority ticketTemplatePriority;

    @Column(name = "phase_template_objective", columnDefinition = "TEXT")
    private String phaseTemplateObjective;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ProjectPhaseDeliverableTemplate> phaseDeliverableTemplates = new ArrayList<>();

    public Project() {}

    public Project(String prefix, String name, String description, Workflow workflow) {
        this.prefix = prefix;
        this.name = name;
        this.description = description;
        this.workflow = workflow;
        this.ticketTemplateEnabled = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public boolean isTicketTemplateEnabled() {
        return ticketTemplateEnabled;
    }

    public void setTicketTemplateEnabled(boolean ticketTemplateEnabled) {
        this.ticketTemplateEnabled = ticketTemplateEnabled;
    }

    public String getTicketTemplateTitle() {
        return ticketTemplateTitle;
    }

    public void setTicketTemplateTitle(String ticketTemplateTitle) {
        this.ticketTemplateTitle = ticketTemplateTitle;
    }

    public String getTicketTemplateDescription() {
        return ticketTemplateDescription;
    }

    public void setTicketTemplateDescription(String ticketTemplateDescription) {
        this.ticketTemplateDescription = ticketTemplateDescription;
    }

    public Long getTicketTemplateCategoryId() {
        return ticketTemplateCategoryId;
    }

    public void setTicketTemplateCategoryId(Long ticketTemplateCategoryId) {
        this.ticketTemplateCategoryId = ticketTemplateCategoryId;
    }

    public TicketPriority getTicketTemplatePriority() {
        return ticketTemplatePriority;
    }

    public void setTicketTemplatePriority(TicketPriority ticketTemplatePriority) {
        this.ticketTemplatePriority = ticketTemplatePriority;
    }

    public void clearTicketTemplate() {
        this.ticketTemplateEnabled = false;
        this.ticketTemplateTitle = null;
        this.ticketTemplateDescription = null;
        this.ticketTemplateCategoryId = null;
        this.ticketTemplatePriority = null;
    }

    public String getPhaseTemplateObjective() {
        return phaseTemplateObjective;
    }

    public void setPhaseTemplateObjective(String phaseTemplateObjective) {
        this.phaseTemplateObjective = phaseTemplateObjective;
    }

    public List<ProjectPhaseDeliverableTemplate> getPhaseDeliverableTemplates() {
        return phaseDeliverableTemplates;
    }

    public void setPhaseDeliverableTemplates(List<ProjectPhaseDeliverableTemplate> phaseDeliverableTemplates) {
        this.phaseDeliverableTemplates = phaseDeliverableTemplates;
    }
}
