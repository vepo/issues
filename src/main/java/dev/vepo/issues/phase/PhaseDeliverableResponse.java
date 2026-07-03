package dev.vepo.issues.phase;

public record PhaseDeliverableResponse(long id, int sortOrder, String text) {
    public static PhaseDeliverableResponse load(PhaseDeliverable deliverable) {
        return new PhaseDeliverableResponse(deliverable.getId(), deliverable.getSortOrder(), deliverable.getText());
    }
}
