import { translate } from '@jsverse/transloco';

const PORTUGUESE_SYSTEM_LABELS = {
  'system.priority.critical': 'Crítica',
  'system.priority.high': 'Alta',
  'system.priority.medium': 'Média',
  'system.priority.low': 'Baixa',
  'system.phase.planned': 'Planejada',
  'system.phase.active': 'Ativa',
  'system.phase.completed': 'Concluída',
  'system.ticketType.epic': 'Épico',
  'system.ticketType.story': 'História',
  'system.ticketType.task': 'Tarefa',
  'system.link.blocks': 'Bloqueia',
  'system.link.relates': 'Relacionado a',
  'system.link.duplicates': 'Duplicata de',
  'system.link.derived': 'Derivado de',
  'system.link.remaining': 'Trabalho restante de',
} as const;

type SystemLabelKey = keyof typeof PORTUGUESE_SYSTEM_LABELS;

function systemLabel(key: SystemLabelKey): string {
  try {
    const translated = translate<string>(key);
    return translated === key ? PORTUGUESE_SYSTEM_LABELS[key] : translated;
  } catch {
    return PORTUGUESE_SYSTEM_LABELS[key];
  }
}

/**
 * Product-owned display strings for enums and fixed UI copy (source locale: pt).
 */
export function priorityLabel(priority: string | null | undefined): string {
  switch ((priority ?? 'MEDIUM').toUpperCase()) {
    case 'CRITICAL':
      return systemLabel('system.priority.critical');
    case 'HIGH':
      return systemLabel('system.priority.high');
    case 'LOW':
      return systemLabel('system.priority.low');
    default:
      return systemLabel('system.priority.medium');
  }
}

export function phaseStatusLabel(status: string): string {
  switch (status) {
    case 'PLANNED':
      return systemLabel('system.phase.planned');
    case 'ACTIVE':
      return systemLabel('system.phase.active');
    case 'COMPLETED':
      return systemLabel('system.phase.completed');
    default:
      return status;
  }
}

export type SystemTicketType = 'EPIC' | 'STORY' | 'TASK';

export function ticketTypeLabel(type: string | null | undefined): string {
  switch (type) {
    case 'EPIC':
      return systemLabel('system.ticketType.epic');
    case 'STORY':
      return systemLabel('system.ticketType.story');
    case 'TASK':
    default:
      return systemLabel('system.ticketType.task');
  }
}

export const TICKET_TYPE_OPTIONS: { value: SystemTicketType; readonly label: string }[] = [
  { value: 'EPIC', get label() { return ticketTypeLabel('EPIC'); } },
  { value: 'STORY', get label() { return ticketTypeLabel('STORY'); } },
  { value: 'TASK', get label() { return ticketTypeLabel('TASK'); } },
];

export type SystemPeerLinkType = 'BLOCKS' | 'RELATES_TO' | 'DUPLICATES' | 'DERIVED_FROM' | 'REMAINING_WORK_OF';

export function peerLinkTypeLabel(type: string): string {
  switch (type) {
    case 'BLOCKS':
      return systemLabel('system.link.blocks');
    case 'RELATES_TO':
      return systemLabel('system.link.relates');
    case 'DUPLICATES':
      return systemLabel('system.link.duplicates');
    case 'DERIVED_FROM':
      return systemLabel('system.link.derived');
    case 'REMAINING_WORK_OF':
      return systemLabel('system.link.remaining');
    default:
      return type;
  }
}

export const PEER_LINK_TYPE_OPTIONS: { value: SystemPeerLinkType; readonly label: string }[] = [
  { value: 'BLOCKS', get label() { return peerLinkTypeLabel('BLOCKS'); } },
  { value: 'RELATES_TO', get label() { return peerLinkTypeLabel('RELATES_TO'); } },
  { value: 'DUPLICATES', get label() { return peerLinkTypeLabel('DUPLICATES'); } },
  { value: 'DERIVED_FROM', get label() { return peerLinkTypeLabel('DERIVED_FROM'); } },
  { value: 'REMAINING_WORK_OF', get label() { return peerLinkTypeLabel('REMAINING_WORK_OF'); } },
];
