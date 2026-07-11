package dev.vepo.issues.ticket.link;

public enum TicketLinkType {
    BLOCKS("Bloqueia", "Bloqueado por"),
    RELATES_TO("Relacionado a", "Relacionado a"),
    DUPLICATES("Duplicata de", "Duplicado por"),
    DERIVED_FROM("Derivado de", "Origem de"),
    REMAINING_WORK_OF("Trabalho restante de", "Tem trabalho restante"),
    CHILD_OF("Filho de", "Pai de");

    private final String outboundLabel;
    private final String inboundLabel;

    TicketLinkType(String outboundLabel, String inboundLabel) {
        this.outboundLabel = outboundLabel;
        this.inboundLabel = inboundLabel;
    }

    public String displayLabel(boolean outbound) {
        return outbound ? outboundLabel : inboundLabel;
    }
}
