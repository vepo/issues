package dev.vepo.issues.project;

import java.util.List;

public record PhaseTemplateResponse(String objective, List<String> deliverables) {

    public static PhaseTemplateResponse load(Project project) {
        return new PhaseTemplateResponse(project.getPhaseTemplateObjective(),
                                         project.getPhaseDeliverableTemplates()
                                                .stream()
                                                .map(ProjectPhaseDeliverableTemplate::getText)
                                                .toList());
    }
}
