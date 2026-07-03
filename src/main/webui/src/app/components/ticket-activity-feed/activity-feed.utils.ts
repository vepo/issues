import { CommentResponse } from '../../generated/model/commentResponse';
import { TicketHistoryResponse } from '../../generated/model/ticketHistoryResponse';

export type ActivityFilter = 'all' | 'comments' | 'changes';

export type ActivityComment = {
  kind: 'comment';
  id: number;
  timestamp: number;
  userName: string;
  content: string;
};

export type ActivityChange = {
  kind: 'change';
  id: number;
  timestamp: number;
  userName: string;
  action: string;
  field?: string;
  oldValue?: string;
  newValue?: string;
};

export type ActivityItem = ActivityComment | ActivityChange;

const FIELD_LABELS: Record<string, string> = {
  title: 'título',
  description: 'descrição',
  category: 'categoria',
  status: 'status',
  assignee: 'responsável',
  subscriber: 'observador',
};

export function buildActivityFeed(history: ReadonlyArray<TicketHistoryResponse>, comments: ReadonlyArray<CommentResponse>): ActivityItem[] {
  const changes: ActivityChange[] = (history ?? []).map(entry => ({
    kind: 'change',
    id: entry.id ?? 0,
    timestamp: entry.timestamp ?? 0,
    userName: entry.user?.name ?? '—',
    action: entry.action ?? '',
    field: entry.field,
    oldValue: entry.oldValue,
    newValue: entry.newValue,
  }));

  const commentItems: ActivityComment[] = (comments ?? []).map(comment => ({
    kind: 'comment',
    id: comment.id ?? 0,
    timestamp: comment.createdAt ?? 0,
    userName: comment.author?.name ?? '—',
    content: comment.content ?? '',
  }));

  return [...changes, ...commentItems].sort((a, b) => {
    if (b.timestamp !== a.timestamp) {
      return b.timestamp - a.timestamp;
    }
    return b.id - a.id;
  });
}

export function filterActivity(items: ActivityItem[], filter: ActivityFilter): ActivityItem[] {
  if (filter === 'comments') {
    return items.filter(item => item.kind === 'comment');
  }
  if (filter === 'changes') {
    return items.filter(item => item.kind === 'change');
  }
  return items;
}

export function activityIcon(item: ActivityItem): string {
  if (item.kind === 'comment') {
    return 'chat';
  }
  switch (item.action) {
    case 'CREATED':
      return 'add_circle_outline';
    case 'FIELD_CHANGED':
      return 'edit';
    case 'STATUS_CHANGED':
      return 'swap_horiz';
    case 'ASSIGNEE_CHANGED':
      return 'person';
    case 'SUBSCRIBED':
    case 'UNSUBSCRIBED':
      return 'notifications';
    case 'DELETED':
      return 'delete_outline';
    default:
      return 'history';
  }
}

export function activitySummary(item: ActivityItem): string {
  if (item.kind === 'comment') {
    return 'comentou';
  }
  switch (item.action) {
    case 'CREATED':
      return 'criou o ticket';
    case 'FIELD_CHANGED':
      return `alterou o ${FIELD_LABELS[item.field ?? ''] ?? item.field ?? 'campo'}`;
    case 'STATUS_CHANGED':
      return 'alterou o status';
    case 'ASSIGNEE_CHANGED':
      if (!item.oldValue && item.newValue) {
        return `atribuiu para ${item.newValue}`;
      }
      if (item.oldValue && !item.newValue) {
        return 'removeu o responsável';
      }
      return 'alterou o responsável';
    case 'SUBSCRIBED':
      return item.newValue ? `${item.newValue} começou a observar` : 'começou a observar';
    case 'UNSUBSCRIBED':
      return item.oldValue ? `${item.oldValue} deixou de observar` : 'deixou de observar';
    case 'DELETED':
      return 'excluiu o ticket';
    default:
      return 'registrou uma alteração';
  }
}

export function hasValueChange(item: ActivityItem): boolean {
  return item.kind === 'change'
    && (Boolean(item.oldValue) || Boolean(item.newValue))
    && item.action !== 'CREATED'
    && item.action !== 'DELETED';
}

export function trackActivityItem(item: ActivityItem): string {
  return `${item.kind}-${item.id}`;
}
