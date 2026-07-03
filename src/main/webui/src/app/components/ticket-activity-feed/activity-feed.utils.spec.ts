import {
  ActivityChange,
  ActivityComment,
  activityIcon,
  activitySummary,
  buildActivityFeed,
  filterActivity,
  hasValueChange,
} from './activity-feed.utils';

describe('activity-feed.utils', () => {
  const history = [
    {
      id: 2,
      action: 'STATUS_CHANGED',
      field: 'status',
      oldValue: 'Todo',
      newValue: 'In progress',
      timestamp: 2000,
      user: { name: 'Alice' },
    },
    {
      id: 1,
      action: 'CREATED',
      timestamp: 1000,
      user: { name: 'Alice' },
    },
  ];

  const comments = [
    {
      id: 10,
      content: '<p>Hello</p>',
      createdAt: 3000,
      author: { name: 'Bob' },
    },
  ];

  it('should merge and sort activity newest first', () => {
    const feed = buildActivityFeed(history as never, comments as never);
    expect(feed[0].kind).toBe('comment');
    expect(feed[0].id).toBe(10);
    expect(feed[1].kind).toBe('change');
    if (feed[1].kind === 'change') {
      expect(feed[1].action).toBe('STATUS_CHANGED');
    }
  });

  it('should filter comments and changes', () => {
    const feed = buildActivityFeed(history as never, comments as never);
    expect(filterActivity(feed, 'comments')).toHaveSize(1);
    expect(filterActivity(feed, 'changes')).toHaveSize(2);
    expect(filterActivity(feed, 'all')).toHaveSize(3);
  });

  it('should map icons and summaries', () => {
    const change = feedChange('FIELD_CHANGED', 'title');
    const comment = feedComment();
    expect(activityIcon(change)).toBe('edit');
    expect(activitySummary(change)).toBe('alterou o título');
    expect(activityIcon(comment)).toBe('chat');
    expect(activitySummary(comment)).toBe('comentou');
    expect(hasValueChange(change)).toBeTrue();
  });
});

function feedChange(action: string, field?: string): ActivityChange {
  return {
    kind: 'change',
    id: 1,
    timestamp: 1,
    userName: 'Alice',
    action,
    field,
    oldValue: 'A',
    newValue: 'B',
  };
}

function feedComment(): ActivityComment {
  return {
    kind: 'comment',
    id: 1,
    timestamp: 1,
    userName: 'Bob',
    content: 'Hi',
  };
}
