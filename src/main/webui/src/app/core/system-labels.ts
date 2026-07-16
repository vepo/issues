/**
 * Product-owned display strings for enums and fixed UI copy (source locale: pt).
 */
export function priorityLabel(priority: string | null | undefined): string {
  switch ((priority ?? 'MEDIUM').toUpperCase()) {
    case 'CRITICAL':
      return $localize`:@@system.priority.critical:Crítica`;
    case 'HIGH':
      return $localize`:@@system.priority.high:Alta`;
    case 'LOW':
      return $localize`:@@system.priority.low:Baixa`;
    default:
      return $localize`:@@system.priority.medium:Média`;
  }
}

export function phaseStatusLabel(status: 'PLANNED' | 'ACTIVE' | 'COMPLETED' | string): string {
  switch (status) {
    case 'PLANNED':
      return $localize`:@@system.phase.planned:Planejada`;
    case 'ACTIVE':
      return $localize`:@@system.phase.active:Ativa`;
    case 'COMPLETED':
      return $localize`:@@system.phase.completed:Concluída`;
    default:
      return status;
  }
}

export type SystemTicketType = 'EPIC' | 'STORY' | 'TASK';

export function ticketTypeLabel(type: SystemTicketType | string | null | undefined): string {
  switch (type) {
    case 'EPIC':
      return $localize`:@@system.ticketType.epic:Épico`;
    case 'STORY':
      return $localize`:@@system.ticketType.story:História`;
    case 'TASK':
    default:
      return $localize`:@@system.ticketType.task:Tarefa`;
  }
}

export const TICKET_TYPE_OPTIONS: { value: SystemTicketType; label: string }[] = [
  { value: 'EPIC', label: ticketTypeLabel('EPIC') },
  { value: 'STORY', label: ticketTypeLabel('STORY') },
  { value: 'TASK', label: ticketTypeLabel('TASK') },
];

export type SystemPeerLinkType = 'BLOCKS' | 'RELATES_TO' | 'DUPLICATES' | 'DERIVED_FROM' | 'REMAINING_WORK_OF';

export function peerLinkTypeLabel(type: SystemPeerLinkType | string): string {
  switch (type) {
    case 'BLOCKS':
      return $localize`:@@system.link.blocks:Bloqueia`;
    case 'RELATES_TO':
      return $localize`:@@system.link.relates:Relacionado a`;
    case 'DUPLICATES':
      return $localize`:@@system.link.duplicates:Duplicata de`;
    case 'DERIVED_FROM':
      return $localize`:@@system.link.derived:Derivado de`;
    case 'REMAINING_WORK_OF':
      return $localize`:@@system.link.remaining:Trabalho restante de`;
    default:
      return type;
  }
}

export const PEER_LINK_TYPE_OPTIONS: { value: SystemPeerLinkType; label: string }[] = [
  { value: 'BLOCKS', label: peerLinkTypeLabel('BLOCKS') },
  { value: 'RELATES_TO', label: peerLinkTypeLabel('RELATES_TO') },
  { value: 'DUPLICATES', label: peerLinkTypeLabel('DUPLICATES') },
  { value: 'DERIVED_FROM', label: peerLinkTypeLabel('DERIVED_FROM') },
  { value: 'REMAINING_WORK_OF', label: peerLinkTypeLabel('REMAINING_WORK_OF') },
];
